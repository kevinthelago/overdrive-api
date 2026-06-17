package com.overdrive.competitor.service

import com.overdrive.catalog.domain.competitor.Competitor
import com.overdrive.catalog.domain.competitor.CompetitorRepository
import com.overdrive.catalog.domain.product.Product
import com.overdrive.catalog.domain.product.ProductRepository
import com.overdrive.catalog.domain.rate.ZipCentroid
import com.overdrive.catalog.domain.rate.ZipCentroidRepository
import com.overdrive.common.money.Money
import com.overdrive.competitor.domain.CompetitorComparison
import com.overdrive.competitor.domain.CompetitorResult
import com.overdrive.competitor.domain.CompetitorStrategy
import com.overdrive.competitor.domain.CoverageDecision
import com.overdrive.competitor.domain.NotOfferedReason
import com.overdrive.cost.domain.WeightUnit
import com.overdrive.routing.domain.Location
import com.overdrive.routing.domain.RoutingContext
import com.overdrive.routing.service.RoutingService
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

@Service
@Transactional(readOnly = true)
class CompetitorAnalysisService(
    private val productRepository: ProductRepository,
    private val competitorRepository: CompetitorRepository,
    private val zipCentroidRepository: ZipCentroidRepository,
    private val routingService: RoutingService,
) {

    /**
     * Compares all active competitors for [productId] at [destinationZip].
     * Our delivered cost (product cost + routing freight) is fetched once and
     * used as the savings baseline.
     */
    fun compare(productId: UUID, destinationZip: String): CompetitorComparison {
        val product = productRepository.findById(productId)
            .orElseThrow { NoSuchElementException("Product $productId not found") }

        val centroid = zipCentroidRepository.findById(destinationZip).orElse(null)
            ?: throw NoSuchElementException("ZIP $destinationZip not in centroid table")

        val ourDeliveredCost = resolveOurDeliveredCost(product, centroid)
        val competitors = competitorRepository.findAll()

        val results = competitors.map { evaluateCoverage(product, it, centroid.region, destinationZip, ourDeliveredCost) }
        return CompetitorComparison(
            productId = productId,
            destinationZip = destinationZip,
            ourDeliveredCost = ourDeliveredCost,
            perCompetitor = results,
        )
    }

    /**
     * Batch comparison over all products in [category].
     * Our delivered cost is fetched once per product, not per product-competitor pair.
     */
    fun compareByCategory(category: String, destinationZip: String): List<CompetitorComparison> {
        val products = productRepository.findByCategory(category, Pageable.unpaged()).content
        val centroid = zipCentroidRepository.findById(destinationZip).orElse(null)
            ?: throw NoSuchElementException("ZIP $destinationZip not in centroid table")

        val competitors = competitorRepository.findAll()

        return products.map { product ->
            val ourDeliveredCost = resolveOurDeliveredCost(product, centroid)
            val results = competitors.map { evaluateCoverage(product, it, centroid.region, destinationZip, ourDeliveredCost) }
            CompetitorComparison(
                productId = product.id,
                destinationZip = destinationZip,
                ourDeliveredCost = ourDeliveredCost,
                perCompetitor = results,
            )
        }
    }

    // ── internal ─────────────────────────────────────────────────────────────

    /**
     * Resolves our delivered cost = product.cost + routing freight.
     * Falls back to product.cost if routing has no feasible route.
     */
    private fun resolveOurDeliveredCost(product: Product, centroid: ZipCentroid): Money {
        val routingResult = runCatching {
            routingService.findOptimalRoute(
                RoutingContext(
                    opportunityId = UUID.randomUUID(),
                    origin = Location(countryCode = "US"),
                    destination = Location(countryCode = "US", region = centroid.region),
                    weight = com.overdrive.cost.domain.Weight(product.weightLbs, WeightUnit.LBS),
                    cargoValue = com.overdrive.cost.domain.Money(product.costAmount),
                ),
            )
        }.getOrNull()

        val cost = product.cost()
        val freight = routingResult?.estimatedCost?.amount ?: BigDecimal.ZERO
        return cost + Money.of(freight, cost.currency)
    }

    private fun evaluateCoverage(
        product: Product,
        profile: Competitor,
        region: String,
        destinationZip: String,
        ourDeliveredCost: Money,
    ): CompetitorResult {
        val strategy = CompetitorStrategy.forModel(profile.distributionModel)

        val coverageReason = coverageFailureReason(product, region, profile, strategy)
        val coverage: CoverageDecision = if (coverageReason != null) {
            CoverageDecision.NotOffered(
                competitorId = profile.id,
                competitorName = profile.name,
                reason = coverageReason,
            )
        } else {
            CoverageDecision.Covered(
                competitorId = profile.id,
                competitorName = profile.name,
                competitorType = strategy.type,
                sellingPrice = strategy.estimateSellingPrice(product, profile),
                deliveredCost = strategy.estimateDeliveredCost(product, destinationZip, profile),
            )
        }

        return CompetitorResult.from(coverage, ourDeliveredCost)
    }

    private fun coverageFailureReason(
        product: Product,
        region: String,
        profile: Competitor,
        strategy: CompetitorStrategy,
    ): NotOfferedReason? {
        if (!profile.regionalPresence.contains(region)) return NotOfferedReason.OUT_OF_REGION
        if (product.hazardous && strategy.type in HAZMAT_RESTRICTED) return NotOfferedReason.HAZMAT_RESTRICTION
        if (product.temperatureSensitive && strategy.type in TEMP_RESTRICTED) return NotOfferedReason.TEMPERATURE_RESTRICTION
        if (!strategy.isCovering(product, region, profile)) return NotOfferedReason.CATEGORY_RESTRICTION
        return null
    }

    companion object {
        private val HAZMAT_RESTRICTED = setOf(
            com.overdrive.competitor.domain.CompetitorType.BIG_BOX_RETAIL,
            com.overdrive.competitor.domain.CompetitorType.WAREHOUSE_CLUB,
        )
        private val TEMP_RESTRICTED = setOf(
            com.overdrive.competitor.domain.CompetitorType.BIG_BOX_RETAIL,
            com.overdrive.competitor.domain.CompetitorType.WAREHOUSE_CLUB,
        )
    }
}
