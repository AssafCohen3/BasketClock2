package com.assaf.basketclock.conditions

import com.assaf.basketclock.scheduling.ScheduledGameWithConditions
import kotlin.math.max


const val GAME_START_MINUTES_DELAY = 10
const val GAME_HALFTIME_MINUTES_DURATION = 15
const val PERIOD_BREAK_MINUTES_DURATION = 2

// TODO maybe make this a function or something
const val POSSESSION_ESTIMATED_TIME = 15

suspend fun calculateDifferenceConditionNextRelevantTime(game: ScheduledGameWithConditions, condition: DifferenceConditionData): Long{
    val secondsUntilActive = calculateSecondsUntilGameActive(game)
    val pointsToTargetDiff: Int
    if (condition.sign == ">"){
        pointsToTargetDiff = max(0, condition.difference + 1 - game.gameData.currentDiff)
    }
    else{
        pointsToTargetDiff = max(0, game.gameData.currentDiff - condition.difference)
    }
    val possessionsToTargetDiff = pointsToTargetDiff / 3

    val secondsToTargetDiff = possessionsToTargetDiff * POSSESSION_ESTIMATED_TIME

    val targetMoment = game.gameData.gameMoment!!.addSeconds(secondsToTargetDiff.toDouble())

    val secondsToTargetMoment = secondsBetweenClocks(game.gameData.gameMoment!!, targetMoment)

    // Will zero out if the game is active and the target diff has been reached.
    return (secondsUntilActive + secondsToTargetMoment.toInt()) * 1000
}

suspend fun calculateTimeConditionNextRelevantTime(game: ScheduledGameWithConditions, condition: TimeConditionData): Long{
    if (game.gameData.gameMoment!!.toTotalSeconds() > condition.endMoment.toTotalSeconds()){
        return -1
    }
    // Account for cases in which the total seconds are the same but we at the end of period and
    //  the target is the start of the next period.
    if (game.gameData.gameMoment!!.toTotalSeconds() >= condition.startMoment.toTotalSeconds() && game.gameData.gameMoment!!.period >= condition.startMoment.period){
        return 0
    }

    // Either we at break time and expecting the game to renew (and change period) or we need to
    //  wait more time until the clocks match.
    return (calculateSecondsUntilGameActive(game) + secondsBetweenClocks(game.gameData.gameMoment!!, condition.startMoment).toInt()) * 1000
}

suspend fun calculateLeaderConditionNextRelevantTime(game: ScheduledGameWithConditions, condition: LeaderConditionData): Long{
    val pointsToTargetDiff: Int
    if (condition.leaderTeamId == game.gameData.homeTeam.teamId){
        pointsToTargetDiff = max(0, game.gameData.homeTeam.score - game.gameData.awayTeam.score + 1)
    }
    else{
        pointsToTargetDiff = max(0, game.gameData.awayTeam.score - game.gameData.homeTeam.score + 1)
    }

    val possessionsToTargetDiff = pointsToTargetDiff / 3

    val secondsToTargetDiff = possessionsToTargetDiff * POSSESSION_ESTIMATED_TIME

    val targetMoment = game.gameData.gameMoment!!.addSeconds(secondsToTargetDiff.toDouble())

    val secondsToTargetMoment = secondsBetweenClocks(game.gameData.gameMoment!!, targetMoment)

    // Will zero out if the game is active and the target diff has been reached.
    return (calculateSecondsUntilGameActive(game) + secondsToTargetMoment.toInt()) * 1000
}

suspend fun calculateGameNextRelevantTime(scheduledGameWithConditions: ScheduledGameWithConditions): Long{
    if (scheduledGameWithConditions.gameData.gameStatus > 2){
        // Game ended.
        return -1
    }

    var nextRelevantTime = -1L
    for (condition in scheduledGameWithConditions.gameWithConditions.conditions){
        val conditionNextRelevantTime = condition.calculateNextRelevantTime(scheduledGameWithConditions)
        if (conditionNextRelevantTime == -1L){
            // Condition will never happen.
            return -1
        }
        nextRelevantTime = max(nextRelevantTime, conditionNextRelevantTime)
    }

    return nextRelevantTime
}

