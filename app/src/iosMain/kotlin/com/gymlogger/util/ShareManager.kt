package com.gymlogger.util

import com.gymlogger.model.Routine
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

actual object ShareManager {
    actual fun shareWorkout(htmlContent: String) {
        val window = UIApplication.sharedApplication.keyWindow
        val rootViewController = window?.rootViewController
        val activityViewController = UIActivityViewController(listOf(htmlContent), null)
        rootViewController?.presentViewController(activityViewController, animated = true, completion = null)
    }

    actual fun shareRoutine(routine: Routine) {
        // Simple string serialization for iOS
        val textToShare = "Routine: ${routine.name}"
        val window = UIApplication.sharedApplication.keyWindow
        val rootViewController = window?.rootViewController
        val activityViewController = UIActivityViewController(listOf(textToShare), null)
        rootViewController?.presentViewController(activityViewController, animated = true, completion = null)
    }
}
