package com.overdrive.competitor.domain

import com.overdrive.catalog.domain.DistributionModel

/** Result of the coverage check that precedes every estimate. */
sealed class CoverageDecision {

    /** Competitor covers this product/region; [estimate] is the selling price estimate. */
    data class Covered(
        val competitorId: Long,
        val competitorName: String,
        val model: DistributionModel,
        val sellingPrice: CompetitorEstimate,
        val deliveredCost: CompetitorEstimate,
    ) : CoverageDecision()

    /** Competitor does not serve this product or region; no price is fabricated. */
    data class NotOffered(
        val competitorId: Long,
        val competitorName: String,
        val reason: NotOfferedReason,
    ) : CoverageDecision()
}

enum class NotOfferedReason {
    OUT_OF_REGION,
    CATEGORY_RESTRICTION,
    HAZMAT_RESTRICTION,
    TEMPERATURE_RESTRICTION,
    INSUFFICIENT_WAREHOUSE_FOOTPRINT,
}
