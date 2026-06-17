package com.overdrive.common.money

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency

/**
 * Immutable monetary value. Always stored at 2-decimal-place precision with HALF_UP rounding.
 * Never uses Double — all arithmetic is exact BigDecimal arithmetic.
 */
@ConsistentCopyVisibility
data class Money private constructor(
    val amount: BigDecimal,
    val currency: Currency,
) : Comparable<Money> {
    companion object {
        private val USD = Currency.getInstance("USD")

        fun of(amount: BigDecimal, currency: Currency = USD): Money =
            Money(amount.setScale(2, RoundingMode.HALF_UP), currency)

        fun of(amount: String, currency: Currency = USD): Money =
            of(BigDecimal(amount), currency)

        fun ofCents(cents: Long, currency: Currency = USD): Money =
            of(BigDecimal(cents).movePointLeft(2), currency)

        val ZERO: Money get() = of(BigDecimal.ZERO)
    }

    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return Money(amount + other.amount, currency)
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return Money(amount - other.amount, currency)
    }

    operator fun times(factor: BigDecimal): Money =
        of(amount.multiply(factor), currency)

    operator fun times(factor: Int): Money =
        times(BigDecimal(factor))

    operator fun unaryMinus(): Money = Money(amount.negate(), currency)

    override fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return amount.compareTo(other.amount)
    }

    fun toCents(): Long = amount.movePointRight(2).toLong()

    fun isZero(): Boolean = amount.compareTo(BigDecimal.ZERO) == 0

    fun isNegative(): Boolean = amount < BigDecimal.ZERO

    override fun toString(): String = "${currency.currencyCode} ${amount.toPlainString()}"

    private fun requireSameCurrency(other: Money) {
        require(currency == other.currency) {
            "Currency mismatch: $currency vs ${other.currency}"
        }
    }
}
