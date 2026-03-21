"""
overview.py — Compute overview stat cards (BA, K%, BB%, OBP, HR).

These are traditional counting/rate stats derived from the plate-appearance
outcomes recorded in the Statcast 'events' column.  Each PA ends with exactly
one pitch that has a non-null 'events' value; all other pitches in the PA have
null events and are ignored here.
"""

import pandas as pd


# ---------------------------------------------------------------------------
# Outcome groupings taken from the Statcast event vocabulary.
# ---------------------------------------------------------------------------

# At-bat outcomes: events that count as an official at-bat (exclude walks,
# HBP, sacrifices, and catcher's interference per official scoring rules).
_AB_EVENTS = {
    "single", "double", "triple", "home_run",
    "field_out", "strikeout", "grounded_into_double_play",
    "force_out", "double_play", "field_error",
    "fielders_choice", "fielders_choice_out",
    "strikeout_double_play", "triple_play", "other_out",
}

_HIT_EVENTS = {"single", "double", "triple", "home_run"}

# Both regular and intentional walks count toward BB% and OBP.
_WALK_EVENTS = {"walk", "intent_walk"}

_HBP_EVENTS = {"hit_by_pitch"}

# Sacrifice flies reduce the denominator in OBP but are NOT at-bats.
_SAC_FLY_EVENTS = {"sac_fly", "sac_fly_double_play"}


def _compute_stats(pa_df: pd.DataFrame) -> list[dict]:
    """
    Given a DataFrame of plate-appearance endings (rows where events is not
    null), return the five overview stat cards in the order the UI expects.

    Returns an empty list if pa_df is empty (e.g. no matchup data).
    """
    if pa_df.empty:
        return []

    events = pa_df["events"]

    pa    = len(pa_df)
    ab    = events.isin(_AB_EVENTS).sum()
    hits  = events.isin(_HIT_EVENTS).sum()
    walks = events.isin(_WALK_EVENTS).sum()
    hbp   = events.isin(_HBP_EVENTS).sum()
    sf    = events.isin(_SAC_FLY_EVENTS).sum()
    hr    = (events == "home_run").sum()
    ks    = (events == "strikeout").sum()

    # Batting average: hits / at-bats.
    ba = round(hits / ab, 3) if ab > 0 else 0.0

    # Strikeout rate as a percentage of plate appearances.
    k_pct = round(ks / pa * 100, 1) if pa > 0 else 0.0

    # Walk rate as a percentage of plate appearances.
    bb_pct = round(walks / pa * 100, 1) if pa > 0 else 0.0

    # On-base percentage: (H + BB + HBP) / (AB + BB + HBP + SF).
    obp_denom = ab + walks + hbp + sf
    obp = round((hits + walks + hbp) / obp_denom, 3) if obp_denom > 0 else 0.0

    # Home run total (raw count, not a rate).
    hr_count = int(hr)

    return [
        {"key": "ba",     "label": "BA",   "value": ba,       "isPrimary": True},
        {"key": "k_pct",  "label": "K%",   "value": k_pct,    "isPrimary": True},
        {"key": "bb_pct", "label": "BB%",  "value": bb_pct,   "isPrimary": True},
        {"key": "obp",    "label": "OBP",  "value": obp,      "isPrimary": False},
        {"key": "hr",     "label": "HR",   "value": hr_count, "isPrimary": False},
    ]


def compute_overview_stats(df: pd.DataFrame, pitcher_id: str | None = None) -> dict:
    """
    Build the overview response body for a given batter DataFrame.

    'general' is computed from the full season; 'pitcherSpecific' is computed
    from the subset of pitches thrown by the requested pitcher (empty list []
    when no pitcher is specified, per the API contract).

    Args:
        df:          Full season Statcast DataFrame for the batter.
        pitcher_id:  Optional MLBAM pitcher ID string to filter matchup data.

    Returns:
        { "general": [...], "pitcherSpecific": [...] }
    """
    # Isolate plate-appearance endings (one row per PA).
    pa_df = df[df["events"].notna()]

    general = _compute_stats(pa_df)

    if pitcher_id is not None:
        # Filter to PAs where this specific pitcher threw the final pitch.
        matchup_pa = pa_df[pa_df["pitcher"] == int(pitcher_id)]
        pitcher_specific = _compute_stats(matchup_pa)
    else:
        pitcher_specific = []

    return {"general": general, "pitcherSpecific": pitcher_specific}
