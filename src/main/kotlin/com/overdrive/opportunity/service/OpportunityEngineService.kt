package com.overdrive.opportunity.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.overdrive.catalog.domain.product.Product
import com.overdrive.catalog.domain.product.ProductRepository
import com.overdrive.catalog.domain.supplier.SupplierProductRepository
import com.overdrive.common.money.Money
import com.overdrive.competitor.domain.CompetitorResult
import com.overdrive.competitor.service.CompetitorAnalysisService
import com.overdrive.cost.domain.WeightUnit
import com.overdrive.opportunity.domain.FactorNormalizer
import com.overdrive.opportunity.domain.FactorType
import com.overdrive.opportunity.domain.OpportunityFactor
import com.overdrive.opportunity.domain.OpportunityScore
import com.overdrive.opportunity.domain.RegionalScore
import com.overdrive.opportunity.domain.UsRegion
import com.overdrive.opportunity.domain.ZeroReason
import com.overdrive.opportunity.projection.OpportunityProjection
import com.overdrive.opportunity.projection.OpportunityProjectionRepository
import com.overdrive.routing.domain.Location
import com.overdrive.routing.domain.RoutingContext
import com.overdrive.routing.service.RoutingService
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Service
class OpportunityEngineService(
    private val productRepository: ProductRepository,
    private val supplierProductRepository: SupplierProductRepository,
    private val competitorAnalysisService: CompetitorAnalysisService,
    private val routingService: RoutingService,
    private val projectionRepository: OpportunityProjectionRepository,
    private val objectMapper: ObjectMapper,
) {

    @Transactional(readOnly = true)
    fun getRankedOpportunities(scenarioId: UUID? = null): List<OpportunityScore> {
        val projections = if (scenarioId == null)
            projectionRepository.findAllByScenarioIdIsNullOrderByScoreDesc()
        else
            projectionRepository.findAllByScenarioIdOrderByScoreDesc(scenarioId)
        return projections.map { it.toDomain() }
    }

    @Transactional
    fun recomputeAll(scenarioId: UUID? = null) {
        productRepository.findAll().forEach { product ->
            val score = computeForProduct(product, scenarioId)
            persist(score, scenarioId)
        }
    }

    @Transactional
    fun recomputeForProduct(productId: UUID, scenarioId: UUID? = null): OpportunityScore {
        val product = productRepository.findById(productId)
            .orElseThrow { NoSuchElementException("Product $productId not found") }
        val score = computeForProduct(product, scenarioId)
        persist(score, scenarioId)
        return score
    }

    // ── internal ─────────────────────────────────────────────────────────────

    private fun computeForProduct(product: Product, scenarioId: UUID?): OpportunityScore {
        val baselineZip = UsRegion.MIDWEST.representativeZip

        // ── factor: savings % ────────────────────────────────────────────────
        val savingsPctFactor = run {
            val route = runCatching {
                routingService.findOptimalRoute(
                    RoutingContext(
                        opportunityId = UUID.randomUUID(),
                        origin = Location("US"),
                        destination = Location("US", UsRegion.MIDWEST.regionLabel),
                        weight = com.overdrive.cost.domain.Weight(product.weightLbs, WeightUnit.LBS),
                        cargoValue = com.overdrive.cost.domain.Money(product.costAmount),
                    ),
                )
            }.getOrNull()

            if (route == null) {
                return@run OpportunityFactor(FactorType.SAVINGS_PCT, BigDecimal.ZERO, BigDecimal.ZERO, ZeroReason.NO_ROUTE)
            }

            val comparison = runCatching {
                competitorAnalysisService.compare(product.id, baselineZip)
            }.getOrNull()

            val allNotOffered = comparison?.perCompetitor?.all { it is CompetitorResult.NotOffered } ?: true
            if (allNotOffered && (comparison?.perCompetitor?.isNotEmpty() == true)) {
                return@run OpportunityFactor(FactorType.SAVINGS_PCT, BigDecimal.ZERO, BigDecimal.ZERO,
                    ZeroReason.NOT_OFFERED_BY_ANY_COMPETITOR)
            }

            val median = comparison?.medianSavingsPct?.max(BigDecimal.ZERO) ?: BigDecimal.ZERO
            OpportunityFactor(FactorType.SAVINGS_PCT, median, FactorNormalizer.normalizeSavingsPct(median))
        }

        // ── factor: market size ──────────────────────────────────────────────
        val marketSizeRaw = product.marketSize ?: BigDecimal.ZERO
        val marketSizeFactor = OpportunityFactor(
            FactorType.MARKET_SIZE, marketSizeRaw, FactorNormalizer.normalizeMarketSize(marketSizeRaw),
        )

        // ── factor: order frequency ──────────────────────────────────────────
        val orderFreqRaw = product.orderFrequency ?: BigDecimal.ZERO
        val orderFreqFactor = OpportunityFactor(
            FactorType.ORDER_FREQUENCY, orderFreqRaw, FactorNormalizer.normalizeOrderFrequency(orderFreqRaw),
        )

        // ── factor: category growth ──────────────────────────────────────────
        val growthRaw = product.categoryGrowth ?: BigDecimal.ZERO
        val categoryGrowthFactor = OpportunityFactor(
            FactorType.CATEGORY_GROWTH, growthRaw, FactorNormalizer.normalizeCategoryGrowth(growthRaw),
        )

        // ── factor: supplier availability ───────────────────────────────────
        val supplierLinks = supplierProductRepository.findByProductId(product.id)
        val supplierAvailabilityRaw = if (supplierLinks.isEmpty()) BigDecimal.ZERO
        else supplierLinks.map { it.supplier.reliabilityScore }
            .fold(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal(supplierLinks.size), 6, java.math.RoundingMode.HALF_UP)
        val supplierFactor = OpportunityFactor(
            FactorType.SUPPLIER_AVAILABILITY, supplierAvailabilityRaw,
            FactorNormalizer.normalizeSupplierAvailability(supplierAvailabilityRaw),
            zeroReason = if (supplierAvailabilityRaw <= BigDecimal.ZERO) ZeroReason.NO_SUPPLIER_AVAILABILITY else null,
        )

        // ── factor: logistics complexity ─────────────────────────────────────
        val complexityRaw = product.logisticsComplexity ?: BigDecimal("1.0")
        val complexityFactor = OpportunityFactor(
            FactorType.LOGISTICS_COMPLEXITY, complexityRaw,
            FactorNormalizer.normalizeLogisticsComplexity(complexityRaw),
        )

        // ── per-region breakdown ─────────────────────────────────────────────
        val regionalBreakdown = computeRegionalBreakdown(product)

        return OpportunityScore.compute(
            productId = product.id,
            category = product.category,
            savingsPct = savingsPctFactor,
            marketSize = marketSizeFactor,
            orderFrequency = orderFreqFactor,
            categoryGrowth = categoryGrowthFactor,
            supplierAvailability = supplierFactor,
            logisticsComplexity = complexityFactor,
            regionalBreakdown = regionalBreakdown,
        )
    }

    private fun computeRegionalBreakdown(product: Product): List<RegionalScore> =
        UsRegion.entries.mapNotNull { region ->
            val route = runCatching {
                routingService.findOptimalRoute(
                    RoutingContext(
                        opportunityId = UUID.randomUUID(),
                        origin = Location("US"),
                        destination = Location("US", region.regionLabel),
                        weight = com.overdrive.cost.domain.Weight(product.weightLbs, WeightUnit.LBS),
                        cargoValue = com.overdrive.cost.domain.Money(product.costAmount),
                    ),
                )
            }.getOrNull() ?: return@mapNotNull null

            val comparison = runCatching {
                competitorAnalysisService.compare(product.id, region.representativeZip)
            }.getOrNull() ?: return@mapNotNull null

            val medianSavings = comparison.medianSavingsPct
            val cost = product.cost()
            @Suppress("UNUSED_VARIABLE")
            val ourDeliveredCost = cost + Money.of(route.estimatedCost.amount, cost.currency)
            val score = medianSavings.max(BigDecimal.ZERO)

            RegionalScore(
                productId = product.id,
                region = region.regionLabel,
                representativeZip = region.representativeZip,
                score = score,
                savingsPct = medianSavings,
            )
        }

    private fun persist(score: OpportunityScore, scenarioId: UUID?) {
        projectionRepository.deleteByProductIdAndScenarioId(score.productId, scenarioId)
        projectionRepository.save(score.toProjection(scenarioId))
    }

    private fun OpportunityScore.toProjection(scenarioId: UUID?) = OpportunityProjection(
        productId = productId,
        category = category,
        score = score,
        savingsPct = savingsPct.rawValue,
        marketSizeUsd = marketSize.rawValue,
        orderFrequency = orderFrequency.rawValue,
        categoryGrowthPct = categoryGrowth.rawValue,
        supplierAvailability = supplierAvailability.rawValue,
        logisticsComplexity = logisticsComplexity.rawValue,
        regionBreakdownJson = objectMapper.writeValueAsString(regionalBreakdown),
        zeroReason = this.zeroReason?.name,
        scenarioId = scenarioId,
        computedAt = Instant.now(),
    )

    private fun OpportunityProjection.toDomain(): OpportunityScore {
        val regional: List<RegionalScore> = regionBreakdownJson
            ?.let { objectMapper.readValue(it, Array<RegionalScore>::class.java).toList() }
            ?: emptyList()
        return OpportunityScore(
            productId = productId,
            category = category,
            score = score,
            savingsPct = factor(FactorType.SAVINGS_PCT, savingsPct, FactorNormalizer::normalizeSavingsPct),
            marketSize = factor(FactorType.MARKET_SIZE, marketSizeUsd, FactorNormalizer::normalizeMarketSize),
            orderFrequency = factor(FactorType.ORDER_FREQUENCY, orderFrequency, FactorNormalizer::normalizeOrderFrequency),
            categoryGrowth = factor(FactorType.CATEGORY_GROWTH, categoryGrowthPct, FactorNormalizer::normalizeCategoryGrowth),
            supplierAvailability = factor(FactorType.SUPPLIER_AVAILABILITY, this.supplierAvailability, FactorNormalizer::normalizeSupplierAvailability),
            logisticsComplexity = factor(FactorType.LOGISTICS_COMPLEXITY, logisticsComplexity ?: BigDecimal("1.0"), FactorNormalizer::normalizeLogisticsComplexity),
            zeroReason = this.zeroReason?.let { ZeroReason.valueOf(it) },
            regionalBreakdown = regional,
        )
    }

    private fun factor(type: FactorType, raw: BigDecimal?, normalize: (BigDecimal) -> BigDecimal): OpportunityFactor {
        val r = raw ?: BigDecimal.ZERO
        return OpportunityFactor(type, r, normalize(r))
    }
}
