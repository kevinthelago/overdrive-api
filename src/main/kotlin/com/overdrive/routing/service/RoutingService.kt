package com.overdrive.routing.service

import com.overdrive.cost.domain.Money
import com.overdrive.cost.domain.TransportMode
import com.overdrive.cost.domain.Weight
import com.overdrive.cost.domain.WeightUnit
import com.overdrive.explainability.domain.EngineType
import com.overdrive.explainability.domain.TraceStep
import com.overdrive.explainability.service.ExplainabilityService
import com.overdrive.routing.domain.Location
import com.overdrive.routing.domain.Route
import com.overdrive.routing.domain.RoutingContext
import com.overdrive.scenario.context.service.ScenarioContextResolver
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.UUID

private data class RouteCandidate(
    val carrier: String,
    val mode: TransportMode,
    val transitDays: Int,
    val costUsd: BigDecimal,
    val carrierPreferenceBonus: Double = 0.0
)

@Service
class RoutingService(
    private val explainabilityService: ExplainabilityService,
    private val scenarioContextResolver: ScenarioContextResolver
) {
    /**
     * Finds the optimal route for a distribution opportunity.
     * Scored by a weighted combination of cost (40%) and transit time (60%), with a carrier-preference bonus.
     */
    @Cacheable(
        value = ["routing"],
        key = "#context.opportunityId + ':' + (#context.scenarioContextId ?: 'none')"
    )
    fun findOptimalRoute(context: RoutingContext): Route {
        val candidates = buildCandidates(context)
            .filter { c -> context.requiredDeliveryDays?.let { c.transitDays <= it } ?: true }

        require(candidates.isNotEmpty()) {
            "No routes satisfy required delivery days ${context.requiredDeliveryDays} for opportunity ${context.opportunityId}"
        }

        val steps = mutableListOf<TraceStep>()
        val scored = scoreCandidates(candidates, context, steps)
        val best = scored.maxByOrNull { it.second }!!

        val trace = explainabilityService.record(
            engineType = EngineType.ROUTING,
            opportunityId = context.opportunityId,
            scenarioContextId = context.scenarioContextId,
            inputs = context,
            outputs = mapOf(
                "selectedCarrier" to best.first.carrier,
                "mode" to best.first.mode.name,
                "transitDays" to best.first.transitDays,
                "score" to best.second
            ),
            steps = steps
        )

        return Route(
            id = UUID.randomUUID(),
            opportunityId = context.opportunityId,
            scenarioContextId = context.scenarioContextId,
            origin = context.origin,
            destination = context.destination,
            carrier = best.first.carrier,
            mode = best.first.mode,
            transitDays = best.first.transitDays,
            estimatedCost = Money(best.first.costUsd.setScale(4, RoundingMode.HALF_UP)),
            score = best.second,
            traceId = trace.id,
            calculatedAt = Instant.now()
        )
    }

    /** Returns all ranked route candidates (useful for comparison views). */
    fun findAllRoutes(context: RoutingContext): List<Route> {
        val candidates = buildCandidates(context)
        val steps = mutableListOf<TraceStep>()
        val scored = scoreCandidates(candidates, context, steps)

        val trace = explainabilityService.record(
            engineType = EngineType.ROUTING,
            opportunityId = context.opportunityId,
            scenarioContextId = context.scenarioContextId,
            inputs = context,
            outputs = mapOf("candidateCount" to scored.size),
            steps = steps
        )

        return scored.sortedByDescending { it.second }.map { (candidate, score) ->
            Route(
                id = UUID.randomUUID(),
                opportunityId = context.opportunityId,
                scenarioContextId = context.scenarioContextId,
                origin = context.origin,
                destination = context.destination,
                carrier = candidate.carrier,
                mode = candidate.mode,
                transitDays = candidate.transitDays,
                estimatedCost = Money(candidate.costUsd.setScale(4, RoundingMode.HALF_UP)),
                score = score,
                traceId = trace.id,
                calculatedAt = Instant.now()
            )
        }
    }

    private fun buildCandidates(context: RoutingContext): List<RouteCandidate> {
        val weightKg = context.weight.toKg().value
        val scenarioCarrierPref = context.scenarioContextId
            ?.let { scenarioContextResolver.resolve(it) }
            ?.assumptions?.carrierPreference
        val effectivePreferredCarrier = context.preferredCarrier ?: scenarioCarrierPref

        return CARRIER_ROUTES.map { cr ->
            val ratePerKg = cr.defaultRatePerKg
            val cost = (ratePerKg * weightKg).setScale(4, RoundingMode.HALF_UP)
            RouteCandidate(
                carrier = cr.carrier,
                mode = cr.mode,
                transitDays = cr.transitDays,
                costUsd = cost,
                carrierPreferenceBonus = if (cr.carrier == effectivePreferredCarrier) CARRIER_BONUS else 0.0
            )
        }
    }

    private fun scoreCandidates(
        candidates: List<RouteCandidate>,
        context: RoutingContext,
        steps: MutableList<TraceStep>
    ): List<Pair<RouteCandidate, Double>> {
        if (candidates.isEmpty()) return emptyList()

        val maxCost = candidates.maxOf { it.costUsd }.toDouble()
        val maxDays = candidates.maxOf { it.transitDays }.toDouble()

        return candidates.mapIndexed { idx, c ->
            val normCost = if (maxCost > 0) 1.0 - (c.costUsd.toDouble() / maxCost) else 1.0
            val normDays = if (maxDays > 0) 1.0 - (c.transitDays.toDouble() / maxDays) else 1.0
            val score = (normCost * 0.4) + (normDays * 0.6) + c.carrierPreferenceBonus
            steps += TraceStep(
                step = idx + 1,
                description = "Score candidate ${c.carrier}/${c.mode}",
                inputs = mapOf("costUsd" to c.costUsd, "transitDays" to c.transitDays, "preferenceBonus" to c.carrierPreferenceBonus),
                outputs = mapOf("normCost" to normCost, "normDays" to normDays, "score" to score),
                rule = "score = normCost×0.4 + normDays×0.6 + preferenceBonus"
            )
            c to score
        }
    }

    companion object {
        private const val CARRIER_BONUS = 0.10

        private data class CarrierRoute(
            val carrier: String,
            val mode: TransportMode,
            val transitDays: Int,
            val defaultRatePerKg: BigDecimal
        )

        private val CARRIER_ROUTES = listOf(
            CarrierRoute("UPS", TransportMode.TRUCK, 3, BigDecimal("1.80")),
            CarrierRoute("FedEx", TransportMode.TRUCK, 2, BigDecimal("2.20")),
            CarrierRoute("BNSF", TransportMode.RAIL, 7, BigDecimal("1.10")),
            CarrierRoute("Maersk", TransportMode.OCEAN, 21, BigDecimal("0.55")),
            CarrierRoute("DHL", TransportMode.AIR, 1, BigDecimal("5.80"))
        )
    }
}
