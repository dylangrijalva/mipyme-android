package co.mipyme

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.component.MoshiButton
import co.mipyme.ui.component.MoshiButtonVariant
import co.mipyme.ui.component.MoshiText
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
internal fun PlaceholderScreen() {
    MoshiTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Moshi.colors.background.canvas)
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(Moshi.spacing.space4),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                Moshi.spacing.space4,
                Alignment.CenterVertically,
            ),
        ) {
            MoshiText(
                text = stringResource(R.string.app_name),
                textAlign = TextAlign.Center,
            )
            for (variant in MoshiButtonVariant.entries) {
                val label = when (variant) {
                    MoshiButtonVariant.Primary -> R.string.placeholder_button_primary
                    MoshiButtonVariant.Outline -> R.string.placeholder_button_outline
                    MoshiButtonVariant.Text -> R.string.placeholder_button_text
                }
                PlaceholderButton(variant, stringResource(label))
            }
            MoshiButton(onClick = {}, enabled = false) {
                MoshiText(stringResource(R.string.placeholder_button_disabled))
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Moshi.spacing.space2),
            ) {
                val loadingLabel = stringResource(R.string.placeholder_button_loading)
                MoshiText(loadingLabel)
                MoshiButton(onClick = {}, isLoading = true) {
                    MoshiText(loadingLabel)
                }
            }
        }
    }
}

@Composable
private fun PlaceholderButton(variant: MoshiButtonVariant, label: String) {
    var isLoading by remember { mutableStateOf(false) }
    LaunchedEffect(isLoading) {
        if (isLoading) {
            delay(1.seconds)
            isLoading = false
        }
    }

    MoshiButton(
        onClick = { isLoading = true },
        variant = variant,
        isLoading = isLoading,
    ) {
        MoshiText(label)
    }
}

@Preview(name = "Light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PlaceholderScreenPreview() {
    PlaceholderScreen()
}
