# PitchSense

PitchSense is an Android baseball analytics app that gives pitchers and coaches a data-driven edge at the plate. Select any batter from the roster, optionally filter by a specific pitcher matchup, and instantly see Statcast-derived stats, a color-coded strike-zone heat map, and an AI-generated pitch sequence recommendation tailored to the current game situation.

---

## Features

- **Batter overview** — batting average, strikeout rate, walk rate, OBP, and home runs; optionally filtered to a specific pitcher matchup
- **Advanced Statcast metrics** — xwOBA, xBA, xSLG, Hard Hit%, Barrel%, exit velocity, Sweet Spot%, Chase%, Whiff%, Zone Contact%, and CSW%, alongside a whiff-by-pitch-type table
- **Strike-zone heat map** — a 5×5 color-coded zone grid for BA, SLG, or OPS
- **Pitch sequence recommendations** — AI-driven suggestion of up to 3 pitches, tuned to count, outs, inning, and base-runner state
- **Offline-first** — ships with hardcoded demo data; the remote API is opt-in via a build flag

---

## Architecture

PitchSense follows **single-activity MVVM** with Jetpack Compose and a Repository pattern.

```
Composable Screens
  ↓  collectAsState()
PitchSenseViewModel          ← single shared ViewModel for all screens
  ↓
PitchSenseRepository         ← interface; two implementations
  ├── FakePitchSenseRepository    → hardcoded demo data (default)
  └── RemotePitchSenseRepository  → Retrofit → PitchSenseApiService
```

---

## Project Structure

```
app/src/main/java/com/example/pitchsense/
│
├── MainActivity.kt
├── data/
│   ├── model/PitchSenseModels.kt          # Domain models
│   ├── remote/
│   │   ├── api/ApiClient.kt               # Retrofit + Moshi factory
│   │   ├── api/PitchSenseApiService.kt    # Retrofit interface
│   │   └── model/PitchSenseApiModels.kt  # Moshi DTOs
│   └── repository/
│       ├── PitchSenseRepository.kt        # Interface
│       ├── FakePitchSenseRepository.kt
│       ├── RemotePitchSenseRepository.kt
│       └── RepositoryProvider.kt
└── ui/
    ├── components/CommonUi.kt             # HeaderBar, StatCard, ToggleButton, LegendItem
    ├── components/ScreenScaffold.kt
    ├── navigation/NavGraph.kt
    ├── screens/
    │   ├── LoginScreen.kt
    │   ├── OverviewScreen.kt
    │   ├── AdvancedStatsScreen.kt
    │   ├── HeatMapScreen.kt
    │   └── PitchSequenceScreen.kt
    ├── state/PitchSenseUiState.kt         # Single immutable state class
    ├── theme/
    └── viewmodel/PitchSenseViewModel.kt
```

---

## Navigation

```
login ──► overview ──► advanced_stats
                  └──► heat_map
                  └──► pitch_sequence
```

Login is cleared from the back stack — pressing back from Overview exits the app. All detail screens share a `ScreenScaffold` with a consistent header and "Back to Overview" button.

---

## Build & Run

### Prerequisites

- Android Studio Hedgehog or later
- JDK 11
- Android SDK — min API 24, target/compile API 36

### Build commands

```bash
./gradlew assembleDebug           # Build debug APK
./gradlew assembleRelease         # Build release APK
./gradlew test                    # Run unit tests
./gradlew connectedAndroidTest    # Run instrumented tests (requires emulator/device)
./gradlew lint                    # Run Android lint checks
```

### Running against a local backend

The backend lives in `backend/` and requires Python 3.10+.

```bash
cd backend
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
python app.py          # starts on http://0.0.0.0:5000
```

Then set in `gradle.properties`:

```properties
PITCHSENSE_USE_REMOTE_API=true
PITCHSENSE_API_BASE_URL=http://10.0.2.2:5000/api/v1/
```

> `10.0.2.2` is the Android emulator's alias for `localhost`. For a physical device, use your machine's local network IP (e.g. `192.168.1.42`).
>
> The first request for a given batter fetches live data from Baseball Savant via `pybaseball` (5–15 s). Subsequent requests hit the in-memory cache.

---

## Tech Stack

| Layer | Library | Version |
|---|---|---|
| Language | Kotlin | 2.2.10 |
| UI | Jetpack Compose (BOM) | 2024.09.00 |
| Design system | Material3 | — |
| Navigation | Navigation Compose | — |
| State | ViewModel + StateFlow + Coroutines | AndroidX |
| Networking | Retrofit | 2.11 |
| Serialization | Moshi + KotlinJsonAdapterFactory | 1.15 |
| Min SDK | 24 (Android 7.0) | |
| Target/Compile SDK | 36 | |
| JVM target | 11 | |
