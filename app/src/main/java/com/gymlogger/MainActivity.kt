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
import androidx.compose.ui.platform.LocalFocusManager
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate called, savedInstanceState is null: ${savedInstanceState == null}")
        setContent {
            val context = LocalContext.current
            val themeHue by SettingsRepository.activeThemeHue.collectAsStateWithLifecycle(0f)
            
            var isInitialized by rememberSaveable { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                Log.d("MainActivity", "LaunchedEffect(Unit) triggered, isInitialized: $isInitialized")
                SettingsRepository.init(context)
                ExerciseRepository.init(context)
                RoutineRepository.init(context)
                MealRepository.init(context)

                // Check for in-progress workout and start service if needed
                launch {
                    val inProgress = RoutineRepository.getInProgressWorkout(context)
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

                        // Initialization Overlay
                        if (!isInitialized) {
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
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(64.dp),
                                        strokeWidth = 6.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(
                                        text = "Initializing...",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
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
