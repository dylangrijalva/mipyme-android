---
name: Moshi UI for Android
components:
  button:
    variants:
      Primary:
        container: gray1000
        content: background100
        hoveredContainer: component-local primary hover (22% light / 80% dark)
      Secondary:
        container: background100
        content: gray1000
        hoveredContainer: grayAlpha200
        border: gray400
      Outline:
        container: transparent
        content: gray1000
        hoveredContainer: gray200
        border: gray400
        disabledBorder: gray400
      Text:
        container: transparent
        content: gray1000
        hoveredContainer: gray200
    size: Default
    disabledContainer: gray100
    disabledContent: gray700
    radius: radius.lg
    label: typography.button14
    minWidth: spacing.space-12
    minHeight: spacing.space-12
    horizontalPadding: spacing.space-4
    verticalPadding: spacing.space-2
    contentGap: spacing.space-2
    leadingSlotSize: spacing.space-6
    spinnerSize: spacing.space-4
---

# Moshi UI for Android

Use the Geist color and typography scales through `Moshi.colors` and `Moshi.typography`.
The retained [Geist snapshot](docs/geist-token-research.md) and
[component mappings](docs/geist-component-mapping.md) document the source and Android adaptations.
Moshi spacing, shapes, component geometry, and interactions keep their existing values.
The Android token implementations and component code define the behavior shipped in this app.

## Components

### MoshiSearchField

Use for a single-line search query. The rounded field has a decorative leading Fluent search icon,
a label shown when empty, and a trailing clear button only while the value is nonempty. The label
also supplies the accessible name when a query is entered. `onClear` resets the value through
`onValueChange` by default; override it when the caller owns a larger search transition. The default
clear label is localized, and `clearLabel` can describe a more specific action.

The field uses Copy16 typography, `background100`, and `gray400`, with a 2 dp
`blue900` when focused. Disabled fields and their clear actions use disabled colors and ignore
input. The minimum height and clear touch target are 48 dp. The Search keyboard action dismisses
the keyboard. Compose adjacent filter or scan actions outside this field.

### MoshiEmptyState

Use when a section has no content or a search has no matches. Content appears in this order: a
24 dp icon slot, title, description, optional primary outline button, optional secondary text button.
Pass `MoshiEmptyStateAction` values with a label, callback, and enabled state. Either action may be
omitted independently. Buttons stack vertically and labels wrap at larger font sizes.

The title uses Heading20 typography and heading semantics. The description uses Copy16 typography
and `gray900`. Icon content inherits `gray1000`, matching the title; keep its accessible
description null when the title already explains the state. The caller supplies localized copy,
controls visibility, and provides scrolling when the available height is limited.

### MoshiMonogram

Use as a decorative image fallback beside a full visible name. It displays the first two words of
`name`, adds an ellipsis when more words exist, and truncates within its available width. Its default
size is 64 dp; a caller can set the required image dimensions through `modifier`. The 8 dp rounded
container uses the secondary action background and Label12 text. It has no accessibility node
because the adjacent label should expose the full name.

### MoshiPill

Use for compact tab choices or filter actions. Selected pills use the inverse background and
foreground; unselected pills use the surface background and default border. Pass `count` as an
optional formatted string displayed after the label. Text wraps and the pill grows vertically.
All pills have a minimum 48 × 48 dp touch target and expose their selected and disabled state.
Pass `Role.Tab` and put sibling tabs in a selectable group for navigation between sections;
the default role is Button. Callers own selection and click behavior.

### MoshiStatusBadge

Use for a noninteractive, labeled status. `Neutral` uses gray100 background and gray900 foreground
colors. `Warning`, `Error`, and `Success` use the matching Geist scale steps 100 and 900. The Label12 label
wraps, so status information remains available at larger font sizes. Always include a label rather
than communicating status through color alone. Keep domain-specific status selection in the caller.

### MoshiIconButton

