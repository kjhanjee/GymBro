# Changelog

All notable changes to GymLogger.

## [Unreleased]

### Bug Fixes and Feature Updates (Apr 25, 2026)

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
