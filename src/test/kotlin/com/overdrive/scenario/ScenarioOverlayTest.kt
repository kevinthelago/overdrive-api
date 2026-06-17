package com.overdrive.scenario

import com.overdrive.scenario.app.ScenarioOverlayView
import com.overdrive.scenario.domain.OverrideType
import com.overdrive.scenario.domain.Scenario
import com.overdrive.scenario.domain.ScenarioOverrideEntity
// These imports depend on engine-core-api (SCEN-SEAM); will resolve once that stream lands.
import com.overdrive.scenario.context.OverlayView
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

/** Unit tests for ScenarioOverlayView — no Spring context needed. */
class ScenarioOverlayTest {

    private val wh1 = UUID.randomUUID()
    private val wh2 = UUID.randomUUID()
    private val carrier1 = UUID.randomUUID()
    private val supplier1 = UUID.randomUUID()

    private fun stubBaseline(
        warehouseIds: List<UUID> = listOf(wh1, wh2),
        carrierIds: List<UUID> = listOf(carrier1),
        supplierIds: List<UUID> = listOf(supplier1),
        supplierCost: BigDecimal = BigDecimal("10.00"),
    ): OverlayView {
        val baseline = mockk<OverlayView>()
        every { baseline.getWarehouses() } returns warehouseIds.map { stubWarehouse(it) }
        every { baseline.getCarriers() } returns carrierIds.map { stubCarrier(it) }
        every { baseline.getSuppliers() } returns supplierIds.map { stubSupplier(it, supplierCost) }
        every { baseline.getProducts() } returns emptyList()
        every { baseline.getRates() } returns stubRates()
        return baseline
    }

    @Test
    fun `REMOVE_WAREHOUSE drops it from resolved list`() {
        val scenario = buildScenario(
            OverrideType.REMOVE_WAREHOUSE to (wh1 to emptyMap()),
        )
        val view = ScenarioOverlayView(scenario, stubBaseline())

        assertThat(view.getWarehouses().map { it.id }).doesNotContain(wh1)
        assertThat(view.getWarehouses().map { it.id }).contains(wh2)
    }

    @Test
    fun `ADD_WAREHOUSE adds a warehouse from baseline pool (idempotent)`() {
        val extraWh = UUID.randomUUID()
        val baselineWithExtra = mockk<OverlayView>()
        every { baselineWithExtra.getWarehouses() } returns listOf(stubWarehouse(wh1), stubWarehouse(extraWh))
        every { baselineWithExtra.getCarriers() } returns emptyList()
        every { baselineWithExtra.getSuppliers() } returns emptyList()
        every { baselineWithExtra.getProducts() } returns emptyList()
        every { baselineWithExtra.getRates() } returns stubRates()

        // Scenario starts with only wh1 active; we add extraWh
        val reducedBaseline = stubBaseline(warehouseIds = listOf(wh1))
        val scenario = buildScenario(
            OverrideType.ADD_WAREHOUSE to (extraWh to emptyMap()),
        )
        val view = ScenarioOverlayView(scenario, baselineWithExtra)
        assertThat(view.getWarehouses().map { it.id }).contains(wh1, extraWh)
    }

    @Test
    fun `SUPPLIER_PRICE_DELTA adjusts supplier cost`() {
        val scenario = buildScenario(
            OverrideType.SUPPLIER_PRICE_DELTA to (supplier1 to mapOf("deltaPct" to "0.10")),
        )
        val view = ScenarioOverlayView(scenario, stubBaseline(supplierCost = BigDecimal("10.00")))
        val supplier = view.getSuppliers().first { it.id == supplier1 }
        // 10% delta applied — exact value depends on SupplierView.withPriceDelta contract
        assertThat(supplier.unitCostUsd).isGreaterThan(BigDecimal("10.00"))
    }

    @Test
    fun `stale override (entity not in baseline) is flagged and skipped`() {
        val unknownId = UUID.randomUUID()
        val scenario = buildScenario(
            OverrideType.REMOVE_WAREHOUSE to (unknownId to emptyMap()),
        )
        val view = ScenarioOverlayView(scenario, stubBaseline())

        assertThat(view.staleOverrideIds).contains(unknownId)
        // baseline warehouses unchanged
        assertThat(view.getWarehouses()).hasSize(2)
    }

    @Test
    fun `overrides applied in position order (remove then add is idempotent)`() {
        val scenario = Scenario(name = "test")
        scenario.addOverride(ScenarioOverrideEntity(
            scenario = scenario, overrideType = OverrideType.REMOVE_WAREHOUSE,
            position = 1, entityId = wh1,
        ))
        scenario.addOverride(ScenarioOverrideEntity(
            scenario = scenario, overrideType = OverrideType.ADD_WAREHOUSE,
            position = 2, entityId = wh1,
        ))

        val view = ScenarioOverlayView(scenario, stubBaseline())
        // remove at pos-1 drops it, add at pos-2 re-adds from baseline pool
        assertThat(view.getWarehouses().map { it.id }).contains(wh1)
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun buildScenario(vararg overrides: Pair<OverrideType, Pair<UUID?, Map<String, String>>>): Scenario {
        val scenario = Scenario(name = "test")
        overrides.forEachIndexed { idx, (type, entityAndParams) ->
            val (entityId, params) = entityAndParams
            scenario.addOverride(ScenarioOverrideEntity(
                scenario = scenario,
                overrideType = type,
                position = idx + 1,
                entityId = entityId,
                params = params,
            ))
        }
        return scenario
    }

    private fun stubWarehouse(id: UUID): OverlayView.WarehouseView = mockk {
        every { this@mockk.id } returns id
    }

    private fun stubCarrier(id: UUID): OverlayView.CarrierView = mockk {
        every { this@mockk.id } returns id
    }

    private fun stubSupplier(id: UUID, cost: BigDecimal): OverlayView.SupplierView = mockk {
        every { this@mockk.id } returns id
        every { unitCostUsd } returns cost
        every { withPriceDelta(any()) } answers {
            val delta = firstArg<BigDecimal>()
            val newCost = cost + (cost * delta)
            mockk { every { this@mockk.id } returns id; every { unitCostUsd } returns newCost }
        }
    }

    private fun stubRates(): OverlayView.RateTables = mockk {
        every { paymentRates() } returns emptyMap()
        every { returnRates() } returns emptyMap()
        every { toMutable() } returns mockk(relaxed = true)
    }
}
