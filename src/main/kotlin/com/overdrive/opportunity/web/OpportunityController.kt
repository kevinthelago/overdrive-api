package com.overdrive.opportunity.web

import com.overdrive.opportunity.dto.CategoryOpportunity
import com.overdrive.opportunity.dto.OpportunityResponse
import com.overdrive.opportunity.dto.OpportunityResult
import com.overdrive.opportunity.service.OpportunityEngineService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

@RestController
@RequestMapping("/api/opportunities")
class OpportunityController(
    private val opportunityEngineService: OpportunityEngineService,
) {

    /**
     * GET /api/opportunities
     * Returns ranked OpportunityResult DTOs with all six factor values, and a region breakdown.
     * Pass [scenarioId] to overlay a scenario on top of the baseline.
     */
    @GetMapping
    fun getOpportunities(
        @RequestParam(required = false) scenarioId: UUID?,
    ): ResponseEntity<OpportunityResponse> {
        val scores = opportunityEngineService.getRankedOpportunities(scenarioId)
        val results = scores.map { OpportunityResult.from(it) }
        return ResponseEntity.ok(
            OpportunityResponse(
                products = results,
                categoryRankings = aggregateByCategory(results),
            ),
        )
    }

    /**
     * POST /api/opportunities/recompute
     * Triggers a full recompute of all opportunity scores. Idempotent.
     */
    @PostMapping("/recompute")
    fun recompute(
        @RequestParam(required = false) scenarioId: UUID?,
    ): ResponseEntity<Void> {
        opportunityEngineService.recomputeAll(scenarioId)
        return ResponseEntity.accepted().build()
    }

    private fun aggregateByCategory(results: List<OpportunityResult>): List<CategoryOpportunity> =
        results.groupBy { it.category }
            .map { (category, group) ->
                val avg = group.map { it.score }
                    .fold(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal(group.size), 4, RoundingMode.HALF_UP)
                CategoryOpportunity(
                    category = category,
                    aggregateScore = avg,
                    productCount = group.size,
                    topProductId = group.maxByOrNull { it.score }?.productId,
                )
            }
            .sortedByDescending { it.aggregateScore }
}
