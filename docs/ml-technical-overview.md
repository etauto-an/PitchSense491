# PitchSense ML System: Technical Overview



---

## 1. What the ML System Does

The ML system is responsible for one thing: given a current game situation (pitcher, batter, count, base state, inning), recommend an optimal sequence of pitches for the pitcher to throw.

It solves this as a **sequence planning problem**, not a simple lookup. The model is trained to predict the *outcome* of any given pitch (strikeout, walk, hit, ball, strike, etc.) given the full game context. At inference time, a **beam search** algorithm explores many possible pitch sequences and scores them according to a pitching objective function — maximising strikeouts and weak contact while minimising walks and hits.

The system is split into two phases:

- **Offline training** — a one-time pipeline that downloads data, processes it, and trains the model
- **Online inference** — called at request time by the Flask backend; loads the trained model and runs beam search

---

## 2. Tech Stack

| Layer | Technology | Description |
|---|---|---|
| Language | Python 3.12 | All ML source code |
| Deep Learning | PyTorch 2.0 | Defines, trains, and runs the neural network |
| Data Processing | pandas 2.0 + NumPy 1.24 | DataFrame manipulation during preprocessing and feature construction |
| Baseball Data | pybaseball 2.2.7 | Downloads raw Statcast data and resolves player handedness from MLBAM IDs |
| Data Storage | Parquet (PyArrow 12.0) | Efficient columnar format for storing millions of pitch rows on disk |

---

## 3. Project Structure

```
ML/
├── src/
│   ├── download.py          — Downloads raw Statcast data day-by-day as Parquet files
│   ├── prep_outcomes.py     — Cleans raw data; engineers features; labels each pitch with an outcome
│   ├── train_outcomes.py    — Trains the OutcomeMLP PyTorch model; saves model.pt + encoders.json
│   ├── recommend_sequence.py — Beam search inference; the entry point called by the Flask backend
│   └── recommend_next.py    — Single-pitch scorer; used internally by recommend_sequence.py
├── models/
│   └── pitchsense_outcomes_v1/
│       ├── model.pt          — Saved PyTorch model weights + architecture hyperparameters
│       └── encoders.json     — Label encoder vocabularies + training metadata
├── data/
│   ├── raw/                  — One statcast_YYYY-MM-DD.parquet file per downloaded day
│   └── processed/
│       └── pitchsense_outcomes_v0.parquet — Cleaned, feature-engineered dataset for training
└── Docs/
    └── ARCHITECTURE.md       — Internal architecture notes
```

---

## 4. The Full Pipeline

```
[1] download.py
    ↓  statcast_YYYY-MM-DD.parquet  (one file per game day)
    ↓  → data/raw/

[2] prep_outcomes.py
    ↓  reads raw/*.parquet
    ↓  engineers features, labels outcomes
    ↓  → data/processed/pitchsense_outcomes_v0.parquet

[3] train_outcomes.py
    ↓  reads processed parquet
    ↓  fits label encoders, trains OutcomeMLP
    ↓  → models/pitchsense_outcomes_v1/model.pt
    ↓  → models/pitchsense_outcomes_v1/encoders.json

[4] recommend_sequence.py  (called at request time by Flask backend)
    ↓  loads model.pt + encoders.json once at startup
    ↓  runs beam search over pitch sequences
    ↓  returns ranked BeamState list
```

Steps 1–3 run offline (once, or when retraining). Step 4 runs on every API request.

---

## 5. Step 1 — Data Download: `download.py`

Downloads pitch-by-pitch Statcast data from Baseball Savant via `pybaseball.statcast()`. The script iterates day-by-day over a date range and saves each day as a separate Parquet file (`statcast_YYYY-MM-DD.parquet`) in `data/raw/`.

Key design choices:
- **Day-by-day chunking** — avoids memory issues from trying to download a full season at once, and allows resuming interrupted downloads (already-present files are skipped unless `--force` is passed)
- **Retry logic** — each day gets up to 3 attempts with a 5-second sleep between failures before giving up
- **Configurable sleep** between days (default 2 seconds) to be polite to the Baseball Savant API

Usage:
```bash
python src/download.py --start 2024-03-20 --end 2024-10-31 --outdir data/raw
```

---

## 6. Step 2 — Data Preparation: `prep_outcomes.py`

Transforms the raw Statcast pitch rows into a clean, model-ready dataset.

### What each row becomes

The raw data has one row per pitch. `prep_outcomes.py` keeps only relevant columns and engineers the following additional features:

