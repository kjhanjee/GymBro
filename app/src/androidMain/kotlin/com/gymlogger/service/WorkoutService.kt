package com.gymlogger.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.gymlogger.data.InProgressWorkout
import com.gymlogger.data.RoutineRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.gymlogger.MainActivity
import kotlinx.coroutines.launch
import java.util.Locale

class WorkoutService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null
    private var restTimerJob: Job? = null

    private var startTimeMillis: Long? = null
    private var restEndTimeMillis: Long? = null

    private val _secondsElapsed = MutableStateFlow(0L)
    val secondsElapsed = _secondsElapsed.asStateFlow()

    private val _workoutTitle = MutableStateFlow("Tracked Workout")
    val workoutTitle = _workoutTitle.asStateFlow()

    private val _totalSets = MutableStateFlow(0)
    val totalSets = _totalSets.asStateFlow()

    private val _completedSets = MutableStateFlow(0)
    val completedSets = _completedSets.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive = _isActive.asStateFlow()

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
        Log.d("WorkoutService", "onCreate")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("WorkoutService", "onStartCommand: action=${intent?.action}")
        
        if (intent == null) {
            // Sticky restart
            restoreStateFromRepository()
            return START_STICKY
        }

        when (intent.action) {
            ACTION_START_WORKOUT -> startWorkout()
            ACTION_STOP_WORKOUT -> stopWorkout()
            ACTION_START_REST -> {
                val seconds = intent.getIntExtra(EXTRA_REST_SECONDS, 0)
                startRestTimer(seconds)
            }
            ACTION_RESTORE_WORKOUT -> restoreStateFromRepository()
        }
        return START_STICKY
    }

    private fun restoreStateFromRepository() {
        serviceScope.launch {
            val inProgress = RoutineRepository.getInProgressWorkout()
            if (inProgress != null && (inProgress.startTimeMillis != null || inProgress.exerciseStates.isNotEmpty())) {
                Log.d("WorkoutService", "Restoring workout state")
                
                _workoutTitle.value = inProgress.workoutTitle
                _isActive.value = true
                startTimeMillis = inProgress.startTimeMillis ?: (System.currentTimeMillis() - inProgress.secondsElapsed * 1000)
                restEndTimeMillis = inProgress.restEndTimeMillis

                // Calculate completed/total sets from exercise states
                _totalSets.value = inProgress.exerciseStates.sumOf { it.sets.size }
                _completedSets.value = inProgress.exerciseStates.sumOf { it.sets.count { s -> s.isCompleted } }

                // Immediate notification for foreground status
                ensureForeground()
                
                startTimer()
                
                restEndTimeMillis?.let { endTime ->
                    val remaining = ((endTime - System.currentTimeMillis()) / 1000).toInt()
                    if (remaining > 0) {
                        startRestTimer(remaining, isRestoration = true)
                    } else {
                        restEndTimeMillis = null
                    }
                }
            } else {
                Log.d("WorkoutService", "No workout to restore, stopping service")
                stopSelf()
            }
        }
    }

    private fun ensureForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            // Permission not granted, but we must call startForeground on Android 12+
            // On specialUse, we should have the permission though.
        }
        startForeground(NOTIFICATION_ID, createNotification())
    }

    private fun startWorkout() {
        if (_isActive.value) return

        _isActive.value = true
        
        serviceScope.launch {
            val inProgress = RoutineRepository.getInProgressWorkout()
            if (inProgress?.startTimeMillis != null) {
                startTimeMillis = inProgress.startTimeMillis
            } else {
                startTimeMillis = System.currentTimeMillis()
                RoutineRepository.updateInProgressWorkout { 
                    it.copy(startTimeMillis = startTimeMillis) 
                }
            }
            ensureForeground()
            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (_isActive.value) {
                startTimeMillis?.let { start ->
                    val elapsed = (System.currentTimeMillis() - start) / 1000
                    _secondsElapsed.value = elapsed
                }
                updateNotification()
                delay(1000)
            }
        }
    }

    fun stopWorkout() {
        Log.d("WorkoutService", "stopWorkout")
        timerJob?.cancel()
        timerJob = null
        restTimerJob?.cancel()
        restTimerJob = null
        _isActive.value = false
        _secondsElapsed.value = 0
        _totalSets.value = 0
        _completedSets.value = 0
        _isRestTimerActive.value = false
        _restSecondsRemaining.value = 0
        startTimeMillis = null
        restEndTimeMillis = null
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    fun updateWorkoutStats(title: String, total: Int, completed: Int) {
        _workoutTitle.value = title
        _totalSets.value = total
        _completedSets.value = completed
        updateNotification()
    }

    fun startRestTimer(seconds: Int, isRestoration: Boolean = false) {
        restTimerJob?.cancel()
        
        if (!isRestoration) {
            restEndTimeMillis = System.currentTimeMillis() + (seconds * 1000)
            serviceScope.launch {
                RoutineRepository.updateInProgressWorkout {
                    it.copy(restEndTimeMillis = restEndTimeMillis)
                }
            }
        }

        _restSecondsRemaining.value = seconds
        _isRestTimerActive.value = true
        
        restTimerJob = serviceScope.launch {
            while (_isRestTimerActive.value) {
                restEndTimeMillis?.let { end ->
                    val remaining = ((end - System.currentTimeMillis()) / 1000).toInt()
                    if (remaining <= 0) {
                        _restSecondsRemaining.value = 0
                        _isRestTimerActive.value = false
                        restEndTimeMillis = null
                        RoutineRepository.updateInProgressWorkout {
                            it.copy(restEndTimeMillis = null)
                        }
                        playAlarm()
                    } else {
                        _restSecondsRemaining.value = remaining
                    }
                }
                updateNotification()
                delay(1000)
            }
        }
    }

    fun stopRestTimer() {
        restTimerJob?.cancel()
        _isRestTimerActive.value = false
        _restSecondsRemaining.value = 0
        restEndTimeMillis = null
        serviceScope.launch {
            RoutineRepository.updateInProgressWorkout {
                it.copy(restEndTimeMillis = null)
            }
        }
        updateNotification()
    }

    private fun playAlarm() {
        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val r = RingtoneManager.getRingtone(applicationContext, notification)
            r.play()
        } catch (e: Exception) {
            Log.e("WorkoutService", "Error playing alarm", e)
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
        if (!_isActive.value) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(NOTIFICATION_ID)
            return
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || 
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            manager.notify(NOTIFICATION_ID, createNotification())
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

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GymBro Workout in Progress")
            .setContentText(getNotificationContent())
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onDestroy() {
        Log.d("WorkoutService", "onDestroy")
        _isActive.value = false
        _isRestTimerActive.value = false
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val CHANNEL_ID = "workout_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START_WORKOUT = "ACTION_START_WORKOUT"
        const val ACTION_STOP_WORKOUT = "ACTION_STOP_WORKOUT"
        const val ACTION_START_REST = "ACTION_START_REST"
        const val ACTION_RESTORE_WORKOUT = "ACTION_RESTORE_WORKOUT"
        const val EXTRA_REST_SECONDS = "EXTRA_REST_SECONDS"
    }
}
