package co.mipyme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiPreview
import co.mipyme.ui.MoshiTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoshiBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = Moshi.colors
    val spacing = Moshi.spacing

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = spacing.space3, topEnd = spacing.space3),
        containerColor = colors.background.surface,
        contentColor = colors.foreground.primary,
        scrimColor = colors.overlay.scrim,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.overlay.handle) },
    ) {
        MoshiBottomSheetContent(title = title, content = content)
    }
}

@Composable
private fun MoshiBottomSheetContent(
    title: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = Moshi.spacing

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.pageGutterNarrow)
            .padding(bottom = spacing.space6),
    ) {
        if (title != null) {
            MoshiText(
                text = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { heading() },
                role = MoshiTextRole.Heading24,
                color = Moshi.colors.foreground.primary,
                textAlign = TextAlign.Start,
            )
            Spacer(Modifier.height(spacing.space4))
        }
        content()
    }
}

@MoshiPreview
@Composable
private fun MoshiBottomSheetPreview() {
    MoshiTheme {
        Column(
            modifier = Modifier
                .background(Moshi.colors.background.surface)
                .padding(top = Moshi.spacing.space4),
        ) {
            MoshiBottomSheetContent(title = "Choose a theme") {
                Column(modifier = Modifier.selectableGroup()) {
                    MoshiSelectableItem(label = "System", selected = true, onClick = {})
                    MoshiSelectableItem(label = "Light", selected = false, onClick = {})
                    MoshiSelectableItem(label = "Dark", selected = false, onClick = {})
                }
            }
        }
    }
}
