package com.overdrive.routing.domain

import com.overdrive.common.money.Money
import com.overdrive.cost.domain.TransportMode
import java.time.Instant
import java.util.UUID

data class Route(
    val id: UUID,
    val opportunityId: UUID,
    val scenarioContextId: UUID?,
    val origin: Location,
    val destination: Location,
    val carrier: String,
    val mode: TransportMode,
    val transitDays: Int,
    val estimatedCost: Money,
    val score: Double,
    val traceId: UUID,
    val calculatedAt: Instant,
) : java.io.Serializable
