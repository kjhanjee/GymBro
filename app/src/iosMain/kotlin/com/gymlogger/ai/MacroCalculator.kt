package com.gymlogger.ai

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf

interface IosAiEngine {
    suspend fun generateResponse(prompt: String): String?
    fun startChat()
    suspend fun summarizeMessages(conversationText: String): String?
    suspend fun calculateMacros(mealDescription: String, labelsInfo: String): String?
    
    fun generateResponseStream(prompt: String, onToken: (String) -> Unit, onComplete: () -> Unit, onError: (String) -> Unit)
    fun sendChatMessageStream(prompt: String, onTextToken: (String) -> Unit, onThoughtToken: (String) -> Unit, onComplete: () -> Unit, onError: (String) -> Unit)
    
    suspend fun initEngine()
    suspend fun releaseEngine()
}

actual object MacroCalculator {
    var activeEngine: IosAiEngine? = null

    private val _isInitializing = MutableStateFlow(false)
    actual val isInitializing: StateFlow<Boolean> = _isInitializing
    
    private val _isReady = MutableStateFlow(false)
    actual val isReady: StateFlow<Boolean> = _isReady
    
    private val _downloadProgress = MutableStateFlow<Float?>(null)
    actual val downloadProgress: StateFlow<Float?> = _downloadProgress

    actual suspend fun init() {
        _isInitializing.value = true
        activeEngine?.initEngine()
        _isReady.value = activeEngine != null
        _isInitializing.value = false
    }

    actual suspend fun release() {
        activeEngine?.releaseEngine()
        _isReady.value = false
    }

    actual fun scheduleRelease() {}
    actual fun cancelRelease() {}

    actual suspend fun generateResponse(prompt: String): String? {
        return activeEngine?.generateResponse(prompt) ?: "AI features are currently unavailable on iOS."
    }

    actual fun generateResponseStream(prompt: String): Flow<String> = callbackFlow {
        if (activeEngine == null) {
            trySend("AI features are currently unavailable on iOS.")
            close()
            return@callbackFlow
        }
        activeEngine?.generateResponseStream(
            prompt,
            onToken = { trySend(it) },
            onComplete = { close() },
            onError = { close(Exception(it)) }
        )
        awaitClose { }
    }

    actual fun startChat() {
        activeEngine?.startChat()
    }

    actual suspend fun summarizeMessages(conversationText: String): String? {
        return activeEngine?.summarizeMessages(conversationText) ?: "Summary unavailable."
    }

    actual suspend fun calculateMacros(mealDescription: String, labelsInfo: String): String? {
        return activeEngine?.calculateMacros(mealDescription, labelsInfo)
    }

    actual fun sendChatMessageStream(prompt: String): Flow<AiStreamItem> = callbackFlow {
        if (activeEngine == null) {
            trySend(AiStreamItem.Text("AI features are currently unavailable on iOS."))
            close()
            return@callbackFlow
        }
        activeEngine?.sendChatMessageStream(
            prompt,
            onTextToken = { trySend(AiStreamItem.Text(it)) },
            onThoughtToken = { trySend(AiStreamItem.Thought(it)) },
            onComplete = { close() },
            onError = { close(Exception(it)) }
        )
        awaitClose { }
    }
}
