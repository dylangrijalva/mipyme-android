package co.mipyme.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiPreview
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.R

@Composable
fun MoshiLoadingIndicator(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.moshi_button_loading)
    CircularProgressIndicator(
        modifier = modifier.size(Moshi.spacing.space8).semantics { contentDescription = description },
        color = Moshi.colors.progress.foreground,
        trackColor = Moshi.colors.progress.subtleTrack,
    )
}

@MoshiPreview
@Composable
private fun MoshiLoadingIndicatorPreview() {
    MoshiTheme {
        MoshiLoadingIndicator()
    }
}
