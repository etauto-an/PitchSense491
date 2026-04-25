# Backend Python Files Guide

This document explains the purpose of each Python file in the `backend/` directory and how the files work together.

## Backend Flow

The backend is a Flask API used by the Android app. Most frontend requests follow this path:

```text
Android app
-> Flask route in app.py
-> Statcast data from fetcher.py
-> stat calculation module
-> JSON response back to Android
```

Pitch sequence recommendations add the ML layer:

```text
Android app
-> app.py
-> pitch_sequence.py
-> ML/src/recommend_sequence.py
-> JSON recommendations back to Android
```

## `app.py`

`app.py` is the Flask entry point for the backend.

It creates the Flask app, defines the `/api/v1` route prefix, validates request parameters, handles errors, and connects HTTP endpoints to the helper modules.

Main responsibilities:

- Starts the Flask server when run with `python app.py`
- Defines shared response helpers like `_error()`
- Reads and validates query parameters such as `batterId`, `pitcherId`, `metric`, and `season`
- Exposes the backend API endpoints used by the Android frontend
- Converts Python results into JSON responses

Routes defined:

- `GET /api/v1/overview/stats`
- `GET /api/v1/advanced/stats`
- `GET /api/v1/heatmap`
- `POST /api/v1/pitch-sequence/recommend`

How it connects to other files:

- Calls `fetcher.get_statcast_batter_data()` to load Statcast data
- Calls `overview.compute_overview_stats()` for dashboard stats
- Calls `advanced.compute_advanced_stats()` for Statcast-style metrics
- Calls `heatmap.compute_heatmap()` for zone grid data
- Calls functions from `pitch_sequence.py` for handedness lookup, ML inference, and recommendation formatting

## `fetcher.py`

`fetcher.py` is responsible for retrieving Statcast data through `pybaseball`.

It is shared by multiple routes because overview stats, advanced stats, and heat maps all need pitch-by-pitch batter data.

Main responsibilities:

- Enables the `pybaseball` cache
- Defines an in-memory cache for repeated batter/season requests
- Converts a season year into a Statcast date range
- Fetches full-season batter data with `pybaseball.statcast_batter()`
- Raises `DataNotFoundError` when a player or season has no available data
- Provides `filter_by_pitcher()` for narrowing data to one pitcher matchup

Important functions:

- `_season_date_range(season)`
- `get_statcast_batter_data(player_id, season)`
- `filter_by_pitcher(df, pitcher_id)`

Why it matters:

- Prevents every API call from re-fetching the same Statcast data
- Keeps data loading separate from route and stat-calculation logic
- Gives the backend one consistent place to handle missing Statcast data

## `overview.py`

`overview.py` computes the basic stat cards shown on the app's overview screen.

It uses plate appearance outcomes from the Statcast `events` column. Only rows with a non-null `events` value are counted because those rows represent the final pitch of a plate appearance.

Main responsibilities:

- Defines Statcast event groups for at-bats, hits, walks, hit-by-pitch, and sacrifice flies
- Computes traditional hitter stats
- Builds the `general` and `pitcherSpecific` arrays expected by the frontend

Stats computed:

- `BA`
- `K%`
- `BB%`
- `OBP`
- `HR`

Important functions:

- `_compute_stats(pa_df)`
- `compute_overview_stats(df, pitcher_id=None)`

Why it matters:

- Powers the first scouting snapshot in the Android app
- Supports both full-season batter stats and optional batter-vs-pitcher matchup stats

## `advanced.py`

`advanced.py` computes the deeper Statcast-style metrics used by the advanced stats screen.

It works with pitch-level and batted-ball data from Statcast to calculate contact quality, swing behavior, expected stats, and whiff rates by pitch type.

Main responsibilities:

- Defines pitch-result groupings for swings, whiffs, called strikes, and chase zones
- Maps Statcast pitch codes like `FF`, `SL`, and `CH` to display names
- Computes discipline metrics from pitch descriptions and zone data
- Computes batted-ball metrics from exit velocity and launch angle
- Computes expected stats from Statcast estimated outcome columns
- Builds a whiff-by-pitch-type table

