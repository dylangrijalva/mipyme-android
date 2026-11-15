---
name: Moshi UI for Android
components:
  button:
    variants:
      Primary:
        container: background.action.primary.default
        content: foreground.onAction.primary
        hoveredContainer: background.action.primary.hover
      Secondary:
        container: background.action.secondary.default
        content: foreground.primary
        hoveredContainer: background.action.secondary.hover
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

Use for a focused message or decision that interrupts the current screen. It has a left-aligned
title, optional supporting text, an optional 80 dp icon slot, and up to three actions. The elevated
surface has a 12 dp corner radius and a maximum width of 720 dp. It uses the active Moshi theme,
so the charcoal surface appears in dark theme and the light theme keeps its light elevated surface.
Side insets are 12 dp, the bottom inset is 24 dp, and top padding is 24 dp without an icon and
16 dp with one. Text
uses Subheading and Body roles, with primary and secondary foreground colors. Two actions sit side
by side when the content is at least 260 dp wide. Three actions use a horizontal row at 480 dp or
wider, with the optional tertiary text action at the start and secondary and primary actions at the
end. At narrower widths, actions stack full-width in primary, secondary, tertiary order. Content
scrolls when the available height is limited, and text and buttons grow with the font size.

Pass `MoshiModalAction` values for the required primary action and optional secondary and tertiary
actions. Each action has its own callback, enabled state, and loading state. The primary action uses
`MoshiButtonVariant.Primary` by default; use `primaryActionVariant` for a destructive action. The
secondary action uses `Outline`, and the tertiary action uses `Text`. Actions sit 32 dp below the
text and use 8 dp gaps. The caller owns visibility and must hide the modal in response to the
appropriate callbacks; clicking an action does not automatically dismiss it.

`onDismissRequest` handles Back and outside taps through the native Android dialog.
Pass `DialogProperties` to adjust dismissal behavior, including during a pending operation.
The title is exposed as both a pane title and a heading. Keep icons decorative when the text
already explains the message. The icon slot supplies layout only; the caller provides its
artwork and any background using Moshi colors. All content must be inside `MoshiTheme`.

### MoshiAlertDialog

Use for a confirmation with a title, message, confirm action, and cancel action. It wraps
`MoshiModal` with the message required and maps the confirm action to the primary button and the
cancel action to the secondary button. Each action is a `MoshiModalAction`, so it carries its own
label, callback, enabled state, and loading state. The confirm button uses `Primary` by default;
set `confirmActionVariant` to `Destructive` for sign-out or other destructive confirmations. The
cancel button uses `Outline`. Pass `onDismissRequest` for Back and outside taps; the cancel action
has its own callback. It inherits the same theme, responsive layout, and accessibility behavior as
`MoshiModal`. Use `MoshiModal` directly when the dialog needs a third action.

### MoshiButton

Use `Primary` for a prominent labeled action with the Moshi primary action fill.
Use `Secondary` for a filled, lower-emphasis action that sits beside a `Primary` action, such as
skipping a step; it uses the secondary action fill with the primary foreground, the secondary hover
fill, and no border. Use `Outline` for a secondary action that needs a visible boundary and `Text`
for a quieter action. Both use a transparent resting background and secondary
action colors, with a 1 dp border on `Outline`. Use `Destructive` for the
prominent, confirming action of an irreversible operation, such as deleting data;
it uses the destructive action fill with the on-destructive label color and the
same disabled and loading behavior as `Primary`. Use `DestructiveOutline` for a
lower-emphasis action that signs the user out or removes data. It keeps the transparent resting
background and secondary hover of `Outline` but uses the destructive action
foreground and the error status border. Disabled and loading states fall back to
the shared disabled colors and default border. `Secondary` follows the same disabled and loading
behavior as `Primary`.

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

Use for a single piece of free-form text, such as a name. The field has a required label inside a
64 dp minimum-height container with a 6 dp corner radius and `typography.body` text. The label rests
vertically centered when the field is empty and unfocused, then floats to the top and shrinks to
`typography.caption` when focused or filled. An optional prefix also keeps the label floated.

The container uses `background.surface`, with a 1 dp `border.default` at rest, a 2 dp
`border.focus` while focused, and `border.status.error` when invalid. Disabled styling takes
precedence over error, and error takes precedence over focus. Disabled fields use
`background.disabled`, `border.default`, and `foreground.disabled`.

