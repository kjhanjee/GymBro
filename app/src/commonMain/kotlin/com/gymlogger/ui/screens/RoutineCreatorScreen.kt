package com.gymlogger.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.gymlogger.data.ExerciseRepository
import com.gymlogger.data.RoutineRepository
import com.gymlogger.data.SettingsRepository
import com.gymlogger.model.Exercise
import com.gymlogger.model.Routine
import com.gymlogger.model.WorkoutSet
import com.gymlogger.ui.components.GymBroTopAppBar
import com.gymlogger.ui.components.ExerciseSelectionDialog

import com.gymlogger.util.ToastManager
import kotlinx.serialization.json.Json
import com.gymlogger.util.UnitConverter

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RoutineCreatorScreen(
    routineId: Long? = null,
    onNavigateBack: () -> Unit
) {
    var selectedExercises by remember { mutableStateOf(emptyList<Routine.RoutineExercise>()) }
    var routineName by remember { mutableStateOf("") }
    var routineDescription by remember { mutableStateOf("") }
    var showingAddExerciseDialog by remember { mutableStateOf(false) }
    var exerciseIndexToChange by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    val weightUnit by SettingsRepository.getWeightUnit().collectAsState(initial = SettingsRepository.WeightUnit.LBS)
    val timerUnit by SettingsRepository.getTimerUnit().collectAsState(initial = SettingsRepository.TimerUnit.MINUTES)
    val json = remember { Json { ignoreUnknownKeys = true } }
    val launchImport = com.gymlogger.util.rememberJsonImportLauncher { jsonString ->
        if (jsonString != null) {
            try {
                val importedRoutine = json.decodeFromString<Routine>(jsonString)
                routineName = importedRoutine.name
                routineDescription = importedRoutine.description ?: ""
                selectedExercises = importedRoutine.exercises.mapIndexed { index, ex ->
                    // Match exercise by name to find local ID
                    val localExercise = ExerciseRepository.exerciseDatabase.find { 
                        it.name.equals(ex.exerciseName, ignoreCase = true) 
                    }
                    
                    ex.copy(
                        id = com.gymlogger.util.getCurrentTimeMillis() + index,
                        exerciseId = localExercise?.id ?: ex.exerciseId, // Fallback to source ID if not found, but ideally we'd handle "not found"
                        sets = ex.sets.mapIndexed { setIndex, set ->
                            set.copy(id = (setIndex + 1).toLong())
                        }
                    )
                }
            } catch (e: Exception) {
                ToastManager.showToast("JSON format invalid")
            }
        }
    }

    // Load routine data if editing
    LaunchedEffect(routineId) {
        if (routineId != null) {
            val routine = RoutineRepository.getRoutineById(routineId)
            if (routine != null) {
                routineName = routine.name
                routineDescription = routine.description ?: ""
                selectedExercises = routine.exercises
            }
        }
    }

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
            
            val currentDraggedIndex = selectedExercises.indexOfFirst { it.id == activeDraggingItemId }
            if (currentDraggedIndex == -1) break
            
            val draggedItem = layoutInfo.visibleItemsInfo.find { it.index == currentDraggedIndex + 3 }
            
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
        if (to !in selectedExercises.indices) return
        val mutable = selectedExercises.toMutableList()
        val item = mutable.removeAt(from)
        mutable.add(to, item)
        selectedExercises = mutable
    }

    Scaffold(
        topBar = {
            GymBroTopAppBar(
                title = if (routineId == null) "Create Routine" else "Edit Routine",
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = Color.Black,
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = {
                focusManager.clearFocus()
            })
        },
        bottomBar = {
            Button(
                onClick = {
                    scope.launch {
                        val routine = Routine(
                            id = routineId ?: 0L,
                            name = routineName,
                            description = routineDescription,
                            exercises = selectedExercises.mapIndexed { index, ex -> 
                                ex.copy(
                                    order = index,
                                    sets = ex.sets.map { set ->
                                        set.copy(
                                            targetWeight = set.targetWeight,
                                            restTime = set.restTime
                                        )
                                    }
                                )
                            }
                        )
                        if (routineId == null) {
                            RoutineRepository.createRoutine(routine)
                        } else {
                            RoutineRepository.updateRoutine(routine)
                        }
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                enabled = routineName.isNotBlank() && selectedExercises.isNotEmpty()
            ) {
                Text(
                    text = if (routineId == null) "Save Routine" else "Update Routine",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { launchImport() },
                    color = Color(0xFF1C1C1E),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Import from JSON",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }

            item {
                // Routine info card
                Surface(
                    color = Color(0xFF1C1C1E),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        TextField(
                            value = routineName,
                            onValueChange = { routineName = it },
                            label = { Text("Routine Name", color = Color(0xFF8E8E93)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        TextField(
                            value = routineDescription,
                            onValueChange = { routineDescription = it },
                            label = { Text("Description (optional)", color = Color(0xFF8E8E93)) },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }

            item {
                // Add exercise button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showingAddExerciseDialog = true },
                    color = Color(0xFF1C1C1E),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Add Exercises",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Selected Exercises",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (selectedExercises.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = null,
                            tint = Color(0xFF48484A),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No exercises selected",
                            fontSize = 16.sp,
                            color = Color(0xFF8E8E93)
                        )
                    }
                }
            } else {
                itemsIndexed(selectedExercises, key = { _, exercise -> exercise.id }) { index, exercise ->
                    val isDragging = activeDraggingItemId == exercise.id
                    val isDropping = activeDroppingItemId == exercise.id
                    
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
                            .pointerInput(exercise.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { 
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        activeDraggingItemId = exercise.id
                                        draggingOffset = Offset.Zero
                                    },
                                    onDragEnd = {
                                        val finalOffset = draggingOffset
                                        val droppedId = exercise.id
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
                                        val droppedId = exercise.id
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
                                        
                                        val currentDraggedIndex = selectedExercises.indexOfFirst { it.id == exercise.id }
                                        if (currentDraggedIndex == -1) return@detectDragGesturesAfterLongPress
                                        val absoluteIndex = currentDraggedIndex + 3
                                        
                                        val visibleItems = listState.layoutInfo.visibleItemsInfo
                                        val draggedItem = visibleItems.find { it.index == absoluteIndex } ?: return@detectDragGesturesAfterLongPress
                                        
                                        val spacing = with(density) { 16.dp.toPx() }
                                        
                                        if (draggingOffset.y > (draggedItem.size + spacing) * 0.5f && currentDraggedIndex < selectedExercises.size - 1) {
                                            val nextItem = visibleItems.find { it.index == absoluteIndex + 1 }
                                            val nextSize = nextItem?.size ?: draggedItem.size
                                            moveExercise(currentDraggedIndex, currentDraggedIndex + 1)
                                            draggingOffset = draggingOffset.copy(y = draggingOffset.y - (nextSize + spacing))
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        } else if (draggingOffset.y < -(draggedItem.size + spacing) * 0.5f && currentDraggedIndex > 0) {
                                            val prevItem = visibleItems.find { it.index == absoluteIndex - 1 }
                                            val prevSize = prevItem?.size ?: draggedItem.size
                                            moveExercise(currentDraggedIndex, currentDraggedIndex - 1)
                                            draggingOffset = draggingOffset.copy(y = draggingOffset.y + (prevSize + spacing))
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                )
                            }
                    ) {
                        ExerciseCardItem(
                            routineExercise = exercise,
                            weightUnit = weightUnit,
                            timerUnit = timerUnit,
                            onRemove = {
                                selectedExercises = selectedExercises.toMutableList().apply { removeAt(index) }
                            },
                            onUpdateSets = { newSets ->
                                selectedExercises = selectedExercises.toMutableList().apply {
                                    this[index] = exercise.copy(sets = newSets)
                                }
                            },
                            onUpdateInputType = { newInputType ->
                                selectedExercises = selectedExercises.toMutableList().apply {
                                    this[index] = exercise.copy(inputType = newInputType)
                                }
                            },
                            onMoveUp = {
                                if (index > 0) {
                                    selectedExercises = selectedExercises.toMutableList().apply {
                                        val item = removeAt(index)
                                        add(index - 1, item)
                                    }
                                }
                            },
                            onMoveDown = {
                                if (index < selectedExercises.size - 1) {
                                    selectedExercises = selectedExercises.toMutableList().apply {
                                        val item = removeAt(index)
                                        add(index + 1, item)
                                    }
                                }
                            },
                            onChangeExercise = {
                                exerciseIndexToChange = index
                            }
                        )
                    }
                }
            }
        }
    }

    if (showingAddExerciseDialog || exerciseIndexToChange != null) {
        ExerciseSelectionDialog(
            onDismiss = { 
                showingAddExerciseDialog = false
                exerciseIndexToChange = null
            },
            onSelect = { exercise ->
                if (exerciseIndexToChange != null) {
                    val index = exerciseIndexToChange!!
                    selectedExercises = selectedExercises.toMutableList().apply {
                        this[index] = this[index].copy(
                            exerciseId = exercise.id,
                            exerciseName = exercise.name
                        )
                    }
                    exerciseIndexToChange = null
                } else {
                    selectedExercises = selectedExercises + Routine.RoutineExercise(
                        id = com.gymlogger.util.getCurrentTimeMillis(),
                        exerciseId = exercise.id,
                        exerciseName = exercise.name,
                        order = selectedExercises.size,
                        sets = listOf(
                            Routine.RoutineExercise.SetConfig(
                                id = 1,
                                type = WorkoutSet.SetType.NORMAL,
                                targetReps = 10,
                                targetWeight = 0f,
                                targetRir = 2,
                                restTime = 120
                            )
                        )
                    )
                    showingAddExerciseDialog = false
                }
            }
        )
    }
}

@Composable
fun ExerciseCardItem(
    routineExercise: Routine.RoutineExercise,
    weightUnit: SettingsRepository.WeightUnit,
    timerUnit: SettingsRepository.TimerUnit,
    onRemove: () -> Unit,
    onUpdateSets: (List<Routine.RoutineExercise.SetConfig>) -> Unit,
    onUpdateInputType: (WorkoutSet.InputType) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onChangeExercise: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var exercise by remember { mutableStateOf<Exercise?>(null) }

    LaunchedEffect(routineExercise.exerciseId) {
        exercise = ExerciseRepository.getExerciseById(routineExercise.exerciseId)
    }

    val isBodyweight = exercise?.equipment == Exercise.Equipment.BODYWEIGHT

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        color = Color(0xFF1C1C1E),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF2C2C2E), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        val equipmentName = exercise?.equipment?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: ""
                        Text(
                            text = if (equipmentName.isNotEmpty()) "${routineExercise.exerciseName} ($equipmentName)" else routineExercise.exerciseName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = Color(0xFF8E8E93),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            var restTimeText by remember(routineExercise.sets.firstOrNull()?.restTime, timerUnit) {
                                val rest = routineExercise.sets.firstOrNull()?.restTime ?: 120
                                val unit = timerUnit
                                mutableStateOf(UnitConverter.formatTimer(rest, unit))
                            }
                            BasicTextField(
                                value = restTimeText,
                                onValueChange = { newValue ->
                                    if (newValue.all { it.isDigit() }) {
                                    restTimeText = newValue
                                    if (newValue.isNotEmpty()) {
                                        val unit = timerUnit
                                        val newRest = UnitConverter.timerToBase(newValue, unit)
                                            val newSets = routineExercise.sets.map { it.copy(restTime = newRest) }
                                            onUpdateSets(newSets)
                                        }
                                    }
                                },
                                textStyle = TextStyle(color = Color(0xFF8E8E93), fontSize = 13.sp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .width(30.dp)
                                    .onFocusChanged { state ->
                                        if (!state.isFocused && restTimeText.isEmpty()) {
                                            val rest = routineExercise.sets.firstOrNull()?.restTime ?: 120
                                            val unit = timerUnit
                                            restTimeText = UnitConverter.formatTimer(rest, unit)
                                        }
                                    }
                            )
                            Text(
                                text = if (timerUnit == SettingsRepository.TimerUnit.MINUTES) " min rest" else " sec rest",
                                fontSize = 13.sp,
                                color = Color(0xFF8E8E93)
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onChangeExercise) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Change Exercise",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onRemove) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove",
                            tint = Color(0xFFFF3B30)
                        )
                    }
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = Color(0xFF8E8E93)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                ) {
                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Set", modifier = Modifier.width(30.dp), color = Color(0xFF8E8E93), fontSize = 12.sp, textAlign = TextAlign.Center)
                        
                        if (!isBodyweight) {
                            Text(weightUnit.name.lowercase(), modifier = Modifier.weight(1f), color = Color(0xFF8E8E93), fontSize = 12.sp, textAlign = TextAlign.Center)
                        }

                        // Reps/Time Header with Dropdown
                        var showInputTypeMenu by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { showInputTypeMenu = true }
                            ) {
                                Text(
                                    text = if (routineExercise.inputType == WorkoutSet.InputType.REPS) "Reps" else "Time(s)",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
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
                                        onUpdateInputType(WorkoutSet.InputType.REPS)
                                        showInputTypeMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Time (secs)", color = Color.White) },
                                    onClick = {
                                        onUpdateInputType(WorkoutSet.InputType.TIME)
                                        showInputTypeMenu = false
                                    }
                                )
                            }
                        }

                        if (routineExercise.inputType == WorkoutSet.InputType.REPS) {
                            Text("RIR", modifier = Modifier.weight(1f), color = Color(0xFF8E8E93), fontSize = 12.sp, textAlign = TextAlign.Center)
                        }
                        
                        Spacer(modifier = Modifier.width(40.dp))
                    }

                    routineExercise.sets.forEachIndexed { index, setConfig ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            var showSetTypeMenu by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.width(30.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (setConfig.type == WorkoutSet.SetType.NORMAL) (index + 1).toString() else setConfig.type.shortLabel,
                                    modifier = Modifier.fillMaxWidth().clickable { showSetTypeMenu = true },
                                    color = when (setConfig.type) {
                                        WorkoutSet.SetType.WARMUP -> Color.Yellow
                                        WorkoutSet.SetType.FAILURE -> Color.Red
                                        WorkoutSet.SetType.DROP_SET -> Color.Cyan
                                        else -> Color.White
                                    },
                                    textAlign = TextAlign.Center,
                                    fontWeight = if (setConfig.type == WorkoutSet.SetType.NORMAL) FontWeight.Normal else FontWeight.Bold
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
                                                val newSets = routineExercise.sets.toMutableList()
                                                newSets[index] = setConfig.copy(type = type)
                                                onUpdateSets(newSets)
                                                showSetTypeMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                            
                            // Weight Input
                            if (!isBodyweight) {
                                var weightText by remember(setConfig.targetWeight, weightUnit) { 
                                    val formatted = UnitConverter.formatWeight(setConfig.targetWeight, weightUnit)
                                    mutableStateOf(formatted) 
                                }
                                SmallTextField(
                                    value = weightText,
                                    onValueChange = { newValue ->
                                        if (newValue.isEmpty() || newValue.all { it.isDigit() || it == '.' }) {
                                            weightText = newValue
                                            if (newValue.isNotEmpty() && newValue != ".") {
                                                val weight = UnitConverter.weightToBase(newValue, weightUnit)
                                                val newSets = routineExercise.sets.toMutableList()
                                                newSets[index] = setConfig.copy(targetWeight = weight)
                                                onUpdateSets(newSets)
                                            }
                                        }
                                    },
                                    onFocusChange = { isFocused ->
                                        if (isFocused) {
                                            weightText = ""
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Reps/Time Input
                            var repsText by remember(setConfig.targetReps) { 
                                mutableStateOf(setConfig.targetReps?.toString() ?: "") 
                            }
                            SmallTextField(
                                value = repsText,
                                onValueChange = { newValue ->
                                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                        repsText = newValue
                                        if (newValue.isNotEmpty()) {
                                            val reps = newValue.toIntOrNull()
                                            val newSets = routineExercise.sets.toMutableList()
                                            newSets[index] = setConfig.copy(targetReps = reps)
                                            onUpdateSets(newSets)
                                        }
                                    }
                                },
                                onFocusChange = { isFocused ->
                                    if (isFocused) {
                                        repsText = ""
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )

                            // RIR Input
                            if (routineExercise.inputType == WorkoutSet.InputType.REPS) {
                                var rirText by remember(setConfig.targetRir) { 
                                    mutableStateOf(setConfig.targetRir?.toString() ?: "") 
                                }
                                SmallTextField(
                                    value = rirText,
                                    onValueChange = { newValue ->
                                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                            rirText = newValue
                                            if (newValue.isNotEmpty()) {
                                                val rir = newValue.toIntOrNull()
                                                val newSets = routineExercise.sets.toMutableList()
                                                val updatedType = WorkoutSet.getTypeForRir(rir, setConfig.type)
                                                newSets[index] = setConfig.copy(targetRir = rir, type = updatedType)
                                                onUpdateSets(newSets)
                                            }
                                        }
                                    },
                                    onFocusChange = { isFocused ->
                                        if (isFocused) {
                                            rirText = ""
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            IconButton(
                                onClick = {
                                    val newSets = routineExercise.sets.toMutableList()
                                    newSets.removeAt(index)
                                    onUpdateSets(newSets)
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove Set", tint = Color(0xFF48484A), modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row {
                            IconButton(onClick = onMoveUp) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = Color.White)
                            }
                            IconButton(onClick = onMoveDown) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", tint = Color.White)
                            }
                        }
                        
                        Button(
                            onClick = {
                                val lastSet = routineExercise.sets.lastOrNull()
                                val newSet = Routine.RoutineExercise.SetConfig(
                                    id = (routineExercise.sets.size + 1).toLong(),
                                    type = WorkoutSet.SetType.NORMAL,
                                    targetReps = lastSet?.targetReps ?: 10,
                                    targetWeight = lastSet?.targetWeight ?: 0f,
                                    targetRir = lastSet?.targetRir ?: 2,
                                    restTime = lastSet?.restTime ?: 120
                                )
                                onUpdateSets(routineExercise.sets + newSet)
                            },
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Set", fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmallTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onFocusChange: (Boolean) -> Unit = {}
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .height(48.dp)
            .onFocusChanged { state -> onFocusChange(state.isFocused) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF2C2C2E),
            unfocusedContainerColor = Color(0xFF2C2C2E),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        shape = RoundedCornerShape(8.dp),
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 14.sp)
    )
}
