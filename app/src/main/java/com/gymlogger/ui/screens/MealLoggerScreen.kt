package com.gymlogger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymlogger.ui.components.GymBroTopAppBar
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.gymlogger.util.CsvExporter
import java.io.OutputStreamWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.gymlogger.data.MealRepository
import com.gymlogger.model.Meal
import com.gymlogger.model.MealItem
import com.gymlogger.model.MealMacros
import com.gymlogger.model.MealType
import com.gymlogger.model.MealJson
import com.gymlogger.model.MealItemJson
import androidx.compose.ui.tooling.preview.Preview
import android.content.Context
import android.content.ClipboardManager
import android.content.ClipData
import android.widget.Toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealLoggerScreen(onNavigateBack: () -> Unit) {
    val meals by MealRepository.meals.collectAsStateWithLifecycle(initialValue = emptyList())
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showAddMealDialog by remember { mutableStateOf(false) }
    var editingMeal by remember { mutableStateOf<Meal?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var mealsToExport by remember { mutableStateOf<List<Meal>?>(null) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                val exportList = mealsToExport ?: MealRepository.meals.value
                withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(it)?.use { outputStream ->
                            OutputStreamWriter(outputStream, java.nio.charset.StandardCharsets.UTF_8).use { writer ->
                                writer.write(CsvExporter.exportMealsToCsv(exportList))
                                writer.flush()
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MealLoggerScreen", "Failed to save CSV", e)
                    }
                }
                mealsToExport = null
            }
        }
    }

    val groupedMeals = remember(meals) {
        meals.groupBy {
            val cal = java.util.Calendar.getInstance().apply {
                timeInMillis = it.date
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis
        }.mapValues { entry ->
            entry.value.groupBy { it.type }.toList().sortedBy { it.first.ordinal }
        }.toSortedMap(compareByDescending { it })
    }

    val dailyTotals = remember(meals) {
        meals.groupBy {
            val cal = java.util.Calendar.getInstance().apply {
                timeInMillis = it.date
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis
        }.mapValues { entry ->
            entry.value.fold(MealMacros()) { acc, meal -> acc + meal.macros }
        }
    }

    val expandedGroups = remember { mutableStateMapOf<String, Boolean>() }

    Scaffold(
        topBar = {
            GymBroTopAppBar(
                title = "Meal Logger",
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(onClick = { 
                        val fileName = "meal_logs_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.csv"
                        createDocumentLauncher.launch(fileName)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download CSV",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddMealDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Meal")
            }
        },
        containerColor = Color.Black
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            groupedMeals.forEach { (dateMillis, typesWithMeals) ->
                item(key = "date-$dateMillis") {
                    DateHeader(
                        dateMillis = dateMillis,
                        onDownload = {
                            val dayMeals = typesWithMeals.flatMap { it.second }
                            mealsToExport = dayMeals
                            val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(dateMillis))
                            createDocumentLauncher.launch("meal_logs_$dateStr.csv")
                        }
                    )
                }

                item(key = "totals-$dateMillis") {
                    dailyTotals[dateMillis]?.let { totals ->
                        DailyTotalsCard(totals)
                    }
                }

                typesWithMeals.forEach { (type, typeMeals) ->
                    val groupKey = "$dateMillis-${type.name}"
                    val isExpanded = expandedGroups[groupKey] ?: true

                    item(key = "type-$groupKey") {
                        MealTypeHeader(
                            type = type,
                            isExpanded = isExpanded,
                            onToggle = { expandedGroups[groupKey] = !isExpanded }
                        )
                    }

                    if (isExpanded) {
                        items(typeMeals, key = { it.id }) { meal ->
                            Box(modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)) {
                                MealCard(
                                    meal = meal,
                                    onEdit = { editingMeal = meal },
                                    onDelete = {
                                        coroutineScope.launch {
                                            MealRepository.deleteMeal(context, meal.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }
            }
        }

        if (showAddMealDialog || editingMeal != null) {
            AddMealDialog(
                initialMeal = editingMeal,
                onDismiss = { 
                    showAddMealDialog = false
                    editingMeal = null
                },
                onSave = { meal ->
                    coroutineScope.launch {
                        isSaving = true
                        try {
                            if (editingMeal != null) {
                                MealRepository.updateMeal(context, meal)
                            } else {
                                MealRepository.addMeal(context, meal)
                            }
                        } finally {
                            isSaving = false
                            showAddMealDialog = false
                            editingMeal = null
                        }
                    }
                }
            )
        }

        if (isSaving) {
            Dialog(onDismissRequest = {}) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(Color(0xFF1C1C1E), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun MealCard(meal: Meal, onEdit: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        color = Color(0xFF1C1C1E),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val summaryText = if (meal.items.isEmpty()) "No items" 
                                          else meal.items.joinToString(", ") { it.name }
                        Text(
                            text = if (summaryText.length > 40) summaryText.take(37) + "..." else summaryText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        val timeString = SimpleDateFormat("h:mm a", Locale.getDefault())
                            .format(Date(meal.date))
                        Text(
                            text = "$timeString • ${meal.macros.calories.toInt()} kcal",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF8E8E93)
                        )
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFF8E8E93),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color(0xFF8E8E93),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.Red.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Estimated Macros",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MacroItem("Calories", "${meal.macros.calories.toInt()}")
                    MacroItem("Protein", "${meal.macros.protein.toInt()}g")
                    MacroItem("Carbs", "${meal.macros.carbs.toInt()}g")
                    MacroItem("Fats", "${meal.macros.fats.toInt()}g")
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Micros",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF8E8E93),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MacroItem("Fibre", "${meal.macros.fibre.toInt()}g")
                    MacroItem("Sugar", "${meal.macros.refinedSugar.toInt()}g")
                    MacroItem("Vit B", "${String.format(Locale.getDefault(), "%.1f", meal.macros.vitaminB)}mg")
                    MacroItem("Vit D", "${String.format(Locale.getDefault(), "%.1f", meal.macros.vitaminD)}mcg")
                    MacroItem("Omega", "${meal.macros.omega.toInt()}mg")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MacroItem("Vit C", "${meal.macros.vitaminC.toInt()}mg")
                    MacroItem("Iron", "${meal.macros.iron.toInt()}mg")
                    MacroItem("Potas", "${meal.macros.potassium.toInt()}mg")
                    MacroItem("Magn", "${meal.macros.magnesium.toInt()}mg")
                    MacroItem("Sod", "${meal.macros.sodium.toInt()}mg")
                }
                
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = Color(0xFF2C2C2E)
                )

                Text(
                    text = "Items",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF8E8E93)
                )
                Spacer(modifier = Modifier.height(4.dp))
                meal.items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = item.name, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        Text(text = item.weight, color = Color(0xFF8E8E93), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun DailyTotalsCardPreview() {
    MaterialTheme {
        DailyTotalsCard(
            totals = MealMacros(
                calories = 2000f,
                protein = 150f,
                carbs = 200f,
                fats = 60f,
                fibre = 30f,
                refinedSugar = 20f,
                vitaminB = 1.5f,
                vitaminD = 10f,
                omega = 1000f
            )
        )
    }
}

@Preview
@Composable
fun AddMealDialogPreview() {
    MaterialTheme {
        AddMealDialog(
            initialMeal = null,
            onDismiss = {},
            onSave = {}
        )
    }
}

@Preview
@Composable
fun EditMealDialogPreview() {
    MaterialTheme {
        AddMealDialog(
            initialMeal = Meal(
                id = 1,
                date = System.currentTimeMillis(),
                type = MealType.BREAKFAST,
                items = listOf(MealItem(1, "Chai", "150ml")),
                macros = MealMacros()
            ),
            onDismiss = {},
            onSave = {}
        )
    }
}

@Composable
fun DailyTotalsCard(totals: MealMacros) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        color = Color(0xFF1C1C1E).copy(alpha = 0.7f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2C2E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Daily Totals",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MacroItem("Calories", "${totals.calories.toInt()}")
                MacroItem("Protein", "${totals.protein.toInt()}g")
                MacroItem("Carbs", "${totals.carbs.toInt()}g")
                MacroItem("Fats", "${totals.fats.toInt()}g")
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF2C2C2E))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MacroItem("Fibre", "${totals.fibre.toInt()}g")
                MacroItem("Sugar", "${totals.refinedSugar.toInt()}g")
                MacroItem("Vit B", "${String.format(Locale.getDefault(), "%.1f", totals.vitaminB)}mg")
                MacroItem("Vit D", "${String.format(Locale.getDefault(), "%.1f", totals.vitaminD)}mcg")
                MacroItem("Omega", "${totals.omega.toInt()}mg")
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MacroItem("Vit C", "${totals.vitaminC.toInt()}mg")
                MacroItem("Iron", "${totals.iron.toInt()}mg")
                MacroItem("Potas", "${totals.potassium.toInt()}mg")
                MacroItem("Magn", "${totals.magnesium.toInt()}mg")
                MacroItem("Sod", "${totals.sodium.toInt()}mg")
            }
        }
    }
}

@Composable
fun DateHeader(dateMillis: Long, onDownload: () -> Unit) {
    val date = Date(dateMillis)
    val sdf = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    val dateString = sdf.format(date)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dateString,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )
        IconButton(onClick = onDownload) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Download Day CSV",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun MealTypeHeader(type: MealType, isExpanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = when (type) {
                MealType.BREAKFAST -> "Breakfast"
                MealType.LUNCH -> "Lunch"
                MealType.DINNER -> "Dinner"
                MealType.SNACK -> "Snacks"
                MealType.PRE_WORKOUT -> "Pre-Workout"
            },
            style = MaterialTheme.typography.titleSmall,
            color = Color(0xFF8E8E93),
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = Color(0xFF8E8E93),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun MacroItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF8E8E93))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealDialog(
    initialMeal: Meal?,
    onDismiss: () -> Unit,
    onSave: (Meal) -> Unit
) {
    val context = LocalContext.current
    var mealDate by remember { mutableStateOf(initialMeal?.date ?: System.currentTimeMillis()) }
    
    val initialJson = if (initialMeal != null) {
        val mealJson = MealJson(
            type = initialMeal.type.name,
            items = initialMeal.items.map { item ->
                MealItemJson(
                    name = item.name,
                    calories = initialMeal.macros.calories, // Note: This assumes one item per meal in current UI, but we'll support more in JSON
                    protein = initialMeal.macros.protein,
                    carbs = initialMeal.macros.carbs,
                    fats = initialMeal.macros.fats,
                    fibre = initialMeal.macros.fibre,
                    sugar = initialMeal.macros.refinedSugar,
                    vitaminB = initialMeal.macros.vitaminB,
                    vitaminD = initialMeal.macros.vitaminD,
                    omega = initialMeal.macros.omega,
                    vitaminC = initialMeal.macros.vitaminC,
                    iron = initialMeal.macros.iron,
                    potassium = initialMeal.macros.potassium,
                    magnesium = initialMeal.macros.magnesium,
                    sodium = initialMeal.macros.sodium
                )
            }
        )
        Json { prettyPrint = true }.encodeToString(mealJson)
    } else {
        ""
    }

    var jsonInput by remember { mutableStateOf(initialJson) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTemplateInfo by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            color = Color(0xFF1C1C1E),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialMeal == null) "Log Meal" else "Edit Meal",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    IconButton(onClick = { showTemplateInfo = true }) {
                        Icon(Icons.Default.Info, contentDescription = "JSON Template", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Date Picker Button
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Event, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(mealDate)))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // JSON Input
                Text(
                    text = "Meal Data (JSON)",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                TextField(
                    value = jsonInput,
                    onValueChange = { 
                        jsonInput = it
                        errorMessage = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    placeholder = { Text("Paste meal JSON here...") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF2C2C2E),
                        unfocusedContainerColor = Color(0xFF2C2C2E),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Color.White)
                    }
                    Button(
                        onClick = {
                            try {
                                val json = Json { ignoreUnknownKeys = true }
                                val mealJson = json.decodeFromString<MealJson>(jsonInput)
                                val mealType = try {
                                    MealType.valueOf(mealJson.type.uppercase())
                                } catch (e: Exception) {
                                    MealType.SNACK
                                }

                                val meal = Meal(
                                    id = initialMeal?.id ?: 0,
                                    date = mealDate,
                                    type = mealType,
                                    items = mealJson.items.mapIndexed { index, item -> MealItem(index.toLong(), item.name, "") },
                                    macros = MealMacros(
                                        calories = mealJson.items.sumOf { it.calories.toDouble() }.toFloat(),
                                        protein = mealJson.items.sumOf { it.protein.toDouble() }.toFloat(),
                                        carbs = mealJson.items.sumOf { it.carbs.toDouble() }.toFloat(),
                                        fats = mealJson.items.sumOf { it.fats.toDouble() }.toFloat(),
                                        fibre = mealJson.items.sumOf { it.fibre.toDouble() }.toFloat(),
                                        refinedSugar = mealJson.items.sumOf { it.sugar.toDouble() }.toFloat(),
                                        vitaminB = mealJson.items.sumOf { it.vitaminB.toDouble() }.toFloat(),
                                        vitaminD = mealJson.items.sumOf { it.vitaminD.toDouble() }.toFloat(),
                                        omega = mealJson.items.sumOf { it.omega.toDouble() }.toFloat(),
                                        vitaminC = mealJson.items.sumOf { it.vitaminC.toDouble() }.toFloat(),
                                        iron = mealJson.items.sumOf { it.iron.toDouble() }.toFloat(),
                                        potassium = mealJson.items.sumOf { it.potassium.toDouble() }.toFloat(),
                                        magnesium = mealJson.items.sumOf { it.magnesium.toDouble() }.toFloat(),
                                        sodium = mealJson.items.sumOf { it.sodium.toDouble() }.toFloat()
                                    )
                                )
                                onSave(meal)
                            } catch (e: Exception) {
                                errorMessage = "Invalid JSON format: ${e.localizedMessage}"
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Meal")
                    }
                }
            }
        }
    }

    if (showTemplateInfo) {
        val template = """
            {
              "type": "LUNCH",
              "items": [
                {
                  "name": "Chicken Breast",
                  "calories": 165,
                  "protein": 31,
                  "carbs": 0,
                  "fats": 3.6,
                  "fibre": 0,
                  "sugar": 0,
                  "vitaminB": 0.4,
                  "vitaminD": 0,
                  "omega": 0,
                  "vitaminC": 0,
                  "iron": 1.0,
                  "potassium": 256,
                  "magnesium": 29,
                  "sodium": 74
                },
                {
                  "name": "White Rice",
                  "calories": 205,
                  "protein": 4.3,
                  "carbs": 45,
                  "fats": 0.4,
                  "fibre": 0.6,
                  "sugar": 0,
                  "vitaminB": 0.1,
                  "vitaminD": 0,
                  "omega": 0,
                  "vitaminC": 0,
                  "iron": 0.2,
                  "potassium": 55,
                  "magnesium": 19,
                  "sodium": 1
                }
              ]
            }
        """.trimIndent()

        AlertDialog(
            onDismissRequest = { showTemplateInfo = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Meal JSON Template")
                    IconButton(onClick = {
                        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Meal JSON Template", template)
                        clipboardManager.setPrimaryClip(clip)
                        Toast.makeText(context, "Template copied to clipboard", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy to clipboard", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            text = {
                Column {
                    Text("Copy this template and replace the values:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color(0xFF2C2C2E),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = template,
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                            color = Color.LightGray
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Valid types: BREAKFAST, LUNCH, DINNER, SNACK, PRE_WORKOUT", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showTemplateInfo = false }) {
                    Text("Got it")
                }
            },
            containerColor = Color(0xFF1C1C1E),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    if (showDatePicker) {
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = mealDate }
        android.app.DatePickerDialog(
            LocalContext.current,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = java.util.Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth)
                mealDate = selectedCalendar.timeInMillis
                showDatePicker = false
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        ).show()
    }
}
