package com.overdrive.explainability.api

import com.overdrive.explainability.domain.EngineTrace
import com.overdrive.explainability.domain.EngineType
import com.overdrive.explainability.service.ExplainabilityService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/traces")
class TraceController(private val explainabilityService: ExplainabilityService) {

    @GetMapping("/{id}")
    fun getTrace(@PathVariable id: UUID): ResponseEntity<EngineTrace> =
        explainabilityService.findById(id)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()

    @GetMapping("/opportunity/{opportunityId}")
    fun getTracesForOpportunity(@PathVariable opportunityId: UUID): ResponseEntity<List<EngineTrace>> =
        ResponseEntity.ok(explainabilityService.findByOpportunityId(opportunityId))

    @GetMapping("/opportunity/{opportunityId}/engine/{engineType}")
    fun getTracesByEngineAndOpportunity(
        @PathVariable opportunityId: UUID,
        @PathVariable engineType: EngineType,
    ): ResponseEntity<List<EngineTrace>> =
        ResponseEntity.ok(explainabilityService.findByEngineTypeAndOpportunityId(engineType, opportunityId))
}
