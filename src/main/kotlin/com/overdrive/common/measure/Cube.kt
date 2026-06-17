package com.overdrive.common.measure

import java.math.BigDecimal

/** A shipment unit combining actual weight and package dimensions — used to derive billable weight. */
data class Cube(
    val weight: Weight,
    val dimensions: Dimensions,
) {
    /**
     * Billable weight is the greater of actual weight and dimensional weight.
     * Most parcel carriers bill at whichever is higher.
     */
    fun billableWeight(dimDivisor: BigDecimal = BigDecimal(139)): Weight {
        val dimWeight = dimensions.dimensionalWeight(dimDivisor)
        return if (dimWeight > weight) dimWeight else weight
    }
}
