# readerapp — Architecture & Data Flow

This README documents the architecture, responsibilities, and data flow for the `readerapp` Android codebase. It references concrete files in the repository and explains how components depend on each other, which parts are decoupled vs tightly coupled, and the runtime flow of data and control.

High-level summary
- Pattern: ViewModel-based UI with unidirectional data flow — commonly called “Unidirectional MVVM” or MVI-like. Views (Composables) emit intents (function calls) to ViewModels; ViewModels own immutable `UiState` (exposed as `StateFlow`) and emit transient UI effects via `SharedFlow`.
- Single source of truth: Per-screen `UiState` objects (data classes) stored in ViewModels.
- Data sources: `Repository` layer mediates network I/O (Retrofit) and local stores.
- Token & auth: `TokenProvider` centralizes token access; OkHttp `Authenticator` coordinates refresh flows.

Checklist (what this README covers)
- Overview of key packages and files
- Concrete data flow diagrams (UI → VM → Repo → Network → Repo → VM → UI)
- File-level responsibilities and dependencies
- Coupling / decoupling analysis and recommendations
- Auth/token flow and concurrency handling
- Short notes on how to extend or change behavior


1) Key packages and representative files
- `ui/` — Jetpack Compose UI screens and components
  - `app/src/main/java/com/storystream/reader_app/ui/screens/TrendingScreen.kt`
  - `app/src/main/java/com/storystream/reader_app/ui/screens/LoginScreen.kt`
  - `app/src/main/java/com/storystream/reader_app/MainActivity.kt` (hosts screens)

- `viewmodel/` — ViewModels that own UI state and handle intents
  - `app/src/main/java/com/storystream/reader_app/viewmodel/TrendingViewModel.kt`
  - `app/src/main/java/com/storystream/reader_app/ui/viewmodel/AuthViewModel.kt`
  - (other VMs: `InsightsViewModel`, `ArticleDetailViewModel`, `SavedViewModel` referenced in `MainActivity`)

- `repository/` — Data access layer (network/local coordination)
  - `app/src/main/java/com/storystream/reader_app/repository/ArticlesRepository.kt`
  - `app/src/main/java/com/storystream/reader_app/repository/AuthRepository.kt`
  - `app/src/main/java/com/storystream/reader_app/repository/AuthStateHolder` (singleton StateFlow of auth state)

- `network/` — Retrofit APIs, OkHttp interceptors and authenticators
  - `app/src/main/java/com/storystream/reader_app/network/ArticlesApi.kt` (Retrofit interface)
  - `app/src/main/java/com/storystream/reader_app/network/AuthApi.kt`
  - `app/src/main/java/com/storystream/reader_app/network/AuthInterceptor.kt` (adds Authorization header)
  - `app/src/main/java/com/storystream/reader_app/network/TokenAuthenticator.kt` (refreshes tokens on 401)
  - `app/src/main/java/com/storystream/reader_app/network/TokenInterceptor.kt` (exists in repo but DI currently uses `AuthInterceptor`)
  - `app/src/main/java/com/storystream/reader_app/network/RefreshApi.kt` / `RefreshModels.kt`

- `data/` — DTOs, models and small local utilities
  - `app/src/main/java/com/storystream/reader_app/data/Models.kt` (domain models like `ArticleResponse`)
  - `app/src/main/java/com/storystream/reader_app/data/UserSession.kt` (decode token claims)
  - `app/src/main/java/com/storystream/reader_app/data/TokenProvider.kt` and `TokenProviderImpl.kt`
  - `app/src/main/java/com/storystream/reader_app/data/SecureTokenStore.kt` (persistence — referenced across code)
  - `app/src/main/java/com/storystream/reader_app/data/SavedRefreshManager.kt` (SharedFlow signal)
  - `app/src/main/java/com/storystream/reader_app/data/LocalStore.kt` (local fallbacks / no-ops)

- `di/` — Hilt modules wiring singletons
  - `app/src/main/java/com/storystream/reader_app/di/DiNetworkModule.kt` (OkHttp, Retrofit, APIs, authenticators)
  - `app/src/main/java/com/storystream/reader_app/di/TokenProviderModule.kt` (provides `TokenProvider` binding)

