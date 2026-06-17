package com.overdrive.cost.domain

import java.math.BigDecimal
import java.util.UUID

data class CostCalculationContext(
    val opportunityId: UUID,
    val originCountry: String,
    val destinationCountry: String,
    val productHsCode: String,
    val weight: Weight,
    val declaredCargoValue: Money,
    val carrier: String,
    val transportMode: TransportMode,
    val scenarioContextId: UUID? = null,
    val scenarioOverrides: Map<String, BigDecimal> = emptyMap()
)
