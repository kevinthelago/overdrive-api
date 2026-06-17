package com.overdrive.scenario.context.port

import com.overdrive.scenario.context.domain.ScenarioContext
import java.util.UUID

/**
 * Port implemented by the scenario-analytics stream.
 * Decouples engine-core from scenario storage implementation.
 */
interface ScenarioContextPort {
    fun findById(scenarioId: UUID): ScenarioContext?
}
