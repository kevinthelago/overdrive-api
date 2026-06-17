package com.overdrive.analytics

import com.overdrive.support.BaseIntegrationTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.math.BigDecimal
import java.util.UUID

@AutoConfigureMockMvc
class AnalyticsEndpointTest : BaseIntegrationTest() {

    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var routeSummaryRepo: AnalyticsRouteSummaryRepository
    @Autowired lateinit var competitorRepo: AnalyticsCompetitorSummaryRepository
    @Autowired lateinit var opportunityRepo: AnalyticsOpportunitySummaryRepository
    @Autowired lateinit var utilizationRepo: AnalyticsWarehouseUtilizationRepository

    @BeforeEach
    fun seed() {
        routeSummaryRepo.deleteAll()
        competitorRepo.deleteAll()
        opportunityRepo.deleteAll()
        utilizationRepo.deleteAll()

        val productId = UUID.randomUUID()
        val warehouseId = UUID.randomUUID()
        val carrierId = UUID.randomUUID()
        val competitorId = UUID.randomUUID()

        routeSummaryRepo.save(AnalyticsRouteSummary(
            productId = productId,
            scenarioId = null,
            destinationZip = "10001",
            destinationRegion = "NORTHEAST",
            warehouseId = warehouseId,
            carrierId = carrierId,
            fulfillmentModel = "WAREHOUSE",
            serviceLevel = "STANDARD",
            transitDays = 3,
            productCostUsd = BigDecimal("25.00"),
            storageCostUsd = BigDecimal("1.50"),
            handlingCostUsd = BigDecimal("0.75"),
            freightCostUsd = BigDecimal("8.20"),
            feesCostUsd = BigDecimal("0.89"),
            reserveCostUsd = BigDecimal("0.45"),
            otherCostUsd = BigDecimal("0.21"),
            totalDeliveredCostUsd = BigDecimal("37.00"),
        ))

        competitorRepo.save(AnalyticsCompetitorSummary(
            productId = productId,
            competitorId = competitorId,
            scenarioId = null,
            ourDeliveredCostUsd = BigDecimal("37.00"),
            competitorPriceUsd = BigDecimal("42.00"),
            savingsPct = BigDecimal("11.90"),
            isOffered = true,
            notOfferedReason = null,
        ))

        opportunityRepo.save(AnalyticsOpportunitySummary(
            productId = productId,
            category = "Electronics",
            scenarioId = null,
            region = "NORTHEAST",
            opportunityScore = BigDecimal("0.7432"),
            savingsPctFactor = BigDecimal("0.80"),
            marketSizeFactor = BigDecimal("0.90"),
            orderFrequencyFactor = BigDecimal("0.85"),
            categoryGrowthFactor = BigDecimal("0.70"),
            supplierAvailabilityFactor = BigDecimal("1.00"),
            logisticsComplexityFactor = BigDecimal("0.60"),
            noRouteReason = null,
        ))

        utilizationRepo.save(AnalyticsWarehouseUtilization(
            warehouseId = warehouseId,
            scenarioId = null,
            palletCapacity = 1000,
            assignedPallets = 600,
            utilizationPct = BigDecimal("60.00"),
        ))
    }

    @Test
    fun `cost-breakdown returns baseline data`() {
        mvc.get("/api/analytics/cost-breakdown")
            .andExpect {
                status { isOk() }
                jsonPath("$.items.length()") { value(1) }
                jsonPath("$.items[0].totalDeliveredCostUsd") { value(37.0) }
            }
    }

    @Test
    fun `freight returns carrier and region breakdowns`() {
        mvc.get("/api/analytics/freight")
            .andExpect {
                status { isOk() }
                jsonPath("$.byCarrier.length()") { value(1) }
                jsonPath("$.byRegion.length()") { value(1) }
                jsonPath("$.byRegion[0].region") { value("NORTHEAST") }
            }
    }

    @Test
    fun `warehouse-utilization returns utilization data`() {
        mvc.get("/api/analytics/warehouse-utilization")
            .andExpect {
                status { isOk() }
                jsonPath("$.warehouses.length()") { value(1) }
                jsonPath("$.warehouses[0].utilizationPct") { value(60.0) }
            }
    }

    @Test
    fun `savings-distribution buckets cover the range`() {
        mvc.get("/api/analytics/savings-distribution")
            .andExpect {
                status { isOk() }
                jsonPath("$.buckets").isArray()
            }
    }

    @Test
    fun `competitor-matrix returns all entries`() {
        mvc.get("/api/analytics/competitor-matrix")
            .andExpect {
                status { isOk() }
                jsonPath("$.entries.length()") { value(1) }
                jsonPath("$.entries[0].savingsPct") { value(11.9) }
            }
    }

    @Test
    fun `opportunity-by-region aggregates region scores`() {
        mvc.get("/api/analytics/opportunity-by-region")
            .andExpect {
                status { isOk() }
                jsonPath("$.regions[0].region") { value("NORTHEAST") }
            }
    }

    @Test
    fun `opportunity-rankings returns products sorted by score desc`() {
        mvc.get("/api/analytics/opportunity-rankings")
            .andExpect {
                status { isOk() }
                jsonPath("$.products.length()") { value(1) }
                jsonPath("$.products[0].category") { value("Electronics") }
            }
    }

    @Test
    fun `scenario-aware endpoints accept scenarioId param`() {
        // No scenario data seeded — should return empty results without error
        val fakeSid = UUID.randomUUID()
        mvc.get("/api/analytics/cost-breakdown?scenarioId=$fakeSid")
            .andExpect { status { isOk() } }
        mvc.get("/api/analytics/freight?scenarioId=$fakeSid")
            .andExpect { status { isOk() } }
    }
}
