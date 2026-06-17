package com.overdrive.cost.domain

import java.math.BigDecimal

data class FreightCost(val carrier: String, val mode: TransportMode, val amount: Money)

data class DutyCost(val hsCode: String, val rate: BigDecimal, val amount: Money)

data class FeeComponent(val type: String, val description: String, val amount: Money)
