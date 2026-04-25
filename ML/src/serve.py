"""
serve.py — Internal Flask model service for PitchSense pitch sequence recommendations.

Wraps recommend_from_raw_state() from recommend_sequence.py and exposes it as:

    POST http://127.0.0.1:5001/recommend/sequence

This is an internal service called by the backend Flask app (backend/app.py).
It is NOT exposed directly to the Android client.

Start with:
    MODEL_DIR=models/pitchsense_v1 \
    DATA_PATH=data/processed/outcomes_with_outs.parquet \
    python src/serve.py

Both paths are relative to the ML/ directory, so run from there.
"""

import os
import sys

import torch
from flask import Flask, jsonify, request

# Import inference functions from the same src/ directory.
sys.path.insert(0, os.path.dirname(__file__))
from recommend_sequence import load_model, recommend_from_raw_state


app = Flask(__name__)


# ---------------------------------------------------------------------------
# Model loading — happens once at startup, shared across all requests.
# ---------------------------------------------------------------------------

MODEL_DIR = os.environ.get("MODEL_DIR", "models/pitchsense_v1")
DATA_PATH = os.environ.get("DATA_PATH", "data/processed/outcomes_with_outs.parquet")

device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

print(f"[serve] Loading model from '{MODEL_DIR}' on {device} ...")
model, encoders, y_enc, cat_cols, num_cols, _ = load_model(MODEL_DIR, device)
print("[serve] Model ready.")

# Only use parquet for arsenal filtering if the file actually exists.
_data_path = DATA_PATH if os.path.exists(DATA_PATH) else None
if _data_path is None:
    print(f"[serve] WARNING: DATA_PATH '{DATA_PATH}' not found — arsenal filtering disabled.")


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _parse_count(raw: str) -> tuple[int, int]:
    """
    Parse a count string like "1-2" into (balls, strikes).
    Raises ValueError on bad input.
    """
    parts = str(raw).split("-")
    if len(parts) != 2:
        raise ValueError(f"expected 'B-S' format, got {raw!r}")
    return int(parts[0]), int(parts[1])


def _bad(message: str, status: int = 400):
    """Return a minimal error JSON response."""
    return jsonify({"error": message}), status


# ---------------------------------------------------------------------------
# Route
# ---------------------------------------------------------------------------

@app.route("/recommend/sequence", methods=["POST"])
def recommend_sequence():
    """
    Accept a game-state JSON body and return ranked pitch sequences.

    Required fields:
        pitcher   (int)  MLBAM pitcher ID
        batter    (int)  MLBAM batter ID
        stand     (str)  batter handedness: "L" or "R"
        p_throws  (str)  pitcher handedness: "L" or "R"
        count     (str)  "{balls}-{strikes}", e.g. "1-2"
        outs      (int)  0–2

    Optional fields:
        on_1b      (int)  1 if runner on 1st, else 0  (default 0)
        on_2b      (int)  1 if runner on 2nd, else 0  (default 0)
        on_3b      (int)  1 if runner on 3rd, else 0  (default 0)
        inning     (int)  1–9 (default None — treated as mid-game)
        depth      (int)  pitches to plan ahead        (default 3)
        beam_width (int)  parallel sequences to track  (default 8)
        prev1      (str)  previous pitch action "PT|Zx" or "NONE" (default "NONE")
        prev2      (str)  pitch before that, or "NONE"            (default "NONE")
        tto        (int)  times through order (default None)

    Response 200:
        {
          "pitcher": 808967, "batter": 665489,
          "count": "1-2", "outs": 1,
          "on_1b": 1, "on_2b": 0, "on_3b": 0,
          "sequences": [
            { "score": 0.1657, "sequence": ["CH|Z3", "CU|Z7", "FF|Z2"] },
            ...
          ]
        }
    """
    body = request.get_json(silent=True)
    if not body:
        return _bad("Request body must be JSON.")

    # Validate required fields are present before trying to parse them.
    required = ("pitcher", "batter", "stand", "p_throws", "count", "outs")
    missing = [f for f in required if f not in body]
    if missing:
        return _bad(f"Missing required fields: {', '.join(missing)}.")

    # Parse and validate all inputs.
    try:
        pitcher   = int(body["pitcher"])
        batter    = int(body["batter"])
        stand     = str(body["stand"]).upper()
        p_throws  = str(body["p_throws"]).upper()
        outs      = int(body["outs"])
        count_raw = body["count"]
        balls, strikes = _parse_count(count_raw)

        on_1b      = int(body.get("on_1b", 0))
        on_2b      = int(body.get("on_2b", 0))
        on_3b      = int(body.get("on_3b", 0))
        depth      = int(body.get("depth", 3))
        beam_width = int(body.get("beam_width", 8))
        prev1      = str(body.get("prev1", "NONE"))
        prev2      = str(body.get("prev2", "NONE"))
        inning     = int(body["inning"]) if body.get("inning") is not None else None
        tto        = int(body["tto"])    if body.get("tto")    is not None else None

    except (ValueError, TypeError) as exc:
        return _bad(f"Invalid field value: {exc}.")

    if stand not in ("L", "R"):
        return _bad(f"'stand' must be 'L' or 'R', got {stand!r}.")
    if p_throws not in ("L", "R"):
        return _bad(f"'p_throws' must be 'L' or 'R', got {p_throws!r}.")

    # Run beam search.
    try:
        beams = recommend_from_raw_state(
            model=model,
            encoders=encoders,
            y_enc=y_enc,
            cat_cols=cat_cols,
            num_cols=num_cols,
            device=device,
            data_path=_data_path,
            pitcher=pitcher,
            batter=batter,
            stand=stand,
            p_throws=p_throws,
            balls=balls,
            strikes=strikes,
            outs=outs,
            on_1b=on_1b,
            on_2b=on_2b,
            on_3b=on_3b,
            inning=inning,
            tto=tto,
            depth=depth,
            beam_width=beam_width,
            prev1=prev1,
            prev2=prev2,
        )
    except Exception as exc:
        print(f"[serve] ERROR during inference: {exc}")
        return _bad(f"Model inference failed: {exc}", status=500)

    if not beams:
        return _bad("Model returned no sequences.", status=500)

    # Convert BeamState objects to the shape the backend adapter expects
    # (see docs/backend-api-contract.md §4.1 model response example).
    sequences = [
        {
            "score":    round(b.score, 4),
            "sequence": [step["pitch_action"] for step in b.seq],
        }
        for b in beams
    ]

    return jsonify({
        "pitcher":   pitcher,
        "batter":    batter,
        "count":     count_raw,
        "outs":      outs,
        "on_1b":     on_1b,
        "on_2b":     on_2b,
        "on_3b":     on_3b,
        "sequences": sequences,
    }), 200


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    # Bind to localhost only — this service is internal and should not be
    # reachable from outside the host machine.  Port 5001 avoids conflicting
    # with the backend Flask app which runs on port 5000.
    app.run(host="127.0.0.1", port=5001, debug=False)
