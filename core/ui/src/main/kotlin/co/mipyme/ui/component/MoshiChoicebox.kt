package co.mipyme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiPreview
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.R

/** [selectedContent] is shown inside the selected tile and must contain no interactive controls. */
data class MoshiChoiceboxOption(
    val value: String,
    val title: String,
    val description: String? = null,
    val enabled: Boolean = true,
    val disabledReason: String? = null,
    val selectedContent: (@Composable ColumnScope.() -> Unit)? = null,
)

/** A controlled group of mutually exclusive choices. Values must be unique within the group. */
@Composable
fun MoshiChoicebox(
    label: String,
    options: List<MoshiChoiceboxOption>,
    selectedValue: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    disabledReason: String? = null,
    orientation: Orientation = Orientation.Horizontal,
) {
    ChoiceboxGroup(
        label = label,
        options = options,
        isSelected = { it == selectedValue },
        onToggle = { value -> if (value != selectedValue) onValueChange(value) },
        multiple = false,
        modifier = modifier,
        enabled = enabled,
        disabledReason = disabledReason,
        orientation = orientation,
    )
}

/** A controlled group of additive choices. Values must be unique within the group. */
@Composable
fun MoshiChoicebox(
    label: String,
    options: List<MoshiChoiceboxOption>,
    selectedValues: Set<String>,
    onValuesChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    disabledReason: String? = null,
    orientation: Orientation = Orientation.Horizontal,
) {
    ChoiceboxGroup(
        label = label,
        options = options,
        isSelected = { it in selectedValues },
        onToggle = { value ->
            onValuesChange(if (value in selectedValues) selectedValues - value else selectedValues + value)
        },
        multiple = true,
        modifier = modifier,
        enabled = enabled,
        disabledReason = disabledReason,
        orientation = orientation,
    )
}

@Composable
private fun ChoiceboxGroup(
    label: String,
    options: List<MoshiChoiceboxOption>,
    isSelected: (String) -> Boolean,
    onToggle: (String) -> Unit,
    multiple: Boolean,
    modifier: Modifier,
    enabled: Boolean,
    disabledReason: String?,
    orientation: Orientation,
) {
    val spacing = Moshi.spacing
    val groupModifier = modifier
        .then(if (multiple) Modifier else Modifier.selectableGroup())
        .semantics {
            contentDescription = label
            isTraversalGroup = true
        }
    val tile: @Composable (MoshiChoiceboxOption, Modifier) -> Unit = { option, tileModifier ->
        key(option.value) {
            ChoiceboxTile(
                option = option,
                selected = isSelected(option.value),
                onClick = { onToggle(option.value) },
                multiple = multiple,
                enabled = enabled && option.enabled,
                disabledReason = if (!enabled) disabledReason ?: option.disabledReason else option.disabledReason,
                modifier = tileModifier,
            )
        }
    }
    if (orientation == Orientation.Horizontal) {
        Row(
            modifier = groupModifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            options.forEach { tile(it, Modifier.weight(1f)) }
        }
    } else {
        Column(
            modifier = groupModifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            options.forEach { tile(it, Modifier.fillMaxWidth()) }
        }
    }
}

