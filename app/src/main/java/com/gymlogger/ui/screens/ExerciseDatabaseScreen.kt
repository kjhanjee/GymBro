package com.gymlogger.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.gymlogger.data.ExerciseRepository
import com.gymlogger.model.Exercise
import com.gymlogger.ui.components.GymBroTopAppBar
import com.gymlogger.model.MuscleGroup
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExerciseDatabaseScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val exercises by if (searchQuery.isBlank()) {
        ExerciseRepository.getAllExercises().collectAsState(initial = emptyList())
    } else {
        ExerciseRepository.searchExercises(searchQuery).collectAsState(initial = emptyList())
    }
    val pagerState = rememberPagerState(pageCount = { MuscleGroup.entries.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            GymBroTopAppBar(
                title = "Exercise Library",
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            ExerciseSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tab selection
            MuscleGroupTabSelector(pagerState = pagerState, scope = scope)

            Spacer(modifier = Modifier.height(16.dp))

            // Exercises pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                val muscleGroup = MuscleGroup.entries[page]
                val filteredExercises = exercises.filter { it.muscleGroups.contains(muscleGroup) }

                ExercisesPagerContent(
                    muscleGroup = muscleGroup,
                    exercises = filteredExercises,
                    onExerciseClick = onNavigateToDetail
                )
            }
        }
    }
}

@Composable
fun ExerciseSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        placeholder = { Text("Search exercises...", color = Color(0xFF8E8E93)) },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF8E8E93))
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF8E8E93))
                }
            }
        },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF1C1C1E),
            unfocusedContainerColor = Color(0xFF1C1C1E),
            disabledContainerColor = Color(0xFF1C1C1E),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MuscleGroupTabSelector(
    pagerState: PagerState,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val muscleGroups = MuscleGroup.entries

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(muscleGroups.size) { index ->
            val muscleGroup = muscleGroups[index]
            val isSelected = pagerState.currentPage == index
            
            Surface(
                modifier = Modifier
                    .size(56.dp)
                    .clickable { scope.launch { pagerState.animateScrollToPage(index) } },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF1C1C1E)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = muscleGroup.getPainter(),
                        contentDescription = muscleGroup.name,
                        modifier = Modifier.size(28.dp),
                        tint = if (isSelected) Color.Black else Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ExercisesPagerContent(
    muscleGroup: MuscleGroup,
    exercises: List<Exercise>,
    onExerciseClick: (Long) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = muscleGroup.name.replaceFirstChar { it.uppercase() },
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(exercises) { exercise ->
                ExerciseCard(
                    exercise = exercise,
                    onClick = { onExerciseClick(exercise.id) }
                )
            }
        }
    }
}

@Composable
fun ExerciseCard(
    exercise: Exercise,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1C1C1E)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = exercise.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (exercise.equipment) {
                            Exercise.Equipment.BARBELL -> Icons.Default.FitnessCenter
                            Exercise.Equipment.DUMBBELL -> Icons.Default.AddCircle
                            Exercise.Equipment.CABLE -> Icons.Default.Cable
                            Exercise.Equipment.MACHINE -> Icons.Default.Settings
                            Exercise.Equipment.BODYWEIGHT -> Icons.Default.Person
                            else -> Icons.Default.Stars
                        },
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF8E8E93)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = exercise.equipment.name.lowercase().replaceFirstChar { it.uppercase() },
                        fontSize = 11.sp,
                        color = Color(0xFF8E8E93)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    exercise.muscleGroups.take(1).forEach { muscle ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = muscle.name.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
