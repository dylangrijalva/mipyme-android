---
name: build-alessia-component
description: Document, design, implement, preview, and test public Alessia Jetpack Compose components in the Facilito Android repository. Use when creating a new Alessia UI component, adapting an external design-system component for mobile, or changing an existing component's API, variants, states, sizing, interaction, accessibility, or DESIGN.md specification.
---

# Build Alessia Component

Create Alessia components through a documentation-first workflow that resolves product decisions before code and keeps implementation, previews, tests, and design guidance synchronized.

## Establish context

1. Read the repository `AGENTS.md` and follow its UI, architecture, and surgical-change rules.
2. Read `DESIGN.md` completely before planning or changing user-facing UI.
3. Inspect existing theme tokens, public components, previews, tests, build configuration, and naming conventions. Prefer the codebase knowledge graph when available; fall back to repository search only when needed.
4. If the request references another design system, browse its current primary documentation. Extract behavior and hierarchy, then adapt them for Android rather than copying web-only concepts.
5. Resolve discoverable facts from the repository instead of asking the user.

## Resolve the component contract

Interview the user one decision at a time. Give a recommended answer with the tradeoff, wait for the response, and retain an explicit decision log. Do not edit documentation or code until the user confirms shared understanding.

Walk through applicable decisions in dependency order:

1. Purpose, scope, and mobile adaptation boundary.
2. Public component, enum, parameter, and package names.
3. Required and optional content; structured data versus composable slots.
4. Variants and their semantic surface, content, and border token mappings.
5. Sizes, typography, icons, padding, gaps, minimum dimensions, and touch targets.
6. Enabled, disabled, loading, selected, error, and other states; define precedence for combinations.
7. Pressed, focused, keyboard, D-pad, and pointer behavior.
8. Localization, system font scaling, wrapping, overflow, and constrained layouts.
9. Accessibility roles, labels, progress/state semantics, focus visibility, and decorative content.
10. Public customization boundaries such as colors, shapes, elevation, and arbitrary modifiers.
11. Preview and test coverage.

Avoid speculative options. Add a public parameter or token only when it represents a distinct product decision or required use case.

Before implementation, restate the complete contract and obtain explicit approval to proceed.

## Document first

Update `DESIGN.md` before Kotlin:

1. Populate the YAML `components` section with normative recipes. Reference foundation tokens rather than repeating raw values.
2. Add Markdown guidance under `## Components` covering purpose, API, variants, sizes, layout, states, behavior, accessibility, writing, and prohibited uses.
3. Keep YAML recipes and Markdown terminology identical to the approved public API.
4. Never reference raw palette tokens from a component recipe when a semantic token exists.

Treat the completed documentation as the implementation contract. If implementation reveals a missing decision, stop and resolve it rather than silently inventing behavior.

## Implement in dependency order

1. Create public components in `io.facilito.ui.component`, normally one component per file.
2. Reuse existing Alessia public primitives before Material components. Use a Material or Foundation primitive internally only when Alessia has no equivalent.
3. Consume `Alessia` semantic tokens and component recipes; do not use raw colors or unexplained numeric values.
4. Preserve the caller's `modifier` as the first optional parameter and follow established Compose parameter ordering.
5. Keep public enums next to the component they configure unless reuse requires a separate module.
6. Keep variant and state resolution private. Expose semantic choices, not arbitrary styling escape hatches.
7. Preserve Android-native interaction, focus, and accessibility behavior while enforcing Alessia visuals.
8. Touch only files required by the approved contract. Do not refactor adjacent code.

## Add previews

Add private light and dark previews for each public component. Cover all meaningful states without generating a redundant Cartesian product:

- Every variant and behavioral state.
- Every size.
- Every supported content configuration.
- Long or translated text and increased-layout-pressure cases.
- Representative semantic colors when the component accepts color.
- Every predefined style when the component exposes a style enum.

Use existing resources or Android framework preview resources rather than adding product assets solely for previews.

## Add tests

Add focused Compose UI tests for observable contracts:

- Action delivery and suppression.
- Enabled, disabled, loading, and state-precedence semantics.
- Accessibility roles and progress/state information.
- Required visible content.
- Every variant and size constructing successfully.
- Wrapping or constrained-width behavior when specified.
- Every public enum value resolving successfully.

Apply the Compose BOM to `androidTestImplementation` as well as production dependencies when test libraries omit explicit versions. Add only the test dependencies required by the new coverage.

## Verify

1. Run `git diff --check`.
2. Compile the relevant development production source set.
3. Compile the Android-test source set.
4. Run the connected UI tests when a compatible emulator or device is available.
5. Inspect the final diff for undocumented behavior, raw-token usage, unrelated changes, public API drift, and missing preview states.

This repository's Compose UI Test 1.7 harness fails before composition on API 37 with a missing `InputManager.getInstance()` reflection method. When that exact infrastructure error occurs, run the unchanged suite on an installed supported AVD such as the minimum-SDK emulator. Do not treat the harness crash as a component failure or upgrade the application Compose stack without user approval.

Report production compilation, test compilation, executed test results, and any environment limitation separately.

## Finish cleanly

Keep documentation, code, previews, and tests in one coherent diff. Remove only imports, helpers, or resources made unused by this work. Do not commit or push unless asked.
