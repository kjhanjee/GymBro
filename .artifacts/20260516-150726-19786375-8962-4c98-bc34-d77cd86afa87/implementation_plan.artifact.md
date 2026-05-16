# Update Changelog and README based on latest commit

The goal is to document the significant improvements and new features introduced in the latest commit (`d090c9840d5da1a902721566b9f5c5894f73fc0c`) by updating `CHANGELOG.md` and `README.md`. Although the commit message was "Bug fixes", the changes represent a major update to the workout tracking and AI subsystems.

## Proposed Changes

### Documentation

#### [CHANGELOG.md](file:///F:/Projects/GymLogger/CHANGELOG.md)

- Add a new entry for May 16, 2026, titled "Workout Tracker Overhaul & AI Enhancements".
- Detail the following improvements:
    - **Workout Tracker**:
        - `WorkoutTrackerViewModel` integration for improved state management.
        - **Aggressive Persistence**: Real-time background saving of in-progress workouts.
        - **Drag-and-Drop**: Interactive exercise reordering within a session.
        - **Exercise Swapping**: Easily replace exercises while tracking.
        - Unified stats and timer sync with `WorkoutService`.
    - **AI Subsystem**:
        - **Streaming Responses**: Real-time text generation in AI Trainer Chat.
        - **GPU Acceleration**: Enhanced LiteRT initialization with automatic CPU fallback.
        - **Auto-Resource Management**: Automatic release of AI engine to save memory (background/high pressure).
        - **Message Summarization**: Ability to summarize chat history.
    - **Platform & UI/UX**:
        - Proactive memory management in `MainActivity`.
        - App-wide lifecycle tracking for resource optimization.
        - Initialization progress overlay with download tracking.
        - Automatic focus clearing on keyboard dismissal.

#### [README.md](file:///F:/Projects/GymLogger/README.md)

- Update the "Latest Developments & Improvements" section.
- Highlight the new Workout Tracker features (Persistence, Drag-and-Drop).
- Update the AI section to mention Streaming and GPU acceleration.
- Update the Project Structure if necessary (added `viewmodel/WorkoutTrackerViewModel.kt`).

---

## Verification Plan

### Automated Tests
- Not applicable for documentation changes.

### Manual Verification
- Review the formatted `CHANGELOG.md` and `README.md` to ensure clarity and accuracy.
- Verify that all mentioned features correspond to the actual changes in the commit.
