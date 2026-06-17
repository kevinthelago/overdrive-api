package com.overdrive.catalog.web.warehouse

import com.overdrive.catalog.app.warehouse.WarehouseService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/catalog/warehouse")
class WarehouseController(private val service: WarehouseService) {

    @GetMapping
    fun list(
        @RequestParam(required = false) type: String?,
        @RequestParam(required = false) state: String?,
        @RequestParam(required = false) search: String?,
        @PageableDefault(size = 20) pageable: Pageable
    ): Page<WarehouseResponse> =
        service.list(type, state, search, pageable).map(WarehouseResponse::from)

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): WarehouseResponse =
        WarehouseResponse.from(service.get(id))

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody req: WarehouseCreateRequest): WarehouseResponse =
        WarehouseResponse.from(service.create(req))

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody req: WarehouseUpdateRequest
    ): WarehouseResponse = WarehouseResponse.from(service.update(id, req))

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: UUID) = service.delete(id)
}
