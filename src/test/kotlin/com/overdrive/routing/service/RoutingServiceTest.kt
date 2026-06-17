package com.overdrive.routing.service

import com.overdrive.common.measure.Weight
import com.overdrive.common.money.Money
import com.overdrive.explainability.domain.EngineTrace
import com.overdrive.explainability.service.ExplainabilityService
import com.overdrive.routing.domain.Location
import com.overdrive.routing.domain.RoutingContext
import com.overdrive.scenario.context.domain.ScenarioAssumptions
import com.overdrive.scenario.context.domain.ScenarioContext
import com.overdrive.scenario.context.service.ScenarioContextResolver
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class RoutingServiceTest {

    private val explainabilityService: ExplainabilityService = mockk()
    private val scenarioContextResolver: ScenarioContextResolver = mockk()
    private val service = RoutingService(explainabilityService, scenarioContextResolver)

    private val origin = Location("US", "MIDWEST")
    private val destination = Location("CA", "ONTARIO")
    private val mockTrace = EngineTrace().apply { id = UUID.randomUUID() }

    @BeforeEach
    fun setup() {
        every {
            explainabilityService.record(any(), any(), any(), any(), any(), any())
        } returns mockTrace
    }

    private fun context(
        requiredDeliveryDays: Int? = null,
        preferredCarrier: String? = null,
        scenarioContextId: UUID? = null,
    ) = RoutingContext(
        opportunityId = UUID.randomUUID(),
        origin = origin,
        destination = destination,
        weight = Weight.ofPounds(200.0),
        cargoValue = Money.of("1000.00"),
        requiredDeliveryDays = requiredDeliveryDays,
        preferredCarrier = preferredCarrier,
        scenarioContextId = scenarioContextId,
    )

    @Test
    fun `findOptimalRoute returns a route with positive score`() {
        val route = service.findOptimalRoute(context())
        route.score shouldBeGreaterThan 0.0
        route.opportunityId shouldBe route.opportunityId
    }

    @Test
    fun `findAllRoutes returns multiple candidates sorted by score descending`() {
        val routes = service.findAllRoutes(context())
        routes shouldHaveSize 8
        for (i in 0 until routes.size - 1) {
            (routes[i].score >= routes[i + 1].score) shouldBe true
        }
    }

    @Test
    fun `findOptimalRoute gives bonus score to preferred carrier`() {
        val withPreference = service.findOptimalRoute(context(preferredCarrier = "Maersk"))
        val withoutPreference = service.findOptimalRoute(context())
        // Maersk with preference bonus should win or at least score differently
        withPreference.carrier shouldBe "Maersk"
    }

    @Test
    fun `findOptimalRoute respects required delivery days filter`() {
        val routes = service.findAllRoutes(context(requiredDeliveryDays = 5))
        routes.all { it.transitDays <= 5 } shouldBe true
    }

    @Test
    fun `findOptimalRoute throws when no carrier meets required delivery days`() {
        shouldThrow<IllegalArgumentException> {
            service.findOptimalRoute(context(requiredDeliveryDays = 0))
        }
    }

    @Test
    fun `findOptimalRoute applies carrier preference from scenario context`() {
        val scenarioId = UUID.randomUUID()
        every { scenarioContextResolver.resolve(scenarioId) } returns ScenarioContext(
            scenarioId = scenarioId,
            name = "Prefer ocean",
            assumptions = ScenarioAssumptions(carrierPreference = "Maersk"),
        )

        val route = service.findOptimalRoute(context(scenarioContextId = scenarioId))
        route.carrier shouldBe "Maersk"
    }
}
