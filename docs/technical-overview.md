# PitchSense: Technical Overview

---

## 1. What the App Does

PitchSense is an Android application designed to serve as in-game decision support for pitching staff. Given a batter–pitcher matchup, it presents four analytical views:

1. **Overview** — season-level and matchup-specific batting statistics
2. **Advanced Stats** — Statcast-style metrics (xwOBA, whiff rates, exit velocity, etc.)
3. **Heat Map** — a 5×5 strike-zone grid color-coded by batting performance (BA, SLG, or OPS)
4. **Pitch Sequence Recommender** — a ML-driven recommendation engine that suggests the optimal sequence of pitches given a specific game situation

Data is fetched from a REST backend running locally on the host machine. When the backend is unavailable, the app falls back to hardcoded demo data so the UI is always functional.

---

## 2. Tech Stack

| Layer | Technology | Description |
|---|---|---|
| Language | Kotlin 2.2.10 | Primary language for all Android source code |
| UI Framework | Jetpack Compose + Material 3 | Android's modern declarative UI toolkit; components are written as Kotlin functions rather than XML layouts |
| Navigation | Navigation Compose | Jetpack library that manages screen transitions and the back stack within a Compose app |
| State Management | ViewModel + `StateFlow` + Coroutines | Android architecture components for holding UI state across recompositions and running asynchronous work (network calls) off the main thread |
| Networking | Retrofit 2.11 + OkHttp | Retrofit is a type-safe HTTP client that turns a Kotlin interface into real API calls; OkHttp is the underlying HTTP engine that handles connections, timeouts, and request/response logging |
| JSON Serialization | Moshi 1.15 + `KotlinJsonAdapterFactory` | Moshi converts JSON responses from the backend into Kotlin data classes; `KotlinJsonAdapterFactory` adds reflection-based support for Kotlin-specific features like default parameter values and nullability |

---

## 3. Architecture

The app follows a **single-activity MVVM** pattern with a **Repository** abstraction layer and **unidirectional data flow**.

```
Composable Screens (stateless)
      ↑  collectAsState()
PitchSenseViewModel         ← single shared ViewModel
      ↓
PitchSenseRepository (interface)
      ↓                         ↓
RemotePitchSenseRepository   FakePitchSenseRepository
  (Retrofit → backend)         (hardcoded demo data)
      ↓
PitchSenseApiService (Retrofit interface)
```

### Key design decisions

**Single ViewModel for all screens.** One `PitchSenseViewModel` instance is created in `NavGraph.kt` and passed down to every screen. All state is centralised in a single `PitchSenseUiState` data class, making cross-screen state sharing trivial (e.g. the batter selected on the Overview screen is automatically reflected on the Heat Map screen).

**Stateless composables.** Every screen composable is a pure function of its inputs — it holds no local state and issues no API calls directly. All business logic lives in the ViewModel.

**Manual dependency injection.** No DI framework (no Hilt or Koin). Dependencies are wired through constructor defaults: `PitchSenseViewModel` defaults to `RepositoryProvider.create()`, which reads a `BuildConfig` flag to choose the real or fake repository.

---

## 4. Project Structure

```
app/src/main/java/com/example/pitchsense/
├── MainActivity.kt
├── ui/
│   ├── navigation/NavGraph.kt         — NavHost, routes, ViewModel instantiation
│   ├── viewmodel/PitchSenseViewModel.kt
│   ├── state/PitchSenseUiState.kt     — immutable single source of truth
│   ├── screens/
│   │   ├── LoginScreen.kt
│   │   ├── OverviewScreen.kt
│   │   ├── AdvancedStatsScreen.kt
│   │   ├── HeatMapScreen.kt
│   │   └── PitchSequenceScreen.kt
│   ├── components/
│   │   ├── CommonUi.kt                — HeaderBar, StatCard, ToggleButton, etc.
│   │   └── ScreenScaffold.kt          — shared wrapper for detail screens
│   └── theme/                         — Material3 color/type/theme definitions
└── data/
    ├── model/PitchSenseModels.kt      — domain models (no serialization)
    ├── remote/
    │   ├── api/PitchSenseApiService.kt — Retrofit interface
    │   ├── api/ApiClient.kt            — OkHttp + Retrofit factory
    │   └── model/PitchSenseApiModels.kt — Moshi DTOs
    └── repository/
        ├── PitchSenseRepository.kt     — interface
        ├── RemotePitchSenseRepository.kt
        ├── FakePitchSenseRepository.kt
        └── RepositoryProvider.kt       — build-time switch
```

---

