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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isAiReady by MacroCalculator.isReady.collectAsState()
    val weightUnit by SettingsRepository.getWeightUnit(context).collectAsState(initial = SettingsRepository.WeightUnit.KG)

    var targetBodyPart by remember { mutableStateOf(MuscleGroup.CHEST) }
    var numExercises by remember { mutableStateOf("4") }
    var trainingGoal by remember { mutableStateOf("Hypertrophy") }
    
    var isLoading by remember { mutableStateOf(false) }
    var optimizedWorkout by remember { mutableStateOf<OptimizedWorkout?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                            
                            val workouts = RoutineRepository.getCompletedWorkouts().first()
                            val availableExercises = ExerciseRepository.filterByMuscleGroup(targetBodyPart)
                                .joinToString(", ") { it.name }

                            val relevantStats = workouts.flatMap { it.sets }
                                .filter { it.isCompleted }
                                .groupBy { it.exerciseName }
                                .map { (name, sets) ->
                                    val avgWeight = sets.mapNotNull { it.weight }.average()
                                    "$name: Avg ${"%.1f".format(avgWeight)}${weightUnit.name.lowercase()}"
                                }.joinToString("\n")

                            val prompt = """
                                System: You are an expert Strength Coach. Create an optimized workout routine.
                                Based on the user's history and goals, suggest specific exercises from the database.
                                CRITICAL: Return ONLY a valid JSON object. No conversational text, no markdown code blocks.
                                
                                JSON Structure to follow:
                                {
                                  "routineName": "string",
                                  "description": "string",
                                  "exercises": [
                                    {
                                      "exerciseName": "string (MUST match database name if possible)",
                                      "equipment": "string",
                                      "sets": [
                                        { "type": "NORMAL" | "WARMUP" | "FAILURE" | "DROP_SET", "reps": number, "weight": number, "rir": number }
                                      ]
                                    }
                                  ]
                                }
                                
                                IMPORTANT: "reps" MUST be an integer. Even for AMRAP, please provide a high number like 20.
                                All weights should be in ${weightUnit.name}.
                                ${if (trainingGoal == "Failure Build") "SPECIAL INSTRUCTION: For 'Failure Build' goal, ensure at least one set per exercise is of type 'FAILURE' with rir = 0." else ""}

                                User Details:
                                Body Part: ${targetBodyPart.name}
                                Goal: $trainingGoal
                                Number of Exercises: $numExercises
                                Preferred Weight Unit: ${weightUnit.name}
                                Available Exercises for this Body Part: $availableExercises
                                User's Average Strength Stats:
                                $relevantStats
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
                                    errorMessage = "Error parsing workout: ${e.localizedMessage}"
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
                                    val exerciseId = dbEx?.id ?: (System.currentTimeMillis() + index)
                                    Routine.RoutineExercise(
                                        id = 0,
                                        exerciseId = exerciseId,
                                        exerciseName = optEx.exerciseName,
                                        order = index,
                                        sets = optEx.sets.mapIndexed { sIndex, optSet ->
                                            Routine.RoutineExercise.SetConfig(
                                                id = sIndex.toLong() + 1,
                                                type = try { WorkoutSet.SetType.valueOf(optSet.type) } catch(e: Exception) { WorkoutSet.SetType.NORMAL },
                                                targetReps = optSet.reps.toIntOrNull() ?: 10,
                                                targetWeight = optSet.weight,
                                                targetRir = optSet.rir,
                                                restTime = 2
                                            )
                                        }
                                    )
                                }
                                val newRoutine = Routine(
                                    id = 0,
                                    name = workout.routineName,
                                    exercises = routineExercises,
                                    description = workout.description
                                )
                                RoutineRepository.createRoutine(context, newRoutine)
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
