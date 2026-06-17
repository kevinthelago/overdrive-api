package com.overdrive.explainability.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.overdrive.explainability.domain.EngineTrace
import com.overdrive.explainability.domain.EngineType
import com.overdrive.explainability.domain.TraceStep
import com.overdrive.explainability.repository.TraceRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.util.Optional
import java.util.UUID

class ExplainabilityServiceTest {

    private val traceRepository: TraceRepository = mockk()
    private val objectMapper = ObjectMapper().findAndRegisterModules()
    private val service = ExplainabilityService(traceRepository, objectMapper)

    @Test
    fun `record saves trace and returns persisted entity`() {
        val slot = slot<EngineTrace>()
        every { traceRepository.save(capture(slot)) } answers { slot.captured }

        val opportunityId = UUID.randomUUID()
        val result = service.record(
            engineType = EngineType.COST,
            opportunityId = opportunityId,
            scenarioContextId = null,
            inputs = mapOf("carrier" to "UPS"),
            outputs = mapOf("total" to "200.00"),
            steps = emptyList(),
        )

        verify(exactly = 1) { traceRepository.save(any()) }
        result.engineType shouldBe EngineType.COST
        result.opportunityId shouldBe opportunityId
        result.scenarioContextId shouldBe null
    }

    @Test
    fun `record serializes inputs to valid JSON string`() {
        val slot = slot<EngineTrace>()
        every { traceRepository.save(capture(slot)) } answers { slot.captured }

        service.record(
            engineType = EngineType.ROUTING,
            opportunityId = UUID.randomUUID(),
            scenarioContextId = null,
            inputs = mapOf("carrier" to "Maersk", "days" to 21),
            outputs = mapOf("score" to 1.5),
            steps = listOf(TraceStep(1, "Route scored", mapOf("rate" to "0.55"), mapOf("score" to 1.5))),
        )

        val captured = slot.captured
        captured.inputs shouldNotBe null
        val parsed = objectMapper.readTree(captured.inputs)
        parsed.get("carrier").asText() shouldBe "Maersk"
    }

    @Test
    fun `findById returns trace when found`() {
        val trace = EngineTrace().apply { id = UUID.randomUUID() }
        every { traceRepository.findById(trace.id) } returns Optional.of(trace)

        val result = service.findById(trace.id)
        result shouldBe trace
    }

    @Test
    fun `findById returns null when not found`() {
        val id = UUID.randomUUID()
        every { traceRepository.findById(id) } returns Optional.empty()

        val result = service.findById(id)
        result shouldBe null
    }

    @Test
    fun `findByOpportunityId delegates to repository`() {
        val opportunityId = UUID.randomUUID()
        val traces = listOf(EngineTrace(), EngineTrace())
        every { traceRepository.findByOpportunityId(opportunityId) } returns traces

        val result = service.findByOpportunityId(opportunityId)
        result shouldBe traces
    }
}
