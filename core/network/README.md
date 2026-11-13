# Mipyme API client

`:core:network` is the Ktor client for the Mipyme backend (`mipyme-ws`). It is a pure
Kotlin/JVM module that reuses the wire types in `:core:contract`.

`openapi.json` is an unchanged copy of `mipyme-ws/src/openapi.json`, taken from the backend
checkout at commit `fd30838`. It documents validation, authentication, error codes,
pagination, and retry rules. The tests read their response examples from it and check that
the client covers every operation it describes.

## Using the client

```kotlin
val api = MipymeApi(
    baseUrl = "https://api.example.com/",
    tokenProvider = AuthTokenProvider { token },
    logger = logger, // optional Ktor `Logger`; defaults to no logging
)
val merchant: Either<NetworkError, Merchant> = api.accounts.onboard(request, idempotencyKey)
```

- Resource groups: `accounts`, `locations`, `products`, `collections`, `customers`,
  `suppliers`, `purchases`, `sales`, `sync`, and `realtime`. `isHealthy()` calls the public
  `/_health` endpoint without a bearer token.
- Every call returns `Either<NetworkError, T>`. Server errors arrive as
  `NetworkError.Response` with the `MipymeError` payload. `Timeout`, `Serialization`,
  `NetworkUnavailable`, and `Unexpected` carry the cause. Cancellation is rethrown.
- `AuthTokenProvider` runs before every authenticated request. It must not depend on
  `:core:auth`; the app adapts its token provider.
- Wire format: `snake_case` JSON through `JsonNamingStrategy.SnakeCase`, with
  `encodeDefaults = false`. A request property left at its contract default is therefore
  omitted, which matches the server default. `OptionalField.Absent` omits a key and
  `OptionalField.Present(null)` sends `null`.
- Create, onboard, and void methods accept an optional `idempotencyKey` sent as the
  `Idempotency-Key` header. Reuse the same key when retrying the same operation. The client
  never retries mutations.
- `networkModule(baseUrl)` registers a single `MipymeApi` and closes it with Koin. It
  resolves `AuthTokenProvider` and Ktor's `Logger` from the container.
- `accounts.currentUser()` returns the identity claims from `GET /me`
  (`JsonWebTokenPayload`), not an onboarding or merchant-membership decision.

## Updating the contracts

1. Copy the backend's `src/openapi.json` over `openapi.json` and update the commit above.
2. Compare the backend routes and contracts, then update `:core:contract` and the API
   classes in the same change.
3. Run `./gradlew :core:network:test`.

The tests use Ktor's `MockEngine`, so they need no running backend.
