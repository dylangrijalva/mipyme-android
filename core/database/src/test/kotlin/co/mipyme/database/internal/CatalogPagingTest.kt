package co.mipyme.database.internal

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import co.mipyme.contract.ProductCollection
import co.mipyme.contract.ProductStatus
import co.mipyme.contract.ProductType
import co.mipyme.database.CatalogGrouping
import co.mipyme.database.CatalogQuery
import co.mipyme.database.FIXTURE_TIME
import co.mipyme.database.collection
import co.mipyme.database.product
import co.mipyme.database.productVariant
import co.mipyme.database.syncResponse
import co.mipyme.database.internal.relation.ProductWithDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CatalogPagingTest {
    private lateinit var database: MipymeCacheDatabase
    private lateinit var cache: RoomMipymeCache

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(), MipymeCacheDatabase::class.java,
        ).build()
        cache = RoomMipymeCache(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `pages search the full catalog and variant matches never duplicate a product`() = runTest {
        val products = (0..125).map { index ->
            product("prod_$index", name = "Sunny ${index.toString().padStart(3, '0')}", variants = listOf(
                productVariant("var_${index}_a", barcode = "WANO-$index").copy(name = "Grand Line", sku = "LOG-$index"),
                productVariant("var_${index}_b", barcode = "WANO-$index").copy(name = "Grand Line"),
            ))
        }
        cache.apply(syncResponse(products = products), "strawhatpirates", FIXTURE_TIME) { true }
        val source = source(CatalogQuery())
        val first = source.load(PagingSource.LoadParams.Refresh(null, 50, false)).page()
        val second = source.load(PagingSource.LoadParams.Append(first.nextKey!!, 50, false)).page()
        val third = source.load(PagingSource.LoadParams.Append(second.nextKey!!, 50, false)).page()
        assertEquals(126, (first.data + second.data + third.data).map { it.product.id }.distinct().size)
        for (query in listOf("sunNY 125", "ano-125", "og-125")) {
            assertEquals(listOf("prod_125"), ids(CatalogQuery(text = query)))
        }
        assertEquals(126, cache.observeCatalogCount(CatalogQuery(text = "grand line")).first())
    }

    @Test
    fun `literal wildcards filters default statuses and stable grouping are applied before paging`() = runTest {
        val products = listOf(
            product("luffy", "Sunny", status = ProductStatus.ACTIVE).copy(collection = ProductCollection("wano", "Old Wano")),
            product("zoro", "Sunny", status = ProductStatus.DRAFT).copy(collection = ProductCollection("wano", "Old Wano")),
            product("nami", "Sunny", status = ProductStatus.ARCHIVED).copy(collection = null),
            product("sanji", "Árbol 100%_! [Wano]*?", status = ProductStatus.ACTIVE).copy(productType = ProductType.SERVICE, collection = ProductCollection("alabasta", "Alabasta")),
        )
        cache.apply(syncResponse(products = products, collections = listOf(
            collection("wano").copy(name = "Wano"), collection("alabasta").copy(name = "Alabasta"),
        )), "strawhatpirates", FIXTURE_TIME) { true }
        assertEquals(4, cache.observeCatalogCount().first())
        assertEquals(listOf("sanji"), ids(CatalogQuery(text = "%_!")))
        assertEquals(listOf("sanji"), ids(CatalogQuery(text = "árBOL")))
        assertEquals(listOf("sanji"), ids(CatalogQuery(text = "[Wano]*?")))
        assertEquals(listOf("luffy", "zoro"), ids(CatalogQuery(categoryIds = setOf("wano"), statuses = setOf(ProductStatus.ACTIVE, ProductStatus.DRAFT), type = ProductType.ITEM)))
        assertEquals(listOf("nami"), ids(CatalogQuery(includeUncategorized = true)))
        assertEquals(listOf("luffy", "nami", "zoro"), ids(CatalogQuery(text = "sunny")))
        assertEquals(listOf("sanji", "luffy", "zoro", "nami"), ids(CatalogQuery(grouping = CatalogGrouping.CATEGORY)))
        assertEquals(listOf("luffy", "sanji", "zoro", "nami"), ids(CatalogQuery(grouping = CatalogGrouping.STATUS)))
        assertEquals("Wano", source(CatalogQuery(text = "Sunny")).load(PagingSource.LoadParams.Refresh(null, 50, false)).page().data.first().product.collectionName)
    }

    @Test
    fun `Unicode case equivalence matches reverse mappings consistently`() = runTest {
        cache.apply(syncResponse(products = listOf(
            product("luffy", "İı", variants = emptyList()), product("zoro", "Σς", variants = emptyList()), product("nami", "ÁRBOL", variants = emptyList()),
            product("robin", "𠀪", variants = emptyList()),
        )), "strawhatpirates", FIXTURE_TIME) { true }
        assertEquals(listOf("luffy"), ids(CatalogQuery(text = "i")))
        assertEquals(listOf("luffy"), ids(CatalogQuery(text = "I")))
        assertEquals(listOf("zoro"), ids(CatalogQuery(text = "σ")))
        assertEquals(listOf("zoro"), ids(CatalogQuery(text = "ς")))
        assertEquals(listOf("nami"), ids(CatalogQuery(text = "árbol")))
        assertEquals(listOf("robin"), ids(CatalogQuery(text = "𠀪")))
    }

    @Test
    fun `each group stays contiguous across a page boundary with uncategorized last`() = runTest {
        val products = (0..109).map { index ->
            product("sunny_$index", "Sunny ${index.toString().padStart(3, '0')}").copy(
                collection = if (index < 75) ProductCollection("wano", "Wano") else null,
            )
        }
        cache.apply(syncResponse(products = products), "strawhatpirates", FIXTURE_TIME) { true }
        val source = source(CatalogQuery(grouping = CatalogGrouping.CATEGORY))
        val first = source.load(PagingSource.LoadParams.Refresh(null, 50, false)).page()
        val second = source.load(PagingSource.LoadParams.Append(first.nextKey!!, 50, false)).page()
        assertTrue(first.data.all { it.product.collectionId == "wano" })
        assertEquals(25, second.data.takeWhile { it.product.collectionId == "wano" }.size)
        assertTrue(second.data.drop(25).all { it.product.collectionId == null })
    }

    @Test
    fun `product variant stock and category writes invalidate the paged generation`() = runTest {
        cache.apply(syncResponse(products = listOf(product("sunny")), collections = listOf(collection("col_1"))), "strawhatpirates", FIXTURE_TIME) { true }
        val updates = listOf(
            "UPDATE products SET name = 'Thousand Sunny'",
            "UPDATE product_variants SET sku = 'WANO'",
            "UPDATE inventory_levels SET on_hand = 1.25",
            "UPDATE collections SET name = 'Water 7'",
        )
        updates.forEach { sql ->
            val source = source(CatalogQuery())
            source.load(PagingSource.LoadParams.Refresh(null, 50, false)).page()
            withContext(Dispatchers.IO) { database.withTransaction { database.openHelper.writableDatabase.execSQL(sql) } }
            withContext(Dispatchers.IO) { withTimeout(5000) { while (!source.invalid) delay(10) } }
            assertTrue(sql, source.invalid)
        }
        val updated = source(CatalogQuery()).load(PagingSource.LoadParams.Refresh(null, 50, false)).page().data.single()
        assertEquals("Water 7", updated.product.collectionName)
        assertEquals(1.25, updated.variants.single().inventoryLevels.single().onHand, 0.0)
    }

    private fun source(query: CatalogQuery): PagingSource<Int, ProductWithDetails> {
        return database.productDao().pageCatalog(query.toSqlQuery())
    }

    private suspend fun ids(query: CatalogQuery): List<String> {
        return source(query).load(PagingSource.LoadParams.Refresh(null, 200, false)).page().data.map { it.product.id }
    }

    private fun PagingSource.LoadResult<Int, ProductWithDetails>.page(): PagingSource.LoadResult.Page<Int, ProductWithDetails> {
        assertTrue(toString(), this is PagingSource.LoadResult.Page)
        return this as PagingSource.LoadResult.Page
    }
}
