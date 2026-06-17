package com.overdrive.explainability.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional
import java.util.UUID

class ExplainabilityServiceTest {

    private val traceRepository: TraceRepository = mockk()
    private val objectMapper = ObjectMapper().registerKotlinModule()
    private lateinit var service: ExplainabilityService

    @BeforeEach
    fun setUp() {
        service = ExplainabilityService(traceRepository, objectMapper)
    }

    @Test
    fun `record saves trace and returns persisted entity`() {
        val opportunityId = UUID.randomUUID()
        val savedSlot = slot<EngineTrace>()
        every { traceRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val steps = listOf(TraceStep(1, "step one", mapOf("a" to 1), mapOf("b" to 2), "rule"))
        val trace = service.record(
            engineType = EngineType.COST,
            opportunityId = opportunityId,
            scenarioContextId = null,
            inputs = mapOf("key" to "value"),
            outputs = mapOf("result" to 42),
            steps = steps
        )

        verify(exactly = 1) { traceRepository.save(any()) }
        trace.engineType shouldBe EngineType.COST
        trace.opportunityId shouldBe opportunityId
        trace.inputs shouldNotBe null
        trace.steps shouldNotBe null
    }

    @Test
    fun `record serializes inputs to valid JSON string`() {
        val savedSlot = slot<EngineTrace>()
        every { traceRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        service.record(
            engineType = EngineType.ROUTING,
            opportunityId = UUID.randomUUID(),
            scenarioContextId = null,
            inputs = mapOf("carrier" to "UPS", "weight" to 100),
            outputs = mapOf("score" to 0.85),
            steps = emptyList()
        )

        val saved = savedSlot.captured
        val parsed = objectMapper.readTree(saved.inputs)
        parsed["carrier"].asText() shouldBe "UPS"
        parsed["weight"].asInt() shouldBe 100
    }

    @Test
    fun `findById returns trace when found`() {
        val id = UUID.randomUUID()
        val trace = EngineTrace(id = id, engineType = EngineType.COST, opportunityId = UUID.randomUUID())
        every { traceRepository.findById(id) } returns Optional.of(trace)

        service.findById(id) shouldBe trace
    }

    @Test
    fun `findById returns null when not found`() {
        val id = UUID.randomUUID()
        every { traceRepository.findById(id) } returns Optional.empty()

        service.findById(id) shouldBe null
    }

    @Test
    fun `findByOpportunityId delegates to repository`() {
        val opportunityId = UUID.randomUUID()
        val traces = listOf(
            EngineTrace(engineType = EngineType.COST, opportunityId = opportunityId),
            EngineTrace(engineType = EngineType.ROUTING, opportunityId = opportunityId)
        )
        every { traceRepository.findByOpportunityId(opportunityId) } returns traces

        service.findByOpportunityId(opportunityId) shouldBe traces
    }
}
