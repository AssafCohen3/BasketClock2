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
import com.assaf.basketclock.AppDatabase
import com.assaf.basketclock.GameData
import com.assaf.basketclock.GameWithConditions
import com.assaf.basketclock.PBPGame
import com.assaf.basketclock.R
import com.assaf.basketclock.fetchPBPData
import com.assaf.basketclock.fetchScoreBoardData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import kotlin.math.max
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class ScheduledGameWithConditions(
    val gameWithConditions: GameWithConditions,
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

data class SessionDetails(
    val sessionId: String,
    val failsCount: Int,
)

class AlarmService: Service() {
    private val channelId = "AlarmServiceChannel"
    private val notificationId = 1
    private lateinit var currentSession: SessionDetails
    companion object{
        private const val INTENT_FAILS_KEY: String = "fails"
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

        currentSession = SessionDetails(
            intent?.getStringExtra(INTENT_SESSION_ID_KEY) ?: Uuid.random().toString(),
            intent?.getIntExtra(INTENT_FAILS_KEY, 0) ?: 0
        )

        ServiceCompat.startForeground(
            this,
            notificationId,
            createNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                checkConditions()
            }
            catch (_: IOException){
                if(currentSession.failsCount < 9){
                    // TODO notify the user about the session failure.
                }
                else{
                    scheduleNextFallbackCheck()
                }
            }
            catch (e: Exception){
                Timber.e(e)
                // TODO show notification?
            }
            stopSelf()
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

    private fun checkConditions() {
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        coroutineScope.launch {
            val relevantGames = collectRelevantGames()
            if (relevantGames.isEmpty()){
                return@launch
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
    }

    private suspend fun collectRelevantGames(): List<ScheduledGameWithConditions> {
        val gamesWithConditions =
            AppDatabase.getDatabase(this@AlarmService).getConditionsRepository()
                .getTodayGamesConditions()

        // TODO handle the case in which the scoreboard has not been updated yet.
        val todayScoreboard = fetchScoreBoardData()

        return gamesWithConditions.mapNotNull{ gameWithConditions ->
            val gameData =
                todayScoreboard.scoreboard.games.find { it.gameId == gameWithConditions.gameId }
            if (gameData != null) {
                ScheduledGameWithConditions(gameWithConditions, gameData, null)
            }
            else{
                null
            }
        }
    }

    private suspend fun calculateGameNextRelevantTime(scheduledGameWithConditions: ScheduledGameWithConditions): ScheduledGameWithConditions{
        return scheduledGameWithConditions.apply {
            nextRelevantTime = com.assaf.basketclock.conditions.calculateGameNextRelevantTime(scheduledGameWithConditions)
        }
    }

    private fun scheduleNextCheckPerGames(relevantGamesWithNextTime: List<ScheduledGameWithConditions>){
        val nextRelevantGame = relevantGamesWithNextTime.minByOrNull { it.nextRelevantTime!! }
        // TODO support in less than minute times.
        val nextRelevantTime = max(System.currentTimeMillis() + 1000 * 61, nextRelevantGame!!.nextRelevantTime!!)
        scheduleNextCheck(nextRelevantTime)
    }

    private fun scheduleNextFallbackCheck(){
        scheduleNextCheck(calculateNextCheckByFails(), true)
    }

    private fun scheduleNextCheck(nextRelevantTime: Long, fail: Boolean = false){
        Timber.d("Scheduling next check at $nextRelevantTime")
        val alarmManager = this.getSystemService(ALARM_SERVICE) as AlarmManager

        // Create an intent for the receiver
        val intent = Intent(this, SimpleServiceStarter::class.java)
        if(fail){
            intent.putExtra(INTENT_FAILS_KEY, currentSession.failsCount + 1)
        }
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
        // TODO implement
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
    val intent = Intent(context, SimpleServiceStarter::class.java)
    PendingIntent.getBroadcast(
        context,
        SimpleServiceStarter.BROADCAST_ID,
        intent,
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
