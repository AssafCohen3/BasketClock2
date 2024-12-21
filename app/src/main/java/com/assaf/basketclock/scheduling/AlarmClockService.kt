package com.assaf.basketclock.scheduling

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.assaf.basketclock.R
import com.assaf.basketclock.data.AppDatabase
import com.assaf.basketclock.data.ConditionGame
import com.assaf.basketclock.data.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmClockService: Service() {
    companion object{
        const val SESSION_ID_INTENT_KEY = "sessionId"
        const val GAME_IDS_INTENT_KEY = "gameIds"
        private const val CHANNEL_ID = "AlarmClockServiceChannel"
        private const val NOTIFICATION_ID = 2
    }

    private lateinit var mediaPlayer: MediaPlayer
    private val stopAlarmHandler = Handler(Looper.getMainLooper())
    private val stopRunnable = Runnable { stopSelf() }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // TODO change alarm.
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm)
        mediaPlayer.isLooping = true
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Alarm Clock Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {


        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch{
            // Ensure the session has not been killed.
            val session = AppDatabase.getDatabase(this@AlarmClockService).getSessionRepository()
                .getSession(intent!!.getIntExtra(SESSION_ID_INTENT_KEY, -1))
            if (session.status == SessionStatus.KILLED){
                this@AlarmClockService.stopSelf()
                return@launch
            }

            // Start playing the alarm sound
            mediaPlayer.start()

            val games = AppDatabase.getDatabase(this@AlarmClockService).getConditionsRepository()
                .getGames(intent.getStringArrayListExtra(GAME_IDS_INTENT_KEY)!!.toList())
            // Show the notification
            showNotification(games)

            // Schedule auto-stop after 1 minute
            // TODO decide time.
            stopAlarmHandler.postDelayed(stopRunnable, 60_000) // 60 seconds
        }


        return START_STICKY
    }

    private fun showNotification(games: List<ConditionGame>) {
        val stopIntent = Intent(this, StopAlarmClockReceiver::class.java)
        val stopPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.app_logo)
            .setContentTitle("BasketClock Alarm!")
            .setContentText(buildNotificationText(games))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setStyle(NotificationCompat.BigTextStyle())
            .addAction(R.drawable.app_logo, "Stop", stopPendingIntent)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    private fun buildNotificationText(games: List<ConditionGame>): String {
        val stringBuilder = StringBuilder()
        stringBuilder.appendLine("The Following Games Conditions Are Met:")
        for (game in games) {
            stringBuilder.appendLine("${game.homeTeamName} vs ${game.awayTeamName}")
        }

        return stringBuilder.toString()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop the alarm sound
        mediaPlayer.stop()
        mediaPlayer.release()

        // Cancel the auto-stop handler
        stopAlarmHandler.removeCallbacks(stopRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}