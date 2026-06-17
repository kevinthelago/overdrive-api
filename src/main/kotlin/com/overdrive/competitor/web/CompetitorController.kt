package com.overdrive.competitor.web

import com.overdrive.competitor.dto.BatchComparisonResponse
import com.overdrive.competitor.dto.CompetitorComparisonResponse
import com.overdrive.competitor.service.CompetitorAnalysisService
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/competitors")
@Validated
class CompetitorController(
    private val competitorAnalysisService: CompetitorAnalysisService,
) {

    /**
     * GET /api/competitors/compare?productId=&zip=
     * Per-competitor price/delivered-cost comparison for one product at one ZIP.
     */
    @GetMapping("/compare")
    fun compare(
        @RequestParam productId: UUID,
        @RequestParam zip: String,
    ): ResponseEntity<CompetitorComparisonResponse> {
        val result = competitorAnalysisService.compare(productId, zip)
        return ResponseEntity.ok(CompetitorComparisonResponse.from(result))
    }

    /**
     * GET /api/competitors/compare/batch?category=&zip=
     * Batch comparison over all products in a category. Our delivered cost is resolved
     * once per product to avoid N+1 routing calls.
     */
    @GetMapping("/compare/batch")
    fun compareBatch(
        @RequestParam category: String,
        @RequestParam zip: String,
    ): ResponseEntity<BatchComparisonResponse> {
        val results = competitorAnalysisService.compareByCategory(category, zip)
        return ResponseEntity.ok(
            BatchComparisonResponse(
                category = category,
                destinationZip = zip,
                comparisons = results.map { CompetitorComparisonResponse.from(it) },
            ),
        )
    }
}
