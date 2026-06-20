package com.gymlogger.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymlogger.ai.MacroCalculator
import com.gymlogger.data.MealRepository
import com.gymlogger.ui.components.GymBroTopAppBar
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

@Serializable
enum class MealType {
    breakfast,
    lunch,
    dinner,
    snack
}

@Serializable
data class OptimizedMeal(
    val dish: String,
    @SerialName("meal type")
    @Serializable(with = MealTypeSerializer::class)
    val mealType: MealType,
    @SerialName("raw materials")
    val rawMaterials: Map<String, String>,
    @SerialName("Macros")
    val macros: OptimizedMacros
)

@Serializable
data class OptimizedMacros(
    @SerialName("Calories")
    val calories: String,
    @SerialName("Protein")
    val protein: String,
    @SerialName("Carbs")
    val carbs: String,
    @SerialName("fats")
    val fats: String
)

@Serializable
data class DietPlan(
    @SerialName("estimated daily calories")
    val estimatedDailyCalories: String,
    @SerialName("target macros")
    val targetMacros: OptimizedMacros,
    @SerialName("diet strategy")
    val dietStrategy: String,
    val meals: List<OptimizedMeal>
)

object MealTypeSerializer : KSerializer<MealType> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("MealType", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: MealType) = encoder.encodeString(value.name)
    override fun deserialize(decoder: Decoder): MealType {
        val string = decoder.decodeString().lowercase()
        return MealType.values().find { it.name.lowercase() == string } ?: MealType.snack
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietOptimizerScreen(onNavigateBack: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val meals by MealRepository.meals.collectAsStateWithLifecycle(initialValue = emptyList())
    val isAiReady by MacroCalculator.isReady.collectAsStateWithLifecycle()
    
    var optimizationGoal by remember { mutableStateOf("Fat Loss") }
    var restrictions by remember { mutableStateOf("") }
    var currentWeight by remember { mutableStateOf("") }
    var targetWeight by remember { mutableStateOf("") }
    var currentHeight by remember { mutableStateOf("") }
    
    var isLoading by remember { mutableStateOf(false) }
    var dietPlan by remember { mutableStateOf<DietPlan?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }



    LaunchedEffect(Unit) {
        if (!isAiReady) {
            MacroCalculator.init()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            coroutineScope.launch {
                MacroCalculator.release()
            }
        }
    }
    val goalOptions = listOf("Bulking", "Fat Loss", "Maintenance", "Body Recomposition", "Muscle Gain")
    var expandedGoal by remember { mutableStateOf(false) }

    val json = Json { 
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    Scaffold(
        topBar = {
            GymBroTopAppBar(
                title = "Diet Optimizer",
                subtitle = if (isAiReady) "AI Powered" else "Initializing AI...",
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isAiReady) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF3A3A3C)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("AI model is initializing...", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = expandedGoal,
                    onExpandedChange = { expandedGoal = !expandedGoal },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = optimizationGoal,
                        onValueChange = { optimizationGoal = it },
                        label = { Text("Optimization Goal") },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGoal) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFF2C2C2E),
                            focusedContainerColor = Color(0xFF1C1C1E),
                            unfocusedContainerColor = Color(0xFF1C1C1E)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedGoal,
                        onDismissRequest = { expandedGoal = false },
                        modifier = Modifier.background(Color(0xFF1C1C1E))
                    ) {
                        goalOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option, color = Color.White) },
                                onClick = {
                                    optimizationGoal = option
                                    expandedGoal = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = restrictions,
                    onValueChange = { restrictions = it },
                    label = { Text("Dietary restrictions (e.g., vegan, no nuts)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFF2C2C2E),
                        focusedContainerColor = Color(0xFF1C1C1E),
                        unfocusedContainerColor = Color(0xFF1C1C1E)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = currentWeight,
                        onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) currentWeight = it },
                        label = { Text("Weight (kg)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFF2C2C2E),
                            focusedContainerColor = Color(0xFF1C1C1E),
                            unfocusedContainerColor = Color(0xFF1C1C1E)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = targetWeight,
                        onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) targetWeight = it },
                        label = { Text("Target (kg)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFF2C2C2E),
                            focusedContainerColor = Color(0xFF1C1C1E),
                            unfocusedContainerColor = Color(0xFF1C1C1E)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = currentHeight,
                        onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) currentHeight = it },
                        label = { Text("Height (cm)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFF2C2C2E),
                            focusedContainerColor = Color(0xFF1C1C1E),
                            unfocusedContainerColor = Color(0xFF1C1C1E)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Button(
                    onClick = {
                        val weight = currentWeight.toDoubleOrNull() ?: 0.0
                        val target = targetWeight.toDoubleOrNull() ?: 0.0
                        val height = currentHeight.toDoubleOrNull() ?: 0.0

                        if (weight <= 0 || target <= 0 || height <= 0) {
                            errorMessage = "Please enter valid Weight and Height values."
                            return@Button
                        }

                        coroutineScope.launch {
                            isLoading = true
                            errorMessage = null
                            
                            val history = meals.takeLast(10).joinToString("\n") { meal ->
                                val items = meal.items.joinToString(", ") { "${it.weight} ${it.name}" }
                                "Meal: $items (Macros: ${meal.macros.calories}kcal, P:${meal.macros.protein}g, C:${meal.macros.carbs}g, F:${meal.macros.fats}g)"
                            }

                            val prompt = """
                                <|turn>system
                                You are an expert Dietician. Based on the user's details, provide a daily calorie and macro requirement estimate (Protein, Carbs, Fats) and a list of optimized meal suggestions (including snacks if necessary to hit the targets).
                                The sum of the Macros and Calories for all suggested meals/snacks must closely match the 'target macros' and 'estimated daily calories' you provide.
                                Return the response ONLY as a JSON object with this exact structure:
                                {
                                  "estimated daily calories": "string (e.g. 2500 kcal)",
                                  "target macros": { "Calories": "string", "Protein": "string", "Carbs": "string", "fats": "string" },
                                  "diet strategy": "string (brief explanation of why this plan fits the goal and target weight)",
                                  "meals": [
                                    {
                                      "dish": "string",
                                      "meal type": "breakfast" | "lunch" | "dinner" | "snack",
                                      "raw materials": { "item": "quantity" },
                                      "Macros": { "Calories": "string", "Protein": "string", "Carbs": "string", "fats": "string" }
                                    }
                                  ]
                                }
                                <|think|><turn|>
                                <|turn>user
                                User Details:
                                Height: $currentHeight cm
                                Weight: $currentWeight kg
                                Target Weight: $targetWeight kg
                                Optimization Goal: $optimizationGoal
                                Restrictions: $restrictions
                                Meal History: $history
                                <turn|>
                                <|turn>model
                            """.trimIndent()

                            val response = MacroCalculator.generateResponse(prompt)
                            if (response != null) {
                                try {
                                    val cleanJson = response.trim().removeSurrounding("```json", "```").removeSurrounding("```").trim()
                                    val jsonStart = cleanJson.indexOf("{")
                                    val jsonEnd = cleanJson.lastIndexOf("}")
                                    if (jsonStart != -1 && jsonEnd != -1) {
                                        val extracted = cleanJson.substring(jsonStart, jsonEnd + 1)
                                        dietPlan = json.decodeFromString<DietPlan>(extracted)
                                    } else {
                                        errorMessage = "AI output format was invalid. Please try again."
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Error parsing response: ${e.message}"
                                }
                            } else {
                                errorMessage = "AI failed to respond. The model might be out of memory or still loading."
                            }
                            isLoading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading && isAiReady && currentWeight.isNotBlank() && targetWeight.isNotBlank() && currentHeight.isNotBlank()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black, strokeWidth = 2.dp)
                    } else {
                        Text("Optimize My Diet")
                    }
                }

                errorMessage?.let {
                    Text(it, color = Color.Red, modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall)
                }

                dietPlan?.let { plan ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Estimated Daily Targets", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            Text(plan.estimatedDailyCalories, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                OptimizedMacroItem("P", plan.targetMacros.protein)
                                OptimizedMacroItem("C", plan.targetMacros.carbs)
                                OptimizedMacroItem("F", plan.targetMacros.fats)
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Diet Strategy", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            Text(plan.dietStrategy, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF8E8E93))
                        }
                    }

                    plan.meals.forEach { meal ->
                        OptimizedMealCard(meal)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun OptimizedMealCard(meal: OptimizedMeal) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(meal.dish, style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text(meal.mealType.name.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color(0xFF8E8E93)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = Color(0xFF2C2C2E))
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text("Ingredients", fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.titleSmall)
                    meal.rawMaterials.forEach { (item, qty) ->
                        Text("• $item: $qty", color = Color(0xFF8E8E93), style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Macros", fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OptimizedMacroItem("Kcal", meal.macros.calories)
                        OptimizedMacroItem("P", meal.macros.protein)
                        OptimizedMacroItem("C", meal.macros.carbs)
                        OptimizedMacroItem("F", meal.macros.fats)
                    }
                }
            }
        }
    }
}

@Composable
fun OptimizedMacroItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF8E8E93))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
    }
}
