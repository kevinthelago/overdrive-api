package com.overdrive

import com.overdrive.explainability.domain.EngineType
import com.overdrive.explainability.domain.TraceStep
import com.overdrive.explainability.repository.TraceRepository
import com.overdrive.explainability.service.ExplainabilityService
import com.overdrive.support.AbstractIntegrationTest
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class ExplainabilityIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var explainabilityService: ExplainabilityService

    @Autowired
    private lateinit var traceRepository: TraceRepository

    @Test
    fun `record persists trace to postgres and can be retrieved by id`() {
        val opportunityId = UUID.randomUUID()
        val steps = listOf(
            TraceStep(1, "compute freight", mapOf("rate" to "1.80"), mapOf("cost" to "180.00"), "cost = rate × weight"),
        )

        val trace = explainabilityService.record(
            engineType = EngineType.COST,
            opportunityId = opportunityId,
            scenarioContextId = null,
            inputs = mapOf("carrier" to "UPS", "weight" to 100),
            outputs = mapOf("total" to "180.00"),
            steps = steps,
        )

        trace.id shouldNotBe null

        val found = explainabilityService.findById(trace.id)
        found shouldNotBe null
        found!!.engineType shouldBe EngineType.COST
        found.opportunityId shouldBe opportunityId
    }

    @Test
    fun `findByOpportunityId returns all traces for an opportunity`() {
        val opportunityId = UUID.randomUUID()

        explainabilityService.record(EngineType.COST, opportunityId, null,
            mapOf("step" to 1), mapOf("result" to "a"), emptyList())
        explainabilityService.record(EngineType.ROUTING, opportunityId, null,
            mapOf("step" to 2), mapOf("result" to "b"), emptyList())

        val traces = explainabilityService.findByOpportunityId(opportunityId)
        traces shouldHaveSize 2
        traces.map { it.engineType }.toSet() shouldBe setOf(EngineType.COST, EngineType.ROUTING)
    }

    @Test
    fun `findByEngineTypeAndOpportunityId filters by engine type`() {
        val opportunityId = UUID.randomUUID()

        explainabilityService.record(EngineType.COST, opportunityId, null,
            mapOf("x" to 1), mapOf("y" to 1), emptyList())
        explainabilityService.record(EngineType.ROUTING, opportunityId, null,
            mapOf("x" to 2), mapOf("y" to 2), emptyList())

        val costTraces = explainabilityService.findByEngineTypeAndOpportunityId(EngineType.COST, opportunityId)
        costTraces shouldHaveSize 1
        costTraces.first().engineType shouldBe EngineType.COST
    }
}
