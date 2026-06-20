package com.gymlogger

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymlogger.data.ExerciseRepository
import com.gymlogger.data.MealRepository
import com.gymlogger.data.RoutineRepository
import com.gymlogger.data.SettingsRepository
import com.gymlogger.ui.navigation.AppNavigation
import com.gymlogger.ui.theme.GymBroTheme

fun MainViewController() = ComposeUIViewController { App() }

@Composable
fun App() {
    var isInitialized by remember { mutableStateOf(false) }
    val themeHue by SettingsRepository.activeThemeHue.collectAsStateWithLifecycle(0f)

    LaunchedEffect(Unit) {
        SettingsRepository.init()
        ExerciseRepository.init()
        RoutineRepository.init()
        MealRepository.init()
        isInitialized = true
    }

    GymBroTheme(hue = themeHue) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isInitialized) {
                    AppNavigation()
                }
            }
        }
    }
}
