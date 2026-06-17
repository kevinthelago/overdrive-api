package com.overdrive.scenario.app

import com.overdrive.scenario.domain.Scenario
import com.overdrive.scenario.web.ScenarioDiffResponse
import com.overdrive.scenario.web.ScenarioDiffItem
import com.overdrive.scenario.web.DiffStatus
// Interfaces from engine-core-api and market-api — available once upstream streams land.
import com.overdrive.scenario.context.OverlayView
import com.overdrive.scenario.context.ScenarioContext
import com.overdrive.routing.RoutingService
import com.overdrive.competitor.CompetitorService
import com.overdrive.opportunity.OpportunityService
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Runs Routing, Competitor, and Opportunity engines under both Baseline and Scenario contexts
 * and returns a structured per-product diff.
 *
 * Each result set is deterministic (engines are pure given the same context),
 * so the diff is cached by (scenarioId, productIds).
 */
@Service
class ScenarioDiffService(
    private val routingService: RoutingService,
    private val competitorService: CompetitorService,
    private val opportunityService: OpportunityService,
    private val baselineOverlayView: OverlayView,
) {

    /**
     * Compare baseline vs scenario for the given product IDs, routing to [destinationZip].
     * Products that become uncostable or over-constrained under the scenario are reported
     * with their reason; stale overrides are flagged at the top level.
     */
    @Cacheable("scenario-diff", key = "#scenarioId + ':' + #productIds.toSorted().toString() + ':' + #destinationZip")
    fun compare(
        scenario: Scenario,
        scenarioId: UUID,
        productIds: List<UUID>,
        destinationZip: String,
        serviceLevel: String,
    ): ScenarioDiffResponse {
        val overlayView = ScenarioOverlayView(scenario, baselineOverlayView)
        val scenarioCtx = ScenarioContext(overlayView)
        val baselineCtx = ScenarioContext.BASELINE

        val items = productIds.map { productId ->
            diffProduct(productId, destinationZip, serviceLevel, baselineCtx, scenarioCtx)
        }

        return ScenarioDiffResponse(
            scenarioId = scenarioId,
            scenarioName = scenario.name,
            staleOverrideEntityIds = overlayView.staleOverrideIds,
            items = items,
        )
    }

    private fun diffProduct(
        productId: UUID,
        destinationZip: String,
        serviceLevel: String,
        baselineCtx: ScenarioContext,
        scenarioCtx: ScenarioContext,
    ): ScenarioDiffItem {
        val baselineRoute = runCatching {
            routingService.solve(productId, destinationZip, quantity = 1, serviceLevel, baselineCtx)
        }.getOrNull()

        val scenarioRoute = runCatching {
            routingService.solve(productId, destinationZip, quantity = 1, serviceLevel, scenarioCtx)
        }.getOrNull()

        val baselineCompetitor = runCatching {
            competitorService.compare(productId, destinationZip, baselineCtx)
        }.getOrNull()

        val scenarioCompetitor = runCatching {
            competitorService.compare(productId, destinationZip, scenarioCtx)
        }.getOrNull()

        val baselineOpp = runCatching {
            opportunityService.scoreForProduct(productId, baselineCtx)
        }.getOrNull()

        val scenarioOpp = runCatching {
            opportunityService.scoreForProduct(productId, scenarioCtx)
        }.getOrNull()

        val status = when {
            scenarioRoute == null -> DiffStatus.UNCOSTABLE
            !scenarioRoute.hasFeasibleRoute -> DiffStatus.OVER_CONSTRAINED
            else -> DiffStatus.CHANGED
        }

        return ScenarioDiffItem(
            productId = productId,
            status = status,
            uncostableReason = if (scenarioRoute == null) "No route computed" else null,
            overConstrainedReason = scenarioRoute?.bindingConstraint,
            baselineDeliveredCost = baselineRoute?.winner?.totalDeliveredCost,
            scenarioDeliveredCost = scenarioRoute?.winner?.totalDeliveredCost,
            deliveredCostDelta = delta(
                baselineRoute?.winner?.totalDeliveredCost,
                scenarioRoute?.winner?.totalDeliveredCost,
            ),
            baselineWinnerRoute = baselineRoute?.winner?.toSummary(),
            scenarioWinnerRoute = scenarioRoute?.winner?.toSummary(),
            baselineSavingsPct = baselineCompetitor?.medianSavingsPct,
            scenarioSavingsPct = scenarioCompetitor?.medianSavingsPct,
            savingsPctDelta = delta(
                baselineCompetitor?.medianSavingsPct,
                scenarioCompetitor?.medianSavingsPct,
            ),
            baselineOpportunityScore = baselineOpp?.score,
            scenarioOpportunityScore = scenarioOpp?.score,
            opportunityScoreDelta = delta(baselineOpp?.score, scenarioOpp?.score),
        )
    }

    private fun delta(baseline: Number?, scenario: Number?): Double? {
        if (baseline == null || scenario == null) return null
        return scenario.toDouble() - baseline.toDouble()
    }
}
