import datetime
import json
import logging
import time
from datetime import timedelta
from pathlib import Path
from typing import Optional

import requests
from fastapi import FastAPI

from models import GameData, ScoreboardResponse, PBPResponse, PBPGame, Scoreboard

# Initialize FastAPI app
app = FastAPI()

session_beginning_seconds: Optional[float] = None

# Simulate the first request to be 2 minutes before the game.
SESSION_GAME_DELAY_MINUTES = 2


def get_scoreboard() -> Scoreboard:
    return ScoreboardResponse(
        **json.loads(Path('assets/scoreboard.json').read_text())
    ).scoreboard


def transform_game_pbp(
        game_pbp: PBPGame,
        simulated_datetime: datetime.datetime,
        game_date_time_shift: float,
) -> PBPGame:
    to_ret = game_pbp.model_copy(deep=True)

    to_ret.actions = [
        action for action in game_pbp.actions
        if action.time_actual <= simulated_datetime
    ]
    for action in to_ret.actions:
        action.time_actual = action.time_actual - timedelta(seconds=game_date_time_shift)

    return to_ret


def get_game_pbp(game_id: str) -> PBPGame:
    pbp_file = Path(f'assets/{game_id}_pbp.json')
    if not pbp_file.exists():
        url = f'https://cdn.nba.com/static/json/liveData/playbyplay/playbyplay_{game_id}.json'
        resp = requests.get(url)
        pbp_file.write_text(json.dumps(resp.json(), indent=4))

    game_pbp = PBPResponse(**json.loads(pbp_file.read_text())).game
    game_pbp.actions = sorted(game_pbp.actions, key=lambda a: a.order_number)
    return game_pbp


def build_game_data_from_datetime(
        game_data: GameData,
        game_pbp: PBPGame,
        current_datetime: datetime.datetime,
        game_date_time_shift: float,
) -> GameData:
    to_ret = game_data.model_copy(deep=True)
    to_ret.game_time_utc = to_ret.game_time_utc - datetime.timedelta(seconds=game_date_time_shift)

    if current_datetime >= game_pbp.actions[-1].time_actual:
        # Just return the final game data.
        return to_ret

    if current_datetime < game_pbp.actions[0].time_actual:
        to_ret.game_status = 1
        to_ret.period = 1
        to_ret.game_clock = ""
        to_ret.home_team.score = 0
        to_ret.away_team.score = 0
    else:
        closest_action = [
            action for action in game_pbp.actions
            if action.time_actual <= current_datetime
        ][-1]
        to_ret.game_status = 2
        to_ret.period = closest_action.period
        to_ret.game_clock = closest_action.clock
        to_ret.home_team.score = closest_action.score_home
        to_ret.away_team.score = closest_action.score_away

    return to_ret


def get_session_beginning_time(scoreboard: Scoreboard) -> datetime.datetime:
    min_first_action_time = None
    for game in scoreboard.games:
        game_pbp = get_game_pbp(game.game_id)
        if min_first_action_time is None or game_pbp.actions[0].time_actual < min_first_action_time:
            min_first_action_time = game_pbp.actions[0].time_actual

    return min_first_action_time - timedelta(minutes=SESSION_GAME_DELAY_MINUTES)


@app.get("/static/json/liveData/scoreboard/todaysScoreboard_00.json")
def simulate_scoreboard() -> ScoreboardResponse:
    global session_beginning_seconds
    if session_beginning_seconds is None:
        session_beginning_seconds = time.time()

    template_scoreboard = get_scoreboard().model_copy(deep=True)
    session_beginning_datetime = get_session_beginning_time(template_scoreboard)
    current_simulated_time = session_beginning_datetime + timedelta(seconds=time.time() - session_beginning_seconds)

    for i in range(len(template_scoreboard.games)):
        game = template_scoreboard.games[i]
        game_pbp = get_game_pbp(game.game_id)
        template_scoreboard.games[i] = build_game_data_from_datetime(
            game,
            game_pbp,
            current_simulated_time,
            game_date_time_shift=session_beginning_datetime.timestamp() - session_beginning_seconds
        )

    return ScoreboardResponse(scoreboard=template_scoreboard)


@app.get("/static/json/liveData/playbyplay/playbyplay_{game_id}.json")
def simulate_pbp(game_id: str) -> PBPResponse:
    global session_beginning_seconds
    if session_beginning_seconds is None:
        raise Exception("Could not make playbyplay requests before simulation initialization.")

    template_scoreboard = get_scoreboard().model_copy(deep=True)
    session_beginning_datetime = get_session_beginning_time(template_scoreboard)
    current_simulated_time = session_beginning_datetime + timedelta(seconds=time.time() - session_beginning_seconds)

    game_pbp = get_game_pbp(game_id)
    game_pbp = transform_game_pbp(
        game_pbp,
        current_simulated_time,
        game_date_time_shift=session_beginning_datetime.timestamp() - session_beginning_seconds
    )

    return PBPResponse(game=game_pbp)


@app.get("/game_fake_data")
def simulate_pbp() -> GameData:
    template_scoreboard = get_scoreboard().model_copy(deep=True)
    session_beginning_datetime = get_session_beginning_time(template_scoreboard)
    current_simulated_time = session_beginning_datetime

    game_to_ret = template_scoreboard.games[0]
    game_to_ret = build_game_data_from_datetime(
        game_to_ret,
        get_game_pbp(game_to_ret.game_id),
        current_simulated_time,
        game_date_time_shift=session_beginning_datetime.timestamp() - time.time()
    )

    return game_to_ret
