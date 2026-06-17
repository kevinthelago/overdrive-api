package com.overdrive.catalog.app.supplier

import com.overdrive.catalog.domain.supplier.Supplier
import com.overdrive.catalog.domain.supplier.SupplierRepository
import com.overdrive.catalog.web.supplier.SupplierCreateRequest
import com.overdrive.catalog.web.supplier.SupplierUpdateRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class SupplierService(private val suppliers: SupplierRepository) {

    @Transactional(readOnly = true)
    fun list(search: String?, pageable: Pageable): Page<Supplier> =
        suppliers.search(search, pageable)

    @Transactional(readOnly = true)
    fun get(id: UUID): Supplier = suppliers.findById(id)
        .orElseThrow { NoSuchElementException("Supplier $id not found") }

    fun create(req: SupplierCreateRequest): Supplier = suppliers.save(
        Supplier(
            name               = req.name,
            moq                = req.moq,
            leadTimeDays       = req.leadTimeDays,
            cost               = req.cost,
            originZip          = req.originZip,
            originLat          = req.originLat,
            originLng          = req.originLng,
            volumeDiscountPct  = req.volumeDiscountPct,
            reliabilityScore   = req.reliabilityScore
        )
    )

    fun update(id: UUID, req: SupplierUpdateRequest): Supplier {
        val supplier = get(id)
        supplier.name              = req.name
        supplier.moq               = req.moq
        supplier.leadTimeDays      = req.leadTimeDays
        supplier.cost              = req.cost
        supplier.originZip         = req.originZip
        supplier.originLat         = req.originLat
        supplier.originLng         = req.originLng
        supplier.volumeDiscountPct = req.volumeDiscountPct
        supplier.reliabilityScore  = req.reliabilityScore
        supplier.updatedAt         = Instant.now()
        return suppliers.save(supplier)
    }

    fun delete(id: UUID) {
        val supplier = get(id)
        suppliers.delete(supplier)
    }
}
