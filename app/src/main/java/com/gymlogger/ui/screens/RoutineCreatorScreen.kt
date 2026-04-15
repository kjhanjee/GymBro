package com.gymlogger.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.gymlogger.data.ExerciseRepository
import com.gymlogger.data.RoutineRepository
import com.gymlogger.data.SettingsRepository
import com.gymlogger.model.Exercise
import com.gymlogger.model.Routine
import com.gymlogger.model.WorkoutSet
import com.gymlogger.ui.components.GymBroTopAppBar
import com.gymlogger.ui.components.ExerciseSelectionDialog
import com.gymlogger.model.MuscleGroup
import com.gymlogger.util.UnitConverter
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val weightUnit by SettingsRepository.getWeightUnit(context).collectAsState(initial = SettingsRepository.WeightUnit.LBS)
    val timerUnit by SettingsRepository.getTimerUnit(context).collectAsState(initial = SettingsRepository.TimerUnit.MINUTES)

    // Load routine data if editing
    LaunchedEffect(routineId) {
        if (routineId != null) {
            val routine = RoutineRepository.getRoutineById(routineId)
            if (routine != null) {
                routineName = routine.name
                routineDescription = routine.description ?: ""
                selectedExercises = routine.exercises.map { ex ->
                    ex.copy(sets = ex.sets.map { set ->
                        set.copy(
                            targetWeight = UnitConverter.formatWeight(set.targetWeight, weightUnit).toFloatOrNull(),
                            restTime = UnitConverter.formatTimer(set.restTime, timerUnit).toIntOrNull() ?: 2
                        )
                    })
                }
            }
        }
    }

    val focusManager = LocalFocusManager.current

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
                                            targetWeight = UnitConverter.weightToBase(set.targetWeight?.toString() ?: "", weightUnit),
                                            restTime = UnitConverter.timerToBase(set.restTime.toString(), timerUnit)
                                        )
                                    }
                                )
                            }
                        )
                        if (routineId == null) {
                            RoutineRepository.createRoutine(context, routine)
                        } else {
                            RoutineRepository.updateRoutine(context, routine)
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
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                itemsIndexed(selectedExercises) { index, exercise ->
                    ExerciseCardItem(
                        routineExercise = exercise,
                        weightUnit = weightUnit.name,
                        timerUnit = timerUnit.name,
                        onRemove = {
                            selectedExercises = selectedExercises.toMutableList().apply { removeAt(index) }
                        },
                        onUpdateSets = { newSets ->
                            selectedExercises = selectedExercises.toMutableList().apply {
                                this[index] = exercise.copy(sets = newSets)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showingAddExerciseDialog) {
        ExerciseSelectionDialog(
            onDismiss = { showingAddExerciseDialog = false },
            onSelect = { exercise ->
                val restInUnit = UnitConverter.formatTimer(2, timerUnit).toIntOrNull() ?: 2
                selectedExercises = selectedExercises + Routine.RoutineExercise(
                    id = System.currentTimeMillis(),
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
                            restTime = restInUnit
                        )
                    )
                )
                showingAddExerciseDialog = false
            }
        )
    }
}

@Composable
fun ExerciseCardItem(
    routineExercise: Routine.RoutineExercise,
    weightUnit: String,
    timerUnit: String,
    onRemove: () -> Unit,
    onUpdateSets: (List<Routine.RoutineExercise.SetConfig>) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

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
                        val equipmentName = routineExercise.exerciseName.let { name ->
                            // If we don't have the full exercise object, we might need to find it
                            // but for simplicity, let's assume we can get it from the repository or it's already in the name
                            // Actually, let's just use the name if it's already formatted, 
                            // but the requirement says "when it is added as a card".
                            // For RoutineCreator, the exerciseName is stored in RoutineExercise.
                            // We should probably lookup the equipment if it's not already in the name.
                            ExerciseRepository.getExerciseById(routineExercise.exerciseId)?.equipment?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: ""
                        }
                        Text(
                            text = if (equipmentName.isNotEmpty()) "${routineExercise.exerciseName} ($equipmentName)" else routineExercise.exerciseName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                            BasicTextField(
                                value = routineExercise.sets.firstOrNull()?.restTime?.toString() ?: "2",
                                onValueChange = { newValue ->
                                    val newRest = newValue.toIntOrNull() ?: 0
                                    val newSets = routineExercise.sets.map { it.copy(restTime = newRest) }
                                    onUpdateSets(newSets)
                                },
                                textStyle = TextStyle(color = Color(0xFF8E8E93), fontSize = 13.sp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier.width(30.dp)
                            )
                            Text(
                                text = if (timerUnit == "MINUTES") " min rest" else " sec rest",
                                fontSize = 13.sp,
                                color = Color(0xFF8E8E93)
                            )
                        }
                    }
                }
                Row {
                    IconButton(onClick = onRemove) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove",
                            tint = Color(0xFFFF3B30)
                        )
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Color(0xFF8E8E93),
                        modifier = Modifier.padding(12.dp)
                    )
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Set", modifier = Modifier.width(30.dp), color = Color(0xFF8E8E93), fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text(weightUnit, modifier = Modifier.weight(1f), color = Color(0xFF8E8E93), fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("Reps", modifier = Modifier.weight(1f), color = Color(0xFF8E8E93), fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("RIR", modifier = Modifier.weight(1f), color = Color(0xFF8E8E93), fontSize = 12.sp, textAlign = TextAlign.Center)
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
                            Text(
                                text = (index + 1).toString(),
                                modifier = Modifier.width(30.dp),
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            
                            // Weight Input
                            var weightText by remember(setConfig.targetWeight) { 
                                // In the creator/editor, targetWeight is already in the current unit
                                // We use LBS here to skip the KG/LBS conversion math in formatWeight
                                val formatted = UnitConverter.formatWeight(setConfig.targetWeight, SettingsRepository.WeightUnit.LBS)
                                mutableStateOf(formatted) 
                            }
                            SmallTextField(
                                value = weightText,
                                onValueChange = { newValue ->
                                    if (newValue.isEmpty() || newValue.all { it.isDigit() || it == '.' }) {
                                        weightText = newValue
                                        if (newValue.isNotEmpty() && newValue != ".") {
                                            val weight = newValue.toFloatOrNull()
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

                            // Reps Input
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
                                            newSets[index] = setConfig.copy(targetRir = rir)
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

                    Button(
                        onClick = {
                            val lastSet = routineExercise.sets.lastOrNull()
                            val newSet = Routine.RoutineExercise.SetConfig(
                                id = (routineExercise.sets.size + 1).toLong(),
                                type = WorkoutSet.SetType.NORMAL,
                                targetReps = lastSet?.targetReps ?: 10,
                                targetWeight = lastSet?.targetWeight ?: 0f,
                                targetRir = lastSet?.targetRir ?: 2,
                                restTime = lastSet?.restTime ?: 2
                            )
                            onUpdateSets(routineExercise.sets + newSet)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
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

