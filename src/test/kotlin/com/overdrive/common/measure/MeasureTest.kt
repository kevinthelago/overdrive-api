package com.overdrive.common.measure

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import java.math.RoundingMode

class MeasureTest : StringSpec({

    // Weight tests
    "Weight.ofPounds accepts zero" {
        Weight.ofPounds(BigDecimal.ZERO).pounds shouldBe BigDecimal("0.0000")
    }

    "Weight.ofPounds rejects negative values" {
        shouldThrow<IllegalArgumentException> { Weight.ofPounds(BigDecimal("-0.0001")) }
    }

    "Weight.ofOunces converts correctly" {
        Weight.ofOunces(16.0).pounds shouldBe BigDecimal("1.0000")
        Weight.ofOunces(8.0).pounds shouldBe BigDecimal("0.5000")
    }

    "Weight addition" {
        val w = Weight.ofPounds(1.5) + Weight.ofPounds(2.5)
        w.pounds shouldBe BigDecimal("4.0000")
    }

    "Weight comparison" {
        Weight.ofPounds(2.0) shouldBeGreaterThan Weight.ofPounds(1.0)
        Weight.ofPounds(0.5) shouldBeLessThan Weight.ofPounds(1.0)
    }

    // Dimensions tests
    "Dimensions rejects zero or negative values" {
        shouldThrow<IllegalArgumentException> { Dimensions.of(0.0, 1.0, 1.0) }
        shouldThrow<IllegalArgumentException> { Dimensions.of(1.0, -1.0, 1.0) }
    }

    "dimensional weight is calculated correctly" {
        val dims = Dimensions.of(12.0, 8.0, 6.0) // 576 in³
        val expected = BigDecimal(576).divide(BigDecimal(139), 4, RoundingMode.HALF_UP)
        dims.dimensionalWeight().pounds shouldBe expected
    }

    // Cube / billable weight tests
    "billable weight uses dimensional weight when it is higher" {
        val dims = Dimensions.of(24.0, 18.0, 12.0) // 5184 in³ → ~37 lbs dim weight
        val cube = Cube(Weight.ofPounds(1.0), dims)
        cube.billableWeight() shouldBeGreaterThan Weight.ofPounds(1.0)
    }

    "billable weight uses actual weight when it is higher" {
        val dims = Dimensions.of(1.0, 1.0, 1.0) // 1 in³ → ~0.007 lbs dim weight
        val cube = Cube(Weight.ofPounds(100.0), dims)
        cube.billableWeight() shouldBe Weight.ofPounds(100.0)
    }

    "billable weight equals actual weight when both are equal" {
        // 139 in³ → 1 lb dim weight exactly
        val dims = Dimensions.of(13.9, 10.0, 1.0) // 139 in³
        val cube = Cube(Weight.ofPounds(1.0), dims)
        cube.billableWeight() shouldBe Weight.ofPounds(1.0)
    }
})
