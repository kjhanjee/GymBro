package com.gymlogger.ai

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

object MacroCalculator {
    private const val TAG = "MacroCalculator"
    private const val MODEL_FILE_NAME = "gemma-4-E2B-it.litertlm"
    
    private var engine: Engine? = null
    
    private val _isInitializing = MutableStateFlow(false)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress: StateFlow<Float?> = _downloadProgress.asStateFlow()

    // Use a dedicated single thread with lower priority to reduce impact on the system
    private val aiExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "AI-Inference-Thread").apply {
            priority = Thread.MIN_PRIORITY
        }
    }.asCoroutineDispatcher()

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
            
            appContext.assets.open(MODEL_FILE_NAME).use { input ->
                FileOutputStream(outFile).use { output ->
                    val buffer = ByteArray(1024 * 1024) 
                    var bytesCopied: Long = 0
                    var bytes = input.read(buffer)
                    while (bytes >= 0) {
                        output.write(buffer, 0, bytes)
                        bytesCopied += bytes
                        _downloadProgress.value = bytesCopied.toFloat() / assetSize
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
            val outFile = File(appContext.filesDir, MODEL_FILE_NAME)
            if (!outFile.exists()) {
                Log.e(TAG, "Model file not found at ${outFile.absolutePath}")
                _isInitializing.value = false
                return@withContext
            }

            val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memoryInfo)
            Log.d(TAG, "Memory status: Avail: ${memoryInfo.availMem / 1024 / 1024}MB / Total: ${memoryInfo.totalMem / 1024 / 1024}MB")

            try {
                Log.d(TAG, "Attempting to initialize LiteRT Engine with GPU...")
                val gpuConfig = EngineConfig(
                    modelPath = outFile.absolutePath,
                    backend = Backend.GPU(),
                    cacheDir = appContext.cacheDir.path
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
                    cacheDir = appContext.cacheDir.path
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

    suspend fun generateResponse(prompt: String): String? = withContext(aiExecutor) {
        val currentEngine = engine
        if (currentEngine == null) {
            Log.w(TAG, "generateResponse: engine is null")
            return@withContext null
        }

        try {
            Log.d(TAG, "Generating response for prompt on thread: ${Thread.currentThread().name}")
            val conversation = currentEngine.createConversation()
            val response = conversation.sendMessage(prompt)
            val fullResponse = (response.contents.contents.firstOrNull() as? Content.Text)?.text ?: ""
            conversation.close()
            fullResponse
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed", e)
            null
        }
    }

    suspend fun calculateMacros(mealDescription: String, labelsInfo: String): String? {
        val prompt = """
            System: You are an expert Dietician. You need to analyze the meals provided by the user and provide the output in the below JSON format: 
            {"calories": float, "protein": float, "carbs": float, "fats": float}
            Do not include any explanation or markdown.
            Use the below label information for analyzing the meals provided by the user, if the corresponding items are present in the meal logged: 
            $labelsInfo
            User: Analyze this meal: $mealDescription
        """.trimIndent()
        
        return generateResponse(prompt)
    }
}
