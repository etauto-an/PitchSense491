# PitchSense Backend: Technical Overview
---

## 1. What the Backend Does

The PitchSense backend is a Python REST API that serves four endpoints to the Android app. It is responsible for:

- Fetching real pitch-by-pitch Statcast data from Baseball Savant (via pybaseball)
- Computing traditional and advanced batting statistics from that raw data
- Building a 5×5 strike-zone heat map grid for a batter
- Running a trained ML model to recommend a pitch sequence for a given game situation

All endpoints live under `/api/v1/` and are consumed exclusively by the Android app. There is no web frontend.

---

## 2. Tech Stack

| Layer | Technology | Description |
|---|---|---|
| Language | Python 3.12 | Primary language for all backend code |
| Web Framework | Flask 3.0 | Lightweight Python web framework that handles HTTP routing, request parsing, and JSON responses |
| Baseball Data | pybaseball 2.2.7 | Python library that scrapes pitch-by-pitch Statcast data from Baseball Savant (MLB's official analytics platform) |
| Data Processing | pandas 2.0 + NumPy 1.24 | pandas provides tabular data structures (DataFrames) for slicing and aggregating pitch data; NumPy handles numerical operations and grid binning |
| ML Inference | PyTorch 2.0 | Runs the trained pitch sequence model in-process on CPU or GPU |
| Parquet I/O | PyArrow 12.0 | Reads the processed training data file used for arsenal filtering during inference |

---

## 3. Project Structure

```
backend/
├── app.py              — Flask entry point; all four routes and input validation
├── fetcher.py          — Statcast data retrieval and in-memory caching
├── overview.py         — BA, K%, BB%, OBP, HR computation
├── advanced.py         — Statcast advanced metrics + whiff-by-pitch-type breakdown
├── heatmap.py          — 5×5 strike-zone grid construction
├── pitch_sequence.py   — Handedness lookup, ML inference, response mapping
└── requirements.txt    — Python dependencies
```

The four module files (`overview.py`, `advanced.py`, `heatmap.py`, `pitch_sequence.py`) each own exactly one concern. `app.py` handles only HTTP — validation and routing — and delegates all computation to those modules. `fetcher.py` is a shared dependency used by all three stat modules.

---

## 4. Data Flow

```
Android app
    ↓  HTTP request
app.py  (validates params, calls fetcher, delegates to module)
    ↓
fetcher.py  (checks cache → scrapes Baseball Savant via pybaseball if miss)
    ↓  pandas DataFrame (one row per pitch)
overview.py / advanced.py / heatmap.py / pitch_sequence.py
    ↓  Python dict
app.py  (jsonify → HTTP response)
    ↓
Android app
```

Every route follows the same four-step pattern:
1. Validate query/body parameters; return `400` on failure
2. Call `get_statcast_batter_data()` from `fetcher.py`; return `404` if no data
3. Delegate the DataFrame to the relevant compute module
4. Return the result as JSON with `200`

---

## 5. Statcast Data: `fetcher.py`

### What Statcast data is

Baseball Savant records every pitch thrown in MLB as a row of data. Each row includes the pitch location (`plate_x`, `plate_z`), pitch type (`FF` = 4-Seam Fastball, `SL` = Slider, etc.), the result (`swinging_strike`, `ball`, `home_run`, etc.), and Statcast model outputs like exit velocity and expected batting average.

`fetcher.py` retrieves this data for a single batter across an entire season. The result is a pandas DataFrame where **each row is one pitch** thrown to that batter.

### In-memory cache

Fetching a full season of data from Baseball Savant takes several seconds. To avoid re-scraping on every request, `fetcher.py` maintains a simple in-process dict:

```python
_cache: dict[tuple, pd.DataFrame] = {}  # key: (player_id_str, season_int)
```

The first request for a given batter/season triggers the scrape and stores the result. Every subsequent request for the same batter in the same process returns instantly from the cache. The cache lives for the lifetime of the Flask process.

### `get_statcast_batter_data(player_id, season)`

The primary function used by all three stat modules. Returns the cached or freshly fetched DataFrame, or raises `DataNotFoundError` (→ HTTP 404) if the player has no data for that season.

### `filter_by_pitcher(df, pitcher_id)`

A utility used by `overview.py` and `advanced.py` to narrow a batter's full season DataFrame to only pitches thrown by a specific pitcher. The `pitcher` column in Statcast data contains MLBAM integer IDs, so the filter is a simple equality check.

---

## 6. Overview Stats: `overview.py`

Computes five traditional stats: **BA** (batting average), **K%** (strikeout rate), **BB%** (walk rate), **OBP** (on-base percentage), and **HR** (home run count).

### How it works

Statcast records one pitch per row. Only the final pitch of a plate appearance has a non-null `events` column (e.g. `"single"`, `"strikeout"`, `"walk"`). The module filters to those rows only:

```
Full DataFrame (all pitches)  →  filter events.notna()  →  one row per plate appearance
```

From that filtered subset, it applies standard baseball formulas:

| Stat | Formula |
|---|---|
| BA | hits / at-bats |
| K% | strikeouts / plate appearances × 100 |
| BB% | walks / plate appearances × 100 |
| OBP | (H + BB + HBP) / (AB + BB + HBP + SF) |
| HR | raw count |

At-bat events (AB), hit events, walk events, etc. are defined as explicit Python sets matching the Statcast event vocabulary (e.g. `_AB_EVENTS`, `_HIT_EVENTS`). This avoids magic strings scattered through the logic.

### General vs. pitcher-specific

`compute_overview_stats(df, pitcher_id)` is called with the full season DataFrame. When `pitcher_id` is provided, it also computes a second set of stats filtered to plate appearances where that specific pitcher threw the final pitch. The response returns both:

```json
{ "general": [...], "pitcherSpecific": [...] }
```

If no pitcher is specified, `pitcherSpecific` is an empty array.

---

## 7. Advanced Stats: `advanced.py`

Computes 12 Statcast-based metrics grouped into three categories, plus a per-pitch-type whiff breakdown table.

### Metric groups

**Expected stats** (xwOBA, xBA, xSLG) — Statcast runs a model on every batted ball to estimate what the outcome *should* have been based on exit velocity and launch angle, independent of fielder positioning. The backend averages these per-batted-ball estimates across all balls put in play.

**Batted-ball quality** (Hard Hit%, Barrel%, Sweet Spot%, Avg EV, Max EV):
- Hard Hit% — exit velocity ≥ 95 mph
- Barrel% — uses the `barrel` column pre-computed by Statcast
- Sweet Spot% — launch angle between 8° and 32° (the optimal trajectory range for hits)
- Avg EV / Max EV — mean and maximum exit velocity in the sample

**Plate discipline** (Chase%, Whiff%, Zone Contact%, CSW%):
- Chase% — swings on pitches outside the strike zone / total out-of-zone pitches
- Whiff% — missed swings / total swings
- Zone Contact% — in-zone swings that made contact / total in-zone swings
- CSW% (Called Strike + Whiff) — (called strikes + whiffs) / total pitches

Statcast zones 1–9 are inside the rulebook strike zone; zones 11–14 are outside (chase zones). These are used to classify each pitch's location for discipline metrics.

### Whiff by pitch type

`_compute_whiff_by_pitch_type` groups pitches by their `pitch_type` code, computes whiff rate for each group, and returns only pitch types with at least 10 swings (to avoid misleading percentages from tiny samples). Results are sorted descending by whiff rate.

---

## 8. Heat Map: `heatmap.py`

Builds the 5×5 strike-zone performance grid sent to the Android app.

### Grid geometry

Each pitch has a `plate_x` (horizontal) and `plate_z` (vertical) coordinate in feet at the front of home plate. The module bins these coordinates into a 5×5 grid:

- **Columns (0–4, left to right from catcher's view):** divided at ±0.708 ft and ±0.236 ft from plate center. Home plate is 17 inches (1.417 ft) wide; the inner three columns cover the strike zone, the outer two are chase lanes.
- **Rows (0–4, top to bottom):** divided at 3.5 ft, 2.833 ft, 2.167 ft, and 1.5 ft. The inner three rows cover the strike zone height; the outer two are high/low chase zones.

`pd.cut` assigns each pitch's `plate_x` and `plate_z` to a column and row index.

### Cell computation

For each of the 25 cells, `_compute_cell_stat` filters to pitches in that cell that were put in play (`type == 'X'`) and computes the requested metric (BA, SLG, or OPS) from the batted-ball outcomes in that cell.

OPS is approximated as zone_BA + zone_SLG, since walk and HBP data is not available per zone in Statcast.

### OUTER classification

Cells in the outer ring (row 0, row 4, col 0, or col 4) are always labeled `OUTER` regardless of their computed stat value, because they represent the chase zone outside the rulebook strike zone. The stat value is still computed and sent to the client for reference.

### Performance levels

Each inner cell's computed value is classified against metric-specific thresholds:

| Metric | ELITE | GOOD | BELOW_AVG | WEAK |
|---|---|---|---|---|
| BA | ≥ .350 | ≥ .280 | ≥ .200 | < .200 |
| SLG | ≥ .600 | ≥ .450 | ≥ .300 | < .300 |
| OPS | ≥ .950 | ≥ .750 | ≥ .550 | < .550 |

---

## 9. Pitch Sequence Recommendation: `pitch_sequence.py`

The most complex module. It handles three responsibilities: handedness lookup, ML model inference, and response mapping.

### Handedness lookup

The ML model requires the batter's batting side (`L`/`R`) and the pitcher's throwing arm (`L`/`R`). These are not in the API request body (the app only sends MLBAM IDs), so the backend resolves them using `pybaseball.playerid_reverse_lookup`. Results are cached in `_handedness_cache` per player ID. Switch hitters are treated as right-handed batters for the MVP.

If either player is not found in the lookup, the endpoint returns a `404` before reaching the model.

### ML model

The model is a **sequence model** (loaded from `ML/models/pitchsense_outcomes_v1/` via `ML/src/recommend_sequence.py`) that predicts the next-best pitch given the current game state. It is loaded once at startup (`load_model(MODEL_DIR, device)`) and reused across all requests.

Inference is triggered by `call_model_service(payload)`, which calls `recommend_from_raw_state()` with:

| Input | Source |
|---|---|
| `pitcher`, `batter` | MLBAM IDs from the request |
| `stand`, `p_throws` | Resolved via `_lookup_handedness` |
| `count` | `"balls-strikes"` string, e.g. `"1-2"` |
| `outs`, `on_1b/2b/3b`, `inning` | Directly from `gameSituation` in the request |
| `tto` | Times through the order (optional) |
| `depth` | `pitchesToPredict` from the request (1–3) |
| `beam_width` | Fixed at 8 — number of candidate sequences to explore |

The model uses **beam search** with width 8, returning up to 8 candidate sequences ranked by score. The top sequence is used for the response. Each step in a sequence is a token in the format `"PT|Zx"` (e.g. `"SL|Z9"` = Slider, low-away zone).

### Response mapping

`build_recommendations` converts the top beam-search sequence into the API response:

1. **Token parsing** (`_parse_token`) — splits `"SL|Z9"` into pitch code `"SL"` and zone `"Z9"`
2. **Pitch name** — `_PITCH_NAME` maps `"SL"` → `"Slider"`
3. **Description** (`_make_description`) — combines a zone phrase (e.g. `"Low and away"` for `Z9`) with a count-based intent phrase (e.g. `"chase for strike three"` on two strikes)
4. **Effectiveness** (`_normalize_effectiveness`) — min-max normalizes the beam scores across all 8 candidates into a 65–90% range; step *i* uses the score of the *i*-th ranked sequence, so effectiveness decreases for later pitches in the sequence

### Applied scenario

`derive_applied_scenario` maps the game situation to one of five scenario labels (evaluated in priority order):

| Priority | Condition | Label |
|---|---|---|
| 1 | `strikes == 2` | "Two-Strike Putaway" |
| 2 | `balls == 3` | "Hitter Count Damage Control" |
| 3 | Inning ≥ 7 with runners, or 2 outs with 2+ runners | "High Leverage" |
| 4 | Runner on third with < 2 outs | "Runner on 3rd, <2 Outs" |
| 5 | None of the above | "Neutral Situation" |

This classification is done entirely on the backend and returned as `appliedScenario` in the response. The model is not involved in this step.

---

## 10. Input Validation

`app.py` validates all input before touching any data. Validation follows a consistent pattern throughout:

- **Required query parameters** — `_require_param(name)` returns a `400` with a structured error body if the parameter is missing
- **Season parsing** — `_parse_season()` converts the string to int and returns `400` for non-numeric values; defaults to 2025
- **Metric allowlist** — the heatmap endpoint explicitly checks `metric in {"BA", "SLG", "OPS"}` and returns `400` otherwise
- **Request body fields** — the pitch sequence endpoint validates every required field in `gameSituation` individually, with range checks (`1 ≤ inning ≤ 9`, `0 ≤ balls ≤ 3`, etc.)

All error responses follow a uniform JSON shape per the API contract:

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Missing required query parameter: batterId",
    "details": { "field": "batterId" }
  }
}
```

---

## 11. Error Handling and HTTP Status Codes

| Situation | Status | Error Code |
|---|---|---|
| Missing or invalid query/body parameter | 400 | `VALIDATION_ERROR` |
| Player/season not found in Statcast | 404 | `NOT_FOUND` |
| ML model inference failure | 502 | `MODEL_UNAVAILABLE` |

`DataNotFoundError` is raised by `fetcher.py` and caught in `app.py`. `ModelServiceError` is raised by `pitch_sequence.py` and caught in the same place. Neither exception propagates to Flask's default error handler, so the client always receives a structured JSON error body rather than an HTML error page.

---

## 12. Running the Server

```bash
cd backend
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
python app.py
```

The server binds to `0.0.0.0:5000`. The Android emulator reaches it at `http://10.0.2.2:5000/api/v1/` — `10.0.2.2` is the emulator's special alias for `localhost` on the host machine.

Two environment variables can override the default ML paths:

| Variable | Default | Purpose |
|---|---|---|
| `MODEL_DIR` | `../ML/models/pitchsense_outcomes_v1` | Path to the trained model directory |
| `DATA_PATH` | `../ML/data/processed/pitchsense_outcomes_v0.parquet` | Path to the processed training data for arsenal filtering |

If `DATA_PATH` does not exist, the server starts anyway and logs a warning — arsenal filtering is disabled but all other functionality works normally.
