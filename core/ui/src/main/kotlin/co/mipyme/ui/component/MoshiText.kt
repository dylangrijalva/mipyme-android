package co.mipyme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import co.mipyme.ui.MoshiPreview
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.foundation.MoshiColors

enum class MoshiTextRole {
    Caption,
    Small,
    Body,
    Subheading,
    Heading,
    Display,
}

/** Displays plain text using a typography role from the enclosing [MoshiTheme]. */
@Composable
fun MoshiText(
    text: String,
    modifier: Modifier = Modifier,
    role: MoshiTextRole = MoshiTextRole.Body,
    color: Color = MoshiColors.TextPrimary,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val typography = MoshiTheme.typography
    val style = when (role) {
        MoshiTextRole.Caption -> typography.caption
        MoshiTextRole.Small -> typography.small
        MoshiTextRole.Body -> typography.body
        MoshiTextRole.Subheading -> typography.subheading
        MoshiTextRole.Heading -> typography.heading
        MoshiTextRole.Display -> typography.display
    }

    Text(
        text = text,
        modifier = modifier,
        color = color,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
        style = style,
    )
}

@MoshiPreview
@Composable
private fun MoshiTextCaptionPreview() {
    MoshiTextRolePreview(MoshiTextRole.Caption)
}

@MoshiPreview
@Composable
private fun MoshiTextSmallPreview() {
    MoshiTextRolePreview(MoshiTextRole.Small)
}

@MoshiPreview
@Composable
private fun MoshiTextBodyPreview() {
    MoshiTextRolePreview(MoshiTextRole.Body)
}

@MoshiPreview
@Composable
private fun MoshiTextSubheadingPreview() {
    MoshiTextRolePreview(MoshiTextRole.Subheading)
}

@MoshiPreview
@Composable
private fun MoshiTextHeadingPreview() {
    MoshiTextRolePreview(MoshiTextRole.Heading)
}

@MoshiPreview
@Composable
private fun MoshiTextDisplayPreview() {
    MoshiTextRolePreview(MoshiTextRole.Display)
}

@Composable
private fun MoshiTextRolePreview(role: MoshiTextRole) {
    MoshiTheme {
        Box(
            modifier = Modifier
                .background(MoshiColors.background.canvas)
                .padding(MoshiTheme.spacing.space4),
        ) {
            MoshiText(text = role.name, role = role)
        }
    }
}
