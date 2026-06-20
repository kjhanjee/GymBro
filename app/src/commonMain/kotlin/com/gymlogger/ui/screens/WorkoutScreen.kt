package com.gymlogger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymlogger.model.Routine
import com.gymlogger.data.RoutineRepository
import com.gymlogger.ui.components.GymBroTopAppBar

@Composable
fun WorkoutScreen(
    onNavigateToCreateRoutine: () -> Unit,
    onNavigateToTrackWorkout: (Long) -> Unit,
    onNavigateToExercises: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val routines by RoutineRepository.getRoutines().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            GymBroTopAppBar(
                title = "Workout",
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                // Start Empty Workout Button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clickable { onNavigateToTrackWorkout(-1L) },
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1C1C1E)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Start Empty Workout",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Routines",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Row {
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Folder", tint = Color.White)
                        }
                        IconButton(onClick = onNavigateToCreateRoutine) {
                            Icon(Icons.Default.Add, contentDescription = "Add Routine", tint = Color.White)
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RoutineActionButton(
                        icon = Icons.AutoMirrored.Filled.Assignment,
                        text = "New Routine",
                        onClick = onNavigateToCreateRoutine,
                        modifier = Modifier.weight(1f)
                    )
                    RoutineActionButton(
                        icon = Icons.Default.Search,
                        text = "Explore",
                        onClick = onNavigateToExercises,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF8E8E93))
                    Text(
                        "My Routines (${routines.size})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8E8E93)
                    )
                }
            }

            items(routines) { routine ->
                WorkoutRoutineCard(routine = routine, onStart = { onNavigateToTrackWorkout(routine.id) })
            }
        }
    }
}

@Composable
fun RoutineActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1C1C1E)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = Color.White)
            Spacer(Modifier.width(10.dp))
            Text(
                text,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White
            )
        }
    }
}

@Composable
fun WorkoutRoutineCard(routine: Routine, onStart: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1C1C1E),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    routine.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontSize = 18.sp
                )
                Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = Color(0xFF8E8E93))
            }
            Spacer(Modifier.height(4.dp))
            Text(
                routine.exercises.joinToString(", ") { it.exerciseName },
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF8E8E93),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(16.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clickable { onStart() },
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "Start Routine",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Black,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
