package com.assaf.basketclock

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.assaf.basketclock.conditions.AbstractConditionData
import com.assaf.basketclock.conditions.ConditionType
import com.assaf.basketclock.conditions.decodeConditionData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.TimeZone


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

@Dao
interface ConditionDao {
    @Insert
    suspend fun insertCondition(condition: Condition)

    @Delete
    suspend fun deleteCondition(condition: Condition)

    @Query("SELECT * FROM Conditions WHERE gameId = :gameId")
    fun getGameConditions(gameId: String): Flow<List<Condition>>

    @Query("""
        SELECT * from Conditions
        where gameDateTime BETWEEN :startEpoch AND :startEpoch + :timeWindow
    """)
    suspend fun getConditionsWithinTimeRange(startEpoch: Long, timeWindow: Long): List<Condition>
}

class ZonedDateTypeConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): ZonedDateTime? {
        return value?.let { Instant.ofEpochSecond(value).atZone(ZoneId.of("UTC")) }
    }

    @TypeConverter
    fun dateToTimestamp(date: ZonedDateTime?): Long? {
        return date?.toEpochSecond()
    }
}

class ConditionTypeConverter {

    @TypeConverter
    fun fromConditionType(value: ConditionType?): String? {
        return value?.name // Convert enum to String
    }

    @TypeConverter
    fun toConditionType(value: String?): ConditionType? {
        return value?.let { ConditionType.valueOf(it) } // Convert String back to enum
    }
}

class MapTypeConverter {

    @TypeConverter
    fun fromMap(value: Map<String, Any>?): String? {
        return value?.let { Gson().toJson(value) }
    }

    @TypeConverter
    fun toMap(value: String?): Map<String, Any>? {
        return value?.let {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            Gson().fromJson<Map<String, Any>>(value, type)
        }
    }
}

class ConditionsRepository(private val conditionDao: ConditionDao) {
    suspend fun insertCondition(condition: Condition) {
        conditionDao.insertCondition(condition)
    }

    suspend fun deleteCondition(condition: Condition) {
        conditionDao.deleteCondition(condition)
    }

    fun getGameConditions(gameId: String): Flow<List<Condition>> {
        return conditionDao.getGameConditions(gameId)
    }

    suspend fun getTodayConditions(): List<Condition> {
        // Get the time between the beginning of the day and its end, in relation to est clock.
        val startTime = Calendar.getInstance(TimeZone.getTimeZone("EST")).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        return conditionDao.getConditionsWithinTimeRange(startTime / 1000, 60*60*24)
    }
}

@Database(entities = [Condition::class], version = 1, exportSchema = false)
@TypeConverters(ZonedDateTypeConverter::class, ConditionTypeConverter::class, MapTypeConverter::class)
abstract class AppDatabase: RoomDatabase() {
    abstract fun conditionDao(): ConditionDao

    @Volatile
    private var ConditionsRepository: ConditionsRepository? = null

    fun getConditionsRepository(): ConditionsRepository {
        return ConditionsRepository ?: synchronized(this) {
            ConditionsRepository(conditionDao()).also { ConditionsRepository = it }
        }
    }


    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // if the Instance is not null, return it, otherwise create a new database instance.
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "app_database")
                    .build()
                    .also { Instance = it }
            }
        }
    }
}