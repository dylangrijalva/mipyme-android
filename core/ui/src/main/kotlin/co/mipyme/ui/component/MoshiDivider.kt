package co.mipyme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiPreview
import co.mipyme.ui.MoshiTheme

@Composable
fun MoshiDivider(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Moshi.colors.border.default),
    )
}

@MoshiPreview
@Composable
private fun MoshiDividerPreview() {
    MoshiTheme {
        Column(
            modifier = Modifier
                .background(Moshi.colors.background.surface)
                .padding(Moshi.spacing.space4),
        ) {
            MoshiText(text = "Above")
            Spacer(Modifier.height(Moshi.spacing.space4))
            MoshiDivider()
            Spacer(Modifier.height(Moshi.spacing.space4))
            MoshiText(text = "Below")
        }
    }
}
