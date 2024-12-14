package com.assaf.basketclock.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Query
import androidx.room.Upsert
import com.assaf.basketclock.conditions.AbstractConditionData
import timber.log.Timber
import java.time.ZonedDateTime

enum class SessionGameStatus{
    SCHEDULING,
    SCHEDULED,
    ALARMED,
    TO_BE_SCHEDULED,
    FINISHED
}

@Entity(tableName = "SessionGame", primaryKeys = ["sessionId", "gameId"])
data class SessionGame(
    val sessionId: Int,
    val gameId: String,
    val sessionGameStatus: SessionGameStatus,
    val scheduledTime: Long?
){
    fun isFinished(): Boolean{
        return sessionGameStatus != SessionGameStatus.FINISHED && sessionGameStatus != SessionGameStatus.ALARMED
    }
}

data class SessionGameWithConditions(
    val sessionId: Int,
    val gameId: String,
    var sessionGameStatus: SessionGameStatus,
    var scheduledTime: Long?,
    val gameDateTime: ZonedDateTime,
    val homeTeamId: Int,
    val homeTeamName: String,
    val homeTeamTricode: String,
    val awayTeamId: Int,
    val awayTeamName: String,
    val awayTeamTricode: String,
    val conditions: List<AbstractConditionData>
){
    val sessionGame: SessionGame
        get() = SessionGame(sessionId, gameId, sessionGameStatus, scheduledTime)
}

@Dao
interface SessionGameDao {
    @Upsert
    suspend fun insertOrUpdateSessionGame(sessionGame: SessionGame)

    @Upsert
    suspend fun insertOrUpdateSessionGames(sessionGames: List<SessionGame>)

    @Query("""
        SELECT * FROM SessionGame
        WHERE sessionId = :sessionId
    """)
    suspend fun getSessionGames(sessionId: Int): List<SessionGame>
}

class SessionGamesRepository(private val sessionGameDao: SessionGameDao, private val conditionsRepository: ConditionsRepository) {
    suspend fun insertOrUpdateSessionGame(sessionGame: SessionGame) {
        sessionGameDao.insertOrUpdateSessionGame(sessionGame)
    }

    suspend fun insertOrUpdateSessionGames(sessionGames: List<SessionGame>) {
        sessionGameDao.insertOrUpdateSessionGames(sessionGames)
    }

    suspend fun getTodaySessionGamesWithConditions(sessionId: Int): List<SessionGameWithConditions>{
        val todayGamesWithConditions = conditionsRepository.getTodayGamesConditions()
        Timber.d("Today games: $todayGamesWithConditions")
        val sessionGames = sessionGameDao.getSessionGames(sessionId).associateBy { it.gameId }
        val toRet = mutableListOf<SessionGameWithConditions>()
        for (gameWithConditions in todayGamesWithConditions){
            if (
                gameWithConditions.gameId in sessionGames &&
                sessionGames[gameWithConditions.gameId]!!.isFinished()
            ){
                // No need in finished games.
                continue
            }

            toRet.add(SessionGameWithConditions(
                sessionId,
                gameWithConditions.gameId,
                sessionGames[gameWithConditions.gameId]?.sessionGameStatus ?: SessionGameStatus.TO_BE_SCHEDULED,
                sessionGames[gameWithConditions.gameId]?.scheduledTime,
                gameWithConditions.gameDateTime,
                gameWithConditions.homeTeamId,
                gameWithConditions.homeTeamName,
                gameWithConditions.homeTeamTricode,
                gameWithConditions.awayTeamId,
                gameWithConditions.awayTeamName,
                gameWithConditions.awayTeamTricode,
                gameWithConditions.conditions.toList()
            ))
        }

        return toRet
    }
}