| Feature | How it's derived |
|---|---|
| `pitch_action` | `pitch_type + "\|" + loc_bucket` — e.g. `"FF\|Z1"` (4-seam fastball, top-left of zone) |
| `loc_bucket` | Statcast `zone` mapped to `"Z1"`–`"Z9"` (in zone) or `"Z11"`–`"Z14"` (chase); `"UNK_LOC"` for missing |
| `prev_action_1` | The `pitch_action` of the previous pitch in the same at-bat (or `"NONE"`) |
| `prev_action_2` | The `pitch_action` two pitches prior (or `"NONE"`) |
| `inning_bucket` | Inning 1–3 → `"early"`, 4–6 → `"mid"`, 7–9 → `"late"`, 10+ → `"extras"` |
| `tto_bucket` | Times through order: 1 → `"1"`, 2 → `"2"`, 3+ → `"3+"` |
| `base_state` | Three-character string encoding runner positions: `"1--"`, `"-23"`, `"---"`, etc. |
| `runners_count` | Integer 0–3 counting total runners on base |
| `outs` | From `outs_when_up`, clipped to 0–2 |
| `outcome` | See outcome labels below |

### Outcome labels

Each pitch is labeled with one of eight outcomes. Terminal outcomes (end of plate appearance) take priority over pitch-level outcomes:

| Label | Meaning | Source |
|---|---|---|
| `K` | Strikeout | `events` = `strikeout` or `strikeout_double_play` |
| `BB` | Walk / HBP | `events` in walk/HBP set |
| `HIT` | Hit | `events` in `{single, double, triple, home_run}` |
| `BIP_OUT` | Ball in play, out | `events` in ground out, fly out, double play, etc. |
| `BALL` | Ball | `description` = `ball` |
| `STRIKE` | Called or swinging strike | `description` in strike set |
| `FOUL` | Foul ball | `description` in foul set |
| `OTHER` | Everything else | Catchall for rare descriptions |

The `pitch_action` token (`"FF|Z1"`) encodes **what the pitcher chose**. The `outcome` label encodes **what happened as a result**. The model learns the mapping from choice + context → likely outcome.

---

## 7. Step 3 — Model Training: `train_outcomes.py`

### The model: `OutcomeMLP`

A feedforward neural network (multi-layer perceptron) with **entity embeddings** for categorical features.

**Architecture:**

```
Categorical features (10 columns)
  → Embedding(vocab_size, emb_dim=16) for each
  → Concatenate all embeddings

Numerical features (4 columns, normalised 0–1)
  → Concatenate with embeddings

Combined vector  (10 × 16 + 4 = 164 dimensions)
  → Linear(164, 128) → ReLU → Dropout(0.05)
  → Linear(128, 128) → ReLU → Dropout(0.05)
  → Linear(128, num_classes)   ← one logit per outcome class
  → Softmax                    ← probability distribution over outcomes
```

**Why embeddings?** Categorical features like `pitcher` (MLBAM ID) and `batter` have thousands of distinct values. A simple one-hot encoding would produce enormous sparse vectors. Entity embeddings compress each categorical value into a dense 16-dimensional learned vector, allowing the model to capture similarity between players and situations.

### Features

| Feature | Type | Description |
|---|---|---|
| `pitcher` | Categorical | MLBAM pitcher ID — model learns a per-pitcher embedding |
| `batter` | Categorical | MLBAM batter ID — model learns a per-batter embedding |
| `stand` | Categorical | Batter's batting side (`L` / `R`) |
| `p_throws` | Categorical | Pitcher's throwing arm (`L` / `R`) |
| `inning_bucket` | Categorical | `early` / `mid` / `late` / `extras` |
| `base_state` | Categorical | Runner positions string (e.g. `"1-3"`) |
| `prev_action_1` | Categorical | Previous pitch token (e.g. `"SL|Z9"`) |
| `prev_action_2` | Categorical | Pitch before that |
| `pitch_action` | Categorical | **The pitch being evaluated** (e.g. `"FF|Z1"`) |
| `tto_bucket` | Categorical | Times through order bucket |
| `balls` | Numerical | 0–3, normalised by dividing by 3 |
| `strikes` | Numerical | 0–2, normalised by dividing by 2 |
| `runners_count` | Numerical | 0–3, normalised by dividing by 3 |
| `outs` | Numerical | 0–2, normalised by dividing by 2 |

### Train / validation split

Three split modes are available (controlled by `--split_mode`):

