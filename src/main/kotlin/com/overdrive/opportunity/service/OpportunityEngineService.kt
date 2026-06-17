package com.overdrive.opportunity.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.overdrive.catalog.service.ProductRepository
import com.overdrive.catalog.service.SupplierRepository
import com.overdrive.competitor.service.CompetitorAnalysisService
import com.overdrive.opportunity.domain.FactorNormalizer
import com.overdrive.opportunity.domain.FactorType
import com.overdrive.opportunity.domain.OpportunityFactor
import com.overdrive.opportunity.domain.OpportunityScore
import com.overdrive.opportunity.domain.RegionalScore
import com.overdrive.opportunity.domain.UsRegion
import com.overdrive.opportunity.domain.ZeroReason
import com.overdrive.opportunity.projection.OpportunityProjection
import com.overdrive.opportunity.projection.OpportunityProjectionRepository
import com.overdrive.routing.service.RoutingService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Service
class OpportunityEngineService(
    private val productRepository: ProductRepository,
    private val supplierRepository: SupplierRepository,
    private val competitorAnalysisService: CompetitorAnalysisService,
    private val routingService: RoutingService,
    private val projectionRepository: OpportunityProjectionRepository,
    private val objectMapper: ObjectMapper,
) {

    /**
     * Returns all ranked baseline opportunity scores (no scenario).
     * Reads from the pre-computed projection table; scores are only empty on first boot.
     */
    @Transactional(readOnly = true)
    fun getRankedOpportunities(scenarioId: UUID? = null): List<OpportunityScore> {
        val projections = if (scenarioId == null)
            projectionRepository.findAllByScenarioIdIsNullOrderByScoreDesc()
        else
            projectionRepository.findAllByScenarioIdOrderByScoreDesc(scenarioId)

        return projections.map { it.toDomain() }
    }

    /**
     * Recomputes scores for all active products and persists projections.
     * Idempotent: existing projections for the same product+scenario are replaced.
     */
    @Transactional
    fun recomputeAll(scenarioId: UUID? = null) {
        val products = productRepository.findAllActive()
        products.forEach { product ->
            val score = computeForProduct(product.id, scenarioId)
            persist(score, scenarioId)
        }
    }

    /**
     * Recomputes the score for a single product and persists it.
     */
    @Transactional
    fun recomputeForProduct(productId: Long, scenarioId: UUID? = null): OpportunityScore {
        val score = computeForProduct(productId, scenarioId)
        persist(score, scenarioId)
        return score
    }

    // ── internal ─────────────────────────────────────────────────────────────

    private fun computeForProduct(productId: Long, scenarioId: UUID?): OpportunityScore {
        val product = productRepository.findById(productId)
            .orElseThrow { NoSuchElementException("Product $productId not found") }

        // ── factor: savings % (median across competitors, or 0 on no-route / not-offered) ──
        val savingsPctFactor = run {
            val routingResult = runCatching {
                routingService.solveFor(productId, representativeZipForProduct(), quantity = 1)
            }.getOrNull()

            if (routingResult == null || !routingResult.feasible) {
                return@run OpportunityFactor(FactorType.SAVINGS_PCT, BigDecimal.ZERO, BigDecimal.ZERO, ZeroReason.NO_ROUTE)
            }

            val comparison = runCatching {
                competitorAnalysisService.compare(productId, representativeZipForProduct())
            }.getOrNull()

            val medianSavings = comparison?.medianSavingsPct ?: BigDecimal.ZERO
            if (medianSavings <= BigDecimal.ZERO && comparison?.perCompetitor?.all {
                    it is com.overdrive.competitor.domain.CompetitorResult.NotOffered
                } == true) {
                return@run OpportunityFactor(FactorType.SAVINGS_PCT, BigDecimal.ZERO, BigDecimal.ZERO,
                    ZeroReason.NOT_OFFERED_BY_ANY_COMPETITOR)
            }
            val normalized = FactorNormalizer.normalizeSavingsPct(medianSavings.max(BigDecimal.ZERO))
            OpportunityFactor(FactorType.SAVINGS_PCT, medianSavings, normalized)
        }

        // ── factor: market size ──────────────────────────────────────────────
        val marketSizeRaw = product.marketSize ?: BigDecimal.ZERO
        val marketSizeFactor = OpportunityFactor(
            FactorType.MARKET_SIZE, marketSizeRaw,
            FactorNormalizer.normalizeMarketSize(marketSizeRaw),
        )

        // ── factor: order frequency ──────────────────────────────────────────
        val orderFreqRaw = product.orderFrequency ?: BigDecimal.ZERO
        val orderFreqFactor = OpportunityFactor(
            FactorType.ORDER_FREQUENCY, orderFreqRaw,
            FactorNormalizer.normalizeOrderFrequency(orderFreqRaw),
        )

        // ── factor: category growth ──────────────────────────────────────────
        val growthRaw = product.categoryGrowth ?: BigDecimal.ZERO
        val categoryGrowthFactor = OpportunityFactor(
            FactorType.CATEGORY_GROWTH, growthRaw,
            FactorNormalizer.normalizeCategoryGrowth(growthRaw),
        )

        // ── factor: supplier availability ───────────────────────────────────
        val supplierAvailabilityRaw = supplierRepository.findByProductId(productId)
            .let { suppliers ->
                if (suppliers.isEmpty()) BigDecimal.ZERO
                else suppliers.map { it.reliabilityScore }.average().toBigDecimal()
            }
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
        val regionalBreakdown = computeRegionalBreakdown(productId, savingsPctFactor)

        return OpportunityScore.compute(
            productId = productId,
            categoryId = product.category.id,
            savingsPct = savingsPctFactor,
            marketSize = marketSizeFactor,
            orderFrequency = orderFreqFactor,
            categoryGrowth = categoryGrowthFactor,
            supplierAvailability = supplierFactor,
            logisticsComplexity = complexityFactor,
            regionalBreakdown = regionalBreakdown,
        )
    }

    private fun computeRegionalBreakdown(productId: Long, baselineSavings: OpportunityFactor): List<RegionalScore> =
        UsRegion.entries.mapNotNull { region ->
            val routingResult = runCatching {
                routingService.solveFor(productId, region.representativeZip, quantity = 1)
            }.getOrNull() ?: return@mapNotNull null

            if (!routingResult.feasible) return@mapNotNull null

            val comparison = runCatching {
                competitorAnalysisService.compare(productId, region.representativeZip)
            }.getOrNull() ?: return@mapNotNull null

            RegionalScore(
                productId = productId,
                region = region.label,
                representativeZip = region.representativeZip,
                score = comparison.medianSavingsPct.max(BigDecimal.ZERO),
                savingsPct = comparison.medianSavingsPct,
            )
        }

    /** Uses the national median ZIP for the baseline savings % computation. */
    private fun representativeZipForProduct() = UsRegion.MIDWEST.representativeZip

    private fun persist(score: OpportunityScore, scenarioId: UUID?) {
        projectionRepository.deleteByProductIdAndScenarioId(score.productId, scenarioId)
        projectionRepository.save(score.toProjection(scenarioId))
    }

    private fun OpportunityScore.toProjection(scenarioId: UUID?) = OpportunityProjection(
        productId = productId,
        categoryId = categoryId,
        score = this.score,
        savingsPct = savingsPct.rawValue,
        marketSizeUsd = marketSize.rawValue,
        orderFrequency = orderFrequency.rawValue,
        categoryGrowthPct = categoryGrowth.rawValue,
        supplierAvailability = supplierAvailability.rawValue,
        logisticsComplexity = logisticsComplexity.rawValue,
        regionBreakdownJson = objectMapper.writeValueAsString(regionalBreakdown),
        zeroReason = zeroReason?.name,
        scenarioId = scenarioId,
        computedAt = Instant.now(),
    )

    private fun OpportunityProjection.toDomain(): OpportunityScore {
        val regional: List<RegionalScore> = regionBreakdownJson
            ?.let { objectMapper.readValue(it, Array<RegionalScore>::class.java).toList() }
            ?: emptyList()
        return OpportunityScore(
            productId = productId,
            categoryId = categoryId,
            score = score,
            savingsPct = OpportunityFactor(FactorType.SAVINGS_PCT, savingsPct ?: BigDecimal.ZERO,
                savingsPct?.let { FactorNormalizer.normalizeSavingsPct(it) } ?: BigDecimal.ZERO),
            marketSize = OpportunityFactor(FactorType.MARKET_SIZE, marketSizeUsd ?: BigDecimal.ZERO,
                marketSizeUsd?.let { FactorNormalizer.normalizeMarketSize(it) } ?: BigDecimal.ZERO),
            orderFrequency = OpportunityFactor(FactorType.ORDER_FREQUENCY, orderFrequency ?: BigDecimal.ZERO,
                orderFrequency?.let { FactorNormalizer.normalizeOrderFrequency(it) } ?: BigDecimal.ZERO),
            categoryGrowth = OpportunityFactor(FactorType.CATEGORY_GROWTH, categoryGrowthPct ?: BigDecimal.ZERO,
                categoryGrowthPct?.let { FactorNormalizer.normalizeCategoryGrowth(it) } ?: BigDecimal.ZERO),
            supplierAvailability = OpportunityFactor(FactorType.SUPPLIER_AVAILABILITY,
                supplierAvailability ?: BigDecimal.ZERO,
                supplierAvailability?.let { FactorNormalizer.normalizeSupplierAvailability(it) } ?: BigDecimal.ZERO),
            logisticsComplexity = OpportunityFactor(FactorType.LOGISTICS_COMPLEXITY,
                logisticsComplexity ?: BigDecimal("1.0"),
                logisticsComplexity?.let { FactorNormalizer.normalizeLogisticsComplexity(it) } ?: BigDecimal("0.5")),
            zeroReason = zeroReason?.let { ZeroReason.valueOf(it) },
            regionalBreakdown = regional,
        )
    }
}
