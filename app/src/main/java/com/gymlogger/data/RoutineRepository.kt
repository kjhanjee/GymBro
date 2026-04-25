package com.gymlogger.data

import android.content.Context
import com.gymlogger.model.Routine
import com.gymlogger.model.WorkoutSet
import com.gymlogger.model.Exercise
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

object RoutineRepository {
    private val ROUTINES_KEY = stringPreferencesKey("routines_list")
    private val WORKOUTS_KEY = stringPreferencesKey("completed_workouts")

    private val defaultRoutines = listOf<Routine>(
        Routine(
            id = 1L,
            name = "Chest & Biceps (Full Coverage)",
            description = "Complete chest (upper, mid, lower, inner) and biceps (long head, short head, brachialis) workout with structured intensity and progression.",
            exercises = listOf(
                Routine.RoutineExercise(1, 2001, "Incline Dumbbell Press", 0, listOf(
                    Routine.RoutineExercise.SetConfig(1, WorkoutSet.SetType.NORMAL, 10, 15f, 2, 120),
                    Routine.RoutineExercise.SetConfig(2, WorkoutSet.SetType.NORMAL, 8, 15f, 2, 120),
                    Routine.RoutineExercise.SetConfig(3, WorkoutSet.SetType.NORMAL, 8, 17.5f, 1, 120),
                    Routine.RoutineExercise.SetConfig(4, WorkoutSet.SetType.NORMAL, 6, 17.5f, 0, 150)
                )),
                Routine.RoutineExercise(2, 2002, "Flat Barbell Bench Press", 1, listOf(
                    Routine.RoutineExercise.SetConfig(5, WorkoutSet.SetType.NORMAL, 10, 0f, 2, 120),
                    Routine.RoutineExercise.SetConfig(6, WorkoutSet.SetType.NORMAL, 8, 0f, 2, 120),
                    Routine.RoutineExercise.SetConfig(7, WorkoutSet.SetType.NORMAL, 8, 0f, 1, 120),
                    Routine.RoutineExercise.SetConfig(8, WorkoutSet.SetType.NORMAL, 6, 0f, 0, 150)
                )),
                Routine.RoutineExercise(3, 2003, "Decline Chest Press Machine", 2, listOf(
                    Routine.RoutineExercise.SetConfig(9, WorkoutSet.SetType.NORMAL, 12, 0f, 2, 90),
                    Routine.RoutineExercise.SetConfig(10, WorkoutSet.SetType.NORMAL, 10, 0f, 1, 90),
                    Routine.RoutineExercise.SetConfig(11, WorkoutSet.SetType.NORMAL, 10, 0f, 1, 90)
                )),
                Routine.RoutineExercise(4, 2004, "Cable Fly (Low to High)", 3, listOf(
                    Routine.RoutineExercise.SetConfig(12, WorkoutSet.SetType.NORMAL, 15, 0f, 1, 60),
                    Routine.RoutineExercise.SetConfig(13, WorkoutSet.SetType.NORMAL, 12, 0f, 1, 60),
                    Routine.RoutineExercise.SetConfig(14, WorkoutSet.SetType.NORMAL, 12, 0f, 0, 60)
                )),
                Routine.RoutineExercise(5, 2005, "Pec Deck Fly", 4, listOf(
                    Routine.RoutineExercise.SetConfig(15, WorkoutSet.SetType.NORMAL, 15, 0f, 1, 60),
                    Routine.RoutineExercise.SetConfig(16, WorkoutSet.SetType.NORMAL, 12, 0f, 0, 60)
                )),
                Routine.RoutineExercise(6, 3001, "Incline Dumbbell Curl", 5, listOf(
                    Routine.RoutineExercise.SetConfig(17, WorkoutSet.SetType.NORMAL, 12, 10f, 2, 90),
                    Routine.RoutineExercise.SetConfig(18, WorkoutSet.SetType.NORMAL, 10, 10f, 1, 90),
                    Routine.RoutineExercise.SetConfig(19, WorkoutSet.SetType.NORMAL, 10, 10f, 1, 90)
                )),
                Routine.RoutineExercise(7, 3002, "Preacher Curl (EZ Bar or Machine)", 6, listOf(
                    Routine.RoutineExercise.SetConfig(20, WorkoutSet.SetType.NORMAL, 12, 0f, 1, 90),
                    Routine.RoutineExercise.SetConfig(21, WorkoutSet.SetType.NORMAL, 10, 0f, 1, 90),
                    Routine.RoutineExercise.SetConfig(22, WorkoutSet.SetType.NORMAL, 10, 0f, 0, 90)
                )),
                Routine.RoutineExercise(8, 3003, "Hammer Curl (Dumbbell or Rope)", 7, listOf(
                    Routine.RoutineExercise.SetConfig(23, WorkoutSet.SetType.NORMAL, 15, 10f, 1, 60),
                    Routine.RoutineExercise.SetConfig(24, WorkoutSet.SetType.NORMAL, 12, 10f, 0, 60)
                ))
            )
        ),
        Routine(
            id = 2L,
            name = "Back & Triceps (Full Coverage)",
            description = "Complete back (lats width, lower lats, mid-back thickness, rear delts, lower back) and triceps (long, lateral, medial heads) workout with structured progressive overload and fatigue management.",
            exercises = listOf(
                Routine.RoutineExercise(1, 21001, "Wide-Grip Lat Pulldown", 0, listOf(
                    Routine.RoutineExercise.SetConfig(25, WorkoutSet.SetType.NORMAL, 12, 45f, 2, 90),
                    Routine.RoutineExercise.SetConfig(26, WorkoutSet.SetType.NORMAL, 10, 50f, 2, 90),
                    Routine.RoutineExercise.SetConfig(27, WorkoutSet.SetType.NORMAL, 8, 55f, 1, 120)
                )),
                Routine.RoutineExercise(2, 21023, "Single-Arm Lat Pulldown (Cable)", 1, listOf(
                    Routine.RoutineExercise.SetConfig(28, WorkoutSet.SetType.NORMAL, 12, 25f, 2, 75),
                    Routine.RoutineExercise.SetConfig(29, WorkoutSet.SetType.NORMAL, 10, 30f, 1, 75),
                    Routine.RoutineExercise.SetConfig(30, WorkoutSet.SetType.NORMAL, 10, 30f, 1, 90)
                )),
                Routine.RoutineExercise(3, 21008, "Chest-Supported Row (Machine)", 2, listOf(
                    Routine.RoutineExercise.SetConfig(31, WorkoutSet.SetType.NORMAL, 10, 40f, 2, 120),
                    Routine.RoutineExercise.SetConfig(32, WorkoutSet.SetType.NORMAL, 8, 45f, 2, 120),
                    Routine.RoutineExercise.SetConfig(33, WorkoutSet.SetType.NORMAL, 8, 50f, 1, 120)
                )),
                Routine.RoutineExercise(4, 21004, "Face Pull (Rope)", 3, listOf(
                    Routine.RoutineExercise.SetConfig(34, WorkoutSet.SetType.NORMAL, 15, 20f, 2, 60),
                    Routine.RoutineExercise.SetConfig(35, WorkoutSet.SetType.NORMAL, 12, 25f, 1, 60),
                    Routine.RoutineExercise.SetConfig(36, WorkoutSet.SetType.NORMAL, 12, 25f, 0, 75)
                )),
                Routine.RoutineExercise(5, 20010, "Dumbbell Romanian Deadlift", 4, listOf(
                    Routine.RoutineExercise.SetConfig(37, WorkoutSet.SetType.NORMAL, 12, 15f, 2, 120),
                    Routine.RoutineExercise.SetConfig(38, WorkoutSet.SetType.NORMAL, 10, 17.5f, 2, 120),
                    Routine.RoutineExercise.SetConfig(39, WorkoutSet.SetType.NORMAL, 10, 20f, 1, 120)
                )),
                Routine.RoutineExercise(6, 8302, "Overhead Cable Triceps Extension (Rope)", 5, listOf(
                    Routine.RoutineExercise.SetConfig(40, WorkoutSet.SetType.NORMAL, 12, 20f, 2, 75),
                    Routine.RoutineExercise.SetConfig(41, WorkoutSet.SetType.NORMAL, 10, 25f, 1, 75),
                    Routine.RoutineExercise.SetConfig(42, WorkoutSet.SetType.NORMAL, 10, 25f, 1, 90)
                )),
                Routine.RoutineExercise(7, 8301, "Cable Triceps Pushdown (V-Bar)", 6, listOf(
                    Routine.RoutineExercise.SetConfig(43, WorkoutSet.SetType.NORMAL, 12, 25f, 2, 75),
                    Routine.RoutineExercise.SetConfig(44, WorkoutSet.SetType.NORMAL, 10, 30f, 1, 75),
                    Routine.RoutineExercise.SetConfig(45, WorkoutSet.SetType.NORMAL, 8, 35f, 1, 90)
                )),
                Routine.RoutineExercise(8, 8336, "Reverse Grip Cable Pushdown", 7, listOf(
                    Routine.RoutineExercise.SetConfig(46, WorkoutSet.SetType.NORMAL, 15, 15f, 2, 60),
                    Routine.RoutineExercise.SetConfig(47, WorkoutSet.SetType.NORMAL, 12, 20f, 1, 60),
                    Routine.RoutineExercise.SetConfig(48, WorkoutSet.SetType.NORMAL, 12, 20f, 0, 75)
                ))
            )
        ),
        Routine(
            id = 3L,
            name = "Legs & Core (Full Coverage)",
            description = "Complete lower body (quads, hamstrings, glutes, adductors, calves, tibialis) and full core (upper, lower, obliques, deep stability) workout optimized for hypertrophy and fat loss.",
            exercises = listOf(
                Routine.RoutineExercise(1, 20003, "Barbell Squat", 0, listOf(
                    Routine.RoutineExercise.SetConfig(49, WorkoutSet.SetType.NORMAL, 10, 40f, 2, 150),
                    Routine.RoutineExercise.SetConfig(50, WorkoutSet.SetType.NORMAL, 8, 50f, 2, 150),
                    Routine.RoutineExercise.SetConfig(51, WorkoutSet.SetType.NORMAL, 8, 60f, 1, 180),
                    Routine.RoutineExercise.SetConfig(52, WorkoutSet.SetType.NORMAL, 6, 60f, 0, 180)
                )),
                Routine.RoutineExercise(2, 20002, "Machine Leg Press", 1, listOf(
                    Routine.RoutineExercise.SetConfig(53, WorkoutSet.SetType.NORMAL, 12, 100f, 2, 120),
                    Routine.RoutineExercise.SetConfig(54, WorkoutSet.SetType.NORMAL, 12, 120f, 2, 120),
                    Routine.RoutineExercise.SetConfig(55, WorkoutSet.SetType.NORMAL, 10, 140f, 1, 150)
                )),
                Routine.RoutineExercise(3, 20006, "Barbell Romanian Deadlift", 2, listOf(
                    Routine.RoutineExercise.SetConfig(56, WorkoutSet.SetType.NORMAL, 10, 40f, 2, 120),
                    Routine.RoutineExercise.SetConfig(57, WorkoutSet.SetType.NORMAL, 10, 50f, 2, 120),
                    Routine.RoutineExercise.SetConfig(58, WorkoutSet.SetType.NORMAL, 8, 60f, 1, 150)
                )),
                Routine.RoutineExercise(4, 20004, "Machine Seated Leg Curl", 3, listOf(
                    Routine.RoutineExercise.SetConfig(59, WorkoutSet.SetType.NORMAL, 15, 35f, 2, 90),
                    Routine.RoutineExercise.SetConfig(60, WorkoutSet.SetType.NORMAL, 12, 40f, 1, 90),
                    Routine.RoutineExercise.SetConfig(61, WorkoutSet.SetType.NORMAL, 12, 45f, 0, 90)
                )),
                Routine.RoutineExercise(5, 7002, "Barbell Hip Thrust", 4, listOf(
                    Routine.RoutineExercise.SetConfig(62, WorkoutSet.SetType.NORMAL, 12, 50f, 2, 120),
                    Routine.RoutineExercise.SetConfig(63, WorkoutSet.SetType.NORMAL, 10, 60f, 1, 120),
                    Routine.RoutineExercise.SetConfig(64, WorkoutSet.SetType.NORMAL, 10, 70f, 0, 150)
                )),
                Routine.RoutineExercise(6, 20007, "Machine Hip Adduction", 5, listOf(
                    Routine.RoutineExercise.SetConfig(65, WorkoutSet.SetType.NORMAL, 15, 35f, 2, 60),
                    Routine.RoutineExercise.SetConfig(66, WorkoutSet.SetType.NORMAL, 15, 40f, 1, 60),
                    Routine.RoutineExercise.SetConfig(67, WorkoutSet.SetType.NORMAL, 12, 45f, 0, 60)
                )),
                Routine.RoutineExercise(7, 23002, "Machine Calf Raise", 6, listOf(
                    Routine.RoutineExercise.SetConfig(68, WorkoutSet.SetType.NORMAL, 20, 20f, 2, 60),
                    Routine.RoutineExercise.SetConfig(69, WorkoutSet.SetType.NORMAL, 15, 30f, 1, 60),
                    Routine.RoutineExercise.SetConfig(70, WorkoutSet.SetType.NORMAL, 15, 35f, 0, 60)
                )),
                Routine.RoutineExercise(8, 23001, "Machine Seated Calf Raise", 7, listOf(
                    Routine.RoutineExercise.SetConfig(71, WorkoutSet.SetType.NORMAL, 20, 20f, 2, 60),
                    Routine.RoutineExercise.SetConfig(72, WorkoutSet.SetType.NORMAL, 18, 25f, 1, 60),
                    Routine.RoutineExercise.SetConfig(73, WorkoutSet.SetType.NORMAL, 15, 30f, 0, 60)
                )),
                Routine.RoutineExercise(9, 23027, "Posterior Tibialis Stretch", 8, listOf(
                    Routine.RoutineExercise.SetConfig(74, WorkoutSet.SetType.NORMAL, 20, 0f, 2, 45),
                    Routine.RoutineExercise.SetConfig(75, WorkoutSet.SetType.NORMAL, 20, 0f, 1, 45),
                    Routine.RoutineExercise.SetConfig(76, WorkoutSet.SetType.NORMAL, 20, 0f, 0, 45)
                ), WorkoutSet.InputType.REPS),
                Routine.RoutineExercise(10, 1005, "Hanging Leg Raise", 9, listOf(
                    Routine.RoutineExercise.SetConfig(77, WorkoutSet.SetType.NORMAL, 15, 0f, 2, 60),
                    Routine.RoutineExercise.SetConfig(78, WorkoutSet.SetType.NORMAL, 12, 0f, 1, 60),
                    Routine.RoutineExercise.SetConfig(79, WorkoutSet.SetType.NORMAL, 12, 0f, 0, 60)
                )),
                Routine.RoutineExercise(11, 1027, "Cable Crunch", 10, listOf(
                    Routine.RoutineExercise.SetConfig(80, WorkoutSet.SetType.NORMAL, 15, 25f, 2, 60),
                    Routine.RoutineExercise.SetConfig(81, WorkoutSet.SetType.NORMAL, 12, 30f, 1, 60),
                    Routine.RoutineExercise.SetConfig(82, WorkoutSet.SetType.NORMAL, 12, 35f, 0, 60)
                )),
                Routine.RoutineExercise(12, 1014, "Cable Wood Chop", 11, listOf(
                    Routine.RoutineExercise.SetConfig(83, WorkoutSet.SetType.NORMAL, 15, 15f, 2, 60),
                    Routine.RoutineExercise.SetConfig(84, WorkoutSet.SetType.NORMAL, 12, 20f, 1, 60),
                    Routine.RoutineExercise.SetConfig(85, WorkoutSet.SetType.NORMAL, 12, 20f, 0, 60)
                )),
                Routine.RoutineExercise(13, 1001, "Plank", 12, listOf(
                    Routine.RoutineExercise.SetConfig(86, WorkoutSet.SetType.NORMAL, 60, 0f, 2, 60),
                    Routine.RoutineExercise.SetConfig(87, WorkoutSet.SetType.NORMAL, 60, 0f, 1, 60),
                    Routine.RoutineExercise.SetConfig(88, WorkoutSet.SetType.NORMAL, 60, 0f, 0, 60)
                ), WorkoutSet.InputType.TIME)
            )
        ),
        Routine(
            id = 4L,
            name = "Shoulders Growth + Biceps Shaping",
            description = "Shoulder-focused hypertrophy session targeting anterior, lateral, and posterior deltoids with progressive overload, followed by biceps shaping work (long head, short head, brachialis) using controlled volume and moderate intensity to enhance definition without excessive fatigue.",
            createdAt = 1777106841920L,
            exercises = listOf(
                Routine.RoutineExercise(1, 4001, "Seated Dumbbell Shoulder Press", 0, listOf(
                    Routine.RoutineExercise.SetConfig(89, WorkoutSet.SetType.NORMAL, 10, 12.5f, 2, 120),
                    Routine.RoutineExercise.SetConfig(90, WorkoutSet.SetType.NORMAL, 8, 15f, 2, 120),
                    Routine.RoutineExercise.SetConfig(91, WorkoutSet.SetType.NORMAL, 8, 17.5f, 1, 120),
                    Routine.RoutineExercise.SetConfig(92, WorkoutSet.SetType.NORMAL, 6, 17.5f, 0, 150)
                )),
                Routine.RoutineExercise(2, 4002, "Cable Lateral Raise (Single Arm)", 1, listOf(
                    Routine.RoutineExercise.SetConfig(93, WorkoutSet.SetType.NORMAL, 15, 5f, 2, 60),
                    Routine.RoutineExercise.SetConfig(94, WorkoutSet.SetType.NORMAL, 12, 7.5f, 1, 60),
                    Routine.RoutineExercise.SetConfig(95, WorkoutSet.SetType.NORMAL, 12, 7.5f, 1, 60)
                )),
                Routine.RoutineExercise(3, 4004, "Rear Delt Fly (Machine)", 3, listOf(
                    Routine.RoutineExercise.SetConfig(99, WorkoutSet.SetType.NORMAL, 15, 25f, 2, 60),
                    Routine.RoutineExercise.SetConfig(100, WorkoutSet.SetType.NORMAL, 12, 30f, 1, 60),
                    Routine.RoutineExercise.SetConfig(101, WorkoutSet.SetType.NORMAL, 12, 30f, 0, 75)
                )),
                Routine.RoutineExercise(4, 4005, "Face Pull (Cable - Rope)", 4, listOf(
                    Routine.RoutineExercise.SetConfig(102, WorkoutSet.SetType.NORMAL, 15, 15f, 2, 60),
                    Routine.RoutineExercise.SetConfig(103, WorkoutSet.SetType.NORMAL, 12, 20f, 1, 60),
                    Routine.RoutineExercise.SetConfig(104, WorkoutSet.SetType.NORMAL, 12, 20f, 1, 60)
                )),
                Routine.RoutineExercise(5, 4101, "Incline Dumbbell Curl", 5, listOf(
                    Routine.RoutineExercise.SetConfig(105, WorkoutSet.SetType.NORMAL, 15, 7.5f, 2, 60),
                    Routine.RoutineExercise.SetConfig(106, WorkoutSet.SetType.NORMAL, 12, 7.5f, 2, 60),
                    Routine.RoutineExercise.SetConfig(107, WorkoutSet.SetType.NORMAL, 12, 10f, 1, 60)
                )),
                Routine.RoutineExercise(6, 4102, "Preacher Curl (Machine or EZ Bar)", 6, listOf(
                    Routine.RoutineExercise.SetConfig(108, WorkoutSet.SetType.NORMAL, 15, 15f, 2, 60),
                    Routine.RoutineExercise.SetConfig(109, WorkoutSet.SetType.NORMAL, 12, 20f, 1, 60),
                    Routine.RoutineExercise.SetConfig(110, WorkoutSet.SetType.NORMAL, 12, 20f, 1, 60)
                )),
                Routine.RoutineExercise(7, 4103, "Hammer Curl (Dumbbells)", 7, listOf(
                    Routine.RoutineExercise.SetConfig(111, WorkoutSet.SetType.NORMAL, 15, 7.5f, 2, 60),
                    Routine.RoutineExercise.SetConfig(112, WorkoutSet.SetType.NORMAL, 12, 10f, 1, 60)
                ))
            )
        ),
        Routine(
            id = 5L,
            name = "Chest & Triceps (Full Coverage - Shaping)",
            description = "Shaping-focused chest and triceps workout covering upper, mid, lower, inner chest and all triceps heads (long, lateral, medial) using moderate weights, higher reps, and controlled intensity without failure emphasis.",
            createdAt = 1777106841920L,
            exercises = listOf(
                Routine.RoutineExercise(1, 2001, "Incline Dumbbell Press", 0, listOf(
                    Routine.RoutineExercise.SetConfig(113, WorkoutSet.SetType.NORMAL, 15, 12.5f, 3, 90),
                    Routine.RoutineExercise.SetConfig(114, WorkoutSet.SetType.NORMAL, 15, 12.5f, 2, 90),
                    Routine.RoutineExercise.SetConfig(115, WorkoutSet.SetType.NORMAL, 12, 15f, 2, 90)
                )),
                Routine.RoutineExercise(2, 2002, "Flat Machine Chest Press", 1, listOf(
                    Routine.RoutineExercise.SetConfig(116, WorkoutSet.SetType.NORMAL, 15, 0f, 3, 90),
                    Routine.RoutineExercise.SetConfig(117, WorkoutSet.SetType.NORMAL, 12, 0f, 2, 90),
                    Routine.RoutineExercise.SetConfig(118, WorkoutSet.SetType.NORMAL, 12, 0f, 2, 90)
                )),
                Routine.RoutineExercise(3, 2003, "Decline Cable Press", 2, listOf(
                    Routine.RoutineExercise.SetConfig(119, WorkoutSet.SetType.NORMAL, 15, 0f, 3, 75),
                    Routine.RoutineExercise.SetConfig(120, WorkoutSet.SetType.NORMAL, 15, 0f, 2, 75),
                    Routine.RoutineExercise.SetConfig(121, WorkoutSet.SetType.NORMAL, 12, 0f, 2, 75)
                )),
                Routine.RoutineExercise(4, 2004, "Cable Fly (Mid / Inner Chest Focus)", 3, listOf(
                    Routine.RoutineExercise.SetConfig(122, WorkoutSet.SetType.NORMAL, 15, 0f, 2, 60),
                    Routine.RoutineExercise.SetConfig(123, WorkoutSet.SetType.NORMAL, 15, 0f, 1, 60),
                    Routine.RoutineExercise.SetConfig(124, WorkoutSet.SetType.NORMAL, 15, 0f, 1, 60)
                )),
                Routine.RoutineExercise(5, 3001, "Overhead Cable Triceps Extension", 4, listOf(
                    Routine.RoutineExercise.SetConfig(125, WorkoutSet.SetType.NORMAL, 15, 0f, 3, 75),
                    Routine.RoutineExercise.SetConfig(126, WorkoutSet.SetType.NORMAL, 15, 0f, 2, 75),
                    Routine.RoutineExercise.SetConfig(127, WorkoutSet.SetType.NORMAL, 12, 0f, 2, 75)
                )),
                Routine.RoutineExercise(6, 3002, "Cable Triceps Pushdown (Straight Bar)", 5, listOf(
                    Routine.RoutineExercise.SetConfig(128, WorkoutSet.SetType.NORMAL, 15, 0f, 2, 60),
                    Routine.RoutineExercise.SetConfig(129, WorkoutSet.SetType.NORMAL, 15, 0f, 1, 60),
                    Routine.RoutineExercise.SetConfig(130, WorkoutSet.SetType.NORMAL, 12, 0f, 1, 60)
                )),
                Routine.RoutineExercise(7, 3003, "Reverse Grip Cable Pushdown (Medial Head Focus)", 6, listOf(
                    Routine.RoutineExercise.SetConfig(131, WorkoutSet.SetType.NORMAL, 15, 0f, 2, 60),
                    Routine.RoutineExercise.SetConfig(132, WorkoutSet.SetType.NORMAL, 15, 0f, 1, 60)
                ))
            )
        ),
        Routine(
            id = 6L,
            name = "Back & Shoulders (Full Shaping)",
            description = "Back and shoulders shaping workout covering lats (width), mid-back (thickness), lower back, and rear delts along with all three shoulder heads using controlled, higher-rep training without failure for definition and muscle detailing.",
            createdAt = 1777106841920L,
            exercises = listOf(
                Routine.RoutineExercise(1, 4001, "Wide-Grip Lat Pulldown", 0, listOf(
                    Routine.RoutineExercise.SetConfig(133, WorkoutSet.SetType.NORMAL, 15, 40f, 3, 75),
                    Routine.RoutineExercise.SetConfig(134, WorkoutSet.SetType.NORMAL, 15, 45f, 2, 75),
                    Routine.RoutineExercise.SetConfig(135, WorkoutSet.SetType.NORMAL, 12, 45f, 2, 75)
                )),
                Routine.RoutineExercise(2, 4002, "Seated Cable Row (Wide or Neutral Grip)", 1, listOf(
                    Routine.RoutineExercise.SetConfig(136, WorkoutSet.SetType.NORMAL, 15, 40f, 3, 75),
                    Routine.RoutineExercise.SetConfig(137, WorkoutSet.SetType.NORMAL, 12, 45f, 2, 75),
                    Routine.RoutineExercise.SetConfig(138, WorkoutSet.SetType.NORMAL, 12, 45f, 2, 75)
                )),
                Routine.RoutineExercise(3, 4003, "Straight-Arm Cable Pulldown", 2, listOf(
                    Routine.RoutineExercise.SetConfig(139, WorkoutSet.SetType.NORMAL, 15, 20f, 3, 60),
                    Routine.RoutineExercise.SetConfig(140, WorkoutSet.SetType.NORMAL, 15, 25f, 2, 60),
                    Routine.RoutineExercise.SetConfig(141, WorkoutSet.SetType.NORMAL, 12, 25f, 2, 60)
                )),
                Routine.RoutineExercise(4, 4004, "Chest-Supported Dumbbell Row", 3, listOf(
                    Routine.RoutineExercise.SetConfig(142, WorkoutSet.SetType.NORMAL, 15, 12.5f, 3, 75),
                    Routine.RoutineExercise.SetConfig(143, WorkoutSet.SetType.NORMAL, 12, 15f, 2, 75),
                    Routine.RoutineExercise.SetConfig(144, WorkoutSet.SetType.NORMAL, 12, 15f, 2, 75)
                )),
                Routine.RoutineExercise(5, 4005, "Back Extension (Bodyweight or Light Plate)", 4, listOf(
                    Routine.RoutineExercise.SetConfig(145, WorkoutSet.SetType.NORMAL, 15, 0f, 3, 60),
                    Routine.RoutineExercise.SetConfig(146, WorkoutSet.SetType.NORMAL, 15, 5f, 2, 60)
                )),
                Routine.RoutineExercise(6, 4006, "Seated Dumbbell Shoulder Press", 5, listOf(
                    Routine.RoutineExercise.SetConfig(147, WorkoutSet.SetType.NORMAL, 15, 10f, 3, 75),
                    Routine.RoutineExercise.SetConfig(148, WorkoutSet.SetType.NORMAL, 12, 12.5f, 2, 75),
                    Routine.RoutineExercise.SetConfig(149, WorkoutSet.SetType.NORMAL, 12, 12.5f, 2, 75)
                )),
                Routine.RoutineExercise(7, 4007, "Dumbbell Lateral Raise", 6, listOf(
                    Routine.RoutineExercise.SetConfig(150, WorkoutSet.SetType.NORMAL, 15, 5f, 3, 60),
                    Routine.RoutineExercise.SetConfig(151, WorkoutSet.SetType.NORMAL, 15, 7.5f, 2, 60),
                    Routine.RoutineExercise.SetConfig(152, WorkoutSet.SetType.NORMAL, 15, 7.5f, 2, 60)
                )),
                Routine.RoutineExercise(8, 4008, "Reverse Pec Deck (Rear Delt Fly)", 7, listOf(
                    Routine.RoutineExercise.SetConfig(153, WorkoutSet.SetType.NORMAL, 15, 25f, 3, 60),
                    Routine.RoutineExercise.SetConfig(154, WorkoutSet.SetType.NORMAL, 15, 30f, 2, 60),
                    Routine.RoutineExercise.SetConfig(155, WorkoutSet.SetType.NORMAL, 12, 30f, 2, 60)
                ))
            )
        ),
        Routine(
            id = 7L,
            name = "Full Body A (Push Emphasis + Core)",
            description = "Push-focused full body with upper chest, anterior delts, triceps and balanced lower + core.",
            createdAt = 1777106841920L,
            exercises = listOf(
                Routine.RoutineExercise(1, 3001, "Incline Dumbbell Press", 0, listOf(
                    Routine.RoutineExercise.SetConfig(1, WorkoutSet.SetType.NORMAL, 10, 15f, 2, 120),
                    Routine.RoutineExercise.SetConfig(2, WorkoutSet.SetType.NORMAL, 8, 17.5f, 2, 120),
                    Routine.RoutineExercise.SetConfig(3, WorkoutSet.SetType.NORMAL, 8, 17.5f, 1, 120)
                )),
                Routine.RoutineExercise(2, 3002, "Seated Cable Row", 1, listOf(
                    Routine.RoutineExercise.SetConfig(4, WorkoutSet.SetType.NORMAL, 12, 40f, 2, 90),
                    Routine.RoutineExercise.SetConfig(5, WorkoutSet.SetType.NORMAL, 10, 45f, 2, 90),
                    Routine.RoutineExercise.SetConfig(6, WorkoutSet.SetType.NORMAL, 10, 45f, 1, 90)
                )),
                Routine.RoutineExercise(3, 3003, "Leg Press", 2, listOf(
                    Routine.RoutineExercise.SetConfig(7, WorkoutSet.SetType.NORMAL, 12, 100f, 2, 120),
                    Routine.RoutineExercise.SetConfig(8, WorkoutSet.SetType.NORMAL, 10, 120f, 2, 120),
                    Routine.RoutineExercise.SetConfig(9, WorkoutSet.SetType.NORMAL, 10, 120f, 1, 120)
                )),
                Routine.RoutineExercise(4, 3004, "Dumbbell Shoulder Press", 3, listOf(
                    Routine.RoutineExercise.SetConfig(10, WorkoutSet.SetType.NORMAL, 10, 12.5f, 2, 90),
                    Routine.RoutineExercise.SetConfig(11, WorkoutSet.SetType.NORMAL, 8, 15f, 2, 90),
                    Routine.RoutineExercise.SetConfig(12, WorkoutSet.SetType.NORMAL, 8, 15f, 1, 90)
                )),
                Routine.RoutineExercise(5, 3005, "Overhead Dumbbell Triceps Extension", 4, listOf(
                    Routine.RoutineExercise.SetConfig(13, WorkoutSet.SetType.NORMAL, 12, 10f, 2, 60),
                    Routine.RoutineExercise.SetConfig(14, WorkoutSet.SetType.NORMAL, 10, 12.5f, 1, 60)
                )),
                Routine.RoutineExercise(6, 3006, "Incline Dumbbell Curl", 5, listOf(
                    Routine.RoutineExercise.SetConfig(15, WorkoutSet.SetType.NORMAL, 12, 10f, 2, 60),
                    Routine.RoutineExercise.SetConfig(16, WorkoutSet.SetType.NORMAL, 10, 12.5f, 1, 60)
                )),
                Routine.RoutineExercise(7, 3007, "Hanging Knee Raises", 6, listOf(
                    Routine.RoutineExercise.SetConfig(17, WorkoutSet.SetType.NORMAL, 15, 0f, 1, 45),
                    Routine.RoutineExercise.SetConfig(18, WorkoutSet.SetType.NORMAL, 12, 0f, 0, 45)
                ))
            )
        ),
        Routine(
            id = 8L,
            name = "Full Body B (Pull Emphasis + Posterior Chain)",
            description = "Pull-dominant day focusing on lats, posterior chain, biceps peak and shoulder width.",
            createdAt = 1777106841920L,
            exercises = listOf(
                Routine.RoutineExercise(1, 3101, "Lat Pulldown (Neutral Grip)", 0, listOf(
                    Routine.RoutineExercise.SetConfig(19, WorkoutSet.SetType.NORMAL, 12, 50f, 2, 90),
                    Routine.RoutineExercise.SetConfig(20, WorkoutSet.SetType.NORMAL, 10, 55f, 2, 90),
                    Routine.RoutineExercise.SetConfig(21, WorkoutSet.SetType.NORMAL, 10, 55f, 1, 90)
                )),
                Routine.RoutineExercise(2, 3102, "Flat Bench Press Machine", 1, listOf(
                    Routine.RoutineExercise.SetConfig(22, WorkoutSet.SetType.NORMAL, 12, 40f, 2, 120),
                    Routine.RoutineExercise.SetConfig(23, WorkoutSet.SetType.NORMAL, 10, 45f, 2, 120),
                    Routine.RoutineExercise.SetConfig(24, WorkoutSet.SetType.NORMAL, 8, 50f, 1, 120)
                )),
                Routine.RoutineExercise(3, 3103, "Romanian Deadlift (Dumbbell)", 2, listOf(
                    Routine.RoutineExercise.SetConfig(25, WorkoutSet.SetType.NORMAL, 10, 20f, 2, 120),
                    Routine.RoutineExercise.SetConfig(26, WorkoutSet.SetType.NORMAL, 8, 22.5f, 2, 120),
                    Routine.RoutineExercise.SetConfig(27, WorkoutSet.SetType.NORMAL, 8, 22.5f, 1, 120)
                )),
                Routine.RoutineExercise(4, 3104, "Dumbbell Lateral Raise", 3, listOf(
                    Routine.RoutineExercise.SetConfig(28, WorkoutSet.SetType.NORMAL, 15, 7.5f, 2, 60),
                    Routine.RoutineExercise.SetConfig(29, WorkoutSet.SetType.NORMAL, 12, 7.5f, 1, 60)
                )),
                Routine.RoutineExercise(5, 3105, "Preacher Curl", 4, listOf(
                    Routine.RoutineExercise.SetConfig(30, WorkoutSet.SetType.NORMAL, 12, 25f, 1, 60),
                    Routine.RoutineExercise.SetConfig(31, WorkoutSet.SetType.NORMAL, 10, 30f, 0, 60)
                )),
                Routine.RoutineExercise(6, 3106, "Cable Triceps Pushdown", 5, listOf(
                    Routine.RoutineExercise.SetConfig(32, WorkoutSet.SetType.NORMAL, 12, 25f, 1, 60),
                    Routine.RoutineExercise.SetConfig(33, WorkoutSet.SetType.NORMAL, 10, 30f, 0, 60)
                )),
                Routine.RoutineExercise(7, 3107, "Plank", 6, listOf(
                    Routine.RoutineExercise.SetConfig(34, WorkoutSet.SetType.NORMAL, 60, 0f, 1, 45),
                    Routine.RoutineExercise.SetConfig(35, WorkoutSet.SetType.NORMAL, 60, 0f, 0, 45)
                ))
            )
        ),
        Routine(
            id = 9L,
            name = "Full Body C (Balance + Isolation Finishers)",
            description = "Balanced full-body day covering lower chest, rear delts, hamstrings, calves, brachialis and core finishing.",
            createdAt = 1777106841920L,
            exercises = listOf(
                Routine.RoutineExercise(1, 3201, "Decline Dumbbell Press", 0, listOf(
                    Routine.RoutineExercise.SetConfig(36, WorkoutSet.SetType.NORMAL, 12, 15f, 2, 90),
                    Routine.RoutineExercise.SetConfig(37, WorkoutSet.SetType.NORMAL, 10, 17.5f, 2, 90),
                    Routine.RoutineExercise.SetConfig(38, WorkoutSet.SetType.NORMAL, 8, 17.5f, 1, 90)
                )),
                Routine.RoutineExercise(2, 3202, "Single Arm Cable Row", 1, listOf(
                    Routine.RoutineExercise.SetConfig(39, WorkoutSet.SetType.NORMAL, 12, 25f, 2, 75),
                    Routine.RoutineExercise.SetConfig(40, WorkoutSet.SetType.NORMAL, 10, 30f, 1, 75)
                )),
                Routine.RoutineExercise(3, 3203, "Leg Curl Machine", 2, listOf(
                    Routine.RoutineExercise.SetConfig(41, WorkoutSet.SetType.NORMAL, 15, 35f, 2, 75),
                    Routine.RoutineExercise.SetConfig(42, WorkoutSet.SetType.NORMAL, 12, 40f, 1, 75)
                )),
                Routine.RoutineExercise(4, 3204, "Standing Calf Raise", 3, listOf(
                    Routine.RoutineExercise.SetConfig(43, WorkoutSet.SetType.NORMAL, 20, 20f, 1, 60),
                    Routine.RoutineExercise.SetConfig(44, WorkoutSet.SetType.NORMAL, 15, 25f, 0, 60)
                )),
                Routine.RoutineExercise(5, 3205, "Reverse Fly (Rear Delt)", 4, listOf(
                    Routine.RoutineExercise.SetConfig(45, WorkoutSet.SetType.NORMAL, 15, 7.5f, 1, 60),
                    Routine.RoutineExercise.SetConfig(46, WorkoutSet.SetType.NORMAL, 12, 7.5f, 0, 60)
                )),
                Routine.RoutineExercise(6, 3206, "Hammer Curl", 5, listOf(
                    Routine.RoutineExercise.SetConfig(47, WorkoutSet.SetType.NORMAL, 12, 12.5f, 1, 60),
                    Routine.RoutineExercise.SetConfig(48, WorkoutSet.SetType.NORMAL, 10, 12.5f, 0, 60)
                )),
                Routine.RoutineExercise(7, 3207, "Decline Crunch", 6, listOf(
                    Routine.RoutineExercise.SetConfig(49, WorkoutSet.SetType.NORMAL, 15, 5f, 1, 45),
                    Routine.RoutineExercise.SetConfig(50, WorkoutSet.SetType.NORMAL, 12, 5f, 0, 45)
                ))
            )
        )
    )

