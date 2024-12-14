package com.assaf.basketclock.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Condition::class, SessionGame::class, Session::class], version = 2, exportSchema = false)
@TypeConverters(
    ZonedDateTypeConverter::class, ConditionTypeConverter::class, MapTypeConverter::class,
    SessionGameStatusTypeConverter::class, SessionStatusTypeConverter::class
)
abstract class AppDatabase: RoomDatabase() {
    abstract fun conditionDao(): ConditionDao
    abstract fun sessionGamesDao(): SessionGameDao
    abstract fun sessionDao(): SessionDao

    @Volatile
    private var ConditionsRepository: ConditionsRepository? = null

    @Volatile
    private var SessionGamesRepository: SessionGamesRepository? = null

    @Volatile
    private var SessionRepository: SessionRepository? = null

    fun getConditionsRepository(): ConditionsRepository {
        return ConditionsRepository ?: synchronized(this) {
            ConditionsRepository(conditionDao()).also { ConditionsRepository = it }
        }
    }

    fun getSessionRepository(): SessionRepository {
        return SessionRepository ?: synchronized(this) {
            SessionRepository(sessionDao()).also { SessionRepository = it }
        }
    }

    fun getSessionGamesRepository(): SessionGamesRepository {
        return SessionGamesRepository ?: synchronized(this) {
            SessionGamesRepository(sessionGamesDao(), getConditionsRepository()).also { SessionGamesRepository = it }
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
