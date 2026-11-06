package co.mipyme.ui.component

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import co.mipyme.ui.MoshiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MoshiButtonTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun activationRequiresEnabledAndIdle() {
        val enabled = mutableStateOf(true)
        val loading = mutableStateOf(false)
        var clicks = 0
        composeRule.setContent {
            MoshiTheme {
                MoshiButton(
                    onClick = { clicks++ },
                    modifier = Modifier.testTag("saveButton"),
                    variant = MoshiButtonVariant.Primary,
                    size = MoshiButtonSize.Default,
                    enabled = enabled.value,
                    isLoading = loading.value,
                ) {
                    MoshiText("Save")
                }
            }
        }

        val button = composeRule.onNodeWithTag("saveButton")
        button.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        button.assertIsEnabled().performTouchInput { click() }

        composeRule.runOnIdle { enabled.value = false }
        button.assertIsNotEnabled()
        button.assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ProgressBarRangeInfo))
        button.performTouchInput { click() }

        composeRule.runOnIdle {
            enabled.value = true
            loading.value = true
        }
        button.assertIsNotEnabled()
        button.assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo.Indeterminate,
            ),
        )
        button.performTouchInput { click() }

        composeRule.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun loadingHidesContentAndPreservesSize() {
        val loading = mutableStateOf(false)
        composeRule.setContent {
            MoshiTheme(darkTheme = true) {
                MoshiButton(
                    onClick = {},
                    modifier = Modifier.testTag("saveButton"),
                    enabled = false,
                    isLoading = loading.value,
                    leadingContent = { MoshiText("+") },
                    trailingContent = { MoshiText("→") },
                ) {
                    MoshiText("Save")
                }
            }
        }

        val button = composeRule.onNodeWithTag("saveButton")
        val idleSize = button.fetchSemanticsNode().size
        val leading = composeRule.onNodeWithText("+", useUnmergedTree = true)
        val trailing = composeRule.onNodeWithText("→", useUnmergedTree = true)
        val label = composeRule.onNodeWithText("Save", useUnmergedTree = true)
        leading.assertExists()
        trailing.assertExists()
        label.assertExists()

        composeRule.runOnIdle { loading.value = true }

        assertEquals(idleSize, button.fetchSemanticsNode().size)
        button.assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))

        composeRule.runOnIdle { loading.value = false }

        leading.assertExists()
        trailing.assertExists()
        label.assertExists()
    }
}