suspend fun calculateSecondsUntilGameActive(game: ScheduledGameWithConditions): Long{
    val gameData = game.gameData
    if (gameData.gameStatus > 2){
        // Game ended.
        throw Exception("Should not happen.")
    }

    if (gameData.gameStatus == 1){
        // Time until 10 minutes after official game time.
        return (gameData.realGameDateTimeUTC.toEpochSecond() + 60 * GAME_START_MINUTES_DELAY) - System.currentTimeMillis() / 1000
    }

    if (gameData.period == 2 && gameData.gameClock!!.minutes == 0 && gameData.gameClock.seconds == 0.0){
        // Calculate time until halftime ends.
        val gamePBPData = game.getPBPData()
        val sortedActions = gamePBPData.getSortedActions()
        val lastAction = sortedActions.last()
        // Last play is indeed the second period end.
        if (lastAction.actionType == "period" && lastAction.subType == "end"){
            return (lastAction.timeActual.toEpochSecond() + 60 * GAME_HALFTIME_MINUTES_DURATION) - System.currentTimeMillis() / 1000
        }
    }

    return 0L
}

fun secondsBetweenClocks(gameMoment1: GameMoment, gameMoment2: GameMoment): Double{
    var gameMoment1 = gameMoment1
    var gameMoment2 = gameMoment2

    // We cant calculate accurately at the end of a period since we dont know how much time
    //  passed since its ended so assume the next period is starting.
    if (gameMoment1.clock.minutes == 0 && gameMoment1.clock.seconds == 0.0){
        gameMoment1 = GameMoment(gameMoment1.period + 1, Clock(getPeriodTotalMinutes(gameMoment1.period + 1), 0.0))
    }

    // Account for game breaks.
    var periodsBreakTime = max(0, (gameMoment2.period - gameMoment1.period)) * PERIOD_BREAK_MINUTES_DURATION * 60
    if(gameMoment1.period <= 2 && gameMoment2.period >= 3){
        // Compensate for halftime break that has been counted as period break.
        periodsBreakTime += (GAME_HALFTIME_MINUTES_DURATION - PERIOD_BREAK_MINUTES_DURATION) * 60
    }

    return gameMoment2.toTotalSeconds() - gameMoment1.toTotalSeconds() + periodsBreakTime
}

fun getPeriodTotalMinutes(period: Int): Int{
    return if (period < 5) 12 else 5
}

data class Clock(
    val minutes: Int,
    val seconds: Double
){
    fun toTotalSeconds(): Double{
        return minutes*60 + seconds
    }

    companion object{
        fun fromSeconds(seconds: Double): Clock{
            return Clock(seconds.toInt() / 60, seconds % 60.0)
        }
    }
}

data class GameMoment(
    val period: Int,
    val clock: Clock
){

    fun addSeconds(seconds: Double): GameMoment{
        var totalSecondsDiff = clock.toTotalSeconds() - seconds
        var newPeriod = period
        while (totalSecondsDiff < 0){
            newPeriod += 1
            totalSecondsDiff += getPeriodTotalMinutes(newPeriod) * 60
        }

        return GameMoment(newPeriod, Clock.fromSeconds(totalSecondsDiff))
    }

    fun getReverseClock(): Clock{
        // Get the clock since the period began (and not the clock until the period ends).
        val periodMinutes = getPeriodTotalMinutes(period)
        var minutes = periodMinutes - clock.minutes - 1
        var seconds = 60 - clock.seconds

        if (clock.seconds == 0.0){
            seconds = 0.0
            minutes = minutes + 1
        }
        return Clock(minutes, seconds)
    }

    fun toTotalSeconds(): Double{
        var totalSeconds = getReverseClock().toTotalSeconds()
        for (i in 1 until period){
            totalSeconds += getPeriodTotalMinutes(i) * 60
        }
        return totalSeconds
    }
}
