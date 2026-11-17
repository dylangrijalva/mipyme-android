package co.mipyme.ui.component

import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Bitmap
import android.graphics.Typeface
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import co.mipyme.R
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiTheme
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GeistRenderTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun lightDefault() { capture(false, 1f) }

    @Test
    fun darkDefault() { capture(true, 1f) }

    @Test
    fun lightLarge() { capture(false, 2f) }

    @Test
    fun darkLarge() { capture(true, 2f) }

    private fun capture(dark: Boolean, scale: Float) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resources = context.createConfigurationContext(
            Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag("es")) },
        ).resources
        val title = resources.getString(R.string.verify_otp_code_title)
        val copy = resources.getString(R.string.verify_otp_code_guidance)
        val action = resources.getString(R.string.sign_in_with_email_continue)
        val secondary = resources.getString(R.string.verify_otp_code_go_back)
        val search = resources.getString(R.string.inventory_search_products)
        var authoredSrgb = false
        var sansTypeface: Typeface? = null
        var monoTypeface: Typeface? = null
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                MoshiTheme(darkTheme = dark) {
                    authoredSrgb = Moshi.colors.selection.foreground.colorSpace == ColorSpaces.Srgb
                    val resolver = LocalFontFamilyResolver.current
                    sansTypeface = resolver.resolve(Moshi.typography.copy16.fontFamily, FontWeight(550)).value as Typeface
                    monoTypeface = resolver.resolve(Moshi.typography.copy14Mono.fontFamily, FontWeight(550)).value as Typeface
                    Column(
                        modifier = Modifier
                            .testTag("geist-render")
                            .background(Moshi.colors.background.surface)
                            .verticalScroll(rememberScrollState())
                            .padding(Moshi.spacing.space4),
                        verticalArrangement = Arrangement.spacedBy(Moshi.spacing.space3),
                    ) {
                        MoshiText(title, role = MoshiTextRole.Heading24)
                        MoshiText(
                            buildAnnotatedString { withMoshiEmphasis { append(copy) } },
                            role = MoshiTextRole.Copy16,
                        )
                        MoshiText("012345", role = MoshiTextRole.Copy14Mono, emphasis = MoshiTextEmphasis.Strong)
                        MoshiSearchField("", {}, search, modifier = Modifier.fillMaxWidth())
                        MoshiButton({}, modifier = Modifier.testTag("primary").fillMaxWidth()) { MoshiText(action) }
                        MoshiButton({}, variant = MoshiButtonVariant.Secondary, modifier = Modifier.fillMaxWidth()) {
                            MoshiText(secondary)
                        }
                        MoshiButton({}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                            MoshiText(action, emphasis = MoshiTextEmphasis.Default)
                        }
                        MoshiText(title, role = MoshiTextRole.Heading24, emphasis = MoshiTextEmphasis.Subtle)
                    }
                }
            }
        }
        compose.onNodeWithTag("primary").assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
        compose.waitForIdle()
        compose.runOnIdle { assertTrue(authoredSrgb) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            compose.runOnIdle {
                assertEquals(550, checkNotNull(sansTypeface).weight)
                assertEquals(550, checkNotNull(monoTypeface).weight)
            }
        }
        val render = compose.onNodeWithTag("geist-render")
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            render.captureToImage().asAndroidBitmap()
        } else {
            val bounds = render.fetchSemanticsNode().boundsInWindow
            val image = Bitmap.createBitmap(bounds.width.roundToInt(), bounds.height.roundToInt(), Bitmap.Config.ARGB_8888)
            compose.runOnUiThread {
                val canvas = Canvas(image)
                canvas.translate(-bounds.left, -bounds.top)
                compose.activity.window.decorView.draw(canvas)
            }
            image
        }
        val directory = File(context.getExternalFilesDir(null), "geist").apply { mkdirs() }
        val locale = resources.configuration.locales[0].language
        val file = File(directory, "geist-${if (dark) "dark" else "light"}-$scale-$locale.png")
        file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        assertTrue(file.length() > 0)
    }
}
