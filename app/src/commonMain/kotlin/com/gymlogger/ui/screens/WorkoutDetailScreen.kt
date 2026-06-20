package com.gymlogger.ui.screens


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymlogger.data.RoutineRepository
import com.gymlogger.data.SettingsRepository
import com.gymlogger.data.Workout
import com.gymlogger.util.ShareManager
import com.gymlogger.ui.components.GymBroTopAppBar
import com.gymlogger.model.WorkoutSet
import com.gymlogger.util.UnitConverter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WorkoutDetailScreen(
    workoutId: Long,
    onNavigateBack: () -> Unit
) {
    val workouts by RoutineRepository.completedWorkouts.collectAsState()
    val workout = workouts.find { it.id == workoutId }
    val context = LocalContext.current
    val weightUnit by SettingsRepository.getWeightUnit().collectAsState(initial = SettingsRepository.WeightUnit.LBS)
    val dateFormatter = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault())
    val scope = rememberCoroutineScope()
    
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (workout == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Workout not found", color = Color.White)
        }
        return
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Workout Log?") },
            text = { Text("Are you sure you want to remove this workout? It will be permanently deleted and excluded from your statistics.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            RoutineRepository.deleteWorkout(workout.id)
                            onNavigateBack()
                        }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF1C1C1E),
            titleContentColor = Color.White,
            textContentColor = Color(0xFF8E8E93)
        )
    }

    Scaffold(
        topBar = {
            GymBroTopAppBar(
                title = workout.routine.name,
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                    }
                    IconButton(onClick = {
                        val htmlContent = buildWorkoutHtml(workout, weightUnit, dateFormatter)
                        ShareManager.shareWorkout(htmlContent)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        },
        containerColor = Color.Black
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = dateFormatter.format(Date(workout.date)),
                    fontSize = 16.sp,
                    color = Color(0xFF8E8E93)
                )
            }

            // Statistics Summary Cards
            item {
                WorkoutStatisticsGrid(workout, weightUnit)
            }

            item {
                HorizontalDivider(color = Color(0xFF2C2C2E))
            }

            // Group sets by exercise
            val setsByExercise = workout.sets.groupBy { it.exerciseName }

            items(setsByExercise.entries.toList()) { (exerciseName, sets) ->
                WorkoutSummaryExerciseRow(
                    exerciseName = exerciseName,
                    sets = sets,
                    weightUnit = weightUnit
                )
            }
        }
    }
}

private fun buildWorkoutHtml(workout: Workout, weightUnit: SettingsRepository.WeightUnit, dateFormatter: SimpleDateFormat): String {
    val sb = StringBuilder()
    sb.append("<h2>Workout: ${workout.routine.name}</h2>")
    sb.append("<p>Date: ${dateFormatter.format(Date(workout.date))}</p>")
    
    val volume = workout.sets.sumOf { ((it.weight ?: 0f) * (it.reps ?: 0)).toDouble() }.toFloat()
    sb.append("<p><b>Total Volume:</b> ${UnitConverter.formatWeight(volume, weightUnit)} ${weightUnit.name}</p>")
    sb.append("<p><b>Total Sets:</b> ${workout.sets.size}</p>")
    
    sb.append("<table border='1' style='border-collapse: collapse; width: 100%;'>")
    sb.append("<tr style='background-color: #f2f2f2;'>")
    sb.append("<th style='padding: 8px; text-align: left;'>Exercise</th>")
    sb.append("<th style='padding: 8px; text-align: center;'>Set</th>")
    sb.append("<th style='padding: 8px; text-align: center;'>Reps</th>")
    sb.append("<th style='padding: 8px; text-align: center;'>Weight</th>")
    sb.append("<th style='padding: 8px; text-align: center;'>RIR</th>")
    sb.append("</tr>")
    
    val setsByExercise = workout.sets.groupBy { it.exerciseName }
    setsByExercise.forEach { (exerciseName, sets) ->
        sets.forEachIndexed { index, set ->
            sb.append("<tr>")
            if (index == 0) {
                sb.append("<td rowspan='${sets.size}' style='padding: 8px;'>$exerciseName</td>")
            }
            sb.append("<td style='padding: 8px; text-align: center;'>${index + 1}</td>")
            sb.append("<td style='padding: 8px; text-align: center;'>${set.reps ?: 0}</td>")
            val weightStr = if ((set.weight ?: 0f) > 0) "${UnitConverter.formatWeight(set.weight!!, weightUnit)} ${weightUnit.name}" else "BW"
            sb.append("<td style='padding: 8px; text-align: center;'>$weightStr</td>")
            val rirStr = set.rir?.toString() ?: "-"
            sb.append("<td style='padding: 8px; text-align: center;'>$rirStr</td>")
            sb.append("</tr>")
        }
    }
    sb.append("</table>")
    sb.append("<p>Shared from GymBro</p>")
    
    return sb.toString()
}

@Composable
fun WorkoutStatisticsGrid(workout: Workout, weightUnit: SettingsRepository.WeightUnit) {
    // Basic Calculations
    val totalSets = workout.sets.size
    val volume = workout.sets.sumOf { ((it.weight ?: 0f) * (it.reps ?: 0)).toDouble() }.toFloat()
    val rirList = workout.sets.mapNotNull { it.rir }
    val avgRir = if (rirList.isNotEmpty()) rirList.average() else 0.0
    val failureSets = workout.sets.count { it.type == WorkoutSet.SetType.FAILURE }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatDetailCard("Volume", "${UnitConverter.formatWeight(volume, weightUnit)} ${weightUnit.name}", Modifier.weight(1f))
            StatDetailCard("Avg RIR", String.format(Locale.getDefault(), "%.1f", avgRir), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatDetailCard("Total Sets", totalSets.toString(), Modifier.weight(1f))
            StatDetailCard("Failure Sets", failureSets.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
fun StatDetailCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(label, fontSize = 12.sp, color = Color(0xFF8E8E93))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}
