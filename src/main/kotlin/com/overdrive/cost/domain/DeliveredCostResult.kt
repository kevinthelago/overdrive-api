package com.overdrive.cost.domain

import java.io.Serializable
import java.time.Instant
import java.util.UUID

data class DeliveredCostResult(
    val id: UUID,
    val opportunityId: UUID,
    val scenarioContextId: UUID?,
    val freightCost: FreightCost,
    val dutyCost: DutyCost,
    val fees: List<FeeComponent>,
    val totalDeliveredCost: Money,
    val traceId: UUID,
    val calculatedAt: Instant
) : Serializable
