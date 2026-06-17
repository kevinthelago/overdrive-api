package com.overdrive.scenario.app

import com.overdrive.scenario.context.domain.ScenarioAssumptions
import com.overdrive.scenario.context.domain.ScenarioContext
import com.overdrive.scenario.context.port.ScenarioContextPort
import com.overdrive.scenario.domain.OverrideType
import com.overdrive.scenario.domain.ScenarioRepository
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

/**
 * Bridges stored Scenario overrides into the engine-core ScenarioContextPort seam.
 * Provides a real ScenarioContextPort bean; NoOpScenarioContextAdapter backs off via
 * @ConditionalOnMissingBean when this bean is present.
 */
@Component
class ScenarioContextAdapter(
    private val scenarioRepository: ScenarioRepository,
) : ScenarioContextPort {

    override fun findById(scenarioId: UUID): ScenarioContext? {
        val scenario = scenarioRepository.findById(scenarioId).orElse(null) ?: return null

        var carrierPreference: String? = null
        val freightRateOverrides = mutableMapOf<String, BigDecimal>()
        val feeOverrides = mutableMapOf<String, BigDecimal>()

        for (override in scenario.overrides) {
            when (override.overrideType) {
                OverrideType.ADD_CARRIER -> {
                    override.params["carrierName"]?.let { carrierPreference = it }
                }
                OverrideType.FEE_SCHEDULE_CHANGE -> {
                    val newRate = override.params["newRate"]?.toBigDecimalOrNull() ?: continue
                    val key = override.entityId?.toString() ?: continue
                    feeOverrides[key] = newRate
                }
                OverrideType.DEMAND_FACTOR_CHANGE -> {
                    val newValue = override.params["newValue"]?.toBigDecimalOrNull() ?: continue
                    val factorKey = override.params["factorKey"] ?: continue
                    freightRateOverrides["freight.rate.$factorKey"] = newValue
                }
                else -> { /* warehouse/supplier overrides have no engine-seam analog */ }
            }
        }

        return ScenarioContext(
            scenarioId = scenario.id,
            name = scenario.name,
            assumptions = ScenarioAssumptions(
                freightRateOverrides = freightRateOverrides,
                feeOverrides = feeOverrides,
                carrierPreference = carrierPreference,
            ),
        )
    }
}
