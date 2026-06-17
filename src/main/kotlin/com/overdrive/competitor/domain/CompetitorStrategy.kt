package com.overdrive.competitor.domain

import com.overdrive.catalog.domain.CompetitorProfile
import com.overdrive.catalog.domain.DistributionModel
import com.overdrive.catalog.domain.Product
import com.overdrive.common.money.Money
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Strategy that derives an estimated selling price and delivered cost from a competitor's
 * catalog profile. Each concrete strategy captures the pricing logic for one distribution
 * model. All results carry the inputs used so callers can render an explain payload.
 *
 * Coverage guard: before calling either estimate method, check [isCovering].
 */
sealed class CompetitorStrategy {

    abstract val type: DistributionModel

    /**
     * Returns true when this competitor plausibly offers [product] in [region].
     * Callers must call this before estimating — uncovered products short-circuit to
     * [CoverageDecision.NotOffered].
     */
    abstract fun isCovering(product: Product, region: String, profile: CompetitorProfile): Boolean

    abstract fun estimateSellingPrice(product: Product, profile: CompetitorProfile): CompetitorEstimate

    /** Flat per-shipment cost the competitor charges to reach [destZipRegion]. */
    abstract fun estimateDeliveredCost(
        product: Product,
        destZipRegion: String,
        profile: CompetitorProfile,
    ): CompetitorEstimate

    // ── concrete strategies ──────────────────────────────────────────────────

    /**
     * Buys at cost and marks up by their estimated margin.
     * Price = product.cost / (1 − margin%).
     */
    object MarginReseller : CompetitorStrategy() {
        override val type = DistributionModel.MARGIN_RESELLER

        override fun isCovering(product: Product, region: String, profile: CompetitorProfile) =
            profile.regionalPresence.contains(region) && !product.hazardous

        override fun estimateSellingPrice(product: Product, profile: CompetitorProfile): CompetitorEstimate {
            val margin = profile.estimatedMarginPct.coerceIn(MARGIN_FLOOR, MARGIN_CAP)
            val divisor = BigDecimal.ONE.subtract(margin)
            val price = product.cost.amount.divide(divisor, MC)
            return CompetitorEstimate(
                price = product.cost.withAmount(price),
                assumptions = mapOf(
                    "strategy" to type.name,
                    "baseCost" to product.cost.amount,
                    "marginPct" to margin,
                ),
            )
        }

        override fun estimateDeliveredCost(
            product: Product,
            destZipRegion: String,
            profile: CompetitorProfile,
        ): CompetitorEstimate {
            val selling = estimateSellingPrice(product, profile)
            // Margin resellers typically pass carrier cost through; estimate by weight class.
            val freight = freightByWeight(product)
            return CompetitorEstimate(
                price = selling.price.add(freight),
                assumptions = selling.assumptions + mapOf(
                    "estimatedFreight" to freight.amount,
                    "freightBasis" to "weight_class",
                ),
            )
        }
    }

    /**
     * MSRP-anchored. Big-box chains negotiate volume deals and sell near MSRP.
     * Price = product.msrp × (1 − volumeDiscount%).
     * volumeDiscount is derived from their margin on a standard big-box curve.
     */
    object BigBoxRetail : CompetitorStrategy() {
        override val type = DistributionModel.BIG_BOX_RETAIL

        override fun isCovering(product: Product, region: String, profile: CompetitorProfile) =
            profile.regionalPresence.contains(region) && !product.hazardous && !product.temperatureSensitive

        override fun estimateSellingPrice(product: Product, profile: CompetitorProfile): CompetitorEstimate {
            val margin = profile.estimatedMarginPct.coerceIn(MARGIN_FLOOR, MARGIN_CAP)
            // Big-box volume discount is typically margin − a small uplift for shelf space.
            val discount = margin.subtract(BigDecimal("0.03")).coerceAtLeast(BigDecimal.ZERO)
            val price = product.msrp.amount.multiply(BigDecimal.ONE.subtract(discount), MC)
            return CompetitorEstimate(
                price = product.msrp.withAmount(price),
                assumptions = mapOf(
                    "strategy" to type.name,
                    "msrpAnchor" to product.msrp.amount,
                    "volumeDiscountPct" to discount,
                    "marginPct" to margin,
                ),
            )
        }

        override fun estimateDeliveredCost(
            product: Product,
            destZipRegion: String,
            profile: CompetitorProfile,
        ): CompetitorEstimate {
            val selling = estimateSellingPrice(product, profile)
            // Big-box chains offer free or subsidised shipping on qualifying orders.
            val freight = if (product.cost.amount >= FREE_SHIP_THRESHOLD) Money.ZERO_USD
            else freightByWeight(product)
            return CompetitorEstimate(
                price = selling.price.add(freight),
                assumptions = selling.assumptions + mapOf(
                    "freeShippingThreshold" to FREE_SHIP_THRESHOLD,
                    "estimatedFreight" to freight.amount,
                ),
            )
        }

        private val FREE_SHIP_THRESHOLD = BigDecimal("35.00")
    }

    /**
     * Cost-plus B2B pricing. Industrial distributors add a fixed markup over their
     * cost (which approximates [product.cost] scaled by their supply chain efficiency).
     */
    object IndustrialDistributor : CompetitorStrategy() {
        override val type = DistributionModel.INDUSTRIAL_DISTRIBUTOR

