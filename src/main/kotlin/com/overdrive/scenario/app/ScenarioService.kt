package com.overdrive.scenario.app

import com.overdrive.scenario.domain.Scenario
import com.overdrive.scenario.domain.ScenarioOverrideEntity
import com.overdrive.scenario.domain.ScenarioRepository
import com.overdrive.scenario.web.ScenarioCreateRequest
import com.overdrive.scenario.web.ScenarioUpdateRequest
import com.overdrive.scenario.web.OverrideRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

@Service
@Transactional
class ScenarioService(
    private val scenarioRepository: ScenarioRepository,
) {

    @Transactional(readOnly = true)
    fun listAll(): List<Scenario> = scenarioRepository.findAll()

    @Transactional(readOnly = true)
    fun getById(id: UUID): Scenario =
        scenarioRepository.findById(id).orElseThrow {
            ScenarioNotFoundException(id)
        }

    fun create(request: ScenarioCreateRequest): Scenario {
        if (scenarioRepository.existsByName(request.name)) {
            throw ScenarioNameConflictException(request.name)
        }
        val scenario = Scenario(name = request.name, description = request.description)
        request.overrides.forEachIndexed { idx, req ->
            scenario.addOverride(req.toEntity(scenario, idx + 1))
        }
        return scenarioRepository.save(scenario)
    }

    fun update(id: UUID, request: ScenarioUpdateRequest): Scenario {
        val scenario = getById(id)
        if (scenarioRepository.existsByNameAndIdNot(request.name, id)) {
            throw ScenarioNameConflictException(request.name)
        }
        scenario.name = request.name
        scenario.description = request.description
        scenario.clearOverrides()
        request.overrides.forEachIndexed { idx, req ->
            scenario.addOverride(req.toEntity(scenario, idx + 1))
        }
        scenario.updatedAt = OffsetDateTime.now()
        return scenarioRepository.save(scenario)
    }

    fun delete(id: UUID) {
        if (!scenarioRepository.existsById(id)) throw ScenarioNotFoundException(id)
        scenarioRepository.deleteById(id)
    }
}

class ScenarioNotFoundException(id: UUID) :
    RuntimeException("Scenario not found: $id")

class ScenarioNameConflictException(name: String) :
    RuntimeException("A scenario named '$name' already exists")

private fun OverrideRequest.toEntity(scenario: Scenario, position: Int) =
    ScenarioOverrideEntity(
        scenario = scenario,
        overrideType = overrideType,
        position = position,
        entityId = entityId,
        params = params,
    )
