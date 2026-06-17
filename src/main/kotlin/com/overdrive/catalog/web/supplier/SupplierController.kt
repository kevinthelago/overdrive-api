package com.overdrive.catalog.web.supplier

import com.overdrive.catalog.app.supplier.SupplierService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/catalog/supplier")
class SupplierController(private val service: SupplierService) {

    @GetMapping
    fun list(
        @RequestParam(required = false) search: String?,
        @PageableDefault(size = 20) pageable: Pageable
    ): Page<SupplierResponse> = service.list(search, pageable).map(SupplierResponse::from)

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): SupplierResponse =
        SupplierResponse.from(service.get(id))

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody req: SupplierCreateRequest): SupplierResponse =
        SupplierResponse.from(service.create(req))

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody req: SupplierUpdateRequest
    ): SupplierResponse = SupplierResponse.from(service.update(id, req))

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: UUID) = service.delete(id)
}
