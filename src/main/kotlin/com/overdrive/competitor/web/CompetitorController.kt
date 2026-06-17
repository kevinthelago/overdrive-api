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

@RestController
@RequestMapping("/api/competitors")
@Validated
class CompetitorController(
    private val competitorAnalysisService: CompetitorAnalysisService,
) {

    /**
     * GET /api/competitors/compare?productId=&zip=
     * Returns a per-competitor price/delivered-cost comparison for one product at one ZIP.
     */
    @GetMapping("/compare")
    fun compare(
        @RequestParam productId: Long,
        @RequestParam zip: String,
    ): ResponseEntity<CompetitorComparisonResponse> {
        val result = competitorAnalysisService.compare(productId, zip)
        return ResponseEntity.ok(CompetitorComparisonResponse.from(result))
    }

    /**
     * GET /api/competitors/compare/batch?categoryId=&zip=
     * Batch comparison over all products in a category. Our delivered cost is resolved
     * once per product, not per product-competitor pair, avoiding N+1.
     */
    @GetMapping("/compare/batch")
    fun compareBatch(
        @RequestParam categoryId: Long,
        @RequestParam zip: String,
    ): ResponseEntity<BatchComparisonResponse> {
        val results = competitorAnalysisService.compareByCategory(categoryId, zip)
        return ResponseEntity.ok(
            BatchComparisonResponse(
                categoryId = categoryId,
                destinationZip = zip,
                comparisons = results.map { CompetitorComparisonResponse.from(it) },
            ),
        )
    }
}
