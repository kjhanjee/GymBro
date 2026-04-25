package com.gymlogger.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.gestures.scrollBy
import kotlin.math.roundToInt
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
import com.gymlogger.service.WorkoutService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import java.util.Locale

@Stable
class SetState(
    weight: String = "",
    reps: String = "",
    rir: String = "",
    type: WorkoutSet.SetType = WorkoutSet.SetType.NORMAL,
    isCompleted: Boolean = false
) {
    var weight by mutableStateOf(weight)
    var reps by mutableStateOf(reps)
    var rir by mutableStateOf(rir)
    var type by mutableStateOf(type)
    var isCompleted by mutableStateOf(isCompleted)
}

@Stable
class ExerciseState(
    val exercise: Exercise,
    restTime: Int = 2,
    val sets: SnapshotStateList<SetState> = mutableStateListOf(),
    inputType: WorkoutSet.InputType = WorkoutSet.InputType.REPS,
    val id: Long = System.currentTimeMillis() + (Math.random() * 1000000).toLong()
) {
    var restTime by mutableStateOf(restTime)
    var restTimeInput by mutableStateOf(restTime.toString())
    var inputType by mutableStateOf(inputType)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkoutTrackerScreen(
    onNavigateBack: () -> Unit,
    routineId: Long? = null
) {
    val exercises = remember { mutableStateListOf<ExerciseState>() }
    var workoutTitle by remember { mutableStateOf("Log Workout") }
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
    
    val context = LocalContext.current
    val weightUnit by SettingsRepository.getWeightUnit(context).collectAsState(initial = SettingsRepository.WeightUnit.LBS)
    val timerUnit by SettingsRepository.getTimerUnit(context).collectAsState(initial = SettingsRepository.TimerUnit.MINUTES)

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
    val secondsElapsed by workoutService?.secondsElapsed?.collectAsState(0L) ?: remember { mutableStateOf(0L) }
    val restSecondsRemaining by workoutService?.restSecondsRemaining?.collectAsState(0) ?: remember { mutableStateOf(0) }
    val isRestTimerActive by workoutService?.isRestTimerActive?.collectAsState(false) ?: remember { mutableStateOf(false) }
    var activeRestTimerExerciseId by remember { mutableStateOf<Long?>(null) }

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
                val newRestTime = UnitConverter.formatTimer(baseTimer, timerUnit).toIntOrNull() ?: 0
                ex.restTime = newRestTime
                ex.restTimeInput = newRestTime.toString()
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
                            restTime = restInUnit,
                            sets = mutableStateListOf(),
                            inputType = routineEx.inputType,
                            id = routineEx.id
                        ).apply {
                            restTimeInput = restInUnit.toString()
                        }
                        routineEx.sets.forEach { setConfig ->
                            exerciseState.sets.add(
                                SetState(
                                    weight = if (setConfig.targetWeight == null || setConfig.targetWeight == 0f) "" else UnitConverter.formatWeight(setConfig.targetWeight, weightUnit),
                                    reps = if (setConfig.targetReps == null || setConfig.targetReps == 0) "" else setConfig.targetReps.toString(),
                                    rir = if (setConfig.targetRir == null) "" else setConfig.targetRir.toString(),
                                    type = setConfig.type
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
                            weight = UnitConverter.weightToBase(setState.weight, weightUnit),
                            rir = setState.rir.toIntOrNull(),
                            inputType = exerciseState.inputType,
                            isCompleted = setState.isCompleted
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
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                itemsIndexed(exercises, key = { _, state -> state.id }) { index, exerciseState ->
                    val isDragging = activeDraggingItemId == exerciseState.id
                    val isDropping = activeDroppingItemId == exerciseState.id
                    
                    val elevation by animateDpAsState(if (isDragging) 24.dp else 0.dp)
                    val scale by animateFloatAsState(if (isDragging) 1.08f else 1f)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (isDragging) Modifier else Modifier.animateItemPlacement())
                            .zIndex(if (isDragging) 100f else 1f)
                            .graphicsLayer {
                                if (isDragging) {
                                    translationX = draggingOffset.x
                                    translationY = draggingOffset.y
                                    scaleX = scale
                                    scaleY = scale
                                    shadowElevation = elevation.toPx()
                                    shape = RoundedCornerShape(12.dp)
                                    clip = false
                                } else if (isDropping) {
                                    translationX = droppingOffset.value.x
                                    translationY = droppingOffset.value.y
                                }
                                ambientShadowColor = Color.Black
                                spotShadowColor = Color.Black
                            }
                            .pointerInput(exerciseState.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { 
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        activeDraggingItemId = exerciseState.id
                                        draggingOffset = Offset.Zero
                                    },
                                    onDragEnd = {
                                        val finalOffset = draggingOffset
                                        val droppedId = exerciseState.id
                                        activeDroppingItemId = droppedId
                                        activeDraggingItemId = null
                                        draggingOffset = Offset.Zero
                                        scope.launch {
                                            droppingOffset.snapTo(finalOffset)
                                            droppingOffset.animateTo(Offset.Zero, spring(stiffness = 500f))
                                            if (activeDroppingItemId == droppedId) activeDroppingItemId = null
                                        }
                                    },
                                    onDragCancel = {
                                        val finalOffset = draggingOffset
                                        val droppedId = exerciseState.id
                                        activeDroppingItemId = droppedId
                                        activeDraggingItemId = null
                                        draggingOffset = Offset.Zero
                                        scope.launch {
                                            droppingOffset.snapTo(finalOffset)
                                            droppingOffset.animateTo(Offset.Zero, spring(stiffness = 500f))
                                            if (activeDroppingItemId == droppedId) activeDroppingItemId = null
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        draggingOffset += dragAmount
                                        
                                        val currentDraggedIndex = exercises.indexOfFirst { it.id == exerciseState.id }
                                        if (currentDraggedIndex == -1) return@detectDragGesturesAfterLongPress
                                        
                                        val visibleItems = listState.layoutInfo.visibleItemsInfo
                                        val draggedItem = visibleItems.find { it.index == currentDraggedIndex } ?: return@detectDragGesturesAfterLongPress
                                        
                                        val spacing = with(density) { 16.dp.toPx() }

                                        if (draggingOffset.y > (draggedItem.size + spacing) * 0.5f && currentDraggedIndex < exercises.size - 1) {
                                            val nextItem = visibleItems.find { it.index == currentDraggedIndex + 1 }
                                            val nextSize = nextItem?.size ?: draggedItem.size
                                            moveExercise(currentDraggedIndex, currentDraggedIndex + 1)
                                            draggingOffset = draggingOffset.copy(y = draggingOffset.y - (nextSize + spacing))
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        } else if (draggingOffset.y < -(draggedItem.size + spacing) * 0.5f && currentDraggedIndex > 0) {
                                            val prevItem = visibleItems.find { it.index == currentDraggedIndex - 1 }
                                            val prevSize = prevItem?.size ?: draggedItem.size
                                            moveExercise(currentDraggedIndex, currentDraggedIndex - 1)
                                            draggingOffset = draggingOffset.copy(y = draggingOffset.y + (prevSize + spacing))
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                )
                            }
                    ) {
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
                                    val seconds = UnitConverter.timerToSeconds(exerciseState.restTime.toString(), timerUnit)
                                    workoutService?.startRestTimer(seconds)
                                    activeRestTimerExerciseId = exerciseState.exercise.id
                                } else {
                                    if (activeRestTimerExerciseId == exerciseState.exercise.id) {
                                        workoutService?.stopRestTimer()
                                    }
                                }
                            },
                            onRestTimeChange = { newTime ->
                                if (isRestTimerActive && activeRestTimerExerciseId == exerciseState.exercise.id) {
                                    val seconds = UnitConverter.timerToSeconds(newTime.toString(), timerUnit)
                                    workoutService?.startRestTimer(seconds)
                                }
                            },
                            onChangeExercise = {
                                exerciseIndexToChange = index
                                showAddExerciseDialog = true
                            }
                        )
                    }
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
            onDismiss = { 
                showAddExerciseDialog = false
                exerciseIndexToChange = null
            },
            onSelect = { exercise ->
                val restInUnit = UnitConverter.formatTimer(2, timerUnit).toIntOrNull() ?: 2
                val indexToChange = exerciseIndexToChange
                if (indexToChange != null) {
                    val currentEx = exercises[indexToChange]
                    exercises[indexToChange] = ExerciseState(
                        exercise = exercise,
                        restTime = currentEx.restTime,
                        sets = currentEx.sets,
                        inputType = currentEx.inputType
                    ).apply {
                        restTimeInput = currentEx.restTimeInput
                    }
                } else {
                    exercises.add(ExerciseState(exercise, restInUnit, mutableStateListOf(SetState())))
                }
                showAddExerciseDialog = false
                exerciseIndexToChange = null
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
                            onClick = { 
                                val stopIntent = Intent(context, WorkoutService::class.java).apply {
                                    action = WorkoutService.ACTION_STOP_WORKOUT
                                }
                                context.startService(stopIntent)
                                onNavigateBack() 
                            },
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
    onRestTimeChange: (Int) -> Unit,
    onChangeExercise: () -> Unit
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
                        value = exerciseState.restTimeInput,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() }) {
                                exerciseState.restTimeInput = newValue
                                if (newValue.isNotEmpty()) {
                                    val newTime = newValue.toInt()
                                    exerciseState.restTime = newTime
                                    onRestTimeChange(newTime)
                                }
                            }
                        },
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .width(30.dp)
                            .onFocusChanged { state ->
                                if (!state.isFocused && exerciseState.restTimeInput.isEmpty()) {
                                    exerciseState.restTimeInput = exerciseState.restTime.toString()
                                }
                            }
                    )
                    Text(
                        text = if (timerUnit == "MINUTES") " min rest" else " sec rest",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onChangeExercise) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Change Exercise",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
        }

        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("SET", modifier = Modifier.width(40.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            
            if (exerciseState.exercise.equipment != Exercise.Equipment.BODYWEIGHT) {
                Text(weightUnit, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }

            // Reps/Time Header with Dropdown
            var showInputTypeMenu by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showInputTypeMenu = true }
                ) {
                    Text(
                        text = if (exerciseState.inputType == WorkoutSet.InputType.REPS) "REPS" else "TIME (S)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                DropdownMenu(
                    expanded = showInputTypeMenu,
                    onDismissRequest = { showInputTypeMenu = false },
                    modifier = Modifier.background(Color(0xFF1C1C1E))
                ) {
                    DropdownMenuItem(
                        text = { Text("Reps", color = Color.White) },
                        onClick = {
                            exerciseState.inputType = WorkoutSet.InputType.REPS
                            showInputTypeMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Time (secs)", color = Color.White) },
                        onClick = {
                            exerciseState.inputType = WorkoutSet.InputType.TIME
                            showInputTypeMenu = false
                        }
                    )
                }
            }

            if (exerciseState.inputType == WorkoutSet.InputType.REPS) {
                Text("RIR", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
            Spacer(modifier = Modifier.width(40.dp))
        }

        // Table Rows
        exerciseState.sets.forEachIndexed { index, setState ->
            SetRow(
                index = index + 1,
                setState = setState,
                onToggleComplete = onToggleSet,
                inputType = exerciseState.inputType,
                isBodyweight = exerciseState.exercise.equipment == Exercise.Equipment.BODYWEIGHT
            )
        }

        // Add Set Button and Re-ordering
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                IconButton(onClick = onMoveUp) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onMoveDown) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            Button(
                onClick = { exerciseState.sets.add(SetState()) },
                modifier = Modifier.weight(1f).padding(start = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                Text("+ Add Set", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondary)
            }
        }
    }
}