- `auth/` — small helpers for auth state reaction
  - `app/src/main/java/com/storystream/reader_app/auth/AuthStateObserver.kt` (observes `TokenProvider.tokenFlow` and updates `AuthStateHolder`)


2) Concrete data & control flow (typical UI interaction)
Example: User opens Trending screen and saves an article

1. Composition & initial load
   - `MainActivity` composes `TrendingScreen` and creates/obtains `TrendingViewModel` using `viewModel()`.
   - `TrendingScreen` uses `val uiState by viewModel.uiState.collectAsStateWithLifecycle()` to observe state.
   - On first composition `LaunchedEffect(Unit)` calls `viewModel.loadTrending()`.

2. Intent handling in ViewModel
   - `TrendingViewModel.loadTrending()` checks `_uiState.value.loading`, sets `_uiState` to loading via `_uiState.update { it.copy(loading = true) }` and launches a coroutine.
   - The ViewModel calls `repo.getTrending(limit)` on the `ArticlesRepository` using an injected `ioDispatcher` (decouples threading).

3. Repository & network I/O
   - `ArticlesRepository.getTrending` calls the Retrofit `ArticlesApi` (provided by DI in `DiNetworkModule`) which executes network I/O.
   - OkHttp interceptors/authenticator may add `Authorization` headers or trigger refresh when necessary (`AuthInterceptor`, `TokenAuthenticator`).

4. Back to ViewModel & UI
   - The repository returns a `Result<List<ArticleResponse>>`. The ViewModel updates `_uiState` immutably using `.copy(...)` to set `trendingArticles` and `loading=false`.
   - The `TrendingScreen` recomposes because it is observing `uiState`.

5. One-shot events
   - If the user presses save on an article, `ArticleCard` calls the passed `onSave` -> `viewModel.saveArticle(id)`.
   - `TrendingViewModel.saveArticle` calls `repo.saveArticle(id)`, triggers `SavedRefreshManager.triggerRefresh()` and emits a `TrendingEvent` into `_events` (a `MutableSharedFlow`).
   - `TrendingScreen` collects `viewModel.events` and shows a snackbar when `TrendingEvent.SaveOk` arrives.


3) Auth / token flow (special case, global state & side-effects)
- Token storage & provider
  - `SecureTokenStore` (concrete persistence) stores raw tokens.
  - `TokenProvider` interface (`TokenProvider.kt`) exposes synchronous reads (`getToken()`) for OkHttp interceptors and a `tokenFlow: StateFlow<String?>` for observers.
  - `TokenProviderImpl` (`TokenProviderImpl.kt`) keeps a memory-backed `MutableStateFlow` synchronized with `SecureTokenStore` — this decouples consumers from direct store access.

- Interceptors & authenticator
  - `DiNetworkModule` wires `AuthInterceptor` (an `Interceptor`) into the main `OkHttpClient`. `AuthInterceptor` uses `tokenProvider.getToken()` synchronously to append `Authorization` headers.
  - `TokenAuthenticator` handles 401 responses and performs a single-flight refresh against `RefreshApi` (dedupes concurrent refreshes using locks and wait/notify). It calls `tokenProvider.updateTokens(...)` on success.
  - Note: `TokenInterceptor.kt` exists in the codebase, but `DiNetworkModule` currently provides `AuthInterceptor` to OkHttp — check for duplication if you change interceptors.

- Auth state observable
  - `AuthRepository` calls `tokenProvider.updateTokens(...)` on login/register which updates both persisted store and the in-memory `tokenFlow`.
  - `AuthStateObserver` subscribes to `tokenProvider.tokenFlow` and updates `AuthStateHolder` (a `MutableStateFlow` singleton) so UI host (`MainActivity`) can react to auth state changes.


4) File-level responsibilities and dependencies (quick map)
- UI (`ui/...`)
  - Responsibility: Render based on `UiState`, send user intents to ViewModel, display one-shot events (snackbars). Examples: `TrendingScreen.kt`, `LoginScreen.kt`.
  - Depends on: ViewModels (via `viewModel()`), Compose runtime, theme/components.
  - Coupling: Loose coupling to ViewModel (observes `StateFlow` and calls exposed methods). UI does not mutate VM state directly.

