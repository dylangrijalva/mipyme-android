package co.mipyme.ui.component

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MoshiSemanticColorUsageRenderTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun selectedDrawerItemUsesLightSelectedStrongBackground() {
        assertDrawerColor(darkTheme = false)
    }

    @Test
    fun selectedDrawerItemUsesDarkSelectedStrongBackground() {
        assertDrawerColor(darkTheme = true)
    }

    @Test
    fun inactiveStepUsesLightProgressTrack() {
        assertInactiveStepColor(darkTheme = false)
    }

    @Test
    fun inactiveStepUsesDarkProgressTrack() {
        assertInactiveStepColor(darkTheme = true)
    }

    private fun assertDrawerColor(darkTheme: Boolean) {
        var expectedColor = 0
        compose.setContent {
            MoshiTheme(darkTheme = darkTheme) {
                expectedColor = Moshi.colors.background.selectedStrong.toArgb()
                Column(Modifier.width(320.dp)) {
                    MoshiNavigationDrawerItem(
                        label = "Inventory",
                        icon = co.mipyme.ui.R.drawable.ic_fluent_shopping_bag_24_regular,
                        selected = true,
                        onClick = {},
                        modifier = Modifier.testTag("selected-drawer-item"),
                    )
                }
            }
        }
        val bitmap = captureTag("selected-drawer-item")
        val sampledColor = bitmap.getPixel(bitmap.width - 6, bitmap.height / 2)
        compose.runOnIdle { assertEquals(expectedColor, sampledColor) }
    }

    private fun captureTag(tag: String): Bitmap {
        val bounds = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInWindow
        val bitmap = Bitmap.createBitmap(bounds.width.roundToInt(), bounds.height.roundToInt(), Bitmap.Config.ARGB_8888)
        compose.runOnUiThread {
            val canvas = Canvas(bitmap)
            canvas.translate(-bounds.left, -bounds.top)
            compose.activity.window.decorView.draw(canvas)
        }
        return bitmap
    }

    private fun assertInactiveStepColor(darkTheme: Boolean) {
        var expectedColor = 0
        compose.setContent {
            MoshiTheme(darkTheme = darkTheme) {
                expectedColor = Moshi.colors.progress.track.toArgb()
                MoshiStepProgressIndicator(
                    stepCount = 2,
                    currentStep = 0,
                    modifier = Modifier.width(320.dp).testTag("step-progress"),
                )
            }
        }
        val bitmap = captureTag("step-progress")
        val sampledColor = bitmap.getPixel(bitmap.width * 3 / 4, bitmap.height / 2)
        compose.runOnIdle { assertEquals(expectedColor, sampledColor) }
    }
}
