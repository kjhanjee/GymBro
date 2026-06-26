package com.gymlogger.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymlogger.model.Routine
import com.gymlogger.ui.components.GymBroTopAppBar
import com.gymlogger.ui.theme.GymBroTheme
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    GymBroTheme {
        HomeScreen(
            onNavigateToCreateRoutine = {},
            onNavigateToTrackWorkout = {},
            onEditRoutine = {},
            onNavigateToStatistics = {},
            onNavigateToRecentWorkouts = {},
            onNavigateToExercises = {},
            onNavigateToSettings = {},
            onNavigateToMealLogger = {}
        )
    }
}

@Composable
fun HomeScreen(
    onNavigateToCreateRoutine: () -> Unit,
    onNavigateToTrackWorkout: (Long?) -> Unit,
    onEditRoutine: (Long) -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToRecentWorkouts: () -> Unit,
    onNavigateToExercises: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToMealLogger: () -> Unit
) {
    val routines by com.gymlogger.data.RoutineRepository.getRoutines().collectAsStateWithLifecycle(initialValue = emptyList())
    val workouts by com.gymlogger.data.RoutineRepository.completedWorkouts.collectAsStateWithLifecycle(initialValue = emptyList())
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    var routineToDelete by remember { mutableStateOf<Long?>(null) }

    if (routineToDelete != null) {
        AlertDialog(
            onDismissRequest = { routineToDelete = null },
            title = { Text("Delete Routine?") },
            text = { Text("Are you sure you want to delete this routine? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        routineToDelete?.let { id ->
                            coroutineScope.launch {
                                com.gymlogger.data.RoutineRepository.deleteRoutine(context, id)
                            }
                        }
                        routineToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { routineToDelete = null }) {
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
                title = "Home",
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                // Header section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "Hello, Bro!",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ready to crush your workouts today?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8E8E93)
                    )
                }
            }

            item {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val actions = listOf(
                        QuickActionItem("Start Workout", Icons.Default.PlayArrow) { onNavigateToTrackWorkout(null) },
                        QuickActionItem("Create Routine", Icons.Default.Add, onNavigateToCreateRoutine),
                        QuickActionItem("Statistics", Icons.Default.BarChart, onNavigateToStatistics),
                        QuickActionItem("Meal Logger", Icons.Default.Restaurant, onNavigateToMealLogger),
                        QuickActionItem("Recent Workouts", Icons.Default.History, onNavigateToRecentWorkouts),
                        QuickActionItem("Exercise Library", Icons.Default.Folder, onNavigateToExercises)
                    )

                    actions.chunked(2).forEach { rowActions ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowActions.forEach { action ->
                                QuickActionCard(action, modifier = Modifier.weight(1f))
                            }
                            if (rowActions.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Your Routines",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    routines.forEach { routine ->
                        val lastWorkout = workouts.filter { it.routine.id == routine.id }
                            .maxByOrNull { it.date }
                        
                        val lastCompleted = if (lastWorkout != null) {
                            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
                            val calendar = Calendar.getInstance()
                            val today = calendar.get(Calendar.DAY_OF_YEAR)
                            val thisYear = calendar.get(Calendar.YEAR)
                            
                            calendar.timeInMillis = lastWorkout.date
                            val workoutDay = calendar.get(Calendar.DAY_OF_YEAR)
                            val workoutYear = calendar.get(Calendar.YEAR)
                            
                            if (today == workoutDay && thisYear == workoutYear) {
                                "Today"
                            } else {
                                sdf.format(Date(lastWorkout.date))
                            }
                        } else "Never"

                        HomeRoutineCard(
                            id = routine.id,
                            name = routine.name,
                            lastCompleted = lastCompleted,
                            exercisesCount = routine.exercises.size,
                            onStartWorkout = { onNavigateToTrackWorkout(routine.id) },
                            onEditRoutine = { onEditRoutine(routine.id) },
                            onDeleteRoutine = {
                                routineToDelete = routine.id
                            },
                            onShareRoutine = {
                                shareRoutine(context, routine)
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun shareRoutine(context: Context, routine: Routine) {
    try {
        val json = Json { prettyPrint = true }.encodeToString(routine)
        val fileName = "${routine.name.replace(" ", "_")}_routine.json"
        
        val cachePath = File(context.cacheDir, "shared_routines")
        cachePath.mkdirs()
        
        val file = File(cachePath, fileName)
        file.writeText(json)
        
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "Sharing Routine: ${routine.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(intent, "Share Routine via"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

data class QuickActionItem(
    val text: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun QuickActionCard(item: QuickActionItem, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .height(84.dp)
            .clickable { item.onClick() },
        color = Color(0xFF1C1C1E),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = item.text,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun HomeRoutineCard(
    id: Long,
    name: String,
    lastCompleted: String,
    exercisesCount: Int,
    onStartWorkout: () -> Unit,
    onEditRoutine: () -> Unit,
    onDeleteRoutine: () -> Unit,
    onShareRoutine: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 110.dp)
                .clickable(onClick = { expanded = true }),
            color = Color(0xFF1C1C1E),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontSize = 19.sp,
                        lineHeight = 24.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$exercisesCount exercises",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8E8E93)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = lastCompleted,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8E8E93).copy(alpha = 0.7f)
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = Color(0xFF48484A)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF2C2C2E))
        ) {
            DropdownMenuItem(
                text = { Text("Start Logging", color = Color.White) },
                onClick = {
                    expanded = false
                    onStartWorkout()
                },
                leadingIcon = {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            )
            DropdownMenuItem(
                text = { Text("Edit Routine", color = Color.White) },
                onClick = {
                    expanded = false
                    onEditRoutine()
                },
                leadingIcon = {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White)
                }
            )
            DropdownMenuItem(
                text = { Text("Share Routine", color = Color.White) },
                onClick = {
                    expanded = false
                    onShareRoutine()
                },
                leadingIcon = {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                }
            )
            DropdownMenuItem(
                text = { Text("Delete Routine", color = Color.Red) },
                onClick = {
                    expanded = false
                    onDeleteRoutine()
                },
                leadingIcon = {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                }
            )
        }
    }
}
