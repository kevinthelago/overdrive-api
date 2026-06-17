package com.overdrive.scenario.context.domain

import java.math.BigDecimal
import java.util.UUID

data class ScenarioAssumptions(
    val freightRateOverrides: Map<String, BigDecimal> = emptyMap(),
    val dutyRateOverrides: Map<String, BigDecimal> = emptyMap(),
    val feeOverrides: Map<String, BigDecimal> = emptyMap(),
    val carrierPreference: String? = null,
)

data class ScenarioContext(
    val scenarioId: UUID,
    val name: String,
    val assumptions: ScenarioAssumptions,
)
