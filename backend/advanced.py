"""
advanced.py — Compute advanced Statcast metrics for the Advanced Stats screen.

Metrics fall into three groups:
  1. Expected stats  — xwOBA, xBA, xSLG (model-based, per batted ball)
  2. Batted ball     — Hard Hit%, Barrel%, Sweet Spot%, Avg EV, Max EV
  3. Discipline      — Chase%, Zone Contact%, Whiff%, CSW%

All percentages are returned as numbers (e.g. 27.1) not decimals; rate stats
like xwOBA are returned as decimals (e.g. 0.372).  This matches the API contract.
"""

import numpy as np
import pandas as pd


# ---------------------------------------------------------------------------
# Pitch description groupings from the Statcast vocabulary.
# ---------------------------------------------------------------------------

# Swing outcomes — any description where the batter offered at the pitch.
_SWING_DESCS = {
    "swinging_strike",
    "swinging_strike_blocked",
    "foul",
    "foul_tip",
    "foul_bunt",
    "bunt_foul_tip",
    "missed_bunt",
    "hit_into_play",
    "hit_into_play_no_out",
    "hit_into_play_score",
}

# Whiff (missed swing) outcomes — a subset of swings.
_WHIFF_DESCS = {
    "swinging_strike",
    "swinging_strike_blocked",
    "missed_bunt",
}

# Called or swinging strike — used for CSW% (Called Strike + Whiff rate).
_CSW_DESCS = _WHIFF_DESCS | {"called_strike"}

# Statcast zones 1-9 are inside the rulebook strike zone; 11-14 are chase zones.
_IN_ZONE = set(range(1, 10))
_OUT_ZONE = {11, 12, 13, 14}


# ---------------------------------------------------------------------------
# Pitch type code → display name mapping (per API contract).
# ---------------------------------------------------------------------------
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


def _safe_mean(series: pd.Series) -> float | None:
    """Return the mean of non-null values, or None if the series is empty."""
    valid = series.dropna()
    return float(round(valid.mean(), 3)) if not valid.empty else None


def _pct(numerator: int | float, denominator: int | float, decimals: int = 1) -> float:
    """
    Compute a percentage rounded to 'decimals' places.
    Returns 0.0 when the denominator is zero to avoid division errors.
    """
    return round(numerator / denominator * 100, decimals) if denominator > 0 else 0.0


def _compute_discipline(df: pd.DataFrame) -> dict:
    """
    Compute plate-discipline metrics from pitch-level data.

    Chase%       — swings on out-of-zone pitches / total out-of-zone pitches
    Whiff%       — missed swings / total swings
    Zone Contact%— in-zone swings that made contact / total in-zone swings
    CSW%         — (called strikes + whiffs) / total pitches
    """
    desc = df["description"]
    zone = df["zone"]

    is_swing      = desc.isin(_SWING_DESCS)
    is_whiff      = desc.isin(_WHIFF_DESCS)
    is_csw        = desc.isin(_CSW_DESCS)
    is_in_zone    = zone.isin(_IN_ZONE)
    is_out_zone   = zone.isin(_OUT_ZONE)
    is_contact    = desc.isin(_SWING_DESCS - _WHIFF_DESCS)  # swings that weren't misses

    chase_pct = _pct(
        (is_swing & is_out_zone).sum(),
        is_out_zone.sum(),
    )
    whiff_pct = _pct(
        is_whiff.sum(),
        is_swing.sum(),
    )
    zone_contact_pct = _pct(
        (is_contact & is_in_zone).sum(),
        (is_swing & is_in_zone).sum(),
    )
    csw_pct = _pct(is_csw.sum(), len(df))

    return {
        "chase_pct":        chase_pct,
        "whiff_pct":        whiff_pct,
        "zone_contact_pct": zone_contact_pct,
        "csw_pct":          csw_pct,
    }


def _compute_batted_ball(df: pd.DataFrame) -> dict:
    """
    Compute batted-ball quality metrics from pitches put in play (type == 'X').

    Hard Hit% — exit velocity ≥ 95 mph
    Barrel%   — uses the 'barrel' column when available; otherwise 0
    Sweet Spot% — launch angle between 8° and 32° (optimal trajectory range)
    Avg EV    — mean exit velocity across all batted balls
    Max EV    — maximum exit velocity in the sample
    """
    # Restrict to pitches actually put in play.
    bb = df[df["type"] == "X"].copy()

    # Exit velocity is the basis for most batted-ball stats.
    ev_valid = bb["launch_speed"].dropna()
    la_valid = bb["launch_angle"].dropna()

    total_bb = len(ev_valid)

    hard_hit_pct  = _pct((ev_valid >= 95).sum(), total_bb)
    sweet_spot_pct = _pct(((la_valid >= 8) & (la_valid <= 32)).sum(), len(la_valid))

    avg_ev = round(float(ev_valid.mean()), 1) if not ev_valid.empty else 0.0
    max_ev = round(float(ev_valid.max()), 1) if not ev_valid.empty else 0.0

    # Barrel% uses the pre-computed 'barrel' column from Statcast when present.
    if "barrel" in bb.columns:
        barrel_pct = _pct(bb["barrel"].fillna(0).sum(), total_bb)
    else:
        barrel_pct = 0.0

    return {
        "hard_hit_pct":   hard_hit_pct,
        "barrel_pct":     barrel_pct,
        "sweet_spot_pct": sweet_spot_pct,
        "avg_ev":         avg_ev,
        "max_ev":         max_ev,
    }


