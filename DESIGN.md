---
name: Moshi UI for Android
components:
  button:
    variants:
      Primary:
        container: background.action.primary.default
        content: foreground.onAction.primary
        hoveredContainer: background.action.primary.hover
      Outline:
        container: transparent
        content: foreground.action.secondary
        hoveredContainer: background.action.secondary.hover
        border: border.interactive
        disabledBorder: border.default
      Text:
        container: transparent
        content: foreground.action.secondary
        hoveredContainer: background.action.secondary.hover
    size: Default
    disabledContainer: background.disabled
    disabledContent: foreground.disabled
    radius: radius.lg
    label: typography.small
    minWidth: spacing.space-12
    minHeight: spacing.space-12
    horizontalPadding: spacing.space-4
    verticalPadding: spacing.space-2
    contentGap: spacing.space-2
    leadingSlotSize: spacing.space-6
    spinnerSize: spacing.space-4
---

# Moshi UI for Android

Use the semantic colors, native typography, spacing, and shape tokens from the
[Moshi design spec](https://github.com/dylangrijalva/moshi-ui/blob/main/DESIGN.md).
The Android token implementations and component code define the behavior shipped in this app.

## Components

### MoshiButton

Use `Primary` for a prominent labeled action with the Moshi primary action fill.
Use `Outline` for a secondary action that needs a visible boundary and `Text`
for a quieter action. Both use a transparent resting background and secondary
action colors, with a 1 dp border on `Outline`.

`Default` has a minimum visible size
and touch target of 48 × 48 dp. The button grows with the label as needed. Use
sentence case for labels and let text wrap on narrow layouts or at larger font sizes.

The required content slot provides the accessible label. `MoshiText` inherits
the button's foreground and Small role unless the caller supplies an override.
Optional leading and trailing content follows the layout direction. Treat icons
as decorative when the label already names the action.

Activation requires `enabled && !isLoading`. Both disabled and loading buttons
use the disabled colors in either theme. During loading, the button keeps the
width and height of its normal content and shows only a centered indeterminate
spinner. Hide the label and side content from accessibility services while
exposing progress through the button's accessibility semantics.

Use native button interaction and ripple feedback.
