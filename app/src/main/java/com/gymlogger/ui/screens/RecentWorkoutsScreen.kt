package com.gymlogger.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymlogger.data.RoutineRepository
import com.gymlogger.data.Workout
import com.gymlogger.ui.components.GymBroTopAppBar
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentWorkoutsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToWorkoutDetail: (Long) -> Unit
) {
    val workouts by RoutineRepository.completedWorkouts.collectAsState()
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Scaffold(
        topBar = {
            GymBroTopAppBar(
                title = "Recent Workouts",
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = Color.Black
    ) { padding ->
        if (workouts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No workouts recorded yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(workouts.reversed()) { workout ->
                    WorkoutListItem(
                        name = workout.routine.name,
                        date = dateFormatter.format(Date(workout.date)),
                        sets = workout.sets.size,
                        onClick = { onNavigateToWorkoutDetail(workout.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun WorkoutListItem(
    name: String,
    date: String,
    sets: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1C1C1E)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = date,
                    fontSize = 13.sp,
                    color = Color(0xFF8E8E93)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "$sets sets",
                fontSize = 14.sp,
                color = Color(0xFF8E8E93)
            )
        }
    }
}
