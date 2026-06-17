package com.overdrive.scenario.context

import java.math.BigDecimal
import java.util.UUID

/**
 * Read-only view of the logistics network used by engines.
 * ScenarioOverlayView applies non-destructive overrides on top of a baseline implementation.
 */
interface OverlayView {

    fun getWarehouses(): List<WarehouseView>
    fun getCarriers(): List<CarrierView>
    fun getSuppliers(): List<SupplierView>
    fun getProducts(): List<ProductView>
    fun getRates(): RateTables

    interface WarehouseView {
        val id: UUID
        val name: String
    }

    interface CarrierView {
        val id: UUID
        val name: String
    }

    interface SupplierView {
        val id: UUID
        val name: String
        val unitCostUsd: BigDecimal
        fun withPriceDelta(deltaPct: BigDecimal): SupplierView
    }

    interface ProductView {
        val id: UUID
        fun withDemandFactor(factorKey: String, newValue: BigDecimal): ProductView
    }

    interface RateTables {
        fun paymentRates(): Map<String, BigDecimal>
        fun returnRates(): Map<String, BigDecimal>

        companion object {
            fun of(paymentRates: Map<String, BigDecimal>, returnRates: Map<String, BigDecimal>): RateTables =
                SimpleRateTables(paymentRates, returnRates)
        }
    }
}

private data class SimpleRateTables(
    private val payment: Map<String, BigDecimal>,
    private val returns: Map<String, BigDecimal>,
) : OverlayView.RateTables {
    override fun paymentRates() = payment
    override fun returnRates() = returns
}
