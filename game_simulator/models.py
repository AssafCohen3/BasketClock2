from datetime import datetime, date
from typing import Optional

from pydantic import BaseModel, Field, field_validator


class TeamGameData(BaseModel):
    team_id: int = Field(..., alias="teamId")
    team_name: Optional[str] = Field(..., alias="teamName")
    team_city: Optional[str] = Field(..., alias="teamCity")
    team_tricode: Optional[str] = Field(..., alias="teamTricode")
    wins: int = -1
    losses: int = -1
    score: int
    seed: Optional[int] = None
    in_bonus: Optional[str] = Field(None, alias="inBonus")
    timeouts_remaining: Optional[int] = Field(None, alias="timeoutsRemaining")


class GameData(BaseModel):
    game_id: str = Field(..., alias="gameId")
    game_code: str = Field(..., alias="gameCode")
    game_status: int = Field(..., alias="gameStatus")
    game_status_text: str = Field(..., alias="gameStatusText")
    period: Optional[int] = None
    game_clock: Optional[str] = Field(None, alias="gameClock")
    game_time_utc: datetime = Field(..., alias="gameTimeUTC")
    regulation_periods: Optional[int] = Field(None, alias="regulationPeriods")
    if_necessary: bool = Field(False, alias="ifNecessary")
    series_game_number: Optional[str] = Field(None, alias="seriesGameNumber")
    game_label: Optional[str] = Field(None, alias="gameLabel")
    game_sub_label: Optional[str] = Field(None, alias="gameSubLabel")
    series_text: Optional[str] = Field(None, alias="seriesText")
    series_conference: Optional[str] = Field(None, alias="seriesConference")
    po_round_desc: Optional[str] = Field(None, alias="poRoundDesc")
    game_subtype: Optional[str] = Field(None, alias="gameSubtype")
    home_team: TeamGameData = Field(..., alias="homeTeam")
    away_team: TeamGameData = Field(..., alias="awayTeam")

    class Config:
        json_encoders = {
            datetime: lambda dt: dt.strftime("%Y-%m-%dT%H:%M:%SZ")
        }


class CalendarGameData(BaseModel):
    game_id: str = Field(..., alias="gameId")


class Scoreboard(BaseModel):
    game_date: str = Field(..., alias="gameDate")
    league_id: str = Field(..., alias="leagueId")
    league_name: str = Field(..., alias="leagueName")
    games: list[GameData]

    class Config:
        allow_population_by_field_name = True
        arbitrary_types_allowed = True


class ScoreboardResponse(BaseModel):
    scoreboard: Scoreboard


class GameDate(BaseModel):
    game_date: str = Field(..., alias="gameDate")
    games: list[CalendarGameData]


class LeagueSchedule(BaseModel):
    season_year: str = Field(..., alias="seasonYear")
    league_id: str = Field(..., alias="leagueId")
    game_dates: list[GameDate] = Field(..., alias="gameDates")


class CalendarResponse(BaseModel):
    league_schedule: LeagueSchedule = Field(..., alias="leagueSchedule")


class PBPAction(BaseModel):
    action_number: int = Field(..., alias="actionNumber")
    clock: str
    time_actual: datetime = Field(..., alias="timeActual")
    period: int
    period_type: str = Field(..., alias="periodType")
    action_type: str = Field(..., alias="actionType")
    sub_type: Optional[str] = Field(None, alias="subType")
    score_home: int = Field(..., alias="scoreHome")
    score_away: int = Field(..., alias="scoreAway")
    order_number: int = Field(..., alias="orderNumber")
    team_id: Optional[int] = Field(None, alias="teamId")

    @field_validator('score_home', mode='before')
    def convert_str_to_int(cls, value):
        # Convert string to int
        if isinstance(value, str):
            return int(value)
        return value

    @field_validator('score_away', mode='before')
    def convert_str_to_int(cls, value):
        # Convert string to int
        if isinstance(value, str):
            return int(value)
        return value


class PBPGame(BaseModel):
    game_id: str = Field(..., alias="gameId")
    actions: list[PBPAction]


class PBPResponse(BaseModel):
    game: PBPGame
