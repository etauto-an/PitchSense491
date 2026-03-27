# PitchSense Backend API Contract (Draft v0.2)

## Scope
This contract reflects the current mobile app screens and data needs:
- Overview (general + pitcher-specific cards)
- Advanced stats (direct Statcast metrics + whiff by pitch type)
- Heat map
- Pitch sequence recommendation

## Player Identifiers
Player selection is hardcoded client-side. The app passes MLBAM player IDs as
`batterId`/`pitcherId` in all requests (e.g., `"665489"` for Vladimir Guerrero Jr.
or `"808967"` for Yoshinobu Yamamoto). Display names remain UI-only labels.

## Conventions
- Base path: `/api/v1`
- JSON request/response
- Auth: not required for MVP. All endpoints are unauthenticated.
- Dates: ISO-8601 (`YYYY-MM-DD`)
- Percent values: return as numbers (e.g., `27.1`) not strings; client formats `%`
- Rate stats: return as decimals (e.g., `0.372`) not string `.372`; client formats display

## Common Error Shape
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Human-readable message",
    "details": {
      "field": "selectedBatterId"
    }
  }
}
```

Error codes in use:
- `VALIDATION_ERROR` — `400` bad/missing parameters
- `NOT_FOUND` — `404` unknown `batterId` or `pitcherId`

## 1) Overview Cards

### `GET /overview/stats?batterId=...&pitcherId=...&season=2025`
Returns both overview columns in one call. `pitcherId` is optional; omitting it returns the `general` column populated and `pitcherSpecific` as an empty array `[]`. `season` is optional and defaults to `2025`.

Response `404`: unknown `batterId` or `pitcherId`.

Response `200`:
```json
{
  "general": [
    { "key": "ba", "label": "BA", "value": 0.285, "isPrimary": true },
    { "key": "k_pct", "label": "K%", "value": 24.3, "isPrimary": true },
    { "key": "bb_pct", "label": "BB%", "value": 11.2, "isPrimary": true },
    { "key": "obp", "label": "OBP", "value": 0.362, "isPrimary": false },
    { "key": "hr", "label": "HR", "value": 37, "isPrimary": false }
  ],
  "pitcherSpecific": [
    { "key": "ba", "label": "BA", "value": 0.318, "isPrimary": true },
    { "key": "k_pct", "label": "K%", "value": 19.2, "isPrimary": true },
    { "key": "bb_pct", "label": "BB%", "value": 10.7, "isPrimary": true },
    { "key": "obp", "label": "OBP", "value": 0.385, "isPrimary": false },
    { "key": "hr", "label": "HR", "value": 5, "isPrimary": false }
  ]
}
```

## 2) Advanced Stats

### `GET /advanced/stats?batterId=...&pitcherId=...&season=2025`
Should return only directly available Statcast-style metrics currently shown. `pitcherId` is optional. `season` is optional and defaults to `2025`.

> **MVP scope note:** Situational stats (`RISP BA`, `2-Strike BA`, `Ahead in Count`, `High Leverage OPS`) are not included in this endpoint — they are hardcoded placeholders on the client for the MVP. Do not implement them on the backend.

Response `404`: unknown `batterId` or `pitcherId`.

Response `200`:
```json
{
  "directMetrics": [
    { "key": "xwoba", "label": "xwOBA", "value": 0.372 },
    { "key": "xba", "label": "xBA", "value": 0.291 },
    { "key": "xslg", "label": "xSLG", "value": 0.536 },
    { "key": "hard_hit_pct", "label": "Hard Hit%", "value": 45.2 },
    { "key": "barrel_pct", "label": "Barrel%", "value": 12.8 },
    { "key": "avg_ev", "label": "Avg EV", "value": 91.8 },
    { "key": "max_ev", "label": "Max EV", "value": 113.2 },
    { "key": "sweet_spot_pct", "label": "Sweet Spot%", "value": 34.4 },
    { "key": "chase_pct", "label": "Chase%", "value": 27.1 },
    { "key": "whiff_pct", "label": "Whiff%", "value": 22.9 },
    { "key": "zone_contact_pct", "label": "Zone Contact%", "value": 84.6 },
    { "key": "csw_pct", "label": "CSW%", "value": 28.7 }
  ],
  "whiffByPitchType": [
    { "pitchType": "4-Seam Fastball", "whiffPct": 18.5 },
    { "pitchType": "Slider", "whiffPct": 35.8 },
    { "pitchType": "Changeup", "whiffPct": 28.3 }
  ]
}
```

> **Pitch type format:** `pitchType` must be a full display name (e.g. `"4-Seam Fastball"`, `"Slider"`) — not a Statcast code (`FF`, `SL`). The client renders these strings directly.

## 3) Heat Map

### `GET /heatmap?batterId=...&metric=BA&season=2025`
Supported `metric`: `BA`, `SLG`, `OPS`. `season` is optional and defaults to `2025`.

Response `404`: unknown `batterId`.

#### Grid layout

`grid` is always a **5×5** matrix. Rows are indexed 0–4 top-to-bottom (high to low in the zone); columns are indexed 0–4 left-to-right from the catcher's perspective.

| Zone | Rows | Cols |
|------|------|------|
| Strike zone | 1–3 | 1–3 |
| Chase zone (outer ring) | 0 or 4 | 0 or 4 |

`level` values: `ELITE`, `GOOD`, `BELOW_AVG`, `WEAK`, `OUTER` (outside strike zone / chase area).

Response `200` (abbreviated — full response is 5×5):
```json
{
  "metric": "BA",
  "grid": [
    [{"value":0.200,"level":"OUTER"},{"value":0.215,"level":"OUTER"},{"value":0.220,"level":"OUTER"},{"value":0.210,"level":"OUTER"},{"value":0.198,"level":"OUTER"}],
    [{"value":0.238,"level":"OUTER"},{"value":0.275,"level":"BELOW_AVG"},{"value":0.310,"level":"GOOD"},{"value":0.268,"level":"BELOW_AVG"},{"value":0.230,"level":"OUTER"}],
    [{"value":0.245,"level":"OUTER"},{"value":0.332,"level":"GOOD"},{"value":0.388,"level":"ELITE"},{"value":0.316,"level":"GOOD"},{"value":0.241,"level":"OUTER"}],
    [{"value":0.235,"level":"OUTER"},{"value":0.270,"level":"BELOW_AVG"},{"value":0.295,"level":"BELOW_AVG"},{"value":0.258,"level":"BELOW_AVG"},{"value":0.228,"level":"OUTER"}],
    [{"value":0.195,"level":"OUTER"},{"value":0.205,"level":"OUTER"},{"value":0.212,"level":"OUTER"},{"value":0.202,"level":"OUTER"},{"value":0.190,"level":"OUTER"}]
  ]
}
```

## 4) Pitch Sequence Recommendation

### `POST /pitch-sequence/recommend`
Generate recommended sequence for current game situation. `batterId` and `pitcherId` are both required.

Response `404`: unknown `batterId` or `pitcherId`.

Request:
```json
{
  "batterId": "665489",
  "pitcherId": "808967",
  "pitchesToPredict": 3,
  "gameSituation": {
    "inning": 7,
    "balls": 1,
    "strikes": 2,
    "outs": 1,
    "runnerOnFirst": true,
    "runnerOnSecond": false,
    "runnerOnThird": true
  }
}
```

Response `200`:
```json
{
  "appliedScenario": "Two-Strike Putaway",
  "recommendations": [
    {
      "step": 1,
      "pitchType": "Slider",
      "description": "Back-foot, lower third — chase for strike three",
      "effectivenessPct": 84
    },
    {
      "step": 2,
      "pitchType": "Splitter",
      "description": "Bottom edge — late drop below zone",
      "effectivenessPct": 80
    },
    {
      "step": 3,
      "pitchType": "4-Seam Fastball",
      "description": "Top rail, glove side — finish above bat path",
      "effectivenessPct": 76
    }
  ]
}
```

> **Pitch type format:** `pitchType` must be a full display name (e.g. `"Slider"`, `"Splitter"`) — not a Statcast code. The client renders these strings directly.

## 4.1) Model Adapter Contract (Backend Internal)

The backend may keep the trained model service contract unchanged and translate at the API layer.

Internal model endpoint (example from backend team):
- `POST http://127.0.0.1:5000/recommend/sequence`