    private val routines = mutableListOf<Routine>()

    private val _routinesFlow = MutableStateFlow<List<Routine>>(emptyList())
    val routinesFlow: StateFlow<List<Routine>> = _routinesFlow.asStateFlow()

    private val _workouts = MutableStateFlow<List<Workout>>(emptyList())
    val completedWorkouts: StateFlow<List<Workout>> = _workouts.asStateFlow()

    suspend fun init(context: Context) {
        val preferences = context.dataStore.data.first()
        val routinesJson = preferences[ROUTINES_KEY]
        if (routinesJson != null) {
            try {
                val decoded = Json.decodeFromString<List<Routine>>(routinesJson)
                routines.clear()
                routines.addAll(decoded)
            } catch (e: Exception) {
                routines.clear()
                routines.addAll(defaultRoutines)
            }
        } else {
            routines.clear()
            routines.addAll(defaultRoutines)
        }
        _routinesFlow.value = routines.toList()

        val workoutsJson = preferences[WORKOUTS_KEY]
        if (workoutsJson != null) {
            try {
                val decoded = Json.decodeFromString<List<Workout>>(workoutsJson)
                _workouts.value = decoded
            } catch (e: Exception) {
                _workouts.value = emptyList()
            }
        }
    }

    private suspend fun saveRoutines(context: Context) {
        val json = Json.encodeToString(routines.toList())
        context.dataStore.edit { preferences ->
            preferences[ROUTINES_KEY] = json
        }
        _routinesFlow.value = routines.toList()
    }

