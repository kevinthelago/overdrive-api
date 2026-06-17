package com.overdrive.catalog.web.product

import com.overdrive.catalog.app.product.ProductService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/catalog/product")
class ProductController(private val service: ProductService) {

    @GetMapping
    fun list(
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) search: String?,
        @PageableDefault(size = 20) pageable: Pageable
    ): Page<ProductResponse> = service.list(category, search, pageable).map(ProductResponse::from)

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): ProductResponse =
        ProductResponse.from(service.get(id))

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody req: ProductCreateRequest): ProductResponse =
        ProductResponse.from(service.create(req))

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody req: ProductUpdateRequest
    ): ProductResponse = ProductResponse.from(service.update(id, req))

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: UUID) = service.delete(id)
}
