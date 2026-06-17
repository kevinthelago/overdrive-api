package com.overdrive.common.measure

import java.math.BigDecimal
import java.math.RoundingMode

/** Immutable package dimensions (length × width × height) in inches. All values must be positive. */
data class Dimensions(
    val lengthIn: BigDecimal,
    val widthIn: BigDecimal,
    val heightIn: BigDecimal,
) {
    init {
        require(lengthIn > BigDecimal.ZERO) { "Length must be positive, got $lengthIn" }
        require(widthIn > BigDecimal.ZERO) { "Width must be positive, got $widthIn" }
        require(heightIn > BigDecimal.ZERO) { "Height must be positive, got $heightIn" }
    }

    companion object {
        fun of(lengthIn: Double, widthIn: Double, heightIn: Double) = Dimensions(
            BigDecimal(lengthIn.toString()),
            BigDecimal(widthIn.toString()),
            BigDecimal(heightIn.toString()),
        )
    }

    fun cubicInches(): BigDecimal = lengthIn * widthIn * heightIn

    /**
     * Dimensional weight using the carrier's DIM divisor (default 139 in³/lb for domestic ground).
     * Result is rounded up to the nearest pound per typical carrier practice.
     */
    fun dimensionalWeight(divisor: BigDecimal = BigDecimal(139)): Weight {
        val dimLbs = cubicInches().divide(divisor, 4, RoundingMode.HALF_UP)
        return Weight.ofPounds(dimLbs)
    }
}
