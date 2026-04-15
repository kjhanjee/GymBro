# GymBro (GymLogger) - Fitness Tracking App

GymBro is a modern, personal fitness tracking application built with Jetpack Compose and Android Room. It allows users to create custom workout routines, track their performance with high precision, and visualize their progress over time.

## Latest Developments & Improvements

### UI/UX & Input Stability
- **Refined Numeric Inputs**: Implemented local `String` state management for Weight, Reps, and RIR fields. This eliminates "0.0" ghosting and flickering during active editing, providing a smooth input experience.
- **Global Focus Management**: Integrated a global focus removal system in `MainActivity`. Tapping outside any input field or closing the keyboard automatically clears focus, preventing accidental edits.
- **Professional Instruction Formatting**: Added an intelligent parsing engine for exercise "How-to" instructions. It automatically converts raw blocks of text or inconsistently numbered lists into clean, professional, and properly enumerated step-by-step guides.

### Data & Performance
- **Room Migration (In Progress)**: Moving from DataStore-based JSON persistence to a robust Room database for improved data integrity, complex querying, and faster performance.
- **Real-time Analytics**: Replaced mock data in the `ExerciseDetailScreen` with live workout history. Users can now see their actual **Weight Trends** and **Training Frequency** charts based on logged sessions.
- **Dynamic Logic**: Implemented conditional rendering for exercise charts—if no workouts are logged for a specific exercise, the trends section is hidden to maintain a clean UI.

## Features

### Core Functionality
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

### Prerequisites
1. **Android Studio** (Ladybug or newer)
2. **JDK 17** or higher

### Installation
1. Clone the repository or download the source.
2. Open the project in Android Studio.
3. Wait for Gradle sync to finish.
4. Run the `:app` module on an Android device or emulator (API 26+).

## Project Structure
```
app/
├── src/main/
│   ├── java/com/gymlogger/
│   │   ├── data/          # Room DB, DAOs, and Repositories
│   │   ├── model/         # Data entities (Exercise, Routine, Workout)
│   │   ├── ui/
│   │   │   ├── navigation/ # Compose Navigation routes
│   │   │   ├── screens/    # Screen-level Composables
│   │   │   └── theme/      # Material 3 Theme definitions
│   │   └── MainActivity.kt
```

## License
Personal use only. Not for distribution or sale.
