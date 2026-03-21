"""
app.py — Flask entry point for the PitchSense backend.

Exposes three read-only endpoints under /api/v1:
    GET  /api/v1/overview/stats   — general + pitcher-specific stat cards
    GET  /api/v1/advanced/stats   — Statcast metrics + whiff by pitch type
    GET  /api/v1/heatmap          — 5×5 strike-zone performance grid

All endpoints are unauthenticated (MVP scope).
Run locally with:
    python app.py
The Android emulator reaches this server at http://10.0.2.2:5000/api/v1/.
"""

from flask import Flask, jsonify, request

from fetcher import DataNotFoundError, filter_by_pitcher, get_statcast_batter_data
import overview as ov
import advanced as adv
import heatmap as hm


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
    Defaults to 2026 when not provided.  Returns an error tuple for bad values.
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


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    # host="0.0.0.0" makes the server reachable from the Android emulator via
    # the special alias 10.0.2.2 which maps to the host machine's loopback.
    app.run(host="0.0.0.0", port=5000, debug=True)
