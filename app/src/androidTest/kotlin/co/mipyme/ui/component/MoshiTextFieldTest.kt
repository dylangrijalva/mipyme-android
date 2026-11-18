package co.mipyme.ui.component

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MoshiTextFieldTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun trailingActionPreservesFieldHeight() {
        composeRule.setContent {
            MoshiTheme {
                Column(Modifier.width(320.dp)) {
                    val label = stringResource(R.string.moshi_search_clear)
                    MoshiTextField(
                        value = "",
                        onValueChange = {},
                        label = label,
                        modifier = Modifier.testTag("plain"),
                    )
                    MoshiTextField(
                        value = "",
                        onValueChange = {},
                        label = label,
                        modifier = Modifier.testTag("action"),
                        trailingAction = MoshiTextFieldTrailingAction(
                            icon = R.drawable.ic_fluent_more_vertical_24_regular,
                            contentDescription = label,
                            onClick = {},
                        ),
                    )
                }
            }
        }

        assertEquals(
            composeRule.onNodeWithTag("plain").fetchSemanticsNode().boundsInRoot.height,
            composeRule.onNodeWithTag("action").fetchSemanticsNode().boundsInRoot.height,
        )
    }

    @Test
    fun productEditorNameAndBarcodeHaveEqualHeights() {
        composeRule.setContent {
            MoshiTheme {
                LazyColumn(Modifier.width(320.dp)) {
                    item {
                        MoshiTextField(
                            value = "",
                            onValueChange = {},
                            label = stringResource(co.mipyme.R.string.product_editor_product_name),
                            modifier = Modifier.testTag("name"),
                        )
                    }
                    item {
                        MoshiTextField(
                            value = "",
                            onValueChange = {},
                            label = stringResource(co.mipyme.R.string.product_editor_barcode),
                            modifier = Modifier.testTag("barcode"),
                            trailingAction = MoshiTextFieldTrailingAction(
                                icon = co.mipyme.R.drawable.ic_fluent_barcode_scanner_24_regular,
                                contentDescription = stringResource(co.mipyme.R.string.inventory_scan),
                                onClick = {},
                            ),
                        )
                    }
                }
            }
        }

        assertEquals(
            composeRule.onNodeWithTag("name").fetchSemanticsNode().boundsInRoot.height,
            composeRule.onNodeWithTag("barcode").fetchSemanticsNode().boundsInRoot.height,
        )
    }
}
