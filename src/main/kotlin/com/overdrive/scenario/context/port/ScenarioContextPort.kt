package com.overdrive.scenario.context.port

import com.overdrive.scenario.context.domain.ScenarioContext
import java.util.UUID

/**
 * Port implemented by the scenario-analytics stream.
 * Engine core uses this seam to fetch scenario-specific overrides
 * without coupling to the scenario storage implementation.
 */
interface ScenarioContextPort {
    fun findById(scenarioId: UUID): ScenarioContext?
}
