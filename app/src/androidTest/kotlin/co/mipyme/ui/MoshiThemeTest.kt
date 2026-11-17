package co.mipyme.ui

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

class MoshiThemeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun lightThemePreservesActivityAndOverridesResources() {
        assertTheme(darkTheme = false)
    }

    @Test
    fun darkThemePreservesActivityAndOverridesResources() {
        assertTheme(darkTheme = true)
    }

    private fun assertTheme(darkTheme: Boolean) {
        val expectedMode = if (darkTheme) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        composeRule.setContent {
            MoshiTheme(darkTheme = darkTheme) {
                rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
                assertSame(composeRule.activity, LocalActivityResultRegistryOwner.current)
                assertEquals(expectedMode, LocalConfiguration.current.uiMode and Configuration.UI_MODE_NIGHT_MASK)
                assertEquals(expectedMode, LocalContext.current.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
                assertEquals(expectedMode, LocalResources.current.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
            }
        }
        composeRule.waitForIdle()
    }
}