Use for an icon-only action. Pass a vendored Fluent drawable and a localized `label` for its
accessible name. The circular button has a 24 dp icon, a minimum 48 × 48 dp touch target, ripple
feedback, and disabled foreground styling. Disabled buttons ignore clicks.

### MoshiLoadingIndicator and MoshiPullToRefreshBox

Use `MoshiLoadingIndicator` for indeterminate local loading. It uses a 32 dp spinner, semantic Moshi
colors, and a localized loading label. `MoshiPullToRefreshBox` wraps scrollable content with the
native pull gesture and a Moshi-colored refresh indicator. Its `isRefreshing` state and `onRefresh`
callback belong to the caller. Keep a vertically scrollable child even for empty content so the
gesture can start. The wrapper does not replace cached content or trigger data access itself.

### MoshiModal

Use for a focused message or decision that interrupts the current screen. It has a left-aligned
title, optional supporting text, an optional 80 dp icon slot, and up to three actions. The elevated
surface has a 12 dp corner radius and a maximum width of 720 dp. It uses the active Moshi theme,
with `background100` in light mode and `gray100` in dark mode to separate the dialog from the backdrop.
Side insets are 12 dp, the bottom inset is 24 dp, and top padding is 24 dp without an icon and
16 dp with one. Text
uses Heading20 and Copy16 roles, with primary and secondary foreground colors. Two actions sit side
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

Use `Primary` for a prominent labeled action with a neutral gray1000 fill and background100 text.
Use `Secondary` for a lower-emphasis action beside `Primary`, such as skipping a step. It uses
background100, gray1000 text, a gray400 border, and grayAlpha200 on hover. Use `Outline` for a secondary action that needs a visible boundary and `Text`
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
the button's foreground and Button14 role unless the caller supplies an override.
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
circle inside a 48 × 48 dp touch target. Checked uses `gray1000`
with `background100` for the check; unchecked uses a `gray700`
outline. Disabled checked controls use `gray600`; disabled unchecked controls use
`gray100` fill and a `gray500` outline.

`MoshiCheckbox` renders only the box and exposes `Role.Checkbox`. Place the label beside it
in a `Row` so the label can contain its own links.

### MoshiPageIndicator

Use below a paged surface to show position. The current page is a 24 × 8 dp pill in
`gray1000`; other pages are 8 dp dots in `gray500`. The indicator is a single
accessibility node announcing "Page x of y". It is not interactive.

### MoshiStepProgressIndicator

Use in a multi-step workflow to show the current and completed steps. Each step is an equal-width,
rounded segment with `spacing.space1` height and gaps. Current and completed steps use
`gray1000`; upcoming steps use `gray400`. The indicator is a single accessibility
node announcing "Step x of y". `currentStep` is zero-based, and nonpositive `stepCount` renders no
indicator.

### MoshiText with `AnnotatedString`

An `AnnotatedString` overload of `MoshiText` supports inline styled spans and links.
Style links with `blue900` and an underline.

### MoshiTextField

Use for a single piece of free-form text, such as a name. The field has a required label inside a
64 dp minimum-height container with a 6 dp corner radius and `typography.copy16` text. The label rests
vertically centered when the field is empty and unfocused, then floats to the top and shrinks to
`typography.label12` when focused or filled. An optional prefix also keeps the label floated.

The container uses `background100`, with a 1 dp `gray400` at rest, a 2 dp
`blue900` while focused, and `red900` when invalid. Disabled styling takes
precedence over error, and error takes precedence over focus. Disabled fields use
`gray100`, `gray400`, and `gray700`.

