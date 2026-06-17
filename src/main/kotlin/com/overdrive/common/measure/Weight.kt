package com.overdrive.common.measure

import java.math.BigDecimal
import java.math.RoundingMode

/** Immutable weight value, stored in pounds with 4-decimal-place precision. */
data class Weight(val pounds: BigDecimal) : Comparable<Weight> {

    init {
        require(pounds >= BigDecimal.ZERO) { "Weight must be non-negative, got $pounds" }
    }

    companion object {
        val ZERO = Weight(BigDecimal.ZERO)

        fun ofPounds(lbs: BigDecimal) = Weight(lbs.setScale(4, RoundingMode.HALF_UP))
        fun ofPounds(lbs: Double) = ofPounds(BigDecimal(lbs.toString()))
        fun ofOunces(oz: BigDecimal) = ofPounds(oz.divide(BigDecimal(16), 4, RoundingMode.HALF_UP))
        fun ofOunces(oz: Double) = ofOunces(BigDecimal(oz.toString()))
    }

    fun toOunces(): BigDecimal = pounds.multiply(BigDecimal(16))

    operator fun plus(other: Weight) = ofPounds(pounds + other.pounds)
    operator fun minus(other: Weight) = ofPounds((pounds - other.pounds).max(BigDecimal.ZERO))

    override fun compareTo(other: Weight): Int = pounds.compareTo(other.pounds)

    override fun toString(): String = "${pounds.stripTrailingZeros().toPlainString()} lbs"
}
