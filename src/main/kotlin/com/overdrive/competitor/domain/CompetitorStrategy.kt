package com.overdrive.competitor.domain

import com.overdrive.catalog.domain.competitor.Competitor
import com.overdrive.catalog.domain.product.Product
import com.overdrive.common.money.Money
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Local classification of competitor pricing/delivery strategy.
 * Maps from the catalog's generic [Competitor.distributionModel] string (DIRECT | DISTRIBUTOR |
 * HYBRID | MARKETPLACE) to the four domain strategy types described in the acceptance criteria.
 */
enum class CompetitorType {
    /** Direct-to-consumer retail, MSRP-anchored pricing. Catalog model: DIRECT. */
    BIG_BOX_RETAIL,

    /** Traditional B2B cost-plus distribution. Catalog model: DISTRIBUTOR. */
    INDUSTRIAL_DISTRIBUTOR,

    /** Margin-reseller, cost-based markup. Catalog model: HYBRID. */
    MARGIN_RESELLER,

    /** Membership/bulk warehouse club — thin margin, high freight. Catalog model: MARKETPLACE. */
    WAREHOUSE_CLUB;

    companion object {
        fun fromCatalogModel(model: String?): CompetitorType = when (model?.uppercase()) {
            "DIRECT" -> BIG_BOX_RETAIL
            "DISTRIBUTOR" -> INDUSTRIAL_DISTRIBUTOR
            "MARKETPLACE" -> WAREHOUSE_CLUB
            else -> MARGIN_RESELLER // HYBRID and unknown fall here
        }
    }
}

/**
 * Strategy that derives an estimated selling price and delivered cost from a competitor's
 * catalog profile. Each concrete strategy captures the pricing logic for one distribution
 * type. All results carry the input assumptions for the explain affordance.
 *
 * Coverage guard: call [isCovering] before calling either estimate method.
 */
sealed class CompetitorStrategy {

    abstract val type: CompetitorType

    abstract fun isCovering(product: Product, region: String, profile: Competitor): Boolean

    abstract fun estimateSellingPrice(product: Product, profile: Competitor): CompetitorEstimate

    abstract fun estimateDeliveredCost(
        product: Product,
        destZipRegion: String,
        profile: Competitor,
    ): CompetitorEstimate

    // ── concrete strategies ──────────────────────────────────────────────────

    /**
     * Buys at cost, marks up by their estimated margin.
     * Price = product.cost / (1 − margin%).
     */
    object MarginReseller : CompetitorStrategy() {
        override val type = CompetitorType.MARGIN_RESELLER

        override fun isCovering(product: Product, region: String, profile: Competitor) =
            profile.regionalPresence.contains(region) && !product.hazardous

        override fun estimateSellingPrice(product: Product, profile: Competitor): CompetitorEstimate {
            val cost = product.cost()
            val margin = (profile.estimatedMargin ?: DEFAULT_MARGIN).coerceIn(MARGIN_FLOOR, MARGIN_CAP)
            val divisor = BigDecimal.ONE.subtract(margin)
            val price = cost.amount.divide(divisor, MC)
            return CompetitorEstimate(
                price = Money.of(price, cost.currency),
                assumptions = mapOf(
                    "strategy" to type.name,
                    "baseCost" to cost.amount,
                    "marginPct" to margin,
                ),
            )
        }

        override fun estimateDeliveredCost(
            product: Product,
            destZipRegion: String,
            profile: Competitor,
        ): CompetitorEstimate {
            val selling = estimateSellingPrice(product, profile)
            val freight = freightByWeight(product)
            return CompetitorEstimate(
                price = selling.price + freight,
                assumptions = selling.assumptions + mapOf(
                    "estimatedFreight" to freight.amount,
                    "freightBasis" to "weight_class",
                ),
            )
        }
    }

    /**
     * MSRP-anchored. Direct sellers apply a volume discount derived from their margin.
     * Price = product.msrp × (1 − discount%).
     */
    object BigBoxRetail : CompetitorStrategy() {
        override val type = CompetitorType.BIG_BOX_RETAIL

        override fun isCovering(product: Product, region: String, profile: Competitor) =
            profile.regionalPresence.contains(region) && !product.hazardous && !product.temperatureSensitive

        override fun estimateSellingPrice(product: Product, profile: Competitor): CompetitorEstimate {
            val msrp = product.msrp()
            val margin = (profile.estimatedMargin ?: DEFAULT_MARGIN).coerceIn(MARGIN_FLOOR, MARGIN_CAP)
            val discount = margin.subtract(BigDecimal("0.03")).coerceAtLeast(BigDecimal.ZERO)
            val price = msrp.amount.multiply(BigDecimal.ONE.subtract(discount), MC)
            return CompetitorEstimate(
                price = Money.of(price, msrp.currency),
                assumptions = mapOf(
                    "strategy" to type.name,
                    "msrpAnchor" to msrp.amount,
                    "volumeDiscountPct" to discount,
                    "marginPct" to margin,
                ),
            )
        }

