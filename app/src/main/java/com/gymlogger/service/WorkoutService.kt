package com.gymlogger.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.gymlogger.MainActivity
import java.util.Locale

class WorkoutService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var timerJob: Job? = null
    private var restTimerJob: Job? = null

    private val _secondsElapsed = MutableStateFlow(0L)
    val secondsElapsed = _secondsElapsed.asStateFlow()

    private val _restSecondsRemaining = MutableStateFlow(0)
    val restSecondsRemaining = _restSecondsRemaining.asStateFlow()

    private val _isRestTimerActive = MutableStateFlow(false)
    val isRestTimerActive = _isRestTimerActive.asStateFlow()

    private val binder = WorkoutBinder()

    inner class WorkoutBinder : Binder() {
        fun getService(): WorkoutService = this@WorkoutService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_WORKOUT -> startWorkout()
            ACTION_STOP_WORKOUT -> stopWorkout()
            ACTION_START_REST -> {
                val seconds = intent.getIntExtra(EXTRA_REST_SECONDS, 0)
                startRestTimer(seconds)
            }
        }
        return START_STICKY
    }

    private fun startWorkout() {
        if (timerJob != null) return
        
        startForeground(NOTIFICATION_ID, createNotification("Workout in progress..."))
        
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1000)
                _secondsElapsed.value += 1
                updateNotification()
            }
        }
    }

    private fun stopWorkout() {
        timerJob?.cancel()
        timerJob = null
        restTimerJob?.cancel()
        restTimerJob = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    fun startRestTimer(seconds: Int) {
        restTimerJob?.cancel()
        _restSecondsRemaining.value = seconds
        _isRestTimerActive.value = true
        
        restTimerJob = serviceScope.launch {
            while (_restSecondsRemaining.value > 0) {
                delay(1000)
                _restSecondsRemaining.value -= 1
                updateNotification()
            }
            _isRestTimerActive.value = false
            playAlarm()
            updateNotification()
        }
    }

    fun stopRestTimer() {
        restTimerJob?.cancel()
        _isRestTimerActive.value = false
        _restSecondsRemaining.value = 0
        updateNotification()
    }

    private fun playAlarm() {
        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val r = RingtoneManager.getRingtone(applicationContext, notification)
            r.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Workout Timer",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || 
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            manager.notify(NOTIFICATION_ID, createNotification(getNotificationContent()))
        }
    }

    private fun getNotificationContent(): String {
        val h = _secondsElapsed.value / 3600
        val m = (_secondsElapsed.value % 3600) / 60
        val s = _secondsElapsed.value % 60
        val workoutTime = if (h > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", m, s)
        }

        return if (_isRestTimerActive.value) {
            val rm = _restSecondsRemaining.value / 60
            val rs = _restSecondsRemaining.value % 60
            "Workout: $workoutTime | Rest: ${String.format(Locale.getDefault(), "%02d:%02d", rm, rs)}"
        } else {
            "Workout: $workoutTime"
        }
    }

    private fun createNotification(content: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GymBro Workout")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val CHANNEL_ID = "workout_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START_WORKOUT = "ACTION_START_WORKOUT"
        const val ACTION_STOP_WORKOUT = "ACTION_STOP_WORKOUT"
        const val ACTION_START_REST = "ACTION_START_REST"
        const val EXTRA_REST_SECONDS = "EXTRA_REST_SECONDS"
    }
}
