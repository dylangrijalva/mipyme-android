package co.mipyme.screen.reports

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

fun Module.reportsScreenModule(): Module {
    viewModelOf(::ReportsViewModel)
    return this
}

@Composable
internal fun ReportsScreen(viewModel: ReportsViewModel = koinViewModel()) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    ReportsContent(state = state)
}

@Composable
private fun ReportsContent(state: ReportsScreenUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Moshi.colors.background.surface),
    ) {
        MoshiToolbar(
            title = stringResource(R.string.home_destination_reports),
            navigation = MoshiToolbarNavigation.Menu(onClick = LocalHomeDrawer.current::open),
        )
    }
}

@ScreenPreview
@Composable
fun ReportsScreenPreview() {
    MoshiTheme {
        ReportsContent(state = ReportsScreenUiState)
    }
}
