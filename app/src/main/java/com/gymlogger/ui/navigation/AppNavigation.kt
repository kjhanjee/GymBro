package com.gymlogger.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gymlogger.ui.screens.ExerciseDatabaseScreen
import com.gymlogger.ui.screens.ExerciseDetailScreen
import com.gymlogger.ui.screens.HomeScreen
import com.gymlogger.ui.screens.StatisticsScreen
import com.gymlogger.ui.screens.WorkoutScreen
import com.gymlogger.ui.screens.WorkoutTrackerScreen
import com.gymlogger.ui.screens.RoutineCreatorScreen
import com.gymlogger.ui.screens.SettingsScreen
import com.gymlogger.ui.screens.RecentWorkoutsScreen
import com.gymlogger.ui.screens.WorkoutDetailScreen
import com.gymlogger.ui.screens.MealLoggerScreen
import com.gymlogger.ui.screens.DietOptimizerScreen
import com.gymlogger.ui.screens.WorkoutOptimizerScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier
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
}