def _compute_expected_stats(df: pd.DataFrame) -> dict:
    """
    Compute expected (Statcast model-based) stats from batted-ball rows.

    xwOBA and xBA are averages of the per-pitch model estimates; xSLG uses
    'estimated_slg_using_speedangle' when available in the dataset.
    """
    bb = df[df["type"] == "X"]

    xwoba = _safe_mean(bb.get("estimated_woba_using_speedangle", pd.Series(dtype=float)))
    xba   = _safe_mean(bb.get("estimated_ba_using_speedangle",   pd.Series(dtype=float)))
    xslg  = _safe_mean(bb.get("estimated_slg_using_speedangle",  pd.Series(dtype=float)))

    return {
        "xwoba": xwoba or 0.0,
        "xba":   xba   or 0.0,
        "xslg":  xslg  or 0.0,
    }


def _compute_whiff_by_pitch_type(df: pd.DataFrame) -> list[dict]:
    """
    Break down whiff rate by pitch type so the UI can display a per-pitch
    breakdown table.  Only pitch types with at least 10 swings are included
    to avoid misleading percentages from tiny samples.
    """
    results = []
    is_swing = df["description"].isin(_SWING_DESCS)
    is_whiff = df["description"].isin(_WHIFF_DESCS)

    for code, group in df.groupby("pitch_type"):
        swings = is_swing[group.index].sum()
        if swings < 10:
            # Too small a sample to report meaningful whiff rate.
            continue
        whiffs = is_whiff[group.index].sum()
        display_name = _PITCH_NAME.get(str(code), str(code))
        results.append({
            "pitchType": display_name,
            "whiffPct":  round(whiffs / swings * 100, 1),
        })

    # Sort descending by whiff rate so highest-whiff pitches appear first.
    results.sort(key=lambda x: x["whiffPct"], reverse=True)
    return results


def compute_advanced_stats(df: pd.DataFrame, pitcher_id: str | None = None) -> dict:
    """
    Build the advanced stats response body for a given batter DataFrame.

    When pitcher_id is provided, all metrics are computed from the batter's
    at-bats against that specific pitcher only.

    Returns:
        {
            "directMetrics":    [ { key, label, value }, ... ],
            "whiffByPitchType": [ { pitchType, whiffPct }, ... ],
        }
    """
    if pitcher_id is not None:
        df = df[df["pitcher"] == int(pitcher_id)]

    if df.empty:
        return {"directMetrics": [], "whiffByPitchType": []}

    discipline   = _compute_discipline(df)
    batted_ball  = _compute_batted_ball(df)
    expected     = _compute_expected_stats(df)
    whiff_by_pt  = _compute_whiff_by_pitch_type(df)

    # Flatten into the ordered list the UI renders as stat cards.
    direct_metrics = [
        {"key": "xwoba",            "label": "xwOBA",          "value": expected["xwoba"]},
        {"key": "xba",              "label": "xBA",             "value": expected["xba"]},
        {"key": "xslg",             "label": "xSLG",            "value": expected["xslg"]},
        {"key": "hard_hit_pct",     "label": "Hard Hit%",       "value": batted_ball["hard_hit_pct"]},
        {"key": "barrel_pct",       "label": "Barrel%",         "value": batted_ball["barrel_pct"]},
        {"key": "avg_ev",           "label": "Avg EV",          "value": batted_ball["avg_ev"]},
        {"key": "max_ev",           "label": "Max EV",          "value": batted_ball["max_ev"]},
        {"key": "sweet_spot_pct",   "label": "Sweet Spot%",     "value": batted_ball["sweet_spot_pct"]},
        {"key": "chase_pct",        "label": "Chase%",          "value": discipline["chase_pct"]},
        {"key": "whiff_pct",        "label": "Whiff%",          "value": discipline["whiff_pct"]},
        {"key": "zone_contact_pct", "label": "Zone Contact%",   "value": discipline["zone_contact_pct"]},
        {"key": "csw_pct",          "label": "CSW%",            "value": discipline["csw_pct"]},
    ]

    return {"directMetrics": direct_metrics, "whiffByPitchType": whiff_by_pt}
