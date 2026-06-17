package com.overdrive.opportunity.dto

import com.overdrive.opportunity.domain.FactorType
import com.overdrive.opportunity.domain.OpportunityScore
import com.overdrive.opportunity.domain.RegionalScore
import com.overdrive.opportunity.domain.ZeroReason
import java.math.BigDecimal
import java.util.UUID

data class OpportunityResponse(
    val products: List<OpportunityResult>,
    val categoryRankings: List<CategoryOpportunity>,
)

data class OpportunityResult(
    val productId: UUID,
    val category: String,
    val score: BigDecimal,
    val zeroReason: ZeroReason?,
    val factors: Map<FactorType, FactorDto>,
    val regionalBreakdown: List<RegionalScoreDto>,
) {
    companion object {
        fun from(domain: OpportunityScore) = OpportunityResult(
            productId = domain.productId,
            category = domain.category,
            score = domain.score,
            zeroReason = domain.zeroReason,
            factors = mapOf(
                FactorType.SAVINGS_PCT to FactorDto(domain.savingsPct.rawValue, domain.savingsPct.normalizedValue),
                FactorType.MARKET_SIZE to FactorDto(domain.marketSize.rawValue, domain.marketSize.normalizedValue),
                FactorType.ORDER_FREQUENCY to FactorDto(domain.orderFrequency.rawValue, domain.orderFrequency.normalizedValue),
                FactorType.CATEGORY_GROWTH to FactorDto(domain.categoryGrowth.rawValue, domain.categoryGrowth.normalizedValue),
                FactorType.SUPPLIER_AVAILABILITY to FactorDto(domain.supplierAvailability.rawValue, domain.supplierAvailability.normalizedValue),
                FactorType.LOGISTICS_COMPLEXITY to FactorDto(domain.logisticsComplexity.rawValue, domain.logisticsComplexity.normalizedValue),
            ),
            regionalBreakdown = domain.regionalBreakdown.map { RegionalScoreDto.from(it) },
        )
    }
}

data class FactorDto(
    val rawValue: BigDecimal,
    val normalizedValue: BigDecimal,
)

data class RegionalScoreDto(
    val region: String,
    val representativeZip: String,
    val score: BigDecimal,
    val savingsPct: BigDecimal,
) {
    companion object {
        fun from(domain: RegionalScore) = RegionalScoreDto(
            region = domain.region,
            representativeZip = domain.representativeZip,
            score = domain.score,
            savingsPct = domain.savingsPct,
        )
    }
}

data class CategoryOpportunity(
    val category: String,
    val aggregateScore: BigDecimal,
    val productCount: Int,
    val topProductId: UUID?,
)
