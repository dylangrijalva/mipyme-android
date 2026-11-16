package co.mipyme.screen.transactions

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

fun Module.transactionsScreenModule(): Module {
    viewModelOf(::TransactionsViewModel)
    return this
}

@Composable
internal fun TransactionsScreen(viewModel: TransactionsViewModel = koinViewModel()) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    TransactionsContent(state = state)
}

@Composable
private fun TransactionsContent(state: TransactionsScreenUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Moshi.colors.background100),
    ) {
        MoshiToolbar(
            title = stringResource(R.string.home_destination_transactions),
            navigation = MoshiToolbarNavigation.Menu(onClick = LocalHomeDrawer.current::open),
        )
    }
}

@ScreenPreview
@Composable
fun TransactionsScreenPreview() {
    MoshiTheme {
        TransactionsContent(state = TransactionsScreenUiState)
    }
}
