package com.overdrive.competitor

import com.overdrive.catalog.domain.Category
import com.overdrive.catalog.domain.Dimensions
import com.overdrive.catalog.domain.Weight
import com.overdrive.catalog.domain.WeightUnit

fun testCategory(id: Long = 1L, name: String = "Hardware") = Category(id = id, name = name)

fun testWeight(lbs: Double) = Weight(value = (lbs * 16).toBigDecimal(), unit = WeightUnit.OUNCE)

fun testDimensions() = Dimensions(
    lengthIn = java.math.BigDecimal("10.0"),
    widthIn = java.math.BigDecimal("8.0"),
    heightIn = java.math.BigDecimal("4.0"),
)
