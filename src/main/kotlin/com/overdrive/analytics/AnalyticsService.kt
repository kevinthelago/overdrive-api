package com.overdrive.analytics

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

/**
 * Aggregates analytics data from the denormalized read-model tables.
 * No cost-engine logic lives here — only reads from analytics_* projections.
 * All methods are scenario-aware: pass null scenarioId for baseline.
 */
@Service
@Transactional(readOnly = true)
class AnalyticsService(
    private val routeSummaryRepo: AnalyticsRouteSummaryRepository,
    private val competitorRepo: AnalyticsCompetitorSummaryRepository,
    private val opportunityRepo: AnalyticsOpportunitySummaryRepository,
    private val utilizationRepo: AnalyticsWarehouseUtilizationRepository,
) {

    @Cacheable("analytics-cost-breakdown", key = "#scenarioId")
    fun costBreakdown(scenarioId: UUID?): CostBreakdownResponse {
        val rows = routeSummaryRepo.findByScenarioId(scenarioId)
        val grouped = rows.groupBy { it.productId }
        val items = grouped.map { (productId, entries) ->
            val latest = entries.maxBy { it.computedAt }
            CostBreakdownEntry(
                productId = productId,
                productName = productId.toString(),
                productCostUsd = latest.productCostUsd ?: BigDecimal.ZERO,
                storageCostUsd = latest.storageCostUsd ?: BigDecimal.ZERO,
                handlingCostUsd = latest.handlingCostUsd ?: BigDecimal.ZERO,
                freightCostUsd = latest.freightCostUsd ?: BigDecimal.ZERO,
                feesUsd = latest.feesCostUsd ?: BigDecimal.ZERO,
                reserveUsd = latest.reserveCostUsd ?: BigDecimal.ZERO,
                otherUsd = latest.otherCostUsd ?: BigDecimal.ZERO,
                totalDeliveredCostUsd = latest.totalDeliveredCostUsd ?: BigDecimal.ZERO,
            )
        }
        return CostBreakdownResponse(items)
    }

    @Cacheable("analytics-freight", key = "#scenarioId")
    fun freightAnalysis(scenarioId: UUID?): FreightAnalysisResponse {
        val rows = routeSummaryRepo.findByScenarioId(scenarioId)

        val byCarrier = rows
            .filter { it.carrierId != null }
            .groupBy { it.carrierId!! }
            .map { (carrierId, entries) ->
                FreightByCarrierEntry(
                    carrierId = carrierId,
                    carrierName = carrierId.toString(),
                    totalFreightUsd = entries.sumOf { it.freightCostUsd ?: BigDecimal.ZERO },
                    shipmentCount = entries.size.toLong(),
                    avgTransitDays = entries.mapNotNull { it.transitDays }.average().takeIf { !it.isNaN() } ?: 0.0,
                )
            }
            .sortedByDescending { it.totalFreightUsd }

        val byRegion = rows
            .filter { it.destinationRegion != null }
            .groupBy { it.destinationRegion!! }
            .map { (region, entries) ->
                FreightByRegionEntry(
                    region = region,
                    totalFreightUsd = entries.sumOf { it.freightCostUsd ?: BigDecimal.ZERO },
                    shipmentCount = entries.size.toLong(),
                )
            }
            .sortedByDescending { it.totalFreightUsd }

        return FreightAnalysisResponse(byCarrier = byCarrier, byRegion = byRegion)
    }

    @Cacheable("analytics-utilization", key = "#scenarioId")
    fun warehouseUtilization(scenarioId: UUID?): WarehouseUtilizationResponse {
        val rows = utilizationRepo.findByScenarioId(scenarioId)
        val items = rows.map { row ->
            WarehouseUtilizationEntry(
                warehouseId = row.warehouseId,
                warehouseName = row.warehouseId.toString(),
                palletCapacity = row.palletCapacity ?: 0,
                assignedPallets = row.assignedPallets ?: 0,
                utilizationPct = row.utilizationPct ?: BigDecimal.ZERO,
            )
        }
        return WarehouseUtilizationResponse(items)
    }

    @Cacheable("analytics-savings-distribution", key = "#scenarioId")
    fun savingsDistribution(scenarioId: UUID?): SavingsDistributionResponse {
        val rows = competitorRepo.findByScenarioId(scenarioId)
        val pcts = rows.mapNotNull { it.savingsPct?.toDouble() }
        if (pcts.isEmpty()) return SavingsDistributionResponse(emptyList())

        val min = pcts.min()
        val max = pcts.max()
        val bucketCount = 10
        val step = if (max == min) 1.0 else (max - min) / bucketCount

        val buckets = (0 until bucketCount).map { i ->
            val lo = min + i * step
            val hi = lo + step
            SavingsDistributionBucket(
                lowerBoundPct = lo.roundTo(2),
                upperBoundPct = hi.roundTo(2),
                productCount = pcts.count { it >= lo && (i == bucketCount - 1 || it < hi) },
            )
        }
        return SavingsDistributionResponse(buckets)
    }

    @Cacheable("analytics-competitor-matrix", key = "#scenarioId")
    fun competitorMatrix(scenarioId: UUID?): CompetitorMatrixResponse {
        val rows = competitorRepo.findByScenarioId(scenarioId)
        val entries = rows.map { row ->
            CompetitorMatrixEntry(
                productId = row.productId,
                productName = row.productId.toString(),
                competitorId = row.competitorId,
                competitorName = row.competitorId.toString(),
                ourDeliveredCostUsd = row.ourDeliveredCostUsd,
                competitorPriceUsd = row.competitorPriceUsd,
                savingsPct = row.savingsPct,
                isOffered = row.isOffered,
                notOfferedReason = row.notOfferedReason,
            )
        }
        return CompetitorMatrixResponse(entries)
    }

    @Cacheable("analytics-opportunity-region", key = "#scenarioId")
    fun opportunityByRegion(scenarioId: UUID?): OpportunityByRegionResponse {
        val rawRows = opportunityRepo.aggregateByRegion(scenarioId)
        val regions = rawRows.map { row ->
            OpportunityRegionEntry(
                region = row[0] as String,
                opportunityScore = (row[1] as Double).let {
                    BigDecimal.valueOf(it).setScale(4, RoundingMode.HALF_UP)
                },
                productCount = (row[2] as Long).toInt(),
            )
        }.sortedByDescending { it.opportunityScore }
        return OpportunityByRegionResponse(regions)
    }

    @Cacheable("analytics-opportunity-rankings", key = "#scenarioId")
    fun opportunityRankings(scenarioId: UUID?): OpportunityRankingsResponse {
        val rows = opportunityRepo.findByScenarioIdOrderByOpportunityScoreDesc(scenarioId)
        val products = rows.map { row ->
            OpportunityRankEntry(
                productId = row.productId,
                productName = row.productId.toString(),
                category = row.category ?: "",
                opportunityScore = row.opportunityScore ?: BigDecimal.ZERO,
                savingsPctFactor = row.savingsPctFactor ?: BigDecimal.ZERO,
                marketSizeFactor = row.marketSizeFactor ?: BigDecimal.ZERO,
                orderFrequencyFactor = row.orderFrequencyFactor ?: BigDecimal.ZERO,
                categoryGrowthFactor = row.categoryGrowthFactor ?: BigDecimal.ZERO,
                supplierAvailabilityFactor = row.supplierAvailabilityFactor ?: BigDecimal.ZERO,
                logisticsComplexityFactor = row.logisticsComplexityFactor ?: BigDecimal.ZERO,
                noRouteReason = row.noRouteReason,
            )
        }
        return OpportunityRankingsResponse(products)
    }
}

private fun Double.roundTo(scale: Int): Double =
    BigDecimal.valueOf(this).setScale(scale, RoundingMode.HALF_UP).toDouble()