An optional non-editable `prefixText` identifies the value's unit. The placeholder uses
`gray900` and appears only while the field is focused and empty. Supporting text wraps
below the field with a 16 dp start inset aligned to the field content. It uses `typography.label12`
with `gray900`, or `red900` when invalid. The label is exposed as the
field's accessible name, and supporting text is also
exposed as the error message when the enabled field is invalid. An optional trailing action uses a
24 dp icon in a 48 dp touch target and requires an accessible name. Use sentence case for labels
and pass `KeyboardOptions`, `KeyboardActions`, and `VisualTransformation` for keyboard and display
behavior. Pass an optional autofill `ContentType`, such as `ContentType.EmailAddress`, so the system
can suggest saved values. The field is single line.

### MoshiCodeField

Use for a fixed-length numeric one-time code, such as the email verification code. It draws one cell
per digit (six by default) with a 6 dp corner radius and a 64 dp minimum height. Cells use
`background100` with a 1 dp `gray500` border, and the cell that receives the next
digit gets a 2 dp `blue900` border while the field is focused. Invalid fields use
`red900`. Disabled fields use `gray100`, `gray400`, and
`gray700`. Disabled styling takes precedence over error, and error takes precedence over
focus. Digits use the Label20 role in `gray1000`.

Input is filtered to digits and truncated to `length`, and the keyboard is numeric with the Done
action. Pass `KeyboardActions` to react to Done. Supporting text wraps below the cells with
`typography.label12` in `gray900`, or `red900` when invalid.

The whole field is a single accessibility node named by the required `label`; the cells are
decorative. Supporting text is also exposed as the error message when the enabled field is invalid.
Tapping anywhere on the cells focuses the field and shows the keyboard.

### MoshiCardSelector