### API request -> model request mapping
- `batter` <- `batterId` (MLBAM ID; cast to number if model requires integer)
- `pitcher` <- `pitcherId` (MLBAM ID)
- `count` <- `"{balls}-{strikes}"` (for example `"0-2"`)
- `outs` <- `gameSituation.outs`
- `on_1b` <- `runnerOnFirst ? 1 : 0`
- `on_2b` <- `runnerOnSecond ? 1 : 0`
- `on_3b` <- `runnerOnThird ? 1 : 0`

Expected model response shape:
```json
{
  "batter": 665489,
  "count": "0-2",
  "on_1b": 1,
  "on_2b": 0,
  "on_3b": 1,
  "outs": 1,
  "pitcher": 808967,
  "sequences": [
    { "score": 0.1657, "sequence": ["CH|Z3", "CU|Z7", "FF|Z2"] }
  ]
}
```

### Model response -> API response mapping
- `appliedScenario`: derive from request context:
  - `strikes == 2` -> `Two-Strike Putaway`
  - else if `balls == 3` -> `Hitter Count Damage Control`
  - else if high leverage (`inning >= 7` and >=1 runner, or `outs == 2` and >=2 runners) -> `High Leverage`
  - else if `runnerOnThird == true` and `outs < 2` -> `Runner on 3rd, <2 Outs`
  - else -> `Neutral Situation`
- `recommendations`: build from top ranked candidate sequence:
  - use `sequences[0].sequence`
  - trim to `pitchesToPredict`
  - convert each token `PT|Zx` into `{ step, pitchType, description, effectivenessPct }`

Pitch code -> display pitch name:
- `FF` -> `4-Seam Fastball`
- `SI` -> `Sinker`
- `FC` -> `Cutter`
- `SL` -> `Slider`
- `CU` -> `Curveball`
- `CH` -> `Changeup`
- `FS` -> `Splitter`
- fallback -> raw code

Zone token guidance for `description`:
- `Z1..Z3` upper band, `Z4..Z6` middle band, `Z7..Z9` lower band
- Use side/height language only — do **not** include pitch speed. Speed data is not available from the model output.
- Example: `"Low-away (Z9) — chase contact"`, `"Back-foot, lower third — chase for strike three"`.

Score -> `effectivenessPct` guidance:
- If multiple candidates exist, min-max normalize scores to `65..90` and round to integer.
- If only one candidate exists, default to `80`.

Error mapping guidance:
- invalid API payload -> `400 VALIDATION_ERROR`
- unknown `batterId`/`pitcherId` -> `404 NOT_FOUND`
- model timeout/upstream failure -> `502` (or explicit fallback strategy if enabled)

## Validation Rules
- `pitchesToPredict`: `1..3`
- `inning`: `1..9` (MVP scope; extras not supported)
- `balls`: `0..3`
- `strikes`: `0..2`
- `outs`: `0..2`
- `metric`: `BA | SLG | OPS`
- `pitcherId` optional in overview endpoints, required for pitch-sequence endpoint

