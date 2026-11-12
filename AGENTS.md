# AGENTS.md

This file provides guidance to Claude (claude.com/claude-code) and Codex (openai.com/codex) when working in this
repository. See `@README.md` for an overview of the product and its tech stack.

## Architecture

- **UI:** Jetpack Compose using MVI.
- **State:** Unidirectional Data Flow using Kotlin Coroutines and `Flow`. Every `ViewModel` must expose exactly one
  immutable `StateFlow<XState>` named `stateFlow`, along with a `state` getter.
- **Navigation:** Jetpack Compose Navigation 3. Every screen must have a navigation key defined in the `Route` sealed
  interface.
- **Dependency Injection:** Koin.
- **Data Sources:**
    - **Room:** Structured, queryable persistent data. See [Offline-First Architecture](#offline-first-architecture).
    - **DataStore:** Asynchronous key-value storage for lightweight data, such as bearer tokens and local preferences.
    - **Ktor:** Asynchronous network requests to Mipyme's API and external services.

### Architecture Boundaries

- Keep presentation logic in `ViewModel` classes, not Composables.
- Keep data access and synchronization logic out of `ViewModel` classes.
- Use repositories to coordinate data sources.
- Preserve unidirectional data flow.
- Do not introduce additional sources of truth for data already managed elsewhere.
- Follow existing dependency injection and navigation patterns.

## Design System

All user interfaces must be built with **Moshi**, Mipyme's design system. Moshi lives in the `:core:ui` module (
`co.mipyme.ui`). See `@DESIGN.md` for component usage guidelines and `@core/ui/README.md` for the token source.

- **Theme:** Render all UI inside `MoshiTheme { … }`. It provides the light and dark palettes and follows the system
  setting by default.
- **Components:** Use Moshi components (e.g., `MoshiButton`, `MoshiText`) instead of their Material 3 or Foundation
  equivalents. Do not use Material 3 components directly when a Moshi component exists.
- **Tokens:** Read colors, typography, and spacing from `Moshi.colors`, `Moshi.typography`, and `Moshi.spacing`. Do not
  hardcode colors, font sizes, or `dp` values that a Moshi token already covers.
- **Spacing between elements:** Use `Spacer` (e.g., `Spacer(Modifier.height(spacing.space4))`) to separate sibling
  composables instead of applying margins through `padding` modifiers on them. Reserve `padding` for a container's inner
  insets.
- **Semantic colors:** Prefer semantic tokens (e.g., `background.action.primary`, `foreground.disabled`,
  `border.interactive`) that match the element's purpose over raw palette values.
- **Missing components:** If a required component or token does not exist in Moshi, ask before creating it. New
  components belong in `:core:ui` under `co.mipyme.ui.component`, use the `Moshi` prefix, consume only Moshi tokens, and
  must be documented in `DESIGN.md`.
- **Previews:** Wrap previews in `MoshiTheme`. Inside `:core:ui`, annotate component previews with `@MoshiPreview` to
  render both light and dark themes.
- **Accessibility:** Preserve Moshi's accessibility behavior, including the 48 × 48 dp minimum touch target,
  sentence-case labels, and text that wraps at larger font sizes.

## Coding Style

- Prefer `val`; use `var` only when necessary.
- Use block bodies instead of expression bodies for function declarations.
- Follow these naming conventions:
    - `camelCase` for variables and functions.
    - `PascalCase` for classes.
    - `UPPER_SNAKE_CASE` for constants.
    - Lowercase package names, using underscores for multiple words (e.g., `getting_started`).
- Use `kotlinx.coroutines`; avoid `Thread`, `AsyncTask`, and `GlobalScope`.
- Use `Flow` for reactive streams.
- Prefer named, feature- or domain-specific `Flow` functions over large anonymous `ViewModel` pipelines or generic
  transformer utilities.
