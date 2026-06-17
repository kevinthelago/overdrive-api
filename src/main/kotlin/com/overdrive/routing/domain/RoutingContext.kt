package com.overdrive.routing.domain

import com.overdrive.common.measure.Weight
import com.overdrive.common.money.Money
import java.math.BigDecimal
import java.util.UUID

data class RoutingContext(
    val opportunityId: UUID,
    val origin: Location,
    val destination: Location,
    val weight: Weight,
    val cargoValue: Money,
    val requiredDeliveryDays: Int? = null,
    val preferredCarrier: String? = null,
    val scenarioContextId: UUID? = null,
    val scenarioOverrides: Map<String, BigDecimal> = emptyMap(),
)
