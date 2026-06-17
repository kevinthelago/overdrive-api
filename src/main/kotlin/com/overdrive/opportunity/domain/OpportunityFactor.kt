package com.overdrive.opportunity.domain

import java.math.BigDecimal

/**
 * One of six contributing factors in the opportunity score formula.
 * [rawValue] is the original input; [normalizedValue] is on a documented 0–1 scale.
 * Both are carried in the response for the explain affordance.
 *
 * Normalization scales:
 *   SAVINGS_PCT         raw = savings %     → clamp to [0, 1] (already a ratio)
 *   MARKET_SIZE         raw = USD           → log10 scale, normalized to [0, 1] over $1k–$1B range
 *   ORDER_FREQUENCY     raw = orders/year   → normalized to [0, 1] over 1–365 range
 *   CATEGORY_GROWTH     raw = growth %/yr   → clamp to [0, 2] then / 2
 *   SUPPLIER_AVAILABILITY raw = 0.0–1.0     → already normalized; 0 triggers zero-score guard
 *   LOGISTICS_COMPLEXITY raw = 1.0–5.0 scale → inverted to [0, 1]: (5 − raw) / 4
 */
data class OpportunityFactor(
    val type: FactorType,
    val rawValue: BigDecimal,
    val normalizedValue: BigDecimal,
    val zeroReason: ZeroReason? = null,
)

enum class FactorType {
    SAVINGS_PCT,
    MARKET_SIZE,
    ORDER_FREQUENCY,
    CATEGORY_GROWTH,
    SUPPLIER_AVAILABILITY,
    LOGISTICS_COMPLEXITY,
}

enum class ZeroReason {
    NO_ROUTE,
    NOT_OFFERED_BY_ANY_COMPETITOR,
    NO_SUPPLIER_AVAILABILITY,
    LOGISTICS_COMPLEXITY_AT_FLOOR,
}

object FactorNormalizer {
    private val ZERO = BigDecimal.ZERO
    private val ONE = BigDecimal.ONE

    private val MARKET_SIZE_LOG_MIN = Math.log10(1_000.0)   // $1 k
    private val MARKET_SIZE_LOG_MAX = Math.log10(1_000_000_000.0) // $1 B
    private val LOG_RANGE = MARKET_SIZE_LOG_MAX - MARKET_SIZE_LOG_MIN

    private val ORDER_FREQ_MAX = BigDecimal("365")
    private val CATEGORY_GROWTH_CAP = BigDecimal("2.0")
    private val LOGISTICS_SCALE_MAX = BigDecimal("5.0")
    private val LOGISTICS_SCALE_MIN = BigDecimal("1.0")

    fun normalizeSavingsPct(raw: BigDecimal): BigDecimal =
        raw.coerceIn(ZERO, ONE)

    fun normalizeMarketSize(rawUsd: BigDecimal): BigDecimal {
        if (rawUsd <= ZERO) return ZERO
        val logVal = Math.log10(rawUsd.toDouble())
        val normalized = (logVal - MARKET_SIZE_LOG_MIN) / LOG_RANGE
        return BigDecimal(normalized.coerceIn(0.0, 1.0)).setScale(6, java.math.RoundingMode.HALF_UP)
    }

    fun normalizeOrderFrequency(rawOrdersPerYear: BigDecimal): BigDecimal {
        if (rawOrdersPerYear <= ZERO) return ZERO
        return rawOrdersPerYear.divide(ORDER_FREQ_MAX, 6, java.math.RoundingMode.HALF_UP)
            .coerceIn(ZERO, ONE)
    }

    fun normalizeCategoryGrowth(rawPctPerYear: BigDecimal): BigDecimal {
        // Growth of 0 → neutral (0.5); negative → below 0.5; cap at +200%.
        val clamped = rawPctPerYear.coerceIn(CATEGORY_GROWTH_CAP.negate(), CATEGORY_GROWTH_CAP)
        return clamped.add(CATEGORY_GROWTH_CAP).divide(CATEGORY_GROWTH_CAP.multiply(BigDecimal("2")), 6, java.math.RoundingMode.HALF_UP)
    }

    fun normalizeSupplierAvailability(raw: BigDecimal): BigDecimal =
        raw.coerceIn(ZERO, ONE)

    /** Inverts the complexity scale: high complexity → low opportunity. */
    fun normalizeLogisticsComplexity(raw: BigDecimal): BigDecimal {
        val clamped = raw.coerceIn(LOGISTICS_SCALE_MIN, LOGISTICS_SCALE_MAX)
        val range = LOGISTICS_SCALE_MAX.subtract(LOGISTICS_SCALE_MIN)
        return LOGISTICS_SCALE_MAX.subtract(clamped).divide(range, 6, java.math.RoundingMode.HALF_UP)
    }
}

private fun BigDecimal.coerceIn(min: BigDecimal, max: BigDecimal) = this.max(min).min(max)
