package co.mipyme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiPreview
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.R

data class MoshiEmptyStateAction(
    val label: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
)

@Composable
fun MoshiEmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    primaryAction: MoshiEmptyStateAction? = null,
    secondaryAction: MoshiEmptyStateAction? = null,
) {
    val spacing = Moshi.spacing
    Column(
        modifier = modifier.padding(spacing.space6),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CompositionLocalProvider(LocalContentColor provides Moshi.colors.foreground.primary) {
            Box(Modifier.size(spacing.space6), contentAlignment = Alignment.Center) {
                icon()
            }
        }
        Spacer(Modifier.height(spacing.space4))
        MoshiText(
            text = title,
            modifier = Modifier.fillMaxWidth().semantics { heading() },
            role = MoshiTextRole.Heading20,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(spacing.space1))
        MoshiText(
            text = description,
            modifier = Modifier.fillMaxWidth(),
            color = Moshi.colors.foreground.secondary,
            textAlign = TextAlign.Center,
        )
        if (primaryAction != null || secondaryAction != null) {
            Spacer(Modifier.height(spacing.space6))
        }
        if (primaryAction != null) {
            MoshiButton(
                onClick = primaryAction.onClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = primaryAction.enabled,
                variant = MoshiButtonVariant.Outline,
            ) {
                MoshiText(primaryAction.label, textAlign = TextAlign.Center)
            }
        }
        if (primaryAction != null && secondaryAction != null) {
            Spacer(Modifier.height(spacing.space2))
        }
        if (secondaryAction != null) {
            MoshiButton(
                onClick = secondaryAction.onClick,
                enabled = secondaryAction.enabled,
                variant = MoshiButtonVariant.Text,
            ) {
                MoshiText(secondaryAction.label, textAlign = TextAlign.Center)
            }
        }
    }
}

@MoshiPreview
@Composable
private fun MoshiEmptyStatePreview() {
    MoshiTheme {
        MoshiEmptyState(
            title = stringResource(R.string.moshi_empty_preview_title),
            description = stringResource(R.string.moshi_empty_preview_description),
            modifier = Modifier.background(Moshi.colors.background.surface),
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_fluent_search_24_regular),
                    contentDescription = null,
                    modifier = Modifier.size(Moshi.spacing.space6),
                )
            },
            primaryAction = MoshiEmptyStateAction(stringResource(R.string.moshi_search_clear), {}),
        )
    }
}
