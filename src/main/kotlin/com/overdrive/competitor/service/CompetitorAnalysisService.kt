package com.overdrive.competitor.service

import com.overdrive.catalog.domain.CompetitorProfile
import com.overdrive.catalog.domain.Product
import com.overdrive.catalog.service.CompetitorProfileRepository
import com.overdrive.catalog.service.ProductRepository
import com.overdrive.common.geo.ZipCentroid
import com.overdrive.common.geo.ZipCentroidRepository
import com.overdrive.competitor.domain.CompetitorComparison
import com.overdrive.competitor.domain.CompetitorResult
import com.overdrive.competitor.domain.CompetitorStrategy
import com.overdrive.competitor.domain.CoverageDecision
import com.overdrive.competitor.domain.NotOfferedReason
import com.overdrive.routing.service.RoutingService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CompetitorAnalysisService(
    private val productRepository: ProductRepository,
    private val competitorProfileRepository: CompetitorProfileRepository,
    private val zipCentroidRepository: ZipCentroidRepository,
    private val routingService: RoutingService,
) {

    /**
     * Compares all active competitors for [productId] at [destinationZip].
     * Our delivered cost is fetched once from the routing engine; savings are relative to it.
     */
    fun compare(productId: Long, destinationZip: String): CompetitorComparison {
        val product = productRepository.findById(productId)
            .orElseThrow { NoSuchElementException("Product $productId not found") }

        val centroid = zipCentroidRepository.findByZip(destinationZip)
            ?: throw NoSuchElementException("ZIP $destinationZip not in centroid table")

        val routingResult = routingService.solveFor(productId, destinationZip, quantity = 1)
        val ourDeliveredCost = routingResult.deliveredCost

        val competitors = competitorProfileRepository.findAllActive()
        val results = competitors.map { profile ->
            evaluateCoverage(product, profile, centroid.region, destinationZip, ourDeliveredCost)
        }

        return CompetitorComparison(
            productId = productId,
            destinationZip = destinationZip,
            ourDeliveredCost = ourDeliveredCost,
            perCompetitor = results,
        )
    }

    /**
     * Batch comparison over all products in [categoryId].
     * Our delivered cost is fetched once per product, not per product-competitor pair.
     */
    fun compareByCategory(categoryId: Long, destinationZip: String): List<CompetitorComparison> {
        val products = productRepository.findByCategoryId(categoryId)
        val centroid = zipCentroidRepository.findByZip(destinationZip)
            ?: throw NoSuchElementException("ZIP $destinationZip not in centroid table")

        val competitors = competitorProfileRepository.findAllActive()

        return products.map { product ->
            val routingResult = routingService.solveFor(product.id, destinationZip, quantity = 1)
            val ourDeliveredCost = routingResult.deliveredCost
            val results = competitors.map { profile ->
                evaluateCoverage(product, profile, centroid.region, destinationZip, ourDeliveredCost)
            }
            CompetitorComparison(
                productId = product.id,
                destinationZip = destinationZip,
                ourDeliveredCost = ourDeliveredCost,
                perCompetitor = results,
            )
        }
    }

    // ── internal ─────────────────────────────────────────────────────────────

    private fun evaluateCoverage(
        product: Product,
        profile: CompetitorProfile,
        region: String,
        destinationZip: String,
        ourDeliveredCost: com.overdrive.common.money.Money,
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
                model = profile.distributionModel,
                sellingPrice = strategy.estimateSellingPrice(product, profile),
                deliveredCost = strategy.estimateDeliveredCost(product, destinationZip, profile),
            )
        }

        return CompetitorResult.from(coverage, ourDeliveredCost)
    }

    private fun coverageFailureReason(
        product: Product,
        region: String,
        profile: CompetitorProfile,
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
            com.overdrive.catalog.domain.DistributionModel.BIG_BOX_RETAIL,
            com.overdrive.catalog.domain.DistributionModel.WAREHOUSE_CLUB,
        )
        private val TEMP_RESTRICTED = setOf(
            com.overdrive.catalog.domain.DistributionModel.BIG_BOX_RETAIL,
            com.overdrive.catalog.domain.DistributionModel.WAREHOUSE_CLUB,
        )
    }
}
