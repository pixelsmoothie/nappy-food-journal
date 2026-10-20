package com.tracker.nappy

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Reusable Gemini API Client.
 * Handles the raw HTTP networking, timeouts, and JSON mode configuration.
 */
class GeminiClient(private val apiKey: String) {

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    suspend fun generateJson(prompt: String, systemInstruction: String? = null): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext "[]"

        // Build the standard Gemini REST payload
        val payload = JsonObject().apply {
            add("contents", JsonArray().apply {
                add(JsonObject().apply {
                    add("parts", JsonArray().apply {
                        add(JsonObject().apply { addProperty("text", prompt) })
                    })
                })
            })
            if (!systemInstruction.isNullOrBlank()) {
                add("systemInstruction", JsonObject().apply {
                    add("parts", JsonArray().apply {
                        add(JsonObject().apply { addProperty("text", systemInstruction) })
                    })
                })
            }
            add("generationConfig", JsonObject().apply {
                addProperty("temperature", 0.1)
                addProperty("responseMimeType", "application/json")
            })
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                Log.e("GeminiClient", "Gemini HTTP ${response.code}: $body")
                return@withContext "[]"
            }

            // Extract the generated text from candidates[0].content.parts[0].text
            val json = gson.fromJson(body, JsonObject::class.java)
            val text = json.getAsJsonArray("candidates")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("content")
                ?.getAsJsonArray("parts")
                ?.get(0)?.asJsonObject
                ?.get("text")?.asString ?: "[]"

            text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        } catch (e: Exception) {
            Log.e("GeminiClient", "Network error: ${e.message}")
            "[]"
        }
    }
}
