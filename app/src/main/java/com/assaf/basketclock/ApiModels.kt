package com.assaf.basketclock

import com.assaf.basketclock.conditions.Clock
import com.assaf.basketclock.conditions.GameMoment
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime
import java.util.Date
import kotlin.Int
import kotlin.math.abs


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
    @Serializable(with = ClockSerializer::class)
    val gameClock: Clock? = null,
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

    val gameMoment: GameMoment?
        get() = if (gameClock != null && period != null) GameMoment(period, gameClock) else null

    val currentDiff: Int
        get() = abs(homeTeam.score - awayTeam.score)
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

@Serializable
data class PBPResponse(
    val game: PBPGame
)

@Serializable
data class PBPGame(
    val gameId: String,
    val actions: List<PBPAction>
){
    fun getSortedActions(): List<PBPAction>{
        return actions.sortedBy { it.orderNumber }
    }
}

@Serializable
data class PBPAction(
    val actionNumber: Int,
    @Serializable(with = ClockSerializer::class)
    val clock: Clock,
    @Serializable(with = KZonedDateTimeSerializer::class)
    val timeActual: ZonedDateTime,
    val period: Int,
    val periodType: String,
    // Relevant values: "period"
    val actionType: String,
    // Relevant values: "end", "start"
    val subType: String? = null,
    val scoreHome: String,
    val scoreAway: String,
    val orderNumber: Int,
    val teamId: Int? = null
)
