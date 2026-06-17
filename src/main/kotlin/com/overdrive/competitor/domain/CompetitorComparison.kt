package com.overdrive.competitor.domain

import com.overdrive.common.money.Money
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.UUID

/**
 * Full price comparison for a single product at a single destination region,
 * across all active competitors. [ourDeliveredCost] is the routing engine's cost
 * (product.cost + freight) — the baseline for savings calculations.
 */
data class CompetitorComparison(
    val productId: UUID,
    val destinationZip: String,
    val ourDeliveredCost: Money,
    val perCompetitor: List<CompetitorResult>,
) {
    val medianSavingsPct: BigDecimal
        get() {
            val savingsValues = perCompetitor
                .filterNot { it is CompetitorResult.NotOffered }
                .map { it.savingsPct }
            if (savingsValues.isEmpty()) return BigDecimal.ZERO
            val sorted = savingsValues.sorted()
            val mid = sorted.size / 2
            return if (sorted.size % 2 == 0)
                (sorted[mid - 1] + sorted[mid]).divide(BigDecimal("2"), 10, RoundingMode.HALF_UP)
            else
                sorted[mid]
        }
}

/**
 * Per-competitor outcome for one product/ZIP pair.
 * Savings % = (their delivered cost − our delivered cost) / our delivered cost.
 * Positive = competitor is more expensive (we win); negative = competitor is cheaper (we lose).
 */
sealed class CompetitorResult {
    abstract val competitorId: UUID
    abstract val competitorName: String
    abstract val savingsPct: BigDecimal

    data class Win(
        override val competitorId: UUID,
        override val competitorName: String,
        val ourCost: Money,
        val theirDeliveredCost: Money,
        override val savingsPct: BigDecimal,
        val sellingPriceEstimate: CompetitorEstimate,
        val deliveredCostEstimate: CompetitorEstimate,
    ) : CompetitorResult()

    data class Loss(
        override val competitorId: UUID,
        override val competitorName: String,
        val ourCost: Money,
        val theirDeliveredCost: Money,
        override val savingsPct: BigDecimal,
        val sellingPriceEstimate: CompetitorEstimate,
        val deliveredCostEstimate: CompetitorEstimate,
    ) : CompetitorResult()

    data class NotOffered(
        override val competitorId: UUID,
        override val competitorName: String,
        val reason: NotOfferedReason,
    ) : CompetitorResult() {
        override val savingsPct = BigDecimal.ZERO
    }

    companion object {
        private val MC = MathContext(10, RoundingMode.HALF_UP)

        fun from(coverage: CoverageDecision, ourDeliveredCost: Money): CompetitorResult = when (coverage) {
            is CoverageDecision.NotOffered -> NotOffered(
                competitorId = coverage.competitorId,
                competitorName = coverage.competitorName,
                reason = coverage.reason,
            )

            is CoverageDecision.Covered -> {
                val theirCost = coverage.deliveredCost.price
                // Positive savings = we are competitive (their cost is higher)
                val savings = theirCost.amount.subtract(ourDeliveredCost.amount)
                    .divide(ourDeliveredCost.amount, MC)
                if (savings >= BigDecimal.ZERO)
                    Win(
                        competitorId = coverage.competitorId,
                        competitorName = coverage.competitorName,
                        ourCost = ourDeliveredCost,
                        theirDeliveredCost = theirCost,
                        savingsPct = savings,
                        sellingPriceEstimate = coverage.sellingPrice,
                        deliveredCostEstimate = coverage.deliveredCost,
                    )
                else
                    Loss(
                        competitorId = coverage.competitorId,
                        competitorName = coverage.competitorName,
                        ourCost = ourDeliveredCost,
                        theirDeliveredCost = theirCost,
                        savingsPct = savings,
                        sellingPriceEstimate = coverage.sellingPrice,
                        deliveredCostEstimate = coverage.deliveredCost,
                    )
            }
        }
    }
}
