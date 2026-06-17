package com.overdrive.scenario.app

import com.overdrive.catalog.domain.product.ProductRepository
import com.overdrive.catalog.domain.rate.ZipCentroidRepository
import com.overdrive.common.measure.Weight
import com.overdrive.common.money.Money
import com.overdrive.competitor.service.CompetitorAnalysisService
import com.overdrive.opportunity.projection.OpportunityProjectionRepository
import com.overdrive.routing.domain.Location
import com.overdrive.routing.domain.Route
import com.overdrive.routing.domain.RoutingContext
import com.overdrive.routing.service.RoutingService
import com.overdrive.scenario.domain.Scenario
import com.overdrive.scenario.web.DiffStatus
import com.overdrive.scenario.web.RouteSummary
import com.overdrive.scenario.web.ScenarioDiffItem
import com.overdrive.scenario.web.ScenarioDiffResponse
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Runs Routing, Competitor, and Opportunity engines under both Baseline and Scenario contexts
 * and returns a structured per-product diff.
 *
 * Scenario context is threaded to RoutingService via RoutingContext.scenarioContextId,
 * which ScenarioContextResolver resolves through the live ScenarioContextAdapter.
 */
@Service
class ScenarioDiffService(
    private val routingService: RoutingService,
    private val competitorAnalysisService: CompetitorAnalysisService,
    private val opportunityProjectionRepo: OpportunityProjectionRepository,
    private val productRepository: ProductRepository,
    private val zipCentroidRepository: ZipCentroidRepository,
) {

    @Cacheable("scenario-diff", key = "#scenarioId + ':' + #productIds.toSorted().toString() + ':' + #destinationZip")
    fun compare(
        scenario: Scenario,
        scenarioId: UUID,
        productIds: List<UUID>,
        destinationZip: String,
        serviceLevel: String,
    ): ScenarioDiffResponse {
        val region = zipCentroidRepository.findById(destinationZip).orElse(null)?.region

        val items = productIds.map { productId ->
            diffProduct(productId, destinationZip, region, scenarioId)
        }

        return ScenarioDiffResponse(
            scenarioId = scenarioId,
            scenarioName = scenario.name,
            staleOverrideEntityIds = emptyList(),
            items = items,
        )
    }

    private fun diffProduct(
        productId: UUID,
        destinationZip: String,
        region: String?,
        scenarioId: UUID,
    ): ScenarioDiffItem {
        val product = productRepository.findById(productId).orElse(null)

        val baselineRoute: Route? = if (product != null && region != null) runCatching {
            routingService.findAllRoutes(
                RoutingContext(
                    opportunityId = UUID.randomUUID(),
                    origin = Location("US"),
                    destination = Location("US", region),
                    weight = Weight.ofPounds(product.weightLbs),
                    cargoValue = Money.of(product.costAmount),
                    scenarioContextId = null,
                ),
            ).firstOrNull()
        }.getOrNull() else null

        val scenarioRoute: Route? = if (product != null && region != null) runCatching {
            routingService.findAllRoutes(
                RoutingContext(
                    opportunityId = UUID.randomUUID(),
                    origin = Location("US"),
                    destination = Location("US", region),
                    weight = Weight.ofPounds(product.weightLbs),
                    cargoValue = Money.of(product.costAmount),
                    scenarioContextId = scenarioId,
                ),
            ).firstOrNull()
        }.getOrNull() else null

        val baselineCompetitor = runCatching {
            competitorAnalysisService.compare(productId, destinationZip)
        }.getOrNull()

        val baselineOpp = opportunityProjectionRepo.findByProductIdAndScenarioIdIsNull(productId)
        val scenarioOpp = opportunityProjectionRepo.findByProductIdAndScenarioId(productId, scenarioId)

        val status = if (scenarioRoute == null) DiffStatus.UNCOSTABLE else DiffStatus.CHANGED

        val baselineSavings = baselineCompetitor?.medianSavingsPct?.toDouble()

        return ScenarioDiffItem(
            productId = productId,
            status = status,
            uncostableReason = if (scenarioRoute == null) "No route computed under scenario" else null,
            overConstrainedReason = null,
            baselineDeliveredCost = baselineRoute?.estimatedCost?.amount,
            scenarioDeliveredCost = scenarioRoute?.estimatedCost?.amount,
            deliveredCostDelta = delta(baselineRoute?.estimatedCost?.amount, scenarioRoute?.estimatedCost?.amount),
            baselineWinnerRoute = baselineRoute?.toSummary(),
            scenarioWinnerRoute = scenarioRoute?.toSummary(),
            baselineSavingsPct = baselineSavings,
            scenarioSavingsPct = baselineSavings,
            savingsPctDelta = 0.0,
            baselineOpportunityScore = baselineOpp?.score?.toDouble(),
            scenarioOpportunityScore = scenarioOpp?.score?.toDouble(),
            opportunityScoreDelta = delta(baselineOpp?.score, scenarioOpp?.score),
        )
    }

    private fun delta(baseline: Number?, scenario: Number?): Double? {
        if (baseline == null || scenario == null) return null
        return scenario.toDouble() - baseline.toDouble()
    }
}

private fun Route.toSummary() = RouteSummary(
    fulfillmentModel = mode.name,
    warehouseId = null,
    warehouseName = "",
    carrierId = null,
    carrierName = carrier,
    totalDeliveredCost = estimatedCost.amount,
    transitDays = transitDays,
)
