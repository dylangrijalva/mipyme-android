package co.mipyme.ui.component

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiTheme
import java.io.File
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class MoshiChoiceboxTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val options = listOf(
        MoshiChoiceboxOption("water7", "Water 7", "Visit the shipwrights."),
        MoshiChoiceboxOption("alabasta", "Alabasta", "Explore the desert kingdom."),
    )

    @Test
    fun singleSelectionUsesWholeTilesAndRemainsExclusive() {
        val selected = mutableStateOf<String?>("water7")
        var changes = 0
        composeRule.setContent {
            MoshiTheme {
                MoshiChoicebox(
                    label = "Choose a destination",
                    options = options,
                    selectedValue = selected.value,
                    onValueChange = {
                        selected.value = it
                        changes++
                    },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Choose a destination")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.SelectableGroup, Unit))
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(2)
        composeRule.onNodeWithText("Water 7")
            .assertIsSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assert(hasText("Visit the shipwrights."))
            .performClick()
        composeRule.runOnIdle { assertEquals(0, changes) }

        // The edge of the tile selects it, outside the label and visual indicator.
        composeRule.onNodeWithText("Alabasta").performTouchInput { click(Offset(2f, center.y)) }
        composeRule.onNodeWithText("Alabasta").assertIsSelected()
        composeRule.onNodeWithText("Water 7").assertIsNotSelected()
        composeRule.runOnIdle { assertEquals(1, changes) }
    }

    @Test
    fun multipleSelectionAddsAndRemovesWithoutClearingOtherChoices() {
        val selected = mutableStateOf(setOf("water7"))
        composeRule.setContent {
            MoshiTheme(darkTheme = true) {
                MoshiChoicebox(
                    label = "Choose destinations",
                    options = options,
                    selectedValues = selected.value,
                    onValuesChange = { selected.value = it },
                )
            }
        }

        composeRule.onAllNodes(hasClickAction()).assertCountEquals(2)
        composeRule.onNodeWithText("Water 7").assertIsOn()
        composeRule.onNodeWithText("Alabasta")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assertIsOff()
            .performClick()
        composeRule.runOnIdle { assertEquals(setOf("water7", "alabasta"), selected.value) }
        composeRule.onNodeWithText("Alabasta").assertIsOn()
        composeRule.onNodeWithText("Water 7").performClick().assertIsOff()
        composeRule.runOnIdle { assertEquals(setOf("alabasta"), selected.value) }
    }

    @Test
    fun groupAndIndividualDisabledStatesPreventActivationAndExposeReasons() {
        val enabled = mutableStateOf(true)
        var changes = 0
        composeRule.setContent {
            MoshiTheme {
                MoshiChoicebox(
                    label = "Choose a destination",
                    options = listOf(options[0], options[1].copy(enabled = false, disabledReason = "Route closed.")),
                    selectedValue = "alabasta",
                    onValueChange = { changes++ },
                    enabled = enabled.value,
                    disabledReason = "Routes unavailable.",
                )
            }
        }

        composeRule.onNodeWithText("Water 7").assertIsEnabled()
        composeRule.onNodeWithText("Alabasta")
            .assertIsNotEnabled()
            .assertIsSelected()
            .assert(hasText("Route closed."))
            .performTouchInput { click() }
        composeRule.runOnIdle { enabled.value = false }
        composeRule.onNodeWithText("Water 7")
            .assertIsNotEnabled()
            .assert(hasText("Routes unavailable."))
            .performTouchInput { click() }
        composeRule.runOnIdle { assertEquals(0, changes) }
    }

    @Test
    fun disabledCheckboxGroupPreservesItsSelection() {
        var changes = 0
        composeRule.setContent {
            MoshiTheme(darkTheme = true) {
                MoshiChoicebox(
                    label = "Choose destinations",
                    options = options,
                    selectedValues = setOf("water7"),
                    onValuesChange = { changes++ },
                    enabled = false,
                    orientation = Orientation.Vertical,
                )
            }
        }

        composeRule.onNodeWithText("Water 7").assertIsNotEnabled().assertIsOn()
            .performTouchInput { click() }
        composeRule.onNodeWithText("Alabasta").assertIsNotEnabled().assertIsOff()
            .performTouchInput { click() }
        composeRule.runOnIdle { assertEquals(0, changes) }
    }

    @Test
    fun customContentAppearsOnlyWhileItsOptionIsSelected() {
        val selected = mutableStateOf<String?>(null)
        composeRule.setContent {
            MoshiTheme {
                MoshiChoicebox(
                    label = "Choose a destination",
                    options = listOf(
                        options[0].copy(selectedContent = {
                            BoxWithConstraints {
                                MoshiText(if (maxWidth > 48.dp) "Shipwrights are ready." else "Water 7")
                            }
                        }),
                        options[1],
                    ),
                    selectedValue = selected.value,
                    onValueChange = { selected.value = it },
                )
            }
        }

        composeRule.onNodeWithText("Shipwrights are ready.", useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithText("Water 7").performClick()
        composeRule.onNodeWithText("Water 7").assert(hasText("Shipwrights are ready."))
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(2)
        composeRule.onNodeWithText("Alabasta").performClick()
        composeRule.onNodeWithText("Shipwrights are ready.", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun largeTextKeepsContentAndMinimumTouchTargets() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                MoshiTheme {
                    MoshiChoicebox(
                        label = "Choose a destination",
                        options = options,
                        selectedValue = "water7",
                        onValueChange = {},
                    )
                }
            }
        }

        options.forEach { option ->
            composeRule.onNodeWithText(option.title)
                .assertHeightIsAtLeast(48.dp)
                .assertWidthIsAtLeast(48.dp)
                .assert(hasText(option.description!!))
            composeRule.onNodeWithText(option.title, useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { getLayout ->
                    val results = mutableListOf<TextLayoutResult>()
                    getLayout(results)
                    assertFalse(results.single().hasVisualOverflow)
                }
        }
    }

    @Test
    fun capturesLightAndDarkChoiceboxStates() {
        val dark = mutableStateOf(false)
        composeRule.setContent {
            MoshiTheme(darkTheme = dark.value) {
                Column(
                    modifier = Modifier
                        .testTag("choicebox-render")
                        .width(360.dp)
                        .background(Moshi.colors.background.surface)
                        .padding(Moshi.spacing.space4),
                    verticalArrangement = Arrangement.spacedBy(Moshi.spacing.space4),
                ) {
                    MoshiChoicebox(
                        label = "Choose a destination",
                        options = listOf(
                            options[0].copy(selectedContent = { MoshiText("Shipwrights are ready.") }),
                            options[1],
                        ),
                        selectedValue = "water7",
                        onValueChange = {},
                    )
                    MoshiChoicebox(
                        label = "Choose destinations",
                        options = listOf(options[0], options[1].copy(enabled = false, disabledReason = "Route closed.")),
                        selectedValues = setOf("water7"),
                        onValuesChange = {},
                    )
                    MoshiChoicebox(
                        label = "Choose a destination",
                        options = options,
                        selectedValue = "alabasta",
                        onValueChange = {},
                        enabled = false,
                        disabledReason = "Routes unavailable.",
                    )
                }
            }
        }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "choicebox")
        check(directory.exists() || directory.mkdirs())
        listOf(false, true).forEach { darkTheme ->
            composeRule.runOnIdle { dark.value = darkTheme }
            composeRule.waitForIdle()
            val bounds = composeRule.onNodeWithTag("choicebox-render").fetchSemanticsNode().boundsInWindow
            val bitmap = Bitmap.createBitmap(
                bounds.width.roundToInt(),
                bounds.height.roundToInt(),
                Bitmap.Config.ARGB_8888,
            )
            composeRule.runOnUiThread {
                val canvas = Canvas(bitmap)
                canvas.translate(-bounds.left, -bounds.top)
                composeRule.activity.window.decorView.draw(canvas)
            }
            val file = File(directory, if (darkTheme) "choicebox-dark.png" else "choicebox-light.png")
            file.outputStream().use { output -> check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) }
            bitmap.recycle()
        }
    }
}