- ViewModel (`viewmodel/...`)
  - Responsibility: Own per-screen `UiState` (data class), translate intents → domain calls, orchestrate repository calls, expose `StateFlow` + `SharedFlow` for events.
  - Depends on: `Repository` interfaces, coroutine dispatchers (injected), `SavedRefreshManager` for cross-screen signaling.
  - Coupling: Medium — VMs depend on repository implementations via constructor injection (Hilt); however they depend only on repo interfaces or single concrete repository classes.

- Repository (`repository/...`)
  - Responsibility: Stitch network and local storage, return `Result<T>` typed responses, hold small client-side logic for mapping network responses.
  - Depends on: Retrofit APIs (`network`), data models (`data`), `TokenProvider` indirectly via network interceptors.
  - Coupling: Decoupled from UI by the ViewModel layer; depends on Retrofit interfaces provided by DI.

- Network (`network/...`)
  - Responsibility: Define Retrofit interfaces, provide OkHttp interceptors and an `Authenticator` for token refresh.
  - Depends on: `TokenProvider` (for synchronous token reads and updates), `RefreshApi` (for refreshing).
  - Coupling: Decoupled from higher layers by DI; tightly coupled to token storage via `TokenProvider` contract.

- Data (`data/...`)
  - Responsibility: DTOs/Models (`ArticleResponse`, `ReadingInsightsResponse`), token persistence (`SecureTokenStore`), in-memory providers (`TokenProviderImpl`), small signaling utilities (`SavedRefreshManager`).
  - Depends on: low-level Android APIs for persistence (in `SecureTokenStore`) and `Base64/JSON` decoding in `UserSession`.
  - Coupling: `TokenProviderImpl` couples to `SecureTokenStore` for persistence, but consumers are decoupled behind the `TokenProvider` interface.

- DI (`di/...`)
  - Responsibility: Provide singleton instances for `OkHttpClient`, `Retrofit`, `Apis`, `TokenProvider`, `Dispatchers` etc.
  - Files: `DiNetworkModule.kt`, `TokenProviderModule.kt`, `CoroutineModule.kt`.
  - Coupling: Central coupling point (intentionally) — changes here ripple to consumers.

- Auth helpers (`auth/...`)
  - `AuthStateObserver.kt` watches `TokenProvider.tokenFlow` and updates `AuthStateHolder`.
  - This creates a global reactive link between token changes and application-level auth state.


5) Coupling / decoupling analysis

Strongly decoupled
- UI ⟷ ViewModel: decoupled by `StateFlow` + `SharedFlow` (UI observes immutable state and events; UI only calls public VM methods).
- Repositories ⟷ Network: decoupled via Retrofit interfaces and Hilt-provided instances (`DiNetworkModule`). Repos don't construct OkHttp clients.
- Interceptors/Authenticator ⟷ callers: decoupled via the `TokenProvider` interface instead of direct store access.

Tightly coupled or worth noting
- `TokenProviderImpl` → `SecureTokenStore`: `TokenProviderImpl` uses `SecureTokenStore` directly. This is acceptable; `TokenProvider` is the abstraction used by consumers, but the concrete provider is coupled to the storage implementation.
- `AuthStateHolder` global singleton: global mutable StateFlow (convenient, but increases coupling across the app). `MainActivity` and other consumers read it directly; alternative could be a small `AuthRepository` facade.
- `DiNetworkModule` centrally wires OkHttp interceptors. Mistakes there (providing `AuthInterceptor` vs `TokenInterceptor`) can lead to duplication; maintain a single interceptor approach.
- `TokenAuthenticator` synchronizes refresh via internal `lock` and wait/notify. This is imperative and correct for single-flight refresh, but it's a non-trivial concurrency hotspot.


