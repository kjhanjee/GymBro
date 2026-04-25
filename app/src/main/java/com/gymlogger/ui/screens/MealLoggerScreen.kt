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
import com.gymlogger.ai.MacroCalculator
import com.gymlogger.data.MealRepository
import com.gymlogger.data.FoodLabelRepository
import com.gymlogger.model.Meal
import com.gymlogger.model.MealItem
import com.gymlogger.model.MealType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealLoggerScreen(onNavigateBack: () -> Unit) {
    val meals by MealRepository.meals.collectAsState()
    val isAiReady by MacroCalculator.isReady.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showAddMealDialog by remember { mutableStateOf(false) }
    var showLabelDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isInitializingAi by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        FoodLabelRepository.init(context)
        if (!isAiReady) {
            isInitializingAi = true
            // Small delay to ensure UI transition completes smoothly before heavy engine init
            delay(500)
            MacroCalculator.init(context)
            isInitializingAi = false
        }
    }

    Scaffold(
        topBar = {
            GymBroTopAppBar(
                title = "Meal Logger",
                subtitle = if (isInitializingAi) "Initializing AI Engine..." else null,
                onNavigateBack = onNavigateBack,
                actions = {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(meals) { meal ->
                MealCard(meal = meal, onDelete = {
                    coroutineScope.launch {
                        MealRepository.deleteMeal(context, meal.id)
                    }
                })
            }
        }

        if (showAddMealDialog) {
            AddMealDialog(
                onDismiss = { showAddMealDialog = false },
                onSave = { meal ->
                    coroutineScope.launch {
                        isSaving = true
                        try {
                            MealRepository.addMeal(context, meal)
                        } finally {
                            isSaving = false
                            showAddMealDialog = false
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
fun MealCard(meal: Meal, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        color = Color(0xFF1C1C1E),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = meal.type.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFF8E8E93),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // Macros Placeholder Section
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
                    MacroItem("Calories", "${meal.macros.calories.toInt()} kcal")
                    MacroItem("Protein", "${meal.macros.protein.toInt()}g")
                    MacroItem("Carbs", "${meal.macros.carbs.toInt()}g")
                    MacroItem("Fats", "${meal.macros.fats.toInt()}g")
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
                        Text(text = item.name, color = Color.White)
                        Text(text = item.weight, color = Color(0xFF8E8E93))
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${meal.items.size} items",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8E8E93)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodLabelDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val labels by FoodLabelRepository.labels.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    
    var itemName by remember { mutableStateOf("") }
    var servingSize by remember { mutableStateOf("100g") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fats by remember { mutableStateOf("") }

    val isFormValid = itemName.isNotBlank() && 
                      servingSize.isNotBlank() &&
                      calories.isNotBlank() && 
                      protein.isNotBlank() && 
                      carbs.isNotBlank() && 
                      fats.isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1C1C1E),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Food Labels", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                Text(
                    "Define nutrition facts for specific items.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8E8E93)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextField(
                            value = itemName,
                            onValueChange = { itemName = it },
                            label = { Text("Item Name") },
                            modifier = Modifier.weight(1.5f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF2C2C2E),
                                unfocusedContainerColor = Color(0xFF2C2C2E),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        TextField(
                            value = servingSize,
                            onValueChange = { servingSize = it },
                            label = { Text("Serving") },
                            placeholder = { Text("e.g. 100g") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF2C2C2E),
                                unfocusedContainerColor = Color(0xFF2C2C2E),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextField(
                            value = calories,
                            onValueChange = { if (it.isEmpty() || it.toFloatOrNull() != null) calories = it },
                            label = { Text("Calories (kcal)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF2C2C2E),
                                unfocusedContainerColor = Color(0xFF2C2C2E),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        TextField(
                            value = protein,
                            onValueChange = { if (it.isEmpty() || it.toFloatOrNull() != null) protein = it },
                            label = { Text("Protein (g)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF2C2C2E),
                                unfocusedContainerColor = Color(0xFF2C2C2E),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextField(
                            value = carbs,
                            onValueChange = { if (it.isEmpty() || it.toFloatOrNull() != null) carbs = it },
                            label = { Text("Carbs (g)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF2C2C2E),
                                unfocusedContainerColor = Color(0xFF2C2C2E),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        TextField(
                            value = fats,
                            onValueChange = { if (it.isEmpty() || it.toFloatOrNull() != null) fats = it },
                            label = { Text("Fats (g)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF2C2C2E),
                                unfocusedContainerColor = Color(0xFF2C2C2E),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            FoodLabelRepository.saveLabel(
                                context,
                                itemName,
                                calories.toFloatOrNull() ?: 0f,
                                protein.toFloatOrNull() ?: 0f,
                                carbs.toFloatOrNull() ?: 0f,
                                fats.toFloatOrNull() ?: 0f,
                                servingSize
                            )
                            itemName = ""
                            servingSize = "100g"
                            calories = ""
                            protein = ""
                            carbs = ""
                            fats = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isFormValid
                ) {
                    Text("Add Label")
                }

                HorizontalDivider(color = Color(0xFF2C2C2E))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(labels) { label ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2C2C2E), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(label.itemName, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Text(label.labelInfo, color = Color.White, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    FoodLabelRepository.deleteLabel(context, label.itemName)
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Close", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun MacroItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF8E8E93))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealDialog(onDismiss: () -> Unit, onSave: (Meal) -> Unit) {
    var mealType by remember { mutableStateOf(MealType.BREAKFAST) }
    var items by remember { mutableStateOf(listOf(MealItem(0, "", ""))) }
    var expanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1C1C1E),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Add Meal", style = MaterialTheme.typography.headlineSmall, color = Color.White)

                // Meal Type Dropdown
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text(mealType.name.lowercase().replaceFirstChar { it.uppercase() })
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color(0xFF2C2C2E))
                    ) {
                        MealType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }, color = Color.White) },
                                onClick = {
                                    mealType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Meal Items
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextField(
                                value = item.name,
                                onValueChange = { name ->
                                    val newList = items.toMutableList()
                                    newList[index] = item.copy(name = name)
                                    items = newList
                                },
                                label = { Text("Item") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF2C2C2E),
                                    unfocusedContainerColor = Color(0xFF2C2C2E),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                            TextField(
                                value = item.weight,
                                onValueChange = { weight ->
                                    val newList = items.toMutableList()
                                    newList[index] = item.copy(weight = weight)
                                    items = newList
                                },
                                label = { Text("Weight") },
                                modifier = Modifier.width(100.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF2C2C2E),
                                    unfocusedContainerColor = Color(0xFF2C2C2E),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                TextButton(
                    onClick = { items = items + MealItem(0, "", "") },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Add Item")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.White)
                    }
                    Button(
                        onClick = {
                            onSave(Meal(0, System.currentTimeMillis(), mealType, items.filter { it.name.isNotBlank() }))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Save", color = Color.Black)
                    }
                }
            }
        }
    }
}