## 5. Data Flow in Detail

### 5.1 Build-time flag

Two Gradle properties control which data source the app uses:

| Property | Default | Effect |
|---|---|---|
| `PITCHSENSE_USE_REMOTE_API` | `false` | `true` → live backend; `false` → demo data only |
| `PITCHSENSE_API_BASE_URL` | `http://10.0.2.2:8080/api/v1/` | Backend URL; `10.0.2.2` is the Android emulator's alias for `localhost` on the host machine |

These are injected into `BuildConfig` at compile time by `app/build.gradle.kts`.

`RepositoryProvider.kt` reads `BuildConfig.USE_REMOTE_API` once at startup:

```kotlin
// RepositoryProvider.kt
fun create(): PitchSenseRepository {
    if (!BuildConfig.USE_REMOTE_API) return FakePitchSenseRepository()
    return RemotePitchSenseRepository(api = ApiClient.create())
}
```

### 5.2 Optimistic fallback strategy

For the Overview, Advanced Stats, and Heat Map screens, the ViewModel uses an **optimistic fallback** pattern:

1. **Immediately** populate the UI with `FakePitchSenseRepository` data (synchronous, zero latency) so the screen never appears blank.
2. **Concurrently** issue the real API call.
3. On **success** → replace fake data with live data, set `isOffline = false`.
4. On **exception** → keep the fake data visible, set `isOffline = true` (triggers the amber "Offline — showing demo data" banner).

```
Screen appears  →  fake data shown instantly
                →  API call starts in background
                        ↓ success: live data replaces fake; offline banner hides
                        ↓ failure: fake data stays; amber banner appears
```

The Pitch Sequence screen is the **exception**: it shows a spinner (no fake intermediate result) because displaying a fake pitch recommendation would be misleading to the user. The sequence is only shown after the backend responds.

### 5.3 Guard flags

Three boolean flags on the ViewModel — `hasLoadedOverview`, `hasLoadedAdvanced`, `hasLoadedHeatMap` — prevent re-fetching on recomposition or back-navigation. Each is set to `true` on the first load and is not reset on error, so a failed load does not automatically retry.

---

## 6. State Management

### PitchSenseUiState

`ui/state/PitchSenseUiState.kt` is an **immutable data class** that is the single source of truth for all UI state. The ViewModel exposes it as a `StateFlow<PitchSenseUiState>`; screens observe it with `collectAsState()` and call ViewModel methods (never mutate state directly).

Selected fields and their purpose:

| Field | Type | Purpose |
|---|---|---|
| `selectedBatterId` | `String` | MLBAM player ID sent to the API |
| `selectedBatter` | `String` | Display name shown in the dropdown |
| `selectedPitcherId` | `String` | Pitcher MLBAM ID; empty string = no pitcher |
| `selectedHeatMetric` | `String` | One of `"BA"`, `"SLG"`, `"OPS"` |
| `balls`, `strikes`, `outs`, `inning` | `String` | Live (editable) pitch sequence form controls |
| `generatedBalls`, ... | `String` | Snapshot of the form at the moment "Generate" was pressed — these drive the actual API call and are what the results pane reflects |
| `isPitchSequenceLoading` | `Boolean` | Shows `CircularProgressIndicator` on the sequence screen |
| `isOffline` | `Boolean` | Shows the amber offline banner on any screen |
| `heatMap` | `List<List<HeatMapCell>>` | Always 5×5; enforced by the repository |
| `recommendedSequence` | `List<SequenceRecommendation>` | Ordered pitch steps returned by the backend |

The separation between `balls`/`strikes`/`outs` (live form) and `generatedBalls`/`generatedStrikes`/`generatedOuts` (snapshot) means the user can change the form inputs without immediately invalidating the current results — the results only update when "Generate Updated Sequence" is pressed.

---

## 7. API Layer

### 7.1 Retrofit Interface

`data/remote/api/PitchSenseApiService.kt` declares four `suspend` functions:

| Method | Endpoint | Key Parameters |
|---|---|---|
| `GET` | `/overview/stats` | `batterId`, `pitcherId?`, `season` |
| `GET` | `/advanced/stats` | `batterId`, `pitcherId?`, `season` |
| `GET` | `/heatmap` | `batterId`, `metric`, `season` |
| `POST` | `/pitch-sequence/recommend` | JSON body with batter/pitcher IDs, game situation |

All are `suspend` functions, called from `viewModelScope.launch` coroutines. The season is always pinned to `2025`.

### 7.2 HTTP Client