An optional non-editable `prefixText` identifies the value's unit. The placeholder uses
`foreground.tertiary` and appears only while the field is focused and empty. Supporting text wraps
below the field with a 16 dp start inset aligned to the field content. It uses `typography.caption`
with `foreground.secondary`, or `foreground.status.error` when invalid. The label is exposed as the
field's accessible name, and supporting text is also
exposed as the error message when the enabled field is invalid. An optional trailing action uses a
24 dp icon in a 48 dp touch target and requires an accessible name. Use sentence case for labels
and pass `KeyboardOptions`, `KeyboardActions`, and `VisualTransformation` for keyboard and display
behavior. Pass an optional autofill `ContentType`, such as `ContentType.EmailAddress`, so the system
can suggest saved values. The field is single line.

### MoshiCodeField

Use for a fixed-length numeric one-time code, such as the email verification code. It draws one cell
per digit (six by default) with a 6 dp corner radius and a 64 dp minimum height. Cells use
`background.surface` with a 1 dp `border.interactive` border, and the cell that receives the next
digit gets a 2 dp `border.focus` border while the field is focused. Invalid fields use
`border.status.error`. Disabled fields use `background.disabled`, `border.default`, and
`foreground.disabled`. Disabled styling takes precedence over error, and error takes precedence over
focus. Digits use the Heading role in `foreground.primary`.

Input is filtered to digits and truncated to `length`, and the keyboard is numeric with the Done
action. Pass `KeyboardActions` to react to Done. Supporting text wraps below the cells with
`typography.caption` in `foreground.secondary`, or `foreground.status.error` when invalid.

The whole field is a single accessibility node named by the required `label`; the cells are
decorative. Supporting text is also exposed as the error message when the enabled field is invalid.
Tapping anywhere on the cells focuses the field and shows the keyboard.

### MoshiCardSelector

