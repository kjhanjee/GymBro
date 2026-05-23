# Add Delete Workout Functionality

This plan outlines the changes needed to allow users to delete completed workouts from their history. Deleted workouts will be removed from the log and excluded from statistics calculations.

## User Review Required

> [!NOTE]
> The delete button will be added both to the list view in `RecentWorkoutsScreen` and the detail view in `WorkoutDetailScreen` for ease of use.

## Proposed Changes

### Data Layer

#### [RoutineRepository.kt](file:///F:/Projects/GymLogger/app/src/main/java/com/gymlogger/data/RoutineRepository.kt)

- Add a `deleteWorkout(context: Context, workoutId: Long)` method to remove a workout from the `_workouts` flow and update the persistent storage.

```kotlin
    suspend fun deleteWorkout(context: Context, workoutId: Long) {
        _workouts.update { current -> current.filterNot { it.id == workoutId } }
        saveWorkouts(context)
    }
```

---

### UI Layer

#### [RecentWorkoutsScreen.kt](file:///F:/Projects/GymLogger/app/src/main/java/com/gymlogger/ui/screens/RecentWorkoutsScreen.kt)

- Update `WorkoutListItem` to include a delete icon button.
- Implement a confirmation `AlertDialog` to prevent accidental deletions.
- Handle the deletion logic using the new `RoutineRepository.deleteWorkout` method.

#### [WorkoutDetailScreen.kt](file:///F:/Projects/GymLogger/app/src/main/java/com/gymlogger/ui/screens/WorkoutDetailScreen.kt)

- Add a delete icon button to the top app bar.
- Implement a confirmation `AlertDialog`.
- Navigate back to the previous screen after a workout is deleted.

---

## Verification Plan

### Manual Verification
1. **Logging a Workout**: Start and complete a new workout to ensure it appears in the recent workouts log.
2. **Checking Statistics**: Navigate to the Statistics screen and note the current values (e.g., total workouts, total volume).
3. **Deleting from List**: Go to the Recent Workouts screen and use the new delete button on the logged workout.
    - Verify that a confirmation dialog appears.
    - Confirm deletion and verify the workout is removed from the list.
4. **Verifying Statistics Update**: Go back to the Statistics screen and verify that the values have decreased/updated accordingly.
5. **Deleting from Detail View**: Log another workout, navigate to its detail view, and use the delete button in the top bar.
    - Verify that it deletes and navigates back to the log.
