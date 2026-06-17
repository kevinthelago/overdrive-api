package com.overdrive.competitor.domain

import java.util.UUID

/** Result of the coverage check that precedes every estimate. */
sealed class CoverageDecision {

    data class Covered(
        val competitorId: UUID,
        val competitorName: String,
        val competitorType: CompetitorType,
        val sellingPrice: CompetitorEstimate,
        val deliveredCost: CompetitorEstimate,
    ) : CoverageDecision()

    data class NotOffered(
        val competitorId: UUID,
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
