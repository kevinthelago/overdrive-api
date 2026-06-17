package com.overdrive.cost.domain

import java.math.BigDecimal
import java.math.RoundingMode

data class Money(val amount: BigDecimal, val currency: String = "USD") {
    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "Cannot add different currencies: $currency vs ${other.currency}" }
        return copy(amount = amount + other.amount)
    }

    fun scaled(): Money = copy(amount = amount.setScale(4, RoundingMode.HALF_UP))
}
