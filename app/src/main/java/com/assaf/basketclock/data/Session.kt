package com.assaf.basketclock.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update

enum class SessionStatus{
    RUNNING,
    FINISHED,
    KILLED
}

@Entity(tableName = "Session")
data class Session(
    @PrimaryKey(autoGenerate = true)
    var sessionId: Int = 0,
    val creationTime: Long,
    var failsCount: Int = 0,
    var status: SessionStatus
)

@Dao
interface SessionDao {
    @Insert
    suspend fun newSession(session: Session): Long

    @Update
    suspend fun updateSession(session: Session)

    @Query("SELECT * FROM Session WHERE sessionId = :sessionId")
    suspend fun getSession(sessionId: Int): Session

    @Query("SELECT * FROM Session ORDER BY creationTime DESC LIMIT 1")
    suspend fun getLastSession(): Session
}

class SessionRepository(private val sessionDao: SessionDao) {
    suspend fun newSession(): Session{
        val session = Session(creationTime = System.currentTimeMillis(), status = SessionStatus.RUNNING)
        session.sessionId = sessionDao.newSession(session).toInt()
        return session
    }

    suspend fun updateSessionFailCount(sessionId: Int, failCount: Int){
        val session = sessionDao.getSession(sessionId)
        session.failsCount = failCount
        sessionDao.updateSession(session)
    }

    suspend fun updateSessionStatus(sessionId: Int, status: SessionStatus){
        val session = sessionDao.getSession(sessionId)
        session.status = status
        sessionDao.updateSession(session)
    }

    suspend fun getSession(sessionId: Int): Session{
        return sessionDao.getSession(sessionId)
    }

    suspend fun getLastSession(): Session{
        return sessionDao.getLastSession()
    }
}
