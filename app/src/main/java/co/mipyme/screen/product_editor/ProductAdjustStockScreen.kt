package co.mipyme.screen.product_editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.mipyme.R
import co.mipyme.preview.ScreenPreview
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.component.MoshiText
import co.mipyme.ui.component.MoshiTextRole
import co.mipyme.ui.component.MoshiToolbar
import co.mipyme.ui.component.MoshiToolbarNavigation

@Composable
internal fun ProductAdjustStockScreen(
    viewModel: ProductEditorViewModel,
    locationId: String,
    onBack: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)
    ProductAdjustStockContent(
        locationName = state.locations.firstOrNull { it.id == locationId }?.name,
        onBack = onBack,
    )
}

@Composable
private fun ProductAdjustStockContent(locationName: String? = null, onBack: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().background(Moshi.colors.background.surface)) {
        MoshiToolbar(
            title = stringResource(R.string.product_editor_adjust_stock),
            subtitle = locationName,
            navigation = MoshiToolbarNavigation.Back(onClick = onBack),
        )
        Column(
            Modifier
                .weight(1f)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(Moshi.spacing.space4),
        ) {
            MoshiText(
                text = stringResource(R.string.product_editor_adjust_stock_placeholder),
                role = MoshiTextRole.Copy16,
                color = Moshi.colors.foreground.secondary,
            )
        }
    }
}

@ScreenPreview
@Composable
private fun ProductAdjustStockPreview() {
    MoshiTheme {
        ProductAdjustStockContent()
    }
}
