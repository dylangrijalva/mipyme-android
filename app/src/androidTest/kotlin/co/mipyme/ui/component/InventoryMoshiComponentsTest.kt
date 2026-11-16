package co.mipyme.ui.component

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import co.mipyme.R
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiTheme
import java.io.File
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class InventoryMoshiComponentsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val searchLabel = context.getString(R.string.inventory_search_products)
    private val clearLabel = context.getString(R.string.inventory_clear_search)
    private val secondaryLabel = context.getString(R.string.inventory_clear_filters)

    @Test
    fun searchClearAppearsForTextAndClearsThroughValueCallback() {
        val query = mutableStateOf("")
        composeRule.setContent {
            MoshiTheme {
                MoshiSearchField(
                    value = query.value,
                    onValueChange = { query.value = it },
                    label = searchLabel,
                    clearLabel = clearLabel,
                )
            }
        }

        composeRule.onNodeWithContentDescription(clearLabel).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(searchLabel).performTextInput("Straw Hat")
        composeRule.onNodeWithContentDescription(clearLabel)
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
            .performClick()
        composeRule.runOnIdle { assertEquals("", query.value) }
        composeRule.onNodeWithContentDescription(clearLabel).assertDoesNotExist()
    }

    @Test
    fun disabledSearchKeepsClearVisibleAndDoesNotActivateIt() {
        var clears = 0
        composeRule.setContent {
            MoshiTheme {
                MoshiSearchField(
                    value = "Straw Hat",
                    onValueChange = {},
                    label = searchLabel,
                    clearLabel = clearLabel,
                    onClear = { clears++ },
                    enabled = false,
                )
            }
        }

        composeRule.onNodeWithContentDescription(searchLabel).assertIsNotEnabled()
        composeRule.onNodeWithContentDescription(clearLabel)
            .assertIsDisplayed()
            .assertIsNotEnabled()
            .performTouchInput { click() }
        composeRule.runOnIdle { assertEquals(0, clears) }
    }

    @Test
    fun emptyStateSupportsAllOptionalActionCombinations() {
        val primaryVisible = mutableStateOf(false)
        val secondaryVisible = mutableStateOf(false)
        var primaryClicks = 0
        var secondaryClicks = 0
        composeRule.setContent {
            MoshiTheme {
                MoshiEmptyState(
                    title = context.getString(R.string.inventory_no_matches_title),
                    description = context.getString(R.string.inventory_no_matches_description),
                    icon = {},
                    primaryAction = if (primaryVisible.value) {
                        MoshiEmptyStateAction(clearLabel, { primaryClicks++ })
                    } else {
                        null
                    },
                    secondaryAction = if (secondaryVisible.value) {
                        MoshiEmptyStateAction(secondaryLabel, { secondaryClicks++ })
                    } else {
                        null
                    },
                )
            }
        }

        val primary = composeRule.onNodeWithText(clearLabel)
        val secondary = composeRule.onNodeWithText(secondaryLabel)
        primary.assertDoesNotExist()
        secondary.assertDoesNotExist()

        composeRule.runOnIdle { primaryVisible.value = true }
        primary.assertIsDisplayed().assertIsEnabled().performClick()
        secondary.assertDoesNotExist()

        composeRule.runOnIdle {
            primaryVisible.value = false
            secondaryVisible.value = true
        }
        primary.assertDoesNotExist()
        secondary.assertIsDisplayed().assertIsEnabled().performClick()

        composeRule.runOnIdle { primaryVisible.value = true }
        primary.assertIsDisplayed()
        secondary.assertIsDisplayed()
        val primaryBounds = primary.fetchSemanticsNode().boundsInRoot
        val secondaryBounds = secondary.fetchSemanticsNode().boundsInRoot
        assertTrue(primaryBounds.bottom <= secondaryBounds.top)
        composeRule.runOnIdle {
            assertEquals(1, primaryClicks)
            assertEquals(1, secondaryClicks)
        }
    }

    @Test
    fun pillExposesSelectionAndDisabledActionsIgnoreTouches() {
        val selected = mutableStateOf(false)
        val enabled = mutableStateOf(true)
        var clicks = 0
        composeRule.setContent {
            MoshiTheme {
                MoshiPill(
                    label = context.getString(R.string.inventory_products),
                    selected = selected.value,
                    enabled = enabled.value,
                    onClick = {
                        selected.value = true
                        clicks++
                    },
                    role = Role.Tab,
                    modifier = Modifier.testTag("productsPill"),
                )
            }
        }

        val pill = composeRule.onNodeWithTag("productsPill")
        pill.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
        pill.assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
        pill.assertIsNotSelected().assertIsEnabled().performClick()
        pill.assertIsSelected()
        composeRule.runOnIdle { enabled.value = false }
        pill.assertIsNotEnabled().assertIsSelected().performTouchInput { click() }
        composeRule.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun captureLightComponents() {
        captureComponents(darkTheme = false, fontScale = 1f, filename = "inventory-moshi-light.png")
    }

    @Test
    fun captureDarkComponents() {
        captureComponents(darkTheme = true, fontScale = 1f, filename = "inventory-moshi-dark.png")
    }

    @Test
    fun captureLargeFontComponents() {
        captureComponents(darkTheme = false, fontScale = 2f, filename = "inventory-moshi-large-font.png")
    }

    private fun captureComponents(darkTheme: Boolean, fontScale: Float, filename: String) {
        composeRule.setContent {
            MoshiTheme(darkTheme = darkTheme) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                    ComponentRenderContent()
                }
            }
        }
        composeRule.onNodeWithTag("render").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.inventory_no_matches_description))
            .assertIsDisplayed()
        composeRule.waitForIdle()
        val render = composeRule.onNodeWithTag("render")
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            render.captureToImage().asAndroidBitmap()
        } else {
            val bounds = render.fetchSemanticsNode().boundsInWindow
            val content = Bitmap.createBitmap(
                bounds.width.roundToInt(),
                bounds.height.roundToInt(),
                Bitmap.Config.ARGB_8888,
            )
            composeRule.runOnUiThread {
                val canvas = Canvas(content)
                canvas.translate(-bounds.left, -bounds.top)
                composeRule.activity.window.decorView.draw(canvas)
            }
            content
        }
        val directory = File(checkNotNull(context.getExternalFilesDir(null)), "inventory-moshi")
        check(directory.exists() || directory.mkdirs())
        val screenshot = File(directory, filename)
        screenshot.outputStream().use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
        }
        assertTrue(screenshot.length() > 0)
        Log.i("InventoryMoshiComponents", "Screenshot: ${screenshot.absolutePath}")
    }

    @Composable
    private fun ComponentRenderContent() {
        val spacing = Moshi.spacing
        Column(
            modifier = Modifier
                .testTag("render")
                .width(320.dp)
                .background(Moshi.colors.background100)
                .padding(spacing.space4),
        ) {
            MoshiSearchField(
                value = "Straw Hat",
                onValueChange = {},
                label = searchLabel,
                clearLabel = clearLabel,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(spacing.space3))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MoshiPill(
                    label = context.getString(R.string.inventory_products),
                    selected = true,
                    onClick = {},
                    role = Role.Tab,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(spacing.space2))
                MoshiPill(
                    label = context.getString(R.string.inventory_categories),
                    selected = false,
                    onClick = {},
                    enabled = false,
                    role = Role.Tab,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(spacing.space3))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MoshiMonogram("Straw Hat Pirates")
                Spacer(Modifier.width(spacing.space3))
                MoshiStatusBadge(
                    label = context.getString(R.string.inventory_out_of_stock),
                    tone = MoshiStatusBadgeTone.Warning,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
            Spacer(Modifier.height(spacing.space3))
            MoshiEmptyState(
                title = context.getString(R.string.inventory_no_matches_title),
                description = context.getString(R.string.inventory_no_matches_description),
                icon = {
                    Icon(
                        painter = painterResource(co.mipyme.ui.R.drawable.ic_fluent_search_24_regular),
                        contentDescription = null,
                        modifier = Modifier.size(spacing.space12),
                    )
                },
            )
        }
    }
}
