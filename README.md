# GymBro (GymLogger) - Fitness Tracking App

GymBro is a modern, personal fitness tracking application built with Jetpack Compose and Android Room. It allows users to create custom workout routines, track their performance with high precision, and visualize their progress over time.

## Latest Developments & Improvements

### AI Trainer & Personalization
- **AI Trainer Chat**: Integrated a sophisticated AI Chat interface powered by Google's LiteRT (formerly TensorFlow Lite). Users can interact with an AI trainer to get personalized workout advice, form tips, and nutrition guidance.
- **Enhanced Macro Calculator**: Leverages the local AI model to provide precise macronutrient targets based on user-specific physique and fitness goals.
- **Settings & Goals Persistence**: Added a robust settings management system using Jetpack DataStore to persist user preferences (units, themes) and personal metrics (height, weight, target weight, fitness goals).

### Data Layer Refactoring
- **Room Database Migration**: Completed migration from DataStore to a robust Room database with new DAOs (`ExerciseDao.kt`), converters, and entity definitions. This provides improved data integrity, complex querying, and faster performance.
- **New Workout Service**: Implemented comprehensive background workout tracking service with foreground service support for uninterrupted logging sessions.
- **FileProvider Integration**: Added secure file access via FileProvider for exporting workout data and sharing progress reports.
- **Notification Permissions**: Added `POST_NOTIFICATIONS` permission to enable workout reminders and session completion alerts.

### Workout Persistence Fix
- **Active Workout Restoration**: Fixed issue where closing the app during an active workout would lose all tracking data. The app now properly saves timer state (elapsed seconds, workout title) alongside exercise data in real-time.
- **Service Restoration**: When reopening the app and tapping the `WorkoutMiniCard`, the `WorkoutService` restores the timer from the saved position and continues counting.
- **Data Integrity**: In-progress workout data is cleared properly when a workout is completed, ensuring clean state between sessions.

### UI/UX & Input Stability
- **Refined Numeric Inputs**: Implemented local `String` state management for Weight, Reps, and RIR fields. This eliminates "0.0" ghosting and flickering during active editing, providing a smooth input experience.
- **Global Focus Management**: Integrated a global focus removal system in `MainActivity`. Tapping outside any input field or closing the keyboard automatically clears focus, preventing accidental edits.
- **Professional Instruction Formatting**: Added an intelligent parsing engine for exercise "How-to" instructions. It automatically converts raw blocks of text or inconsistently numbered lists into clean, professional, and properly enumerated step-by-step guides.
- **New Mini Card Component**: Introduced `WorkoutMiniCard` for compact workout summary displays in lists and overviews.

### Data & Performance
- **Real-time Analytics**: Replaced mock data in the `ExerciseDetailScreen` with live workout history. Users can now see their actual **Weight Trends** and **Training Frequency** charts based on logged sessions.
- **Dynamic Logic**: Implemented conditional rendering for exercise charts—if no workouts are logged for a specific exercise, the trends section is hidden to maintain a clean UI.
- **Extended Default Routines**: Added comprehensive default routine templates for diverse training styles.
- **Exercise ID Normalization**: Updated RoutineRepository to use exercise IDs that exist in the ExerciseRepository. This ensures consistency between the exercise database and workout routines.


## Features

### Core Functionality
- **AI Trainer Chat**: Real-time personalized coaching and nutritional advice.
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


[Built APK]: https://drive.google.com/file/d/16YW4_YKfdNrdKwiZVQeGUgSSYiiBtkam/view?usp=drive_link