- Represent UI state and events using sealed classes or data classes.
- Update `MutableStateFlow` using `update`.
- Use `viewModelScope` or `lifecycleScope`.
- Avoid throwing exceptions from coroutines; use `Result` or Arrow's `Either`.

## Screen Constraints

Every screen must be backed by a `ViewModel`, either screen-specific or shared within a workflow.

Every screen must declare its `@Preview` composable in the same file as the screen. Annotate it with `@ScreenPreview` (
`co.mipyme.preview`), which renders light and dark themes with system UI. Previews render the stateless screen content
directly, without a `ViewModel` or Koin, and are wrapped in `MoshiTheme`.

- A screen-specific `ViewModel` owns that screen's presentation logic and UI state.
- A `ViewModel` must never directly depend on another `ViewModel`.
- Multiple screens may share a `ViewModel` only when they form a single presentation unit.

Choose the state-sharing strategy based on the screens' presentation logic.

### Shared ViewModel

Use a single `ViewModel` when multiple screens belong to the same workflow and function as one presentation unit.

Example workflow:

```text
CheckoutScreen
    ↓
CartDetailScreen
    ↓
ConfirmPaymentScreen
```

These screens may share a `CheckoutViewModel` if:

- They operate on the same UI state.
- Their presentation logic belongs to the workflow as a whole.
- They do not require independent screen-specific presentation logic.

The shared `ViewModel` must be scoped to the workflow.

### Screen ViewModels with a Store

Use a separate `ViewModel` for each screen and a shared `Store` when screens belong to the same workflow but require
independent presentation logic.

Example structure:

```text
CheckoutStore
├── CheckoutViewModel
├── CartDetailViewModel
└── ConfirmPaymentViewModel
```

- Each screen has its own `ViewModel`.
- Each `ViewModel` owns only its screen's UI state and behavior.
- The `Store` owns the mutable state shared across the workflow.
- All participating `ViewModel` instances must receive the same `Store` instance.
- The `Store` must be scoped to the workflow and released when the workflow ends.

### Decision Rule

- **Shared `ViewModel`:** Use when multiple screens form one presentation unit.
- **Separate `ViewModel` instances with a shared `Store`:** Use when screens require independent presentation logic but
  share mutable workflow state.

## Single Source of Truth

Each service must define a **single source of truth** that maintains consistent, correct, and up-to-date data.

Repositories must expose data directly from their source of truth. A source of truth may be a data source, such as a
database, or an in-memory cache maintained by the repository.

Repositories are responsible for combining data sources, resolving conflicts, and updating the source of truth
periodically or in response to user actions.

Different repositories may use different sources of truth. For example:

- `LoginRepository` may use an in-memory cache.
- `PaymentsRepository` may use a network data source.

For offline-first support, prefer a local data source, such as a database, as the source of truth.

## Offline-First Architecture

The application follows an **offline-first architecture** so merchants can continue selling and taking orders during
internet outages.

Offline mode has the following constraints:

1. **Restricted writes:** Most write operations must be disabled while offline. Only sales and refunds are allowed.
   These operations must be queued, and a background service must upload them when connectivity is restored.
2. **No business data updates:** Offline transactions must not change inventory quantities, reports, or other existing
   business data while offline. Queued operations must still be persisted for later synchronization.

**Note:** When uploading queued orders, the server ignores the `allowBackOrders` property. This prevents losing a
completed sale when the merchant could not adjust inventory due to the lack of connectivity.

### Offline Operation Safety

- Preserve the existing behavior of offline sales and refunds.
- Never discard queued operations before successful synchronization.
- Do not update inventory quantities, reports, or other business data from offline transactions while offline.
- Do not change synchronization behavior or conflict-resolution rules without approval.
- Treat changes affecting queued transactions as potentially data-critical.

## Threading

All data sources and repositories must be **main-safe**, meaning they can be called safely from the main thread.

These components are responsible for moving long-running or blocking operations to the appropriate thread. This includes
operations such as:

- Reading files from a data source.
- Performing expensive filtering on large collections in a repository.

Prefer existing main-safe APIs when available, such as the suspending APIs provided by Room, Retrofit, and Ktor.

## Verification

- Run relevant tests after making changes.
- Run applicable compilation and static analysis checks.
- Fix errors introduced by your changes.
- Do not modify unrelated code to make checks pass.
- Never claim tests or checks passed unless they were actually executed.
- If verification cannot be completed, explain what was not verified and why.

## Git & Commits

- Follow Conventional Commits, matching the repository's existing history (e.g., `fix(sync): …`, `docs(design): …`).
- Never append `Co-Authored-By` or other agent attribution trailers. Use only the commit message.
- Commit or push changes **only when explicitly asked**.

## Behavioral Guidelines

### Before Making Changes

- Read the relevant code before modifying it.
- Look for existing implementations of similar functionality and follow their patterns.
- Reuse existing abstractions, utilities, and components when appropriate.
- Do not introduce new dependencies or architectural patterns without approval.
- Do not assume how something works when its implementation can be inspected.

# Planning

When asked to plan a feature, architectural change, or complex implementation:

1. Explore the relevant design decisions and their dependencies.
2. Ask one question at a time and wait for my response before continuing.
3. Provide your recommended answer with each question.
4. Resolve factual questions by inspecting the codebase rather than asking me.
5. Leave architectural and product decisions to me. Present each decision and wait for my answer.
6. Do not implement until I explicitly approve the plan.

For straightforward implementation tasks, follow established repository conventions. Ask for clarification only when a
decision cannot be resolved from the codebase or existing instructions.

# Implementation Delegation

Before starting any implementation, always ask me which model should handle it, even for straightforward tasks.

- Detect the current AI vendor and the models available for delegation.
- Recommend the least expensive model that can reliably implement the task, considering complexity, reasoning
  requirements, and risk.
- Prefer lower-cost models for straightforward, well-specified implementations.
- Recommend a more capable model when complexity or architectural risk justifies it.
- Briefly explain the recommendation, including any relevant cost/capability trade-offs.
- Never select the implementation model on my behalf. Wait for my explicit choice.
- Once selected, delegate implementation to a subagent using that model and provide it with the approved plan, relevant
  repository context, constraints, and acceptance criteria.
- If model-specific delegation is unavailable, explain the limitation rather than silently using a different model.

# Code Review

After every implementation, always spawn a separate code-review subagent before considering the task complete.

- The reviewer must be independent of the implementation subagent.
- Review the actual changes against the approved plan, requirements, and repository conventions.
- Check for correctness, bugs, regressions, security issues, edge cases, maintainability, and missing tests.
- Prioritize actionable findings by severity and include file and line references.
- Do not modify code during the review. The reviewer should report findings independently.
- If issues are found, delegate fixes to the implementation agent and request another independent review of the fixes.
- Repeat until no blocking findings remain, or report unresolved issues explicitly.
- Summarize the implementation, validation results, review findings, and any remaining risks.

Never skip the review, even for small changes. If subagent spawning is unavailable, explicitly disclose that the
required independent review could not be performed.

### Surgical Changes

**Make only the changes necessary to complete the task.** Do not modify unrelated code.

When editing existing code:

- Do not improve unrelated code, comments, or formatting.
- Do not refactor code that is not broken.
- Match the existing style, even if you would otherwise prefer a different approach.
- If you discover unrelated dead code, mention it without removing it.

When your changes leave code unused:

- Remove imports, variables, and functions made unused **by your changes**.
- Do not remove pre-existing dead code unless explicitly asked.

### After Making Changes

- Summarize what changed and why.
- Report which tests and checks were executed and their results.
- Mention any known limitations or unresolved issues.
- Do not claim functionality was verified without evidence.
- Do not commit or push unless explicitly requested.
