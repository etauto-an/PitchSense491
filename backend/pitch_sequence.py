"""
pitch_sequence.py — Pitch sequence recommendation logic for the backend API.

Handles three responsibilities:
  1. Handedness lookup — resolves 'stand' and 'p_throws' from MLBAM player IDs via
     pybaseball, since these are required by the ML model but absent from the API request.
  2. Model inference — calls recommend_from_raw_state() directly from ML/src, loading
     the model once at startup rather than routing through a separate HTTP service.
  3. Response mapping — converts raw beam-search token sequences into the API response
     shape defined in docs/backend-api-contract.md §4.1.
"""

import os
import sys

import pybaseball
import torch

# ---------------------------------------------------------------------------
# ML model bootstrap
# ---------------------------------------------------------------------------

# Resolve ML/src relative to this file so the import works regardless of CWD.
_ML_SRC = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "ML", "src"))
sys.path.insert(0, _ML_SRC)

from recommend_sequence import load_model, recommend_from_raw_state  # noqa: E402

# Default paths are anchored to the ML/ directory tree so callers don't need
# to set env vars for the typical single-repo layout.
_ML_ROOT  = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "ML"))
MODEL_DIR = os.environ.get("MODEL_DIR", os.path.join(_ML_ROOT, "models", "pitchsense_outcomes_v1"))
DATA_PATH = os.environ.get("DATA_PATH", os.path.join(_ML_ROOT, "data", "processed", "pitchsense_outcomes_v0.parquet"))

_device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

print(f"[pitch_sequence] Loading model from '{MODEL_DIR}' on {_device} ...")
_model, _encoders, _y_enc, _cat_cols, _num_cols, _ = load_model(MODEL_DIR, _device)
print("[pitch_sequence] Model ready.")

# Arsenal filtering requires the processed parquet; skip silently if absent.
_data_path = DATA_PATH if os.path.exists(DATA_PATH) else None
if _data_path is None:
    print(f"[pitch_sequence] WARNING: DATA_PATH '{DATA_PATH}' not found — arsenal filtering disabled.")


# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------

# Pitch code → display name (client renders these strings directly).
_PITCH_NAME = {
    "FF": "4-Seam Fastball",
    "SI": "Sinker",
    "FC": "Cutter",
    "SL": "Slider",
    "CU": "Curveball",
    "CH": "Changeup",
    "FS": "Splitter",
    "ST": "Sweeper",
    "SV": "Slurve",
    "KC": "Knuckle Curve",
    "KN": "Knuckleball",
    "EP": "Eephus",
}

# Zone token → natural-language location phrase.
# Uses height and side language only — pitch speed is not available from model output.
_ZONE_PHRASE = {
    "Z1": "Up and in at the hands",
    "Z2": "Top of the zone",
    "Z3": "High and away",
    "Z4": "Back-foot, middle band",
    "Z5": "Heart of the plate",
    "Z6": "Away, middle band",
    "Z7": "Low and in at the knees",
    "Z8": "Low in the zone",
    "Z9": "Low and away",
    # Zones 11-14 are the four ball quadrants surrounding the strike zone
    "Z11": "High and inside, off the plate",
    "Z12": "High and away, off the plate",
    "Z13": "Low and inside, off the plate",
    "Z14": "Low and away, off the plate",
    # Legacy token and fallback for unknown location
    "OZ": "Off the plate",
    "UNK_LOC": "Off the plate",
}


# ---------------------------------------------------------------------------
# Handedness lookup
# ---------------------------------------------------------------------------

# In-memory cache keyed by MLBAM player ID integer.
# Populated on first request per player; lives for the process lifetime.
_handedness_cache: dict[int, dict[str, str]] = {}


