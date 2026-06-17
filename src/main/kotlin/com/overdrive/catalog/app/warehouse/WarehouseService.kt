package com.overdrive.catalog.app.warehouse

import com.overdrive.catalog.domain.warehouse.Warehouse
import com.overdrive.catalog.domain.warehouse.WarehouseRepository
import com.overdrive.catalog.web.warehouse.WarehouseCreateRequest
import com.overdrive.catalog.web.warehouse.WarehouseUpdateRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class WarehouseService(private val warehouses: WarehouseRepository) {

    @Transactional(readOnly = true)
    fun list(type: String?, state: String?, search: String?, pageable: Pageable): Page<Warehouse> =
        warehouses.search(type, state, search, pageable)

    @Transactional(readOnly = true)
    fun get(id: UUID): Warehouse = warehouses.findById(id)
        .orElseThrow { NoSuchElementException("Warehouse $id not found") }

    fun create(req: WarehouseCreateRequest): Warehouse = warehouses.save(
        Warehouse(
            name                       = req.name,
            type                       = req.type,
            state                      = req.state,
            zip                        = req.zip,
            lat                        = req.lat,
            lng                        = req.lng,
            ceilingHeightFt            = req.ceilingHeightFt,
            palletCapacity             = req.palletCapacity,
            pickFeeAmount              = req.pickFee.amount,
            pickFeeCurrency            = req.pickFee.currency,
            receivingFeeAmount         = req.receivingFee.amount,
            receivingFeeCurrency       = req.receivingFee.currency,
            storageFeePerPalletAmount  = req.storageFeePerPallet.amount,
            storageFeePerPalletCurrency = req.storageFeePerPallet.currency
        )
    )

    fun update(id: UUID, req: WarehouseUpdateRequest): Warehouse {
        val w = get(id)
        w.name                        = req.name
        w.type                        = req.type
        w.state                       = req.state
        w.zip                         = req.zip
        w.lat                         = req.lat
        w.lng                         = req.lng
        w.ceilingHeightFt             = req.ceilingHeightFt
        w.palletCapacity              = req.palletCapacity
        w.pickFeeAmount               = req.pickFee.amount
        w.pickFeeCurrency             = req.pickFee.currency
        w.receivingFeeAmount          = req.receivingFee.amount
        w.receivingFeeCurrency        = req.receivingFee.currency
        w.storageFeePerPalletAmount   = req.storageFeePerPallet.amount
        w.storageFeePerPalletCurrency = req.storageFeePerPallet.currency
        w.updatedAt                   = Instant.now()
        return warehouses.save(w)
    }

    fun delete(id: UUID) = warehouses.delete(get(id))
}
