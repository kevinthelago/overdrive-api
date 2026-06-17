package com.overdrive.catalog.web.carrier

import com.overdrive.catalog.app.carrier.CarrierService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/catalog/carrier")
class CarrierController(private val service: CarrierService) {

    @GetMapping
    fun list(
        @RequestParam(required = false) pricingModel: String?,
        @RequestParam(required = false) search: String?,
        @PageableDefault(size = 20) pageable: Pageable
    ): Page<CarrierResponse> =
        service.list(pricingModel, search, pageable).map(CarrierResponse::from)

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): CarrierResponse =
        CarrierResponse.from(service.get(id))

    @GetMapping("/{id}/lanes")
    fun getLanes(@PathVariable id: UUID): List<CarrierLaneResponse> =
        service.getLanes(id).map(CarrierLaneResponse::from)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody req: CarrierCreateRequest): CarrierResponse =
        CarrierResponse.from(service.create(req))

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody req: CarrierUpdateRequest
    ): CarrierResponse = CarrierResponse.from(service.update(id, req))

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: UUID) = service.delete(id)

    @PostMapping("/{id}/lanes")
    @ResponseStatus(HttpStatus.CREATED)
    fun addLane(
        @PathVariable id: UUID,
        @Valid @RequestBody req: CarrierLaneCreateRequest
    ): CarrierLaneResponse = CarrierLaneResponse.from(service.addLane(id, req))

    @DeleteMapping("/{carrierId}/lanes/{laneId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteLane(
        @PathVariable carrierId: UUID,
        @PathVariable laneId: UUID
    ) = service.deleteLane(laneId)
}
