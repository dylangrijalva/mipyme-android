# Moshi UI for Android

Wrap Moshi components in `MoshiTheme { … }` to provide the light or dark
palette. The theme follows the system setting by default. Pass `darkTheme`
to override it. Read the foundations through `MoshiTheme.colors`,
`MoshiTheme.typography`, and `MoshiTheme.spacing`.

## Moshi token source

The Kotlin foundations use
[Moshi UI v0.1 at `ca215799dcfdc136de3596a23c8993b58db1fd92`](https://github.com/dylangrijalva/moshi-ui/commit/ca215799dcfdc136de3596a23c8993b58db1fd92).
When adopting upstream changes, update the Kotlin values and this revision together.

## Font

Moshi typography uses all nine bundled Geist Sans upright weights, from Thin
(100) through Black (900), from
[Geist v1.9.2](https://github.com/vercel/geist-font/releases/tag/v1.9.2), linked from
[Vercel's font page](https://vercel.com/font). The unmodified TTF files live in
`src/main/res/font`; their SIL Open Font License is bundled in
`src/main/assets/fonts/geist-OFL.txt`.
