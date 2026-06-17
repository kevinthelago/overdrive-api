package com.overdrive.catalog.web.warehouse

import com.overdrive.catalog.domain.warehouse.Warehouse
import com.overdrive.common.money.Money
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class WarehouseCreateRequest(
    @field:NotBlank @field:Size(max = 255) val name: String,
    @field:NotBlank @field:Size(max = 20)  val type: String,
    @field:NotBlank @field:Size(min = 2, max = 2) val state: String,
    @field:NotBlank @field:Size(max = 10)  val zip: String,
    @field:DecimalMin("-90")  @field:DecimalMax("90")  val lat: Double,
    @field:DecimalMin("-180") @field:DecimalMax("180") val lng: Double,
    @field:Positive val ceilingHeightFt: BigDecimal? = null,
    @field:Min(1)   val palletCapacity: Int,
    @field:Valid val pickFee: Money,
    @field:Valid val receivingFee: Money,
    @field:Valid val storageFeePerPallet: Money
)

data class WarehouseUpdateRequest(
    @field:NotBlank @field:Size(max = 255) val name: String,
    @field:NotBlank @field:Size(max = 20)  val type: String,
    @field:NotBlank @field:Size(min = 2, max = 2) val state: String,
    @field:NotBlank @field:Size(max = 10)  val zip: String,
    @field:DecimalMin("-90")  @field:DecimalMax("90")  val lat: Double,
    @field:DecimalMin("-180") @field:DecimalMax("180") val lng: Double,
    @field:Positive val ceilingHeightFt: BigDecimal? = null,
    @field:Min(1)   val palletCapacity: Int,
    @field:Valid val pickFee: Money,
    @field:Valid val receivingFee: Money,
    @field:Valid val storageFeePerPallet: Money
)

data class WarehouseResponse(
    val id: UUID,
    val version: Long,
    val name: String,
    val type: String,
    val state: String,
    val zip: String,
    val lat: Double,
    val lng: Double,
    val ceilingHeightFt: BigDecimal?,
    val palletCapacity: Int,
    val pickFee: Money,
    val receivingFee: Money,
    val storageFeePerPallet: Money,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(w: Warehouse) = WarehouseResponse(
            id                  = w.id,
            version             = w.version,
            name                = w.name,
            type                = w.type,
            state               = w.state,
            zip                 = w.zip,
            lat                 = w.lat,
            lng                 = w.lng,
            ceilingHeightFt     = w.ceilingHeightFt,
            palletCapacity      = w.palletCapacity,
            pickFee             = w.pickFee,
            receivingFee        = w.receivingFee,
            storageFeePerPallet = w.storageFeePerPallet,
            createdAt           = w.createdAt,
            updatedAt           = w.updatedAt
        )
    }
}