def _lookup_handedness(player_id: str) -> dict[str, str] | None:
    """
    Return {"bats": "L"|"R", "throws": "L"|"R"} for an MLBAM player ID.

    Uses pybaseball.playerid_reverse_lookup and caches the result so each
    player is fetched at most once per process.  Switch hitters are treated
    as right-handed batters for the MVP.  Returns None if the player is not
    found or the lookup raises an exception.
    """
    key = int(player_id)
    if key in _handedness_cache:
        return _handedness_cache[key]

    try:
        df = pybaseball.playerid_reverse_lookup([key], key_type="mlbam")
    except Exception:
        return None

    if df is None or df.empty:
        return None

    row = df.iloc[0]
    bats   = str(row.get("bats",   "R")).upper().strip()
    throws = str(row.get("throws", "R")).upper().strip()

    result = {
        # Switch hitters ("S") treated as right-handed for MVP scope.
        "bats":   "R" if bats   not in ("L", "R") else bats,
        "throws": "R" if throws not in ("L", "R") else throws,
    }
    _handedness_cache[key] = result
    return result


def get_batter_stand(batter_id: str) -> str | None:
    """Return 'L' or 'R' for a batter's handedness, or None if not found."""
    h = _lookup_handedness(batter_id)
    return h["bats"] if h else None


def get_pitcher_throws(pitcher_id: str) -> str | None:
    """Return 'L' or 'R' for a pitcher's throwing arm, or None if not found."""
    h = _lookup_handedness(pitcher_id)
    return h["throws"] if h else None


# ---------------------------------------------------------------------------
# appliedScenario derivation
# ---------------------------------------------------------------------------

def derive_applied_scenario(
    balls: int,
    strikes: int,
    outs: int,
    inning: int,
    runner_on_first: bool,
    runner_on_second: bool,
    runner_on_third: bool,
) -> str:
    """
    Map game situation to one of the five scenario labels.
    Evaluated in priority order per the API contract (§4.1).
    """
    runners = int(runner_on_first) + int(runner_on_second) + int(runner_on_third)
    # High-leverage: late game with runners, or two outs with multiple runners.
    high_leverage = (inning >= 7 and runners >= 1) or (outs == 2 and runners >= 2)

    if strikes == 2:
        return "Two-Strike Putaway"
    if balls == 3:
        return "Hitter Count Damage Control"
    if high_leverage:
        return "High Leverage"
    if runner_on_third and outs < 2:
        return "Runner on 3rd, <2 Outs"
    return "Neutral Situation"


# ---------------------------------------------------------------------------
# Token → recommendation mapping
# ---------------------------------------------------------------------------

def _parse_token(token: str) -> tuple[str, str]:
    """
    Split a 'PT|Zx' action token into (pitch_code, zone).
    Falls back to (token, 'OZ') for malformed tokens.
    """
    if "|" in token:
        pt, zone = token.split("|", 1)
        return pt.upper(), zone.upper()
    return token.upper(), "OZ"


def _make_description(zone: str, balls: int, strikes: int) -> str:
    """
    Build a location + intent phrase for a recommended pitch.

    Location comes from the zone token; intent comes from the current count.
    Pitch speed is intentionally excluded — it is not available from model output.
    """
    location = _ZONE_PHRASE.get(zone, "Off the plate")
    # Z11-Z14 are ball zones outside the strike zone; OZ and UNK_LOC are legacy/unknown out-of-zone tokens
    is_chase = zone in {"OZ", "UNK_LOC", "Z11", "Z12", "Z13", "Z14"}

    if strikes == 2 and is_chase:
        intent = "chase for strike three"
    elif strikes == 2:
        intent = "finish the at-bat"
    elif balls == 3:
        intent = "must locate a strike"
    elif is_chase:
        intent = "get a chase"
    elif balls > strikes:
        intent = "steal a strike"
    else:
        intent = "work the count"

    return f"{location} — {intent}"