`data/remote/api/ApiClient.kt` configures OkHttp with:
- **3-second connect timeout** and **5-second read timeout** — deliberately short so the app reaches the fallback path quickly rather than blocking on OkHttp's 10-second default
- `HttpLoggingInterceptor` at `Level.BASIC` for debug HTTP logging (method, path, response status)

### 7.3 DTOs vs. Domain Models

The codebase maintains a strict separation between two model layers:

- **DTOs** (`data/remote/model/PitchSenseApiModels.kt`) — Moshi-annotated data classes that mirror the JSON wire format exactly. They contain raw `Double` values, enum strings as received from the API, and nullable fields matching the API contract.
- **Domain models** (`data/model/PitchSenseModels.kt`) — clean Kotlin data classes used by the ViewModel and screens. No serialization annotations, no networking concerns.

`RemotePitchSenseRepository` is the only place where DTOs are mapped to domain models. This keeps the UI layer entirely decoupled from the API contract.

### 7.4 Response Validation

`RemotePitchSenseRepository` validates the heat map grid dimensions before caching:

```kotlin
if (response.grid.size != 5 || response.grid.any { it.size != 5 }) {
    throw IllegalStateException("Heat map response is not a 5×5 grid")
}
```

This exception propagates to the ViewModel's catch block, setting `isOffline = true` and keeping the fallback data visible.

### 7.5 Caching in the Repository

`RemotePitchSenseRepository` implements two lightweight in-memory caches to avoid redundant API calls:

- `advancedCache: Pair<String, AdvancedStatsResponseDto>?` — keyed by `batterId`. All four advanced stat methods (`advancedSummaryStats`, `advancedDisciplineStats`, `advancedBattedBallStats`, `pitchTypeStats`) call a shared `fetchAdvancedResponse(batterId)` function that checks the cache before making a network call.
- `heatMapCache: Triple<String, String, HeatMapResponseDto>?` — keyed by `(metric, batterId)`. Different metrics require separate API calls, so both must be part of the cache key.

These caches live for the lifetime of the `RemotePitchSenseRepository` instance (which is the lifetime of the `PitchSenseViewModel`, scoped to the `NavHost` composable).

---

## 8. Navigation

`ui/navigation/NavGraph.kt` defines a `NavHost` with five routes:

| Route | Screen | Notes |
|---|---|---|
| `"login"` | `LoginScreen` | Start destination. "Enter" navigates to Overview and removes Login from the back stack (`popUpTo("login") { inclusive = true }`), so back-press exits the app rather than returning to Login. |
| `"overview"` | `OverviewScreen` | `LaunchedEffect(Unit)` triggers `vm.onOverviewVisible()` on first composition. |
| `"advanced_stats"` | `AdvancedStatsScreen` | Same pattern — `vm.onAdvancedVisible()` on entry. |
| `"heat_map"` | `HeatMapScreen` | `vm.onHeatMapVisible()` on entry. |
| `"pitch_sequence"` | `PitchSequenceScreen` | No `LaunchedEffect`; data is purely user-triggered. |

The single `PitchSenseViewModel` instance is created once at the `NavHost` level (`viewModel()` in `PitchSenseApp()`) and is passed down as a parameter to every screen composable. This ensures all screens share one state object.

---

## 9. Screen Descriptions

### LoginScreen
A simple splash entry screen with the PitchSense logo and an "Enter" button. No authentication is implemented in this version.

### OverviewScreen
The primary dashboard. Displays batter and pitcher dropdowns with roster metadata:
- Batters are sorted by lineup order (1–9) and displayed with a numeric prefix
- Pitchers are grouped by role (Starter / Reliever / Closer) with section headers in the dropdown

When no pitcher is selected, only the "General" stat column is shown. Selecting a pitcher adds a "Pitcher Specific" column showing matchup stats side by side.

Player headshots are resolved from drawable resources using a slug derived from the player name (Unicode normalization, lowercase, non-alphanumeric characters replaced with underscores). An initials fallback is shown when no matching drawable is found.

Navigation buttons at the bottom link to the three detail screens.

### AdvancedStatsScreen
Displays Statcast-style metrics in two sections:
1. A 3-column grid of 12 key metrics (xwOBA, xBA, xSLG, Hard Hit%, Barrel%, Avg EV, Max EV, Sweet Spot%, Chase%, Whiff%, Zone Contact%, CSW%)
2. A scrollable two-column table of whiff rates broken down by pitch type

The metric grid is built by merging three stat groups from the repository (summary, discipline, batted ball) into a `Map` by title, then projecting them into a fixed display order.

