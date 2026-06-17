package com.overdrive.competitor.domain

import com.overdrive.common.money.Money

/**
 * An immutable price estimate produced by a [CompetitorStrategy].
 * [assumptions] captures every input used, satisfying the explain affordance.
 */
data class CompetitorEstimate(
    val price: Money,
    val assumptions: Map<String, Any>,
)
