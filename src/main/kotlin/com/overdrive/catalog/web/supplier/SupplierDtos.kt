package com.overdrive.catalog.web.supplier

import com.overdrive.catalog.domain.supplier.Supplier
import com.overdrive.catalog.web.MoneyDto
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class SupplierCreateRequest(
    @field:NotBlank @field:Size(max = 255) val name: String,
    @field:Min(1) val moq: Int = 1,
    @field:Min(0) val leadTimeDays: Int,
    @field:Valid val cost: MoneyDto? = null,
    @field:Size(max = 10) val originZip: String? = null,
    @field:DecimalMin("-90")  @field:DecimalMax("90")  val originLat: Double? = null,
    @field:DecimalMin("-180") @field:DecimalMax("180") val originLng: Double? = null,
    @field:DecimalMin("0") @field:DecimalMax("1") val volumeDiscountPct: BigDecimal = BigDecimal.ZERO,
    @field:DecimalMin("0") @field:DecimalMax("1") val reliabilityScore: BigDecimal  = BigDecimal.ONE
)

data class SupplierUpdateRequest(
    @field:NotBlank @field:Size(max = 255) val name: String,
    @field:Min(1) val moq: Int = 1,
    @field:Min(0) val leadTimeDays: Int,
    @field:Valid val cost: MoneyDto? = null,
    @field:Size(max = 10) val originZip: String? = null,
    @field:DecimalMin("-90")  @field:DecimalMax("90")  val originLat: Double? = null,
    @field:DecimalMin("-180") @field:DecimalMax("180") val originLng: Double? = null,
    @field:DecimalMin("0") @field:DecimalMax("1") val volumeDiscountPct: BigDecimal = BigDecimal.ZERO,
    @field:DecimalMin("0") @field:DecimalMax("1") val reliabilityScore: BigDecimal  = BigDecimal.ONE
)

data class SupplierResponse(
    val id: UUID,
    val version: Long,
    val name: String,
    val moq: Int,
    val leadTimeDays: Int,
    val cost: MoneyDto?,
    val originZip: String?,
    val originLat: Double?,
    val originLng: Double?,
    val volumeDiscountPct: BigDecimal,
    val reliabilityScore: BigDecimal,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(s: Supplier) = SupplierResponse(
            id                = s.id,
            version           = s.version,
            name              = s.name,
            moq               = s.moq,
            leadTimeDays      = s.leadTimeDays,
            cost              = s.costAmount?.let { MoneyDto(it, s.costCurrency) },
            originZip         = s.originZip,
            originLat         = s.originLat,
            originLng         = s.originLng,
            volumeDiscountPct = s.volumeDiscountPct,
            reliabilityScore  = s.reliabilityScore,
            createdAt         = s.createdAt,
            updatedAt         = s.updatedAt
        )
    }
}
