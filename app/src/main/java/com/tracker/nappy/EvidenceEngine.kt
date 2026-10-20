package com.tracker.nappy

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * Stage 2: Ground Truth Evidence Engine.
 * 1. Fast local cache for common staples (ICMR-NIN & USDA benchmarks).
 * 2. Targeted Web RAG for branded foods via Tavily Search + Gemini Evidence Judge.
 */
class EvidenceEngine(
    private val gemini: GeminiClient,
    private val tavilyApiKey: String
) {
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    // Verified Local Ground Truth Database (Standard 100g basis)
    private val localStaples = mapOf(
        "chapati" to CanonicalFoodBasis("Chapati", 100.0, MacroNutrients(297, 10.4, 57.0, 2.5), "nin.res.in"),
        "roti" to CanonicalFoodBasis("Roti", 100.0, MacroNutrients(297, 10.4, 57.0, 2.5), "nin.res.in"),
        "egg" to CanonicalFoodBasis("Boiled Egg", 100.0, MacroNutrients(155, 12.6, 1.1, 10.6), "usda.gov"),
        "boiled egg" to CanonicalFoodBasis("Boiled Egg", 100.0, MacroNutrients(155, 12.6, 1.1, 10.6), "usda.gov"),
        "cooked rice" to CanonicalFoodBasis("Cooked White Rice", 100.0, MacroNutrients(130, 2.7, 28.0, 0.3), "nin.res.in"),
        "rice" to CanonicalFoodBasis("Cooked White Rice", 100.0, MacroNutrients(130, 2.7, 28.0, 0.3), "nin.res.in"),
        "cooking oil" to CanonicalFoodBasis("Cooking Oil", 100.0, MacroNutrients(884, 0.0, 0.0, 100.0), "usda.gov"),
        "oil" to CanonicalFoodBasis("Cooking Oil", 100.0, MacroNutrients(884, 0.0, 0.0, 100.0), "usda.gov"),
        "chicken breast" to CanonicalFoodBasis("Chicken Breast (Cooked)", 100.0, MacroNutrients(165, 31.0, 0.0, 3.6), "usda.gov"),
        "milk" to CanonicalFoodBasis("Cow Milk", 100.0, MacroNutrients(62, 3.2, 4.7, 3.5), "nin.res.in"),
        "soya chunks" to CanonicalFoodBasis("Soya Chunks (Dry)", 100.0, MacroNutrients(345, 52.0, 33.0, 0.5), "nin.res.in"),
        "soya granules" to CanonicalFoodBasis("Soya Granules (Dry)", 100.0, MacroNutrients(345, 52.0, 33.0, 0.5), "nin.res.in"),
        "soy chunks" to CanonicalFoodBasis("Soya Chunks (Dry)", 100.0, MacroNutrients(345, 52.0, 33.0, 0.5), "nin.res.in"),
        "paneer" to CanonicalFoodBasis("Paneer", 100.0, MacroNutrients(265, 18.3, 1.2, 20.8), "nin.res.in"),
        "dal" to CanonicalFoodBasis("Cooked Yellow Dal", 100.0, MacroNutrients(104, 6.8, 17.0, 1.2), "nin.res.in"),
        "yellow dal" to CanonicalFoodBasis("Cooked Yellow Dal", 100.0, MacroNutrients(104, 6.8, 17.0, 1.2), "nin.res.in")
    )

    suspend fun resolveBasis(entity: ParsedFoodEntity): CanonicalFoodBasis? = withContext(Dispatchers.IO) {
        val key = entity.canonicalName.lowercase().trim()

        // 1. Fast local match (ICMR-NIN & USDA benchmarks)
        localStaples[key]?.let { return@withContext it }
        for ((stapleKey, basis) in localStaples) {
            if (key.contains(stapleKey) || stapleKey.contains(key)) return@withContext basis
        }

        // 2. Targeted Web RAG for branded foods
        if (tavilyApiKey.isNotBlank()) {
            val query = entity.searchQuery ?: "${entity.brand ?: ""} ${entity.canonicalName} nutrition facts per 100g label"
            return@withContext searchAndExtractWebEvidence(entity.canonicalName, query)
        }

        null
    }

    private suspend fun searchAndExtractWebEvidence(foodName: String, query: String): CanonicalFoodBasis? {
        try {
            // Step 2a: Tavily Web Search
            val tavilyPayload = JsonObject().apply {
                addProperty("query", query)
                addProperty("max_results", 3)
                addProperty("search_depth", "basic")
            }
            val searchRequest = Request.Builder()
                .url("https://api.tavily.com/search")
                .header("Authorization", "Bearer $tavilyApiKey")
                .post(tavilyPayload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val searchResponse = client.newCall(searchRequest).execute()
            val searchBody = searchResponse.body?.string() ?: ""
            val searchJson = gson.fromJson(searchBody, JsonObject::class.java)
            val results = searchJson.getAsJsonArray("results") ?: return null
            if (results.size() == 0) return null

            val firstResult = results[0].asJsonObject
            val snippet = firstResult.get("content")?.asString ?: ""
            val sourceUrl = firstResult.get("url")?.asString ?: ""
            val domain = try { URI(sourceUrl).host?.removePrefix("www.") ?: "web" } catch (_: Exception) { "web" }

            // Step 2b: Gemini Evidence Judge extracts 100g basis
            val judgePrompt = """
                Extract standard 100g nutrition facts from this snippet for "$foodName":
                Snippet: "$snippet"
                
                Return JSON ONLY:
                {"calories": 350, "protein": 20.0, "carbs": 50.0, "fat": 5.0}
            """.trimIndent()

            val text = gemini.generateJson(judgePrompt)
            val macrosJson = gson.fromJson(text.trim(), JsonObject::class.java)

            val cal = extractInt(macrosJson, "calories")
            val p = extractDouble(macrosJson, "protein")
            val c = extractDouble(macrosJson, "carbs")
            val f = extractDouble(macrosJson, "fat")

            if (cal > 0) {
                return CanonicalFoodBasis(foodName, 100.0, MacroNutrients(cal, p, c, f), domain)
            }
        } catch (e: Exception) {
            Log.w("EvidenceEngine", "Web extraction error for $foodName: ${e.message}")
        }
        return null
    }

    private fun extractDouble(obj: JsonObject, key: String): Double {
        val elem = obj.get(key) ?: return 0.0
        if (elem.isJsonNull) return 0.0
        return try {
            elem.asDouble
        } catch (_: Exception) {
            val str = elem.asString.replace(Regex("[^0-9.]"), "").trim()
            str.toDoubleOrNull() ?: 0.0
        }
    }

    private fun extractInt(obj: JsonObject, key: String): Int {
        val elem = obj.get(key) ?: return 0
        if (elem.isJsonNull) return 0
        return try {
            elem.asInt
        } catch (_: Exception) {
            val str = elem.asString.replace(Regex("[^0-9.]"), "").trim()
            str.toDoubleOrNull()?.toInt() ?: 0
        }
    }
}
