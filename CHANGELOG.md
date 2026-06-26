# Changelog

All notable changes to GymLogger.

### AI Removal & JSON Meal Logger (June 26, 2026)

**AI Removal:**
- **Complete Decommissioning**: Removed all AI-based features, including AI Trainer Chat, Diet Optimizer, and Workout Optimizer.
- **Dependency Cleanup**: Removed LiteRT (formerly TensorFlow Lite) and related AI libraries from the project to reduce app size and complexity.
- **Resource Optimization**: Eliminated background AI resource management logic and model file handling.

**Meal Logger Update:**
- **JSON-Based Logging**: Replaced the item-by-item manual entry with a flexible JSON input system.
- **Improved Accuracy**: Users can now directly input exact macro and micro values for every meal.
- **Template Info Button**: Added an information ("i") button in the meal logger to show the required JSON template for easy copying.
- **Copy to Clipboard**: Integrated a "Copy to Clipboard" button in the JSON template dialog for faster data entry.
- **Direct Macro Entry**: Macros and Micros are now logged directly from the JSON input, bypassing the need for AI estimation.
- **Simplified UI**: Removed the "Food Labels" management system to streamline the meal logging process.
- **Enhanced Data Portability**: 
    - Added **per-day download buttons** to daily meal log headers for targeted CSV exports.
    - Expanded CSV export format to include detailed **Macros and Micros columns** (Calories, Protein, Fibre, Vitamin C, etc.).

**Technical Updates:**
- **Build Configuration**: Streamlined `build.gradle.kts` by removing AI-related configurations.
- **CI/CD**: Added a GitHub Action to automatically compile and package the APK on every push.

### App Experience & Advanced Analytics (June 14, 2026)

**Statistics & Analytics:**
- **Advanced Exercise Stats**: Introduced an IQR (Interquartile Range) filter for "All-Time Averages" to remove outliers and provide more accurate progress tracking.
- **Workout Summary Card**: Added a dedicated shareable card in Statistics showing a detailed breakdown of the most recent workout, including per-set reps, weight, and RIR.
- **Micronutrient Analytics**: Added a new section for tracking weekly averages of 10 different micronutrients with meal-type breakdowns.

**AI & Nutrition:**
- **Expanded Micronutrient Estimation**: The AI now estimates Vitamin C, Iron, Potassium, Magnesium, and Sodium from meal descriptions.
- **Meal Data Portability**: Added CSV export functionality specifically for meal logs.
- **AI Lifecycle Management**: 
    - Added a manual AI engine restart/refresh button in the Meal Logger.
    - Improved UI with "Warming up Gemma 2B" status indicators during engine initialization.
    - Support for displaying AI "thought" process during streaming responses.

**Workout Tracking Improvements:**
- **Rest Time Tracking**: Added a `restTime` field to `WorkoutSet` to track recovery periods between sets.
- **Intelligent Set Classification**: Implemented automatic `SetType` assignment (Warmup, Normal, Failure) based on the recorded Reps In Reserve (RIR).

**Technical Updates:**
- **Unit Testing**: Added comprehensive unit tests for `MealMacros` and `WorkoutSet` to ensure data integrity and logic correctness.
- **UI Refinements**: Updated `MealLoggerScreen` and `StatisticsScreen` with improved layouts for the new nutritional data and analytics.

### AI Refinements & Data Portability (May 23, 2026)

**AI & Nutrition:**
- **Macro Calculator Improvements**: Refined the AI logic for macro calculations to provide more consistent and accurate results.
- **AI Trainer Chat UI**: Enhanced the chat interface for better message readability and smoother interaction.
- **Meal Logger Updates**: Improved state management and UI feedback when logging meals.

**Data Management:**
- **CSV Export**: Introduced functionality to export workout history to CSV files, enabling users to analyze their data externally.
- **Repository Enhancements**: Added support for retrieving workout data specifically for export in `RoutineRepository`.

**UI/UX Improvements:**
- **Recent Workouts Screen**: Updated the list view to better display historical data.
- **Workout Detail Screen**: Enhanced the detail view for completed workouts with clearer stats and notes.
- **Optimizer Screens**: Refined the UI for Diet and Workout optimizers for better clarity.

**Technical Updates:**
- **Build Configuration**: Updated `build.gradle.kts` dependencies and configuration.
- **Unit Testing**: Added `CsvExporterTest` to ensure reliable data export functionality.

### Workout Tracker Overhaul & AI Enhancements (May 16, 2026)

**Workout Tracker:**
- **ViewModel Integration**: Migrated workout tracking logic to `WorkoutTrackerViewModel` for robust state management.
- **Aggressive Persistence**: Implemented real-time background saving of in-progress workouts, ensuring no data loss on app death or restart.
- **Drag-and-Drop Reordering**: Added interactive exercise reordering within active sessions with haptic feedback and auto-scrolling.
- **Exercise Swapping**: Users can now easily swap an exercise for another while a workout is in progress.
- **Unified Sync**: Seamless synchronization of workout stats and timers with `WorkoutService`.

**AI Subsystem:**
- **Gemma 2B Model**: Upgraded the AI engine to Gemma 2B for significantly more precise nutritional analysis and conversational capabilities.
- **Micronutrient Estimation**: The AI can now estimate key micronutrients (Fibre, Sugar, Vit B, Vit D, Omega) from meal descriptions.
- **Streaming Responses**: Enabled real-time text generation in AI Trainer Chat for a more responsive experience.
- **GPU Acceleration**: Enhanced LiteRT initialization to prioritize GPU usage with automatic CPU fallback.
- **Auto-Resource Management**: 
    - Implemented a 5-minute background auto-release timer for the AI engine.
    - Added high memory pressure detection to proactively release AI resources.
- **Conversation Summarization**: Added capability to generate concise summaries of chat history with the AI Trainer.

**Nutritional Tracking & Data:**
- **Micronutrient Support**: Added comprehensive tracking for Fibre, Refined Sugar, Vitamin B, Vitamin D, and Omega-3 across the app.
- **Custom Food Labels**: Introduced a dedicated repository and UI for defining custom nutrition facts for specific items, which are used to ground AI macro/micro calculations.

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
