package com.overdrive.scenario.context.adapter

import com.overdrive.scenario.context.domain.ScenarioContext
import com.overdrive.scenario.context.port.ScenarioContextPort
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Component
import java.util.UUID

/** Fallback adapter — returns null until the scenario-analytics stream wires a real implementation. */
@Component
@ConditionalOnMissingBean(ScenarioContextPort::class)
class NoOpScenarioContextAdapter : ScenarioContextPort {
    override fun findById(scenarioId: UUID): ScenarioContext? = null
}
