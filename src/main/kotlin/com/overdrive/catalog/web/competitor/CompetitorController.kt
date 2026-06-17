package com.overdrive.catalog.web.competitor

import com.overdrive.catalog.app.competitor.CompetitorService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/catalog/competitor")
class CompetitorController(private val service: CompetitorService) {

    @GetMapping
    fun list(
        @RequestParam(required = false) distributionModel: String?,
        @RequestParam(required = false) search: String?,
        @PageableDefault(size = 20) pageable: Pageable
    ): Page<CompetitorResponse> =
        service.list(distributionModel, search, pageable).map(CompetitorResponse::from)

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): CompetitorResponse =
        CompetitorResponse.from(service.get(id))

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody req: CompetitorCreateRequest): CompetitorResponse =
        CompetitorResponse.from(service.create(req))

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody req: CompetitorUpdateRequest
    ): CompetitorResponse = CompetitorResponse.from(service.update(id, req))

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: UUID) = service.delete(id)
}
