import datetime
import json
import time
from datetime import timedelta
from pathlib import Path

import requests
from fastapi import FastAPI

from models import GameData, ScoreboardResponse, PBPResponse, PBPGame, Scoreboard

# Initialize FastAPI app
app = FastAPI()

session_beginning_seconds = None
pbp_cache: dict[str, PBPGame] = {}

# Simulate the first request to be 2 minutes before the game.
SESSION_GAME_DELAY_MINUTES = 2


def get_scoreboard() -> Scoreboard:
    return ScoreboardResponse(
        **json.loads(Path('assets/scoreboard.json').read_text())
    ).scoreboard


def get_game_pbp(game_id: str) -> PBPGame:
    global pbp_cache
    if game_id not in pbp_cache:
        url = f'https://cdn.nba.com/static/json/liveData/playbyplay/playbyplay_{game_id}.json'
        resp = requests.get(url).json()
        pbp_cache[game_id] = PBPResponse(**resp).game

    game_pbp = pbp_cache[game_id]
    game_pbp.actions = sorted(game_pbp.actions, key=lambda a: a.order_number)
    return game_pbp


def build_game_data_from_datetime(game_data: GameData, game_pbp: PBPGame, current_datetime: datetime.datetime) -> GameData:
    to_ret = game_data.model_copy(deep=True)
    if current_datetime >= game_pbp.actions[-1].time_actual:
        # Just return the final game data.
        return game_data

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


@app.get("/simulate")
def simulate():
    global session_beginning_seconds
    if session_beginning_seconds is None:
        session_beginning_seconds = time.time()

    template_scoreboard = get_scoreboard().model_copy(deep=True)
    session_beginning_datetime = get_session_beginning_time(template_scoreboard)
    current_simulated_time = session_beginning_datetime + timedelta(seconds=time.time() - session_beginning_seconds)

    for i in range(len(template_scoreboard.games)):
        game = template_scoreboard.games[i]
        game_pbp = get_game_pbp(game.game_id)
        template_scoreboard.games[i] = build_game_data_from_datetime(game, game_pbp, current_simulated_time)

    return template_scoreboard
