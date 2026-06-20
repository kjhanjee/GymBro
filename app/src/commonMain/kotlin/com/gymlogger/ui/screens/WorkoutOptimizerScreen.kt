package com.gymlogger.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymlogger.ai.MacroCalculator
import com.gymlogger.data.ExerciseRepository
import com.gymlogger.data.RoutineRepository
import com.gymlogger.data.SettingsRepository
import com.gymlogger.model.Routine
import com.gymlogger.model.WorkoutSet
import com.gymlogger.model.MuscleGroup
import com.gymlogger.ui.components.GymBroTopAppBar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class OptimizedWorkout(
    @SerialName("routineName")
    val routineName: String,
    @SerialName("description")
    val description: String,
    @SerialName("exercises")
    val exercises: List<OptimizedExercise>
)

@Serializable
data class OptimizedExercise(
    @SerialName("exerciseName")
    val exerciseName: String,
    @SerialName("equipment")
    val equipment: String,
    @SerialName("sets")
    val sets: List<OptimizedSet>
)

@Serializable
data class OptimizedSet(
    @SerialName("type")
    val type: String,
    @SerialName("reps")
    val reps: String,
    @SerialName("weight")
    val weight: Float,
    @SerialName("rir")
    val rir: Int? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutOptimizerScreen(onNavigateBack: () -> Unit) {

    val coroutineScope = rememberCoroutineScope()
    val isAiReady by MacroCalculator.isReady.collectAsStateWithLifecycle()
    val weightUnit by SettingsRepository.getWeightUnit().collectAsStateWithLifecycle(initialValue = SettingsRepository.WeightUnit.KG)

    var targetBodyPart by remember { mutableStateOf(MuscleGroup.CHEST) }
    var numExercises by remember { mutableStateOf("4") }
    var trainingGoal by remember { mutableStateOf("Hypertrophy") }
    
    var isLoading by remember { mutableStateOf(false) }
    var optimizedWorkout by remember { mutableStateOf<OptimizedWorkout?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (!isAiReady) {
            MacroCalculator.init()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            coroutineScope.launch {
                MacroCalculator.release()
            }
        }
    }
    val goalOptions = listOf("Hypertrophy", "Strength", "Shaping", "Endurance", "Failure Build")
    var expandedGoal by remember { mutableStateOf(false) }
    var expandedMuscle by remember { mutableStateOf(false) }

    val json = Json { 
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    Scaffold(
        topBar = {
            GymBroTopAppBar(
                title = "Workout Optimizer",
                subtitle = if (isAiReady) "AI Powered" else "Initializing AI...",
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Body Part Selection
                ExposedDropdownMenuBox(
                    expanded = expandedMuscle,
                    onExpandedChange = { expandedMuscle = !expandedMuscle },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = targetBodyPart.name,
                        onValueChange = {},
                        label = { Text("Target Body Part") },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMuscle) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFF2C2C2E),
                            focusedContainerColor = Color(0xFF1C1C1E),
                            unfocusedContainerColor = Color(0xFF1C1C1E)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedMuscle,
                        onDismissRequest = { expandedMuscle = false },
                        modifier = Modifier.background(Color(0xFF1C1C1E))
                    ) {
                        MuscleGroup.entries.forEach { group ->
                            DropdownMenuItem(
                                text = { Text(group.name, color = Color.White) },
                                onClick = {
                                    targetBodyPart = group
                                    expandedMuscle = false
                                }
                            )
                        }
                    }
                }

                // Training Goal
                ExposedDropdownMenuBox(
                    expanded = expandedGoal,
                    onExpandedChange = { expandedGoal = !expandedGoal },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = trainingGoal,
                        onValueChange = {},
                        label = { Text("Training Goal") },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGoal) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFF2C2C2E),
                            focusedContainerColor = Color(0xFF1C1C1E),
                            unfocusedContainerColor = Color(0xFF1C1C1E)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedGoal,
                        onDismissRequest = { expandedGoal = false },
                        modifier = Modifier.background(Color(0xFF1C1C1E))
                    ) {
                        goalOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option, color = Color.White) },
                                onClick = {
                                    trainingGoal = option
                                    expandedGoal = false
                                }
                            )
                        }
                    }
                }

                // Num Exercises
                OutlinedTextField(
                    value = numExercises,
                    onValueChange = { if (it.all { char -> char.isDigit() }) numExercises = it },
                    label = { Text("Number of Exercises") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFF2C2C2E),
                        focusedContainerColor = Color(0xFF1C1C1E),
                        unfocusedContainerColor = Color(0xFF1C1C1E)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Weight Unit (Read-only)
                OutlinedTextField(
                    value = weightUnit.name,
                    onValueChange = {},
                    label = { Text("Weight Unit") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Gray,
                        unfocusedTextColor = Color.Gray,
                        focusedBorderColor = Color(0xFF2C2C2E),
                        unfocusedBorderColor = Color(0xFF2C2C2E),
                        focusedContainerColor = Color(0xFF1C1C1E),
                        unfocusedContainerColor = Color(0xFF1C1C1E)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp)) }
                )

                Button(
                    onClick = {
                        val count = numExercises.toIntOrNull() ?: 0
                        if (count <= 0) {
                            errorMessage = "Please enter a valid number of exercises."
                            return@Button
                        }
                        if (count > 15) {
                            errorMessage = "Maximum 15 exercises per routine."
                            return@Button
                        }

                        coroutineScope.launch {
                            isLoading = true
                            errorMessage = null
                            
                            val weightUnit = SettingsRepository.getWeightUnit().first()
                            val unit = weightUnit.name.lowercase()
                            val scheduleNames = SettingsRepository.getTrainingContext().first()
                            val allRoutines = RoutineRepository.getRoutines().first()
                            val scheduledRoutines = allRoutines.filter { it.name in scheduleNames }

                            val currentSchedule = if (scheduledRoutines.isEmpty()) {
                                "No routines scheduled."
                            } else {
                                scheduledRoutines.joinToString("\n") { routine ->
                                    val exerciseDetails = routine.exercises.joinToString("\n") { exercise ->
                                        val setsDetails = exercise.sets.joinToString(", ") { set ->
                                            val w = set.targetWeight?.toString() ?: "?"
                                            val r = set.targetReps?.toString() ?: "?"
                                            val rir = set.targetRir?.toString() ?: "?"
                                            "$w$unit x $r @ RIR $rir"
                                        }
                                        "  * ${exercise.exerciseName}: $setsDetails"
                                    }
                                    "- ${routine.name}:\n$exerciseDetails"
                                }
                            }

                            val workouts = RoutineRepository.getCompletedWorkouts().first().sortedByDescending { it.date }
                            val availableExercises = ExerciseRepository.filterByMuscleGroup(targetBodyPart).first()
                                .joinToString(", ") { "${it.name} (Equipment: ${it.equipment})" }

                            val relevantStats = workouts.flatMap { workout ->
                                workout.sets.filter { it.isCompleted }.map { it to workout.date }
                            }
                                .groupBy { it.first.exerciseName }
                                .map { (name, setPairs) ->
                                    val sets = setPairs.map { it.first }
                                    val maxWeight = sets.mapNotNull { it.weight }.maxOrNull() ?: 0f
                                    val latestDate = setPairs.maxOf { it.second }
                                    val recentSets = setPairs.filter { it.second == latestDate }.map { it.first }
                                    val recentAvgWeight = recentSets.mapNotNull { it.weight }.average()
                                    val recentAvgReps = recentSets.mapNotNull { it.reps }.average()
                                    "$name: Max ${com.gymlogger.util.formatFloat(maxWeight, 1)}$unit | Last Session: ${com.gymlogger.util.formatFloat(recentAvgWeight.toFloat(), 1)}$unit x ${com.gymlogger.util.formatFloat(recentAvgReps.toFloat(), 1)} reps"
                                }.joinToString("\n")

                            val prompt = """
                                <|turn>system
                                You are "GymBro AI", a world-class Strength and Conditioning Coach.
                                Your mission is to design a high-performance workout routine tailored to the user's specific goals and equipment.
                                
                                CONTEXT:
                                - Target Muscle Group: ${targetBodyPart.name}
                                - Training Goal: $trainingGoal
                                - Preferred Unit: ${weightUnit.name}
                                - Available Exercises in Database: $availableExercises
                                
                                USER HISTORY & PROGRESS:
                                $relevantStats
                                
                                CURRENT WEEKLY SCHEDULE (For context):
                                $currentSchedule

                                INSTRUCTIONS:
                                1. Select exactly $numExercises exercises from the "Available Exercises" list that best fit the Goal.
                                2. For each exercise, prescribe a detailed set/rep/weight/RIR scheme.
                                3. Progressively overload: If the user has history for an exercise, suggest weights slightly higher than their "Last Session" or "Max" if appropriate for the goal.
                                4. Set Types: Use 'WARMUP' for the first set if it's a heavy compound. Use 'NORMAL' generally. Use 'FAILURE' or 'DROP_SET' for high intensity.
                                5. RIR (Reps In Reserve): 0-1 for Strength/Failure, 1-3 for Hypertrophy.
                                
                                OUTPUT FORMAT:
                                Return ONLY a valid JSON object. No markdown, no extra text.
                                {
                                  "routineName": "Creative name for the workout",
                                  "description": "Short explanation of the strategy",
                                  "exercises": [
                                    {
                                      "exerciseName": "Exact name from available list",
                                      "equipment": "Equipment type",
                                      "sets": [
                                        { "type": "NORMAL", "reps": "10", "weight": 60.0, "rir": 2 }
                                      ]
                                    }
                                  ]
                                }
                                
                                CRITICAL: "reps" must be a STRING. "weight" must be a FLOAT. "rir" must be an INTEGER or null.
                                <|think|><turn|>
                                <|turn>user
                                Optimize my workout for ${targetBodyPart.name} with goal: $trainingGoal.
                                <turn|>
                                <|turn>model
                            """.trimIndent()

                            val response = MacroCalculator.generateResponse(prompt)
                            if (response != null) {
                                try {
                                    val cleanJson = response.trim()
                                        .replace("```json", "")
                                        .replace("```", "")
                                        .trim()
                                    
                                    val jsonStart = cleanJson.indexOf("{")
                                    val jsonEnd = cleanJson.lastIndexOf("}")
                                    if (jsonStart != -1 && jsonEnd != -1) {
                                        val extracted = cleanJson.substring(jsonStart, jsonEnd + 1)
                                        optimizedWorkout = json.decodeFromString<OptimizedWorkout>(extracted)
                                    } else {
                                        errorMessage = "AI output did not contain a valid JSON object."
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Error parsing workout: ${e.message}"
                                }
                            } else {
                                errorMessage = "AI failed to respond."
                            }
                            isLoading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading && isAiReady
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black, strokeWidth = 2.dp)
                    } else {
                        Text("Optimize My Workout")
                    }
                }

                errorMessage?.let {
                    Text(it, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                }

                optimizedWorkout?.let { workout ->
                    Text(workout.routineName, style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text(workout.description, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF8E8E93))
                    
                    workout.exercises.forEach { exercise ->
                        OptimizedExerciseCard(exercise, weightUnit.name.lowercase())
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val routineExercises = workout.exercises.mapIndexed { index, optEx ->
                                    val dbEx = ExerciseRepository.exerciseDatabase.find { it.name.lowercase() == optEx.exerciseName.lowercase() }
                                    val exerciseId = dbEx?.id ?: (com.gymlogger.util.getCurrentTimeMillis() + index)
                                    Routine.RoutineExercise(
                                        id = 0,
                                        exerciseId = exerciseId,
                                        exerciseName = optEx.exerciseName,
                                        order = index,
                                        sets = optEx.sets.mapIndexed { sIndex, optSet ->
                                            Routine.RoutineExercise.SetConfig(
                                                id = sIndex.toLong() + 1,
                                                type = try { WorkoutSet.SetType.valueOf(optSet.type) } catch(e: Exception) { WorkoutSet.SetType.NORMAL },
                                                targetReps = optSet.reps.toIntOrNull() ?: optSet.reps.filter { it.isDigit() }.toIntOrNull() ?: 10,
                                                targetWeight = optSet.weight,
                                                targetRir = optSet.rir,
                                                restTime = when(trainingGoal) {
                                                    "Strength" -> 180
                                                    "Hypertrophy" -> 90
                                                    "Endurance" -> 60
                                                    else -> 90
                                                },
                                                completedSets = emptyList()
                                            )
                                        },
                                        inputType = WorkoutSet.InputType.REPS
                                    )
                                }
                                val newRoutine = Routine(
                                    id = 0,
                                    name = workout.routineName,
                                    exercises = routineExercises,
                                    description = workout.description,
                                    createdAt = com.gymlogger.util.getCurrentTimeMillis()
                                )
                                RoutineRepository.createRoutine(newRoutine)
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Add as New Routine", color = Color.Black)
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun OptimizedExerciseCard(exercise: OptimizedExercise, unit: String) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(exercise.exerciseName, style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text("${exercise.equipment} • ${exercise.sets.size} Sets", style = MaterialTheme.typography.bodySmall, color = Color(0xFF8E8E93))
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color(0xFF8E8E93)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(color = Color(0xFF2C2C2E))
                    exercise.sets.forEachIndexed { index, set ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Set ${index + 1}: ${set.type}", style = MaterialTheme.typography.bodySmall, color = Color.White)
                            Text("${set.reps} reps @ ${set.weight}$unit (RIR: ${set.rir ?: "N/A"})", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
