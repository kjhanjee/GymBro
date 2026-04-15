package com.gymlogger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymlogger.data.SettingsRepository
import com.gymlogger.ui.components.GymBroTopAppBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val savedWeightUnit by SettingsRepository.getWeightUnit(context).collectAsState(initial = SettingsRepository.WeightUnit.LBS)
    val savedTimerUnit by SettingsRepository.getTimerUnit(context).collectAsState(initial = SettingsRepository.TimerUnit.MINUTES)

    var weightUnit by remember(savedWeightUnit) { mutableStateOf(savedWeightUnit) }
    var timerUnit by remember(savedTimerUnit) { mutableStateOf(savedTimerUnit) }
    val themeHue by SettingsRepository.activeThemeHue.collectAsState()

    val onHueChange = { newHue: Float ->
        SettingsRepository.updateActiveHue(newHue)
    }

    Scaffold(
        topBar = {
            GymBroTopAppBar(
                title = "Settings",
                onNavigateBack = onNavigateBack,
                actions = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                SettingsRepository.setWeightUnit(context, weightUnit)
                                SettingsRepository.setTimerUnit(context, timerUnit)
                                SettingsRepository.setThemeHue(context, themeHue)
                                onNavigateBack()
                            }
                        }
                    ) {
                        Text("Save", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            SettingsSection(title = "Units") {
                UnitToggle(
                    label = "Weight Unit",
                    options = listOf("KG", "LBS"),
                    selectedOption = weightUnit.name,
                    onOptionSelected = {
                        weightUnit = SettingsRepository.WeightUnit.valueOf(it)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                UnitToggle(
                    label = "Timer Unit",
                    options = listOf("MINUTES", "SECONDS"),
                    selectedOption = timerUnit.name,
                    onOptionSelected = {
                        timerUnit = SettingsRepository.TimerUnit.valueOf(it)
                    }
                )
            }

            SettingsSection(title = "Appearance") {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Theme Color", color = Color.White, fontSize = 16.sp)
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Slider(
                        value = themeHue,
                        onValueChange = onHueChange,
                        valueRange = 0f..360f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color(0xFF2C2C2E)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Surface(
            color = Color(0xFF1C1C1E),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun UnitToggle(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.White, fontSize = 16.sp)
        Row(
            modifier = Modifier
                .height(32.dp)
                .width(160.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                Button(
                    onClick = { onOptionSelected(option) },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = if (option == options.first()) MaterialTheme.shapes.small else MaterialTheme.shapes.small, // Simplified
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF2C2C2E),
                        contentColor = if (isSelected) Color.White else Color(0xFF8E8E93)
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(text = option, fontSize = 12.sp)
                }
            }
        }
    }
}
