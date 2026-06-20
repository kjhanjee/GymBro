package com.gymlogger.util

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.filled.Download

actual fun getCurrentTimeMillis(): Long = System.currentTimeMillis()

actual fun formatFloat(value: Float, decimals: Int): String {
    return String.format(Locale.US, "%.${decimals}f", value)
}

actual fun formatTimestamp(timestampMillis: Long, format: String): String {
    val sdf = SimpleDateFormat(format, Locale.getDefault())
    return sdf.format(Date(timestampMillis))
}

actual fun logError(tag: String, message: String, t: Throwable?) {
    Log.e(tag, message, t)
}

actual fun getDayOfWeek(timestampMillis: Long): Int {
    val calendar = java.util.Calendar.getInstance()
    calendar.timeInMillis = timestampMillis
    val day = calendar.get(java.util.Calendar.DAY_OF_WEEK)
    return if (day == java.util.Calendar.SUNDAY) 6 else day - 2
}

actual fun getStartOfWeekMillis(): Long {
    val calendar = java.util.Calendar.getInstance()
    calendar.firstDayOfWeek = java.util.Calendar.MONDAY
    calendar.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
    calendar.set(java.util.Calendar.MINUTE, 0)
    calendar.set(java.util.Calendar.SECOND, 0)
    calendar.set(java.util.Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

actual fun getStartOfDayMillis(timestampMillis: Long): Long {
    val cal = java.util.Calendar.getInstance()
    cal.timeInMillis = timestampMillis
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}

@Composable
actual fun ExportCsvIconButton(mealsProvider: () -> List<com.gymlogger.model.Meal>) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val createDocumentLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            coroutineScope.kotlinx.coroutines.launch {
                val latestMeals = mealsProvider()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(it)?.use { outputStream ->
                            java.io.OutputStreamWriter(outputStream, java.nio.charset.StandardCharsets.UTF_8).use { writer ->
                                writer.write(com.gymlogger.util.CsvExporter.exportMealsToCsv(latestMeals))
                                writer.flush()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ExportCsv", "Failed to save CSV", e)
                    }
                }
            }
        }
    }

    androidx.compose.material3.IconButton(onClick = { 
        val fileName = "meal_logs_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.csv"
        createDocumentLauncher.launch(fileName)
    }) {
        androidx.compose.material3.Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.Download,
            contentDescription = "Download CSV",
            tint = androidx.compose.ui.graphics.Color.White
        )
}

@Composable
actual fun rememberJsonImportLauncher(onResult: (String?) -> Unit): () -> Unit {
    val context = androidx.compose.ui.platform.LocalContext.current
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) {
            onResult(null)
            return@rememberLauncherForActivityResult
        }
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            onResult(jsonString)
        } catch (e: Exception) {
            onResult(null)
        }
    }
    return { launcher.launch("application/json") }
}
