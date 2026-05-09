# Changelog

All notable changes to GymLogger.

## [Unreleased]

### AI Trainer & Personalization (May 9, 2026)

**New Features:**
- **AI Trainer Chat**: Introduced an interactive AI trainer powered by LiteRT (Gemma-4-E2B) for personalized workout advice and macro calculations.
- **Settings Persistence**: Implemented `SettingsRepository` using DataStore for persisting user preferences, physique (height, weight), and fitness goals.

**Improvements:**
- **Enhanced Macro Calculator**: Significantly improved the AI-driven macro calculation logic and prompt engineering for more accurate nutritional guidance.
- **UI/UX Refinements**:
    - Set `MainActivity` to `singleTask` launch mode for improved stability.
    - Added `configChanges` handling for smoother orientation and screen size transitions.
    - Updated `MealLoggerScreen` with improved state management for better performance.
    - Refined `SettingsScreen` with a new "Physique & Goals" section.

**Bug Fixes:**
- **Timer Fixes**: Resolved issues with workout timers in `WorkoutService` to ensure consistent tracking across app restarts.
- **UI Consistency**: Fixed layout issues in `ExerciseDatabaseScreen` and `HomeScreen`.

**Data Layer:**
- Updated `ExerciseRepository` and `RoutineRepository` with improved data access patterns.
- Added `IExerciseRepository` interface for better testability and abstraction.

### Bug Fixes and Feature Updates (Apr 26, 2026)

**Workout Persistence Fix:**
- Fixed workout state not persisting when app is closed/killed during active workout tracking
- `WorkoutService` now restores timer state from saved `InProgressWorkout` data on restart
- Added `ACTION_RESTORE_WORKOUT` to `WorkoutService` for restoring active workout sessions
- Timer elapsed seconds and workout title now saved continuously alongside exercise data
- `WorkoutMiniCard` now triggers workout service restoration before navigating to tracker screen
- Completed workouts now properly clear in-progress workout data from storage

**New Features:**
- Added `WorkoutMiniCard` UI component for compact workout display
- Added foreground service permissions for workout tracking in background
- Added FileProvider for secure file access
- Added `POST_NOTIFICATIONS` permission for workout alerts

**Database & Data Layer:**
- Created new `Converters.kt` for Room type conversions
- Created `ExerciseDao.kt` for database access layer
- Updated `AppDatabase.kts` with new DAOs and entity definitions

**Workout Service:**
- Enhanced `WorkoutService.kt` with comprehensive workout logging logic
- Added foreground service type `specialUse` for workout tracking
- Added service property for workout tracking and timers subtype

**UI Updates:**
- Updated `AppNavigation.kt` with new navigation routes
- Updated `WorkoutTrackerScreen.kt` with new workout tracking UI
- Updated `RoutineCreatorScreen.kt` with extended default routines
- Updated `ExerciseDetailScreen.kt` with new exercise details UI

**Code Cleanup:**
- Removed unused exercises from `ExerciseRepository.kt`
- Cleaned up `ExerciseRepository.kt.tmp` (removed temp file)
- Updated `Gradle` wrapper properties

**Exercise Repository:**
- Updated `RoutineRepository.kt` with corrected exercise IDs to match the `ExerciseRepository`
- All 58 exercise references in routines now point to valid exercise IDs in the exercise database

**Configuration:**
- Updated `build.gradle.kts` with new dependencies
- Updated `gradle.properties` with new Gradle configuration
- Updated `gradle/wrapper/gradle-wrapper.properties`

---

## [0.1.0] - Initial Release (Apr 25, 2026)

**New Features:**
- Core workout tracking functionality
- Exercise database with detailed instructions
- Routine creation and management
- Home screen with workout summaries
- Meal logging screen

**Data Layer:**
- Room database setup with entities
- Exercise and routine repositories
- Workout service for background tracking

**UI:**
- Main activity with navigation
- Workout tracker screen
- Routine creator screen
- Exercise detail screen
- Meal logger screen

**Permissions:**
- INTERNET permission for API access
- ACCESS_NETWORK_STATE for network status
