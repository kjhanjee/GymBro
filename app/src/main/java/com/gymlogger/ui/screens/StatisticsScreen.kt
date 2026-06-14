package com.gymlogger.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.scale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymlogger.data.SettingsRepository
import com.gymlogger.data.MealRepository
import com.gymlogger.model.Meal
import com.gymlogger.model.MealType
import com.gymlogger.model.WorkoutSet
import com.gymlogger.data.Workout
import com.gymlogger.data.RoutineRepository
import com.gymlogger.util.UnitConverter
import androidx.compose.ui.platform.LocalContext
import com.gymlogger.ui.components.GymBroTopAppBar
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val weightUnit by SettingsRepository.getWeightUnit(context).collectAsStateWithLifecycle(initialValue = SettingsRepository.WeightUnit.LBS)
    val timerUnit by SettingsRepository.getTimerUnit(context).collectAsStateWithLifecycle(initialValue = SettingsRepository.TimerUnit.MINUTES)
    val workouts by RoutineRepository.completedWorkouts.collectAsStateWithLifecycle(initialValue = emptyList())
    val meals by MealRepository.meals.collectAsStateWithLifecycle(initialValue = emptyList())

    // Aggregate stats
    val totalWorkouts = workouts.size
    val totalSets = workouts.sumOf { it.sets.size }
    val totalVolume = workouts.sumOf { workout -> 
        workout.sets.sumOf { set -> ((set.reps ?: 0) * (set.weight ?: 0f)).toDouble() }
    }.toFloat()
    
    val totalRestTime = workouts.sumOf { workout ->
        workout.sets.sumOf { it.restTime ?: 0 }
    }
    
    val avgRest = if (totalSets > 0) {
        totalRestTime / totalSets
    } else 0

    // Current Week Stats Calculation
    val calendar = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val startOfWeek = calendar.timeInMillis
    calendar.add(Calendar.DAY_OF_YEAR, 7)
    val endOfWeek = calendar.timeInMillis

    val weekMeals = meals.filter { it.date in startOfWeek until endOfWeek }
    val weekWorkouts = workouts.filter { it.date in startOfWeek until endOfWeek }

    val daysWithMeals = weekMeals.map { 
        Calendar.getInstance().apply { timeInMillis = it.date }.get(Calendar.DAY_OF_YEAR)
    }.distinct().size.coerceAtLeast(1)

    val daysWithWorkouts = weekWorkouts.map {
        Calendar.getInstance().apply { timeInMillis = it.date }.get(Calendar.DAY_OF_YEAR)
    }.distinct().size.coerceAtLeast(1)

    Scaffold(
        topBar = {
            GymBroTopAppBar(
                title = "Statistics",
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            item {
                // Stats Overview Card
                StatsOverviewCard(
                    totalWorkouts = totalWorkouts,
                    totalSets = totalSets,
                    totalVolume = "${UnitConverter.formatWeight(totalVolume, weightUnit)} ${weightUnit.name.lowercase()}",
                    avgRest = "${UnitConverter.formatTimer(avgRest, timerUnit)} ${if (timerUnit == SettingsRepository.TimerUnit.MINUTES) "min" else "sec"}"
                )
            }

            item {
                CurrentWeekAveragesCard(weekMeals = weekMeals, weekWorkouts = weekWorkouts)
            }

            item {
                // Macronutritional Breakdown Section
                MacronutritionalBreakdownSection(weekMeals = weekMeals, daysLogged = daysWithMeals)
            }

            item {
                // Micronutritional Breakdown Section
                MicronutritionalBreakdownSection(weekMeals = weekMeals, daysLogged = daysWithMeals)
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                // Weekly Activity (Calculated from actual sets)
                val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                val weeklyData = dayNames.map { dayName ->
                    val setsCount = weekWorkouts.filter { workout ->
                        val workoutCal = Calendar.getInstance().apply { timeInMillis = workout.date }
                        val dayOfWeek = workoutCal.get(Calendar.DAY_OF_WEEK)
                        val targetDayOfWeek = when(dayName) {
                            "Sun" -> Calendar.SUNDAY
                            "Mon" -> Calendar.MONDAY
                            "Tue" -> Calendar.TUESDAY
                            "Wed" -> Calendar.WEDNESDAY
                            "Thu" -> Calendar.THURSDAY
                            "Fri" -> Calendar.FRIDAY
                            "Sat" -> Calendar.SATURDAY
                            else -> -1
                        }
                        dayOfWeek == targetDayOfWeek
                    }.sumOf { it.sets.size }
                    dayName to setsCount
                }

                val totalSetsThisWeek = weeklyData.sumOf { it.second }
                val avgSetsPerDay = totalSetsThisWeek.toFloat() / daysWithWorkouts

                WeeklyActivityChart(dailySets = weeklyData, avgSets = avgSetsPerDay)
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (workouts.isNotEmpty()) {
                item {
                    ExerciseAveragesCard(workouts = workouts, weightUnit = weightUnit)
                }
            }

            if (workouts.isNotEmpty()) {
                val lastWorkout = workouts.last()
                val lastWorkoutVolume = lastWorkout.sets.sumOf { ((it.reps ?: 0) * (it.weight ?: 0f)).toDouble() }.toFloat()
                item {
                    // Most Recent Workout Card
                    WorkoutSummaryShareCard(
                        workoutName = lastWorkout.routine.name,
                        date = "Last Workout",
                        totalSets = lastWorkout.sets.size,
                        totalVolume = "${UnitConverter.formatWeight(lastWorkoutVolume, weightUnit)} ${weightUnit.name.lowercase()}",
                        weightUnit = weightUnit,
                        workout = lastWorkout
                    )
                }
            }
        }
    }
}

@Composable
fun ExerciseAveragesCard(
    workouts: List<Workout>,
    weightUnit: SettingsRepository.WeightUnit
) {
    var useIqr by remember { mutableStateOf(false) }
    
    // Process data
    val exerciseWeights = workouts.flatMap { it.sets }
        .filter { (it.weight ?: 0f) > 0f }
        .groupBy { it.exerciseName }
        .mapValues { (_, sets) -> sets.map { it.weight!! } }
        .filter { it.value.isNotEmpty() }

    val sortedExercises = exerciseWeights.map { (name, weights) ->
        val processedWeights = if (useIqr && weights.size >= 4) {
            val sorted = weights.sorted()
            val q1Index = (sorted.size * 0.25).toInt()
            val q3Index = (sorted.size * 0.75).toInt()
            val q1 = sorted[q1Index]
            val q3 = sorted[q3Index]
            val iqr = q3 - q1
            val lowerBound = q1 - 1.5f * iqr
            val upperBound = q3 + 1.5f * iqr
            weights.filter { it in lowerBound..upperBound }
        } else {
            weights
        }
        
        val avg = if (processedWeights.isNotEmpty()) processedWeights.average().toFloat() else 0f
        Triple(name, avg, weights.size)
    }.sortedByDescending { it.second }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "All-Time Averages",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("IQR Filter", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8E8E93))
                    Switch(
                        checked = useIqr,
                        onCheckedChange = { useIqr = it },
                        modifier = Modifier.scale(0.7f),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            Text(
                "Calculated from all recorded sessions",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8E8E93)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                sortedExercises.forEach { (name, avg, count) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            Text("$count sets total", color = Color(0xFF8E8E93), style = MaterialTheme.typography.labelSmall)
                        }
                        Text(
                            "${UnitConverter.formatWeight(avg, weightUnit)} ${weightUnit.name.lowercase()}",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CurrentWeekAveragesCard(weekMeals: List<Meal>, weekWorkouts: List<Workout>) {
    val totalCalories = weekMeals.sumOf { it.macros.calories.toDouble() }.toFloat()
    val totalProtein = weekMeals.sumOf { it.macros.protein.toDouble() }.toFloat()
    val totalCarbs = weekMeals.sumOf { it.macros.carbs.toDouble() }.toFloat()
    val totalFats = weekMeals.sumOf { it.macros.fats.toDouble() }.toFloat()
    
    val totalSets = weekWorkouts.sumOf { it.sets.size }

    val avgCalories = totalCalories / 7f
    val avgProtein = totalProtein / 7f
    val avgCarbs = totalCarbs / 7f
    val avgFats = totalFats / 7f
    val avgSets = totalSets.toFloat() / 7f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Current Week's Average",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                "Daily average for the full week (Totals / 7)",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8E8E93)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MacroStatColumn("Calories", "${avgCalories.toInt()}", "kcal")
                MacroStatColumn("Protein", "${avgProtein.toInt()}", "g")
                MacroStatColumn("Sets", String.format(Locale.getDefault(), "%.1f", avgSets), "sets")
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SmallMacroText("Carbs: ${avgCarbs.toInt()}g")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SmallMacroText("Fats: ${avgFats.toInt()}g")
                }
            }
        }
    }
}

