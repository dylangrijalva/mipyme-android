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

### MoshiModal

Use for a focused message or decision that interrupts the current screen. Based on the
Android Dialog variants on the Native Components Modals page, it has a centered title,
optional supporting text, an optional 80 dp icon slot, and one or two stacked full-width actions.
The elevated surface has a 12 dp corner radius, a maximum width of 296 dp, and 24 dp side
and bottom insets. Top padding is 32 dp without an icon and 16 dp with one. Text uses
Subheading and Body roles, with primary and secondary foreground colors. Content scrolls
when the available height is limited, and text and buttons grow with the font size.

Pass `MoshiModalAction` values for the required primary action and optional secondary action.
Each action has its own callback, enabled state, and loading state. The primary action uses
`MoshiButtonVariant.Primary` by default; use `primaryActionVariant` for a destructive action.
The secondary action uses `Outline`. Actions have a 16 dp gap and sit 32 dp below the text.
The caller owns visibility and must hide the modal in response to the appropriate callbacks;
clicking an action does not automatically dismiss it.

`onDismissRequest` handles Back and outside taps through the native Android dialog.
Pass `DialogProperties` to adjust dismissal behavior, including during a pending operation.
The title is exposed as both a pane title and a heading. Keep icons decorative when the text
already explains the message. The icon slot supplies layout only; the caller provides its
artwork and any background using Moshi colors. All content must be inside `MoshiTheme`.

### MoshiButton

Use `Primary` for a prominent labeled action with the Moshi primary action fill.
Use `Outline` for a secondary action that needs a visible boundary and `Text`
for a quieter action. Both use a transparent resting background and secondary
action colors, with a 1 dp border on `Outline`. Use `Destructive` for the
prominent, confirming action of an irreversible operation, such as deleting data;
it uses the destructive action fill with the on-destructive label color and the
same disabled and loading behavior as `Primary`. Use `DestructiveOutline` for a
lower-emphasis action that signs the user out or removes data. It keeps the transparent resting
background and secondary hover of `Outline` but uses the destructive action
foreground and the error status border. Disabled and loading states fall back to
the shared disabled colors and default border.

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

### MoshiCheckbox

Use for a single opt-in choice, such as accepting terms. The control is a 20 dp
circle inside a 48 × 48 dp touch target. Checked uses `background.action.primary.default`
with `foreground.onAction.primary` for the check; unchecked uses a `border.interactive`
outline; disabled uses `background.disabled` and `border.default`.

`MoshiCheckbox` renders only the box and exposes `Role.Checkbox`. Place the label beside it
in a `Row` so the label can contain its own links.

### MoshiPageIndicator

Use below a paged surface to show position. The current page is a 24 × 8 dp pill in
`foreground.primary`; other pages are 8 dp dots in `border.strong`. The indicator is a single
accessibility node announcing "Page x of y". It is not interactive.

### MoshiStepProgressIndicator

Use in a multi-step workflow to show the current and completed steps. Each step is an equal-width,
rounded segment with `spacing.space1` height and gaps. Current and completed steps use
`foreground.primary`; upcoming steps use `border.default`. The indicator is a single accessibility
node announcing "Step x of y". `currentStep` is zero-based, and nonpositive `stepCount` renders no
indicator.

### MoshiText with `AnnotatedString`

An `AnnotatedString` overload of `MoshiText` supports inline styled spans and links.
Style links with `foreground.link` and an underline.

### MoshiTextField

Use for a single piece of free-form text, such as a name. The field is a 48 dp minimum height
container on `background.surface` with a 12 dp corner radius and `typography.body` text.
The border is 1 dp `border.interactive` at rest, 2 dp `border.focus` while focused, and
`border.status.error` when `isError` is set. Disabled fields use `background.disabled`,
`border.default`, and `foreground.disabled`.

The optional label sits above the field in `typography.small`, and the placeholder uses
`foreground.tertiary`. Optional supporting text sits below in `typography.caption` with
`foreground.secondary`, or `foreground.status.error` when `isError` is set. The label and supporting text are
rendered inside the field, so they merge into its accessibility node and are announced with the
typed text or placeholder, and the supporting text is also exposed as the error message while
`isError` is set. Use sentence case for labels and pass `KeyboardOptions` and `KeyboardActions`
for the keyboard type and IME action. The field is single line unless `singleLine` is `false`.

### MoshiCardSelector

Use for a single choice presented as a card, such as the business type during onboarding.
Based on [Mobile Card Selector V02](https://www.figma.com/design/XIRCkRjiI126cvhOjZXn4r/Business-Native?node-id=11131-274057),
it has an 8 dp corner radius, a 64 dp minimum height, a body label, optional Small supporting
text, and a trailing radio indicator. Labels and supporting text wrap as needed. The onboarding
business type grid gives every card the height of its tallest option. The card uses
`background.surface` in both selection states, `border.default` when unselected, and
`foreground.primary` for its selected border and indicator. Disabled cards use
`background.disabled`, `border.default`, and `foreground.disabled`.

The whole card exposes `Role.RadioButton`; the indicator has no separate click action.
Place related cards inside `Modifier.selectableGroup()` and keep selection in the caller's state.
Use Moshi spacing tokens for the 16 dp leading inset and content gap, 4 dp trailing inset,
and 12 dp vertical text insets. Typography and colors follow Moshi in both themes.

### MoshiRadioOption

Use for one choice in a list where exactly one option applies, such as a business type. The
option is a full-width row with the label first and a 20 dp selected indicator last, with a 64 dp
minimum height and a 12 dp corner radius. This keeps cards aligned while allowing them to grow when
labels wrap. Unselected options use `background.surface` and
`border.default`. The selected option uses `background.selected`, a `border.interactive` border,
and a `background.action.primary.default` outline and dot in the indicator. Disabled options use
`background.disabled`, `border.default`, and `foreground.disabled`.

Pass `leadingContent` to show a visual before the label, such as a country flag. It is placed
`space3` before the label and should stay decorative, because the label already names the option.

`MoshiRadioOption` exposes `Role.RadioButton` and is selectable as a whole row. Place the options
in a container with `Modifier.selectableGroup()` so assistive technology announces them as one
group. Space the options with `Spacer` or `Arrangement.spacedBy`.
