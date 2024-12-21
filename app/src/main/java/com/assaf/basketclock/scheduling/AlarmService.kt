package com.assaf.basketclock.scheduling

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.assaf.basketclock.data.AppDatabase
import com.assaf.basketclock.GameData
import com.assaf.basketclock.PBPGame
import com.assaf.basketclock.R
import com.assaf.basketclock.data.Session
import com.assaf.basketclock.data.SessionGameStatus
import com.assaf.basketclock.data.SessionGameWithConditions
import com.assaf.basketclock.data.SessionStatus
import com.assaf.basketclock.fetchPBPData
import com.assaf.basketclock.fetchScoreBoardData
import com.assaf.basketclock.systemSecondsToLocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import kotlin.math.max
import kotlin.uuid.ExperimentalUuidApi

/*
The alarm service flow:

> Collect all games with conditions of today which has not been finished on the current session
> Attach the real time game data from the current scoreboard to each game.
> For each game calculate the next relevant time for check.
> Alarm games that should be alarmed.
> Schedule the next check to the closest next relevant time.
*/

data class ScheduledGameWithConditions(
    val gameWithConditions: SessionGameWithConditions,
    val gameData: GameData,
    var nextRelevantTime: Long?,
    var pbpData: PBPGame? = null
){
    suspend fun getPBPData(): PBPGame{
        if (pbpData == null){
            pbpData = fetchPBPData(gameData.gameId).game
        }
        return pbpData!!
    }
}

