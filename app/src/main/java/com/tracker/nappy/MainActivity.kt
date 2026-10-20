package com.tracker.nappy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Minimalist Studio Palette Tokens from original Nappy
private val CanvasBg = Color(0xFFECEBF0)
private val TextPrimary = Color(0xFF111114)
private val TextSecondary = Color(0xFF6E6E73)
private val TextMuted = Color(0xFF8E8E93)
private val TangerineOrange = Color(0xFFF59E3F)
private val HairlineDivider = Color(0x1F000000)
private val ModalSheetBg = Color(0xFFFAF8F5)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pipeline = NutritionPipeline(applicationContext)

        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CanvasBg
                ) {
                    MinimalJournalScreen(pipeline)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinimalJournalScreen(pipeline: NutritionPipeline) {
    val coroutineScope = rememberCoroutineScope()
    var activeInputText by remember { mutableStateOf("") }
    var selectedEntryForModal by remember { mutableStateOf<JournalEntry?>(null) }
    val entries = remember {
        mutableStateListOf(
            JournalEntry(
                rawText = "1 scoop raw soya granules (sbji) and 4 chapati",
                status = EntryStatus.COMPLETED,
                calories = 515,
                protein = 29.6,
                carbs = 89.3,
                fat = 8.5,
                items = listOf(
                    LoggedFoodItem("Soya Granules (Dry)", "1 scoop", 28.8, MacroNutrients(99, 15.0, 9.5, 0.1), "1 scoop (28.8g) / 100g * 345 kcal = 99 kcal", "nin.res.in", "Atwater Check: Passed"),
                    LoggedFoodItem("Cooking Oil", "5g", 5.0, MacroNutrients(44, 0.0, 0.0, 5.0), "5g / 100g * 884 kcal = 44 kcal", "usda.gov", "Atwater Check: Passed"),
                    LoggedFoodItem("Chapati", "4 piece", 140.0, MacroNutrients(416, 14.6, 79.8, 3.5), "4 piece (140g) / 100g * 297 kcal = 416 kcal", "nin.res.in", "Atwater Check: Passed")
                )
            ),
            JournalEntry(
                rawText = "2 boiled eggs and 1 glass milk",
                status = EntryStatus.COMPLETED,
                calories = 279,
                protein = 19.0,
                carbs = 10.5,
                fat = 17.6,
                items = listOf(
                    LoggedFoodItem("Boiled Egg", "2 piece", 100.0, MacroNutrients(155, 12.6, 1.1, 10.6), "2 piece (100g) / 100g * 155 kcal = 155 kcal", "usda.gov", "Atwater Check: Passed"),
                    LoggedFoodItem("Cow Milk", "200ml", 200.0, MacroNutrients(124, 6.4, 9.4, 7.0), "200ml / 100ml * 62 kcal = 124 kcal", "nin.res.in", "Atwater Check: Passed")
                )
            )
        )
    }

    val totalCalories = entries.filter { it.status == EntryStatus.COMPLETED }.sumOf { it.calories }
    val totalProtein = entries.filter { it.status == EntryStatus.COMPLETED }.sumOf { it.protein }
    val totalCarbs = entries.filter { it.status == EntryStatus.COMPLETED }.sumOf { it.carbs }
    val totalFat = entries.filter { it.status == EntryStatus.COMPLETED }.sumOf { it.fat }

    fun submitMeal() {
        val text = activeInputText.trim()
        if (text.isBlank()) return

        val newEntry = JournalEntry(rawText = text, status = EntryStatus.THINKING)
        entries.add(newEntry)
        activeInputText = ""

        coroutineScope.launch {
            val result = pipeline.processMealNote(text)
            val index = entries.indexOfFirst { it.id == newEntry.id }
            if (index != -1) {
                entries[index] = result
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Top Minimalist Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Today",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)),
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$totalCalories",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "kcal total",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                }
            }

            // Clean Macro Summary Line
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = "P: ${totalProtein.toInt()}g", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                Text(text = "C: ${totalCarbs.toInt()}g", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                Text(text = "F: ${totalFat.toInt()}g", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(HairlineDivider)
                    .padding(vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Clean Document Canvas / Text Editor Lines
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (entry.status == EntryStatus.COMPLETED) {
                                    selectedEntryForModal = entry
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: The user's input text
                        Text(
                            text = entry.rawText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = TextPrimary,
                            lineHeight = 22.sp,
                            modifier = Modifier.weight(1f).padding(end = 12.dp)
                        )

                        // Right: The output calories pill
                        when (entry.status) {
                            EntryStatus.THINKING -> {
                                Text(
                                    text = "...",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TangerineOrange
                                )
                            }
                            EntryStatus.COMPLETED -> {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = TangerineOrange.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${entry.calories} kcal",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            EntryStatus.ERROR -> {
                                Text(
                                    text = "Retry",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD32F2F),
                                    modifier = Modifier
                                        .clickable {
                                            val index = entries.indexOfFirst { it.id == entry.id }
                                            if (index != -1) {
                                                entries[index] = entry.copy(status = EntryStatus.THINKING)
                                                coroutineScope.launch {
                                                    val result = pipeline.processMealNote(entry.rawText)
                                                    val currIdx = entries.indexOfFirst { it.id == entry.id }
                                                    if (currIdx != -1) {
                                                        entries[currIdx] = result
                                                    }
                                                }
                                            }
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Live Active Note Line: Text editor input at the bottom
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BasicTextField(
                            value = activeInputText,
                            onValueChange = { activeInputText = it },
                            modifier = Modifier.weight(1f).padding(end = 12.dp),
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                                color = TextPrimary,
                                lineHeight = 22.sp
                            ),
                            cursorBrush = SolidColor(TangerineOrange),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { submitMeal() }),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (activeInputText.isEmpty()) {
                                        Text(
                                            text = "Type food here (e.g. 4 chapati, 1 bowl dal)...",
                                            fontSize = 16.sp,
                                            color = TextMuted
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        if (activeInputText.isNotBlank()) {
                            IconButton(
                                onClick = { submitMeal() },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(TextPrimary)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Log",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Details Modal Bottom Sheet
        selectedEntryForModal?.let { entry ->
            MealDetailModal(
                entry = entry,
                onDismiss = { selectedEntryForModal = null },
                onDelete = {
                    entries.remove(entry)
                    selectedEntryForModal = null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealDetailModal(
    entry: JournalEntry,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ModalSheetBg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFD6D3D1))
            )
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Title & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MEAL BREAKDOWN",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = entry.rawText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0x0E000000))
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                }
            }

            // Total Calories & Macro Hero
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = TangerineOrange,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${entry.calories} kcal",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = "P: ${entry.protein.toInt()}g • C: ${entry.carbs.toInt()}g • F: ${entry.fat.toInt()}g",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Itemized Breakdown
            Text(
                text = "ITEMS & CALCULATIONS",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.sp
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                entry.items.forEach { item ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.name} (${item.loggedQuantity})",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${item.macros.calories} kcal",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TangerineOrange
                                )
                            }

                            Text(
                                text = "P: ${item.macros.proteinGrams}g • C: ${item.macros.carbsGrams}g • F: ${item.macros.fatGrams}g",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )

                            // Math trace (Zero AI Math Proof)
                            Text(
                                text = "📐 ${item.mathTrace}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color(0xFF00796B),
                                modifier = Modifier.padding(top = 2.dp)
                            )

                            // Badges: Source + Atwater
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "🌐 ${item.sourceDomain}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF1976D2)
                                )
                                Text(
                                    text = "✓ ${item.atwaterStatus}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF388E3C)
                                )
                            }
                        }
                    }
                }
            }

            // Delete entry button
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Delete Entry")
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
