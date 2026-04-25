PitchSense

AI-Assisted Pitch Sequencing Using Statcast Data

Overview

- PitchSense is a PyTorch-based system that analyzes MLB Statcast pitch-by-pitch data to recommend optimal pitch selections and multi-pitch sequences for pitchers. The goal is to support pitcher decision-making by modeling how pitch type, count, handedness, and pitch history influence outcomes such as strikeouts, balls, and balls in play.
  This project focuses on sequence-aware decision making, rather than treating each pitch independently.

Key Features
- Statcast-based data pipeline using pybaseball
- Neural network outcome model trained in PyTorch
- Pitch sequence planning via beam search
- Optimization toward pitcher-favorable outcomes (e.g. strikeouts)
- Repeat penalties and pitch tunneling heuristics
- Pitcher-specific arsenals (only recommends pitches a pitcher actually throws)

Project Structure
PitchSense/
├── src/
│   ├── download.py              # Download raw Statcast data
│   ├── prep_outcomes.py         # Clean & preprocess data
│   ├── train_outcomes.py        # Train PyTorch outcome model
│   ├── recommend_next.py        # Single-pitch scoring (used internally by recommend_sequence)
│   └── recommend_sequence.py   # Recommend multi-pitch sequences (API entry point)
├── data/
│   ├── raw/                     # Raw downloaded Statcast data (ignored by git)
│   └── processed/               # Cleaned model-ready data (ignored by git)
├── models/                      # Trained PyTorch models (ignored by git)
└── README.md

Installation
Requirements

Python 3.10+

pybaseball

pandas

numpy

torch

pyarrow

Pipeline Usage
1. Download Statcast Data (download.py)

Downloads pitch-by-pitch data for a date range. A full season requires many days of data;
start with a smaller range to verify the pipeline before downloading everything.

python src/download.py --start 2024-03-20 --end 2024-10-31

Raw parquet files are written to data/raw/ (one file per day).

2. Preprocess Data (prep_outcomes.py)

Cleans raw data, normalizes outcomes, and prepares features for training.

python src/prep_outcomes.py --indir data/raw --out data/processed/outcomes_with_outs.parquet


3. Train Outcome Model (train_outcomes.py)

Trains a PyTorch classification model that predicts pitch outcomes given context.

python src/train_outcomes.py --data data/processed/outcomes_with_outs.parquet --outdir models/Test_v9_outs

Outputs two files that must both be present for inference:

models/Test_v9_outs/
 ├── model.pt        # PyTorch model weights and architecture metadata
 └── encoders.json   # Feature encoder vocabulary (required at inference time)

The --outdir path is the value to pass as --modeldir (and as MODEL_DIR for the Flask service).
Keep the processed parquet around after training — it is also needed at inference time to
determine each pitcher's arsenal (which pitch types they actually throw).


4. Serving as an API (Flask wrapper)

The API contract references an internal model service at POST http://127.0.0.1:5001/recommend/sequence.
This service does not exist yet and must be written by the backend team.

The recommended implementation is a thin Flask app that:
  1. Loads model.pt and encoders.json once at startup via load_model()
  2. Accepts the JSON request body documented in docs/backend-api-contract.md §4.1
  3. Calls recommend_from_raw_state() directly (no subprocess)
  4. Returns ranked sequences as JSON

Required inputs for recommend_from_raw_state():

  model, encoders, y_enc, cat_cols, num_cols   — loaded once from modeldir via load_model()
  device                                        — torch.device("cuda" if available else "cpu")
  data_path                                     — path to outcomes_with_outs.parquet (for arsenal filtering)
  pitcher          int    MLBAM pitcher ID
  batter           int    MLBAM batter ID
  stand            str    batter handedness: "L" or "R"   ← see Handedness Lookup below
  p_throws         str    pitcher handedness: "L" or "R"  ← see Handedness Lookup below
  balls            int    0–3
  strikes          int    0–2
  outs             int    0–2
  inning           int    1–9 (or None; defaults to "mid" bucket)
  on_1b            int    1 if runner on 1st, else 0
  on_2b            int    1 if runner on 2nd, else 0
  on_3b            int    1 if runner on 3rd, else 0
  prev1            str    previous pitch action "PT|Zx" or "NONE"
  prev2            str    pitch before that, or "NONE"
  depth            int    pitches to plan ahead — set to pitchesToPredict from the API request
  beam_width       int    number of parallel sequences to track — use 8 as the default
  tto              int    times through order (optional; pass None if unknown)

Output is a list of BeamState objects. The API response should be built from beams[0] (the
highest-scoring sequence). Each step in beams[0].seq contains pitch_action ("PT|Zx") which
maps to the token format expected by the API contract's response-mapping rules.

Handedness Lookup

stand (batter) and p_throws (pitcher) are required by the model but are not included in
the API request. The backend must resolve them from the MLBAM player ID before calling the model.

Recommended approach: query pybaseball.playerid_reverse_lookup([player_id]) at request time
and cache the result per player ID. This returns a row with throws and bats columns.

  - throws → p_throws ("L" or "R")
  - bats   → stand ("L" or "R"; treat "S" switch-hitters as "R" for MVP)

If the lookup fails for a given pitcher/batter, return 404 NOT_FOUND.

Beam Width and Depth

  depth       = pitchesToPredict from the API request (1–3)
  beam_width  = 8 (fixed default; not exposed in the API)

A beam_width of 8 gives good sequence diversity without a significant latency cost.


5. Running Directly (recommend_sequence.py)

Uses beam search to recommend a sequence of pitches optimized for pitcher success.

python src/recommend_sequence.py `
  --modeldir models/Test_v9_outs `
  --data data/processed/outcomes_with_outs.parquet `
  --pitcher 669373 `
  --batter 669257 `
  --stand R `
  --p_throws L `
  --balls 1 `
  --strikes 2 `
  --outs 2 `
  --inning 4 `
  --tto 3 `
  --on_1b `
  --beam_width 8 `
  --depth 3 `
  --json


Modeling Approach

- Inputs: pitcher ID, batter ID, batter handedness, pitcher handedness, count, outs,
  inning bucket, base state, previous pitches, times-through-order

- Outputs: probabilities for pitch outcomes (K, BB, HIT, BIP_OUT, BALL, STRIKE, FOUL)

- Sequence planning: beam search with:
  - repeat pitch penalties
  - alternation penalties (prevents ABABAB patterns)
  - walk-risk penalties (discourages drifting to 3-ball counts)
  - pitcher-specific arsenal constraints

- The system currently models pitcher vs. batter as a matchup using both IDs;
  the batter features are included but the model is primarily pitcher-driven.

Planned Extensions

- Pitch location modeling (zone buckets) #complete

- Batter-specific matchups #complete

- Maybe add three general classifications of a batter (Aggressive, Mix, passive) so a sequence is generated for a specific archetype

- Larger training data set #complete kinda, could still be bigger

- Implement "stuff-awareness" (some pitchers may have better pitches than others even if they throw the same pitch type). Model should understand understand when a pitcher has a specific unique/dominant pitch that would be more effective than if it came form a different pitcher

Notes

...
