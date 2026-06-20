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
import com.gymlogger.ai.MacroCalculator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.gymlogger.data.MealRepository
import com.gymlogger.data.FoodLabelRepository
import com.gymlogger.model.Meal
import com.gymlogger.model.MealItem
import com.gymlogger.model.MealMacros
import com.gymlogger.model.MealType
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealLoggerScreen(onNavigateBack: () -> Unit) {
    val meals by MealRepository.meals.collectAsStateWithLifecycle(initialValue = emptyList())
    val isAiReady by MacroCalculator.isReady.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showAddMealDialog by remember { mutableStateOf(false) }
    var editingMeal by remember { mutableStateOf<Meal?>(null) }
    var showLabelDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isInitializingAi by remember { mutableStateOf(false) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                val latestMeals = MealRepository.meals.value
                withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(it)?.use { outputStream ->
                            OutputStreamWriter(outputStream, java.nio.charset.StandardCharsets.UTF_8).use { writer ->
                                writer.write(CsvExporter.exportMealsToCsv(latestMeals))
                                writer.flush()
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MealLoggerScreen", "Failed to save CSV", e)
                    }
                }
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

    LaunchedEffect(Unit) {
        FoodLabelRepository.init()
        if (!isAiReady) {
            isInitializingAi = true
            // Small delay to ensure UI transition completes smoothly before heavy engine init
            delay(500)
            MacroCalculator.init()
            isInitializingAi = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            coroutineScope.launch {
                MacroCalculator.release()
            }
        }
    }

    Scaffold(
        topBar = {
            GymBroTopAppBar(
                title = "Meal Logger",
                subtitle = if (isInitializingAi) "Initializing AI Engine..." else null,
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            isInitializingAi = true
                            MacroCalculator.release()
                            MacroCalculator.init()
                            isInitializingAi = false
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Restart Gemma",
                            tint = Color.White
                        )
                    }
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
                    IconButton(onClick = { showLabelDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Label,
                            contentDescription = "Food Labels",
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
                    DateHeader(dateMillis)
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
                                            MealRepository.deleteMeal(meal.id)
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
                                MealRepository.updateMeal(meal)
                            } else {
                                MealRepository.addMeal(meal)
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

        if (showLabelDialog) {
            FoodLabelDialog(
                onDismiss = { showLabelDialog = false }
            )
        }

        if (isInitializingAi) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Warming up Gemma 2B...",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "This may take a few seconds",
                            color = Color(0xFF8E8E93),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
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
fun DateHeader(dateMillis: Long) {
    val date = Date(dateMillis)
    val sdf = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    val dateString = sdf.format(date)
    
    Text(
        text = dateString,
        style = MaterialTheme.typography.titleLarge,
        color = Color.White,
        modifier = Modifier.padding(vertical = 16.dp)
    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodLabelDialog(onDismiss: () -> Unit) {
    val labels by FoodLabelRepository.labels.collectAsStateWithLifecycle(initialValue = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            color = Color(0xFF1C1C1E),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Food Labels",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
                
                Text(
                    "Save label information to help AI calculate macros more accurately.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8E8E93),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(labels) { label ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        label.itemName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                FoodLabelRepository.deleteLabel(label.itemName)
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.6f))
                                    }
                                }
                                Text(
                                    label.labelInfo,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF8E8E93)
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add New Label")
                }
            }
        }
    }

    if (showAddDialog) {
        var itemName by remember { mutableStateOf("") }
        var labelInfo by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Food Label", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        label = { Text("Food Name (e.g., Whey Protein)") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF2C2C2E),
                            unfocusedContainerColor = Color(0xFF2C2C2E),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    TextField(
                        value = labelInfo,
                        onValueChange = { labelInfo = it },
                        label = { Text("Nutritional Info (e.g., 24g protein per 30g scoop)") },
                        modifier = Modifier.height(100.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF2C2C2E),
                            unfocusedContainerColor = Color(0xFF2C2C2E),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (itemName.isNotBlank() && labelInfo.isNotBlank()) {
                            coroutineScope.launch {
                                // FoodLabelRepository.addLabel(itemName, labelInfo)
                                showAddDialog = false
                            }
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF1C1C1E)
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
    var mealType by remember { mutableStateOf(initialMeal?.type ?: MealType.BREAKFAST) }
    var mealDate by remember { mutableStateOf(initialMeal?.date ?: System.currentTimeMillis()) }
    var items by remember { mutableStateOf(initialMeal?.items ?: listOf(MealItem(0, "", ""))) }
    var showDatePicker by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

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
                Text(
                    text = if (initialMeal == null) "Log Meal" else "Edit Meal",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

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

                // Meal Type Selector
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        value = mealType.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }.replace("_", "-"),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Meal Type", color = Color.White) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(
                            focusedContainerColor = Color(0xFF2C2C2E),
                            unfocusedContainerColor = Color(0xFF2C2C2E),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color.LightGray,
                            unfocusedLabelColor = Color.LightGray,
                            cursorColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color(0xFF2C2C2E))
                    ) {
                        MealType.values().forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = type.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }.replace("_", "-"),
                                        color = Color.White
                                    )
                                },
                                onClick = {
                                    mealType = type
                                    expanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Items List
                Text(
                    text = "Items",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items.size) { index ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = items[index].name,
                                onValueChange = { newName ->
                                    items = items.toMutableList().also {
                                        it[index] = it[index].copy(name = newName)
                                    }
                                },
                                placeholder = { Text("Food (e.g. Eggs)") },
                                modifier = Modifier.weight(2f),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF2C2C2E),
                                    unfocusedContainerColor = Color(0xFF2C2C2E),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            TextField(
                                value = items[index].weight,
                                onValueChange = { newWeight ->
                                    items = items.toMutableList().also {
                                        it[index] = it[index].copy(weight = newWeight)
                                    }
                                },
                                placeholder = { Text("Qty (e.g. 2)") },
                                modifier = Modifier.weight(1f),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF2C2C2E),
                                    unfocusedContainerColor = Color(0xFF2C2C2E),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            IconButton(onClick = {
                                if (items.size > 1) {
                                    items = items.toMutableList().also { it.removeAt(index) }
                                }
                            }) {
                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove", tint = Color.Red.copy(alpha = 0.6f))
                            }
                        }
                    }
                    item {
                        TextButton(
                            onClick = { items = items + MealItem(0, "", "") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Item")
                        }
                    }
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
                            val validItems = items.filter { it.name.isNotBlank() }
                            if (validItems.isNotEmpty()) {
                                onSave(
                                    Meal(
                                        id = initialMeal?.id ?: 0,
                                        date = mealDate,
                                        type = mealType,
                                        items = validItems,
                                        macros = initialMeal?.macros ?: MealMacros()
                                    )
                                )
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
