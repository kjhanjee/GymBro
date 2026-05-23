package com.gymlogger.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymlogger.ai.MacroCalculator
import com.gymlogger.data.RoutineRepository
import com.gymlogger.data.SettingsRepository
import com.gymlogger.ui.components.GymBroTopAppBar
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
enum class ChatRole {
    SYSTEM, USER, AI, SUMMARY
}

@Serializable
data class ChatMessage(
    val text: String,
    val role: ChatRole,
    val thought: String = ""
) {
    val isUser: Boolean get() = role == ChatRole.USER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiTrainerChatScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var isThinking by remember { mutableStateOf(false) }

    val isAiReady by MacroCalculator.isReady.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val trainingContextNames by SettingsRepository.getTrainingContext(context).collectAsStateWithLifecycle(initialValue = emptyList())
    val allRoutines by RoutineRepository.routinesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        MacroCalculator.startChat()
        
        // Restore chat history
        val historyJson = SettingsRepository.getChatHistory(context)
        if (!historyJson.isNullOrEmpty()) {
            try {
                val restored = Json.decodeFromString<List<ChatMessage>>(historyJson)
                messages.clear()
                messages.addAll(restored)
            } catch (e: Exception) {
                android.util.Log.e("AiTrainerChatScreen", "Failed to restore chat history", e)
            }
        }

        if (!isAiReady) {
            MacroCalculator.init(context)
        }
    }

    // Persist chat history whenever messages change
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            val historyJson = Json.encodeToString(messages.toList())
            SettingsRepository.setChatHistory(context, historyJson)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            scope.launch {
                MacroCalculator.release()
            }
        }
    }

    suspend fun getSystemPrompt(context: android.content.Context): String {
        val height = SettingsRepository.getHeight(context).first()
        val weight = SettingsRepository.getWeight(context).first()
        val gender = SettingsRepository.getGender(context).first()
        val targetWeight = SettingsRepository.getTargetWeight(context).first()
        val goal = SettingsRepository.getGoal(context).first()
        val weightUnit = SettingsRepository.getWeightUnit(context).first()
        val scheduleNames = SettingsRepository.getTrainingContext(context).first()
        val allRoutines = RoutineRepository.getRoutines().first()
        val scheduledRoutines = allRoutines.filter { it.name in scheduleNames }

        val physiqueJson = """{"height": $height, "heightUnit": "cm", "weight": $weight, "weightUnit": "kg", "gender": "$gender"}"""
        val objectiveJson = """{"targetWeight": $targetWeight, "targetWeightUnit": "kg", "goal": "$goal"}"""

        val currentSchedule = if (scheduledRoutines.isEmpty()) {
            "No routines scheduled."
        } else {
            val unit = weightUnit.name.lowercase()
            scheduledRoutines.joinToString("\n") { routine ->
                val exerciseDetails = routine.exercises.joinToString("\n") { exercise ->
                    val setsDetails = exercise.sets.joinToString(", ") { set ->
                        val weight = set.targetWeight?.toString() ?: "?"
                        val reps = set.targetReps?.toString() ?: "?"
                        val rir = set.targetRir?.toString() ?: "?"
                        "$weight$unit x $reps @ RIR $rir (Rest: ${set.restTime}s)"
                    }
                    "  * ${exercise.exerciseName}: $setsDetails"
                }
                "- ${routine.name}:\n$exerciseDetails"
            }
        }

        return """
            <|turn>system
            You are a Gym Instructor helping me create perfect gym schedule, optimizing my workouts meeting my targets, and also helping rate and verify my current workouts if asked. All of this needs to be done as per my current Physique.

            Ensure that you are objective in your opinions and do not simply agree with me for everything. You need to provide neutral and unbiased analysis for my workouts and help me improve further.

            CRITICAL: Do not repeat my Physique, Schedule, or Objective details back to me in your response unless I specifically ask for them or it's absolutely necessary to explain a specific piece of advice. Focus on answering my question directly and naturally.

            My Physique: $physiqueJson

            My Current Schedule (with exercises):
            $currentSchedule

            My Objective: $objectiveJson
            <|think|><turn|>""".trimIndent()
    }

    suspend fun compactMessages(
        messages: SnapshotStateList<ChatMessage>,
        scope: CoroutineScope
    ) {
        val systemMessage = messages.firstOrNull { it.role == ChatRole.SYSTEM }
        val lastThree = messages.takeLast(3)
        val toSummarize = messages.filter { 
            it.role != ChatRole.SYSTEM && !lastThree.contains(it) && it.role != ChatRole.SUMMARY 
        }

        if (toSummarize.isNotEmpty()) {
            val conversationText = toSummarize.joinToString("\n") { "${it.role}: ${it.text}" }
            val summaryText = MacroCalculator.summarizeMessages(conversationText) ?: "Summary of previous conversation."
            
            val newMessages = mutableListOf<ChatMessage>()
            if (systemMessage != null) newMessages.add(systemMessage)
            newMessages.add(ChatMessage(summaryText, ChatRole.SUMMARY))
            newMessages.addAll(lastThree)
            
            messages.clear()
            messages.addAll(newMessages)
            
            // Re-prime the AI engine with the compacted state
            MacroCalculator.startChat()
            val compactedPrompt = buildString {
                if (systemMessage != null) append(systemMessage.text).append("\n")
                append("<|turn>user\nHere is a summary of our previous conversation: $summaryText\nAnd here are the last few messages for context:\n")
                lastThree.forEach { msg ->
                    val role = if (msg.role == ChatRole.USER) "user" else "model"
                    append("<|turn>$role\n${msg.text}<turn|>\n")
                }
                append("<|turn>model\nUnderstood. I have the context of our previous discussion. How can I help you further?<turn|>")
            }
            // We send this as a "hidden" turn to the engine to establish state
            MacroCalculator.sendChatMessageStream(compactedPrompt).collect { /* consume but don't show */ }
        }
    }

    fun sendMessage() {
        if (inputText.isBlank() || isThinking) return

        keyboardController?.hide()
        focusManager.clearFocus()
        val userText = inputText
        
        scope.launch {
            val isFirstTurn = messages.none { it.role == ChatRole.USER }

            // Ensure system message is present
            if (messages.none { it.role == ChatRole.SYSTEM }) {
                val systemPrompt = getSystemPrompt(context)
                messages.add(0, ChatMessage(systemPrompt, ChatRole.SYSTEM))
            }

            messages.add(ChatMessage(userText, ChatRole.USER))
            inputText = ""
            isThinking = true

            val prompt = if (isFirstTurn) {
                val systemText = messages.first { it.role == ChatRole.SYSTEM }.text
                "$systemText\n<|turn>user\n$userText<turn|>\n<|turn>model"
            } else {
                "<|turn>user\n$userText<turn|>\n<|turn>model"
            }

            val aiMessage = ChatMessage("", ChatRole.AI)
            messages.add(aiMessage)
            val aiMessageIndex = messages.size - 1
            var responseText = ""
            var responseThought = ""

            MacroCalculator.sendChatMessageStream(prompt).collect { item ->
                when (item) {
                    is MacroCalculator.AiStreamItem.Thought -> {
                        responseThought += item.delta
                        messages[aiMessageIndex] = aiMessage.copy(text = responseText, thought = responseThought)
                    }
                    is MacroCalculator.AiStreamItem.Text -> {
                        responseText += item.delta
                        messages[aiMessageIndex] = aiMessage.copy(text = responseText, thought = responseThought)
                    }
                }
                isThinking = false
                
                // Keep scrolling as text comes in
                launch {
                    try {
                        listState.scrollToItem(messages.size - 1)
                    } catch (e: Exception) {
                        // Ignore scroll interruption
                    }
                }
            }

            if (responseText.isEmpty()) {
                messages[aiMessageIndex] = aiMessage.copy(text = "I'm having trouble connecting right now, bro. Try again?")
            } else {
                // Check for compaction after successful AI response
                if (messages.size > 15) {
                    compactMessages(messages, scope)
                }
            }
            isThinking = false
        }
    }

    Scaffold(
        topBar = {
            GymBroTopAppBar(
                title = "AI Gym Instructor",
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = {
                            messages.clear()
                            MacroCalculator.startChat()
                            scope.launch {
                                SettingsRepository.setChatHistory(context, "")
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "New Chat",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!isAiReady) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("AI is warming up...", color = Color.White)
                }
            } else {
                LaunchedEffect(messages.size, isThinking) {
                    val targetIndex = if (isThinking) messages.size else messages.size - 1
                    if (targetIndex >= 0) {
                        listState.animateScrollToItem(targetIndex)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    reverseLayout = false,
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages.filter { it.role == ChatRole.USER || it.role == ChatRole.AI }) { message ->
                        ChatBubble(message)
                    }
                    if (isThinking) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.CenterStart) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }

            // Input Area
            Surface(
                color = Color(0xFF1C1C1E),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Training Context Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            TextButton(
                                onClick = { expanded = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add routine to context", fontSize = 13.sp)
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(Color(0xFF2C2C2E))
                            ) {
                                allRoutines.forEach { routine ->
                                    DropdownMenuItem(
                                        text = { Text(routine.name, color = Color.White) },
                                        onClick = {
                                            if (!trainingContextNames.contains(routine.name)) {
                                                scope.launch {
                                                    SettingsRepository.setTrainingContext(
                                                        context,
                                                        trainingContextNames + routine.name
                                                    )
                                                }
                                            }
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(end = 12.dp)
                        ) {
                            items(trainingContextNames) { name ->
                                InputChip(
                                    selected = true,
                                    onClick = {
                                        scope.launch {
                                            SettingsRepository.setTrainingContext(
                                                context,
                                                trainingContextNames.filter { it != name }
                                            )
                                        }
                                    },
                                    label = { Text(name, fontSize = 12.sp) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    colors = InputChipDefaults.inputChipColors(
                                        selectedContainerColor = Color(0xFF2C2C2E),
                                        selectedLabelColor = Color.White,
                                        selectedTrailingIconColor = Color.White
                                    ),
                                    border = null
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .padding(bottom = 12.dp)
                            .navigationBarsPadding()
                            .imePadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Ask your trainer...", color = Color(0xFF8E8E93)) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF2C2C2E),
                                unfocusedContainerColor = Color(0xFF2C2C2E),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(24.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { sendMessage() })
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { sendMessage() },
                            enabled = inputText.isNotBlank() && !isThinking,
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary,
                                disabledContentColor = Color(0xFF48484A)
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val horizontalAlignment = if (message.role == ChatRole.USER) Alignment.End else Alignment.Start
    val bgColor = if (message.role == ChatRole.USER) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    val textColor = if (message.role == ChatRole.USER) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = horizontalAlignment
    ) {
        if (message.role == ChatRole.AI && message.thought.isNotEmpty()) {
            Surface(
                color = Color(0xFF1C1C1E),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF3A3A3C)),
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .widthIn(max = 280.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI THOUGHTS",
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = message.thought,
                        color = Color.LightGray.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        if (message.text.isNotEmpty()) {
            Surface(
                color = bgColor,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (message.role == ChatRole.USER) 16.dp else 0.dp,
                    bottomEnd = if (message.role == ChatRole.USER) 0.dp else 16.dp
                ),
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                MarkdownText(
                    text = message.text,
                    modifier = Modifier.padding(12.dp),
                    color = textColor,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun MarkdownText(text: String, modifier: Modifier, color: Color, fontSize: TextUnit) {
    val annotatedString = remember(text) {
        buildAnnotatedString {
            val lines = text.split("\n")
            lines.forEachIndexed { lineIndex, line ->
                var currentLine = line
                
                // Bullet point handling
                val isBullet = currentLine.trim().startsWith("- ") || currentLine.trim().startsWith("* ")
                if (isBullet) {
                    append("  • ")
                    currentLine = currentLine.trim().substring(2)
                }
                
                // Bold handling
                val boldRegex = Regex("""\*\*(.*?)\*\*""")
                var lastIndex = 0
                boldRegex.findAll(currentLine).forEach { match ->
                    append(currentLine.substring(lastIndex, match.range.first))
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(match.groupValues[1])
                    }
                    lastIndex = match.range.last + 1
                }
                append(currentLine.substring(lastIndex))
                
                if (lineIndex < lines.size - 1) {
                    append("\n")
                }
            }
        }
    }
    Text(
        text = annotatedString,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        lineHeight = (fontSize.value * 1.4).sp
    )
}
