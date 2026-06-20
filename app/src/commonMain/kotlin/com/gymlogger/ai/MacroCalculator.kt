package com.gymlogger.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

expect object MacroCalculator {
    val isInitializing: StateFlow<Boolean>
    val isReady: StateFlow<Boolean>
    val downloadProgress: StateFlow<Float?>

    suspend fun init()
    suspend fun release()
    fun scheduleRelease()
    fun cancelRelease()

    suspend fun generateResponse(prompt: String): String?
    fun generateResponseStream(prompt: String): Flow<String>

    fun startChat()
    
    suspend fun summarizeMessages(conversationText: String): String?
    suspend fun calculateMacros(mealDescription: String, labelsInfo: String): String?
    fun sendChatMessageStream(prompt: String): Flow<AiStreamItem>
}

sealed class AiStreamItem {
    data class Thought(val delta: String) : AiStreamItem()
    data class Text(val delta: String) : AiStreamItem()
}