@Composable
private fun ChoiceboxTile(
    option: MoshiChoiceboxOption,
    selected: Boolean,
    onClick: () -> Unit,
    multiple: Boolean,
    enabled: Boolean,
    disabledReason: String?,
    modifier: Modifier,
) {
    val colors = Moshi.colors
    val spacing = Moshi.spacing
    val shape = RoundedCornerShape(6.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val borderColor = when {
        enabled && (selected || focused) -> colors.selection.border
        enabled && hovered -> colors.border.hovered
        else -> colors.border.default
    }
    val optionBackground = when {
        enabled && selected && hovered -> colors.selection.hovered
        enabled && selected -> colors.selection.background
        enabled && hovered -> colors.background.subtle
        else -> colors.background.surface
    }
    val titleColor = when {
        !enabled -> colors.foreground.disabledStrong
        selected -> colors.selection.foreground
        else -> colors.foreground.primary
    }
    val descriptionColor = when {
        !enabled -> colors.foreground.disabledStrong
        selected -> colors.selection.foreground
        else -> colors.foreground.secondary
    }
    val selectionModifier = if (multiple) {
        Modifier.toggleable(
            value = selected,
            enabled = enabled,
            role = Role.Checkbox,
            interactionSource = interactionSource,
            indication = ripple(),
            onValueChange = { onClick() },
        )
    } else {
        Modifier.selectable(
            selected = selected,
            enabled = enabled,
            role = Role.RadioButton,
            interactionSource = interactionSource,
            indication = ripple(),
            onClick = onClick,
        )
    }

    Column(
        modifier = modifier
            .sizeIn(minWidth = spacing.space12, minHeight = spacing.space12)
            .clip(shape)
            .background(colors.background.surface)
            .border(if (enabled && focused) 2.dp else 1.dp, borderColor, shape)
            .then(selectionModifier),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .sizeIn(minHeight = spacing.space12)
                .background(optionBackground)
                .padding(spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                MoshiText(
                    text = option.title,
                    role = MoshiTextRole.Label14,
                    emphasis = MoshiTextEmphasis.Strong,
                    color = titleColor,
                )
                if (!option.description.isNullOrEmpty()) {
                    Spacer(Modifier.height(spacing.space1))
                    MoshiText(option.description, role = MoshiTextRole.Copy14, color = descriptionColor)
                }
                if (!enabled && !disabledReason.isNullOrEmpty()) {
                    Spacer(Modifier.height(spacing.space1))
                    MoshiText(disabledReason, role = MoshiTextRole.Copy14, color = descriptionColor)
                }
            }
            Spacer(Modifier.width(spacing.space6))
            ChoiceboxIndicator(selected = selected, multiple = multiple, enabled = enabled)
        }
        if (selected && option.selectedContent != null) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(borderColor))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.space3),
                content = option.selectedContent,
            )
        }
    }
}

@Composable
private fun ChoiceboxIndicator(selected: Boolean, multiple: Boolean, enabled: Boolean) {
    val colors = Moshi.colors
    val spacing = Moshi.spacing
    val shape = if (multiple) RoundedCornerShape(4.dp) else CircleShape
    val indicatorColor = when {
        !enabled && selected -> if (multiple) colors.selection.disabledFill else colors.selection.disabledDot
        selected -> colors.selection.foreground
        else -> colors.control.mutedBorder
    }
    val fill = when {
        multiple && selected -> indicatorColor
        !enabled && !selected -> colors.background.disabled
        else -> colors.background.surface
    }
    Box(
        modifier = Modifier
            .size(spacing.space4)
            .background(fill, shape)
            .border(1.dp, indicatorColor, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected && multiple) {
            Icon(
                painter = painterResource(R.drawable.ic_fluent_checkmark_24_regular),
                contentDescription = null,
                modifier = Modifier.size(spacing.space3),
                tint = colors.foreground.inverse,
            )
        } else if (selected) {
            Box(Modifier.size(spacing.space2).background(indicatorColor, CircleShape))
        }
    }
}

@MoshiPreview
@Composable
private fun MoshiChoiceboxPreview() {
    MoshiTheme {
        val spacing = Moshi.spacing
        val options = listOf(
            MoshiChoiceboxOption(
                value = "water7",
                title = stringResource(R.string.moshi_choicebox_preview_water_seven),
                description = stringResource(R.string.moshi_choicebox_preview_shipwrights),
                selectedContent = { MoshiText(stringResource(R.string.moshi_choicebox_preview_selected_content)) },
            ),
            MoshiChoiceboxOption(
                value = "alabasta",
                title = stringResource(R.string.moshi_choicebox_preview_alabasta),
                description = stringResource(R.string.moshi_choicebox_preview_desert),
            ),
        )
        val label = stringResource(R.string.moshi_choicebox_preview_group)
        val reason = stringResource(R.string.moshi_choicebox_preview_disabled_reason)
        Column(
            modifier = Modifier.background(Moshi.colors.background.surface).padding(spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space4),
        ) {
            MoshiChoicebox(label, options, selectedValue = "water7", onValueChange = {})
            MoshiChoicebox(label, options, selectedValues = setOf("alabasta"), onValuesChange = {})
            MoshiChoicebox(
                label,
                options.map { it.copy(selectedContent = null) },
                selectedValue = "water7",
                onValueChange = {},
                enabled = false,
                disabledReason = reason,
            )
            MoshiChoicebox(
                label,
                options.map { it.copy(enabled = it.value != "alabasta", disabledReason = reason, selectedContent = null) },
                selectedValues = setOf("alabasta"),
                onValuesChange = {},
                orientation = Orientation.Vertical,
            )
        }
    }
}
