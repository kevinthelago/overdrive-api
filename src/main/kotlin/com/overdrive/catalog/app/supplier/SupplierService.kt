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
            name              = req.name,
            moq               = req.moq,
            leadTimeDays      = req.leadTimeDays,
            costAmount        = req.cost?.amount,
            costCurrency      = req.cost?.currency ?: "USD",
            originZip         = req.originZip,
            originLat         = req.originLat,
            originLng         = req.originLng,
            volumeDiscountPct = req.volumeDiscountPct,
            reliabilityScore  = req.reliabilityScore
        )
    )

    fun update(id: UUID, req: SupplierUpdateRequest): Supplier {
        val s = get(id)
        s.name              = req.name
        s.moq               = req.moq
        s.leadTimeDays      = req.leadTimeDays
        s.costAmount        = req.cost?.amount
        s.costCurrency      = req.cost?.currency ?: "USD"
        s.originZip         = req.originZip
        s.originLat         = req.originLat
        s.originLng         = req.originLng
        s.volumeDiscountPct = req.volumeDiscountPct
        s.reliabilityScore  = req.reliabilityScore
        s.updatedAt         = Instant.now()
        return suppliers.save(s)
    }

    fun delete(id: UUID) = suppliers.delete(get(id))
}
