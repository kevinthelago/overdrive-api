package com.overdrive.scenario.app

import com.overdrive.scenario.domain.OverrideType
import com.overdrive.scenario.domain.Scenario
import com.overdrive.scenario.domain.ScenarioOverrideEntity
// Interfaces from engine-core-api (com.overdrive.scenario.context) — will be available
// once SCEN-SEAM lands; import paths match the seam contract.
import com.overdrive.scenario.context.OverlayView
import com.overdrive.scenario.context.OverlayView.CarrierView
import com.overdrive.scenario.context.OverlayView.WarehouseView
import com.overdrive.scenario.context.OverlayView.SupplierView
import com.overdrive.scenario.context.OverlayView.ProductView
import com.overdrive.scenario.context.OverlayView.RateTables
import java.math.BigDecimal
import java.util.UUID

/**
 * Applies a Scenario's ordered overrides on top of a baseline OverlayView.
 * Overrides are processed in ascending position order (fold over baseline state).
 * Stale overrides (entity not found in baseline) are recorded but do not fail.
 */
class ScenarioOverlayView(
    private val scenario: Scenario,
    private val baseline: OverlayView,
) : OverlayView {

    /** Entity IDs that were referenced by overrides but not found in baseline. */
    val staleOverrideIds: List<UUID>
        get() = _staleOverrideIds

    private val _staleOverrideIds = mutableListOf<UUID>()
    private val resolvedWarehouses: List<WarehouseView>
    private val resolvedCarriers: List<CarrierView>
    private val resolvedSuppliers: List<SupplierView>
    private val resolvedProducts: List<ProductView>
    private val resolvedRates: RateTables

    init {
        val warehouses = baseline.getWarehouses().toMutableList()
        val carriers = baseline.getCarriers().toMutableList()
        val suppliers = baseline.getSuppliers().toMutableList()
        val products = baseline.getProducts().toMutableList()
        val rates = baseline.getRates().toMutable()

        // Overrides are already sorted by position (Scenario.overrides uses @OrderBy)
        for (override in scenario.overrides) {
            apply(override, warehouses, carriers, suppliers, products, rates)
        }

        resolvedWarehouses = warehouses
        resolvedCarriers = carriers
        resolvedSuppliers = suppliers
        resolvedProducts = products
        resolvedRates = rates.toImmutable()
    }

    override fun getWarehouses(): List<WarehouseView> = resolvedWarehouses
    override fun getCarriers(): List<CarrierView> = resolvedCarriers
    override fun getSuppliers(): List<SupplierView> = resolvedSuppliers
    override fun getProducts(): List<ProductView> = resolvedProducts
    override fun getRates(): RateTables = resolvedRates

    private fun apply(
        override: ScenarioOverrideEntity,
        warehouses: MutableList<WarehouseView>,
        carriers: MutableList<CarrierView>,
        suppliers: MutableList<SupplierView>,
        products: MutableList<ProductView>,
        rates: MutableRateTables,
    ) {
        val entityId = override.entityId
        when (override.overrideType) {
            OverrideType.ADD_WAREHOUSE -> {
                if (entityId == null) return
                val wh = baseline.getWarehouses().find { it.id == entityId }
                if (wh == null) { _staleOverrideIds += entityId; return }
                if (warehouses.none { it.id == entityId }) warehouses += wh
            }
            OverrideType.REMOVE_WAREHOUSE -> {
                if (entityId == null) return
                val removed = warehouses.removeIf { it.id == entityId }
                if (!removed) _staleOverrideIds += entityId
            }
            OverrideType.ADD_CARRIER -> {
                if (entityId == null) return
                val carrier = baseline.getCarriers().find { it.id == entityId }
                if (carrier == null) { _staleOverrideIds += entityId; return }
                if (carriers.none { it.id == entityId }) carriers += carrier
            }
            OverrideType.REMOVE_CARRIER -> {
                if (entityId == null) return
                val removed = carriers.removeIf { it.id == entityId }
                if (!removed) _staleOverrideIds += entityId
            }
            OverrideType.SUPPLIER_PRICE_DELTA -> {
                if (entityId == null) return
                val deltaPct = override.params["deltaPct"]?.toBigDecimalOrNull() ?: return
                val idx = suppliers.indexOfFirst { it.id == entityId }
                if (idx < 0) { _staleOverrideIds += entityId; return }
                suppliers[idx] = suppliers[idx].withPriceDelta(deltaPct)
            }
            OverrideType.FEE_SCHEDULE_CHANGE -> {
                if (entityId == null) return
                val newRate = override.params["newRate"]?.toBigDecimalOrNull() ?: return
                val entityType = override.params["entityType"] ?: return
                rates.applyFeeChange(entityType, entityId, newRate)
            }
            OverrideType.DEMAND_FACTOR_CHANGE -> {
                if (entityId == null) return
                val factorKey = override.params["factorKey"] ?: return
                val newValue = override.params["newValue"]?.toBigDecimalOrNull() ?: return
                val idx = products.indexOfFirst { it.id == entityId }
                if (idx < 0) { _staleOverrideIds += entityId; return }
                products[idx] = products[idx].withDemandFactor(factorKey, newValue)
            }
        }
    }
}

/** Mutable working state used only inside init; immediately converted to immutable RateTables. */
private class MutableRateTables(base: RateTables) {
    private val paymentRates: MutableMap<String, BigDecimal> = base.paymentRates().toMutableMap()
    private val returnRates: MutableMap<String, BigDecimal> = base.returnRates().toMutableMap()

    fun applyFeeChange(entityType: String, entityId: UUID, newRate: BigDecimal) {
        when (entityType.uppercase()) {
            "PAYMENT_RATE" -> paymentRates[entityId.toString()] = newRate
            "RETURN_RATE"  -> returnRates[entityId.toString()] = newRate
        }
    }

    fun toImmutable(): RateTables = RateTables.of(paymentRates, returnRates)
}

private fun RateTables.toMutable() = MutableRateTables(this)
