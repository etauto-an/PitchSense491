"""
heatmap.py — Build the 5×5 strike-zone heat map grid.

The grid maps pitch locations (plate_x, plate_z) onto a 5×5 matrix where:
  - Rows 0–4 run top-to-bottom (row 0 = high chase, row 4 = low chase).
  - Cols 0–4 run left-to-right from the catcher's perspective
    (col 0 = far off outside edge, col 4 = far off inside edge for
    a right-handed batter).

Only the inner 3×3 (rows 1–3, cols 1–3) represents the rulebook strike zone;
the outer ring is labeled OUTER regardless of the stat value.

Metrics supported: BA, SLG, OPS (as per the API contract).
"""

import numpy as np
import pandas as pd


# ---------------------------------------------------------------------------
# Zone boundary definitions.
#
# Home plate is 17 inches (1.417 ft) wide.  The strike zone extends ±0.708 ft
# from the center of the plate.  We divide that into three equal columns of
# 0.472 ft each, then add an outer "chase" column on each side.
#
# Strike zone height is approximated as 1.5 ft – 3.5 ft (a 2.0 ft window)
# split into three rows of ~0.667 ft each.
# ---------------------------------------------------------------------------

# Horizontal bin edges (plate_x): left-to-right from catcher's perspective.
# Negative plate_x = catcher's left (first-base side for a right-handed batter).
X_BINS = [-np.inf, -0.708, -0.236, 0.236, 0.708, np.inf]

# Vertical bin edges (plate_z): top-to-bottom (row index increases downward).
# Note: pd.cut uses (left, right] intervals, so we reverse the ordering here
# and flip the resulting category codes after binning.
Z_BINS = [np.inf, 3.5, 2.833, 2.167, 1.5, -np.inf]  # descending → row 0 at top


# ---------------------------------------------------------------------------
# Performance level thresholds per metric.
# OUTER is assigned based on grid position, not stat value.
# ---------------------------------------------------------------------------

_THRESHOLDS = {
    "BA": [
        ("ELITE",     0.350),
        ("GOOD",      0.280),
        ("BELOW_AVG", 0.200),
        ("WEAK",      0.0),
    ],
    "SLG": [
        ("ELITE",     0.600),
        ("GOOD",      0.450),
        ("BELOW_AVG", 0.300),
        ("WEAK",      0.0),
    ],
    "OPS": [
        ("ELITE",     0.950),
        ("GOOD",      0.750),
        ("BELOW_AVG", 0.550),
        ("WEAK",      0.0),
    ],
}


def _classify_level(value: float, metric: str) -> str:
    """
    Map a stat value to a performance level string using the metric's thresholds.
    Iterates from best to worst and returns the first level whose minimum is met.
    """
    for level, minimum in _THRESHOLDS[metric]:
        if value >= minimum:
            return level
    return "WEAK"


def _compute_cell_stat(cell_df: pd.DataFrame, metric: str) -> tuple[float, str]:
    """
    Compute the requested metric for pitches whose contact point falls in a
    single grid cell.  Returns (value, level).

    Only pitches put in play (type == 'X') contribute to BA, SLG, and OPS
    because those stats require a batted-ball outcome.  Cells with no batted
    balls return (0.0, 'WEAK').

    OPS here is computed as zone_BA + zone_SLG (an approximation; true OPS
    would require walk/HBP data per zone which Statcast doesn't readily expose).
    """
    bb = cell_df[cell_df["type"] == "X"]
    if bb.empty:
        return 0.0, "WEAK"

    events  = bb["events"].dropna()
    n_balls = len(events)  # each in-play pitch ends a PA, so this ≈ at-bats

    if n_balls == 0:
        return 0.0, "WEAK"

    hits    = events.isin({"single", "double", "triple", "home_run"}).sum()
    doubles = (events == "double").sum()
    triples = (events == "triple").sum()
    hrs     = (events == "home_run").sum()
    singles = hits - doubles - triples - hrs

    # Total bases: 1B=1, 2B=2, 3B=3, HR=4.
    total_bases = singles + (2 * doubles) + (3 * triples) + (4 * hrs)

    ba  = hits / n_balls
    slg = total_bases / n_balls

    if metric == "BA":
        value = round(ba, 3)
    elif metric == "SLG":
        value = round(slg, 3)
    else:  # OPS
        value = round(ba + slg, 3)

    return value, _classify_level(value, metric)


def compute_heatmap(df: pd.DataFrame, metric: str) -> dict:
    """
    Build the full 5×5 grid response for the heat map endpoint.

    Steps:
      1. Assign each pitch to a row (0–4) and column (0–4) based on its
         plate_x and plate_z location.
      2. For each cell, compute the requested metric from batted-ball outcomes.
      3. Mark cells in the outer ring (row 0/4 or col 0/4) as OUTER.

    Args:
        df:     Full season Statcast DataFrame for the batter.
        metric: One of 'BA', 'SLG', 'OPS'.

    Returns:
        { "metric": "BA", "grid": [[...5 cols...] × 5 rows] }
    """
    # Drop pitches with missing location data (rare, but present in some games).
    loc_df = df.dropna(subset=["plate_x", "plate_z"]).copy()

    # Assign column index (0–4) based on horizontal location.
    loc_df["col"] = pd.cut(
        loc_df["plate_x"],
        bins=X_BINS,
        labels=[0, 1, 2, 3, 4],
        right=True,
    ).astype(int)

    # Assign row index (0–4) from top to bottom.  pd.cut with descending bins
    # gives category 0 = highest z (top of zone), 4 = lowest.
    loc_df["row"] = pd.cut(
        loc_df["plate_z"],
        bins=sorted(Z_BINS),           # pd.cut needs ascending bins
        labels=[4, 3, 2, 1, 0],        # reverse labels so row 0 = top
        right=True,
    ).astype(int)

    # Build the 5×5 grid as a list of row lists.
    grid = []
    for row in range(5):
        row_cells = []
        for col in range(5):
            # The outer ring of cells is always labeled OUTER; we still
            # compute the stat value for context but override the level.
            is_outer = (row == 0 or row == 4 or col == 0 or col == 4)

            cell_df = loc_df[(loc_df["row"] == row) & (loc_df["col"] == col)]
            value, level = _compute_cell_stat(cell_df, metric)

            if is_outer:
                level = "OUTER"

            row_cells.append({"value": value, "level": level})
        grid.append(row_cells)

    return {"metric": metric, "grid": grid}
