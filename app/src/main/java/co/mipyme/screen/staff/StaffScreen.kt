package co.mipyme.screen.staff

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

fun Module.staffScreenModule(): Module {
    viewModelOf(::StaffViewModel)
    return this
}

@Composable
internal fun StaffScreen(viewModel: StaffViewModel = koinViewModel()) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    StaffContent(state = state)
}

@Composable
private fun StaffContent(state: StaffScreenUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Moshi.colors.background.canvas),
    ) {
        MoshiToolbar(
            title = stringResource(R.string.home_destination_staff),
            navigation = MoshiToolbarNavigation.Menu(onClick = LocalHomeDrawer.current::open),
        )
    }
}

@ScreenPreview
@Composable
fun StaffScreenPreview() {
    MoshiTheme {
        StaffContent(state = StaffScreenUiState)
    }
}
