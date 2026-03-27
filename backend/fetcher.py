"""
fetcher.py — Statcast data retrieval via pybaseball.

All routes share this module so that repeated requests for the same
batter/season pair hit the in-memory cache instead of re-scraping
Baseball Savant on every call.
"""

from datetime import date
import pybaseball
import pandas as pd


# Suppress pybaseball's progress bar output in server logs.
pybaseball.cache.enable()


# ---------------------------------------------------------------------------
# In-memory cache: { (player_id_str, season_int): DataFrame }
# Populated on first request; lives for the process lifetime.
# ---------------------------------------------------------------------------
_cache: dict[tuple, pd.DataFrame] = {}


class DataNotFoundError(Exception):
    """Raised when no Statcast data exists for the requested player/season."""
    pass


def _season_date_range(season: int) -> tuple[str, str]:
    """
    Return (start_date, end_date) strings for a given MLB season year.
    Regular season typically runs late March through late October.
    If the season is the current calendar year and today is before the
    estimated start, we still attempt the fetch so the cache stores an
    empty frame (callers will surface a 404).
    """
    starts = {
        2022: "2022-04-07",
        2023: "2023-03-30",
        2024: "2024-03-20",
        2025: "2025-03-18",
        2026: "2026-03-26",
    }
    # Default: guess a reasonable opening day for unknown seasons.
    start = starts.get(season, f"{season}-04-01")
    # Use today as the end date for the current season so we capture
    # games played up to now; use Oct 31 for past seasons.
    today = date.today()
    if today.year == season:
        end = today.strftime("%Y-%m-%d")
    else:
        end = f"{season}-10-31"
    return start, end


def get_statcast_batter_data(player_id: str, season: int) -> pd.DataFrame:
    """
    Fetch pitch-by-pitch Statcast data for a batter for an entire season.

    Returns a DataFrame where each row is one pitch thrown to this batter.
    Key columns used downstream:
        events        — plate-appearance outcome (non-null only on final pitch)
        description   — pitch result (swinging_strike, called_strike, ball, …)
        type          — B / S / X  (ball / strike / in-play)
        zone          — Statcast zone 1-9 (strike) or 11-14 (chase)
        pitch_type    — Statcast pitch code (FF, SL, CH, …)
        plate_x/z     — pitch location in feet at the front of home plate
        launch_speed  — exit velocity (mph), non-null only on batted balls
        launch_angle  — launch angle (degrees), non-null only on batted balls
        estimated_ba_using_speedangle   — xBA per batted ball
        estimated_woba_using_speedangle — xwOBA per batted ball
        barrel        — 1 if barrel, 0 otherwise (may be absent in older data)
        pitcher       — MLBAM pitcher ID (int) who threw the pitch

    Raises DataNotFoundError if the player has no data for that season
    (unknown ID, season hasn't started, etc.).
    """
    cache_key = (player_id, season)
    if cache_key in _cache:
        return _cache[cache_key]

    start, end = _season_date_range(season)

    try:
        df = pybaseball.statcast_batter(start, end, player_id=int(player_id))
    except Exception as exc:
        # pybaseball can raise on network errors or invalid IDs.
        raise DataNotFoundError(
            f"Failed to fetch data for batter {player_id}: {exc}"
        ) from exc

    if df is None or df.empty:
        raise DataNotFoundError(
            f"No Statcast data found for batter {player_id}, season {season}."
        )

    # Store in cache before returning.
    _cache[cache_key] = df
    return df


def filter_by_pitcher(df: pd.DataFrame, pitcher_id: str) -> pd.DataFrame:
    """
    Narrow a batter's Statcast DataFrame to pitches thrown by a specific pitcher.
    The 'pitcher' column contains MLBAM IDs as integers.
    Returns an empty DataFrame (not an error) if the matchup has no data.
    """
    return df[df["pitcher"] == int(pitcher_id)].copy()
