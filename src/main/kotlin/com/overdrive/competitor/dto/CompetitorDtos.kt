package com.overdrive.competitor.dto

import com.overdrive.competitor.domain.CompetitorComparison
import com.overdrive.competitor.domain.CompetitorResult
import com.overdrive.competitor.domain.CompetitorType
import com.overdrive.competitor.domain.NotOfferedReason
import java.math.BigDecimal
import java.util.UUID

data class CompetitorComparisonResponse(
    val productId: UUID,
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
    abstract val competitorId: UUID
    abstract val competitorName: String
    abstract val offered: Boolean

    data class CoveredDto(
        override val competitorId: UUID,
        override val competitorName: String,
        val competitorType: CompetitorType,
        val sellingPriceUsd: BigDecimal,
        val deliveredCostUsd: BigDecimal,
        val savingsPct: BigDecimal,
        val weWin: Boolean,
        val assumptions: Map<String, Any>,
    ) : CompetitorResultDto() {
        override val offered = true
    }

    data class NotOfferedDto(
        override val competitorId: UUID,
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
                competitorType = CompetitorType.fromCatalogModel(
                    domain.sellingPriceEstimate.assumptions["strategy"]?.toString(),
                ),
                sellingPriceUsd = domain.sellingPriceEstimate.price.amount,
                deliveredCostUsd = domain.deliveredCostEstimate.price.amount,
                savingsPct = domain.savingsPct,
                weWin = true,
                assumptions = domain.deliveredCostEstimate.assumptions,
            )

            is CompetitorResult.Loss -> CoveredDto(
                competitorId = domain.competitorId,
                competitorName = domain.competitorName,
                competitorType = CompetitorType.fromCatalogModel(
                    domain.sellingPriceEstimate.assumptions["strategy"]?.toString(),
                ),
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
    val category: String,
    val destinationZip: String,
    val comparisons: List<CompetitorComparisonResponse>,
)
