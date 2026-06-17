package com.overdrive.competitor.domain

import com.overdrive.common.money.Money
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Full price comparison for a single product at a single ZIP, across all active competitors.
 * [ourDeliveredCost] is the routed cost from our network (the baseline for savings).
 */
data class CompetitorComparison(
    val productId: Long,
    val destinationZip: String,
    val ourDeliveredCost: Money,
    val perCompetitor: List<CompetitorResult>,
) {
    val medianSavingsPct: BigDecimal
        get() {
            val covered = perCompetitor.filterIsInstance<CompetitorResult.Win>() +
                perCompetitor.filterIsInstance<CompetitorResult.Loss>()
            if (covered.isEmpty()) return BigDecimal.ZERO
            val sorted = covered.map { it.savingsPct }.sorted()
            val mid = sorted.size / 2
            return if (sorted.size % 2 == 0)
                (sorted[mid - 1] + sorted[mid]).divide(BigDecimal("2"), MC)
            else
                sorted[mid]
        }
}

/**
 * Per-competitor outcome for one product/ZIP pair.
 * Savings % = (competitor delivered cost − our delivered cost) / our delivered cost.
 * Positive = we are cheaper (win); negative = competitor is cheaper (loss).
 */
sealed class CompetitorResult {

    abstract val competitorId: Long
    abstract val competitorName: String
    abstract val savingsPct: BigDecimal

    data class Win(
        override val competitorId: Long,
        override val competitorName: String,
        val ourCost: Money,
        val theirDeliveredCost: Money,
        override val savingsPct: BigDecimal,
        val sellingPriceEstimate: CompetitorEstimate,
        val deliveredCostEstimate: CompetitorEstimate,
    ) : CompetitorResult()

    data class Loss(
        override val competitorId: Long,
        override val competitorName: String,
        val ourCost: Money,
        val theirDeliveredCost: Money,
        override val savingsPct: BigDecimal,
        val sellingPriceEstimate: CompetitorEstimate,
        val deliveredCostEstimate: CompetitorEstimate,
    ) : CompetitorResult()

    data class NotOffered(
        override val competitorId: Long,
        override val competitorName: String,
        val reason: NotOfferedReason,
    ) : CompetitorResult() {
        override val savingsPct = BigDecimal.ZERO
    }

    companion object {
        private val MC = MathContext(10, RoundingMode.HALF_UP)

        fun from(
            coverage: CoverageDecision,
            ourDeliveredCost: Money,
        ): CompetitorResult = when (coverage) {
            is CoverageDecision.NotOffered -> NotOffered(
                competitorId = coverage.competitorId,
                competitorName = coverage.competitorName,
                reason = coverage.reason,
            )

            is CoverageDecision.Covered -> {
                val theirCost = coverage.deliveredCost.price
                val savings = ourDeliveredCost.amount.subtract(theirCost.amount)
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
