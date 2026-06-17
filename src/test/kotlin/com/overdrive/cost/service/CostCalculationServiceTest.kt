package com.overdrive.cost.service

import com.overdrive.common.measure.Weight
import com.overdrive.common.money.Money
import com.overdrive.cost.domain.CostCalculationContext
import com.overdrive.cost.domain.TransportMode
import com.overdrive.explainability.domain.EngineTrace
import com.overdrive.explainability.domain.EngineType
import com.overdrive.explainability.service.ExplainabilityService
import com.overdrive.scenario.context.domain.ScenarioAssumptions
import com.overdrive.scenario.context.domain.ScenarioContext
import com.overdrive.scenario.context.service.ScenarioContextResolver
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class CostCalculationServiceTest {

    private val explainabilityService: ExplainabilityService = mockk()
    private val scenarioContextResolver: ScenarioContextResolver = mockk()
    private val service = CostCalculationService(explainabilityService, scenarioContextResolver)

    private val mockTrace = EngineTrace().apply { id = UUID.randomUUID() }

    @BeforeEach
    fun setup() {
        every {
            explainabilityService.record(any(), any(), any(), any(), any(), any())
        } returns mockTrace
    }

    @Test
    fun `calculate returns correct total for standard truck shipment`() {
        val context = CostCalculationContext(
            opportunityId = UUID.randomUUID(),
            originCountry = "US",
            destinationCountry = "US",
            productHsCode = "1234.56",
            weight = Weight.ofPounds(100.0),
            declaredCargoValue = Money.of("500.00"),
            carrier = "UPS",
            transportMode = TransportMode.TRUCK,
        )

        val result = service.calculate(context)

        // freight = 100 × 1.80 (default TRUCK) = 180.00
        // duty   = 500 × 0.035 (US) = 17.50
        // handling = 500 × 0.02 = 10.00
        // total  = 207.50
        result.freightCost.amount shouldBe Money.of("180.00")
        result.dutyCost.amount shouldBe Money.of("17.50")
        result.fees.first().amount shouldBe Money.of("10.00")
        result.totalDeliveredCost shouldBe Money.of("207.50")
    }

    @Test
    fun `calculate applies freight rate override from scenarioOverrides`() {
        val context = CostCalculationContext(
            opportunityId = UUID.randomUUID(),
            originCountry = "US",
            destinationCountry = "US",
            productHsCode = "1234.56",
            weight = Weight.ofPounds(100.0),
            declaredCargoValue = Money.of("500.00"),
            carrier = "UPS",
            transportMode = TransportMode.TRUCK,
            scenarioOverrides = mapOf("freight.rate.UPS" to BigDecimal("2.00")),
        )

        val result = service.calculate(context)

        // freight = 100 × 2.00 = 200.00
        result.freightCost.amount shouldBe Money.of("200.00")
    }

    @Test
    fun `calculate applies duty rate override from scenarioOverrides`() {
        val context = CostCalculationContext(
            opportunityId = UUID.randomUUID(),
            originCountry = "US",
            destinationCountry = "US",
            productHsCode = "1234.56",
            weight = Weight.ofPounds(100.0),
            declaredCargoValue = Money.of("500.00"),
            carrier = "UPS",
            transportMode = TransportMode.TRUCK,
            scenarioOverrides = mapOf("duty.rate.1234.56" to BigDecimal("0.10")),
        )

        val result = service.calculate(context)

        // duty = 500 × 0.10 = 50.00
        result.dutyCost.amount shouldBe Money.of("50.00")
    }

    @Test
    fun `calculate records a trace via explainability service`() {
        val opportunityId = UUID.randomUUID()
        val context = CostCalculationContext(
            opportunityId = opportunityId,
            originCountry = "US",
            destinationCountry = "US",
            productHsCode = "1234.56",
            weight = Weight.ofPounds(50.0),
            declaredCargoValue = Money.of("200.00"),
            carrier = "FedEx Ground",
            transportMode = TransportMode.TRUCK,
        )

        service.calculate(context)

        verify(exactly = 1) {
            explainabilityService.record(
                engineType = EngineType.COST,
                opportunityId = opportunityId,
                scenarioContextId = null,
                inputs = any(),
                outputs = any(),
                steps = any(),
            )
        }
    }

    @Test
    fun `calculate applies overrides from resolved scenario context`() {
        val scenarioId = UUID.randomUUID()
        val scenarioContext = ScenarioContext(
            scenarioId = scenarioId,
            name = "High freight scenario",
            assumptions = ScenarioAssumptions(
                freightRateOverrides = mapOf("freight.rate.UPS" to BigDecimal("3.00")),
            ),
        )
        every { scenarioContextResolver.resolve(scenarioId) } returns scenarioContext

        val context = CostCalculationContext(
            opportunityId = UUID.randomUUID(),
            originCountry = "US",
            destinationCountry = "US",
            productHsCode = "1234.56",
            weight = Weight.ofPounds(100.0),
            declaredCargoValue = Money.of("500.00"),
            carrier = "UPS",
            transportMode = TransportMode.TRUCK,
            scenarioContextId = scenarioId,
        )

        val result = service.calculate(context)

        // freight = 100 × 3.00 = 300.00
        result.freightCost.amount shouldBe Money.of("300.00")
    }
}
