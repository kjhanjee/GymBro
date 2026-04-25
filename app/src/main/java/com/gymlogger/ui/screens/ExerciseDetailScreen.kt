package com.gymlogger.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.gymlogger.data.ExerciseRepository
import com.gymlogger.data.RoutineRepository
import com.gymlogger.data.SettingsRepository
import com.gymlogger.model.Exercise
import com.gymlogger.ui.components.GymBroTopAppBar
import com.gymlogger.model.Routine
import com.gymlogger.util.UnitConverter
import kotlinx.coroutines.launch

@Composable
fun ExerciseDetailScreen(
    exerciseId: Long,
    onNavigateBack: () -> Unit
) {
    var exercise by remember { mutableStateOf<Exercise?>(null) }
    var showRoutineDialog by remember { mutableStateOf(false) }

    LaunchedEffect(exerciseId) {
        exercise = ExerciseRepository.getExerciseById(exerciseId)
    }

    if (exercise == null) return

    val exerciseData = exercise!!

    val context = androidx.compose.ui.platform.LocalContext.current
    val weightUnit by SettingsRepository.getWeightUnit(context).collectAsState(initial = SettingsRepository.WeightUnit.LBS)
    
    val completedWorkouts by RoutineRepository.getCompletedWorkouts().collectAsState(initial = emptyList())
    
    // Process exercise-specific data
    val exerciseSets = remember(completedWorkouts, exerciseId) {
        completedWorkouts.flatMap { workout ->
            workout.sets.filter { it.exerciseId == exerciseId && it.isCompleted }
                .map { it to workout.date }
        }.sortedBy { it.second }
    }

    val weightDataPoints = remember(exerciseSets, weightUnit) {
        exerciseSets.mapNotNull { (set, _) -> set.weight }
    }

    val activeDaysOfWeek = remember(exerciseSets) {
        exerciseSets.map { (_, date) ->
            val calendar = java.util.Calendar.getInstance()
            calendar.timeInMillis = date
            // Calendar.DAY_OF_WEEK: 1=Sun, 2=Mon, ..., 7=Sat
            // We want 0=Mon, 1=Tue, ..., 6=Sun
            val day = calendar.get(java.util.Calendar.DAY_OF_WEEK)
            if (day == java.util.Calendar.SUNDAY) 6 else day - 2
        }.distinct()
    }

    Scaffold(
        topBar = {
            GymBroTopAppBar(
                title = exerciseData.name,
                onNavigateBack = onNavigateBack
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(16.dp)
            ) {
                Button(
                    onClick = { showRoutineDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Add to Routine", fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (weightDataPoints.isNotEmpty()) {
                // Trend Chart Section
                Text(
                    "Weight Trend (${weightUnit.name})",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                WeightTrendChart(weightDataPoints, weightUnit)

                Spacer(modifier = Modifier.height(24.dp))
            }

            if (activeDaysOfWeek.isNotEmpty()) {
                // Frequency Section
                Text(
                    "Training Frequency",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                DayFrequencyView(activeDaysOfWeek)

                Spacer(modifier = Modifier.height(24.dp))
            }

            // How-to Section
            Text(
                "How-to Instruction",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            InstructionSection(exerciseData.formattedInstructions)

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    val hue by SettingsRepository.activeThemeHue.collectAsState()
    if (showRoutineDialog) {
        com.gymlogger.ui.theme.GymBroTheme(hue = hue) {
            AddToRoutineDialog(
                exercise = exerciseData,
                onDismiss = { showRoutineDialog = false }
            )
        }
    }
}

@Composable
fun WeightTrendChart(dataPoints: List<Float>, weightUnit: SettingsRepository.WeightUnit = SettingsRepository.WeightUnit.LBS) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val maxVal = dataPoints.maxOrNull() ?: 1f
            val minVal = dataPoints.minOrNull() ?: 0f
            val range = (maxVal - minVal).coerceAtLeast(1f)
            
            val path = Path()
            dataPoints.forEachIndexed { index, value ->
                val x = if (dataPoints.size > 1) index * (width / (dataPoints.size - 1)) else width / 2
                val y = height - ((value - minVal) / range * height)
                
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                
                drawCircle(
                    color = primaryColor,
                    radius = 4.dp.toPx(),
                    center = Offset(x, y)
                )
            }
            
            if (dataPoints.size > 1) {
                drawPath(
                    path = path,
                    color = primaryColor,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun DayFrequencyView(activeDays: List<Int>) {
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    val primaryColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.secondary

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEachIndexed { index, day ->
            val isActive = activeDays.contains(index)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isActive) primaryColor else Color(0xFF1C1C1E)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        day,
                        color = if (isActive) Color.White else Color(0xFF8E8E93),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun InstructionSection(steps: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        steps.forEachIndexed { index, step ->
            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    "${index + 1}.",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(28.dp)
                )
                Text(
                    step,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToRoutineDialog(
    exercise: Exercise,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val routines = RoutineRepository.getRoutines().collectAsState(initial = emptyList())
    val filteredRoutines = routines.value.filter { it.name.contains(searchQuery, ignoreCase = true) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Add to Routine",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search routines...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.secondary,
                        unfocusedContainerColor = MaterialTheme.colorScheme.secondary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSecondary,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredRoutines) { routine ->
                        ListItem(
                            headlineContent = { Text(routine.name, color = MaterialTheme.colorScheme.onSurface) },
                            modifier = Modifier.clickable {
                                // Logic to add exercise to routine would go here
                                onDismiss()
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}
