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

### AI Enhancements
- **Gemma 2B Model**: Integrated the powerful Gemma 2B model for high-precision nutritional analysis and more natural trainer interactions.
- **Streaming Chat**: Experience instant feedback with streaming AI responses in the Trainer Chat interface.
- **Micronutrient Intelligence**: The AI now analyzes meals for key micros including Fibre, Sugar, Vit B, Vit D, and Omega-3.
- **Hardware Acceleration**: Optimized LiteRT (formerly TensorFlow Lite) integration with GPU acceleration support for faster inference and lower battery consumption.
- **Intelligent Resource Management**: The AI engine now automatically releases resources when the app is in the background or when the device is under high memory pressure, ensuring system stability.

### UI/UX & System Stability
- **Global Focus Management**: Automatic clearing of input focus when the keyboard is hidden, preventing accidental edits.
- **Enhanced Initialization Flow**: Real-time progress tracking for AI model initialization and assets preparation.
- **Lifecycle Awareness**: The app now intelligently monitors foreground/background transitions to optimize performance and battery life.


## Features

### Core Functionality
- **AI Trainer Chat**: Real-time personalized coaching and nutritional advice.
- **Nutritional Deep-Dive**: 
  - Track **Macros** (Calories, Protein, Carbs, Fats).
  - Track **Micros** (Fibre, Refined Sugar, Vitamin B, Vitamin D, Omega-3).
  - **Custom Food Labels**: Define custom nutrition facts for specific items to ensure 100% accuracy in your logs.
- **Personalized Settings & Goals**: Track your physique and set clear targets for your fitness journey.
- **Massive Exercise Database**: Over **1,000 exercises** categorized by muscle groups (Chest, Back, Shoulders, Legs, Arms, Core, etc.).
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
- Shareable workout summaries.

## Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Database**: Room (Migrating from DataStore)
- **Architecture**: MVVM with Repository Pattern
- **Design System**: Material 3 with Dynamic Theming support

## Setup Instructions

### AI Model Setup
The "Workout Optimizer" and "Macro Calculator" features require a LiteRT (TensorFlow Lite) model.
1. **Download the model**: [gemma-4-E2B-it.litertlm](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/blob/main/gemma-4-E2B-it.litertlm)
2. **Place the file**: Move the downloaded `.litertlm` file into the following directory:
   `app/src/main/assets/gemma-4-E2B-it.litertlm`
   *(If the `assets` folder doesn't exist, create it under `app/src/main/`)*

### Build Instructions
1. **Clone the repository** or download the source.
2. **Open the project** in Android Studio (Ladybug or newer).
3. **Gradle Sync**: Wait for Android Studio to finish syncing the project.
4. **Build & Run**:
   - Connect an Android device (API 26+) or start an emulator.
   - Use the "Run" button in Android Studio or execute the following command in the terminal:
     ```bash
     ./gradlew installDebug
     ```

### Prerequisites
1. **Android Studio** (Ladybug or newer)
2. **JDK 17** or higher
3. **Physical Device Recommended**: AI inference (LiteRT) performs significantly better on physical hardware with GPU support.

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

## Built Application
https://drive.google.com/file/d/16YW4_YKfdNrdKwiZVQeGUgSSYiiBtkam/view?usp=drive_link
