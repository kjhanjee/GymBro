package com.gymlogger.util

import androidx.compose.runtime.Composable
import com.gymlogger.model.Meal

expect fun getCurrentTimeMillis(): Long
expect fun formatFloat(value: Float, decimals: Int): String
expect fun formatTimestamp(timestampMillis: Long, format: String): String
expect fun logError(tag: String, message: String, t: Throwable? = null)
expect fun getDayOfWeek(timestampMillis: Long): Int
expect fun getStartOfWeekMillis(): Long
expect fun getStartOfDayMillis(timestampMillis: Long): Long

@Composable
expect fun ExportCsvIconButton(mealsProvider: () -> List<Meal>)

@Composable
expect fun rememberJsonImportLauncher(onResult: (String?) -> Unit): () -> Unit

@Composable
expect fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit)
