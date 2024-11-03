package com.assaf.basketclock

import kotlinx.serialization.Serializable


@Serializable
data class ScoreboardResponse(
    val scoreboard: Scoreboard
)

@Serializable
data class Scoreboard(
    val gameDate: String,
    val leagueId: String,
    val leagueName: String,
    val games: List<GameData>
)

@Serializable
data class GameData(
    val gameId: String,
    val gameCode: String,
    val gameStatus: Int,
    val gameStatusText: String,
    val period: Int,
    val gameClock: String,
    val gameTimeUTC: String,
    val gameDateTimeUTC: String? = null,
    val regulationPeriods: Int,
    val ifNecessary: Boolean,
    val seriesGameNumber: String?,
    val gameLabel: String?,
    val gameSubLabel: String?,
    val seriesText: String?,
    val seriesConference: String?,
    val poRoundDesc: String?,
    val gameSubtype: String?,
    val homeTeam: TeamGameData,
    val awayTeam: TeamGameData,
)

@Serializable
data class TeamGameData(
    val teamId: Int,
    val teamName: String,
    val teamCity: String,
    val teamTricode: String,
    val wins: Int,
    val losses: Int,
    val score: Int,
    val seed: Int?,
    val inBonus: String?,
    val timeoutsRemaining: Int,
)
