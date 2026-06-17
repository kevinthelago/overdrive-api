package com.overdrive.scenario.context.adapter

import com.overdrive.scenario.context.domain.ScenarioContext
import com.overdrive.scenario.context.port.ScenarioContextPort
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Component
import java.util.UUID

/** Default adapter — returns null for all scenarios until the scenario-analytics stream provides a real impl. */
@Component
@ConditionalOnMissingBean(ScenarioContextPort::class)
class NoOpScenarioContextAdapter : ScenarioContextPort {
    override fun findById(scenarioId: UUID): ScenarioContext? = null
}
