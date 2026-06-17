package com.overdrive.catalog.web.carrier

import com.overdrive.catalog.domain.carrier.Carrier
import com.overdrive.catalog.domain.carrier.CarrierLane
import com.overdrive.catalog.web.MoneyDto
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class CarrierCreateRequest(
    @field:NotBlank @field:Size(max = 255) val name: String,
    @field:Size(max = 10) val scac: String? = null,
    @field:NotBlank @field:Size(max = 20) val pricingModel: String,
    @field:Valid val liftgateSurcharge: MoneyDto,
    @field:Valid val residentialSurcharge: MoneyDto,
    @field:Positive val dimFactor: BigDecimal? = null,
    @field:DecimalMin("0") @field:DecimalMax("1") val fuelSurchargePct: BigDecimal = BigDecimal.ZERO
)

data class CarrierUpdateRequest(
    @field:NotBlank @field:Size(max = 255) val name: String,
    @field:Size(max = 10) val scac: String? = null,
    @field:NotBlank @field:Size(max = 20) val pricingModel: String,
    @field:Valid val liftgateSurcharge: MoneyDto,
    @field:Valid val residentialSurcharge: MoneyDto,
    @field:Positive val dimFactor: BigDecimal? = null,
    @field:DecimalMin("0") @field:DecimalMax("1") val fuelSurchargePct: BigDecimal = BigDecimal.ZERO
)

data class CarrierResponse(
    val id: UUID,
    val version: Long,
    val name: String,
    val scac: String?,
    val pricingModel: String,
    val liftgateSurcharge: MoneyDto,
    val residentialSurcharge: MoneyDto,
    val dimFactor: BigDecimal?,
    val fuelSurchargePct: BigDecimal,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(c: Carrier) = CarrierResponse(
            id                   = c.id,
            version              = c.version,
            name                 = c.name,
            scac                 = c.scac,
            pricingModel         = c.pricingModel,
            liftgateSurcharge    = MoneyDto(c.liftgateSurchargeAmount,    c.liftgateSurchargeCurrency),
            residentialSurcharge = MoneyDto(c.residentialSurchargeAmount, c.residentialSurchargeCurrency),
            dimFactor            = c.dimFactor,
            fuelSurchargePct     = c.fuelSurchargePct,
            createdAt            = c.createdAt,
            updatedAt            = c.updatedAt
        )
    }
}

data class CarrierLaneCreateRequest(
    @field:NotBlank @field:Size(max = 20) val serviceLevel: String,
    @field:Size(max = 10) val originZone: String? = null,
    @field:Size(max = 10) val destZone: String? = null,
    @field:Size(max = 5)  val originZipPrefix: String? = null,
    @field:Size(max = 5)  val destZipPrefix: String? = null,
    @field:Min(0) val transitDays: Int,
    @field:Valid  val baseRate: MoneyDto,
    @field:PositiveOrZero val perLbRate: BigDecimal? = null,
    @field:PositiveOrZero val perCwtRate: BigDecimal? = null,
    @field:Valid  val minCharge: MoneyDto? = null
)

data class CarrierLaneResponse(
    val id: UUID,
    val carrierId: UUID,
    val serviceLevel: String,
    val originZone: String?,
    val destZone: String?,
    val originZipPrefix: String?,
    val destZipPrefix: String?,
    val transitDays: Int,
    val baseRate: MoneyDto,
    val perLbRate: BigDecimal?,
    val perCwtRate: BigDecimal?,
    val minCharge: MoneyDto?,
    val createdAt: Instant
) {
    companion object {
        fun from(l: CarrierLane) = CarrierLaneResponse(
            id              = l.id,
            carrierId       = l.carrier.id,
            serviceLevel    = l.serviceLevel,
            originZone      = l.originZone,
            destZone        = l.destZone,
            originZipPrefix = l.originZipPrefix,
            destZipPrefix   = l.destZipPrefix,
            transitDays     = l.transitDays,
            baseRate        = MoneyDto(l.baseRateAmount, l.baseRateCurrency),
            perLbRate       = l.perLbRate,
            perCwtRate      = l.perCwtRate,
            minCharge       = l.minChargeAmount?.let { MoneyDto(it, l.minChargeCurrency) },
            createdAt       = l.createdAt
        )
    }
}