        override fun estimateDeliveredCost(
            product: Product,
            destZipRegion: String,
            profile: Competitor,
        ): CompetitorEstimate {
            val selling = estimateSellingPrice(product, profile)
            val freight = if (product.costAmount >= FREE_SHIP_THRESHOLD) Money.ZERO
            else freightByWeight(product)
            return CompetitorEstimate(
                price = selling.price + freight,
                assumptions = selling.assumptions + mapOf(
                    "freeShippingThreshold" to FREE_SHIP_THRESHOLD,
                    "estimatedFreight" to freight.amount,
                ),
            )
        }

        private val FREE_SHIP_THRESHOLD = BigDecimal("35.00")
    }

    /**
     * Cost-plus B2B pricing. Distributor adds their margin on top of cost.
     */
    object IndustrialDistributor : CompetitorStrategy() {
        override val type = CompetitorType.INDUSTRIAL_DISTRIBUTOR

        override fun isCovering(product: Product, region: String, profile: Competitor) =
            profile.regionalPresence.contains(region)

        override fun estimateSellingPrice(product: Product, profile: Competitor): CompetitorEstimate {
            val cost = product.cost()
            val margin = (profile.estimatedMargin ?: DEFAULT_MARGIN).coerceIn(MARGIN_FLOOR, MARGIN_CAP)
            val price = cost.amount.multiply(BigDecimal.ONE.add(margin), MC)
            return CompetitorEstimate(
                price = Money.of(price, cost.currency),
                assumptions = mapOf(
                    "strategy" to type.name,
                    "baseCost" to cost.amount,
                    "markupPct" to margin,
                ),
            )
        }

        override fun estimateDeliveredCost(
            product: Product,
            destZipRegion: String,
            profile: Competitor,
        ): CompetitorEstimate {
            val selling = estimateSellingPrice(product, profile)
            val freight = freightByWeight(product) * FUEL_SURCHARGE_MULTIPLIER
            return CompetitorEstimate(
                price = selling.price + freight,
                assumptions = selling.assumptions + mapOf(
                    "estimatedFreight" to freight.amount,
                    "fuelSurchargeMultiplier" to FUEL_SURCHARGE_MULTIPLIER,
                ),
            )
        }

        private val FUEL_SURCHARGE_MULTIPLIER = BigDecimal("1.12")
    }

    /**
     * Membership/bulk model. Sells at near-cost regardless of profile margin.
     */
    object WarehouseClub : CompetitorStrategy() {
        override val type = CompetitorType.WAREHOUSE_CLUB

        override fun isCovering(product: Product, region: String, profile: Competitor) =
            profile.regionalPresence.contains(region) &&
                (profile.numWarehouses ?: 0) >= MIN_WAREHOUSES_FOR_COVERAGE &&
                !product.hazardous &&
                !product.temperatureSensitive

        override fun estimateSellingPrice(product: Product, profile: Competitor): CompetitorEstimate {
            val cost = product.cost()
            val price = cost.amount.multiply(BigDecimal.ONE.add(WAREHOUSE_CLUB_TYPICAL_MARGIN), MC)
            return CompetitorEstimate(
                price = Money.of(price, cost.currency),
                assumptions = mapOf(
                    "strategy" to type.name,
                    "baseCost" to cost.amount,
                    "thinMarginPct" to WAREHOUSE_CLUB_TYPICAL_MARGIN,
                    "profileMarginIgnored" to (profile.estimatedMargin ?: BigDecimal.ZERO),
                ),
            )
        }

        override fun estimateDeliveredCost(
            product: Product,
            destZipRegion: String,
            profile: Competitor,
        ): CompetitorEstimate {
            val selling = estimateSellingPrice(product, profile)
            val freight = freightByWeight(product) * LTL_MULTIPLIER
            return CompetitorEstimate(
                price = selling.price + freight,
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
        private val DEFAULT_MARGIN = BigDecimal("0.25")

        fun freightByWeight(product: Product): Money {
            val weightLbs = product.weightLbs.toDouble()
            val amount = when {
                weightLbs <= 1.0 -> BigDecimal("5.99")
                weightLbs <= 5.0 -> BigDecimal("9.99")
                weightLbs <= 20.0 -> BigDecimal("14.99")
                weightLbs <= 70.0 -> BigDecimal("34.99")
                else -> BigDecimal("79.99")
            }
            return Money.of(amount)
        }

        fun forModel(model: String?): CompetitorStrategy = when (CompetitorType.fromCatalogModel(model)) {
            CompetitorType.BIG_BOX_RETAIL -> BigBoxRetail
            CompetitorType.INDUSTRIAL_DISTRIBUTOR -> IndustrialDistributor
            CompetitorType.MARGIN_RESELLER -> MarginReseller
            CompetitorType.WAREHOUSE_CLUB -> WarehouseClub
        }
    }
}

private fun BigDecimal.coerceIn(min: BigDecimal, max: BigDecimal) = this.max(min).min(max)
private fun BigDecimal.coerceAtLeast(min: BigDecimal) = this.max(min)