### HeatMapScreen
Renders a 5×5 strike-zone grid with color-coded cells. The outer 16 cells (outside the 3×3 strike zone) are always displayed in a muted light green (`OUTER` level) regardless of their metric value. The inner 3×3 zone cells are colored based on their `HeatLevel`:

| Level | Color | Meaning |
|---|---|---|
| `ELITE` | Teal `#00897B` | Batter performs very well in this zone |
| `GOOD` | Light teal `#80CBC4` | Above-average zone |
| `BELOW_AVG` | Amber `#FFCA28` | Below-average zone |
| `WEAK` | Orange `#FFB74D` | Batter struggles in this zone |

Three metric toggle buttons (BA / SLG / OPS) allow switching views. Each metric change triggers a new API call (with caching on the repository side).

### PitchSequenceScreen
The most interactive screen. The user configures a game situation (inning, balls, strikes, outs, runners on base, times through the order, number of pitches to predict) and presses "Generate Updated Sequence" to request a recommendation.

The form uses a custom `CountCircles` composable — a row of tappable circles where each filled circle represents one unit. Tapping an already-selected circle decrements the count, making the control behave like a toggle.

Pressing "Generate Updated Sequence" snapshots the form state into `generated*` fields on `PitchSenseUiState`, then calls the recommendation API. The button is disabled while loading or when no pitcher is selected.

The results pane shows:
- An "Applied Scenario" label (returned by the backend, e.g. "Two-Strike Putaway")
- An ordered list of pitch recommendations, each showing: step number, pitch type, description, and effectiveness percentage
- A narrative analysis sentence synthesizing batter weaknesses, pitcher arsenal, count, base state, and inning

---

## 10. Shared UI Components

### `ScreenScaffold`
A wrapper composable used by all three detail screens (Advanced Stats, Heat Map, Pitch Sequence). Provides:
- `HeaderBar` with the screen title
- Optional `OfflineBanner` (amber bar shown when `isOffline = true`)
- A "Back to Overview" button
- 24dp padding around the content slot

### `CommonUi.kt`

| Composable | Purpose |
|---|---|
| `HeaderBar(title)` | Navy top bar used on every screen |
| `OfflineBanner()` | Amber warning bar shown when serving demo data |
| `StatCard(title, value, isPrimary)` | Stat display card; primary cards get a blue-tinted background |
| `ToggleButton(text, isSelected, onClick)` | Pill-shaped selection button; used for heat metric tabs and runner toggles |
| `LegendItem(color, label)` | Color swatch + label row; used in the heat map legend |

---

## 11. Demo Data

`FakePitchSenseRepository` provides a fully functional offline experience using the **2025 LA Dodgers roster** as its dataset:

- **9 batters** (George Springer, Vladimir Guerrero Jr., Alejandro Kirk, etc. — representing the Blue Jays/Dodgers demo matchup)
- **24 pitchers** (Yoshinobu Yamamoto, Shohei Ohtani, Blake Snell, Tyler Glasnow, Edwin Díaz, etc.)

All stat values are fixed constants — every batter returns the same numbers regardless of which player is selected. The heat map and pitch sequence also use static hardcoded responses. This is intentional: the demo data is designed to make the UI look realistic for demonstrations without requiring a live backend.

The fake repository's `recommendedSequence` method derives a scenario from the count and base state using the same leverage logic as the real UI (strike count, ball count, runners on base, inning) and returns a plausible-looking 3-pitch sequence, truncated to however many pitches the user requested.

---

## 12. Key Design Patterns Summary

| Pattern | Where Used | Why |
|---|---|---|
| Unidirectional data flow | ViewModel → StateFlow → Composables | Predictable state; no race conditions between screens |
| Optimistic fallback | `loadOverview`, `loadAdvanced`, `loadHeatMap` | UI is never blank; network errors degrade gracefully |
| Snapshot vs. live fields | `balls` vs. `generatedBalls` in `PitchSenseUiState` | Prevents stale results from updating while the user edits the form |
| DTO / domain model separation | `PitchSenseApiModels.kt` vs. `PitchSenseModels.kt` | UI is decoupled from the API contract; mapping is isolated to one class |
| Build-time data source switch | `RepositoryProvider` + `BuildConfig.USE_REMOTE_API` | Backend dependency is optional; UI development and demos work offline |
| In-memory caching | `advancedCache`, `heatMapCache` in `RemotePitchSenseRepository` | Avoids redundant API calls when switching between screens or metrics |
| Guard flags | `hasLoadedOverview`, etc. on the ViewModel | Prevents duplicate fetches on recomposition and back-navigation |
