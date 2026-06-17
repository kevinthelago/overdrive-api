package com.overdrive.cost.domain

import com.overdrive.common.measure.Weight
import com.overdrive.common.money.Money
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
    val scenarioOverrides: Map<String, BigDecimal> = emptyMap(),
)
