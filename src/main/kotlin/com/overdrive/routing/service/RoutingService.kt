package com.overdrive.routing.service

import com.overdrive.common.money.Money
import com.overdrive.cost.domain.TransportMode
import com.overdrive.explainability.domain.EngineType
import com.overdrive.explainability.domain.TraceStep
import com.overdrive.explainability.service.ExplainabilityService
import com.overdrive.routing.domain.Route
import com.overdrive.routing.domain.RoutingContext
import com.overdrive.scenario.context.service.ScenarioContextResolver
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.UUID

private data class Candidate(
    val carrier: String,
    val mode: TransportMode,
    val transitDays: Int,
    val ratePerPound: BigDecimal,
)

@Service
class RoutingService(
    private val explainabilityService: ExplainabilityService,
    private val scenarioContextResolver: ScenarioContextResolver,
) {
    @Cacheable(value = ["optimal-route"], key = "#context.opportunityId + ':' + #context.scenarioContextId")
    fun findOptimalRoute(context: RoutingContext): Route = findAllRoutes(context).first()

    fun findAllRoutes(context: RoutingContext): List<Route> {
        val preferredCarrier = resolvePreferredCarrier(context)
        val overrides = resolveRateOverrides(context)

        val candidates = CANDIDATES.filter { c ->
            context.requiredDeliveryDays == null || c.transitDays <= context.requiredDeliveryDays
        }.ifEmpty {
            throw IllegalArgumentException(
                "No carrier meets the required delivery window of ${context.requiredDeliveryDays} days"
            )
        }

        val costs = candidates.map { c ->
            val rate = overrides["freight.rate.${c.carrier}"] ?: c.ratePerPound
            c to Money.of(context.weight.pounds.multiply(rate).setScale(2, RoundingMode.HALF_UP))
        }

        val maxCost = costs.maxOf { (_, cost) -> cost.amount.toDouble() }
        val maxDays = candidates.maxOf { it.transitDays }.toDouble()

        val scored = costs.map { (c, cost) ->
            val costNorm = if (maxCost == 0.0) 1.0 else cost.amount.toDouble() / maxCost
            val daysNorm = if (maxDays == 0.0) 1.0 else c.transitDays / maxDays
            val baseScore = 1.0 / (costNorm * 0.4 + daysNorm * 0.6 + 0.001)
            // Preferred carrier is always ranked first; score is pinned to MAX_VALUE so it
            // survives any cost/time combination without relying on a small multiplier.
            val score = if (c.carrier == preferredCarrier) Double.MAX_VALUE else baseScore
            Triple(c, cost, score)
        }.sortedByDescending { it.third }

        val steps = scored.mapIndexed { i, (c, cost, score) ->
            TraceStep(
                step = i + 1,
                description = "Candidate: ${c.carrier}",
                inputs = mapOf("carrier" to c.carrier, "mode" to c.mode, "days" to c.transitDays, "ratePerLb" to c.ratePerPound),
                outputs = mapOf("cost" to cost.amount, "score" to score),
                rule = "score = 1 / (costNorm×0.4 + daysNorm×0.6)",
            )
        }

        val trace = explainabilityService.record(
            engineType = EngineType.ROUTING,
            opportunityId = context.opportunityId,
            scenarioContextId = context.scenarioContextId,
            inputs = context,
            outputs = mapOf("candidateCount" to scored.size, "winner" to scored.first().first.carrier),
            steps = steps,
        )

        return scored.map { (c, cost, score) ->
            Route(
                id = UUID.randomUUID(),
                opportunityId = context.opportunityId,
                scenarioContextId = context.scenarioContextId,
                origin = context.origin,
                destination = context.destination,
                carrier = c.carrier,
                mode = c.mode,
                transitDays = c.transitDays,
                estimatedCost = cost,
                score = score,
                traceId = trace.id,
                calculatedAt = Instant.now(),
            )
        }
    }

    private fun resolvePreferredCarrier(context: RoutingContext): String? =
        context.preferredCarrier
            ?: context.scenarioContextId?.let { scenarioContextResolver.resolve(it) }
                ?.assumptions?.carrierPreference

    private fun resolveRateOverrides(context: RoutingContext): Map<String, BigDecimal> {
        val scenarioOverrides = context.scenarioContextId
            ?.let { scenarioContextResolver.resolve(it) }
            ?.assumptions?.freightRateOverrides
            .orEmpty()
        return scenarioOverrides + context.scenarioOverrides
    }

    companion object {
        private val CANDIDATES = listOf(
            Candidate("UPS", TransportMode.TRUCK, 3, BigDecimal("1.85")),
            Candidate("FedEx Ground", TransportMode.TRUCK, 4, BigDecimal("1.75")),
            Candidate("XPO", TransportMode.TRUCK, 5, BigDecimal("1.60")),
            Candidate("Norfolk Southern", TransportMode.RAIL, 7, BigDecimal("1.10")),
            Candidate("BNSF", TransportMode.RAIL, 8, BigDecimal("1.05")),
            Candidate("Maersk", TransportMode.OCEAN, 21, BigDecimal("0.55")),
            Candidate("FedEx Air", TransportMode.AIR, 1, BigDecimal("5.80")),
            Candidate("DHL", TransportMode.AIR, 2, BigDecimal("5.20")),
        )
    }
}
