package co.mipyme.screen.product_editor

import android.view.inputmethod.InputMethodManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.paging.PagingData
import androidx.test.platform.app.InstrumentationRegistry
import arrow.core.Either
import co.mipyme.contract.*
import co.mipyme.contract.request.*
import co.mipyme.data.inventory.InventoryContext
import co.mipyme.data.inventory.InventoryRepository
import co.mipyme.data.product_editor.ProductEditorContext
import co.mipyme.data.product_editor.ProductEditorRepository
import co.mipyme.database.CatalogQuery
import co.mipyme.screen.inventory.inventoryScreenModule
import co.mipyme.navigation.NavigationRoot
import co.mipyme.navigation.Route
import co.mipyme.network.NetworkError
import co.mipyme.screen.inventory.InventoryScreen
import co.mipyme.sync.SyncError
import co.mipyme.ui.MoshiTheme
import java.io.File
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.koin.compose.KoinIsolatedContext
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProductEditorFlowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val catalog = EditorTestCatalog()
    private val testApplication = koinApplication {
        modules(
            module {
                inventoryScreenModule()
                productEditorScreenModule()
                navigation<Route.Home> { InventoryScreen() }
            },
            module {
                single<InventoryRepository> { catalog }
                single<ProductEditorRepository> { catalog.editor }
            },
        )
    }

    @After
    fun closeTestApplication() {
        testApplication.close()
    }

    @Test
    fun createItemAppliesPricingAndReturnsToInventory() {
        start()
        compose.onNodeWithContentDescription("Add").performClick()
        compose.onNodeWithText("Add product").performClick()
        compose.onNodeWithText("Save").assertIsNotEnabled()
        capture("item")
        compose.onNodeWithContentDescription("Product name").performTextInput("Log Pose")
        compose.onNodeWithContentDescription("Barcode").performTextInput("00123")
        compose.onNodeWithText("Pricing").assertDoesNotExist()
        compose.onNodeWithText("Selling price", substring = true).performScrollTo()
        compose.onNodeWithText("Unit cost", substring = true).assertExists()
        compose.onNodeWithText("Selling price", substring = true).performClick()
        compose.onNodeWithContentDescription("Selling price").performTextInput("20")
        compose.onNodeWithContentDescription("Unit cost").performTextInput("25")
        compose.onNodeWithText("Done").assertIsNotEnabled()
        compose.onNodeWithText("Selling price must be at least the unit cost.").assertExists()
        compose.onNodeWithContentDescription("Unit cost").performTextReplacement("12")
        compose.runOnUiThread {
            compose.activity.getSystemService(InputMethodManager::class.java)
                .hideSoftInputFromWindow(compose.activity.window.decorView.windowToken, 0)
        }
        capture("pricing")
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("Save").performClick()
        compose.onNodeWithText("Product created").assertExists()
        compose.onNodeWithText("Inventory").assertExists()
        assertEquals("00123", catalog.created?.barcode)
        assertEquals("12", catalog.created?.unitCost?.value)
    }

    @Test
    fun serviceHasInlineSellingPriceAndReturnsAfterSave() {
        start()
        compose.onNodeWithContentDescription("Add").performClick()
        compose.onNodeWithText("Add service").performClick()
        compose.onNodeWithContentDescription("Service name").assertExists()
        compose.onNodeWithContentDescription("Barcode").assertDoesNotExist()
        compose.onNodeWithText("Pricing").assertDoesNotExist()
        capture("service")
        compose.onNodeWithText("Collection").performClick()
        compose.onNodeWithContentDescription("Search collections").performTextInput("Navigation")
        compose.onNodeWithText("Create “Navigation”").performClick()
        compose.onNodeWithText("Navigation").assertExists()
        compose.onNodeWithContentDescription("Service name").performTextInput("Navigation lesson")
        compose.onNodeWithContentDescription("Selling price").performTextInput("25")
        compose.onNodeWithText("Save").performClick()
        compose.onNodeWithText("Service created").assertExists()
        assertEquals(ProductType.SERVICE, catalog.created?.productType)
        assertEquals("navigation", catalog.created?.collectionId?.value)
    }

    @Test
    fun existingServiceOpensFromCatalogAndPromptsOnlyAfterChanges() {
        start()
        compose.onNodeWithText("Ship repair").performClick()
        compose.onNodeWithText("Edit service").assertExists()
        compose.onNodeWithText("Save").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Service name").performTextReplacement("Hull repair")
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Discard changes?").assertExists()
        compose.onNodeWithText("Discard").performClick()
        compose.onNodeWithText("Ship repair").assertExists()
    }

    private fun start() {
        compose.setContent {
            KoinIsolatedContext(testApplication) {
                MoshiTheme { NavigationRoot(startRoute = Route.Home) }
            }
        }
        compose.onNodeWithText("Ship repair").assertExists()
    }

    private fun capture(name: String) {
        if (InstrumentationRegistry.getArguments().getString("captureEditor") != "true") return
        compose.waitForIdle()
        val file = File(compose.activity.getExternalFilesDir(null), "editor-$name.png")
        lateinit var screenshot: Bitmap
        compose.runOnUiThread {
            val view = compose.activity.window.decorView
            screenshot = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(screenshot))
        }
        try {
            file.outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally {
            screenshot.recycle()
        }
    }
}

private class EditorTestCatalog : InventoryRepository {
    private val time = LocalDateTime.of(2026, 11, 17, 12, 0)
    private val service = Product(
        "galleyla", "Ship repair", "ship-repair", Decimal("0"), Decimal("30"), ProductType.SERVICE,
        ProductStatus.ACTIVE, null, null, emptyList(), emptyList(), time, time, null,
    )
    private val location = Location(
        "water7", "Water 7", BusinessType.HARDWARE_STORE, null, null, null, null,
        1, 1, false, null, null, time, time, Decimal("1"),
    )
    var created: CreateProductRequest? = null

    val editor = object : ProductEditorRepository {
        override fun observeContext(): Flow<ProductEditorContext> {
            return flowOf(ProductEditorContext(true, true, listOf(location), country = CountryIsoCode.NI))
        }
        override suspend fun findProduct(id: String): Either<NetworkError, Product> {
            return Either.Right(service)
        }
        override suspend fun createProduct(request: CreateProductRequest, idempotencyKey: String): Either<NetworkError, Product> {
            created = request
            return Either.Right(service.copy(
                id = "created", name = request.name, productType = request.productType,
                price = Decimal(request.price.value), unitCost = request.unitCost ?: Decimal("0"),
            ))
        }
        override suspend fun updateProduct(id: String, request: UpdateProductRequest): Either<NetworkError, Product> {
            error("Unexpected product update")
        }
        override suspend fun createCollection(request: CreateCollectionRequest, idempotencyKey: String): Either<NetworkError, co.mipyme.contract.Collection> {
            return Either.Right(co.mipyme.contract.Collection("navigation", request.name, "navigation", time, time))
        }
        override suspend fun uploadPhoto(productId: String, uri: Uri): Either<NetworkError, Product> {
            error("Unexpected photo upload")
        }
    }

    override fun observeProducts(query: CatalogQuery): Flow<PagingData<Product>> {
        return flowOf(PagingData.from(listOf(service)))
    }
    override fun observeProductCount(): Flow<Int> { return flowOf(1) }
    override fun observeCategories(): Flow<List<co.mipyme.contract.Collection>> { return flowOf(emptyList()) }
    override fun observeContext(): Flow<InventoryContext> { return flowOf(InventoryContext(canAdd = true)) }
    override suspend fun refresh(): Either<SyncError, Unit> { return Either.Right(Unit) }
}
