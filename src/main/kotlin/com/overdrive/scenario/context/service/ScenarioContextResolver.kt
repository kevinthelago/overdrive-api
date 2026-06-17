package com.overdrive.scenario.context.service

import com.overdrive.scenario.context.domain.ScenarioContext
import com.overdrive.scenario.context.port.ScenarioContextPort
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ScenarioContextResolver(private val scenarioContextPort: ScenarioContextPort) {
    @Cacheable(value = ["scenario-contexts"], key = "#scenarioId")
    fun resolve(scenarioId: UUID): ScenarioContext? = scenarioContextPort.findById(scenarioId)
}
