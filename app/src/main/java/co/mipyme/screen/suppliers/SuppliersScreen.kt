package co.mipyme.screen.suppliers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.mipyme.R
import co.mipyme.preview.ScreenPreview
import co.mipyme.screen.home.LocalHomeDrawer
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.component.MoshiToolbar
import co.mipyme.ui.component.MoshiToolbarNavigation
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf

fun Module.suppliersScreenModule(): Module {
    viewModelOf(::SuppliersViewModel)
    return this
}

@Composable
internal fun SuppliersScreen(viewModel: SuppliersViewModel = koinViewModel()) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    SuppliersContent(state = state)
}

@Composable
private fun SuppliersContent(state: SuppliersScreenUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Moshi.colors.background.surface),
    ) {
        MoshiToolbar(
            title = stringResource(R.string.home_destination_suppliers),
            navigation = MoshiToolbarNavigation.Menu(onClick = LocalHomeDrawer.current::open),
        )
    }
}

@ScreenPreview
@Composable
fun SuppliersScreenPreview() {
    MoshiTheme {
        SuppliersContent(state = SuppliersScreenUiState)
    }
}
