package com.gymlogger.util

import androidx.compose.runtime.Composable
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSString
import androidx.compose.material.icons.filled.Download
import platform.Foundation.stringWithFormat
import platform.Foundation.timeIntervalSince1970

actual fun getCurrentTimeMillis(): Long {
    return (NSDate().timeIntervalSince1970 * 1000.0).toLong()
}

actual fun formatFloat(value: Float, decimals: Int): String {
    val formatString = "%." + decimals + "f"
    return NSString.stringWithFormat(formatString, value.toDouble())
}

actual fun formatTimestamp(timestampMillis: Long, format: String): String {
    val date = NSDate(timeIntervalSinceReferenceDate = timestampMillis / 1000.0 - 978307200.0)
    val formatter = NSDateFormatter()
    formatter.dateFormat = format
    return formatter.stringFromDate(date)
}

actual fun logError(tag: String, message: String, t: Throwable?) {
    println("[$tag] ERROR: $message ${t?.message ?: ""}")
}

actual fun getDayOfWeek(timestampMillis: Long): Int {
    val date = NSDate(timeIntervalSinceReferenceDate = timestampMillis / 1000.0 - 978307200.0)
    val calendar = platform.Foundation.NSCalendar.currentCalendar
    val components = calendar.components(platform.Foundation.NSWeekdayCalendarUnit, date)
    val day = components.weekday.toInt()
    return if (day == 1) 6 else day - 2
}

actual fun getStartOfWeekMillis(): Long {
    val calendar = platform.Foundation.NSCalendar.currentCalendar
    // 2 is Monday in iOS
    calendar.firstWeekday = 2u
    val now = platform.Foundation.NSDate()
    val components = calendar.components(
        platform.Foundation.NSYearCalendarUnit or 
        platform.Foundation.NSMonthCalendarUnit or 
        platform.Foundation.NSWeekOfYearCalendarUnit or 
        platform.Foundation.NSWeekdayCalendarUnit,
        now
    )
    components.weekday = 2
    val startOfWeek = calendar.dateFromComponents(components)
    return ((startOfWeek?.timeIntervalSince1970 ?: 0.0) * 1000).toLong()
}

actual fun getStartOfDayMillis(timestampMillis: Long): Long {
    val calendar = platform.Foundation.NSCalendar.currentCalendar
    val date = platform.Foundation.NSDate(timeIntervalSinceReferenceDate = timestampMillis / 1000.0 - 978307200.0)
    val startOfDay = calendar.startOfDayForDate(date)
    return (startOfDay.timeIntervalSince1970 * 1000).toLong()
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS doesn't have a default hardware back button to intercept like Android
}

@Composable
actual fun ExportCsvIconButton(mealsProvider: () -> List<com.gymlogger.model.Meal>) {
    androidx.compose.material3.IconButton(onClick = { 
        // iOS CSV export not implemented yet
    }) {
        androidx.compose.material3.Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.Download,
            contentDescription = "Download CSV",
            tint = androidx.compose.ui.graphics.Color.White
        )
    }
}

@Composable
actual fun rememberJsonImportLauncher(onResult: (String?) -> Unit): () -> Unit {
    return { 
        // iOS import not implemented yet
        onResult(null)
    }
}
