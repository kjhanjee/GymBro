package com.gymlogger

import android.util.Log
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import com.gymlogger.ai.MacroCalculator
import com.gymlogger.data.ExerciseRepository
import com.gymlogger.data.MealRepository
import com.gymlogger.data.RoutineRepository
import com.gymlogger.data.SettingsRepository
import com.gymlogger.service.WorkoutService
import kotlinx.coroutines.*
import com.gymlogger.ui.navigation.AppNavigation
import com.gymlogger.ui.theme.GymBroTheme
import android.content.Intent
import kotlinx.coroutines.launch

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import android.content.ComponentCallbacks2

import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity(), ComponentCallbacks2 {

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            Log.w("MainActivity", "Memory pressure high (level $level) - releasing AI engine")
            scope.launch {
                MacroCalculator.release()
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate called, savedInstanceState is null: ${savedInstanceState == null}")
        setContent {
            val context = LocalContext.current
            val themeHue by SettingsRepository.activeThemeHue.collectAsStateWithLifecycle(0f)
            val downloadProgress by MacroCalculator.downloadProgress.collectAsStateWithLifecycle()
            val isAiReady by MacroCalculator.isReady.collectAsStateWithLifecycle()
            
            var isInitialized by rememberSaveable { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                Log.d("MainActivity", "LaunchedEffect(Unit) triggered, isInitialized: $isInitialized, isAiReady: $isAiReady")
                SettingsRepository.init()
                ExerciseRepository.init()
                RoutineRepository.init()
                MealRepository.init()

                // Check for in-progress workout and start service if needed
                launch {
                    val inProgress = RoutineRepository.getInProgressWorkout()
                    if (inProgress != null && (inProgress.startTimeMillis != null || inProgress.exerciseStates.isNotEmpty())) {
                        Log.d("MainActivity", "Starting WorkoutService to restore in-progress workout")
                        val intent = Intent(context, WorkoutService::class.java).apply {
                            action = WorkoutService.ACTION_RESTORE_WORKOUT
                        }
                        context.startService(intent)
                    }
                }
                
                if (!isInitialized) {
                    launch {
                        isInitialized = true
                        Log.d("MainActivity", "Initialization complete")
                    }
                } else {
                    Log.d("MainActivity", "Already initialized")
                }
            }

            GymBroTheme(hue = themeHue) {
                val focusManager = LocalFocusManager.current
                val isImeVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp
                
                LaunchedEffect(isImeVisible) {
                    if (!isImeVisible) {
                        focusManager.clearFocus()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (isInitialized) {
                            Column(modifier = Modifier.statusBarsPadding()) {
                                AppNavigation()
                            }
                        }

                        // Download/Initialization Overlay
                        if (downloadProgress != null || !isInitialized) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.7f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(32.dp)
                                ) {
                                    if (downloadProgress != null) {
                                        CircularProgressIndicator(
                                            progress = downloadProgress ?: 0f,
                                            modifier = Modifier.size(64.dp),
                                            strokeWidth = 6.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(64.dp),
                                            strokeWidth = 6.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(
                                        text = if (downloadProgress != null) "Downloading AI Model..." else "Initializing...",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    val currentProgress = downloadProgress
                                    if (currentProgress != null) {
                                        Text(
                                            text = "${(currentProgress * 100).toInt()}%",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Gemma 2B is ~1.5GB. Please stay on this screen.",
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
