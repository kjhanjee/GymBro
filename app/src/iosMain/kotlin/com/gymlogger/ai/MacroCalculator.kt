package com.gymlogger.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

actual object MacroCalculator {
    private val _isInitializing = MutableStateFlow(false)
    actual val isInitializing: StateFlow<Boolean> = _isInitializing
    
    private val _isReady = MutableStateFlow(true) // Stub is always ready
    actual val isReady: StateFlow<Boolean> = _isReady
    
    private val _downloadProgress = MutableStateFlow<Float?>(null)
    actual val downloadProgress: StateFlow<Float?> = _downloadProgress

    actual suspend fun init() {
        // Stub for iOS - LiteRT is not yet available for iOS KMP in this project.
        println("MacroCalculator stub initialized on iOS")
    }

    actual suspend fun release() {
    }

    actual fun scheduleRelease() {
    }

    actual fun cancelRelease() {
    }

    actual suspend fun generateResponse(prompt: String): String? {
        return "AI features are currently unavailable on iOS."
    }

    actual fun generateResponseStream(prompt: String): Flow<String> {
        return flowOf("AI features are currently unavailable on iOS.")
    }

    actual fun startChat() {
    }

    actual suspend fun summarizeMessages(conversationText: String): String? {
        return "Summary unavailable."
    }

    actual suspend fun calculateMacros(mealDescription: String, labelsInfo: String): String? {
        return null // Return null to fallback to empty macros
    }

    actual fun sendChatMessageStream(prompt: String): Flow<AiStreamItem> {
        return flowOf(AiStreamItem.Text("AI features are currently unavailable on iOS."))
    }
}
