package com.assaf.basketclock

import kotlinx.serialization.Serializable
import java.time.ZonedDateTime
import java.util.Date


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
    val inBonus: String? = null,
    val timeoutsRemaining: Int? = null,
)

@Serializable
data class GameData(
    val gameId: String,
    val gameCode: String,
    val gameStatus: Int,
    val gameStatusText: String,
    val period: Int? = null,
    val gameClock: String? = null,
    @Serializable(with = KZonedDateTimeSerializer::class)
    val gameTimeUTC: ZonedDateTime,
    @Serializable(with = KZonedDateTimeSerializer::class)
    val gameDateTimeUTC: ZonedDateTime? = null,
    val regulationPeriods: Int? = null,
    val ifNecessary: Boolean,
    val seriesGameNumber: String?,
    val gameLabel: String?,
    val gameSubLabel: String?,
    val seriesText: String?,
    val seriesConference: String? = null,
    val poRoundDesc: String? = null,
    val gameSubtype: String?,
    val homeTeam: TeamGameData,
    val awayTeam: TeamGameData,
){
    val realGameDateTimeUTC: ZonedDateTime
        get() = gameDateTimeUTC ?: gameTimeUTC
}

@Serializable
data class Scoreboard(
    @Serializable(with = SimpleDateSerializer::class)
    val gameDate: Date,
    val leagueId: String,
    val leagueName: String,
    val games: List<GameData>
)

@Serializable
data class ScoreboardResponse(
    val scoreboard: Scoreboard
)

@Serializable
data class GameDate(
    @Serializable(with = DateWithTimeSerializer::class)
    val gameDate: Date,
    var games: List<GameData>
)

@Serializable
data class LeagueSchedule(
    val seasonYear: String,
    val leagueId: String,
    val gameDates: List<GameDate>
)

@Serializable
data class CalendarResponse(
    val leagueSchedule: LeagueSchedule,
)


data class CalendarResponseWithTodayDate(
    val leagueSchedule: LeagueSchedule,
    val todayDate: Date
)
