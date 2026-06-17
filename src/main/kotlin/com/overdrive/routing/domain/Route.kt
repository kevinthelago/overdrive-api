package com.overdrive.routing.domain

import com.overdrive.cost.domain.Money
import java.util.UUID

/** Stub — real implementation owned by engine-core-api stream. */
data class Route(
    val routeId: UUID = UUID.randomUUID(),
    val estimatedCost: Money,
)
