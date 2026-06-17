package com.overdrive.explainability.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.overdrive.explainability.domain.EngineTrace
import com.overdrive.explainability.domain.EngineType
import com.overdrive.explainability.domain.TraceStep
import com.overdrive.explainability.repository.TraceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ExplainabilityService(
    private val traceRepository: TraceRepository,
    private val objectMapper: ObjectMapper
) {
    @Transactional
    fun record(
        engineType: EngineType,
        opportunityId: UUID,
        scenarioContextId: UUID?,
        inputs: Any,
        outputs: Any,
        steps: List<TraceStep>
    ): EngineTrace {
        val trace = EngineTrace(
            engineType = engineType,
            opportunityId = opportunityId,
            scenarioContextId = scenarioContextId,
            inputs = objectMapper.writeValueAsString(inputs),
            outputs = objectMapper.writeValueAsString(outputs),
            steps = objectMapper.writeValueAsString(steps)
        )
        return traceRepository.save(trace)
    }

    @Transactional(readOnly = true)
    fun findById(id: UUID): EngineTrace? = traceRepository.findById(id).orElse(null)

    @Transactional(readOnly = true)
    fun findByOpportunityId(opportunityId: UUID): List<EngineTrace> =
        traceRepository.findByOpportunityId(opportunityId)

    @Transactional(readOnly = true)
    fun findByEngineTypeAndOpportunityId(engineType: EngineType, opportunityId: UUID): List<EngineTrace> =
        traceRepository.findByEngineTypeAndOpportunityId(engineType, opportunityId)
}