    private suspend fun saveWorkouts(context: Context) {
        val json = Json.encodeToString(_workouts.value)
        context.dataStore.edit { preferences ->
            preferences[WORKOUTS_KEY] = json
        }
    }

    fun getRoutines(): Flow<List<Routine>> {
        return routinesFlow
    }

    suspend fun createRoutine(context: Context, routine: Routine): Long {
        val id = (routines.maxOfOrNull { it.id } ?: 0L) + 1L
        routines.add(Routine(id, routine.name, routine.exercises, routine.description))
        saveRoutines(context)
        return id
    }

    suspend fun deleteRoutine(context: Context, id: Long) {
        routines.removeAll { it.id == id }
        saveRoutines(context)
    }

    suspend fun getRoutineById(id: Long): Routine? {
        return routines.find { it.id == id }
    }

    suspend fun updateRoutine(context: Context, routine: Routine) {
        val index = routines.indexOfFirst { it.id == routine.id }
        if (index != -1) {
            routines[index] = routine
            saveRoutines(context)
        }
    }

    suspend fun startWorkout(routineId: Long): Workout {
        val routine = routines.find { it.id == routineId } ?: Routine(0, "Unknown", emptyList())
        val id = (_workouts.value.maxOfOrNull { it.id } ?: 0L) + 1L
        return Workout(
            id = id,
            routine = routine,
            date = System.currentTimeMillis(),
            sets = routine.exercises.flatMap { exercise ->
                exercise.sets.map { setConfig ->
                    WorkoutSet(
                        id = setConfig.id,
                        exerciseId = exercise.exerciseId,
                        exerciseName = exercise.exerciseName,
                        type = setConfig.type,
                        reps = setConfig.targetReps,
                        weight = setConfig.targetWeight,
                        rir = setConfig.targetRir,
                        inputType = exercise.inputType,
                        notes = null,
                        isCompleted = false
                    )
                }
            },
            notes = null
        )
    }

    suspend fun completeWorkout(context: Context, workout: Workout) {
        _workouts.update { it + workout }
        saveWorkouts(context)
    }

    fun getCompletedWorkouts(): Flow<List<Workout>> {
        return completedWorkouts
    }

    suspend fun saveWorkout(context: Context, workout: Workout) {
        completeWorkout(context, workout)
    }

    fun getWorkoutsByDateRange(startDate: Long, endDate: Long): Flow<List<Workout>> {
        return flow {
            val start = startDate
            val end = endDate
            emit(_workouts.value.filter { it.date in start..end }.sortedByDescending { it.date })
        }
    }
}

@Serializable
data class Workout(
    val id: Long,
    val routine: Routine,
    val date: Long,
    val sets: List<WorkoutSet>,
    val notes: String? = null
)
