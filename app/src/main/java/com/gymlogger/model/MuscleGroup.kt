package com.gymlogger.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import com.gymlogger.R

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

    @Composable
    fun getPainter(): Painter {
        return when (this) {
            CHEST -> painterResource(id = R.drawable.ic_chest)
            BACK -> painterResource(id = R.drawable.ic_back)
            SHOULDERS -> painterResource(id = R.drawable.ic_shoulders)
            BICEPS -> painterResource(id = R.drawable.ic_bicep)
            TRICEPS -> painterResource(id = R.drawable.ic_tricep)
            FOREARMS -> painterResource(id = R.drawable.ic_forearm)
            LEGS -> painterResource(id = R.drawable.ic_legs)
            CORE -> painterResource(id = R.drawable.ic_abs)
            CARDIO -> painterResource(id = R.drawable.ic_cardio)
        }
    }
}