def _normalize_effectiveness(scores: list[float]) -> list[int]:
    """
    Min-max normalize a list of beam scores into the 65–90 range.

    Each step i in the top sequence is assigned the normalized score of
    sequences[i] so that per-step confidence decreases naturally as we
    rely on lower-ranked beam candidates for later pitches.
    Returns [80] when there is only one score or all scores are equal.
    """
    if len(scores) == 1:
        return [80]
    lo, hi = min(scores), max(scores)
    if hi == lo:
        return [80] * len(scores)
    return [round(65 + (s - lo) / (hi - lo) * 25) for s in scores]


def build_recommendations(
    top_sequence: list[str],
    all_scores: list[float],
    balls: int,
    strikes: int,
    pitches_to_predict: int,
) -> list[dict]:
    """
    Convert the top beam-search sequence into the API recommendations array.

    top_sequence:      'PT|Zx' token list from sequences[0].sequence
    all_scores:        cumulative scores for all returned sequences (for normalization)
    balls / strikes:   starting count from the request (used for description intent)
    pitches_to_predict: trim the sequence to this length
    """
    tokens = top_sequence[:pitches_to_predict]
    effectiveness = _normalize_effectiveness(all_scores)

    recommendations = []
    for i, token in enumerate(tokens):
        pitch_code, zone = _parse_token(token)
        pitch_name  = _PITCH_NAME.get(pitch_code, pitch_code)
        description = _make_description(zone, balls, strikes)

        # Step i uses the normalized score of sequences[i] — the next-best
        # candidate — so effectiveness decreases with each subsequent pitch.
        eff = effectiveness[min(i, len(effectiveness) - 1)]

        recommendations.append({
            "step":            i + 1,
            "pitchType":       pitch_name,
            "description":     description,
            "effectivenessPct": eff,
        })

    return recommendations


# ---------------------------------------------------------------------------
# Model inference
# ---------------------------------------------------------------------------

class ModelServiceError(Exception):
    """Raised when ML model inference fails, mapping to a 502 response in app.py."""
    pass


def call_model_service(payload: dict) -> dict:
    """
    Run pitch sequence inference using the embedded ML model.

    Accepts the same payload dict shape that was previously POSTed to the
    separate HTTP model service, so app.py requires no changes.  Calls
    recommend_from_raw_state() in-process and returns a dict matching the
    shape that app.py expects: {"sequences": [{"score": float, "sequence": [...]}]}.

    Raises ModelServiceError on inference failure so callers receive the same
    502 error path as before.
    """
    # Parse the "B-S" count string into separate ints.
    count_raw = payload["count"]
    balls_raw, strikes_raw = count_raw.split("-", 1)
    balls, strikes = int(balls_raw), int(strikes_raw)

    try:
        beams = recommend_from_raw_state(
            model=_model,
            encoders=_encoders,
            y_enc=_y_enc,
            cat_cols=_cat_cols,
            num_cols=_num_cols,
            device=_device,
            data_path=_data_path,
            pitcher=int(payload["pitcher"]),
            batter=int(payload["batter"]),
            stand=str(payload["stand"]),
            p_throws=str(payload["p_throws"]),
            balls=balls,
            strikes=strikes,
            outs=int(payload.get("outs", 0)),
            on_1b=int(payload.get("on_1b", 0)),
            on_2b=int(payload.get("on_2b", 0)),
            on_3b=int(payload.get("on_3b", 0)),
            inning=payload.get("inning"),
            tto=payload.get("tto"),
            depth=int(payload.get("depth", 3)),
            beam_width=int(payload.get("beam_width", 8)),
            prev1=str(payload.get("prev1", "NONE")),
            prev2=str(payload.get("prev2", "NONE")),
        )
    except Exception as exc:
        raise ModelServiceError(f"Model inference failed: {exc}") from exc

    if not beams:
        raise ModelServiceError("Model returned no sequences.")

    sequences = [
        {
            "score":    round(b.score, 4),
            "sequence": [step["pitch_action"] for step in b.seq],
        }
        for b in beams
    ]
    return {"sequences": sequences}
