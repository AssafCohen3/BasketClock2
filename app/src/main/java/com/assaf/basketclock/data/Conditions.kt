package com.assaf.basketclock.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import com.assaf.basketclock.conditions.AbstractConditionData
import com.assaf.basketclock.conditions.ConditionType
import com.assaf.basketclock.conditions.decodeConditionData
import com.assaf.basketclock.systemSecondsToLocalDateTime
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.TimeZone


data class ConditionGame(
    val gameId: String,
    val gameDateTime: ZonedDateTime,
    val homeTeamId: Int,
    val homeTeamName: String,
    val homeTeamTricode: String,
    val awayTeamId: Int,
    val awayTeamName: String,
    val awayTeamTricode: String,
)


@Entity(tableName = "Conditions")
data class Condition(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val gameId: String,
    val gameDateTime: ZonedDateTime,
    val homeTeamId: Int,
    val homeTeamName: String,
    val homeTeamTricode: String,
    val awayTeamId: Int,
    val awayTeamName: String,
    val awayTeamTricode: String,
    val conditionType: ConditionType,
    val conditionData: Map<String, Any>
){
    val parsedConditionData: AbstractConditionData
        get() = decodeConditionData(this)
}

data class GameWithConditions(
    val gameId: String,
    val gameDateTime: ZonedDateTime,
    val homeTeamId: Int,
    val homeTeamName: String,
    val homeTeamTricode: String,
    val awayTeamId: Int,
    val awayTeamName: String,
    val awayTeamTricode: String,
    val conditions: MutableList<AbstractConditionData>
)

@Dao
interface ConditionDao {
    @Insert
    suspend fun insertCondition(condition: Condition)

    @Delete
    suspend fun deleteCondition(condition: Condition)

    @Query("SELECT * FROM Conditions WHERE gameId = :gameId")
    suspend fun getGameConditionsSync(gameId: String): List<Condition>

    @Query("SELECT * FROM Conditions WHERE gameId = :gameId")
    fun getGameConditions(gameId: String): Flow<List<Condition>>

    @Query("""
        SELECT * from Conditions
        where gameDateTime BETWEEN :startEpoch AND :startEpoch + :timeWindow
    """)
    suspend fun getConditionsWithinTimeRange(startEpoch: Long, timeWindow: Long): List<Condition>

    // Get game data for game ids list
    @Query("SELECT distinct gameId, gameDateTime, homeTeamId, homeTeamName, homeTeamTricode, " +
            "awayTeamId, awayTeamName, awayTeamTricode" +
            " FROM Conditions WHERE gameId IN (:gameIds)")
    suspend fun getGames(gameIds: List<String>): List<ConditionGame>
}

class ConditionsRepository(private val conditionDao: ConditionDao) {
    suspend fun insertCondition(condition: Condition) {
        conditionDao.insertCondition(condition)
    }

    suspend fun deleteCondition(condition: Condition) {
        conditionDao.deleteCondition(condition)
    }

    suspend fun getGameConditionsSync(gameId: String): List<Condition> {
        return conditionDao.getGameConditionsSync(gameId)
    }

    fun getGameConditions(gameId: String): Flow<List<Condition>> {
        return conditionDao.getGameConditions(gameId)
    }

    suspend fun getTodayConditions(): List<Condition> {
        // Get the time between the beginning of the day and its end, in relation to est clock.
        val startTime = Calendar.getInstance(TimeZone.getTimeZone("EST")).apply {
            // If its before 3 am eastern, treat this as yesterday
            if (get(Calendar.HOUR_OF_DAY) < 3){
                add(Calendar.DAY_OF_YEAR, -1)
            }

            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        Timber.d("Starting to search from ${systemSecondsToLocalDateTime(startTime)}")

        return conditionDao.getConditionsWithinTimeRange(startTime / 1000, 60*60*24)
    }

    suspend fun getTodayGamesConditions(): List<GameWithConditions> {
        val todayConditions = getTodayConditions()
        val todayGamesWithCondition = mutableMapOf<String, GameWithConditions>()
        for (condition in todayConditions){
            if (condition.gameId !in todayGamesWithCondition){
                todayGamesWithCondition[condition.gameId] = GameWithConditions(
                    condition.gameId,
                    condition.gameDateTime,
                    condition.homeTeamId,
                    condition.homeTeamName,
                    condition.homeTeamTricode,
                    condition.awayTeamId,
                    condition.awayTeamName,
                    condition.awayTeamTricode,
                    mutableListOf()
                )
            }
            todayGamesWithCondition[condition.gameId]?.conditions?.add(condition.parsedConditionData)
        }

        return todayGamesWithCondition.values.toList()
    }

    suspend fun getGames(gameIds: List<String>): List<ConditionGame> {
        return conditionDao.getGames(gameIds)
    }
}