Use for a single choice presented as a card, such as the business type during onboarding.
Based on [Mobile Card Selector V02](https://www.figma.com/design/XIRCkRjiI126cvhOjZXn4r/Business-Native?node-id=11131-274057),
it has an 8 dp corner radius, a 64 dp minimum height, an optional 24 dp icon above the label, a
Label16 label, optional Copy14 supporting text, and a trailing radio indicator. Pass `icon` as a
Fluent drawable; it sits `space2` above the label, takes the label color (including the disabled
color), and stays decorative because the label already names the option. Labels and supporting text wrap as needed. The onboarding
business type grid gives every card the height of its tallest option. Every card has a
`gray400` border. Unselected cards use `background100`; the selected card uses
`gray200`, a neutral selected fill, with a
`gray1000` indicator. Disabled cards use
`gray100`, `gray400`, and `gray700`.

The whole card exposes `Role.RadioButton`; the indicator has no separate click action.
Place related cards inside `Modifier.selectableGroup()` and keep selection in the caller's state.
Use Moshi spacing tokens for the 16 dp leading inset and content gap, 4 dp trailing inset,
and 12 dp vertical text insets. Typography and colors follow Moshi in both themes.

### MoshiRadioOption

Use for one choice in a list where exactly one option applies, such as a business type. The
option is a full-width row with the label first and a 20 dp selected indicator last, with a 64 dp
minimum height and a 12 dp corner radius. This keeps cards aligned while allowing them to grow when
labels wrap. Every option has a `gray400` border. Unselected options use
`background100`; the selected option uses a neutral `gray200` fill and a `blue900`
outline and dot in the indicator.
Disabled options use
`gray100`, `gray400`, and `gray700`.

Pass `leadingContent` to show a visual before the label, such as a country flag. It is placed
`space3` before the label and should stay decorative, because the label already names the option.

`MoshiRadioOption` exposes `Role.RadioButton` and is selectable as a whole row. Place the options
in a container with `Modifier.selectableGroup()` so assistive technology announces them as one
group. Space the options with `Spacer` or `Arrangement.spacedBy`.

### MoshiToolbar

Use at the top of a screen to show where the user is. Based on the Android App Bar, it has an
optional `typography.heading20` title in `gray1000`, an optional `typography.copy16` subtitle in
`gray900` (such as the current location), and an optional navigation action before the
title. The bar uses `background100`, has a 64 dp minimum height, and grows when text wraps at
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
`background100` in light mode and `gray100` in dark mode, listing every action with its icon and label; choosing one closes the menu
and runs its callback. Set `compact = true` (default `false`) to use the "More actions" menu for any
number of actions, even a single one.

### MoshiLinearProgressIndicator

Use to show how far along a determinate task or workflow is, such as the onboarding steps. It is a
single continuous, fully rounded bar with `spacing.space1` height. The track uses `gray400`
and the fill uses `gray1000`. Pass `progress` from `0f` to `1f`; values outside that range
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
content on the start edge, dims it with `grayAlpha700`, and closes on a scrim tap or Back,
including the predictive back gesture. Back closes the drawer before any handler registered before
it opened.
The sheet uses `background100` with square corners, a fixed width of 310 dp, and respects the
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
`gray1000`, a 12 dp gap, and a Label16 label in `gray1000`, with no trailing
chevron. The selected item fills a 12 dp rounded container with `gray100`;
other items stay on `background100`. The whole row is the click target, with a minimum height of
48 dp and native ripple feedback. The row grows and the label wraps at larger font sizes.

Mark exactly one item as `selected`. Pass `selectedIcon` (optional, defaults to `icon`) to show the
filled variant of the icon while the item is selected. It exposes `Role.Tab` with its selected state,
and the label provides the accessible name, so the icon is decorative. Use sentence case for labels.

### MoshiListItem

Use for a tappable row in a list of choices or actions, such as the entries of a settings screen. It
fills the available width with no outer margins, so the whole row from edge to edge is the click
target and shows native ripple feedback. Content sits inside a 16 dp horizontal inset
(`pageGutterNarrow`) within that target. The row has 16 dp vertical padding (`space4`), so a
single-line row is 56 dp tall, and a 48 dp minimum height, and grows when the label wraps at larger font sizes. The label uses `typography.label16` in
`gray1000`.

Pass `leadingIcon` (optional) to show a 24 dp icon before the label, separated by a 12 dp gap. The
icon is decorative; the label provides the accessible name. Pass `trailing` (optional) to place
content after the label, such as `MoshiListItemChevron` to signal that the row opens another screen.
`MoshiListItemChevron` is a decorative 16 dp chevron in `gray900`. Leave `trailing` empty
for rows that perform an action in place.

Set `destructive` to true for irreversible or account-level actions, such as signing out. The label
and leading icon then use `red900`. The row exposes `Role.Button`. Use
sentence case for labels. Items have no dividers; stack them directly.

### MoshiBottomSheet

Use for a short, focused choice that slides up from the bottom edge, such as picking one option from a
small list. It wraps the Material 3 modal bottom sheet. The elevated surface uses
`background100` with 12 dp top corners, a `grayAlpha700` scrim, and a drag handle in
`gray500`. Content sits inside a 16 dp horizontal inset (`pageGutterNarrow`), with 24 dp below
it. Material's default window insets keep the content clear of the navigation bar.

Pass `title` (optional) to show a left-aligned `typography.heading24` title in `gray1000`,
exposed as a heading and followed by a 16 dp gap before the content. Swiping down, tapping the scrim,
and Back all call `onDismissRequest`. The caller owns visibility and must stop composing the sheet in
response. The sheet opens fully expanded; pass `sheetState` to change that.

### MoshiSelectableItem

Use for a row in a single-choice list inside a surface such as `MoshiBottomSheet`. Wrap the rows in a
parent with `Modifier.selectableGroup()`. It fills the available width with a 12 dp corner radius and
a 48 dp minimum height, with 16 dp horizontal and 12 dp vertical padding. The whole row is the
selectable target with a rounded native ripple and exposes `Role.RadioButton`.

The label uses `typography.label16` in `gray1000`, or `gray700` when disabled.
Pass `supportingText` (optional) for a `typography.copy14` description in `gray900`
below the label; both texts use `gray700` when disabled. Text wraps at larger font
sizes. Selected rows fill with `gray200` and show a decorative 24 dp
checkmark in `gray1000` at the end; unselected rows are transparent with no
border.

### MoshiDivider

Use to separate groups of content, such as the header of a drawer from its items. It is a 1 dp
horizontal line in `gray400` that fills the available width. It is decorative and hidden
from accessibility services. Pass a `modifier` to inset it from the edges or to change its width.

## Geist foundations

`Moshi.colors` exposes `background100`, `background200`, and the ten steps `100` through
`1000` in `gray`, `grayAlpha`, `blue`, `red`, `amber`, `green`, `teal`, `purple`, and `pink`.
Choose the actual treatment from the component mapping. Gray900 is secondary text and gray1000
is primary text. GrayAlpha is for translucent edges and overlays. Old semantic token aliases
have been removed.

`Moshi.typography` and `MoshiTextRole` expose all 30 retained Geist styles, such as `heading24`
/ `Heading24`, `button14` / `Button14`, `label14` / `Label14`, and `copy16` / `Copy16`.
Copy16 is the default paragraph role. Buttons provide Button14 to their content. Use labels
for controls and copy for paragraphs. Copy14Mono is present in deployed CSS but is not listed
in the public typography table. Unspecified source tracking stays unspecified.

Use `emphasis = MoshiTextEmphasis.Strong` for supported copy and label roles. Heading16,
Heading20, Heading24, and Heading32 support `Subtle` instead. An unsupported modifier fails
with a descriptive error. For inline emphasis, call `withMoshiEmphasis { append(...) }` inside
`buildAnnotatedString`. It chooses the heading subtle or copy/label strong treatment for its
containing role. Copy strong is exact weight 550, label strong and heading subtle are 500.
Modifiers use gray1000 or gray900 by default. An explicit `MoshiText` color or an inherited
component color takes precedence over modifier colors, including disabled button content.
Unannotated caller spans keep their existing styling.

Font sizes, line heights, and explicit tracking use `sp`. Font padding is disabled to honor the
specified line boxes while preserving Android font scaling and wrapping. Static Sans and Mono
550 instances support API 24 without approximating the glyph weight. Production Moshi uses the
authored sRGB fallbacks, as approved after inspecting Compose UI 1.11's solid-color rendering
path. Full P3 data and tested conversion remain available in the retained snapshot and reference
for a future renderer that supports it. Browser and Android glyph rasterization can differ.

### MoshiChoicebox

Use `MoshiChoicebox` for a small set of choices that need descriptions and a larger tap target.
It follows [Geist Choicebox](https://vercel.com/geist/choicebox): 12 dp padding and tile gaps,
6 dp corners, a gray400 border, and blue100 / blue600 / blue900 selected treatments. Titles
use Label14 with Strong emphasis; descriptions use Copy14. Radio dots and checkbox checkmarks
make selection visible without relying on color.

Pass a localized `label` for the accessible group name and `MoshiChoiceboxOption` entries with
unique `value` keys, localized titles, and optional descriptions. The single-select overload
accepts `selectedValue: String?` and `onValueChange`; the multi-select overload accepts
`selectedValues: Set<String>` and `onValuesChange`. Both are controlled: the caller owns state.
Use `enabled = false` on the group or an individual option to prevent changes. Supply a localized
`disabledReason` to explain unavailable choices; it is shown and announced within the disabled
tile. A group reason takes precedence while the group is disabled.

The default horizontal layout gives tiles equal widths and lets their height fit the content. Use
`orientation = Orientation.Vertical` for narrow screens or longer descriptions. Text wraps and
scales with Android font preferences, and each tile has a minimum 48 × 48 dp touch target.
The entire tile is one radio or checkbox target; indicators add no separate actions. Native
Compose focus and activation behavior is retained. An option's `selectedContent` slot appears
below a separator only while selected. Keep this content descriptive, with no nested buttons,
links, or other interactive controls. Prefer four to six choices; use a list or picker for more.

