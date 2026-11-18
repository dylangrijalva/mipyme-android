package co.mipyme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiPreview
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.R

@Composable
fun MoshiSection(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = Moshi.spacing.space4),
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val spacing = Moshi.spacing
    Column(modifier = modifier.fillMaxWidth().padding(contentPadding)) {
        MoshiText(
            text = title,
            modifier = Modifier.semantics { heading() },
            role = MoshiTextRole.Heading20,
        )
        if (description != null) {
            Spacer(Modifier.height(spacing.space1))
            MoshiText(
                text = description,
                role = MoshiTextRole.Copy14,
                color = Moshi.colors.foreground.secondary,
            )
        }
        if (content != null) {
            Spacer(Modifier.height(spacing.space3))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.space3),
                content = content,
            )
        }
    }
}

@MoshiPreview
@Composable
private fun MoshiSectionPreview() {
    MoshiTheme {
        MoshiSection(
            title = stringResource(R.string.moshi_section_preview_title),
            modifier = Modifier.background(Moshi.colors.background.surface),
            description = stringResource(R.string.moshi_section_preview_description),
        ) {
            MoshiTextField(
                value = "",
                onValueChange = {},
                label = stringResource(R.string.moshi_section_preview_name),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
