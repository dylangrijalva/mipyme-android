---
name: fluent-icon
description: >-
  Vendor a Fluent System Icon into this Android app by copying a single drawable
  from the local fluentui-system-icons checkout into app/src/main/res/drawable.
  Use this whenever the user wants to add, use, swap in, or replace an icon in the
  app. Icons must always come from Fluent System Icons and be vendored one file at
  a time; never add the com.microsoft.design:fluent-system-icons AAR dependency,
  which lags the IDE. The app standardizes on the 24-grid, regular style, so this
  always vendors the ic_fluent_<name>_24_regular variant.
---

# Fluent icon vendoring

## Why this exists

The app standardizes on **Fluent System Icons**, but the `com.microsoft.design:fluent-system-icons`
AAR makes the IDE lag, so we do **not** depend on the library. Instead, copy the one drawable you
need out of a local checkout of the icon repo. One icon = one vendored file.

## Source of truth

Local checkout (already cloned on this machine):

```
/Users/dagn/dev/android/fluentui-system-icons/android/library/src/main/res/drawable
```

Every icon is a flat vector XML named with the library's convention:

```
ic_fluent_<name>_<size>_<style>.xml
```

The library ships multiple sizes and styles, but this app standardizes on `24` + `regular` for
visual consistency. Always vendor `ic_fluent_<name>_24_regular.xml` and keep its exact filename.

These drawables fill with `@color/fluent_default_icon_tint`, a shared color the library keeps in
`res/color/`. If it is missing, AAPT resource linking fails, so the script also vendors it once.
Compose `Icon` composables tint over the fallback color with their current content color.

## How to add an icon

1. Resolve the canonical library name. For example, `email` maps to `mail`, `close` maps to
   `dismiss`, and `show` maps to `eye`. If unsure, list matching files in the source checkout.

2. From the project root, run the bundled script with only the icon name:

   ```
   .agents/skills/fluent-icon/scripts/add_icon.sh mail
   ```

   The script copies the `24` `regular` variant into `app/src/main/res/drawable/` and prints the
   corresponding `R.drawable` reference. If the variant does not exist, it lists close matches.

3. Reference the local resource through the app's own `R`:

   ```kotlin
   Icon(
       painter = painterResource(R.drawable.ic_fluent_mail_24_regular),
       contentDescription = null,
       modifier = Modifier.size(20.dp),
   )
   ```

## Rules

- Never add the Fluent AAR Gradle dependency or reference `com.microsoft.fluent.mobile.icons.R`.
- Do not hand-author icon drawables or paste SVGs from elsewhere; vendor from the checkout.
- Brand marks, including the app logo and third-party logos, are out of scope.