Metrics computed:

- `xwOBA`
- `xBA`
- `xSLG`
- `Hard Hit%`
- `Barrel%`
- `Sweet Spot%`
- `Avg EV`
- `Max EV`
- `Chase%`
- `Whiff%`
- `Zone Contact%`
- `CSW%`
- Whiff rate by pitch type

Important functions:

- `_compute_discipline(df)`
- `_compute_batted_ball(df)`
- `_compute_expected_stats(df)`
- `_compute_whiff_by_pitch_type(df)`
- `compute_advanced_stats(df, pitcher_id=None)`

Why it matters:

- Helps the frontend explain hitter quality beyond basic stats
- Supports decisions like whether to challenge a hitter in the zone or expand the zone
- Keeps advanced stat math separate from Flask route handling

## `heatmap.py`

`heatmap.py` builds the 5x5 heat map used by the batter heat map screen.

It maps Statcast pitch locations into a grid based on `plate_x` and `plate_z`, then calculates hitter performance inside each location bucket.

Main responsibilities:

- Defines horizontal and vertical zone boundaries
- Assigns each pitch to a row and column in a 5x5 grid
- Computes cell-level `BA`, `SLG`, or `OPS`
- Classifies each cell into a performance level
- Marks the outer ring as chase-area context instead of the core strike zone

Supported metrics:

- `BA`
- `SLG`
- `OPS`

Important functions:

- `_classify_level(value, metric)`
- `_compute_cell_stat(cell_df, metric)`
- `compute_heatmap(df, metric)`

Why it matters:

- Converts location-based hitting data into a visual tool
- Gives the Android app a simple grid structure to render
- Helps users identify strong and weak pitch locations for a hitter

## `pitch_sequence.py`

`pitch_sequence.py` handles pitch sequence recommendation support for the backend.

This file connects the Flask API to the local ML model in `ML/src/recommend_sequence.py`. It loads the model once when the backend starts, resolves player handedness, calls model inference, and maps raw model output into API-friendly recommendations.

Main responsibilities:

- Adds `ML/src` to the Python import path
- Loads the trained pitch recommendation model at startup
- Resolves batter handedness and pitcher throwing arm through `pybaseball`
- Derives a human-readable applied scenario from the game state
- Calls `recommend_from_raw_state()` for pitch sequence inference
- Converts model tokens like pitch type and zone into frontend-ready recommendation objects

Important functions:

- `get_batter_stand(batter_id)`
- `get_pitcher_throws(pitcher_id)`
- `derive_applied_scenario(...)`
- `build_recommendations(...)`
- `call_model_service(payload)`

Important class:

- `ModelServiceError`

Why it matters:

- Powers the app's main decision-support feature
- Keeps ML-specific setup and response mapping out of `app.py`
- Translates raw model output into pitch type, description, and effectiveness values that the Android app can display

## File Relationship Summary

```text
app.py
├── fetcher.py
├── overview.py
├── advanced.py
├── heatmap.py
└── pitch_sequence.py
    └── ../ML/src/recommend_sequence.py
```

## Quick Reference

| File | Main Role | Used By |
| --- | --- | --- |
| `app.py` | Flask routes, validation, JSON responses | Run directly by backend |
| `fetcher.py` | Statcast loading and caching | `app.py` |
| `overview.py` | Basic overview stat cards | `app.py` |
| `advanced.py` | Advanced Statcast metrics | `app.py` |
| `heatmap.py` | 5x5 location grid data | `app.py` |
| `pitch_sequence.py` | ML-backed pitch sequence recommendations | `app.py` |

## Typical Local Run

From the `backend/` directory:

```bash
python app.py
```

The Android emulator should reach the API at:

```text
http://10.0.2.2:5000/api/v1/
```

