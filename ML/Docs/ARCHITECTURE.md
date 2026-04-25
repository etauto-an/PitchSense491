# PitchSense – Architecture & API Notes

This document explains the internal structure of PitchSense

## System Overview

PitchSense is divided into two layers:

### Offline training
- `download.py` – fetches raw Statcast data
- `prep_outcomes.py` – cleans data and defines outcome labels
- `train_outcomes.py` – trains the PyTorch outcome model

### Online inference
Called by the model service — the API should ONLY interact with this layer.
- `recommend_next.py` — single-pitch scorer; not called directly, used internally by recommend_sequence
- `recommend_sequence.py` — beam search over multi-pitch sequences; this is the API entry point

The model service is a Flask app (not included in this repo) that wraps `recommend_from_raw_state()`
from `recommend_sequence.py`. It loads `model.pt` and `encoders.json` once at startup and exposes:

    POST http://127.0.0.1:5001/recommend/sequence

See README.md §4 "Serving as an API" for the full integration details, including the
handedness lookup requirement and the depth/beam_width defaults.

---

## Data Flow

Game state
(pitcher, handedness, count, pitch history)
→ PyTorch model inference
→ scoring + sequencing logic
→ ranked pitch recommendations

---

## Entry Points

### recommend_next.py
Returns a ranked list of candidate pitches for the *next pitch only*.

**Inputs**
- pitcher (MLBAM id, int)
- stand (L / R)
- p_throws (L / R)
- balls (0–3)
- strikes (0–2)
- prev1 (optional pitch type)
- prev2 (optional pitch type)

**Output**
- ordered pitches with outcome probabilities and score

---

### recommend_sequence.py
Returns ranked pitch sequences using beam search.

**Inputs**
- Same as recommend_next.py, plus:
- batter (int) — MLBAM batter ID
- outs (int) — 0–2
- inning (int or None) — bucketed internally into early/mid/late
- on_1b, on_2b, on_3b (int) — 1 if runner present, else 0
- tto (int or None) — times through order
- beam_width (int) — number of parallel sequences; use 8 as default
- depth (int) — pitches to plan ahead; set to pitchesToPredict from the API request
- data_path (str or None) — parquet path for pitcher arsenal filtering

**Handedness requirement**
stand (batter: L/R) and p_throws (pitcher: L/R) are required inputs but are not in the
API request body. The model service must resolve them from MLBAM IDs via
pybaseball.playerid_reverse_lookup() before calling recommend_from_raw_state().

**Output**
- List of BeamState objects ordered by score (descending)
- beams[0] is the top recommendation
- Each BeamState.seq step contains pitch_action in "PT|Zx" token format
- Scores are relative and only meaningful within a single request.

---

## Model Notes

- Model is a PyTorch classifier trained to predict pitch outcomes
- Inference is CPU-only by default
- Model weights and encoders are loaded from disk
- No training occurs during inference

---


