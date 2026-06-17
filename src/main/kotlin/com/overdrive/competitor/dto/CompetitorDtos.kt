package com.overdrive.competitor.dto

import com.overdrive.catalog.domain.DistributionModel
import com.overdrive.competitor.domain.CompetitorComparison
import com.overdrive.competitor.domain.CompetitorResult
import com.overdrive.competitor.domain.NotOfferedReason
import java.math.BigDecimal

data class CompetitorComparisonResponse(
    val productId: Long,
    val destinationZip: String,
    val ourDeliveredCostUsd: BigDecimal,
    val medianSavingsPct: BigDecimal,
    val competitors: List<CompetitorResultDto>,
) {
    companion object {
        fun from(domain: CompetitorComparison) = CompetitorComparisonResponse(
            productId = domain.productId,
            destinationZip = domain.destinationZip,
            ourDeliveredCostUsd = domain.ourDeliveredCost.amount,
            medianSavingsPct = domain.medianSavingsPct,
            competitors = domain.perCompetitor.map { CompetitorResultDto.from(it) },
        )
    }
}

sealed class CompetitorResultDto {
    abstract val competitorId: Long
    abstract val competitorName: String
    abstract val offered: Boolean

    data class CoveredDto(
        override val competitorId: Long,
        override val competitorName: String,
        val distributionModel: DistributionModel,
        val sellingPriceUsd: BigDecimal,
        val deliveredCostUsd: BigDecimal,
        val savingsPct: BigDecimal,
        val weWin: Boolean,
        val assumptions: Map<String, Any>,
    ) : CompetitorResultDto() {
        override val offered = true
    }

    data class NotOfferedDto(
        override val competitorId: Long,
        override val competitorName: String,
        val reason: NotOfferedReason,
    ) : CompetitorResultDto() {
        override val offered = false
    }

    companion object {
        fun from(domain: CompetitorResult): CompetitorResultDto = when (domain) {
            is CompetitorResult.Win -> CoveredDto(
                competitorId = domain.competitorId,
                competitorName = domain.competitorName,
                distributionModel = domain.sellingPriceEstimate.assumptions["strategy"]
                    .let { DistributionModel.valueOf(it.toString()) },
                sellingPriceUsd = domain.sellingPriceEstimate.price.amount,
                deliveredCostUsd = domain.deliveredCostEstimate.price.amount,
                savingsPct = domain.savingsPct,
                weWin = true,
                assumptions = domain.deliveredCostEstimate.assumptions,
            )

            is CompetitorResult.Loss -> CoveredDto(
                competitorId = domain.competitorId,
                competitorName = domain.competitorName,
                distributionModel = domain.sellingPriceEstimate.assumptions["strategy"]
                    .let { DistributionModel.valueOf(it.toString()) },
                sellingPriceUsd = domain.sellingPriceEstimate.price.amount,
                deliveredCostUsd = domain.deliveredCostEstimate.price.amount,
                savingsPct = domain.savingsPct,
                weWin = false,
                assumptions = domain.deliveredCostEstimate.assumptions,
            )

            is CompetitorResult.NotOffered -> NotOfferedDto(
                competitorId = domain.competitorId,
                competitorName = domain.competitorName,
                reason = domain.reason,
            )
        }
    }
}

data class BatchComparisonResponse(
    val categoryId: Long,
    val destinationZip: String,
    val comparisons: List<CompetitorComparisonResponse>,
)
