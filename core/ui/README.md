# Moshi UI for Android

Wrap components in `MoshiTheme { … }`. It follows the system theme; pass `darkTheme` to
override it. Read `Moshi.colors`, `Moshi.typography`, and `Moshi.spacing` inside the theme.

## Token source

Color and typography use the [16 November 2026 Geist snapshot](../../docs/geist-token-research.md).
The [JSON fixture](../../docs/geist-tokens.json) retains authored CSS, source URLs, and hashes.
There are 92 colors per theme, 70 P3 overrides per theme, 30 text styles, and 15 modifiers.
The [component table](../../docs/geist-component-mapping.md) records state mappings and native
Android adaptations. Spacing and geometry retain the existing Moshi values.

Use semantic color roles such as `Moshi.colors.foreground.primary`, `Moshi.colors.border.focus`, and `Moshi.colors.action.primary.background`. The retained Geist palette is internal to the foundation and its fixture tests.
Use `Moshi.typography.copy16` and `MoshiTextRole.Copy16` for text.
See [DESIGN.md](../../DESIGN.md) for component usage and emphasis behavior.

## Fonts

Geist Sans and Geist Mono come from the official
[v1.9.2 release archive](https://github.com/vercel/geist-font/releases/tag/v1.9.2).
The archive fonts report Version 1.900. Existing Sans weights 100 through 900 are unchanged.
Static Sans 550 and Mono 400/550 instances were produced from that archive's variable fonts
using fontTools varLib.instancer, so exact 550 works on API 24. The repository tag's variable
files report older versions; they were not used. [Font provenance](../../docs/geist-fonts.json)
records archive members, hashes, and the generation tool. The SIL Open Font License is bundled
in `src/main/assets/fonts/geist-OFL.txt` and applies to both families and the derived instances.

## Verification

Run `./gradlew :core:ui:testDebugUnitTest :core:ui:assembleDebug :core:ui:lintDebug` and
`python3 scripts/verify_geist_tokens.py` from the repository root. The unit tests exercise every
fallback, P3 override, text style, and emphasis modifier against retained fixtures. P3 numerical
references use independent ColorAide conversion. Production uses authored sRGB because Compose UI 1.11 converts solid paint colors to sRGB.
The full P3 data and conversion tests remain for future support. Regenerate the standalone reference with `python3 scripts/generate_geist_reference.py`.