@Composable
fun MacronutritionalBreakdownSection(weekMeals: List<Meal>, daysLogged: Int) {
    val totalCalories = weekMeals.sumOf { it.macros.calories.toDouble() }.toFloat()
    val totalProtein = weekMeals.sumOf { it.macros.protein.toDouble() }.toFloat()
    val totalCarbs = weekMeals.sumOf { it.macros.carbs.toDouble() }.toFloat()
    val totalFats = weekMeals.sumOf { it.macros.fats.toDouble() }.toFloat()

    val avgCalories = totalCalories / daysLogged
    val avgProtein = totalProtein / daysLogged
    val avgCarbs = totalCarbs / daysLogged
    val avgFats = totalFats / daysLogged

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Weekly Average Macros",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                "Daily average based on logged days (${daysLogged} days)",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8E8E93)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Weekly Averages
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MacroStatColumn("Calories", "${avgCalories.toInt()}", "kcal")
                MacroStatColumn("Protein", "${avgProtein.toInt()}", "g")
                MacroStatColumn("Carbs", "${avgCarbs.toInt()}", "g")
                MacroStatColumn("Fats", "${avgFats.toInt()}", "g")
            }

            if (weekMeals.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFF2C2C2E))
                
                Text(
                    "Avg. Daily Breakdown by Meal Type",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF8E8E93)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val mealTypeOrder = listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER, MealType.SNACK, MealType.PRE_WORKOUT)
                
                mealTypeOrder.forEach { type ->
                    val mealsOfType = weekMeals.filter { it.type == type }
                    if (mealsOfType.isNotEmpty()) {
                        val typeCalories = mealsOfType.sumOf { it.macros.calories.toDouble() }.toFloat() / daysLogged
                        val typeProtein = mealsOfType.sumOf { it.macros.protein.toDouble() }.toFloat() / daysLogged
                        val typeCarbs = mealsOfType.sumOf { it.macros.carbs.toDouble() }.toFloat() / daysLogged
                        val typeFats = mealsOfType.sumOf { it.macros.fats.toDouble() }.toFloat() / daysLogged

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                type.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                modifier = Modifier.width(80.dp)
                            )
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                SmallMacroText("${typeCalories.toInt()} kcal")
                                SmallMacroText("P: ${typeProtein.toInt()}g")
                                SmallMacroText("C: ${typeCarbs.toInt()}g")
                                SmallMacroText("F: ${typeFats.toInt()}g")
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "No meals logged this week",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8E8E93),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun MicronutritionalBreakdownSection(weekMeals: List<Meal>, daysLogged: Int) {
    val totalFibre = weekMeals.sumOf { it.macros.fibre.toDouble() }.toFloat()
    val totalSugar = weekMeals.sumOf { it.macros.refinedSugar.toDouble() }.toFloat()
    val totalVitB = weekMeals.sumOf { it.macros.vitaminB.toDouble() }.toFloat()
    val totalVitD = weekMeals.sumOf { it.macros.vitaminD.toDouble() }.toFloat()
    val totalOmega = weekMeals.sumOf { it.macros.omega.toDouble() }.toFloat()
    val totalVitC = weekMeals.sumOf { it.macros.vitaminC.toDouble() }.toFloat()
    val totalIron = weekMeals.sumOf { it.macros.iron.toDouble() }.toFloat()
    val totalPotassium = weekMeals.sumOf { it.macros.potassium.toDouble() }.toFloat()
    val totalMagnesium = weekMeals.sumOf { it.macros.magnesium.toDouble() }.toFloat()
    val totalSodium = weekMeals.sumOf { it.macros.sodium.toDouble() }.toFloat()

    val avgFibre = totalFibre / daysLogged
    val avgSugar = totalSugar / daysLogged
    val avgVitB = totalVitB / daysLogged
    val avgVitD = totalVitD / daysLogged
    val avgOmega = totalOmega / daysLogged
    val avgVitC = totalVitC / daysLogged
    val avgIron = totalIron / daysLogged
    val avgPotassium = totalPotassium / daysLogged
    val avgMagnesium = totalMagnesium / daysLogged
    val avgSodium = totalSodium / daysLogged

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Weekly Average Micros",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                "Daily average based on logged days (${daysLogged} days)",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8E8E93)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Weekly Averages
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MacroStatColumn("Fibre", "${avgFibre.toInt()}", "g")
                MacroStatColumn("Ref. Sugar", "${avgSugar.toInt()}", "g")
                MacroStatColumn("Vit B", String.format(Locale.getDefault(), "%.1f", avgVitB), "mg")
                MacroStatColumn("Vit D", String.format(Locale.getDefault(), "%.1f", avgVitD), "mcg")
                MacroStatColumn("Omega", "${avgOmega.toInt()}", "mg")
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MacroStatColumn("Vit C", "${avgVitC.toInt()}", "mg")
                MacroStatColumn("Iron", "${avgIron.toInt()}", "mg")
                MacroStatColumn("Potassium", "${avgPotassium.toInt()}", "mg")
                MacroStatColumn("Magnesium", "${avgMagnesium.toInt()}", "mg")
                MacroStatColumn("Sodium", "${avgSodium.toInt()}", "mg")
            }

            if (weekMeals.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFF2C2C2E))
                
                Text(
                    "Avg. Daily Breakdown by Meal Type",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF8E8E93)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val mealTypeOrder = listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER, MealType.SNACK, MealType.PRE_WORKOUT)
                
                mealTypeOrder.forEach { type ->
                    val mealsOfType = weekMeals.filter { it.type == type }
                    if (mealsOfType.isNotEmpty()) {
                        val typeFibre = mealsOfType.sumOf { it.macros.fibre.toDouble() }.toFloat() / daysLogged
                        val typeSugar = mealsOfType.sumOf { it.macros.refinedSugar.toDouble() }.toFloat() / daysLogged
                        val typeVitB = mealsOfType.sumOf { it.macros.vitaminB.toDouble() }.toFloat() / daysLogged
                        val typeVitD = mealsOfType.sumOf { it.macros.vitaminD.toDouble() }.toFloat() / daysLogged
                        val typeOmega = mealsOfType.sumOf { it.macros.omega.toDouble() }.toFloat() / daysLogged
                        val typeVitC = mealsOfType.sumOf { it.macros.vitaminC.toDouble() }.toFloat() / daysLogged
                        val typeIron = mealsOfType.sumOf { it.macros.iron.toDouble() }.toFloat() / daysLogged
                        val typePotassium = mealsOfType.sumOf { it.macros.potassium.toDouble() }.toFloat() / daysLogged
                        val typeMagnesium = mealsOfType.sumOf { it.macros.magnesium.toDouble() }.toFloat() / daysLogged
                        val typeSodium = mealsOfType.sumOf { it.macros.sodium.toDouble() }.toFloat() / daysLogged

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                type.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                modifier = Modifier.width(80.dp)
                            )
                            
                            Column(horizontalAlignment = Alignment.End) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SmallMacroText("Fib: ${typeFibre.toInt()}g")
                                    SmallMacroText("Sug: ${typeSugar.toInt()}g")
                                    SmallMacroText("B: ${String.format(Locale.getDefault(), "%.1f", typeVitB)}mg")
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SmallMacroText("D: ${String.format(Locale.getDefault(), "%.1f", typeVitD)}mcg")
                                    SmallMacroText("Omg: ${typeOmega.toInt()}mg")
                                    SmallMacroText("C: ${typeVitC.toInt()}mg")
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SmallMacroText("Fe: ${typeIron.toInt()}mg")
                                    SmallMacroText("K: ${typePotassium.toInt()}mg")
                                    SmallMacroText("Mg: ${typeMagnesium.toInt()}mg")
                                    SmallMacroText("Na: ${typeSodium.toInt()}mg")
                                }
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "No meals logged this week",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8E8E93),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun MacroStatColumn(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF8E8E93))
            Spacer(modifier = Modifier.width(2.dp))
            Text(unit, style = MaterialTheme.typography.labelSmall, color = Color(0xFF8E8E93), fontSize = 8.sp)
        }
    }
}

