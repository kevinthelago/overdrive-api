package com.overdrive.routing.domain

import com.overdrive.cost.domain.Money
import com.overdrive.cost.domain.Weight
import java.util.UUID

/** Stub — real implementation owned by engine-core-api stream. */
data class RoutingContext(
    val opportunityId: UUID,
    val origin: Location,
    val destination: Location,
    val weight: Weight,
    val cargoValue: Money,
    val scenarioContextId: UUID? = null,
)
