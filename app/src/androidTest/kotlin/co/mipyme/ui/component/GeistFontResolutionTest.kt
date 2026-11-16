package co.mipyme.ui.component

import android.graphics.Typeface
import android.os.Build
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.test.platform.app.InstrumentationRegistry
import co.mipyme.ui.foundation.MoshiTypography
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

class GeistFontResolutionTest {
    @Test
    fun sansAndMonoResolveExact550WithoutSynthesis() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = createFontFamilyResolver(context)
        val typography = MoshiTypography()
        for (family in listOf(typography.copy16.fontFamily, typography.copy14Mono.fontFamily)) {
            val typeface = resolver.resolve(family, FontWeight(550), fontSynthesis = FontSynthesis.None).value as Typeface
            assertEquals(550, typeface.weight)
        }
    }
}
