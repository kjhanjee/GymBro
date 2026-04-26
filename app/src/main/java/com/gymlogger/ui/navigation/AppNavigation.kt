package com.gymlogger.ui.navigation

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gymlogger.service.WorkoutService
import com.gymlogger.ui.components.WorkoutMiniCard
import com.gymlogger.ui.screens.*

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.fillMaxSize()
        ) {
            composable("home") {
                HomeScreen(
                    onNavigateToCreateRoutine = { navController.navigate("createRoutine") },
                    onNavigateToTrackWorkout = { routineId -> 
                        val route = if (routineId != null) "trackWorkout?routineId=$routineId" else "trackWorkout"
                        navController.navigate(route)
                    },
                    onEditRoutine = { routineId ->
                        navController.navigate("editRoutine/$routineId")
                    },
                    onNavigateToStatistics = { navController.navigate("statistics") },
                    onNavigateToRecentWorkouts = { navController.navigate("recentWorkouts") },
                    onNavigateToExercises = { navController.navigate("exercises") },
                    onNavigateToSettings = { navController.navigate("settings") },
                    onNavigateToMealLogger = { navController.navigate("mealLogger") },
                    onNavigateToDietOptimizer = { navController.navigate("dietOptimizer") },
                    onNavigateToWorkoutOptimizer = { navController.navigate("workoutOptimizer") }
                )
            }

            composable("workoutOptimizer") {
                WorkoutOptimizerScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("dietOptimizer") {
                DietOptimizerScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("mealLogger") {
                MealLoggerScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("settings") {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("createRoutine") {
                RoutineCreatorScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                "editRoutine/{routineId}",
                arguments = listOf(navArgument("routineId") { type = NavType.LongType })
            ) { backStackEntry ->
                val routineId = backStackEntry.arguments?.getLong("routineId") ?: 0L
                RoutineCreatorScreen(
                    routineId = routineId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("trackWorkout?routineId={routineId}",
                arguments = listOf(navArgument("routineId") { 
                    type = NavType.LongType
                    defaultValue = -1L
                })
            ) { backStackEntry ->
                val routineId = backStackEntry.arguments?.getLong("routineId")
                WorkoutTrackerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    routineId = if (routineId == -1L) null else routineId
                )
            }

            composable("statistics") {
                StatisticsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("exercises") {
                ExerciseDatabaseScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { exerciseId ->
                        navController.navigate("exerciseDetail/$exerciseId")
                    }
                )
            }

            composable(
                "exerciseDetail/{exerciseId}",
                arguments = listOf(navArgument("exerciseId") { type = NavType.LongType })
            ) { backStackEntry ->
                val exerciseId = backStackEntry.arguments?.getLong("exerciseId") ?: 0L
                ExerciseDetailScreen(
                    exerciseId = exerciseId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("recentWorkouts") {
                RecentWorkoutsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToWorkoutDetail = { workoutId ->
                        navController.navigate("workoutDetail/$workoutId")
                    }
                )
            }

            composable(
                "workoutDetail/{workoutId}",
                arguments = listOf(navArgument("workoutId") { type = NavType.LongType })
            ) { backStackEntry ->
                val workoutId = backStackEntry.arguments?.getLong("workoutId") ?: 0L
                WorkoutDetailScreen(
                    workoutId = workoutId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        if (currentRoute != "trackWorkout?routineId={routineId}") {
            WorkoutMiniCard(
                onCardClick = {
                    // Restore the workout service with saved state before navigating
                    val intent = Intent(context, WorkoutService::class.java).apply {
                        action = WorkoutService.ACTION_RESTORE_WORKOUT
                    }
                    context.startService(intent)
                    navController.navigate("trackWorkout") {
                        popUpTo("home") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }
    }
}
