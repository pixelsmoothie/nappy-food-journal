package com.tracker.nappy

import java.util.UUID

/**
 * Core domain models for Nappy's AI Food Journal.
 */

enum class FoodState {
    RAW, DRY, COOKED
}

enum class EntryStatus {
    THINKING, COMPLETED, ERROR
}

data class ParsedFoodEntity(
    val rawName: String,
    val canonicalName: String,
    val quantity: Double,
    val unit: String,
    val state: FoodState,
    val brand: String? = null,
    val searchQuery: String? = null
)

data class MacroNutrients(
    val calories: Int,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double
)

data class CanonicalFoodBasis(
    val name: String,
    val basisGrams: Double = 100.0,
    val nutrition: MacroNutrients,
    val sourceDomain: String,
    val isVerified: Boolean = true
)

data class LoggedFoodItem(
    val name: String,
    val loggedQuantity: String,
    val calculatedWeightGrams: Double,
    val macros: MacroNutrients,
    val mathTrace: String,
    val sourceDomain: String,
    val atwaterStatus: String
)

data class JournalEntry(
    val id: String = UUID.randomUUID().toString(),
    val rawText: String,
    val status: EntryStatus = EntryStatus.THINKING,
    val calories: Int = 0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val items: List<LoggedFoodItem> = emptyList(),
    val errorMessage: String? = null
)

data class ValidationResult(
    val isValid: Boolean,
    val atwaterCalories: Double,
    val reportedCalories: Int,
    val discrepancyPercent: Double,
    val reason: String? = null
)