| Mode | How it splits | Purpose |
|---|---|---|
| `row` | Random row shuffle | Fastest; may leak info across at-bats |
| `game` | By `game_pk` (whole games to train or val) | Cleaner separation |
| `atbat` | By `(game_pk, at_bat_number)` | Default; whole at-bats go to one split |

The `atbat` mode is the default because it prevents the model from seeing earlier pitches from the same at-bat in training while predicting a later pitch in validation, which would be data leakage.

### Class weighting

Outcome classes are highly imbalanced — `BALL` and `STRIKE` appear far more frequently than `K`, `HIT`, or `BB`. Optional class weights (`--use_class_weights`) upweight rare outcomes using `sqrt_inv` mode by default:

```
weight = 1 / sqrt(class_count)
```

Weights are clipped to `[0.25, 4.0]` and normalized to mean 1.0, then passed to `CrossEntropyLoss`.

### Training loop

- Optimizer: **Adam** (lr = 0.001)
- Loss: `CrossEntropyLoss` (optionally class-weighted)
- Early stopping: patience of 4 epochs on validation loss
- Best model state is restored before saving

After training, two files are saved to `--outdir`:
- `model.pt` — PyTorch checkpoint with weights and architecture hyperparameters
- `encoders.json` — vocabulary lists for all label encoders + training history

---

## 8. Step 4 — Inference: `recommend_sequence.py`

This file is the entry point called by the Flask backend at request time. It is loaded once at server startup and reused across all requests.

### Loading the model: `load_model(modeldir, device)`

Reads `model.pt` and `encoders.json`, reconstructs the `OutcomeMLP` architecture from the saved hyperparameters, loads the weights, and sets the model to `eval()` mode. Returns the model, encoder dict, target encoder, and column lists.

### Model prediction: `model_predict(...)`

Takes a single game state + one candidate pitch action, constructs the feature vector, runs a forward pass, and applies softmax to return a probability distribution over all 8 outcome classes:

```python
{
  "K": 0.18,
  "BB": 0.04,
  "HIT": 0.09,
  "BIP_OUT": 0.22,
  "BALL": 0.25,
  "STRIKE": 0.14,
  "FOUL": 0.07,
  "OTHER": 0.01,
}
```

### Scoring a pitch: `score_objective(pm, balls, strikes)`

Converts the probability distribution into a scalar score representing how good that pitch is for the pitcher. The objective is designed to maximise getting outs while avoiding walks and hits:

```
base = 1.0 × P(K)  +  0.35 × P(BIP_OUT)  −  bb_weight × P(BB)  −  1.20 × P(HIT)
```

Additional count-aware terms adjust the score based on the current count:
- `strike_weight × P(STRIKE)` — reward going ahead in the count (higher weight on 0 strikes)
- `foul_weight × P(FOUL)` — small reward for fouling off a pitch with fewer than 2 strikes
- `−ball_weight × P(BALL)` — penalty for going further behind in the count (harsher at 3 balls)
- `bb_weight` for the base formula scales up at 3 balls (`0.70` at 0 balls → `1.30` at 3 balls)

### Stochastic count distribution

Rather than assuming the count stays fixed throughout the sequence, the beam search tracks a **probability distribution over possible counts**. After each pitch, `advance_count_distribution` propagates the distribution forward using the model's predicted probabilities for BALL, STRIKE, and FOUL outcomes. This means later pitches in the sequence are evaluated under a realistic spread of possible counts, not a fixed assumption.

### Penalty functions

Several penalty terms are subtracted from the step score to encourage realistic, varied pitch sequences:

| Penalty | What it discourages |
|---|---|
| `repeat_penalty` | Throwing the same pitch type consecutively (scales with count risk) |
| `alternation_penalty` | Alternating between the same two pitch types repeatedly (A-B-A-B pattern) |
| `zone_repeat_penalty` | Throwing to the exact same zone location consecutively |
| `exact_action_repeat_penalty` | Repeating the same pitch type *and* zone (strongest penalty) |
| `pitch_type_frequency_penalty` | Using any pitch type more than once in the full sequence (minor, allows it but costs a small penalty) |
| `count_risk_penalty_from_dist` | Count distributions that drift into 3-ball territory |

### Beam search: `beam_search(...)`

The core algorithm. Explores sequences of pitches breadth-first, keeping only the top `beam_width` candidates at each step.

