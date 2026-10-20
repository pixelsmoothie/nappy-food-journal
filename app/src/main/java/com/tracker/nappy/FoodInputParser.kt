package com.tracker.nappy

import com.google.gson.Gson
import com.google.gson.JsonArray

/**
 * Stage 1: Natural Language Entity Extractor.
 * Takes freeform user text and extracts clean food entities into structured JSON.
 * CRITICAL RULE: Zero AI arithmetic. Preserves physical state (RAW/DRY vs COOKED).
 */
class FoodInputParser(private val gemini: GeminiClient) {

    private val gson = Gson()

    suspend fun parse(rawNote: String): List<ParsedFoodEntity> {
        val trimmed = rawNote.trim()
        if (trimmed.isBlank()) return emptyList()

        val systemInstruction = """
            You are Nappy's Food Entity Parser.
            Your ONLY job is to extract individual food items from a user's food log into structured JSON.
            
            RULES:
            1. DO NOT CALCULATE CALORIES OR MACROS.
            2. PRESERVE PHYSICAL STATE: Mark state as "RAW", "DRY", or "COOKED" (e.g. raw rice vs cooked rice).
            3. PRESERVE BRANDS: Extract brand names cleanly if mentioned (e.g. "Nutrela", "Amul").
            4. PRESERVE EXACT QUANTITY & UNIT: Keep quantity numeric and unit string (e.g. 1 scoop, 4 piece, 200 ml).
            5. COOKING OIL ASSUMPTION: If user logs raw ingredients cooked into a curry/sabji (e.g. "raw soya granules (sbji)"), cooking requires oil. Include an entry for 5g Cooking Oil so dishes do not unrealistically have 0g fat.
            6. SEARCH QUERY: For each item, provide a clean search query to find its 100g nutrition label (e.g. "Nutrela Soya Granules nutrition facts label").
            
            Return JSON Array ONLY with schema:
            [{"rawName": "...", "canonicalName": "...", "quantity": 1.0, "unit": "...", "state": "RAW|DRY|COOKED", "brand": null, "searchQuery": "..."}]
        """.trimIndent()

        val prompt = "User food log:\n\"$trimmed\"\n\nReturn JSON array only:"
        val jsonText = gemini.generateJson(prompt, systemInstruction)

        return parseJsonArray(jsonText, trimmed)
    }

    private fun parseJsonArray(jsonText: String, fallbackName: String): List<ParsedFoodEntity> {
        return try {
            val array = gson.fromJson(jsonText, JsonArray::class.java) ?: return emptyList()
            val list = mutableListOf<ParsedFoodEntity>()

            for (i in 0 until array.size()) {
                val item = array[i].asJsonObject
                val stateStr = item.get("state")?.asString?.uppercase() ?: "COOKED"
                val state = try { FoodState.valueOf(stateStr) } catch (_: Exception) { FoodState.COOKED }

                // Robust number parsing: strips any units like "1.0", "1", or "1 scoop"
                val qtyStr = item.get("quantity")?.asString?.replace(Regex("[^0-9.]"), "") ?: "1.0"
                val qty = qtyStr.toDoubleOrNull() ?: 1.0

                list.add(
                    ParsedFoodEntity(
                        rawName = item.get("rawName")?.asString ?: fallbackName,
                        canonicalName = item.get("canonicalName")?.asString ?: fallbackName,
                        quantity = qty,
                        unit = item.get("unit")?.asString ?: "serving",
                        state = state,
                        brand = item.get("brand")?.takeIf { !it.isJsonNull }?.asString,
                        searchQuery = item.get("searchQuery")?.takeIf { !it.isJsonNull }?.asString
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
