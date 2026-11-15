package co.mipyme.database.internal

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import co.mipyme.contract.EmployeeStatus
import co.mipyme.contract.ProductStatus
import co.mipyme.contract.SyncResponse
import co.mipyme.database.EmployeeLocationAccess
import co.mipyme.database.FIXTURE_TIME
import co.mipyme.database.SyncState
import co.mipyme.database.collection
import co.mipyme.database.customer
import co.mipyme.database.employee
import co.mipyme.database.location
import co.mipyme.database.merchant
import co.mipyme.database.optionGroup
import co.mipyme.database.product
import co.mipyme.database.productOption
import co.mipyme.database.productVariant
import co.mipyme.database.supplier
import co.mipyme.database.syncResponse
import java.time.LocalDateTime
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomMipymeCacheTest {
    private lateinit var database: MipymeCacheDatabase
    private lateinit var cache: RoomMipymeCache

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MipymeCacheDatabase::class.java).build()
        cache = RoomMipymeCache(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `applying a full sync stores every resource and the sync state`() = runTest {
        val product = product("prod_1")
        val response = syncResponse(
            products = listOf(product),
            collections = listOf(collection("col_1")),
            customers = listOf(customer("cus_1")),
            suppliers = listOf(supplier("sup_1")),
            locations = listOf(location("loc_1")),
        )

        store(response, merchantId = "merch_1", syncedAt = FIXTURE_TIME)

        assertEquals(merchant(), cache.observeMerchant().first())
        assertEquals(listOf(product), cache.observeProducts().first())
        assertEquals(listOf(collection("col_1")), cache.observeCollections().first())
        assertEquals(listOf(customer("cus_1")), cache.observeCustomers().first())
        assertEquals(listOf(supplier("sup_1")), cache.observeSuppliers().first())
        assertEquals(listOf(location("loc_1")), cache.observeLocations().first())
        assertEquals(SyncState("token_1", "merch_1", FIXTURE_TIME), cache.readSyncState())
        assertEquals(cache.readSyncState(), cache.observeSyncState().first())
    }

    @Test
    fun `a full sync clears rows missing from the response`() = runTest {
        store(syncResponse(products = listOf(product("old")), collections = listOf(collection("old"))), "merch_1", FIXTURE_TIME)

        store(syncResponse(products = listOf(product("new"))), "merch_1", FIXTURE_TIME)

        assertEquals(listOf("new"), cache.observeProducts().first().map { it.id })
        assertEquals(emptyList<Any>(), cache.observeCollections().first())
    }

    @Test
    fun `an incremental sync keeps rows missing from the response`() = runTest {
        store(syncResponse(products = listOf(product("old"))), "merch_1", FIXTURE_TIME)

        store(
            syncResponse(fullSync = false, syncToken = "token_2", merchant = null, products = listOf(product("new"))),
            "merch_1",
            FIXTURE_TIME,
        )

        assertEquals(listOf("new", "old"), cache.observeProducts().first().map { it.id })
        assertEquals(merchant(), cache.observeMerchant().first())
        assertEquals("token_2", cache.readSyncState()?.syncToken)
    }

    @Test
    fun `replacing a product removes stale variants variant options and inventory levels`() = runTest {
        val small = productOption("opt_s", position = 1.0)
        val large = productOption("opt_l", position = 2.0)
        val original = product(
            id = "prod_1",
            optionGroups = listOf(optionGroup("grp_size", listOf(small, large))),
            variants = listOf(
                productVariant("var_1", barcode = "111", options = listOf(small)),
                productVariant("var_2", barcode = "222", options = listOf(large)),
            ),
        )
        store(syncResponse(products = listOf(original)), "merch_1", FIXTURE_TIME)
        val updated = product(
            id = "prod_1",
            name = "Renamed",
            optionGroups = listOf(optionGroup("grp_size", listOf(small, large))),
            variants = listOf(productVariant("var_1", barcode = "111", options = listOf(small))),
        )

        store(syncResponse(fullSync = false, merchant = null, products = listOf(updated)), "merch_1", FIXTURE_TIME)

        assertEquals(listOf(updated), cache.observeProducts().first())
        assertNull(cache.findVariantByBarcode("222"))
        assertEquals(0, countRows("product_variants WHERE id = 'var_2'"))
        assertEquals(0, countRows("inventory_levels WHERE id = 'var_2_level'"))
        assertEquals(0, countRows("variant_options WHERE variant_id = 'var_2'"))
    }

    @Test
    fun `replacing one product keeps a group shared with another product`() = runTest {
        val small = productOption("opt_s", position = 1.0)
        val large = productOption("opt_l", position = 2.0)
        val shared = optionGroup("grp_size", listOf(small, large))
        val first = product("prod_1", optionGroups = listOf(shared), variants = listOf(productVariant("var_1", options = listOf(small))))
        val second = product("prod_2", optionGroups = listOf(shared), variants = listOf(productVariant("var_2", options = listOf(large))))
        store(syncResponse(products = listOf(first, second)), "merch_1", FIXTURE_TIME)
        val replaced = first.copy(name = "Renamed", optionGroups = emptyList(), variants = listOf(productVariant("var_1")))

        store(syncResponse(fullSync = false, merchant = null, products = listOf(replaced)), "merch_1", FIXTURE_TIME)

        assertEquals(listOf(replaced, second).sortedBy { it.name }, cache.observeProducts().first())
        assertEquals(1, countRows("option_groups"))
        assertEquals(2, countRows("product_options"))
    }

    @Test
    fun `a variant option from an unlinked group round trips through the cache`() = runTest {
        val orphan = productOption("opt_orphan")
        val product = product("prod_1", variants = listOf(productVariant("var_1", options = listOf(orphan))))

        store(syncResponse(products = listOf(product)), "merch_1", FIXTURE_TIME)

        assertEquals(listOf(product), cache.observeProducts().first())
    }

    @Test
    fun `an unlinked option is refreshed when the product is sent again`() = runTest {
        val orphan = productOption("opt_orphan", name = "Old")
        store(syncResponse(products = listOf(product("prod_2", variants = listOf(productVariant("var_2", options = listOf(orphan)))))), "merch_1", FIXTURE_TIME)
        val renamed = orphan.copy(name = "New", position = 4.0)
        val resent = product("prod_2", variants = listOf(productVariant("var_2", options = listOf(renamed))))

        store(syncResponse(fullSync = false, merchant = null, products = listOf(resent)), "merch_1", FIXTURE_TIME)

        assertEquals(listOf(resent), cache.observeProducts().first())
    }

    @Test
    fun `refreshing an unlinked option keeps the group it already has`() = runTest {
        val option = productOption("opt_s", position = 1.0)
        val linked = product("prod_1", optionGroups = listOf(optionGroup("grp_size", listOf(option))))
        val other = product("prod_2", variants = listOf(productVariant("var_2", options = listOf(option.copy(name = "Renamed")))))
        store(syncResponse(products = listOf(linked)), "merch_1", FIXTURE_TIME)

        store(syncResponse(fullSync = false, merchant = null, products = listOf(other)), "merch_1", FIXTURE_TIME)

        assertEquals(1, countRows("product_options WHERE id = 'opt_s' AND option_group_id = 'grp_size'"))
        assertEquals("Renamed", cache.observeProducts().first().first { it.id == "prod_2" }.variants.single().options.single().name)
    }

    @Test
    fun `applying the same response twice is idempotent`() = runTest {
        val response = syncResponse(products = listOf(product("prod_1")))

        store(response, "merch_1", FIXTURE_TIME)
        store(response, "merch_1", FIXTURE_TIME)

        assertEquals(listOf(product("prod_1")), cache.observeProducts().first())
    }

    @Test
    fun `finds a variant and its product by barcode`() = runTest {
        val target = productVariant("var_2", barcode = "222")
        val product = product("prod_1", variants = listOf(productVariant("var_1", barcode = "111"), target))
        store(syncResponse(products = listOf(product)), "merch_1", FIXTURE_TIME)

        val match = cache.findVariantByBarcode("222")

        assertEquals(product, match?.product)
        assertEquals(target, match?.variant)
        assertNull(cache.findVariantByBarcode("999"))
    }

    @Test
    fun `barcode lookup prefers a product that is not archived`() = runTest {
        val archived = product("prod_a", status = ProductStatus.ARCHIVED, variants = listOf(productVariant("var_a", barcode = "333")))
        val active = product("prod_b", variants = listOf(productVariant("var_b", barcode = "333")))
        store(syncResponse(products = listOf(archived, active)), "merch_1", FIXTURE_TIME)

        assertEquals("prod_b", cache.findVariantByBarcode("333")?.product?.id)
    }

    @Test
    fun `clearing the cache removes every table`() = runTest {
        store(
            syncResponse(
                products = listOf(product("prod_1")),
                collections = listOf(collection("col_1")),
                customers = listOf(customer("cus_1")),
                suppliers = listOf(supplier("sup_1")),
                locations = listOf(location("loc_1")),
            ),
            "merch_1",
            FIXTURE_TIME,
        )

        cache.clearCache()

        assertNull(cache.observeMerchant().first())
        assertEquals(emptyList<Any>(), cache.observeProducts().first())
        assertEquals(emptyList<Any>(), cache.observeCollections().first())
        assertEquals(emptyList<Any>(), cache.observeCustomers().first())
        assertEquals(emptyList<Any>(), cache.observeSuppliers().first())
        assertEquals(emptyList<Any>(), cache.observeLocations().first())
        assertNull(cache.readSyncState())
        assertEquals(0, countRows("product_variants"))
        assertEquals(0, countRows("inventory_levels"))
        assertEquals(0, countRows("option_groups"))
        assertEquals(0, countRows("product_options"))
    }

    @Test
    fun `a full sync stores employees and their location ids`() = runTest {
        store(
            syncResponse(
                employees = listOf(
                    employee("emp_1", listOf("loc_1", "loc_2"), isOwner = true),
                    employee("emp_2", status = EmployeeStatus.INVITED, account = null),
                ),
            ),
            "merch_1",
            FIXTURE_TIME,
        )

        assertEquals(2, countRows("employees"))
        assertEquals(1, countRows("employees WHERE id = 'emp_2' AND account_id IS NULL AND status = 'INVITED'"))
        assertEquals(1, countRows("employees WHERE id = 'emp_1' AND permissions = 'CHECKOUT,REFUND' AND is_owner = 1"))
        assertEquals(2, countRows("employee_locations WHERE employee_id = 'emp_1'"))
    }

    @Test
    fun `a full sync removes employees missing from the response`() = runTest {
        store(syncResponse(employees = listOf(employee("old", listOf("loc_1")))), "merch_1", FIXTURE_TIME)

        store(syncResponse(employees = listOf(employee("new", listOf("loc_2")))), "merch_1", FIXTURE_TIME)

        assertEquals(0, countRows("employees WHERE id = 'old'"))
        assertEquals(0, countRows("employee_locations WHERE employee_id = 'old'"))
        assertEquals(1, countRows("employee_locations WHERE employee_id = 'new' AND location_id = 'loc_2'"))
    }

    @Test
    fun `an incremental sync replaces the location ids of the sent employee only`() = runTest {
        store(
            syncResponse(
                employees = listOf(
                    employee("emp_1", listOf("loc_1", "loc_2")),
                    employee("emp_2", listOf("loc_3")),
                ),
            ),
            "merch_1",
            FIXTURE_TIME,
        )

        store(
            syncResponse(fullSync = false, merchant = null, employees = listOf(employee("emp_1", listOf("loc_2", "loc_4")))),
            "merch_1",
            FIXTURE_TIME,
        )

        assertEquals(2, countRows("employee_locations WHERE employee_id = 'emp_1'"))
        assertEquals(0, countRows("employee_locations WHERE employee_id = 'emp_1' AND location_id = 'loc_1'"))
        assertEquals(1, countRows("employee_locations WHERE employee_id = 'emp_1' AND location_id = 'loc_4'"))
        assertEquals(1, countRows("employee_locations WHERE employee_id = 'emp_2' AND location_id = 'loc_3'"))
    }

    @Test
    fun `an incremental sync can remove every location id of an employee`() = runTest {
        store(syncResponse(employees = listOf(employee("emp_1", listOf("loc_1")))), "merch_1", FIXTURE_TIME)

        store(
            syncResponse(fullSync = false, merchant = null, employees = listOf(employee("emp_1", emptyList()))),
            "merch_1",
            FIXTURE_TIME,
        )

        assertEquals(0, countRows("employee_locations"))
        assertEquals(1, countRows("employees"))
    }

    @Test
    fun `clearing the cache removes employees and their location ids`() = runTest {
        store(syncResponse(employees = listOf(employee("emp_1", listOf("loc_1")))), "merch_1", FIXTURE_TIME)

        cache.clearCache()

        assertEquals(0, countRows("employees"))
        assertEquals(0, countRows("employee_locations"))
    }

    @Test
    fun `clearing for another merchant removes employees`() = runTest {
        store(syncResponse(employees = listOf(employee("emp_1", listOf("loc_1")))), "merch_1", FIXTURE_TIME)

        cache.clearCacheUnlessMerchant("merch_2")

        assertEquals(0, countRows("employees"))
        assertEquals(0, countRows("employee_locations"))
    }

    @Test
    fun `a non owner observes only the locations assigned to them`() = runTest {
        store(
            syncResponse(
                locations = listOf(location("loc_1"), location("loc_2"), location("loc_3")),
                employees = listOf(employee("emp_1", listOf("loc_3", "loc_1")), employee("emp_2", listOf("loc_2"))),
            ),
            "merch_1",
            FIXTURE_TIME,
        )

        val first = cache.observeLocationAccess("emp_1").first()
        assertEquals("merch_1", first?.merchantId)
        assertEquals(false, first?.isOwner)
        assertEquals(listOf("loc_1", "loc_3"), first?.locations?.map { it.id })
        assertEquals(listOf("loc_2"), cache.observeLocationAccess("emp_2").first()?.locations?.map { it.id })
    }

    @Test
    fun `an owner observes every cached location`() = runTest {
        store(
            syncResponse(
                locations = listOf(location("loc_2"), location("loc_1")),
                employees = listOf(employee("owner", isOwner = true)),
            ),
            "merch_1",
            FIXTURE_TIME,
        )

        val access = cache.observeLocationAccess("owner").first()

        assertEquals(true, access?.isOwner)
        assertEquals(listOf("loc_1", "loc_2"), access?.locations?.map { it.id })
    }

    @Test
    fun `an enabled non owner without assignments observes an empty list`() = runTest {
        store(
            syncResponse(locations = listOf(location("loc_1")), employees = listOf(employee("emp_1", emptyList()))),
            "merch_1",
            FIXTURE_TIME,
        )

        assertEquals(EmployeeLocationAccess("merch_1", false, emptyList()), cache.observeLocationAccess("emp_1").first())
    }

    @Test
    fun `an enabled owner without cached locations observes an empty list`() = runTest {
        store(syncResponse(employees = listOf(employee("owner", isOwner = true))), "merch_1", FIXTURE_TIME)

        assertEquals(EmployeeLocationAccess("merch_1", true, emptyList()), cache.observeLocationAccess("owner").first())
    }

    @Test
    fun `a non owner that is not enabled or an unknown employee observes an empty synced access`() = runTest {
        store(
            syncResponse(
                locations = listOf(location("loc_1")),
                employees = listOf(
                    employee("disabled", listOf("loc_1"), EmployeeStatus.DISABLED),
                    employee("invited", listOf("loc_1"), EmployeeStatus.INVITED),
                ),
            ),
            "merch_1",
            FIXTURE_TIME,
        )
        val empty = EmployeeLocationAccess("merch_1", false, emptyList())

        assertEquals(empty, cache.observeLocationAccess("disabled").first())
        assertEquals(empty, cache.observeLocationAccess("invited").first())
        assertEquals(empty, cache.observeLocationAccess("missing").first())
    }

    @Test
    fun `an owner observes every cached location whatever their status`() = runTest {
        store(
            syncResponse(
                locations = listOf(location("loc_2"), location("loc_1")),
                employees = listOf(
                    employee("disabled_owner", status = EmployeeStatus.DISABLED, isOwner = true),
                    employee("invited_owner", status = EmployeeStatus.INVITED, isOwner = true),
                ),
            ),
            "merch_1",
            FIXTURE_TIME,
        )

        listOf("disabled_owner", "invited_owner").forEach { id ->
            val access = cache.observeLocationAccess(id).first()

            assertEquals(id, true, access?.isOwner)
            assertEquals(id, listOf("loc_1", "loc_2"), access?.locations?.map { it.id })
        }
    }

    @Test
    fun `location access is unavailable before any sync`() = runTest {
        assertNull(cache.observeLocationAccess("emp_1").first())
    }

    @Test
    fun `location access is unavailable again after the cache is cleared`() = runTest {
        store(syncResponse(employees = listOf(employee("owner", isOwner = true))), "merch_1", FIXTURE_TIME)

        cache.clearCache()

        assertNull(cache.observeLocationAccess("owner").first())
    }

    @Test
    fun `assigned locations ignore ids of locations that are not cached`() = runTest {
        store(
            syncResponse(locations = listOf(location("loc_1")), employees = listOf(employee("emp_1", listOf("loc_1", "gone")))),
            "merch_1",
            FIXTURE_TIME,
        )

        assertEquals(listOf("loc_1"), cache.observeLocationAccess("emp_1").first()?.locations?.map { it.id })
    }

    @Test
    fun `location access updates when the employee or the locations change`() = runTest {
        store(
            syncResponse(locations = listOf(location("loc_1")), employees = listOf(employee("emp_1", listOf("loc_1", "loc_2")))),
            "merch_1",
            FIXTURE_TIME,
        )
        val emissions = Channel<EmployeeLocationAccess?>(Channel.UNLIMITED)
        backgroundScope.launch { cache.observeLocationAccess("emp_1").collect { emissions.send(it) } }
        assertEquals(listOf("loc_1"), emissions.receive()?.locations?.map { it.id })

        store(syncResponse(fullSync = false, merchant = null, locations = listOf(location("loc_2"))), "merch_1", FIXTURE_TIME)
        assertEquals(listOf("loc_1", "loc_2"), emissions.receive()?.locations?.map { it.id })

        store(
            syncResponse(fullSync = false, merchant = null, employees = listOf(employee("emp_1", listOf("loc_2")))),
            "merch_1",
            FIXTURE_TIME,
        )
        assertEquals(listOf("loc_2"), emissions.receive()?.locations?.map { it.id })
    }

    @Test
    fun `merchant and sync state stay single rows`() = runTest {
        store(syncResponse(syncToken = "token_1"), "merch_1", FIXTURE_TIME)
        val later = LocalDateTime.of(2027, 1, 1, 0, 0)

        store(
            syncResponse(fullSync = false, syncToken = "token_2", merchant = merchant().copy(name = "Renamed")),
            "merch_1",
            later,
        )

        assertEquals(1, countRows("merchant"))
        assertEquals(1, countRows("sync_state"))
        assertEquals("Renamed", cache.observeMerchant().first()?.name)
        assertEquals(SyncState("token_2", "merch_1", later), cache.readSyncState())
    }

    @Test
    fun `reading the sync state before any sync returns null`() = runTest {
        assertNull(cache.readSyncState())
        assertNull(cache.observeSyncState().first())
    }

    @Test
    fun `a write whose guard fails stores nothing`() = runTest {
        val applied = cache.apply(syncResponse(products = listOf(product("prod_1"))), "merch_1", FIXTURE_TIME) { false }

        assertEquals(false, applied)
        assertNull(cache.readSyncState())
        assertEquals(emptyList<Any>(), cache.observeProducts().first())
    }

    @Test
    fun `clearing unless the merchant matches only clears another merchant`() = runTest {
        store(syncResponse(products = listOf(product("prod_1"))), "merch_1", FIXTURE_TIME)

        cache.clearCacheUnlessMerchant("merch_1")
        assertEquals(1, cache.observeProducts().first().size)

        cache.clearCacheUnlessMerchant("merch_2")
        assertEquals(emptyList<Any>(), cache.observeProducts().first())
        assertNull(cache.readSyncState())
    }

    private suspend fun store(response: SyncResponse, merchantId: String, syncedAt: LocalDateTime) {
        cache.apply(response, merchantId, syncedAt) { true }
    }

    private fun countRows(tableAndFilter: String): Int {
        return database.openHelper.readableDatabase
            .query("SELECT COUNT(*) FROM $tableAndFilter")
            .use { cursor ->
                cursor.moveToFirst()
                cursor.getInt(0)
            }
    }
}
