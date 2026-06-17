package com.overdrive.routing.service

import com.overdrive.cost.domain.Money
import com.overdrive.cost.domain.Weight
import com.overdrive.cost.domain.WeightUnit
import com.overdrive.explainability.domain.EngineTrace
import com.overdrive.explainability.domain.EngineType
import com.overdrive.explainability.service.ExplainabilityService
import com.overdrive.routing.domain.Location
import com.overdrive.routing.domain.RoutingContext
import com.overdrive.scenario.context.domain.ScenarioAssumptions
import com.overdrive.scenario.context.domain.ScenarioContext
import com.overdrive.scenario.context.service.ScenarioContextResolver
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.UUID

class RoutingServiceTest {

    private val explainabilityService: ExplainabilityService = mockk()
    private val scenarioContextResolver: ScenarioContextResolver = mockk()
    private lateinit var service: RoutingService

    private val fakeTrace = EngineTrace(
        id = UUID.randomUUID(),
        engineType = EngineType.ROUTING,
        opportunityId = UUID.randomUUID()
    )

    @BeforeEach
    fun setUp() {
        service = RoutingService(explainabilityService, scenarioContextResolver)
        every { explainabilityService.record(any(), any(), any(), any(), any(), any()) } returns fakeTrace
    }

    @Test
    fun `findOptimalRoute returns a route with positive score`() {
        val context = baseContext()
        val route = service.findOptimalRoute(context)

        route.opportunityId shouldBe context.opportunityId
        route.score shouldBeGreaterThan 0.0
        route.estimatedCost.amount shouldBeGreaterThan BigDecimal.ZERO
        route.traceId shouldBe fakeTrace.id
    }

    @Test
    fun `findOptimalRoute respects required delivery days`() {
        val context = baseContext().copy(requiredDeliveryDays = 3)
        val route = service.findOptimalRoute(context)

        route.transitDays shouldBeGreaterThan 0
        assert(route.transitDays <= 3) { "Expected transit days <= 3 but got ${route.transitDays}" }
    }

    @Test
    fun `findOptimalRoute throws when no carrier meets required delivery days`() {
        val context = baseContext().copy(requiredDeliveryDays = 0)
        assertThrows<IllegalArgumentException> { service.findOptimalRoute(context) }
    }

    @Test
    fun `findOptimalRoute gives winning score to preferred carrier`() {
        // UPS baseline score (~0.79) is close to FedEx (~0.791); the 0.10 bonus pushes UPS to ~0.89
        val context = baseContext().copy(preferredCarrier = "UPS")
        val route = service.findOptimalRoute(context)

        route.carrier shouldBe "UPS"
    }

    @Test
    fun `findAllRoutes returns multiple candidates sorted by score`() {
        val context = baseContext()
        val routes = service.findAllRoutes(context)

        routes.size shouldBeGreaterThan 1
        val scores = routes.map { it.score }
        scores shouldBe scores.sortedDescending()
    }

    @Test
    fun `findOptimalRoute applies carrier preference from scenario context`() {
        val scenarioId = UUID.randomUUID()
        every { scenarioContextResolver.resolve(scenarioId) } returns ScenarioContext(
            scenarioId = scenarioId,
            name = "Prefer rail",
            assumptions = ScenarioAssumptions(carrierPreference = "BNSF")
        )
        val context = baseContext().copy(scenarioContextId = scenarioId)
        val route = service.findOptimalRoute(context)

        route.carrier shouldBe "BNSF"
    }

    private fun baseContext() = RoutingContext(
        opportunityId = UUID.randomUUID(),
        origin = Location("CN", portCode = "CNSHA"),
        destination = Location("US", portCode = "USLAX"),
        weight = Weight(BigDecimal("500"), WeightUnit.LBS),
        cargoValue = Money(BigDecimal("10000.00")),
        scenarioContextId = null
    )
}
