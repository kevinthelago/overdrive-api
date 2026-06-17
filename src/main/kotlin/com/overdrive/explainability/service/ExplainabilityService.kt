package com.overdrive.explainability.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.overdrive.explainability.domain.EngineTrace
import com.overdrive.explainability.domain.EngineType
import com.overdrive.explainability.domain.TraceStep
import com.overdrive.explainability.repository.TraceRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class ExplainabilityService(
    private val traceRepository: TraceRepository,
    private val objectMapper: ObjectMapper,
) {
    fun record(
        engineType: EngineType,
        opportunityId: UUID,
        scenarioContextId: UUID?,
        inputs: Any,
        outputs: Any,
        steps: List<TraceStep>,
    ): EngineTrace {
        val trace = EngineTrace().apply {
            this.engineType = engineType
            this.opportunityId = opportunityId
            this.scenarioContextId = scenarioContextId
            this.inputs = objectMapper.writeValueAsString(inputs)
            this.outputs = objectMapper.writeValueAsString(outputs)
            this.steps = objectMapper.writeValueAsString(steps)
            this.computedAt = Instant.now()
        }
        return traceRepository.save(trace)
    }

    fun findById(id: UUID): EngineTrace? = traceRepository.findById(id).orElse(null)

    fun findByOpportunityId(opportunityId: UUID): List<EngineTrace> =
        traceRepository.findByOpportunityId(opportunityId)

    fun findByEngineTypeAndOpportunityId(engineType: EngineType, opportunityId: UUID): List<EngineTrace> =
        traceRepository.findByEngineTypeAndOpportunityId(engineType, opportunityId)
}
