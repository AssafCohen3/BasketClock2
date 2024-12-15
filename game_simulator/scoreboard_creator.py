import datetime
import json
from pathlib import Path

import requests

from models import LeagueSchedule, CalendarResponse, GameData, ScoreboardResponse, Scoreboard, \
    CalendarGameData


def get_schedule() -> LeagueSchedule:
    calendar_resp = CalendarResponse(
        **json.loads(Path('assets/schedule.json').read_text())
    )
    return calendar_resp.league_schedule


def get_date_games(games_date: str) -> list[CalendarGameData]:
    schedule = get_schedule()
    for d in schedule.game_dates:
        if d.game_date == games_date:
            return d.games

    return []


def get_boxscore(game_id: str) -> GameData:
    url = f'https://cdn.nba.com/static/json/liveData/boxscore/boxscore_{game_id}.json'
    resp = requests.get(url).json()
    return GameData(**resp["game"])


def create_scoreboard(scoreboard_date: datetime.date):
    formatted_date = scoreboard_date.strftime("%m/%d/%Y %H:%M:%S")
    print(formatted_date)
    date_games = get_date_games(formatted_date)
    games_data = [
        get_boxscore(game.game_id)
        for game in date_games
    ]
    scoreboard = ScoreboardResponse(
        scoreboard=Scoreboard(
            gameDate=scoreboard_date.strftime('%Y-%m-%d'),
            leagueId='00',
            leagueName='National Basketball Association',
            games=games_data
        )
    )
    Path('res_scoreboard.json').write_text(
        json.dumps(scoreboard.model_dump(mode='json', by_alias=True), indent=4)
    )


if __name__ == '__main__':
    create_scoreboard(datetime.date(2024, 12, 8))
