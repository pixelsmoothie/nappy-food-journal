package com.tracker.nappy

import kotlin.math.abs

/**
 * Stage 5: Rule-based Sanity & Atwater Validator.
 * Verifies that retrieved and calculated nutrition facts adhere to physical
 * and biological laws before presenting or saving them.
 */
object NutritionValidator {

    fun validate(basis: CanonicalFoodBasis): ValidationResult {
        val n = basis.nutrition

        // 1. Non-negativity check
        if (n.calories < 0 || n.proteinGrams < 0 || n.carbsGrams < 0 || n.fatGrams < 0) {
            return ValidationResult(
                isValid = false,
                atwaterCalories = 0.0,
                reportedCalories = n.calories,
                discrepancyPercent = 100.0,
                reason = "Macronutrients cannot be negative."
            )
        }

        // 2. Biological Mass Conservation Check
        // Total grams of protein + carbs + fat cannot exceed 105% of the total food weight
        val totalMacroMass = n.proteinGrams + n.carbsGrams + n.fatGrams
        if (totalMacroMass > basis.basisGrams * 1.05) {
            return ValidationResult(
                isValid = false,
                atwaterCalories = 0.0,
                reportedCalories = n.calories,
                discrepancyPercent = ((totalMacroMass - basis.basisGrams) / basis.basisGrams) * 100.0,
                reason = "Macro mass (${totalMacroMass}g) exceeds basis weight (${basis.basisGrams}g)."
            )
        }

        // 3. Atwater 4-4-9 Energy Factor Check
        // 1g Protein = 4 kcal, 1g Carbs = 4 kcal, 1g Fat = 9 kcal
        val atwaterCalories = (n.proteinGrams * 4.0) + (n.carbsGrams * 4.0) + (n.fatGrams * 9.0)

        // If reported calories is 0 (except water/tea), it's missing data
        if (n.calories == 0 && atwaterCalories > 10.0) {
            return ValidationResult(
                isValid = false,
                atwaterCalories = atwaterCalories,
                reportedCalories = 0,
                discrepancyPercent = 100.0,
                reason = "Reported 0 calories but macros suggest ${atwaterCalories.toInt()} kcal."
            )
        }

        val diff = abs(atwaterCalories - n.calories)
        val discrepancyPercent = if (n.calories > 0) (diff / n.calories) * 100.0 else 0.0

        // Allow up to 20% variance for dietary fiber (0-2 kcal/g) and regulatory rounding
        val passesAtwater = discrepancyPercent <= 20.0

        return ValidationResult(
            isValid = passesAtwater,
            atwaterCalories = atwaterCalories,
            reportedCalories = n.calories,
            discrepancyPercent = discrepancyPercent,
            reason = if (passesAtwater) "Passed Atwater sanity check" else "Atwater discrepancy ($discrepancyPercent%) exceeds 20% tolerance."
        )
    }
}
