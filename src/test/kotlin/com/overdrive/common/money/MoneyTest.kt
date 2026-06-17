package com.overdrive.common.money

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import java.util.Currency

class MoneyTest : StringSpec({

    "of() rounds half-up to 2 decimal places" {
        Money.of(BigDecimal("1.555")).amount shouldBe BigDecimal("1.56")
        Money.of(BigDecimal("1.554")).amount shouldBe BigDecimal("1.55")
        Money.of(BigDecimal("0.5")).amount shouldBe BigDecimal("0.50")
    }

    "addition returns correct amount" {
        val result = Money.of(BigDecimal("10.25")) + Money.of(BigDecimal("5.75"))
        result.amount shouldBe BigDecimal("16.00")
    }

    "subtraction returns correct amount" {
        val result = Money.of(BigDecimal("10.00")) - Money.of(BigDecimal("3.75"))
        result.amount shouldBe BigDecimal("6.25")
    }

    "multiplication by scalar rounds correctly" {
        val result = Money.of(BigDecimal("3.33")) * 3
        result.amount shouldBe BigDecimal("9.99")
    }

    "toCents is lossless for two-decimal values" {
        Money.of(BigDecimal("12.34")).toCents() shouldBe 1234L
        Money.of(BigDecimal("0.01")).toCents() shouldBe 1L
    }

    "ofCents round-trips correctly" {
        Money.ofCents(9999L).toCents() shouldBe 9999L
        Money.ofCents(9999L).amount shouldBe BigDecimal("99.99")
    }

    "ZERO is exactly zero" {
        Money.ZERO.isZero() shouldBe true
        Money.ZERO.toCents() shouldBe 0L
    }

    "negative amounts are supported" {
        (-Money.of(BigDecimal("5.00"))).isNegative() shouldBe true
        Money.of(BigDecimal("5.00")).isNegative() shouldBe false
    }

    "adding different currencies throws" {
        shouldThrow<IllegalArgumentException> {
            Money.of(BigDecimal("1.00"), Currency.getInstance("USD")) +
                Money.of(BigDecimal("1.00"), Currency.getInstance("EUR"))
        }
    }

    "comparing different currencies throws" {
        shouldThrow<IllegalArgumentException> {
            Money.of(BigDecimal("1.00"), Currency.getInstance("USD"))
                .compareTo(Money.of(BigDecimal("0.50"), Currency.getInstance("EUR")))
        }
    }

    "money is ordered by amount" {
        Money.of(BigDecimal("2.00")) shouldBeGreaterThan Money.of(BigDecimal("1.00"))
    }
})
