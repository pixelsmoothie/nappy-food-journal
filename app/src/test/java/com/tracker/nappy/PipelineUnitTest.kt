package com.tracker.nappy

import org.junit.Assert.*
import org.junit.Test

class PipelineUnitTest {

    @Test
    fun testDeterministicScalingZeroAiMath() {
        // 100g basis of Soya Granules: 345 kcal, 52g protein, 33g carbs, 0.5g fat
        val basis = CanonicalFoodBasis("Soya Granules (Dry)", 100.0, MacroNutrients(345, 52.0, 33.0, 0.5), "nin.res.in")

        // 1 scoop = 90ml * 0.32 g/ml density = 28.8g
        val scaled = DeterministicCalculator.scale(basis, 1.0, "scoop", "soya granules")

        assertEquals(28.8, scaled.weightGrams, 0.1)
        // 28.8g / 100g * 345 kcal = 99 kcal
        assertEquals(99, scaled.macros.calories)
        // 28.8g / 100g * 52g protein = 15.0g
        assertEquals(15.0, scaled.macros.proteinGrams, 0.1)
        assertTrue(scaled.trace.contains("28.8g"))
    }

    @Test
    fun testChapatiScaling() {
        val basis = CanonicalFoodBasis("Chapati", 100.0, MacroNutrients(297, 10.4, 57.0, 2.5), "nin.res.in")

        // 4 pieces chapati = 4 * 35g = 140g
        val scaled = DeterministicCalculator.scale(basis, 4.0, "piece", "chapati")

        assertEquals(140.0, scaled.weightGrams, 0.1)
        // 140g / 100g * 297 = 416 kcal
        assertEquals(416, scaled.macros.calories)
        assertEquals(14.6, scaled.macros.proteinGrams, 0.1)
    }

    @Test
    fun testAtwaterSanityValidator() {
        // Valid food: 100g has 10g P, 50g C, 5g F -> 4*10 + 4*50 + 9*5 = 285 kcal. Reported: 280 kcal.
        val validBasis = CanonicalFoodBasis("Test Food", 100.0, MacroNutrients(280, 10.0, 50.0, 5.0), "test.org")
        val validResult = NutritionValidator.validate(validBasis)
        assertTrue("Valid food should pass Atwater check", validResult.isValid)

        // Invalid food: Hallucinated macros (e.g. 50g P, 50g C, 50g F = 150g > 100g basis)
        val impossibleBasis = CanonicalFoodBasis("Impossible Food", 100.0, MacroNutrients(800, 50.0, 50.0, 50.0), "hallucinated.org")
        val massResult = NutritionValidator.validate(impossibleBasis)
        assertFalse("Food exceeding total physical mass must fail", massResult.isValid)

        // Severe Atwater divergence: Reported 500 kcal, but macros only add up to 200 kcal
        val divergentBasis = CanonicalFoodBasis("Divergent Food", 100.0, MacroNutrients(500, 10.0, 30.0, 4.0), "bad-data.org")
        val atwaterResult = NutritionValidator.validate(divergentBasis)
        assertFalse("Severe Atwater discrepancy must fail", atwaterResult.isValid)
    }
}
