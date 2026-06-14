package com.gymlogger.ai

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

object MacroCalculator {
    private const val TAG = "MacroCalculator"
    private const val MODEL_FILE_NAME = "gemma-4-E2B-it.litertlm"
    
    sealed class AiStreamItem {
        data class Thought(val delta: String) : AiStreamItem()
        data class Text(val delta: String) : AiStreamItem()
    }
    
    private var engine: Engine? = null
    
    private val _isInitializing = MutableStateFlow(false)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress: StateFlow<Float?> = _downloadProgress.asStateFlow()

    // Use a dedicated single thread for ALL AI operations to prevent native concurrency crashes
    private val aiExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "AI-Inference-Thread").apply {
            priority = Thread.MAX_PRIORITY // Give it priority to finish prefill quickly
        }
    }.asCoroutineDispatcher()

    private val isProcessing = AtomicBoolean(false)

    suspend fun prepareModel(context: Context): Boolean = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val outFile = File(appContext.filesDir, MODEL_FILE_NAME)
        
        try {
            val assetFd = appContext.assets.openFd(MODEL_FILE_NAME)
            val assetSize = assetFd.length
            assetFd.close()

            if (outFile.exists() && outFile.length() == assetSize) {
                Log.d(TAG, "Model file already exists and size matches.")
                return@withContext true
            }
            
            Log.d(TAG, "Copying model from assets: $assetSize bytes")
            _downloadProgress.value = 0f
            var lastReportedProgress = -1f
            
            appContext.assets.open(MODEL_FILE_NAME).use { input ->
                FileOutputStream(outFile).use { output ->
                    val buffer = ByteArray(1024 * 1024) 
                    var bytesCopied: Long = 0
                    var bytes = input.read(buffer)
                    while (bytes >= 0) {
                        output.write(buffer, 0, bytes)
                        bytesCopied += bytes
                        
                        val currentProgress = bytesCopied.toFloat() / assetSize
                        if (currentProgress - lastReportedProgress >= 0.01f || currentProgress >= 1f) {
                            _downloadProgress.value = currentProgress
                            lastReportedProgress = currentProgress
                        }

                        bytes = input.read(buffer)
                    }
                }
            }
            
            _downloadProgress.value = null
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy model from assets", e)
            _downloadProgress.value = null
            return@withContext false
        }
    }

    suspend fun init(context: Context) = withContext(aiExecutor) {
        if (engine != null || _isInitializing.value) return@withContext
        
        val appContext = context.applicationContext
        _isInitializing.value = true
        Log.d(TAG, "Starting LiteRT Engine init on thread: ${Thread.currentThread().name}")
        
        try {
            if (!prepareModel(appContext)) {
                Log.e(TAG, "Failed to prepare model file")
                _isInitializing.value = false
                return@withContext
            }

            val outFile = File(appContext.filesDir, MODEL_FILE_NAME)
            val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memoryInfo)
            Log.d(TAG, "Memory status: Avail: ${memoryInfo.availMem / 1024 / 1024}MB / Total: ${memoryInfo.totalMem / 1024 / 1024}MB")

            try {
                Log.d(TAG, "Attempting to initialize LiteRT Engine with GPU...")
                val gpuConfig = EngineConfig(
                    modelPath = outFile.absolutePath,
                    backend = Backend.GPU(),
                    cacheDir = appContext.cacheDir.path,
                    maxNumTokens = 128000
                )
                val newEngine = Engine(gpuConfig)
                newEngine.initialize()
                engine = newEngine
                Log.d(TAG, "GPU initialization successful")
            } catch (gpuError: Throwable) {
                Log.w(TAG, "GPU initialization failed, falling back to CPU", gpuError)
                
                val cpuConfig = EngineConfig(
                    modelPath = outFile.absolutePath,
                    backend = Backend.CPU(),
                    cacheDir = appContext.cacheDir.path,
                    maxNumTokens = 128000
                )
                val newEngine = Engine(cpuConfig)
                newEngine.initialize()
                engine = newEngine
                Log.d(TAG, "CPU initialization successful")
            }

            _isReady.value = true
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize LiteRT Engine on both GPU and CPU", e)
            _isReady.value = false
        } finally {
            _isInitializing.value = false
        }
    }

    suspend fun release() = withContext(aiExecutor) {
        engine?.close()
        engine = null
        chatConversation?.close()
        chatConversation = null
        _isReady.value = false
        Log.d(TAG, "Engine and chat conversation released")
    }

    suspend fun generateResponse(prompt: String): String? = withContext(aiExecutor) {
        if (isProcessing.getAndSet(true)) {
            Log.w(TAG, "Already processing an AI request, skipping.")
            return@withContext null
        }
        
        try {
            val currentEngine = engine ?: return@withContext null
            Log.d(TAG, "Generating response for prompt: $prompt")
            
            val conversationConfig = ConversationConfig(
                samplerConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 1.0)
            )
            val conversation = currentEngine.createConversation(conversationConfig)
            val response = conversation.sendMessage(prompt, mapOf("max_tokens" to 128000))
            val fullResponse = (response.contents.contents.firstOrNull() as? Content.Text)?.text ?: ""
            conversation.close()
            
            Log.d(TAG, "Full AI Response: $fullResponse")
            
            // Robust cleaning: remove anything before and including <channel|>
            val cleaned = if (fullResponse.contains("<channel|>")) {
                fullResponse.substringAfter("<channel|>").trim()
            } else {
                // If it produced thoughts but didn't end properly, or produced no thoughts
                fullResponse.replace("<|channel>thought", "").trim()
            }
            cleaned
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed", e)
            null
        } finally {
            isProcessing.set(false)
        }
    }

    private fun streamFromConversation(
        conversation: com.google.ai.edge.litertlm.Conversation,
        prompt: String
    ): Flow<AiStreamItem> = callbackFlow {
        Log.d(TAG, ">>> SENDING PROMPT TO MODEL:\n$prompt\n<<< END PROMPT")
        try {
            var lastYieldedThought = ""
            var lastYieldedText = ""
            
            conversation.sendMessageAsync(prompt, object : com.google.ai.edge.litertlm.MessageCallback {
                override fun onMessage(message: com.google.ai.edge.litertlm.Message) {
                    val fullText = (message.contents.contents.firstOrNull() as? Content.Text)?.text ?: ""
                    Log.d(TAG, "RAW MODEL OUTPUT CHUNK: $fullText")
                    
                    // 1. Handle Thoughts: Everything between <|channel>thought and <channel|>
                    if (fullText.contains("<|channel>thought")) {
                        val thoughtPart = fullText.substringAfter("<|channel>thought").substringBefore("<channel|>")
                        val delta = if (thoughtPart.startsWith(lastYieldedThought)) {
                            thoughtPart.substring(lastYieldedThought.length)
                        } else {
                            thoughtPart
                        }
                        
                        if (delta.isNotEmpty()) {
                            Log.d(TAG, "THOUGHT DELTA: $delta")
                            trySend(AiStreamItem.Thought(delta))
                            lastYieldedThought = thoughtPart
                        }
                    }
                    
                    // 2. Handle Text: Everything after <channel|> OR everything if no thought tag seen
                    val textPart = when {
                        fullText.contains("<channel|>") -> {
                            fullText.substringAfter("<channel|>")
                        }
                        !fullText.contains("<|channel>thought") -> {
                            // Model isn't using thinking channel or hasn't started it
                            fullText
                        }
                        else -> "" // Inside thought channel, no text to yield
                    }

                    val delta = if (textPart.startsWith(lastYieldedText)) {
                        textPart.substring(lastYieldedText.length)
                    } else {
                        textPart
                    }

                    if (delta.isNotEmpty()) {
                        Log.d(TAG, "TEXT DELTA: $delta")
                        trySend(AiStreamItem.Text(delta))
                        lastYieldedText = textPart
                    }
                }

                override fun onDone() {
                    Log.d(TAG, "MODEL STREAM DONE")
                    close()
                }
                override fun onError(throwable: Throwable) {
                    Log.e(TAG, "MODEL STREAM ERROR", throwable)
                    close(throwable)
                }
            }, mapOf("max_tokens" to 128000, "max_output_tokens" to 128000))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start sendMessageAsync", e)
            close(e)
        }
        awaitClose { }
    }

    fun generateResponseStream(prompt: String): Flow<String> = channelFlow {
        withContext(aiExecutor) {
            if (isProcessing.getAndSet(true)) {
                Log.w(TAG, "Already processing, skipping stream.")
                return@withContext
            }
            try {
                val currentEngine = engine ?: return@withContext
                val conversation = currentEngine.createConversation(ConversationConfig())
                streamFromConversation(conversation, prompt).collect { item ->
                    if (item is AiStreamItem.Text) send(item.delta)
                }
                conversation.close()
            } finally {
                isProcessing.set(false)
            }
        }
    }.flowOn(aiExecutor)

    private var chatConversation: com.google.ai.edge.litertlm.Conversation? = null
    private var releaseJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun scheduleRelease(context: Context) {
        releaseJob?.cancel()
        releaseJob = scope.launch {
            delay(5 * 60 * 1000)
            release()
        }
    }

    fun cancelRelease() {
        releaseJob?.cancel()
        releaseJob = null
    }

    fun startChat() {
        chatConversation?.close()
        chatConversation = null
        Log.d(TAG, "Chat conversation reset")
    }

    fun sendChatMessageStream(prompt: String): Flow<AiStreamItem> = channelFlow {
        withContext(aiExecutor) {
            if (isProcessing.getAndSet(true)) {
                Log.w(TAG, "Already processing, skipping chat stream.")
                return@withContext
            }
            try {
                val currentEngine = engine ?: return@withContext
                if (chatConversation == null) {
                    chatConversation = currentEngine.createConversation(ConversationConfig())
                }
                streamFromConversation(chatConversation!!, prompt).collect { send(it) }
            } finally {
                isProcessing.set(false)
            }
        }
    }.flowOn(aiExecutor)

    suspend fun summarizeMessages(conversationText: String): String? = withContext(aiExecutor) {
        if (isProcessing.getAndSet(true)) return@withContext null
        try {
            val currentEngine = engine ?: return@withContext null
            val prompt = """
                <|turn>system
                Summarize the following conversation between a user and an AI Gym Instructor. 
                Be concise and capture the key points, progress, and agreed-upon plans.
                <|think|><turn|>
                <|turn>user
                $conversationText
                <turn|>
                <|turn>model
            """.trimIndent()

            val conversation = currentEngine.createConversation(ConversationConfig())
            val response = conversation.sendMessage(prompt, mapOf("max_tokens" to 128000))
            val fullResponse = (response.contents.contents.firstOrNull() as? Content.Text)?.text ?: ""
            conversation.close()
            
            // Extract content after thought tag if present
            if (fullResponse.contains("<channel|>")) {
                fullResponse.substringAfter("<channel|>").trim()
            } else {
                fullResponse.replace("<|channel>thought", "").trim()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Summarization failed", e)
            null
        } finally {
            isProcessing.set(false)
        }
    }

    suspend fun calculateMacros(mealDescription: String, labelsInfo: String): String? {
        val prompt = """
            <|turn>system
            You are an expert Dietician. You need to analyze the meals provided by the user and provide the output in the below JSON format: 
            {"calories": float, "protein": float, "carbs": float, "fats": float, "fibre": float, "sugar": float, "vitaminB": float, "vitaminD": float, "omega": float, "vitaminC": float, "iron": float, "potassium": float, "magnesium": float, "sodium": float}
            Do not include any explanation or markdown. 
            Units: calories in kcal, protein/carbs/fats/fibre/sugar in grams (sugar field refers to total refined sugar), vitaminB in mg, vitaminD in mcg, omega in mg, vitaminC in mg, iron in mg, potassium in mg, magnesium in mg, sodium in mg.
            Use the below label information for analyzing the meals provided by the user, if the corresponding items are present in the meal logged: 
            $labelsInfo
            <|think|><turn|>
            <|turn>user
            Analyze this meal: $mealDescription
            <turn|>
            <|turn>model
        """.trimIndent()
        
        return generateResponse(prompt)
    }
}
