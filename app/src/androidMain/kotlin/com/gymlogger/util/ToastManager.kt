package com.gymlogger.util

import android.widget.Toast
import com.gymlogger.AndroidPlatform

actual object ToastManager {
    actual fun showToast(message: String) {
        val context = AndroidPlatform.context
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
