package com.overdrive.cost.service

import com.overdrive.cost.domain.CostCalculationContext
import com.overdrive.cost.domain.Money
import com.overdrive.cost.domain.TransportMode
import com.overdrive.cost.domain.Weight
import com.overdrive.cost.domain.WeightUnit
import com.overdrive.explainability.domain.EngineTrace
import com.overdrive.explainability.domain.EngineType
import com.overdrive.explainability.service.ExplainabilityService
import com.overdrive.scenario.context.domain.ScenarioAssumptions
import com.overdrive.scenario.context.domain.ScenarioContext
import com.overdrive.scenario.context.service.ScenarioContextResolver
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class CostCalculationServiceTest {

    private val explainabilityService: ExplainabilityService = mockk()
    private val scenarioContextResolver: ScenarioContextResolver = mockk()
    private lateinit var service: CostCalculationService

    private val fakeTrace = EngineTrace(
        id = UUID.randomUUID(),
        engineType = EngineType.COST,
        opportunityId = UUID.randomUUID()
    )

    @BeforeEach
    fun setUp() {
        service = CostCalculationService(explainabilityService, scenarioContextResolver)
        every { explainabilityService.record(any(), any(), any(), any(), any(), any()) } returns fakeTrace
    }

    @Test
    fun `calculate returns positive total delivered cost for standard inputs`() {
        val opportunityId = UUID.randomUUID()
        val context = baseContext(opportunityId)

        val result = service.calculate(context)

        result.opportunityId shouldBe opportunityId
        result.totalDeliveredCost.amount shouldBeGreaterThan BigDecimal.ZERO
        result.traceId shouldBe fakeTrace.id
        result.freightCost.amount.amount shouldBeGreaterThan BigDecimal.ZERO
        result.dutyCost.amount.amount shouldBeGreaterThan BigDecimal.ZERO
        result.fees shouldNotBe emptyList<Any>()
    }

    @Test
    fun `calculate applies freight rate override from scenario overrides`() {
        val opportunityId = UUID.randomUUID()
        val highRate = BigDecimal("10.00")
        val context = baseContext(opportunityId).copy(
            scenarioOverrides = mapOf("freight.rate.UPS" to highRate)
        )

        val result = service.calculate(context)
        val weightKg = context.weight.toKg().value
        val expectedFreight = (highRate * weightKg).setScale(4, java.math.RoundingMode.HALF_UP)

        result.freightCost.amount.amount shouldBe expectedFreight
    }

    @Test
    fun `calculate applies duty rate override from scenario overrides`() {
        val opportunityId = UUID.randomUUID()
        val zeroDutyRate = BigDecimal("0.00")
        val context = baseContext(opportunityId).copy(
            scenarioOverrides = mapOf("duty.rate.8471.30" to zeroDutyRate)
        )

        val result = service.calculate(context)

        result.dutyCost.amount.amount shouldBe BigDecimal("0.0000")
    }

    @Test
    fun `calculate applies overrides from scenario context via resolver`() {
        val scenarioId = UUID.randomUUID()
        val scenarioContext = ScenarioContext(
            scenarioId = scenarioId,
            name = "High tariff scenario",
            assumptions = ScenarioAssumptions(
                dutyRateOverrides = mapOf("duty.rate.8471.30" to BigDecimal("0.50"))
            )
        )
        every { scenarioContextResolver.resolve(scenarioId) } returns scenarioContext

        val context = baseContext(UUID.randomUUID()).copy(scenarioContextId = scenarioId)
        val result = service.calculate(context)

        val expectedDuty = (context.declaredCargoValue.amount * BigDecimal("0.50")).setScale(4, java.math.RoundingMode.HALF_UP)
        result.dutyCost.amount.amount shouldBe expectedDuty
    }

    @Test
    fun `calculate records a trace via explainability service`() {
        val context = baseContext(UUID.randomUUID())
        service.calculate(context)

        verify(exactly = 1) {
            explainabilityService.record(
                engineType = EngineType.COST,
                opportunityId = context.opportunityId,
                scenarioContextId = null,
                inputs = any(),
                outputs = any(),
                steps = any()
            )
        }
    }

    private fun baseContext(opportunityId: UUID) = CostCalculationContext(
        opportunityId = opportunityId,
        originCountry = "CN",
        destinationCountry = "US",
        productHsCode = "8471.30",
        weight = Weight(BigDecimal("100"), WeightUnit.LBS),
        declaredCargoValue = Money(BigDecimal("5000.00")),
        carrier = "UPS",
        transportMode = TransportMode.TRUCK,
        scenarioContextId = null,
        scenarioOverrides = emptyMap()
    )
}
