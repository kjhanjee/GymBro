package com.gymlogger.ui.components

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymlogger.service.WorkoutService
import java.util.Locale

@Composable
fun WorkoutMiniCard(
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var workoutService by remember { mutableStateOf<WorkoutService?>(null) }
    
    val connection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as WorkoutService.WorkoutBinder
                workoutService = binder.getService()
            }
            override fun onServiceDisconnected(name: ComponentName?) {
                workoutService = null
            }
        }
    }

    DisposableEffect(Unit) {
        val intent = Intent(context, WorkoutService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        onDispose {
            context.unbindService(connection)
        }
    }

    val isActiveState = workoutService?.isActive?.collectAsState(false)
    val isActive = isActiveState?.value ?: false
    
    if (!isActive) return

    val titleState = workoutService?.workoutTitle?.collectAsState("Tracked Workout")
    val title = titleState?.value ?: "Tracked Workout"

    val secondsElapsedState = workoutService?.secondsElapsed?.collectAsState(0L)
    val secondsElapsed = secondsElapsedState?.value ?: 0L

    val totalSetsState = workoutService?.totalSets?.collectAsState(0)
    val totalSets = totalSetsState?.value ?: 0

    val completedSetsState = workoutService?.completedSets?.collectAsState(0)
    val completedSets = completedSetsState?.value ?: 0

    val restSecondsRemainingState = workoutService?.restSecondsRemaining?.collectAsState(0)
    val restSecondsRemaining = restSecondsRemainingState?.value ?: 0

    val isRestTimerActiveState = workoutService?.isRestTimerActive?.collectAsState(false)
    val isRestTimerActive = isRestTimerActiveState?.value ?: false

    val timerText = remember(secondsElapsed) {
        val h = secondsElapsed / 3600
        val m = (secondsElapsed % 3600) / 60
        val s = secondsElapsed % 60
        if (h > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", m, s)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = "Sets: $completedSets / $totalSets",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isRestTimerActive) {
                    val rm = restSecondsRemaining / 60
                    val rs = restSecondsRemaining % 60
                    val restText = String.format(Locale.getDefault(), "%02d:%02d", rm, rs)
                    
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(end = 8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = "Rest: $restText",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = timerText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }
    }
}
