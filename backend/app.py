"""
app.py — Flask entry point for the PitchSense backend.

Exposes four endpoints under /api/v1:
    GET  /api/v1/overview/stats          — general + pitcher-specific stat cards
    GET  /api/v1/advanced/stats          — Statcast metrics + whiff by pitch type
    GET  /api/v1/heatmap                 — 5×5 strike-zone performance grid
    POST /api/v1/pitch-sequence/recommend — ML-backed pitch sequence recommendations

All endpoints are unauthenticated (MVP scope).
Run locally with:
    python app.py
The Android emulator reaches this server at http://10.0.2.2:5000/api/v1/.
The ML model is loaded in-process at startup from ML/models/pitchsense_v1.
Override with MODEL_DIR and DATA_PATH environment variables if needed.
"""

from flask import Flask, jsonify, request

from fetcher import DataNotFoundError, filter_by_pitcher, get_statcast_batter_data
import overview as ov
import advanced as adv
import heatmap as hm
import pitch_sequence as ps


app = Flask(__name__)

# Base path prefix shared by all routes, matching the API contract.
BASE = "/api/v1"


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _error(code: str, message: str, status: int, field: str | None = None):
    """
    Build a standardized error response matching the API contract's error shape:
        { "error": { "code": "...", "message": "...", "details": { "field": "..." } } }
    """
    body = {"error": {"code": code, "message": message}}
    if field:
        body["error"]["details"] = {"field": field}
    return jsonify(body), status


def _require_param(name: str) -> tuple[str | None, tuple | None]:
    """
    Extract a required query parameter from the current request.
    Returns (value, None) on success or (None, error_response) on failure.
    """
    value = request.args.get(name)
    if not value:
        return None, _error(
            "VALIDATION_ERROR",
            f"Missing required query parameter: {name}",
            400,
            field=name,
        )
    return value, None


def _parse_season() -> tuple[int | None, tuple | None]:
    """
    Read the optional 'season' query parameter and return it as an integer.
    Defaults to 2025 when not provided.  Returns an error tuple for bad values.
    """
    raw = request.args.get("season", "2025")
    try:
        season = int(raw)
    except ValueError:
        return None, _error(
            "VALIDATION_ERROR",
            f"'season' must be a four-digit year, got: {raw}",
            400,
            field="season",
        )
    return season, None


# ---------------------------------------------------------------------------
# Routes
# ---------------------------------------------------------------------------

@app.route(f"{BASE}/overview/stats", methods=["GET"])
def overview_stats():
    """
    Return general and pitcher-specific overview stat cards.

    Query params:
        batterId  (required) — MLBAM batter ID
        pitcherId (optional) — MLBAM pitcher ID; omit to skip pitcher column
        season    (optional) — four-digit year, defaults to 2026
    """
    batter_id, err = _require_param("batterId")
    if err:
        return err

    season, err = _parse_season()
    if err:
        return err

    pitcher_id = request.args.get("pitcherId")  # optional

    try:
        df = get_statcast_batter_data(batter_id, season)
    except DataNotFoundError as e:
        return _error("NOT_FOUND", str(e), 404, field="batterId")

    # If a pitcher was specified but has no matchup data, the repo returns an
    # empty pitcherSpecific array ([] per contract) — not an error.
    result = ov.compute_overview_stats(df, pitcher_id=pitcher_id)
    return jsonify(result), 200


@app.route(f"{BASE}/advanced/stats", methods=["GET"])
def advanced_stats():
    """
    Return advanced Statcast metrics and per-pitch-type whiff rates.

    Query params:
        batterId  (required) — MLBAM batter ID
        pitcherId (optional) — MLBAM pitcher ID to scope stats to matchup
        season    (optional) — four-digit year, defaults to 2026
    """
    batter_id, err = _require_param("batterId")
    if err:
        return err

    season, err = _parse_season()
    if err:
        return err

    pitcher_id = request.args.get("pitcherId")

    try:
        df = get_statcast_batter_data(batter_id, season)
    except DataNotFoundError as e:
        return _error("NOT_FOUND", str(e), 404, field="batterId")

    result = adv.compute_advanced_stats(df, pitcher_id=pitcher_id)
    return jsonify(result), 200


@app.route(f"{BASE}/heatmap", methods=["GET"])
def heatmap():
    """
    Return a 5×5 strike-zone heat map grid for a batter.

    Query params:
        batterId (required) — MLBAM batter ID
        metric   (required) — one of BA | SLG | OPS
        season   (optional) — four-digit year, defaults to 2026
    """
    batter_id, err = _require_param("batterId")
    if err:
        return err

    metric, err = _require_param("metric")
    if err:
        return err

    # Validate metric against the supported set defined in the API contract.
    allowed_metrics = {"BA", "SLG", "OPS"}
    if metric not in allowed_metrics:
        return _error(
            "VALIDATION_ERROR",
            f"'metric' must be one of {sorted(allowed_metrics)}, got: {metric}",
            400,
            field="metric",
        )

    season, err = _parse_season()
    if err:
        return err

    try:
        df = get_statcast_batter_data(batter_id, season)
    except DataNotFoundError as e:
        return _error("NOT_FOUND", str(e), 404, field="batterId")

    result = hm.compute_heatmap(df, metric)
    return jsonify(result), 200