6) Why this is “Unidirectional MVVM” / MVI-like
- ViewModels expose a single `uiState: StateFlow<...>` per screen (single source of truth).
- UI reads state and renders; UI emits actions as calls to methods on the ViewModel. The View never writes state directly.
- One-shot effects are emitted via `SharedFlow` so they don't pollute persistent screen state.
- There is no two-way data binding; instead immutable `UiState` objects are updated via `.copy(...)` in ViewModel — classic MVI characteristic.
- Not strictly MVI: there isn't a central reducer or a dedicated `Intent`/`Reducer` layer. ViewModel methods act as intent handlers and update state directly. Hence the accurate label: Unidirectional MVVM (MVI-like).


7) Concrete file dependency graph (simplified)
- `MainActivity` → observes `AuthStateHolder.authState` and creates VMs via `viewModel()`
- `TrendingScreen` → observes `TrendingViewModel.uiState`, collects `TrendingViewModel.events`, calls `TrendingViewModel.loadTrending()` / `saveArticle()`
- `TrendingViewModel` → depends on `ArticlesRepository`, `ioDispatcher` (injected)
- `ArticlesRepository` → uses `ArticlesApi` (Retrofit interface) provided by `DiNetworkModule`
- `DiNetworkModule` → constructs `OkHttpClient` with `AuthInterceptor` & `TokenAuthenticator` and creates `Retrofit` → provides `ArticlesApi` and `AuthApi`
- `AuthInterceptor` / `TokenAuthenticator` → depends on `TokenProvider` interface
- `TokenProviderImpl` → depends on `SecureTokenStore` (concrete persistence)
- `AuthRepository` → uses `AuthApi` and `TokenProvider`; updates `AuthStateHolder` on successful login/register
- `AuthStateObserver` → watches `TokenProvider.tokenFlow` and updates `AuthStateHolder`


8) Recommendations & notes for maintainers
- Keep `TokenProvider` as the single abstraction for token access. Avoid scattered direct calls to `SecureTokenStore` — prefer `tokenProvider.updateTokens(...)` so the in-memory flow stays consistent.
- Consider replacing `AuthStateHolder` global with a small `AuthRepository` facade (already exists, but it uses `AuthStateHolder` directly) or provide an injectable `AuthState`-exposing interface to reduce global read dependency.
- Consolidate interceptors: remove unused `TokenInterceptor.kt` if the app uses `AuthInterceptor` consistently to avoid duplication or confusion.
- Add unit tests for `TokenAuthenticator` concurrency behavior (simulate concurrent 401 responses) and for ViewModel state transitions (happy + error paths). ViewModels are easy to unit test because they expose deterministic `StateFlow`.
- Document the DI wiring in `DiNetworkModule` clearly to show which client uses the `AuthInterceptor` and which uses the `RefreshClient` (no auth interceptor) for token refresh.


9) Quick README blurb (one-liner you can paste)
"This app uses a ViewModel-based architecture with unidirectional data flow: composables dispatch intents to ViewModels, ViewModels update immutable `UiState` (exposed as `StateFlow`) and emit one-shot effects via `SharedFlow`; repositories handle data access via Retrofit and DI, and a central `TokenProvider` + `TokenAuthenticator` manage authentication lifecycle."


Appendix: Important files to read first (for new contributors)
1. `app/src/main/java/com/storystream/reader_app/viewmodel/TrendingViewModel.kt` — pattern for UiState + events
2. `app/src/main/java/com/storystream/reader_app/ui/screens/TrendingScreen.kt` — how UI observes state and emits intents
3. `app/src/main/java/com/storystream/reader_app/repository/ArticlesRepository.kt` — repository responsibilities and error handling
4. `app/src/main/java/com/storystream/reader_app/di/DiNetworkModule.kt` — networking DI wiring
5. `app/src/main/java/com/storystream/reader_app/network/TokenAuthenticator.kt` — token refresh single-flight logic
6. `app/src/main/java/com/storystream/reader_app/data/TokenProvider.kt` + `TokenProviderImpl.kt` — abstraction for token access


If you'd like, I can:
- Convert this to a shorter README section suitable for the repo root (trimmed to ~8–12 lines).
- Open a PR that updates `DiNetworkModule` comments and removes duplicate interceptors, or add a small architectural diagram file.

Which follow-up would you prefer?
