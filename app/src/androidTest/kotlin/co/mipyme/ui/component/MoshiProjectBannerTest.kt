package co.mipyme.ui.component

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.R
import java.io.File
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class MoshiProjectBannerTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun actionRemainsAvailableUntilTheCallerResolvesTheState() {
        var actions = 0
        val message = mutableStateOf("Review access to Water 7.")
        composeRule.setContent {
            MoshiTheme {
                MoshiProjectBanner(
                    message = message.value,
                    actionLabel = "Review access",
                    onAction = { actions++ },
                    modifier = Modifier.testTag("banner"),
                    icon = painterResource(R.drawable.ic_fluent_shield_checkmark_24_regular),
                )
            }
        }

        composeRule.onNodeWithTag("banner")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Dismiss))
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(1)
        composeRule.onNodeWithText("Review access")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.runOnIdle {
            assertEquals(1, actions)
            message.value = "Nami is reviewing access to Water 7."
        }
        composeRule.onNodeWithText("Nami is reviewing access to Water 7.").assertIsDisplayed()
        composeRule.onNodeWithText("Review access").performClick()
        composeRule.runOnIdle { assertEquals(2, actions) }
    }

    @Test
    fun longMessageAndActionWrapAtLargeFontScaleWithoutClipping() {
        val message = "Payment failed for Water 7. Update the payment method to keep the project active."
        val action = "Update Water 7 payment method"
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                MoshiTheme(darkTheme = true) {
                    Column(Modifier.width(240.dp)) {
                        MoshiProjectBanner(
                            message = message,
                            actionLabel = action,
                            onAction = {},
                            variant = MoshiProjectBannerVariant.Error,
                            icon = painterResource(R.drawable.ic_fluent_warning_24_regular),
                        )
                    }
                }
            }
        }

        listOf(message, action).forEach { text ->
            composeRule.onNodeWithText(text, useUnmergedTree = true)
                .assertIsDisplayed()
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { getLayout ->
                    val results = mutableListOf<TextLayoutResult>()
                    getLayout(results)
                    assertFalse(results.single().hasVisualOverflow)
                }
        }
        composeRule.onNodeWithText(action).assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
        val messageBounds = composeRule.onNodeWithText(message).fetchSemanticsNode().boundsInRoot
        val actionBounds = composeRule.onNodeWithText(action).fetchSemanticsNode().boundsInRoot
        assertTrue(actionBounds.top >= messageBounds.bottom)
    }

    @Test
    fun wideBannerCentersItsMessageAndActionOnOneRow() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MoshiTheme {
                    Column(Modifier.width(800.dp)) {
                        MoshiProjectBanner("Nami rolled back Water 7.", "Undo rollback", {})
                    }
                }
            }
        }
        val message = composeRule.onNodeWithText("Nami rolled back Water 7.").fetchSemanticsNode().boundsInRoot
        val action = composeRule.onNodeWithText("Undo rollback").fetchSemanticsNode().boundsInRoot
        assertEquals(message.center.y, action.center.y, 1f)
        assertTrue(action.left > message.right)
    }

    @Test
    fun iconIsOptionalAndDoesNotCreateAnotherAction() {
        composeRule.setContent {
            MoshiTheme {
                MoshiProjectBanner("Nami rolled back Water 7.", "Undo rollback", {}, variant = MoshiProjectBannerVariant.Warning)
            }
        }
        composeRule.onNodeWithText("Nami rolled back Water 7.").assertIsDisplayed()
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(1)
        composeRule.onNodeWithText("Undo rollback").performClick()
    }

    @Test
    fun capturesEveryVariantInLightAndDarkThemes() {
        val dark = mutableStateOf(false)
        composeRule.setContent {
            MoshiTheme(darkTheme = dark.value) {
                Column(
                    Modifier.testTag("project-banners").width(360.dp).background(Moshi.colors.background.surface),
                ) {
                    MoshiProjectBannerVariant.entries.forEach { variant ->
                        MoshiProjectBanner(
                            message = when (variant) {
                                MoshiProjectBannerVariant.Default -> "Review access to Water 7."
                                MoshiProjectBannerVariant.Success -> "Extra protection is enabled for Water 7."
                                MoshiProjectBannerVariant.Warning -> "Nami rolled back Water 7."
                                MoshiProjectBannerVariant.Error -> "Payment failed for Water 7. Update your payment method."
                            },
                            actionLabel = when (variant) {
                                MoshiProjectBannerVariant.Default -> "Review access"
                                MoshiProjectBannerVariant.Success -> "Disable protection"
                                MoshiProjectBannerVariant.Warning -> "Undo rollback"
                                MoshiProjectBannerVariant.Error -> "Update payment method"
                            },
                            onAction = {},
                            variant = variant,
                            icon = painterResource(
                                when (variant) {
                                    MoshiProjectBannerVariant.Default, MoshiProjectBannerVariant.Success ->
                                        R.drawable.ic_fluent_shield_checkmark_24_regular
                                    MoshiProjectBannerVariant.Warning -> R.drawable.ic_fluent_arrow_counterclockwise_24_regular
                                    MoshiProjectBannerVariant.Error -> R.drawable.ic_fluent_warning_24_regular
                                },
                            ),
                        )
                        Spacer(Modifier.height(Moshi.spacing.space4))
                    }
                }
            }
        }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "project-banner")
        check(directory.exists() || directory.mkdirs())
        listOf(false, true).forEach { darkTheme ->
            composeRule.runOnIdle { dark.value = darkTheme }
            composeRule.waitForIdle()
            val bounds = composeRule.onNodeWithTag("project-banners").fetchSemanticsNode().boundsInWindow
            val bitmap = Bitmap.createBitmap(bounds.width.roundToInt(), bounds.height.roundToInt(), Bitmap.Config.ARGB_8888)
            composeRule.runOnUiThread {
                val canvas = Canvas(bitmap)
                canvas.translate(-bounds.left, -bounds.top)
                composeRule.activity.window.decorView.draw(canvas)
            }
            val file = File(directory, if (darkTheme) "project-banner-dark.png" else "project-banner-light.png")
            file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            bitmap.recycle()
        }
    }
}