@Composable
fun SetRow(
    index: Int,
    setState: SetState,
    onToggleComplete: (Boolean) -> Unit,
    inputType: WorkoutSet.InputType = WorkoutSet.InputType.REPS,
    isBodyweight: Boolean = false
) {
    val backgroundColor = if (setState.isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Set Number / Type
        var showSetTypeMenu by remember { mutableStateOf(false) }
        Box(modifier = Modifier.width(40.dp), contentAlignment = Alignment.Center) {
            Text(
                text = if (setState.type == WorkoutSet.SetType.NORMAL) index.toString() else setState.type.shortLabel,
                modifier = Modifier.fillMaxWidth().clickable { showSetTypeMenu = true },
                style = MaterialTheme.typography.bodyMedium,
                color = when (setState.type) {
                    WorkoutSet.SetType.WARMUP -> Color.Yellow
                    WorkoutSet.SetType.FAILURE -> Color.Red
                    WorkoutSet.SetType.DROP_SET -> Color.Cyan
                    else -> MaterialTheme.colorScheme.onBackground
                },
                textAlign = TextAlign.Center,
                fontWeight = if (setState.type == WorkoutSet.SetType.NORMAL) FontWeight.Normal else FontWeight.Bold
            )
            DropdownMenu(
                expanded = showSetTypeMenu,
                onDismissRequest = { showSetTypeMenu = false },
                modifier = Modifier.background(Color(0xFF1C1C1E))
            ) {
                WorkoutSet.SetType.values().forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.label, color = Color.White) },
                        onClick = {
                            setState.type = type
                            showSetTypeMenu = false
                        }
                    )
                }
            }
        }

        // Weight Input
        if (!isBodyweight) {
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
        }

        // Reps/Time Input
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
        if (inputType == WorkoutSet.InputType.REPS) {
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
                            val rirNum = newValue.toIntOrNull()
                            if (rirNum != null) {
                                if (rirNum == 0) {
                                    setState.type = WorkoutSet.SetType.FAILURE
                                } else if (rirNum > 2) {
                                    setState.type = WorkoutSet.SetType.WARMUP
                                }
                            }
                        }
                    },
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSecondary, textAlign = TextAlign.Center, fontSize = 16.sp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            // Spacer to keep the alignment if needed, but the requirement says "disappear"
            // If it disappears, the Reps/Time box will take more space or we just leave it.
            // Actually, if it disappears, the layout might shift.
            // Let's see if we should use a Spacer or just let weights take it.
            // The Box(modifier = Modifier.weight(1f)) for Reps and Weight and RIR already manages it.
            // If RIR is gone, Reps and Weight (if present) will share the space.
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