**Setup:**
1. Load the full set of valid `pitch_action` tokens from the encoder vocabulary
2. If `data_path` is provided, restrict actions to the pitcher's actual arsenal (`pitcher_arsenal`) — pitch types that make up at least 1% of that pitcher's historical pitches in the dataset
3. Initialise one `BeamState` at the starting count with an empty sequence and score 0

**Each step (repeated `depth` times):**
1. For every active beam × every candidate action, run `model_predict` for each count state in the current distribution
2. Compute expected step score (weighted average over count distribution)
3. Subtract all applicable penalties
4. Record the step metadata and advance the count distribution
5. Sort all candidate new beams by cumulative score (discounted by `GAMMA = 0.90` per step)
6. Keep the top `beam_width` beams

**Score accumulation:**

```
total_score += GAMMA^(step-1) × step_score
```

The `GAMMA = 0.90` discount factor means later pitches in the sequence contribute slightly less to the total score, reflecting increasing uncertainty further into the sequence.

**Output:** A list of `BeamState` objects sorted by score. `beams[0]` is the top recommendation. Each `BeamState.seq` contains one `step_obj` dict per pitch, with the `pitch_action` token (`"SL|Z9"`), expected count before/after, terminal probability, and the full outcome probability map.

### Pitcher arsenal filtering: `pitcher_arsenal(data_path, pitcher_id)`

Reads the processed parquet to find what pitch types a given pitcher has historically thrown. Pitch types appearing in fewer than 1% of that pitcher's pitches are excluded. The `PO` (pickoff) code is also banned. This prevents the model from recommending a knuckleball to a pitcher who only throws fastballs and sliders.

If the parquet is not available, arsenal filtering is skipped and all pitch types in the encoder vocabulary are considered.

### Feature engineering at inference time

`recommend_from_raw_state` applies the same feature transformations used during training before calling beam search:
- `inning_bucket(inning)` — maps raw inning to bucket string
- `tto_bucket(tto)` — maps times-through-order integer to bucket string
- `base_state(on_1b, on_2b, on_3b)` — encodes runner positions as a string
- `normalize_prev_action(s)` — ensures previous pitch tokens are in `"PT|Zx"` format or `"NONE"`

---

## 9. Model Artifacts

The saved model directory (`models/pitchsense_outcomes_v1/`) contains two files:

**`model.pt`** — a PyTorch checkpoint dict:
```python
{
  "state_dict":  ...,      # model weights
  "cat_sizes":   [...],    # vocabulary sizes for each categorical embedding
  "num_classes": 8,        # number of outcome classes
  "emb_dim":     16,
  "hidden_dim":  128,
  "dropout":     0.05,
}
```

**`encoders.json`** — vocabulary and training metadata:
```json
{
  "cat_cols": ["pitcher", "batter", "stand", ...],
  "num_cols": ["balls", "strikes", "runners_count", "outs"],
  "target_col": "outcome",
  "encoders": { "pitcher": {"classes": ["__UNK__", "123456", ...]}, ... },
  "target_encoder": {"classes": ["__UNK__", "BALL", "BB", "BIP_OUT", "FOUL", "HIT", "K", "OTHER", "STRIKE"]},
  "split_mode": "atbat",
  "best_epoch": ...,
  "best_val_loss": ...,
  "history": [...]
}
```

Unknown categorical values at inference time (players not seen during training) are mapped to the `__UNK__` token via the `LabelEncoder`, so the model degrades gracefully for new players rather than crashing.

---

## 10. Design Decisions Summary

| Decision | What was chosen | Why |
|---|---|---|
| Model type | MLP with entity embeddings | Simpler and faster to train than sequence models (RNN/Transformer); embeddings handle high-cardinality IDs |
| Prediction target | Pitch outcome (K/BB/HIT/etc.) | Reframes pitch selection as outcome prediction — the model learns what pitches work in each context |
| Sequence planning | Beam search at inference time | Decouples sequence logic from the model; allows adding domain-specific penalties without retraining |
| Count handling | Stochastic distribution propagation | More realistic than assuming a fixed count for future pitches; captures the uncertainty of intermediate outcomes |
| Class imbalance | Optional sqrt_inv class weights | Prevents the model from ignoring rare but important outcomes like hits and walks |
| Data split | At-bat level | Prevents data leakage across pitches in the same plate appearance |
| Arsenal filtering | 1% frequency threshold per pitcher | Prevents recommending pitch types a pitcher never throws; requires no separate dataset |
| Unknown players | `__UNK__` token | Graceful degradation for players not seen in training, rather than a crash |
