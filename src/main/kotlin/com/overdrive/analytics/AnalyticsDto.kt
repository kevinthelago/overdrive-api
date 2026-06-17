package com.overdrive.analytics

import java.math.BigDecimal
import java.util.UUID

// ── Cost breakdown distribution (per product, shaped for D3 waterfall/stacked bar) ──

data class CostBreakdownEntry(
    val productId: UUID,
    val productName: String,
    val productCostUsd: BigDecimal,
    val storageCostUsd: BigDecimal,
    val handlingCostUsd: BigDecimal,
    val freightCostUsd: BigDecimal,
    val feesUsd: BigDecimal,
    val reserveUsd: BigDecimal,
    val otherUsd: BigDecimal,
    val totalDeliveredCostUsd: BigDecimal,
)

data class CostBreakdownResponse(val items: List<CostBreakdownEntry>)

// ── Freight analysis (by lane, mode, carrier) ──

data class FreightByCarrierEntry(
    val carrierId: UUID,
    val carrierName: String,
    val totalFreightUsd: BigDecimal,
    val shipmentCount: Long,
    val avgTransitDays: Double,
)

data class FreightByRegionEntry(
    val region: String,
    val totalFreightUsd: BigDecimal,
    val shipmentCount: Long,
)

data class FreightAnalysisResponse(
    val byCarrier: List<FreightByCarrierEntry>,
    val byRegion: List<FreightByRegionEntry>,
)

// ── Warehouse utilization (capacity vs assigned) ──

data class WarehouseUtilizationEntry(
    val warehouseId: UUID,
    val warehouseName: String,
    val palletCapacity: Int,
    val assignedPallets: Int,
    val utilizationPct: BigDecimal,
)

data class WarehouseUtilizationResponse(val warehouses: List<WarehouseUtilizationEntry>)

// ── Savings distribution (histogram of savings % across products) ──

data class SavingsDistributionBucket(
    val lowerBoundPct: Double,
    val upperBoundPct: Double,
    val productCount: Int,
)

data class SavingsDistributionResponse(val buckets: List<SavingsDistributionBucket>)

// ── Competitor matrix ──

data class CompetitorMatrixEntry(
    val productId: UUID,
    val productName: String,
    val competitorId: UUID,
    val competitorName: String,
    val ourDeliveredCostUsd: BigDecimal?,
    val competitorPriceUsd: BigDecimal?,
    val savingsPct: BigDecimal?,
    val isOffered: Boolean,
    val notOfferedReason: String?,
)

data class CompetitorMatrixResponse(val entries: List<CompetitorMatrixEntry>)

// ── Opportunity by region (drives choropleth heat map) ──

data class OpportunityRegionEntry(
    val region: String,
    val opportunityScore: BigDecimal,
    val productCount: Int,
)

data class OpportunityByRegionResponse(val regions: List<OpportunityRegionEntry>)

// ── Opportunity rankings (ranked products + categories) ──

data class OpportunityRankEntry(
    val productId: UUID,
    val productName: String,
    val category: String,
    val opportunityScore: BigDecimal,
    val savingsPctFactor: BigDecimal,
    val marketSizeFactor: BigDecimal,
    val orderFrequencyFactor: BigDecimal,
    val categoryGrowthFactor: BigDecimal,
    val supplierAvailabilityFactor: BigDecimal,
    val logisticsComplexityFactor: BigDecimal,
    val noRouteReason: String?,
)

data class OpportunityRankingsResponse(val products: List<OpportunityRankEntry>)
