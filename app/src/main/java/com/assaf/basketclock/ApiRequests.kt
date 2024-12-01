package com.assaf.basketclock

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

suspend fun fetchPBPData(gameId: String): PBPResponse = withContext(Dispatchers.IO){
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("https://cdn.nba.com/static/json/liveData/playbyplay/playbyplay_$gameId.json")
        .build()

    val response = client.newCall(request).execute()

    if (response.isSuccessful){
        val responseBody = response.body?.string()
        Json { ignoreUnknownKeys = true}.decodeFromString<PBPResponse>(responseBody as String)
    }
    else {
        throw IOException("Failed fetching pbp data.")
    }
}

suspend fun fetchScoreBoardData(): ScoreboardResponse = withContext(Dispatchers.IO){
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("https://cdn.nba.com/static/json/liveData/scoreboard/todaysScoreboard_00.json")
        .build()

    val response = client.newCall(request).execute()

    if (response.isSuccessful){
        val responseBody = response.body?.string()
        Json { ignoreUnknownKeys = true}.decodeFromString<ScoreboardResponse>(responseBody as String)
    }
    else {
        throw IOException("Failed fetching scoreboard data.")
    }
}

fun filterCalendarData(calendarResponse: CalendarResponse): CalendarResponse{

    // Filter games without game code.
    val filteredGameDates = calendarResponse.leagueSchedule.gameDates.map { gameDate ->
        gameDate.copy(games = gameDate.games.filter { !it.gameCode.isEmpty() })
    }
    return calendarResponse.copy(
        leagueSchedule = calendarResponse.leagueSchedule.copy(gameDates = filteredGameDates)
    )
}

suspend fun fetchCalendarData(): CalendarResponse = withContext(Dispatchers.IO){
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("https://cdn.nba.com/static/json/staticData/scheduleLeagueV2_18.json")
        .build()

    val response = client.newCall(request).execute()

    if (response.isSuccessful){
        val responseBody = response.body?.string()
        val parsed = Json { ignoreUnknownKeys = true}.decodeFromString<CalendarResponse>(responseBody as String)
        filterCalendarData(parsed)
    }
    else {
        throw IOException("Failed fetching calendar data.")
    }
}

fun combineCalendarAndScoreboardData(calendarResponse: CalendarResponse, scoreboardResponse: ScoreboardResponse): CalendarResponseWithTodayDate{
    for (gameDate in calendarResponse.leagueSchedule.gameDates){
        if (gameDate.gameDate.equals(scoreboardResponse.scoreboard.gameDate)){
            gameDate.games = scoreboardResponse.scoreboard.games
        }
    }

    return CalendarResponseWithTodayDate(
        leagueSchedule = calendarResponse.leagueSchedule,
        todayDate = scoreboardResponse.scoreboard.gameDate
    )
}


suspend fun fetchCompleteGamesData(): CalendarResponseWithTodayDate{
    val scoreboardData = fetchScoreBoardData()
    val calendarData = fetchCalendarData()
    return combineCalendarAndScoreboardData(calendarData, scoreboardData)
}
