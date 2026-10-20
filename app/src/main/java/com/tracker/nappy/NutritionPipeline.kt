package com.tracker.nappy

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Master Pipeline Orchestrator.
 * Connects Stage 1 (Parser) -> Stage 2 (Evidence) -> Stage 3 (Calculator) -> Stage 4 (Validator).
 */
class NutritionPipeline(private val context: Context) {

    private val geminiApiKey: String
    private val tavilyApiKey: String

    init {
        val prefs = context.getSharedPreferences("workout_coach_settings", Context.MODE_PRIVATE)
        geminiApiKey = prefs.getString("gemini_api_key", null)
            ?: BuildConfig.GEMINI_API_KEY.ifBlank { "" }
        tavilyApiKey = prefs.getString("tavily_api_key", null)
            ?: BuildConfig.TAVILY_API_KEY.ifBlank { "" }
    }

    private val geminiClient = GeminiClient(geminiApiKey)
    private val parser = FoodInputParser(geminiClient)
    private val evidenceEngine = EvidenceEngine(geminiClient, tavilyApiKey)

    suspend fun processMealNote(
        rawNote: String,
        onStatusUpdate: ((String) -> Unit)? = null
    ): JournalEntry = withContext(Dispatchers.IO) {
        val trimmed = rawNote.trim()
        if (trimmed.isBlank()) {
            return@withContext JournalEntry(rawText = rawNote, status = EntryStatus.ERROR)
        }

        val items = analyzeMeal(trimmed, onStatusUpdate)
        if (items.isEmpty()) {
            return@withContext JournalEntry(
                rawText = trimmed,
                status = EntryStatus.ERROR,
                errorMessage = "Could not verify nutritional facts for this entry."
            )
        }

        val totalCalories = items.sumOf { it.macros.calories }
        val totalP = items.sumOf { it.macros.proteinGrams }
        val totalC = items.sumOf { it.macros.carbsGrams }
        val totalF = items.sumOf { it.macros.fatGrams }

        JournalEntry(
            rawText = trimmed,
            status = EntryStatus.COMPLETED,
            calories = totalCalories,
            protein = (totalP * 10.0).roundToInt() / 10.0,
            carbs = (totalC * 10.0).roundToInt() / 10.0,
            fat = (totalF * 10.0).roundToInt() / 10.0,
            items = items
        )
    }

    suspend fun analyzeMeal(
        rawNote: String,
        onStatusUpdate: ((String) -> Unit)? = null
    ): List<LoggedFoodItem> = withContext(Dispatchers.IO) {
        val trimmed = rawNote.trim()
        if (trimmed.isBlank()) return@withContext emptyList()

        onStatusUpdate?.invoke("Stage 1: Extracting items...")
        val entities = parser.parse(trimmed)
        if (entities.isEmpty()) {
            Log.w("NutritionPipeline", "No food entities extracted from '$trimmed'")
            return@withContext emptyList()
        }

        val results = mutableListOf<LoggedFoodItem>()

        for (entity in entities) {
            onStatusUpdate?.invoke("Stage 2: Resolving evidence for ${entity.canonicalName}...")
            val basis = evidenceEngine.resolveBasis(entity) ?: continue

            onStatusUpdate?.invoke("Stage 3: Validating Atwater energy factors...")
            val validation = NutritionValidator.validate(basis)
            val atwaterStatus = if (validation.isValid) "Atwater Check: Passed" else "Atwater: Warning (${validation.discrepancyPercent.toInt()}% diff)"

            onStatusUpdate?.invoke("Stage 4: Deterministic scaling...")
            val scaled = DeterministicCalculator.scale(
                basis = basis,
                userQuantity = entity.quantity,
                userUnit = entity.unit,
                foodName = entity.canonicalName
            )

            results.add(
                LoggedFoodItem(
                    name = scaled.foodName,
                    loggedQuantity = scaled.quantityDesc,
                    calculatedWeightGrams = scaled.weightGrams,
                    macros = scaled.macros,
                    mathTrace = scaled.trace,
                    sourceDomain = scaled.sourceDomain,
                    atwaterStatus = atwaterStatus
                )
            )
        }

        onStatusUpdate?.invoke("Done!")
        results
    }
}
