package com.gymlogger.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import android.media.RingtoneManager
import com.gymlogger.data.ExerciseRepository
import com.gymlogger.data.RoutineRepository
import com.gymlogger.data.SettingsRepository
import com.gymlogger.model.Exercise
import com.gymlogger.model.Routine
import com.gymlogger.model.WorkoutSet
import com.gymlogger.data.Workout
import com.gymlogger.ui.components.GymBroTopAppBar
import com.gymlogger.ui.components.ExerciseSelectionDialog
import com.gymlogger.util.UnitConverter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Stable
class SetState(
    weight: String = "",
    reps: String = "",
    rir: String = "",
    isCompleted: Boolean = false
) {
    var weight by mutableStateOf(weight)
    var reps by mutableStateOf(reps)
    var rir by mutableStateOf(rir)
    var isCompleted by mutableStateOf(isCompleted)
}

@Stable
class ExerciseState(
    val exercise: Exercise,
    restTime: Int = 2,
    val sets: SnapshotStateList<SetState> = mutableStateListOf()
) {
    var restTime by mutableStateOf(restTime)
}

@Composable
fun WorkoutTrackerScreen(
    onNavigateBack: () -> Unit,
    routineId: Long? = null
) {
    val exercises = remember { mutableStateListOf<ExerciseState>() }
    var workoutTitle by remember { mutableStateOf("Log Workout") }
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    
    // Rest Timer State
    var restSecondsRemaining by remember { mutableStateOf(0) }
    var isRestTimerActive by remember { mutableStateOf(false) }
    var activeRestTimerExerciseId by remember { mutableStateOf<Long?>(null) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val weightUnit by SettingsRepository.getWeightUnit(context).collectAsState(initial = SettingsRepository.WeightUnit.LBS)
    val timerUnit by SettingsRepository.getTimerUnit(context).collectAsState(initial = SettingsRepository.TimerUnit.MINUTES)

    // Timer logic
    var secondsElapsed by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            secondsElapsed++
        }
    }

    val timerText = remember(secondsElapsed) {
        val h = secondsElapsed / 3600
        val m = (secondsElapsed % 3600) / 60
        val s = secondsElapsed % 60
        if (h > 0) {
            "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
        } else {
            "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
        }
    }

    // Rest Timer Logic
    LaunchedEffect(isRestTimerActive, restSecondsRemaining) {
        if (isRestTimerActive && restSecondsRemaining > 0) {
            delay(1000)
            restSecondsRemaining -= 1
            if (restSecondsRemaining == 0) {
                isRestTimerActive = false
                // Play alarm
                try {
                    val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val r = RingtoneManager.getRingtone(context, notification)
                    r.play()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Handle unit changes for existing data
    var lastWeightUnit by remember { mutableStateOf(weightUnit) }
    var lastTimerUnit by remember { mutableStateOf(timerUnit) }

    LaunchedEffect(weightUnit) {
        if (lastWeightUnit != weightUnit) {
            exercises.forEach { ex ->
                ex.sets.forEach { set ->
                    if (set.weight.isNotBlank()) {
                        val baseWeight = UnitConverter.weightToBase(set.weight, lastWeightUnit)
                        set.weight = UnitConverter.formatWeight(baseWeight, weightUnit)
                    }
                }
            }
            lastWeightUnit = weightUnit
        }
    }

    LaunchedEffect(timerUnit) {
        if (lastTimerUnit != timerUnit) {
            exercises.forEach { ex ->
                val baseTimer = UnitConverter.timerToBase(ex.restTime.toString(), lastTimerUnit)
                ex.restTime = UnitConverter.formatTimer(baseTimer, timerUnit).toIntOrNull() ?: 0
            }
            lastTimerUnit = timerUnit
        }
    }

    BackHandler {
        showExitDialog = true
    }

    // Initialize from routine if provided
    LaunchedEffect(routineId) {
        if (routineId != null) {
            val routine = RoutineRepository.getRoutineById(routineId)
            if (routine != null) {
                workoutTitle = routine.name
                exercises.clear()
                routine.exercises.forEach { routineEx ->
                    val exercise = ExerciseRepository.getExerciseById(routineEx.exerciseId)
                    if (exercise != null) {
                        val firstSet = routineEx.sets.firstOrNull()
                        val restInUnit = UnitConverter.formatTimer(firstSet?.restTime ?: 2, timerUnit).toIntOrNull() ?: 0
                        val exerciseState = ExerciseState(
                            exercise = exercise,
                            restTime = restInUnit
                        )
                        routineEx.sets.forEach { setConfig ->
                            exerciseState.sets.add(
                                SetState(
                                    weight = if (setConfig.targetWeight == null || setConfig.targetWeight == 0f) "" else UnitConverter.formatWeight(setConfig.targetWeight, weightUnit),
                                    reps = if (setConfig.targetReps == null || setConfig.targetReps == 0) "" else setConfig.targetReps.toString(),
                                    rir = if (setConfig.targetRir == null) "" else setConfig.targetRir.toString()
                                )
                            )
                        }
                        exercises.add(exerciseState)
                    }
                }
            }
        }
    }

    fun saveWorkout() {
        scope.launch {
            val workoutSets = exercises.flatMap { exerciseState ->
                exerciseState.sets.mapIndexed { index, setState ->
                    com.gymlogger.model.WorkoutSet(
                        id = System.currentTimeMillis() + index,
                        exerciseId = exerciseState.exercise.id,
                        exerciseName = exerciseState.exercise.name,
                        type = com.gymlogger.model.WorkoutSet.SetType.NORMAL,
                        reps = setState.reps.toIntOrNull(),
                        weight = UnitConverter.weightToBase(setState.weight, weightUnit),
                        rir = setState.rir.toIntOrNull(),
                        isCompleted = setState.isCompleted
                    )
                }
            }
            
            val workout = com.gymlogger.data.Workout(
                id = System.currentTimeMillis(),
                routine = com.gymlogger.model.Routine(
                    id = routineId ?: 0L,
                    name = workoutTitle,
                    exercises = emptyList() // Simplified for now
                ),
                date = System.currentTimeMillis(),
                sets = workoutSets
            )
            
            RoutineRepository.completeWorkout(context, workout)
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            Column {
                // Timer Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1C1C1E)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = timerText,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        if (isRestTimerActive) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Rest: ${restSecondsRemaining / 60}:${(restSecondsRemaining % 60).toString().padStart(2, '0')}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                GymBroTopAppBar(
                    title = workoutTitle,
                    onNavigateBack = { showExitDialog = true },
                    actions = {
                        Button(
                            onClick = { saveWorkout() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Finish", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                )
            }
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { focusManager.clearFocus() }
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                itemsIndexed(exercises) { index, exerciseState ->
                    ExerciseTrackerItem(
                        exerciseState = exerciseState,
                        weightUnit = weightUnit.name,
                        timerUnit = timerUnit.name,
                        onRemove = { exercises.removeAt(index) },
                        onMoveUp = {
                            if (index > 0) {
                                val item = exercises.removeAt(index)
                                exercises.add(index - 1, item)
                            }
                        },
                        onMoveDown = {
                            if (index < exercises.size - 1) {
                                val item = exercises.removeAt(index)
                                exercises.add(index + 1, item)
                            }
                        },
                        onToggleSet = { completed ->
                            if (completed) {
                                restSecondsRemaining = UnitConverter.timerToSeconds(exerciseState.restTime.toString(), timerUnit)
                                isRestTimerActive = true
                                activeRestTimerExerciseId = exerciseState.exercise.id
                            }
                        },
                        onRestTimeChange = { newTime ->
                            if (isRestTimerActive && activeRestTimerExerciseId == exerciseState.exercise.id) {
                                restSecondsRemaining = UnitConverter.timerToSeconds(newTime.toString(), timerUnit)
                            }
                        }
                    )
                }

                item {
                    Spacer(Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Button(
                            onClick = { showAddExerciseDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C1E)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Add Exercise", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    if (showAddExerciseDialog) {
        ExerciseSelectionDialog(
            onDismiss = { showAddExerciseDialog = false },
            onSelect = { exercise ->
                val restInUnit = UnitConverter.formatTimer(2, timerUnit).toIntOrNull() ?: 2
                exercises.add(ExerciseState(exercise, restInUnit, mutableStateListOf(SetState())))
                showAddExerciseDialog = false
            }
        )
    }

    if (showExitDialog) {
        val hue by SettingsRepository.activeThemeHue.collectAsState()
        com.gymlogger.ui.theme.GymBroTheme(hue = hue) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title = { Text("End Workout?") },
                text = { Text("Do you want to save this workout or discard it?") },
                confirmButton = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { saveWorkout() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Save Workout")
                        }
                        Button(
                            onClick = { onNavigateBack() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Discard Workout", color = MaterialTheme.colorScheme.error)
                        }
                        TextButton(
                            onClick = { showExitDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancel", color = Color.White)
                        }
                    }
                },
                containerColor = Color(0xFF1C1C1E),
                titleContentColor = Color.White,
                textContentColor = Color.White
            )
        }
    }
}

@Composable
fun ExerciseTrackerItem(
    exerciseState: ExerciseState,
    weightUnit: String,
    timerUnit: String,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggleSet: (Boolean) -> Unit,
    onRestTimeChange: (Int) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        // Exercise Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val equipmentName = exerciseState.exercise.equipment.name.lowercase().replaceFirstChar { it.uppercase() }
                Text(
                    "${exerciseState.exercise.name} ($equipmentName)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    BasicTextField(
                        value = exerciseState.restTime.toString(),
                        onValueChange = { newValue ->
                            val newTime = newValue.toIntOrNull() ?: 0
                            exerciseState.restTime = newTime
                            onRestTimeChange(newTime)
                        },
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.width(30.dp)
                    )
                    Text(
                        text = if (timerUnit == "MINUTES") " min rest" else " sec rest",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Color(0xFF1C1C1E))
                ) {
                    DropdownMenuItem(
                        text = { Text("Move Up", color = Color.White) },
                        onClick = { onMoveUp(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color.White) }
                    )
                    DropdownMenuItem(
                        text = { Text("Move Down", color = Color.White) },
                        onClick = { onMoveDown(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color.White) }
                    )
                    DropdownMenuItem(
                        text = { Text("Remove", color = MaterialTheme.colorScheme.error) },
                        onClick = { onRemove(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }

        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("SET", modifier = Modifier.width(40.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Text(weightUnit, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Text("REPS", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Text("RIR", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.width(40.dp))
        }

        // Table Rows
        exerciseState.sets.forEachIndexed { index, setState ->
            SetRow(
                index = index + 1,
                setState = setState,
                onToggleComplete = onToggleSet
            )
        }

        // Add Set Button
        Button(
            onClick = { exerciseState.sets.add(SetState()) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            Text("+ Add Set", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondary)
        }
    }
}

@Composable
fun SetRow(
    index: Int,
    setState: SetState,
    onToggleComplete: (Boolean) -> Unit
) {
    val backgroundColor = if (setState.isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Set Number
        Text(
            text = index.toString(),
            modifier = Modifier.width(40.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        // Weight Input
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
                .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(4.dp))
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = setState.weight,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.all { it.isDigit() || it == '.' }) {
                        setState.weight = newValue
                    }
                },
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSecondary, textAlign = TextAlign.Center, fontSize = 16.sp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Reps Input
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
                .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(4.dp))
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = setState.reps,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                        setState.reps = newValue
                    }
                },
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSecondary, textAlign = TextAlign.Center, fontSize = 16.sp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // RIR Input
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
                .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(4.dp))
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = setState.rir,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                        setState.rir = newValue
                    }
                },
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSecondary, textAlign = TextAlign.Center, fontSize = 16.sp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Completion Checkbox
        Box(
            modifier = Modifier
                .width(40.dp)
                .padding(start = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = { 
                    setState.isCompleted = !setState.isCompleted 
                    onToggleComplete(setState.isCompleted)
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (setState.isCompleted) Color(0xFF4CD964) else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