Use for a single choice presented as a card, such as the business type during onboarding.
Based on [Mobile Card Selector V02](https://www.figma.com/design/XIRCkRjiI126cvhOjZXn4r/Business-Native?node-id=11131-274057),
it has an 8 dp corner radius, a 64 dp minimum height, an optional 24 dp icon above the label, a
body label, optional Small supporting text, and a trailing radio indicator. Pass `icon` as a
Fluent drawable; it sits `space2` above the label, takes the label color (including the disabled
color), and stays decorative because the label already names the option. Labels and supporting text wrap as needed. The onboarding
business type grid gives every card the height of its tallest option. Every card has a
`border.default` border. Unselected cards use `background.surface`; the selected card uses
`background.action.secondary.hover`, a tint of the primary action blue, with a
`background.action.primary.default` indicator. Disabled cards use
`background.disabled`, `border.default`, and `foreground.disabled`.

The whole card exposes `Role.RadioButton`; the indicator has no separate click action.
Place related cards inside `Modifier.selectableGroup()` and keep selection in the caller's state.
Use Moshi spacing tokens for the 16 dp leading inset and content gap, 4 dp trailing inset,
and 12 dp vertical text insets. Typography and colors follow Moshi in both themes.

### MoshiRadioOption

Use for one choice in a list where exactly one option applies, such as a business type. The
option is a full-width row with the label first and a 20 dp selected indicator last, with a 64 dp
minimum height and a 12 dp corner radius. This keeps cards aligned while allowing them to grow when
labels wrap. Every option has a `border.default` border. Unselected options use
`background.surface`; the selected option uses `background.action.secondary.hover`, a tint of the
primary action blue, with a `background.action.primary.default` outline and dot in the indicator.
Disabled options use
`background.disabled`, `border.default`, and `foreground.disabled`.

Pass `leadingContent` to show a visual before the label, such as a country flag. It is placed
`space3` before the label and should stay decorative, because the label already names the option.

`MoshiRadioOption` exposes `Role.RadioButton` and is selectable as a whole row. Place the options
in a container with `Modifier.selectableGroup()` so assistive technology announces them as one
group. Space the options with `Spacer` or `Arrangement.spacedBy`.

### MoshiToolbar

Use at the top of a screen to show where the user is. Based on the Android App Bar, it has an
optional `typography.subheading` title in `foreground.primary`, an optional `typography.body` subtitle in
`foreground.secondary` (such as the current location), and an optional navigation action before the
title. The bar uses `background.canvas`, has a 64 dp minimum height, and grows when text wraps at
larger font sizes. It applies `WindowInsets.statusBars` by default so the background extends behind
the status bar; pass `windowInsets` to change that, for example when a `Scaffold` already handles them.

Pass `MoshiToolbarNavigation.Back` to return to the previous screen or `MoshiToolbarNavigation.Menu`
to open the app menu. Each shows a 24 dp icon centered in a 48 × 48 dp touch target, with a 16 dp
visual inset from the start edge and the title 56 dp from the start edge. Without navigation, the
title sits 16 dp from the start edge. The navigation action exposes `Role.Button` with a localized
label ("Back" or "Open menu"), and the title is exposed as a heading. Omit the title when the
screen content already has its own heading, such as a step in a workflow that only needs a back
action. The navigation action only runs its callback; it has no enabled or loading state.
`MoshiToolbarNavigation.Back` takes an optional `icon` drawable to replace the default arrow, such as a
dismiss icon for a full-screen flow. When you change the icon, also pass a localized
`contentDescription` that matches it; without one, the action keeps the "Back" label.

Pass `actions` (optional) to add `MoshiToolbarAction` items after the title. With one or two actions,
each shows as a 24 dp icon in a 48 × 48 dp touch target, labeled with the action's `label`. With
three or more, the toolbar shows a single "More actions" button that opens a menu on
`background.elevated` listing every action with its icon and label; choosing one closes the menu
and runs its callback. Set `compact = true` (default `false`) to use the "More actions" menu for any
number of actions, even a single one.

### MoshiLinearProgressIndicator

Use to show how far along a determinate task or workflow is, such as the onboarding steps. It is a
single continuous, fully rounded bar with `spacing.space1` height. The track uses `border.default`
and the fill uses `foreground.primary`. Pass `progress` from `0f` to `1f`; values outside that range
are clamped, and the fill animates when the value changes. The bar is one accessibility node that
exposes its progress through `ProgressBarRangeInfo`, so assistive technology announces it as a
percentage. It is not interactive. Use `MoshiStepProgressIndicator` instead when each step should be
shown as its own segment.

Omit `progress` for the indeterminate variant when the work has no measurable progress, such as a
single network request. It uses the same track, height, and colors, and loops a fill segment 30% of
the track wide from the start edge to the end edge. It exposes `ProgressBarRangeInfo.Indeterminate`,
so assistive technology announces it as in progress without a percentage.

### MoshiNavigationDrawer

Use for the app's top-level destinations, opened from the `MoshiToolbarNavigation.Menu` action or by
swiping from the start edge. It wraps the Material 3 modal drawer, so the sheet slides over the
content on the start edge, dims it with `background.overlay`, and closes on a scrim tap or Back,
including the predictive back gesture. Back closes the drawer before any handler registered before
it opened.
The sheet uses `background.canvas` with square corners, a fixed width of 310 dp, and respects the
system window insets.

Pass `drawerState` to open and close the drawer, and set `gesturesEnabled` to false while a screen
should not be dismissed or revealed by swipe. The `header` slot spans the full sheet width, so it
owns its own padding and any `MoshiDivider`. The `items` slot is inset 12 dp from the sheet edges
and spaces its `MoshiNavigationDrawerItem` children 16 dp apart as one selectable group, so screen
readers announce each item's position. The header and items scroll
together on short screens, while the optional `footer` stays pinned to the bottom. The caller
closes the drawer in response to an item click.

### MoshiNavigationDrawerItem

Use inside `MoshiNavigationDrawer` for one destination. It shows a 24 dp icon in
`foreground.primary`, a 12 dp gap, a Body label in `foreground.primary`, and a 16 dp trailing
chevron in `foreground.secondary`. The selected item fills a 12 dp rounded container with `background.action.secondary.default`;
other items stay on `background.canvas`. The whole row is the click target, with a minimum height of
48 dp and native ripple feedback. The row grows and the label wraps at larger font sizes.

Mark exactly one item as `selected`. It exposes `Role.Tab` with its selected state, and the label
provides the accessible name, so the icon and chevron are decorative. Use sentence case for labels.

### MoshiDivider

Use to separate groups of content, such as the header of a drawer from its items. It is a 1 dp
horizontal line in `border.default` that fills the available width. It is decorative and hidden
from accessibility services. Pass a `modifier` to inset it from the edges or to change its width.
