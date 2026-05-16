package com.gymlogger.ui.screens

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymlogger.data.ExerciseRepository
import com.gymlogger.data.RoutineRepository
import com.gymlogger.data.SettingsRepository
import com.gymlogger.model.Exercise
import com.gymlogger.model.WorkoutSet
import com.gymlogger.service.WorkoutService
import com.gymlogger.ui.components.ExerciseSelectionDialog
import com.gymlogger.ui.components.GymBroTopAppBar
import com.gymlogger.util.UnitConverter
import com.gymlogger.viewmodel.WorkoutTrackerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

@Stable
class SetState(
    baseWeight: Float? = null,
    reps: String = "",
    rir: String = "",
    type: WorkoutSet.SetType = WorkoutSet.SetType.NORMAL,
    isCompleted: Boolean = false
) {
    var baseWeight by mutableStateOf(baseWeight) // Always in KG
    var weightDisplay by mutableStateOf("")     // UI String (LBS or KG)
    var reps by mutableStateOf(reps)
    var rir by mutableStateOf(rir)
    var type by mutableStateOf(type)
    var isCompleted by mutableStateOf(isCompleted)
}

@Stable
class ExerciseState(
    val exercise: Exercise,
    restTime: Int = 120,
    val sets: SnapshotStateList<SetState> = mutableStateListOf(),
    inputType: WorkoutSet.InputType = WorkoutSet.InputType.REPS,
    val id: Long = System.currentTimeMillis() + (Math.random() * 1000000).toLong()
) {
    var restTime by mutableStateOf(restTime)
    var restTimeInput by mutableStateOf("")
    var inputType by mutableStateOf(inputType)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkoutTrackerScreen(
    onNavigateBack: () -> Unit,
    routineId: Long? = null,
    viewModel: WorkoutTrackerViewModel = viewModel()
) {
    val context = LocalContext.current
    val exercises = viewModel.exercises
    val workoutTitle by viewModel.workoutTitle.collectAsStateWithLifecycle()
    val isInitialized by viewModel.isInitialized.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.init(context, routineId)
    }

    if (!isInitialized) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var exerciseIndexToChange by remember { mutableStateOf<Int?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    // Drag and drop state
    var activeDraggingItemId by remember { mutableStateOf<Long?>(null) }
    var activeDroppingItemId by remember { mutableStateOf<Long?>(null) }
    var draggingOffset by remember { mutableStateOf(Offset.Zero) }
    val droppingOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val haptic = LocalHapticFeedback.current

    // Auto-scroll logic
    LaunchedEffect(activeDraggingItemId) {
        if (activeDraggingItemId == null) return@LaunchedEffect
        while (true) {
            val layoutInfo = listState.layoutInfo
            val viewportStart = layoutInfo.viewportStartOffset
            val viewportEnd = layoutInfo.viewportEndOffset
            val threshold = with(density) { 60.dp.toPx() }
            
            val currentDraggedIndex = exercises.indexOfFirst { it.id == activeDraggingItemId }
            if (currentDraggedIndex == -1) break
            
            val draggedItem = layoutInfo.visibleItemsInfo.find { it.index == currentDraggedIndex }
            
            if (draggedItem != null) {
                val itemTop = draggedItem.offset + draggingOffset.y
                val itemBottom = itemTop + draggedItem.size
                
                if (itemTop < viewportStart + threshold) {
                    val scrollAmount = (viewportStart + threshold - itemTop).coerceAtMost(20f)
                    val scrolled = listState.scrollBy(-scrollAmount)
                    draggingOffset = draggingOffset.copy(y = draggingOffset.y + scrolled)
                } else if (itemBottom > viewportEnd - threshold) {
                    val scrollAmount = (itemBottom - (viewportEnd - threshold)).coerceAtMost(20f)
                    val scrolled = listState.scrollBy(scrollAmount)
                    draggingOffset = draggingOffset.copy(y = draggingOffset.y + scrolled)
                }
            }
            delay(10)
        }
    }

    fun moveExercise(from: Int, to: Int) {
        if (to !in exercises.indices) return
        val item = exercises.removeAt(from)
        exercises.add(to, item)
    }
    
    val weightUnit by SettingsRepository.getWeightUnit(context).collectAsStateWithLifecycle(initialValue = SettingsRepository.WeightUnit.LBS)
    val timerUnit by SettingsRepository.getTimerUnit(context).collectAsStateWithLifecycle(initialValue = SettingsRepository.TimerUnit.MINUTES)

    // Service Connection
    var workoutService by remember { mutableStateOf<WorkoutService?>(null) }
    val connection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as WorkoutService.WorkoutBinder
                workoutService = binder.getService()
            }
            override fun onServiceDisconnected(name: ComponentName?) {
                workoutService = null
            }
        }
    }

    DisposableEffect(Unit) {
        val intent = Intent(context, WorkoutService::class.java).apply {
            action = WorkoutService.ACTION_START_WORKOUT
        }
        context.startService(intent)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        onDispose {
            context.unbindService(connection)
        }
    }

    // Timer logic from service
    val secondsElapsed by workoutService?.secondsElapsed?.collectAsStateWithLifecycle(0L) ?: remember { mutableStateOf(0L) }
    val restSecondsRemaining by workoutService?.restSecondsRemaining?.collectAsStateWithLifecycle(0) ?: remember { mutableStateOf(0) }
    val isRestTimerActive by workoutService?.isRestTimerActive?.collectAsStateWithLifecycle(false) ?: remember { mutableStateOf(false) }

    val timerText = remember(secondsElapsed) {
        val h = secondsElapsed / 3600
        val m = (secondsElapsed % 3600) / 60
        val s = secondsElapsed % 60
        if (h > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", m, s)
        }
    }

    // Update service stats
    LaunchedEffect(workoutTitle, exercises.size, exercises.sumOf { it.sets.size }, exercises.sumOf { it.sets.count { s -> s.isCompleted } }) {
        val total = exercises.sumOf { it.sets.size }
        val completed = exercises.sumOf { it.sets.count { s -> s.isCompleted } }
        workoutService?.updateWorkoutStats(workoutTitle, total, completed)
    }

    val scope = rememberCoroutineScope()

    // Handle unit changes for existing data
    var lastWeightUnit by remember { mutableStateOf(weightUnit) }
    var lastTimerUnitRef by remember { mutableStateOf<SettingsRepository.TimerUnit?>(null) }

    // Handle unit changes: Refresh UI display values when units toggle
    LaunchedEffect(weightUnit, timerUnit, isInitialized, exercises.size) {
        if (isInitialized) {
            exercises.forEach { ex ->
                // Refresh weight strings for all sets from their BASE (KG)
                ex.sets.forEach { set ->
                    if (set.weightDisplay.isEmpty() || lastWeightUnit != weightUnit) {
                        set.weightDisplay = UnitConverter.formatWeight(set.baseWeight, weightUnit)
                    }
                }
                
                // Refresh rest time input string from its BASE (Seconds)
                if (ex.restTimeInput.isEmpty() || lastTimerUnitRef != timerUnit) {
                    ex.restTimeInput = UnitConverter.formatTimer(ex.restTime, timerUnit)
                }
            }
            lastWeightUnit = weightUnit
            lastTimerUnitRef = timerUnit
        }
    }

    BackHandler {
        showExitDialog = true
    }

    fun saveWorkout() {
        scope.launch {
            // Stop service
            val stopIntent = Intent(context, WorkoutService::class.java).apply {
                action = WorkoutService.ACTION_STOP_WORKOUT
            }
            context.startService(stopIntent)

            val workoutSets = exercises.flatMap { exerciseState ->
                exerciseState.sets
                    .filter { it.isCompleted }
                    .mapIndexed { index, setState ->
                        com.gymlogger.model.WorkoutSet(
                            id = System.currentTimeMillis() + index + exerciseState.exercise.id,
                            exerciseId = exerciseState.exercise.id,
                            exerciseName = exerciseState.exercise.name,
                            type = setState.type,
                            reps = setState.reps.toIntOrNull(),
                            weight = setState.baseWeight ?: 0f,
                            rir = setState.rir.toIntOrNull(),
                            inputType = exerciseState.inputType,
                            isCompleted = setState.isCompleted,
                            restTime = exerciseState.restTime
                        )
                    }
            }
            
            if (workoutSets.isNotEmpty()) {
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
                RoutineRepository.clearInProgressWorkout(context)
            }
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
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                            if (isRestTimerActive) {
                                Spacer(modifier = Modifier.width(12.dp))
                                VerticalDivider(modifier = Modifier.height(14.dp), color = Color.White.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.width(12.dp))
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9F0A),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = String.format(Locale.getDefault(), "%02d:%02d", restSecondsRemaining / 60, restSecondsRemaining % 60),
                                    color = Color(0xFFFF9F0A),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { workoutService?.stopRestTimer() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)
                    }
                }
                GymBroTopAppBar(
                    title = workoutTitle,
                    onNavigateBack = { showExitDialog = true },
                    actions = {
                        TextButton(onClick = { saveWorkout() }) {
                            Text("Finish", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddExerciseDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, "Add Exercise")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                BasicTextField(
                    value = workoutTitle,
                    onValueChange = { viewModel.setWorkoutTitle(it) },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }

            itemsIndexed(exercises, key = { _, item -> item.id }) { index, exerciseState ->
                val isDragging = activeDraggingItemId == exerciseState.id
                val isDropping = activeDroppingItemId == exerciseState.id
                
                val scale by animateFloatAsState(if (isDragging) 1.05f else 1f)
                val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp)
                val dragYOffset by animateFloatAsState(if (isDragging) draggingOffset.y else if (isDropping) droppingOffset.value.y else 0f)
                
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            translationY = dragYOffset
                            scaleX = scale
                            scaleY = scale
                        }
                        .zIndex(if (isDragging) 1f else 0f)
                ) {
                    ExerciseTrackerItem(
                        exerciseState = exerciseState,
                        weightUnit = weightUnit,
                        timerUnit = timerUnit,
                        onAddSet = {
                            exerciseState.sets.add(
                                SetState(
                                    type = exerciseState.sets.lastOrNull()?.type ?: WorkoutSet.SetType.NORMAL
                                )
                            )
                        },
                        onDeleteSet = { setIndex ->
                            if (exerciseState.sets.size > 1) {
                                exerciseState.sets.removeAt(setIndex)
                            }
                        },
                        onRemoveExercise = {
                            exercises.removeAt(index)
                        },
                        onToggleSetCompleted = { setIndex, completed ->
                            exerciseState.sets[setIndex].isCompleted = completed
                            if (completed && exerciseState.restTime > 0) {
                                workoutService?.startRestTimer(exerciseState.restTime)
                            }
                        },
                        onRestTimeChange = { newRest ->
                            exerciseState.restTime = newRest
                        },
                        onSwapExercise = {
                            exerciseIndexToChange = index
                            showAddExerciseDialog = true
                        },
                        modifier = Modifier
                            .pointerInput(exerciseState.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { offset ->
                                        activeDraggingItemId = exerciseState.id
                                        draggingOffset = Offset.Zero
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        draggingOffset += dragAmount
                                        
                                        val currentIndex = exercises.indexOfFirst { it.id == activeDraggingItemId }
                                        val targetIndex = (currentIndex + (draggingOffset.y / 200).roundToInt())
                                            .coerceIn(0, exercises.lastIndex)
                                        
                                        if (targetIndex != currentIndex) {
                                            activeDroppingItemId = exercises[targetIndex].id
                                            val itemHeight = 200f // Approximate
                                            scope.launch {
                                                droppingOffset.snapTo(Offset(0f, if (targetIndex > currentIndex) -itemHeight else itemHeight))
                                                moveExercise(currentIndex, targetIndex)
                                                draggingOffset = draggingOffset.copy(y = draggingOffset.y + (if (targetIndex > currentIndex) -itemHeight else itemHeight))
                                                droppingOffset.animateTo(Offset.Zero, spring())
                                                activeDroppingItemId = null
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        activeDraggingItemId = null
                                        draggingOffset = Offset.Zero
                                    },
                                    onDragCancel = {
                                        activeDraggingItemId = null
                                        draggingOffset = Offset.Zero
                                    }
                                )
                            }
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showAddExerciseDialog) {
        ExerciseSelectionDialog(
            onDismiss = { 
                showAddExerciseDialog = false
                exerciseIndexToChange = null
            },
            onSelect = { exercise ->
                val newState = ExerciseState(
                    exercise = exercise,
                    restTime = 0,
                    sets = mutableStateListOf(SetState()),
                    id = System.currentTimeMillis()
                )
                exerciseIndexToChange?.let { idx ->
                    exercises[idx] = newState
                } ?: run {
                    exercises.add(newState)
                }
                showAddExerciseDialog = false
                exerciseIndexToChange = null
            }
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit Workout?") },
            text = { Text("Your progress will be saved automatically, but the workout won't be completed. You can also discard the progress entirely.") },
            confirmButton = {
                TextButton(onClick = { 
                    showExitDialog = false
                    onNavigateBack() 
                }) {
                    Text("Exit", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        scope.launch {
                            showExitDialog = false
                            // Stop service
                            val stopIntent = Intent(context, WorkoutService::class.java).apply {
                                action = WorkoutService.ACTION_STOP_WORKOUT
                            }
                            context.startService(stopIntent)
                            // Clear state
                            RoutineRepository.clearInProgressWorkout(context)
                            onNavigateBack()
                        }
                    }) {
                        Text("Discard", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { showExitDialog = false }) {
                        Text("Stay")
                    }
                }
            }
        )
    }
}

@Composable
fun ExerciseTrackerItem(
    exerciseState: ExerciseState,
    weightUnit: SettingsRepository.WeightUnit,
    timerUnit: SettingsRepository.TimerUnit,
    onAddSet: () -> Unit,
    onDeleteSet: (Int) -> Unit,
    onRemoveExercise: () -> Unit,
    onToggleSetCompleted: (Int, Boolean) -> Unit,
    onRestTimeChange: (Int) -> Unit,
    onSwapExercise: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1C1C1E)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exerciseState.exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    val equipmentName = exerciseState.exercise.equipment.name.lowercase().replaceFirstChar { it.uppercase() }
                    Text(
                        text = equipmentName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
                
                Row {
                    IconButton(onClick = onSwapExercise) {
                        Icon(Icons.Default.SwapHoriz, "Swap", tint = Color.White.copy(alpha = 0.5f))
                    }
                    IconButton(onClick = onRemoveExercise) {
                        Icon(Icons.Default.Delete, "Remove", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Set", modifier = Modifier.width(40.dp), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f), textAlign = TextAlign.Center)
                Text("Type", modifier = Modifier.width(60.dp), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f), textAlign = TextAlign.Center)
                Text(weightUnit.name.lowercase(), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f), textAlign = TextAlign.Center)
                Text(if (exerciseState.inputType == WorkoutSet.InputType.REPS) "Reps" else "Time", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f), textAlign = TextAlign.Center)
                Text("RIR", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            exerciseState.sets.forEachIndexed { index, setState ->
                SetRow(
                    index = index + 1,
                    setState = setState,
                    onToggleCompleted = { completed -> onToggleSetCompleted(index, completed) },
                    inputType = exerciseState.inputType,
                    weightUnit = weightUnit,
                    onDelete = { onDeleteSet(index) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onAddSet,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Set", style = MaterialTheme.typography.labelLarge)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, null, modifier = Modifier.size(14.dp), tint = Color.White.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.width(4.dp))
                    BasicTextField(
                        value = exerciseState.restTimeInput,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.all { it.isDigit() || it == '.' }) {
                                exerciseState.restTimeInput = newValue
                                if (newValue.isNotEmpty() && newValue != ".") {
                                    val unit = timerUnit
                                    val seconds = UnitConverter.timerToSeconds(newValue, unit)
                                    exerciseState.restTime = seconds
                                    onRestTimeChange(seconds)
                                }
                            }
                        },
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .width(30.dp)
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                            .padding(vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(timerUnit.name.lowercase(), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun SetRow(
    index: Int,
    setState: SetState,
    onToggleCompleted: (Boolean) -> Unit,
    inputType: WorkoutSet.InputType,
    weightUnit: SettingsRepository.WeightUnit,
    onDelete: () -> Unit
) {
    var showTypeMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(
                if (setState.isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else Color.Transparent,
                RoundedCornerShape(8.dp)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Set Number
        Box(modifier = Modifier.width(40.dp), contentAlignment = Alignment.Center) {
            Text(
                text = index.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (setState.isCompleted) MaterialTheme.colorScheme.primary else Color.White
            )
        }

        // Set Type
        Box(modifier = Modifier.width(60.dp), contentAlignment = Alignment.Center) {
            val typeLabel = when (setState.type) {
                WorkoutSet.SetType.NORMAL -> "N"
                WorkoutSet.SetType.WARMUP -> "W"
                WorkoutSet.SetType.DROP_SET -> "D"
                WorkoutSet.SetType.FAILURE -> "F"
            }
            val typeColor = when (setState.type) {
                WorkoutSet.SetType.NORMAL -> Color.White.copy(alpha = 0.3f)
                WorkoutSet.SetType.WARMUP -> Color(0xFFFFD60A)
                WorkoutSet.SetType.DROP_SET -> Color(0xFF32ADE6)
                WorkoutSet.SetType.FAILURE -> Color(0xFFFF453A)
            }

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(typeColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    .clickable { showTypeMenu = true },
                contentAlignment = Alignment.Center
            ) {
                Text(typeLabel, color = typeColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                
                DropdownMenu(
                    expanded = showTypeMenu,
                    onDismissRequest = { showTypeMenu = false },
                    modifier = Modifier.background(Color(0xFF2C2C2E))
                ) {
                    WorkoutSet.SetType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                setState.type = type
                                showTypeMenu = false
                            }
                        )
                    }
                }
            }
        }

        // Weight
        Box(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
            BasicTextField(
                value = setState.weightDisplay,
                onValueChange = { 
                    setState.weightDisplay = it
                    setState.baseWeight = UnitConverter.weightToBase(it, weightUnit)
                },
                textStyle = TextStyle(
                    color = if (setState.isCompleted) MaterialTheme.colorScheme.primary else Color.White,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (it.isFocused && setState.weightDisplay.isEmpty()) { /* help? */ } }
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                    .padding(vertical = 8.dp)
            )
        }

        // Reps / Time
        Box(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
            BasicTextField(
                value = setState.reps,
                onValueChange = { setState.reps = it },
                textStyle = TextStyle(
                    color = if (setState.isCompleted) MaterialTheme.colorScheme.primary else Color.White,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                    .padding(vertical = 8.dp)
            )
        }

        // RIR
        Box(modifier = Modifier.weight(0.8f).padding(horizontal = 4.dp)) {
            BasicTextField(
                value = setState.rir,
                onValueChange = { if (it.length <= 2) setState.rir = it },
                textStyle = TextStyle(
                    color = if (setState.isCompleted) MaterialTheme.colorScheme.primary else Color.White,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                    .padding(vertical = 8.dp)
            )
        }

        // Checkmark / Delete
        Box(modifier = Modifier.width(48.dp), contentAlignment = Alignment.Center) {
            if (setState.isCompleted) {
                IconButton(onClick = { onToggleCompleted(false) }) {
                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                }
            } else {
                IconButton(
                    onClick = { onToggleCompleted(true) }
                ) {
                    Icon(Icons.Default.RadioButtonUnchecked, null, tint = Color.White.copy(alpha = 0.2f))
                }
            }
        }
    }
}
