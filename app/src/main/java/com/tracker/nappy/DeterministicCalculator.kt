package com.tracker.nappy

import kotlin.math.roundToInt

/**
 * Stage 4: Deterministic Nutrition Calculator.
 * CRITICAL RULE: ZERO AI MATH. All scaling, unit conversions, and calorie calculations
 * are executed deterministically in pure Kotlin code.
 */
object DeterministicCalculator {

    // Physical densities (g/mL) for volume-to-weight conversions
    private val DENSITIES = mapOf(
        "soya" to 0.32,
        "soy" to 0.32,
        "granule" to 0.32,
        "chunk" to 0.32,
        "rice" to 0.85,
        "milk" to 1.03,
        "oil" to 0.92,
        "whey" to 0.42,
        "dal" to 1.00,
        "paneer" to 0.65
    )

    fun scale(
        basis: CanonicalFoodBasis,
        userQuantity: Double,
        userUnit: String,
        foodName: String
    ): ScaledResult {
        val lowerUnit = userUnit.lowercase().trim()
        val lowerFood = foodName.lowercase().trim()

        // 1. Resolve effective weight in grams
        val weightGrams = when (lowerUnit) {
            "g", "gram", "grams" -> userQuantity
            "ml", "milliliter" -> {
                val density = findDensity(lowerFood)
                userQuantity * density
            }
            "scoop" -> {
                val scoopVolumeMl = 90.0 // Standard 90ml fitness scoop
                val density = findDensity(lowerFood)
                userQuantity * scoopVolumeMl * density
            }
            "bowl", "katori" -> userQuantity * 150.0
            "plate" -> userQuantity * 350.0
            "piece", "pc" -> {
                if (lowerFood.contains("chapati") || lowerFood.contains("roti")) {
                    userQuantity * 35.0 // ~35g standard roasted chapati
                } else if (lowerFood.contains("egg")) {
                    userQuantity * 50.0 // ~50g standard large egg
                } else {
                    userQuantity * 50.0
                }
            }
            else -> userQuantity * 100.0 // fallback 1 serving = 100g
        }

        // 2. Pure Kotlin Scaling Factor
        val scalingFactor = weightGrams / basis.basisGrams

        val scaledCalories = (basis.nutrition.calories * scalingFactor).roundToInt()
        val scaledProtein = round1(basis.nutrition.proteinGrams * scalingFactor)
        val scaledCarbs = round1(basis.nutrition.carbsGrams * scalingFactor)
        val scaledFat = round1(basis.nutrition.fatGrams * scalingFactor)

        val trace = "${userQuantity.toIntIfWhole()} $userUnit (${round1(weightGrams)}g) / ${basis.basisGrams.toInt()}g basis * ${basis.nutrition.calories} kcal = $scaledCalories kcal"

        return ScaledResult(
            foodName = basis.name,
            quantityDesc = "${userQuantity.toIntIfWhole()} $userUnit",
            weightGrams = round1(weightGrams),
            macros = MacroNutrients(scaledCalories, scaledProtein, scaledCarbs, scaledFat),
            trace = trace,
            sourceDomain = basis.sourceDomain
        )
    }

    private fun findDensity(foodName: String): Double {
        for ((key, density) in DENSITIES) {
            if (foodName.contains(key)) return density
        }
        return 1.0 // default water density
    }

    private fun round1(value: Double): Double = (value * 10.0).roundToInt() / 10.0

    private fun Double.toIntIfWhole(): String =
        if (this % 1.0 == 0.0) this.toInt().toString() else this.toString()
}

data class ScaledResult(
    val foodName: String,
    val quantityDesc: String,
    val weightGrams: Double,
    val macros: MacroNutrients,
    val trace: String,
    val sourceDomain: String
)
