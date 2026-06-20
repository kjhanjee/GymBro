package com.gymlogger.util

import android.content.Intent
import androidx.core.content.FileProvider
import com.gymlogger.AndroidPlatform
import com.gymlogger.model.Routine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

actual object ShareManager {
    actual fun shareWorkout(htmlContent: String) {
        val context = AndroidPlatform.context
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/html"
            putExtra(Intent.EXTRA_TEXT, htmlContent)
            putExtra(Intent.EXTRA_HTML_TEXT, htmlContent)
        }
        val intent = Intent.createChooser(shareIntent, "Share Workout").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    actual fun shareRoutine(routine: Routine) {
        val context = AndroidPlatform.context
        try {
            val json = Json { prettyPrint = true }.encodeToString(routine)
            val fileName = "${routine.name.replace(" ", "_")}_routine.json"
            
            val cachePath = File(context.cacheDir, "shared_routines")
            cachePath.mkdirs()
            
            val file = File(cachePath, fileName)
            file.writeText(json)
            
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Sharing Routine: ${routine.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val intent = Intent.createChooser(shareIntent, "Share Routine via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