class AlarmService: Service() {
    private val channelId = "AlarmServiceChannel"
    private val notificationId = 1
    private lateinit var currentSession: Session
    companion object{
        private const val INTENT_SESSION_ID_KEY: String = "sessionId"

        private var isRunning = false

        fun isRunning(): Boolean{
            return isRunning
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
    }

    @OptIn(ExperimentalUuidApi::class)
    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        Timber.d("Starting service...")

        ServiceCompat.startForeground(
            this,
            notificationId,
            createNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )

        CoroutineScope(Dispatchers.IO).launch {
            try{
                serviceLogic(intent)
            }
            catch (e: Exception){
                Timber.e(e)
                // TODO show notification?
            }
            finally {
                // TODO if returned without setting next check, update session status to finished.
                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Alarm Service")
            .setContentText("Checking your conditions...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            channelId,
            "Alarm Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    private suspend fun serviceLogic(intent: Intent?){
        if (intent != null && intent.getIntExtra(INTENT_SESSION_ID_KEY, -1) != -1){
            currentSession = AppDatabase.getDatabase(this@AlarmService)
                .getSessionRepository().getSession(intent.getIntExtra(INTENT_SESSION_ID_KEY, -1))
        }
        else{
            currentSession = AppDatabase.getDatabase(this@AlarmService)
                .getSessionRepository().newSession()
        }

        Timber.d("Session: $currentSession")

        if (currentSession.status != SessionStatus.RUNNING){
            Timber.d("Session deactivated...")
            return
        }

        try {
            checkConditions()
        }
        catch (_: IOException){
            // Internet error.
            if(currentSession.failsCount < 9){
                // TODO notify the user about the session failure.
                AppDatabase.getDatabase(this).getSessionRepository().updateSessionStatus(
                    currentSession.sessionId,
                    SessionStatus.FINISHED
                )
            }
            else{
                scheduleNextFallbackCheck()
            }
        }
    }

    private suspend fun checkConditions() {
        val relevantGames = collectRelevantGames()
        Timber.d("Number of relevant games: ${relevantGames.size}")
        if (relevantGames.isEmpty()){
            return
        }
        val gamesWithNextTime = relevantGames.map { this@AlarmService.calculateGameNextRelevantTime(it) }

        val gamesToAlarm = gamesWithNextTime.filter { it.nextRelevantTime!! == 0L }
        val relevantGamesWithNextTime = gamesWithNextTime.filter { it.nextRelevantTime!! > 0L }
        if (!relevantGamesWithNextTime.isEmpty()){
            scheduleNextCheckPerGames(relevantGamesWithNextTime)
        }
        if (!gamesToAlarm.isEmpty()){
            startAlarm(gamesToAlarm)
        }
    }

    private suspend fun collectRelevantGames(): List<ScheduledGameWithConditions> {
        val sessionGamesWithConditions =
            AppDatabase.getDatabase(this@AlarmService).getSessionGamesRepository()
                .getTodaySessionGamesWithConditions(currentSession.sessionId)

        // Update the session games status to scheduling.
        sessionGamesWithConditions.forEach {
            it.sessionGameStatus = SessionGameStatus.SCHEDULING
            it.scheduledTime = null
        }
        AppDatabase.getDatabase(this@AlarmService).getSessionGamesRepository()
            .insertOrUpdateSessionGames(sessionGamesWithConditions.map { it.sessionGame })

        // TODO handle the case in which the scoreboard has not been updated yet to today.
        //  In this case probably use the schedule.
        val todayScoreboard = fetchScoreBoardData()

        return sessionGamesWithConditions.mapNotNull{ sessionGameWithConditions ->
            val gameData =
                todayScoreboard.scoreboard.games.find { it.gameId == sessionGameWithConditions.gameId }
            if (gameData != null) {
                ScheduledGameWithConditions(sessionGameWithConditions, gameData, null)
            }
            else{
                // TODO what to do here? the game is marked as scheduling but could not be found in the scoreboard.
                //  maybe mark as could not schedule currently?
                null
            }
        }
    }

    private suspend fun calculateGameNextRelevantTime(scheduledGameWithConditions: ScheduledGameWithConditions): ScheduledGameWithConditions{
        return scheduledGameWithConditions.apply {
            nextRelevantTime = com.assaf.basketclock.conditions.calculateGameNextRelevantTime(scheduledGameWithConditions)

            // Print the relevant time (system seconds) as local datetime
            Timber.d("Game ${gameData.gameId}(${gameData.homeTeam.teamName} vs ${gameData.awayTeam.teamName}) " +
                    "Next relevant time: ${systemSecondsToLocalDateTime(nextRelevantTime!!)}")

            if (nextRelevantTime == -1L){
                gameWithConditions.sessionGameStatus = SessionGameStatus.FINISHED
            }
            else if(nextRelevantTime == 0L){
                gameWithConditions.sessionGameStatus = SessionGameStatus.ALARMED
            }
            else{
                gameWithConditions.sessionGameStatus = SessionGameStatus.SCHEDULED
                gameWithConditions.scheduledTime = nextRelevantTime
            }
            AppDatabase.getDatabase(this@AlarmService).getSessionGamesRepository().insertOrUpdateSessionGame(gameWithConditions.sessionGame)
        }
    }

    private suspend fun scheduleNextCheckPerGames(relevantGamesWithNextTime: List<ScheduledGameWithConditions>){
        val nextRelevantGame = relevantGamesWithNextTime.minByOrNull { it.nextRelevantTime!! }
        // TODO support in less than minute times.
        val nextRelevantTime = max(System.currentTimeMillis() + 1000 * 61, nextRelevantGame!!.nextRelevantTime!!)
        scheduleNextCheck(nextRelevantTime)
    }

    private suspend fun scheduleNextFallbackCheck(){
        // TODO add a field to the sessions table with the next scheduled time and the reason for it.
        //  this way the user will be able to know why we waiting the fallback time (an error happened)
        Timber.d("Fallback check.")
        scheduleNextCheck(calculateNextCheckByFails(), true)
    }

    private suspend fun scheduleNextCheck(nextRelevantTime: Long, fail: Boolean = false){
        Timber.d("Scheduling next check to ${systemSecondsToLocalDateTime(nextRelevantTime)}")
        if(fail){
            AppDatabase.getDatabase(this).getSessionRepository().updateSessionFailCount(currentSession.sessionId, currentSession.failsCount + 1)
        }

        val alarmManager = this.getSystemService(ALARM_SERVICE) as AlarmManager

        // Create an intent for the receiver
        val intent = Intent(this, SimpleServiceStarter::class.java)
        intent.putExtra(INTENT_SESSION_ID_KEY, currentSession.sessionId)

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            SimpleServiceStarter.BROADCAST_ID,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule the next alarm.
        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            nextRelevantTime,
            pendingIntent
        )
    }

    private fun calculateNextCheckByFails(): Long{
        if (currentSession.failsCount < 3){
            return System.currentTimeMillis() + 1000 * 60
        }
        else if (currentSession.failsCount < 5){
            return System.currentTimeMillis() + 1000 * 60 * 5
        }
        else{
            return System.currentTimeMillis() + 1000 * 60 * 10
        }
    }

    private fun startAlarm(alarmedGames: List<ScheduledGameWithConditions>){
        Timber.d("Alarming for ${alarmedGames.size} games!")
        val serviceIntent = Intent(this, AlarmClockService::class.java)
        serviceIntent.putExtra(AlarmClockService.SESSION_ID_INTENT_KEY, currentSession.sessionId)
        serviceIntent.putStringArrayListExtra(AlarmClockService.GAME_IDS_INTENT_KEY, ArrayList(alarmedGames.map { it.gameData.gameId }))
        this.startForegroundService(serviceIntent)
    }
}

fun fireAlarmService(context: Context, extras: Bundle? = null){
    val intent = Intent(context, AlarmService::class.java)
    if (extras != null) {
        intent.putExtras(extras)
    }
    context.startForegroundService(intent)
}

fun stopAlarmService(context: Context){
    context.stopService(Intent(context, AlarmService::class.java))
    cancelAlarmServiceCurrentSession(context)
}

fun cancelAlarmServiceCurrentSession(context: Context){
    if (isSessionActive(context)){
        CoroutineScope(Dispatchers.IO).launch{
            // There is a weird case here in which the AlarmService just started and not created
            //  the new session in the DB yet before this function run.
            // In this case the session will actually won't get killed (The session will get
            //  created after this so it will still be marked as running).
            val lastSession = AppDatabase.getDatabase(context).getSessionRepository().getLastSession()

            // Only kill running sessions.
            if (lastSession.status == SessionStatus.RUNNING){
                AppDatabase.getDatabase(context).getSessionRepository().updateSessionStatus(
                    lastSession.sessionId,
                    SessionStatus.KILLED
                )
            }
        }
    }

    val intent = Intent(context, SimpleServiceStarter::class.java)
    PendingIntent.getBroadcast(
        context,
        SimpleServiceStarter.BROADCAST_ID,
        intent,
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

fun isSessionActive(context: Context): Boolean{
    return AlarmService.isRunning() || isSessionAlarmSet(context)
}
