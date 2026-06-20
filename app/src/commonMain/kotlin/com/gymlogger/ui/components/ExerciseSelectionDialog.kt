package com.gymlogger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gymlogger.data.ExerciseRepository
import com.gymlogger.model.Exercise
import com.gymlogger.model.MuscleGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSelectionDialog(
    onDismiss: () -> Unit,
    onSelect: (Exercise) -> Unit
) {
    val themeHue by com.gymlogger.data.SettingsRepository.activeThemeHue.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var muscleGroup by remember { mutableStateOf<MuscleGroup?>(null) }
    
    val exercises = ExerciseRepository.exerciseDatabase
    val filteredExercises = exercises.filter {
        val matchesQuery = it.name.contains(searchQuery, ignoreCase = true)
        val matchesGroup = muscleGroup == null || it.muscleGroups.contains(muscleGroup)
        matchesQuery && matchesGroup
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        com.gymlogger.ui.theme.GymBroTheme(hue = themeHue) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Add Exercise", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    // Search Bar
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        placeholder = { Text("Search exercises...", color = Color(0xFF8E8E93)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF8E8E93)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1C1C1E),
                            unfocusedContainerColor = Color(0xFF1C1C1E),
                            disabledContainerColor = Color(0xFF1C1C1E),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Muscle group selector
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = muscleGroup == null,
                                onClick = { muscleGroup = null },
                                label = { Text("All") },
                                colors = FilterChipDefaults.filterChipColors(
                                    labelColor = Color.White,
                                    selectedLabelColor = Color.White,
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    containerColor = Color(0xFF1C1C1E)
                                ),
                                border = null,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        items(MuscleGroup.entries.toTypedArray()) { group ->
                            FilterChip(
                                selected = muscleGroup == group,
                                onClick = { muscleGroup = group },
                                label = { 
                                    Text(group.name.lowercase().replaceFirstChar { it.uppercase() })
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    labelColor = Color.White,
                                    selectedLabelColor = Color.White,
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    containerColor = Color(0xFF1C1C1E)
                                ),
                                border = null,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    ) {
                        items(filteredExercises) { ex ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(ex) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ex.name,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White
                                        )
                                        Text(
                                            text = ex.equipment.name,
                                            fontSize = 12.sp,
                                            color = Color(0xFF8E8E93)
                                        )
                                    }
                                }
                                HorizontalDivider(color = Color(0xFF1C1C1E), thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}