@Composable
fun SmallMacroText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFF8E8E93),
        fontSize = 10.sp
    )
}

@Composable
fun StatsOverviewCard(
    totalWorkouts: Int,
    totalSets: Int,
    totalVolume: String,
    avgRest: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatRow(
                value = totalWorkouts.toString(),
                label = "Workouts",
                icon = Icons.Default.CheckCircle,
                color = MaterialTheme.colorScheme.primary
            )

            StatRow(
                value = totalSets.toString(),
                label = "Total Sets",
                icon = Icons.Default.Stars,
                color = MaterialTheme.colorScheme.secondary
            )

            StatRow(
                value = totalVolume,
                label = "Volume",
                icon = Icons.Default.FitnessCenter,
                color = MaterialTheme.colorScheme.tertiary
            )

            StatRow(
                value = avgRest,
                label = "Avg Rest",
                icon = Icons.Default.Timer,
                color = MaterialTheme.colorScheme.primaryContainer
            )
        }
    }
}

@Composable
fun StatRow(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF8E8E93),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun WeeklyActivityChart(
    dailySets: List<Pair<String, Int>>,
    avgSets: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Weekly Activity (Avg: ${String.format(Locale.getDefault(), "%.1f", avgSets)} sets/day)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val maxCount = dailySets.maxOf { it.second }.coerceAtLeast(1)
                dailySets.forEach { (day, count) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (count > 0) count.toString() else "",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .width(16.dp)
                                .background(
                                    color = Color(0xFF2C2C2E),
                                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                ),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            if (count > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(count.toFloat() / maxCount)
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                                )
                                            ),
                                            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                        )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = day,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF8E8E93)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Total sets this week: ${dailySets.sumOf { it.second }}",
                fontSize = 12.sp,
                color = Color(0xFF8E8E93),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun WorkoutSummaryShareCard(
    workoutName: String,
    date: String,
    totalSets: Int,
    totalVolume: String,
    weightUnit: SettingsRepository.WeightUnit,
    workout: Workout? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1C1C1E)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Most Recent Workout",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = workoutName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = date,
                fontSize = 14.sp,
                color = Color(0xFF8E8E93)
            )

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(color = Color(0xFF2C2C2E))

            Spacer(modifier = Modifier.height(12.dp))

            // Table preview
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (workout != null) {
                    // Group sets by exercise
                    val groupedSets = workout.sets.groupBy { it.exerciseId }
                    groupedSets.forEach { (_, sets) ->
                        val exerciseName = sets.firstOrNull()?.exerciseName ?: "Unknown"
                        WorkoutSummaryExerciseRow(
                            exerciseName,
                            sets,
                            weightUnit
                        )
                    }
                } else {
                    // Fallback to placeholder if no workout provided
                    Text("No sets recorded", color = Color(0xFF8E8E93))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Total: $totalSets sets • $totalVolume",
                fontSize = 13.sp,
                color = Color(0xFF8E8E93)
            )
        }
    }
}

