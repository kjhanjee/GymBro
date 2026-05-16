# Changelog

All notable changes to GymLogger.

### Workout Tracker Overhaul & AI Enhancements (May 16, 2026)

**Workout Tracker:**
- **ViewModel Integration**: Migrated workout tracking logic to `WorkoutTrackerViewModel` for robust state management.
- **Aggressive Persistence**: Implemented real-time background saving of in-progress workouts, ensuring no data loss on app death or restart.
- **Drag-and-Drop Reordering**: Added interactive exercise reordering within active sessions with haptic feedback and auto-scrolling.
- **Exercise Swapping**: Users can now easily swap an exercise for another while a workout is in progress.
- **Unified Sync**: Seamless synchronization of workout stats and timers with `WorkoutService`.

**AI Subsystem:**
- **Streaming Responses**: Enabled real-time text generation in AI Trainer Chat for a more responsive experience.
- **GPU Acceleration**: Enhanced LiteRT initialization to prioritize GPU usage with automatic CPU fallback.
- **Auto-Resource Management**: 
    - Implemented a 5-minute background auto-release timer for the AI engine.
    - Added high memory pressure detection to proactively release AI resources.
- **Conversation Summarization**: Added capability to generate concise summaries of chat history with the AI Trainer.

**Platform & UI/UX:**
- **App Lifecycle Tracking**: Integrated `ProcessLifecycleOwner` to optimize resource usage based on app visibility.
- **Improved Initialization**: Added a detailed progress overlay with model download percentage tracking.
- **Focus Management**: Implemented automatic focus clearing when the keyboard is dismissed or when clicking outside inputs.
- **State Restoration**: Enhanced `MainActivity` to restore active workout services on startup if a session was in progress.

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
