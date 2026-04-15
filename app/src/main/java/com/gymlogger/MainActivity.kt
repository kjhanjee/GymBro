package com.gymlogger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.gymlogger.data.MealRepository
import com.gymlogger.data.RoutineRepository
import com.gymlogger.data.SettingsRepository
import com.gymlogger.ui.navigation.AppNavigation
import com.gymlogger.ui.theme.GymBroTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val themeHue by SettingsRepository.activeThemeHue.collectAsState()
            val downloadProgress by MacroCalculator.downloadProgress.collectAsState()
            
            var isInitialized by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                SettingsRepository.init(context)
                RoutineRepository.init(context)
                MealRepository.init(context)
                
                launch {
                    MacroCalculator.prepareModel(context)
                    MacroCalculator.init(context)
                    isInitialized = true
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
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = {
                                focusManager.clearFocus()
                            })
                        },
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
