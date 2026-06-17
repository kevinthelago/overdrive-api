package com.overdrive.cost.domain

import java.math.BigDecimal

enum class WeightUnit { LBS, KG }

data class Weight(val value: BigDecimal, val unit: WeightUnit = WeightUnit.LBS) {
    fun toKg(): Weight = when (unit) {
        WeightUnit.KG -> this
        WeightUnit.LBS -> Weight(value.multiply(BigDecimal("0.453592")), WeightUnit.KG)
    }
}
