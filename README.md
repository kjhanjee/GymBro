# GymBro - Fitness Tracking App

A personal fitness tracking app to help you create workout routines, track workouts, and monitor your progress.

## Features

### Core Functionality
- **Exercise Database**: Extensive library of exercises organized by muscle group
  - Chest exercises (Bench Press, Incline Press, Flyes, etc.)
  - Back exercises (Bent Over Row, Pull-ups, Lat Pulldown, etc.)
  - Shoulder exercises (Overhead Press, Lateral Raises, etc.)
  - Leg exercises (Squat, Romanian Deadlift, Leg Press, etc.)
  - Arm exercises (Curls, Tricep Pushdowns, etc.)
  - Core exercises (Plank, Dead Bug, Russian Twists, etc.)
  - Cardio exercises

- **Workout Routines**: Create custom workout routines
  - Add exercises to your routines
  - Name and describe your routines
  - Save for later use

- **Workout Tracking**: Log your workouts with full details
  - Track sets, reps, and weight
  - **RIR (Reps In Reserve)** tracking
  - **4 Set Types**:
    - Normal Set
    - Warmup Set
    - Failure Set
    - Drop Set
  - Notes for each set

### UI/UX
- **Card-based design** for modern, clean interface
- **Dark mode** support (default)
- **Material 3** design system
- **Dynamic Theming**: Custom primary color based on user preference

### Home Screen
- Quick action buttons for main features
- Your saved routines overview
- Easy navigation

### Statistics
- View completed workouts history
- Track progress over time
- Share workout summaries as images

## Setup Instructions

### Prerequisites
1. **Android Studio** (latest version)
2. **JDK 17** or higher

### Step 1: Import the Project
1. Open Android Studio
2. Select "Open an existing project"
3. Navigate to `F:/Projects/GymBro`

### Step 2: Sync Gradle
Android Studio will automatically sync the Gradle files. Wait for the sync to complete.

### Step 3: Build and Run
1. Connect an Android device or start an emulator
2. Click the Run button (green play icon)
3. Select your device/emulator

### Alternative: Command Line

If you have Java installed and JAVA_HOME configured:

```bash
cd "F:/Projects/GymBro"
./gradlew assembleDebug
```

Then install on your device:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Project Structure

```
app/
├── src/main/
│   ├── java/com/gymlogger/
│   │   ├── data/
│   │   │   ├── ExerciseRepository.kt
│   │   │   └── RoutineRepository.kt
│   │   ├── model/
│   │   │   ├── Exercise.kt
│   │   │   ├── WorkoutSet.kt
│   │   │   ├── Routine.kt
│   │   │   └── MuscleGroup.kt
│   │   ├── ui/
│   │   │   ├── navigation/
│   │   │   ├── screens/
│   │   │   │   ├── HomeScreen.kt
│   │   │   │   ├── WorkoutTrackerScreen.kt
│   │   │   │   ├── ExerciseDatabaseScreen.kt
│   │   │   │   ├── RoutineCreatorScreen.kt
│   │   │   │   └── StatisticsScreen.kt
│   │   │   └── theme/
│   │   └── MainActivity.kt
│   └── res/
│       ├── values/
│       └── values-night/
```

## Exercise Library

The app includes a comprehensive exercise database covering:
- **Chest**: 7 exercises
- **Back**: 7 exercises
- **Shoulders**: 5 exercises
- **Legs**: 8 exercises
- **Arms**: 8 exercises
- **Core**: 5 exercises
- **Cardio**: 4 exercises

## Set Types Explained

### Normal Set
Regular working set towards your training goal.

### Warmup Set
Lighter weight to prepare muscles before main work.

### Failure Set
Go to muscular failure (0 RIR) to maximize stimulus.

### Drop Set
After reaching failure, immediately reduce weight and continue.

## License

Personal use only. Not for distribution or sale.
