package com.overdrive.analytics

import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/analytics")
class AnalyticsController(private val analyticsService: AnalyticsService) {

    /** Optional ?scenarioId= query param; absent = baseline. */

    @GetMapping("/cost-breakdown")
    fun costBreakdown(@RequestParam scenarioId: UUID?): CostBreakdownResponse =
        analyticsService.costBreakdown(scenarioId)

    @GetMapping("/freight")
    fun freight(@RequestParam scenarioId: UUID?): FreightAnalysisResponse =
        analyticsService.freightAnalysis(scenarioId)

    @GetMapping("/warehouse-utilization")
    fun warehouseUtilization(@RequestParam scenarioId: UUID?): WarehouseUtilizationResponse =
        analyticsService.warehouseUtilization(scenarioId)

    @GetMapping("/savings-distribution")
    fun savingsDistribution(@RequestParam scenarioId: UUID?): SavingsDistributionResponse =
        analyticsService.savingsDistribution(scenarioId)

    @GetMapping("/competitor-matrix")
    fun competitorMatrix(@RequestParam scenarioId: UUID?): CompetitorMatrixResponse =
        analyticsService.competitorMatrix(scenarioId)

    @GetMapping("/opportunity-by-region")
    fun opportunityByRegion(@RequestParam scenarioId: UUID?): OpportunityByRegionResponse =
        analyticsService.opportunityByRegion(scenarioId)

    @GetMapping("/opportunity-rankings")
    fun opportunityRankings(@RequestParam scenarioId: UUID?): OpportunityRankingsResponse =
        analyticsService.opportunityRankings(scenarioId)
}