@Composable
fun WorkoutSummaryExerciseRow(
    exerciseName: String,
    sets: List<WorkoutSet>,
    weightUnit: SettingsRepository.WeightUnit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = exerciseName,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        // Header for sets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Set", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF8E8E93), modifier = Modifier.weight(0.15f))
            Text("Reps", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF8E8E93), modifier = Modifier.weight(0.2f))
            Text("Weight", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF8E8E93), modifier = Modifier.weight(0.35f))
            Text("RIR", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF8E8E93), modifier = Modifier.weight(0.2f))
        }

        Spacer(modifier = Modifier.height(4.dp))

        sets.forEachIndexed { index, set ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${index + 1}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.weight(0.15f)
                )
                Text(
                    text = (set.reps ?: 0).toString(),
                    fontSize = 14.sp,
                    color = Color.White,
                    modifier = Modifier.weight(0.2f)
                )
                Text(
                    text = if ((set.weight ?: 0f) > 0) "${UnitConverter.formatWeight(set.weight!!, weightUnit)} ${weightUnit.name.lowercase()}" else "BW",
                    fontSize = 14.sp,
                    color = Color.White,
                    modifier = Modifier.weight(0.35f)
                )
                Text(
                    text = set.rir?.toString() ?: "-",
                    fontSize = 14.sp,
                    color = if ((set.rir ?: 10) <= 1) MaterialTheme.colorScheme.error else Color.White,
                    modifier = Modifier.weight(0.2f)
                )
            }
        }
    }
}

@Composable
fun WorkoutListItem(
    name: String,
    date: String,
    sets: Int,
    volume: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = {})
            .background(Color(0xFF1C1C1E))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = date,
                    fontSize = 12.sp,
                    color = Color(0xFF8E8E93)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "$sets sets • $volume",
                    fontSize = 12.sp,
                    color = Color(0xFF8E8E93)
                )
            }
        }
    }
}
