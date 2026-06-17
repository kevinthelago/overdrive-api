package com.overdrive.opportunity.domain

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.UUID

/**
 * The computed opportunity score for one product, with all six contributing factors.
 *
 * Formula: score = SavingsPct × MarketSize × OrderFrequency × CategoryGrowth × SupplierAvailability
 *                  × LogisticsComplexity_inverted
 *
 * All factor values are normalized to [0,1]. The denominator is the inverted logistics
 * complexity (also [0,1]), floored at [COMPLEXITY_FLOOR] to avoid division by zero.
 *
 * If [zeroReason] is non-null the score is definitively zero and no further computation is performed.
 */
data class OpportunityScore(
    val productId: UUID,
    val category: String,
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
        private val COMPLEXITY_FLOOR = BigDecimal("0.05")

        fun compute(
            productId: UUID,
            category: String,
            savingsPct: OpportunityFactor,
            marketSize: OpportunityFactor,
            orderFrequency: OpportunityFactor,
            categoryGrowth: OpportunityFactor,
            supplierAvailability: OpportunityFactor,
            logisticsComplexity: OpportunityFactor,
            regionalBreakdown: List<RegionalScore> = emptyList(),
        ): OpportunityScore {
            if (supplierAvailability.normalizedValue.compareTo(BigDecimal.ZERO) == 0) {
                return zero(productId, category, ZeroReason.NO_SUPPLIER_AVAILABILITY,
                    savingsPct, marketSize, orderFrequency, categoryGrowth,
                    supplierAvailability, logisticsComplexity)
            }
            if (savingsPct.zeroReason == ZeroReason.NO_ROUTE) {
                return zero(productId, category, ZeroReason.NO_ROUTE,
                    savingsPct, marketSize, orderFrequency, categoryGrowth,
                    supplierAvailability, logisticsComplexity)
            }
            if (savingsPct.zeroReason == ZeroReason.NOT_OFFERED_BY_ANY_COMPETITOR) {
                return zero(productId, category, ZeroReason.NOT_OFFERED_BY_ANY_COMPETITOR,
                    savingsPct, marketSize, orderFrequency, categoryGrowth,
                    supplierAvailability, logisticsComplexity)
            }

            // normalizeLogisticsComplexity already inverts: high raw → low value → penalises score
            val complexityFactor = logisticsComplexity.normalizedValue.max(COMPLEXITY_FLOOR)
            val score = savingsPct.normalizedValue
                .multiply(marketSize.normalizedValue, MC)
                .multiply(orderFrequency.normalizedValue, MC)
                .multiply(categoryGrowth.normalizedValue, MC)
                .multiply(supplierAvailability.normalizedValue, MC)
                .multiply(complexityFactor, MC)
                .multiply(BigDecimal("100"), MC)
                .setScale(4, RoundingMode.HALF_UP)

            return OpportunityScore(
                productId = productId,
                category = category,
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
            productId: UUID,
            category: String,
            reason: ZeroReason,
            savingsPct: OpportunityFactor,
            marketSize: OpportunityFactor,
            orderFrequency: OpportunityFactor,
            categoryGrowth: OpportunityFactor,
            supplierAvailability: OpportunityFactor,
            logisticsComplexity: OpportunityFactor,
        ) = OpportunityScore(
            productId = productId,
            category = category,
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
