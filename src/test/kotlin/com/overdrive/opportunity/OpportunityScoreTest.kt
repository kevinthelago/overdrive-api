package com.overdrive.opportunity

import com.overdrive.opportunity.domain.FactorNormalizer
import com.overdrive.opportunity.domain.FactorType
import com.overdrive.opportunity.domain.OpportunityFactor
import com.overdrive.opportunity.domain.OpportunityScore
import com.overdrive.opportunity.domain.ZeroReason
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.bigdecimal.shouldBeGreaterThan
import io.kotest.matchers.bigdecimal.shouldBeLessThan
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.math.BigDecimal

class OpportunityScoreTest : FreeSpec({

    fun factor(type: FactorType, raw: BigDecimal, normalized: BigDecimal, zero: ZeroReason? = null) =
        OpportunityFactor(type, raw, normalized, zero)

    "OpportunityScore.compute" - {
        "returns positive score for all healthy factors" {
            val score = OpportunityScore.compute(
                productId = 1L,
                categoryId = 10L,
                savingsPct = factor(FactorType.SAVINGS_PCT, BigDecimal("0.20"), BigDecimal("0.20")),
                marketSize = factor(FactorType.MARKET_SIZE, BigDecimal("1000000"), BigDecimal("0.60")),
                orderFrequency = factor(FactorType.ORDER_FREQUENCY, BigDecimal("12"), BigDecimal("0.033")),
                categoryGrowth = factor(FactorType.CATEGORY_GROWTH, BigDecimal("0.08"), BigDecimal("0.52")),
                supplierAvailability = factor(FactorType.SUPPLIER_AVAILABILITY, BigDecimal("0.85"), BigDecimal("0.85")),
                logisticsComplexity = factor(FactorType.LOGISTICS_COMPLEXITY, BigDecimal("2.0"), BigDecimal("0.75")),
            )
            score.score shouldBeGreaterThan BigDecimal.ZERO
            score.zeroReason shouldBe null
        }

        "returns zero score when supplierAvailability is zero" {
            val score = OpportunityScore.compute(
                productId = 1L,
                categoryId = 10L,
                savingsPct = factor(FactorType.SAVINGS_PCT, BigDecimal("0.20"), BigDecimal("0.20")),
                marketSize = factor(FactorType.MARKET_SIZE, BigDecimal("1000000"), BigDecimal("0.60")),
                orderFrequency = factor(FactorType.ORDER_FREQUENCY, BigDecimal("12"), BigDecimal("0.033")),
                categoryGrowth = factor(FactorType.CATEGORY_GROWTH, BigDecimal("0.08"), BigDecimal("0.52")),
                supplierAvailability = factor(FactorType.SUPPLIER_AVAILABILITY, BigDecimal.ZERO, BigDecimal.ZERO),
                logisticsComplexity = factor(FactorType.LOGISTICS_COMPLEXITY, BigDecimal("2.0"), BigDecimal("0.75")),
            )
            score.score shouldBeEqualComparingTo BigDecimal.ZERO
            score.zeroReason shouldBe ZeroReason.NO_SUPPLIER_AVAILABILITY
        }

        "returns zero score when no-route" {
            val score = OpportunityScore.compute(
                productId = 1L,
                categoryId = 10L,
                savingsPct = factor(FactorType.SAVINGS_PCT, BigDecimal.ZERO, BigDecimal.ZERO, ZeroReason.NO_ROUTE),
                marketSize = factor(FactorType.MARKET_SIZE, BigDecimal("1000000"), BigDecimal("0.60")),
                orderFrequency = factor(FactorType.ORDER_FREQUENCY, BigDecimal("12"), BigDecimal("0.033")),
                categoryGrowth = factor(FactorType.CATEGORY_GROWTH, BigDecimal("0.08"), BigDecimal("0.52")),
                supplierAvailability = factor(FactorType.SUPPLIER_AVAILABILITY, BigDecimal("0.85"), BigDecimal("0.85")),
                logisticsComplexity = factor(FactorType.LOGISTICS_COMPLEXITY, BigDecimal("2.0"), BigDecimal("0.75")),
            )
            score.score shouldBeEqualComparingTo BigDecimal.ZERO
            score.zeroReason shouldBe ZeroReason.NO_ROUTE
        }

        "higher logistics complexity lowers score" {
            fun scoreFor(complexity: BigDecimal): BigDecimal {
                val normalized = FactorNormalizer.normalizeLogisticsComplexity(complexity)
                return OpportunityScore.compute(
                    productId = 1L, categoryId = 10L,
                    savingsPct = factor(FactorType.SAVINGS_PCT, BigDecimal("0.20"), BigDecimal("0.20")),
                    marketSize = factor(FactorType.MARKET_SIZE, BigDecimal("1000000"), BigDecimal("0.60")),
                    orderFrequency = factor(FactorType.ORDER_FREQUENCY, BigDecimal("12"), BigDecimal("0.033")),
                    categoryGrowth = factor(FactorType.CATEGORY_GROWTH, BigDecimal("0.08"), BigDecimal("0.52")),
                    supplierAvailability = factor(FactorType.SUPPLIER_AVAILABILITY, BigDecimal("0.85"), BigDecimal("0.85")),
                    logisticsComplexity = factor(FactorType.LOGISTICS_COMPLEXITY, complexity, normalized),
                ).score
            }
            scoreFor(BigDecimal("1.0")) shouldBeGreaterThan scoreFor(BigDecimal("5.0"))
        }
    }

    "FactorNormalizer" - {
        "normalizeSavingsPct clamps to [0,1]" {
            FactorNormalizer.normalizeSavingsPct(BigDecimal("-0.5")).compareTo(BigDecimal.ZERO) shouldBe 0
            FactorNormalizer.normalizeSavingsPct(BigDecimal("1.5")).compareTo(BigDecimal.ONE) shouldBe 0
            FactorNormalizer.normalizeSavingsPct(BigDecimal("0.35")) shouldBeGreaterThan BigDecimal.ZERO
            FactorNormalizer.normalizeSavingsPct(BigDecimal("0.35")) shouldBeLessThan BigDecimal.ONE
        }

        "normalizeMarketSize: $1k → near 0, $1B → near 1" {
            val small = FactorNormalizer.normalizeMarketSize(BigDecimal("1000"))
            val large = FactorNormalizer.normalizeMarketSize(BigDecimal("1000000000"))
            small shouldBeLessThan BigDecimal("0.1")
            large shouldBeGreaterThan BigDecimal("0.9")
        }

        "normalizeOrderFrequency: 365 → 1, 1 → near 0" {
            FactorNormalizer.normalizeOrderFrequency(BigDecimal("365")).compareTo(BigDecimal.ONE) shouldBe 0
            FactorNormalizer.normalizeOrderFrequency(BigDecimal("1")) shouldBeLessThan BigDecimal("0.01")
        }

        "normalizeCategoryGrowth: 0% → 0.5 (neutral)" {
            val neutral = FactorNormalizer.normalizeCategoryGrowth(BigDecimal.ZERO)
            neutral.compareTo(BigDecimal("0.5")) shouldBe 0
        }

        "normalizeLogisticsComplexity: 1 (easy) → near 1, 5 (hard) → near 0" {
            val easy = FactorNormalizer.normalizeLogisticsComplexity(BigDecimal("1.0"))
            val hard = FactorNormalizer.normalizeLogisticsComplexity(BigDecimal("5.0"))
            easy.compareTo(BigDecimal.ONE) shouldBe 0
            hard.compareTo(BigDecimal.ZERO) shouldBe 0
        }

        "normalizeSupplierAvailability clamps to [0,1]" {
            FactorNormalizer.normalizeSupplierAvailability(BigDecimal("1.5")).compareTo(BigDecimal.ONE) shouldBe 0
            FactorNormalizer.normalizeSupplierAvailability(BigDecimal("-0.1")).compareTo(BigDecimal.ZERO) shouldBe 0
        }
    }
})
