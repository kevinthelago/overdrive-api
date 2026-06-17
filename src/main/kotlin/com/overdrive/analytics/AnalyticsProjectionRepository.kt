package com.overdrive.analytics

import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

// ── JPA entities backing the analytics_* tables ────────────────────────────

@Entity
@Table(name = "analytics_route_summary")
class AnalyticsRouteSummary(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(name = "product_id", nullable = false) val productId: UUID,
    @Column(name = "scenario_id") val scenarioId: UUID?,
    @Column(name = "destination_zip") val destinationZip: String?,
    @Column(name = "destination_region") val destinationRegion: String?,
    @Column(name = "warehouse_id") val warehouseId: UUID?,
    @Column(name = "carrier_id") val carrierId: UUID?,
    @Column(name = "fulfillment_model") val fulfillmentModel: String?,
    @Column(name = "service_level") val serviceLevel: String?,
    @Column(name = "transit_days") val transitDays: Int?,
    @Column(name = "product_cost_usd") val productCostUsd: BigDecimal?,
    @Column(name = "storage_cost_usd") val storageCostUsd: BigDecimal?,
    @Column(name = "handling_cost_usd") val handlingCostUsd: BigDecimal?,
    @Column(name = "freight_cost_usd") val freightCostUsd: BigDecimal?,
    @Column(name = "fees_cost_usd") val feesCostUsd: BigDecimal?,
    @Column(name = "reserve_cost_usd") val reserveCostUsd: BigDecimal?,
    @Column(name = "other_cost_usd") val otherCostUsd: BigDecimal?,
    @Column(name = "total_delivered_cost_usd") val totalDeliveredCostUsd: BigDecimal?,
    @Column(name = "computed_at", nullable = false) val computedAt: OffsetDateTime = OffsetDateTime.now(),
)

@Entity
@Table(name = "analytics_competitor_summary")
class AnalyticsCompetitorSummary(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(name = "product_id", nullable = false) val productId: UUID,
    @Column(name = "competitor_id", nullable = false) val competitorId: UUID,
    @Column(name = "scenario_id") val scenarioId: UUID?,
    @Column(name = "our_delivered_cost_usd") val ourDeliveredCostUsd: BigDecimal?,
    @Column(name = "competitor_price_usd") val competitorPriceUsd: BigDecimal?,
    @Column(name = "savings_pct") val savingsPct: BigDecimal?,
    @Column(name = "is_offered", nullable = false) val isOffered: Boolean = true,
    @Column(name = "not_offered_reason") val notOfferedReason: String?,
    @Column(name = "computed_at", nullable = false) val computedAt: OffsetDateTime = OffsetDateTime.now(),
)

@Entity
@Table(name = "analytics_opportunity_summary")
class AnalyticsOpportunitySummary(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(name = "product_id", nullable = false) val productId: UUID,
    @Column val category: String?,
    @Column(name = "scenario_id") val scenarioId: UUID?,
    @Column val region: String?,
    @Column(name = "opportunity_score") val opportunityScore: BigDecimal?,
    @Column(name = "savings_pct_factor") val savingsPctFactor: BigDecimal?,
    @Column(name = "market_size_factor") val marketSizeFactor: BigDecimal?,
    @Column(name = "order_frequency_factor") val orderFrequencyFactor: BigDecimal?,
    @Column(name = "category_growth_factor") val categoryGrowthFactor: BigDecimal?,
    @Column(name = "supplier_availability_factor") val supplierAvailabilityFactor: BigDecimal?,
    @Column(name = "logistics_complexity_factor") val logisticsComplexityFactor: BigDecimal?,
    @Column(name = "no_route_reason") val noRouteReason: String?,
    @Column(name = "computed_at", nullable = false) val computedAt: OffsetDateTime = OffsetDateTime.now(),
)

@Entity
@Table(name = "analytics_warehouse_utilization")
class AnalyticsWarehouseUtilization(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(name = "warehouse_id", nullable = false) val warehouseId: UUID,
    @Column(name = "scenario_id") val scenarioId: UUID?,
    @Column(name = "pallet_capacity") val palletCapacity: Int?,
    @Column(name = "assigned_pallets") val assignedPallets: Int?,
    @Column(name = "utilization_pct") val utilizationPct: BigDecimal?,
    @Column(name = "computed_at", nullable = false) val computedAt: OffsetDateTime = OffsetDateTime.now(),
)

// ── Spring Data repositories ────────────────────────────────────────────────

interface AnalyticsRouteSummaryRepository : JpaRepository<AnalyticsRouteSummary, UUID> {
    fun findByScenarioId(scenarioId: UUID?): List<AnalyticsRouteSummary>

    @Query("""
        SELECT a FROM AnalyticsRouteSummary a
        WHERE a.scenarioId IS NULL AND a.productId IN :productIds
    """)
    fun findBaselineByProductIds(@Param("productIds") productIds: List<UUID>): List<AnalyticsRouteSummary>
}

interface AnalyticsCompetitorSummaryRepository : JpaRepository<AnalyticsCompetitorSummary, UUID> {
    fun findByScenarioId(scenarioId: UUID?): List<AnalyticsCompetitorSummary>
}

interface AnalyticsOpportunitySummaryRepository : JpaRepository<AnalyticsOpportunitySummary, UUID> {
    fun findByScenarioIdOrderByOpportunityScoreDesc(scenarioId: UUID?): List<AnalyticsOpportunitySummary>

    @Query("""
        SELECT a.region, AVG(a.opportunityScore) AS avgScore, COUNT(DISTINCT a.productId) AS productCount
        FROM AnalyticsOpportunitySummary a
        WHERE a.scenarioId = :scenarioId AND a.region IS NOT NULL
        GROUP BY a.region
    """)
    fun aggregateByRegion(@Param("scenarioId") scenarioId: UUID?): List<Array<Any?>>
}

interface AnalyticsWarehouseUtilizationRepository : JpaRepository<AnalyticsWarehouseUtilization, UUID> {
    fun findByScenarioId(scenarioId: UUID?): List<AnalyticsWarehouseUtilization>
}