        override fun isCovering(product: Product, region: String, profile: CompetitorProfile) =
            profile.regionalPresence.contains(region)

        override fun estimateSellingPrice(product: Product, profile: CompetitorProfile): CompetitorEstimate {
            val margin = profile.estimatedMarginPct.coerceIn(MARGIN_FLOOR, MARGIN_CAP)
            // Industrial distributors add margin on top of cost (cost-plus).
            val price = product.cost.amount.multiply(BigDecimal.ONE.add(margin), MC)
            return CompetitorEstimate(
                price = product.cost.withAmount(price),
                assumptions = mapOf(
                    "strategy" to type.name,
                    "baseCost" to product.cost.amount,
                    "markupPct" to margin,
                ),
            )
        }

        override fun estimateDeliveredCost(
            product: Product,
            destZipRegion: String,
            profile: CompetitorProfile,
        ): CompetitorEstimate {
            val selling = estimateSellingPrice(product, profile)
            // Industrial distributors freight-forward; cost is weight-based + fuel surcharge.
            val freight = freightByWeight(product).multiply(FUEL_SURCHARGE_MULTIPLIER)
            return CompetitorEstimate(
                price = selling.price.add(freight),
                assumptions = selling.assumptions + mapOf(
                    "estimatedFreight" to freight.amount,
                    "fuelSurchargeMultiplier" to FUEL_SURCHARGE_MULTIPLIER,
                ),
            )
        }

        private val FUEL_SURCHARGE_MULTIPLIER = BigDecimal("1.12")
    }

    /**
     * Membership-model. Warehouse clubs sell at near-cost to drive membership.
     * Price = product.cost × (1 + thin_margin), where thin_margin is well below [profile.estimatedMarginPct].
     */
    object WarehouseClub : CompetitorStrategy() {
        override val type = DistributionModel.WAREHOUSE_CLUB

        override fun isCovering(product: Product, region: String, profile: CompetitorProfile) =
            profile.regionalPresence.contains(region) &&
                profile.numberOfWarehouses >= MIN_WAREHOUSES_FOR_COVERAGE &&
                !product.hazardous &&
                !product.temperatureSensitive

        override fun estimateSellingPrice(product: Product, profile: CompetitorProfile): CompetitorEstimate {
            // Warehouse clubs compete on price; use a thin fixed margin regardless of profile.
            val thinMargin = WAREHOUSE_CLUB_TYPICAL_MARGIN
            val price = product.cost.amount.multiply(BigDecimal.ONE.add(thinMargin), MC)
            return CompetitorEstimate(
                price = product.cost.withAmount(price),
                assumptions = mapOf(
                    "strategy" to type.name,
                    "baseCost" to product.cost.amount,
                    "thinMarginPct" to thinMargin,
                    "profileMarginIgnored" to profile.estimatedMarginPct,
                ),
            )
        }

        override fun estimateDeliveredCost(
            product: Product,
            destZipRegion: String,
            profile: CompetitorProfile,
        ): CompetitorEstimate {
            val selling = estimateSellingPrice(product, profile)
            // Warehouse clubs ship LTL; cost is high relative to the product price.
            val freight = freightByWeight(product).multiply(LTL_MULTIPLIER)
            return CompetitorEstimate(
                price = selling.price.add(freight),
                assumptions = selling.assumptions + mapOf(
                    "estimatedFreight" to freight.amount,
                    "freightModel" to "LTL",
                ),
            )
        }

        private val WAREHOUSE_CLUB_TYPICAL_MARGIN = BigDecimal("0.14")
        private val LTL_MULTIPLIER = BigDecimal("1.30")
        private const val MIN_WAREHOUSES_FOR_COVERAGE = 3
    }

    // ── shared helpers ───────────────────────────────────────────────────────

    companion object {
        private val MC = MathContext(10, RoundingMode.HALF_UP)
        private val MARGIN_FLOOR = BigDecimal("0.01")
        private val MARGIN_CAP = BigDecimal("0.60")

        /** Flat freight estimate by product weight class (lbs). */
        fun freightByWeight(product: Product): Money {
            val weightLbs = product.weight.toOuncesDouble() / 16.0
            val amount = when {
                weightLbs <= 1.0 -> BigDecimal("5.99")
                weightLbs <= 5.0 -> BigDecimal("9.99")
                weightLbs <= 20.0 -> BigDecimal("14.99")
                weightLbs <= 70.0 -> BigDecimal("34.99")
                else -> BigDecimal("79.99")
            }
            return Money.usd(amount)
        }

        fun forModel(model: DistributionModel): CompetitorStrategy = when (model) {
            DistributionModel.MARGIN_RESELLER -> MarginReseller
            DistributionModel.BIG_BOX_RETAIL -> BigBoxRetail
            DistributionModel.INDUSTRIAL_DISTRIBUTOR -> IndustrialDistributor
            DistributionModel.WAREHOUSE_CLUB -> WarehouseClub
        }
    }
}

private fun BigDecimal.coerceIn(min: BigDecimal, max: BigDecimal) = this.max(min).min(max)
private fun BigDecimal.coerceAtLeast(min: BigDecimal) = this.max(min)
