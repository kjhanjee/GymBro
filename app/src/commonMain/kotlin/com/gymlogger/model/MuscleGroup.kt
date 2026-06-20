package com.gymlogger.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class MuscleGroup {
    CHEST,
    BACK,
    SHOULDERS,
    LEGS,
    BICEPS,
    TRICEPS,
    FOREARMS,
    CORE,
    CARDIO;

    val icon: ImageVector
        get() = when (this) {
            CHEST -> Icons.Default.FitnessCenter
            BACK -> Icons.Default.FormatAlignJustify
            SHOULDERS -> Icons.Default.VerticalAlignTop
            LEGS -> Icons.AutoMirrored.Filled.DirectionsWalk
            BICEPS -> Icons.Default.Architecture
            TRICEPS -> Icons.Default.Settings
            FOREARMS -> Icons.Default.Handyman
            CORE -> Icons.Default.CenterFocusStrong
            CARDIO -> Icons.AutoMirrored.Filled.DirectionsRun
        }


}
