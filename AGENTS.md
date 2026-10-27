# AGENTS.md

This file provides guidance to Claude (claude.com/claude-code) and Codex (openai.com/codex) when working with code in
this repository.

## Overview

See `@README.md` to get an overview of the product and the tech stack used in this project.

## UI & UX

Before planning, implementing, or reviewing UI, UX, themes, Compose
components, user-facing content, or accessibility:

1. Read `DESIGN.md` completely.
2. Treat its YAML tokens as normative.
3. Follow its Markdown guidance for applying those tokens.

Do not load `DESIGN.md` for unrelated tasks.

> NOTE: The code must be the source of truth of the design system. So, if you see a component is using a different
> color, spacing, typography, etc. You need to update the `DESIGN.md`, not the code.

## Architecture

- **UI**: Jetpack Compose as the **UI Toolkit** using **MVI**..
- **State**: Unidirectional Data Flow with Kotlin Coroutines and `Flow`; every `ViewModel` exposes exactly one immutable
  `StateFlow<XState>` named `stateFlow`, plus a `state` getter.
- **Navigation**: Navigation is handled by Jetpack Compose Navigation 3. Each screen must be defined as a key from
  `Route` sealed interface.
- **DI**: Dependency injection is handled by `Koin`.
- **Data**: Data is provided in three different ways:
    - **Room**: For structured and queryable persistent data. See **Offline-First Architecture** section.
    - **DataStore**: Async key-value storage for lightweight data such as bearer tokens, local preferences, etc.
    - **Ktor**: Lightweight, asynchronous tool for making network requests to **Mipyme's API** and external services.

## Coding Style

* Prefer `val`; use `var` only when necessary.
* When declaring functions, use block body instead of expression body.
* Use `camelCase` for variables/functions, `PascalCase` for classes, and `UPPER_SNAKE_CASE` for constants.
* Use lowercase package names; separate multi-word package names with underscores (for example, `getting_started`).
* Use `kotlinx.coroutines`; avoid `Thread`, `AsyncTask`, and `GlobalScope`.
* Use `Flow` for reactive streams.
* Prefer named, feature/domain-specific Flow functions over large anonymous `ViewModel` pipelines or generic transformer
  utilities.
* Represent UI state and events with sealed classes or data classes.
* Update `MutableStateFlow` with `update`.
* Every independent screen should have a `ViewModel`; workflow screens may share one when they share UI state.
* Use `viewModelScope` or `lifecycleScope`.
* Avoid throwing from coroutines; use `Result` or Arrow’s `Either`.

## Screen Constraints

Each screen must follow these constraints:

* Every screen must have a `ViewModel`.
* A screen-specific `ViewModel` owns the presentation logic and UI state of that screen.
* A `ViewModel` must not directly depend on another `ViewModel`.

Choose the state-sharing strategy based on the screen design:

### Shared ViewModel

Use one shared `ViewModel` when multiple screens are part of the same workflow and behave as one presentation unit.

For example:

```text
CheckoutScreen
    ↓
CartDetailScreen
    ↓
ConfirmPaymentScreen
```

These screens may share one `CheckoutViewModel` when:

* They operate on the same UI state.
* Their logic belongs to the workflow as a whole.
* They do not require independent screen-specific presentation logic.

The shared `ViewModel` must be scoped to the workflow.

### Screen ViewModels with a Store

Use one `ViewModel` per screen and a shared `Store` when the screens belong to the same workflow but each screen has its
own presentation logic.

For example:

```text
CheckoutStore
├── CheckoutViewModel
├── CartDetailViewModel
└── ConfirmPaymentViewModel
```

In this design:

* Each screen owns its own `ViewModel`.
* Each `ViewModel` owns only the UI state and behavior of its screen.
* The `Store` owns the mutable state shared across the workflow.
* Every participating `ViewModel` receives the same `Store` instance.
* The `Store` must be scoped to the workflow and released when the workflow ends.

### Decision Rule

Use a shared workflow `ViewModel` when the screens form one presentation unit.

Use one `ViewModel` per screen with a shared `Store` when the screens require independent presentation logic but still
need to share mutable workflow state.

## Single Source of Truth

It's important that each service defines a single source of truth. The source of truth always contains data that is
consistent, correct, and up-to-date. In fact, the data exposed from the repository should always be the data coming
directly from the source of truth.

The source of truth can be a data source—for example, the database—or even an
in-memory cache that the repository might contain. Repositories combine different data sources and solve any potential
conflicts between the data sources to update the single source of truth regularly or due to a user input event.

Different repositories in your app might have different sources of truth. For example, the `LoginRepository` class might
use its cache as the source of truth and the `PaymentsRepository` class might use the network data source.

In order to provide offline-first support, a local data source—such as a database—is the recommended source of truth.

## Offline-First Architecture

The application is designed with an offline-first architecture in mind. We don't want users to stop selling or taking
orders when their devices have lost internet connection during a service outage. However, it comes with tradeoffs:

1. Most of the write operations are locked. Except for the selling or issuing a refund. These operations must be queued
   and once the internet connection is restored, a background service must be fired to upload these orders.
2. None of the data gets updated when the app is in offline mode. Even if the user takes an order, it shouldn't modify
   the current quantity, report or any other information.

> **NOTES**: When sending orders from the queue, the server ignores the `allowBackOrders` property to avoid losing a
> sell since the order was actually made but the merchant couldn't adjust the inventory.

## Threading

Calling data sources and repositories should be main-safe—safe to call from the main thread. These classes are
responsible for moving the execution of their logic to the appropriate thread when performing long-running blocking
operations. For example, it should be main-safe for a data source to read from a file, or for a repository to perform
expensive filtering on a big list.

Note that most data sources already provide main-safe APIs like the suspend method calls provided by Room, Retrofit or
Ktor. Your repository can take advantage of these APIs when they are available.

## Git & Commits

* Use Conventional Commit messages (e.g. `fix(sync): …`, `docs(design): …`), matching the existing history.
* Do **not** append `Co-Authored-By` or other agent attribution trailers — commit with the message only.
* Commit or push only when asked.

## Behavioral Guidelines

### Planning

Interview me relentlessly about every aspect of this plan until we reach a shared understanding. Walk down each branch
of the design tree, resolving dependencies between decisions one-by-one. For each question, provide your recommended
answer.

Ask the questions one at a time, waiting for feedback on each question before continuing. Asking multiple questions at
once is bewildering.

If a fact can be found by exploring the codebase, look it up rather than asking me. The decisions, though, are mine —
put each one to me and wait for my answer.

- Do not enact the plan until I confirm we have reached a shared understanding.
- Do not commit implementation plans, research notes, or agent scratch files. Keep temporary working material outside the worktree. `.plans/` is gitignored only as a safety net for legacy tooling.

### Surgical Changes

Touch only what you must. Clean up only your own mess. When editing existing code:

- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:

- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.
