package com.overdrive.opportunity.domain

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * The computed opportunity score for one product, with all six contributing factors.
 *
 * Formula: score = (SavingsPct × MarketSize × OrderFrequency × CategoryGrowth × SupplierAvailability)
 *                  / LogisticsComplexity_inverted
 *
 * All factor values are normalized to [0,1]. The denominator is the inverted logistics
 * complexity (also [0,1]), floored at [COMPLEXITY_FLOOR] to avoid division-by-zero.
 *
 * If [zeroReason] is non-null the score is definitively zero and no further computation is performed.
 */
data class OpportunityScore(
    val productId: Long,
    val categoryId: Long,
    val score: BigDecimal,
    val savingsPct: OpportunityFactor,
    val marketSize: OpportunityFactor,
    val orderFrequency: OpportunityFactor,
    val categoryGrowth: OpportunityFactor,
    val supplierAvailability: OpportunityFactor,
    val logisticsComplexity: OpportunityFactor,
    val zeroReason: ZeroReason? = null,
    val regionalBreakdown: List<RegionalScore> = emptyList(),
) {
    companion object {
        private val MC = MathContext(10, RoundingMode.HALF_UP)

        /** Prevents divide-by-zero when logistics complexity normalizes to 0. */
        private val COMPLEXITY_FLOOR = BigDecimal("0.05")

        fun compute(
            productId: Long,
            categoryId: Long,
            savingsPct: OpportunityFactor,
            marketSize: OpportunityFactor,
            orderFrequency: OpportunityFactor,
            categoryGrowth: OpportunityFactor,
            supplierAvailability: OpportunityFactor,
            logisticsComplexity: OpportunityFactor,
            regionalBreakdown: List<RegionalScore> = emptyList(),
        ): OpportunityScore {
            // Guard: supplier availability zero → no supply, score is 0.
            if (supplierAvailability.normalizedValue.compareTo(BigDecimal.ZERO) == 0) {
                return zero(productId, categoryId, ZeroReason.NO_SUPPLIER_AVAILABILITY,
                    savingsPct, marketSize, orderFrequency, categoryGrowth,
                    supplierAvailability, logisticsComplexity)
            }
            // Guard: no savings means our cost is already at or below theirs, or no-route.
            if (savingsPct.zeroReason == ZeroReason.NO_ROUTE) {
                return zero(productId, categoryId, ZeroReason.NO_ROUTE,
                    savingsPct, marketSize, orderFrequency, categoryGrowth,
                    supplierAvailability, logisticsComplexity)
            }
            if (savingsPct.zeroReason == ZeroReason.NOT_OFFERED_BY_ANY_COMPETITOR) {
                return zero(productId, categoryId, ZeroReason.NOT_OFFERED_BY_ANY_COMPETITOR,
                    savingsPct, marketSize, orderFrequency, categoryGrowth,
                    supplierAvailability, logisticsComplexity)
            }

            val denominator = logisticsComplexity.normalizedValue.max(COMPLEXITY_FLOOR)
            val numerator = savingsPct.normalizedValue
                .multiply(marketSize.normalizedValue, MC)
                .multiply(orderFrequency.normalizedValue, MC)
                .multiply(categoryGrowth.normalizedValue, MC)
                .multiply(supplierAvailability.normalizedValue, MC)

            val rawScore = numerator.divide(denominator, MC)
            // Scale to [0, 100] for readability.
            val score = rawScore.multiply(BigDecimal("100"), MC).setScale(4, RoundingMode.HALF_UP)

            return OpportunityScore(
                productId = productId,
                categoryId = categoryId,
                score = score,
                savingsPct = savingsPct,
                marketSize = marketSize,
                orderFrequency = orderFrequency,
                categoryGrowth = categoryGrowth,
                supplierAvailability = supplierAvailability,
                logisticsComplexity = logisticsComplexity,
                zeroReason = null,
                regionalBreakdown = regionalBreakdown,
            )
        }

        private fun zero(
            productId: Long,
            categoryId: Long,
            reason: ZeroReason,
            savingsPct: OpportunityFactor,
            marketSize: OpportunityFactor,
            orderFrequency: OpportunityFactor,
            categoryGrowth: OpportunityFactor,
            supplierAvailability: OpportunityFactor,
            logisticsComplexity: OpportunityFactor,
        ) = OpportunityScore(
            productId = productId,
            categoryId = categoryId,
            score = BigDecimal.ZERO,
            savingsPct = savingsPct,
            marketSize = marketSize,
            orderFrequency = orderFrequency,
            categoryGrowth = categoryGrowth,
            supplierAvailability = supplierAvailability,
            logisticsComplexity = logisticsComplexity,
            zeroReason = reason,
        )
    }
}
