# GymBro (GymLogger) - Fitness Tracking App

GymBro is a modern, personal fitness tracking application built with Jetpack Compose and Android Room. It allows users to create custom workout routines, track their performance with high precision, and visualize their progress over time.

## Latest Developments & Improvements

### Workout Tracker Overhaul
- **Advanced State Management**: Introduced `WorkoutTrackerViewModel` to centralize workout logic and provide a reactive, lifecycle-aware tracking experience.
- **Aggressive Persistence**: Real-time background saving of every set and rep. If the app is killed or the system restarts, your workout progress is restored exactly where you left off.
- **Interactive Exercise Management**:
    - **Drag-and-Drop**: Reorder exercises on the fly with long-press gestures.
    - **Dynamic Swapping**: Instantly swap an exercise for a similar one without losing your place in the routine.
- **Improved Timer Sync**: Better coordination between the UI and `WorkoutService` for accurate rest timers and total duration tracking.

### Meal Logger Enhancement (New)
- **JSON-Based Input**: Log your meals and macros using a structured JSON format. You can now add multiple items per meal with their individual macro and micro details.
- **Template Support**: Built-in JSON template helps you quickly fill in macros and micros for any meal, with a **one-tap "Copy to Clipboard"** feature for efficiency.
- **Micronutrient Tracking**: Manually track key micros including Fibre, Sugar, Vitamin B, Vitamin D, Omega, Vitamin C, Iron, Potassium, Magnesium, and Sodium.
- **Per-Day Data Export**: Export specific days of meal logs directly from the daily headers in the Meal Logger.

### Statistics & Advanced Analytics
- **Precision Progress Tracking**: Introduced an **IQR (Interquartile Range) filter** for exercise averages, allowing you to filter out anomalous sets and see your true strength progression.
- **Workout Summaries**: New "Most Recent Workout" card providing a high-fidelity table view of your last session's performance.
- **Enhanced Micronutrient Analytics**: Deep-dive into your weekly nutrition with dedicated sections for 10+ micronutrients, categorized by meal type.

### Workout Tracking Refinements
- **Rest Time Monitoring**: Track the exact recovery time for every set to optimize your training density.
- **Auto-Set Classification**: The app now intelligently classifies sets (Warmup/Failure) based on your reported **Reps In Reserve (RIR)**.


## Features

### Core Functionality
- **Nutritional Deep-Dive**: 
  - Track **Macros** (Calories, Protein, Carbs, Fats).
  - Track **Micros** (Fibre, Refined Sugar, Vitamin B, Vitamin D, Omega-3, Vitamin C, Iron, Potassium, Magnesium, Sodium).
- **Personalized Settings & Goals**: Track your physique and set clear targets for your fitness journey.
- **Massive Exercise Database**: Over **1,000 exercises** categorized by muscle groups.
- **Custom Routine Creator**: Build and save personalized workout plans with ease.
- **Precision Workout Tracking**:
  - Log Sets, Reps, and Weight.
  - **Reps In Reserve (RIR)** tracking for intensity management.
  - **4 Set Types**: Normal, Warmup, Failure, and Drop Sets.
  - Per-set notes for detailed tracking.

### Home Screen
- Quick actions for core features.
- Overview of saved routines.
- Seamless navigation between Library, Routines, and Statistics.

### Statistics & Progress
- Comprehensive workout history.
- Dynamic charts for individual exercise progress.
- **Workout Summaries**: View your most recent session with detailed set-by-set analysis.
- **Data Export**: Export your workout and meal history (including detailed Macros/Micros) to CSV for external analysis.

## Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Database**: Room
- **Architecture**: MVVM with Repository Pattern
- **Design System**: Material 3 with Dynamic Theming support

## Build Instructions
1. **Clone the repository** or download the source.
2. **Open the project** in Android Studio (Ladybug or newer).
3. **Gradle Sync**: Wait for Android Studio to finish syncing the project.
4. **Build & Run**:
   - Connect an Android device (API 26+) or start an emulator.
   - Use the "Run" button in Android Studio or execute the following command in the terminal:
     ```bash
     # Linux/macOS
     ./gradlew installDebug
     
     # Windows
     .\gradlew.bat installDebug
     ```

### Prerequisites
1. **Android Studio** (Ladybug or newer)
2. **JDK 17** or higher

## Project Structure
```
app/
├── src/main/
│   ├── java/com/gymlogger/
│   │   ├── data/          # Room DB, DAOs, Repositories, Converters, Settings
│   │   │   ├── AppDatabase.kt    # Room database definition
│   │   │   ├── ExerciseDao.kt    # Exercise database access
│   │   │   ├── ExerciseRepository.kt
│   │   │   ├── RoutineRepository.kt
│   │   │   ├── SettingsRepository.kt # User preferences & goals DataStore
│   │   │   ├── Converters.kt     # Room type converters
│   │   │   └── WorkoutService.kt  # Background workout tracking
│   │   ├── viewmodel/     # ViewModel implementations
│   │   │   └── WorkoutTrackerViewModel.kt # State management for workout tracking
│   │   ├── model/         # Data entities (Exercise, Routine, Workout)
│   │   ├── ui/
│   │   │   ├── navigation/ # Compose Navigation routes
│   │   │   ├── screens/    # Screen-level Composables
│   │   │   ├── components/ # Reusable UI components (WorkoutMiniCard)
│   │   │   └── theme/      # Material 3 Theme definitions
│   │   └── MainActivity.kt
```

## License
Personal use only. Not for distribution or sale.