@app.route(f"{BASE}/pitch-sequence/recommend", methods=["POST"])
def pitch_sequence_recommend():
    """
    Return a recommended pitch sequence for the current game situation.

    Both batterId and pitcherId are required.  The backend resolves batter/pitcher
    handedness from MLBAM IDs (via pybaseball) and forwards game state to the
    internal ML model service.  appliedScenario is derived server-side from the
    request; the model is not involved in that classification.

    Request body (JSON):
        batterId         (str, required)
        pitcherId        (str, required)
        pitchesToPredict (int, required, 1–3)
        gameSituation    (object, required):
            inning         int  1–9
            balls          int  0–3
            strikes        int  0–2
            outs           int  0–2
            runnerOnFirst  bool
            runnerOnSecond bool
            runnerOnThird  bool
    """
    body = request.get_json(silent=True)
    if not body:
        return _error("VALIDATION_ERROR", "Request body must be JSON.", 400)

    # --- Validate top-level required fields ---
    batter_id         = body.get("batterId")
    pitcher_id        = body.get("pitcherId")
    pitches_to_predict = body.get("pitchesToPredict")
    situation         = body.get("gameSituation")

    if not batter_id:
        return _error("VALIDATION_ERROR", "Missing required field: batterId", 400, field="batterId")
    if not pitcher_id:
        return _error("VALIDATION_ERROR", "Missing required field: pitcherId", 400, field="pitcherId")
    if pitches_to_predict is None:
        return _error("VALIDATION_ERROR", "Missing required field: pitchesToPredict", 400, field="pitchesToPredict")
    if not situation:
        return _error("VALIDATION_ERROR", "Missing required field: gameSituation", 400, field="gameSituation")

    try:
        pitches_to_predict = int(pitches_to_predict)
    except (TypeError, ValueError):
        return _error("VALIDATION_ERROR", "'pitchesToPredict' must be an integer.", 400, field="pitchesToPredict")
    if not 1 <= pitches_to_predict <= 3:
        return _error("VALIDATION_ERROR", "'pitchesToPredict' must be between 1 and 3.", 400, field="pitchesToPredict")

    # --- Validate gameSituation fields ---
    situation_fields = ["inning", "balls", "strikes", "outs",
                        "runnerOnFirst", "runnerOnSecond", "runnerOnThird"]
    for field in situation_fields:
        if field not in situation:
            return _error("VALIDATION_ERROR", f"Missing required gameSituation field: {field}", 400, field=f"gameSituation.{field}")

    try:
        inning          = int(situation["inning"])
        balls           = int(situation["balls"])
        strikes         = int(situation["strikes"])
        outs            = int(situation["outs"])
        runner_on_first  = bool(situation["runnerOnFirst"])
        runner_on_second = bool(situation["runnerOnSecond"])
        runner_on_third  = bool(situation["runnerOnThird"])
    except (TypeError, ValueError) as exc:
        return _error("VALIDATION_ERROR", f"Invalid gameSituation value: {exc}", 400)

    if not 1 <= inning <= 9:
        return _error("VALIDATION_ERROR", "'inning' must be 1–9.", 400, field="gameSituation.inning")
    if not 0 <= balls <= 3:
        return _error("VALIDATION_ERROR", "'balls' must be 0–3.", 400, field="gameSituation.balls")
    if not 0 <= strikes <= 2:
        return _error("VALIDATION_ERROR", "'strikes' must be 0–2.", 400, field="gameSituation.strikes")
    if not 0 <= outs <= 2:
        return _error("VALIDATION_ERROR", "'outs' must be 0–2.", 400, field="gameSituation.outs")

    # --- Resolve handedness (validates player IDs; returns 404 if unknown) ---
    stand = ps.get_batter_stand(batter_id)
    if stand is None:
        return _error("NOT_FOUND", f"Batter not found: {batter_id}", 404, field="batterId")

    p_throws = ps.get_pitcher_throws(pitcher_id)
    if p_throws is None:
        return _error("NOT_FOUND", f"Pitcher not found: {pitcher_id}", 404, field="pitcherId")

    # --- Call ML model service ---
    times_through = body.get("timesThrough")  # optional int, None means unknown

    model_payload = {
        "pitcher":    int(pitcher_id),
        "batter":     int(batter_id),
        "stand":      stand,
        "p_throws":   p_throws,
        "count":      f"{balls}-{strikes}",
        "outs":       outs,
        "on_1b":      int(runner_on_first),
        "on_2b":      int(runner_on_second),
        "on_3b":      int(runner_on_third),
        "inning":     inning,
        "tto":        int(times_through) if times_through is not None else None,
        "depth":      pitches_to_predict,
        "beam_width": 8,
    }

    try:
        model_response = ps.call_model_service(model_payload)
    except ps.ModelServiceError as exc:
        return _error("MODEL_UNAVAILABLE", f"Pitch sequence model unavailable: {exc}", 502)

    sequences = model_response.get("sequences", [])
    if not sequences or not sequences[0].get("sequence"):
        return _error("MODEL_UNAVAILABLE", "Model returned no sequences.", 502)

    # --- Build response ---
    applied_scenario = ps.derive_applied_scenario(
        balls=balls,
        strikes=strikes,
        outs=outs,
        inning=inning,
        runner_on_first=runner_on_first,
        runner_on_second=runner_on_second,
        runner_on_third=runner_on_third,
    )

    all_scores    = [seq["score"] for seq in sequences]
    top_sequence  = sequences[0]["sequence"]
    recommendations = ps.build_recommendations(
        top_sequence=top_sequence,
        all_scores=all_scores,
        balls=balls,
        strikes=strikes,
        pitches_to_predict=pitches_to_predict,
    )

    return jsonify({
        "appliedScenario": applied_scenario,
        "recommendations": recommendations,
    }), 200


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    # host="0.0.0.0" makes the server reachable from the Android emulator via
    # the special alias 10.0.2.2 which maps to the host machine's loopback.
    app.run(host="0.0.0.0", port=5000, debug=True)
