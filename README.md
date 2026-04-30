# PitchSense

PitchSense is an Android baseball analytics app that gives pitchers and coaches a data-driven edge at the plate. Select any batter from the roster, optionally filter by a specific pitcher matchup, and instantly see Statcast-derived stats, a color-coded strike-zone heat map, and an AI-generated pitch sequence recommendation tailored to the current game situation.

---

## Features

- **Batter overview** — batting average, strikeout rate, walk rate, OBP, and home runs; optionally filtered to a specific pitcher matchup
- **Advanced Statcast metrics** — xwOBA, xBA, xSLG, Hard Hit%, Barrel%, exit velocity, Sweet Spot%, Chase%, Whiff%, Zone Contact%, and CSW%, alongside a whiff-by-pitch-type table
- **Strike-zone heat map** — a 5×5 color-coded zone grid for BA, SLG, or OPS
- **Pitch sequence recommendations** — ML-driven suggestion of up to 3 pitches, tuned to count, outs, inning, and base-runner state
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

The backend is a single Flask process. The ML model (`ML/`) is loaded **in-process** by the backend at startup — there is no separate model service.

```
Android app
  ↓ HTTP
backend/app.py  (Flask, port 5000)
  ├── fetcher.py         → pybaseball → Baseball Savant
  ├── overview.py / advanced.py / heatmap.py
  └── pitch_sequence.py  → ML/src/recommend_sequence.py (in-process)
                            ↓
                          models/pitchsense_outcomes_v1/model.pt
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
│   │   └── model/PitchSenseApiModels.kt   # Moshi DTOs
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

backend/                                   # Flask API + ML inference
├── app.py                                 # Entry point; all four routes
├── fetcher.py                             # Statcast data + caching
├── overview.py / advanced.py / heatmap.py
└── pitch_sequence.py                      # Handedness lookup + beam search

ML/                                        # Offline training pipeline
├── src/
│   ├── download.py                        # Download raw Statcast data
│   ├── prep_outcomes.py                   # Feature engineering + outcome labeling
│   ├── train_outcomes.py                  # Train OutcomeMLP; saves model.pt + encoders.json
│   └── recommend_sequence.py             # Beam search inference (imported by backend)
└── models/pitchsense_outcomes_v1/         # Trained model weights + encoder vocabularies
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

### Running against the local backend

Only **one** process needs to run. The ML model is loaded in-process by the backend at startup.

```bash
cd backend
source .venv/bin/activate
python app.py          # starts on http://0.0.0.0:5000
```

The backend expects the trained model at `ML/models/pitchsense_outcomes_v1/`. Override with environment variables if needed:

```bash
MODEL_DIR=ML/models/pitchsense_outcomes_v1 \
DATA_PATH=ML/data/processed/pitchsense_outcomes_v0.parquet \
python app.py
```

`gradle.properties` is already configured for the emulator:

```properties
PITCHSENSE_USE_REMOTE_API=true
PITCHSENSE_API_BASE_URL=http://10.0.2.2:5000/api/v1/
```

> `10.0.2.2` is the Android emulator's alias for `localhost`. For a physical device, replace it with your machine's local network IP (e.g. `192.168.1.42`).
>
> The first request for a given batter fetches live Statcast data from Baseball Savant via `pybaseball` (5–15 s). Subsequent requests hit the in-memory cache.
>
> The ML model loads PyTorch weights at backend startup (~5 s). The first pitch sequence request may take slightly longer than subsequent ones.

---

## Retraining the ML Model

The model can be retrained on any date range of Statcast data using the three-step offline pipeline:

```bash
cd ML
source .venv/bin/activate

# 1. Download raw data
python src/download.py --start 2024-03-20 --end 2024-10-31 --outdir data/raw

# 2. Process into training dataset
python src/prep_outcomes.py --indir data/raw --out data/processed/pitchsense_outcomes_v0.parquet

# 3. Train
python src/train_outcomes.py \
  --data data/processed/pitchsense_outcomes_v0.parquet \
  --outdir models/pitchsense_outcomes_v1 \
  --epochs 20 --split_mode atbat --use_class_weights
```

---

## Tech Stack

### Android app

| Layer | Library |
|---|---|
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose |
| State | ViewModel + StateFlow + Coroutines |
| Networking | Retrofit 2.11 + OkHttp |
| Serialization | Moshi 1.15 + KotlinJsonAdapterFactory |

### Backend

| Layer | Library |
|---|---|
| Language | Python 3.12 |
| Web framework | Flask 3.0 |
| Baseball data | pybaseball 2.2.7 |
| Data processing | pandas 2.0 + NumPy 1.24 |
| ML inference | PyTorch 2.0 |
