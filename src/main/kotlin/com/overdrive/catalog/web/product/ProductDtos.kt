package com.overdrive.catalog.web.product

import com.overdrive.catalog.domain.product.Product
import com.overdrive.catalog.web.DimensionsDto
import com.overdrive.catalog.web.MoneyDto
import com.overdrive.catalog.web.WeightDto
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class ProductCreateRequest(
    @field:NotBlank @field:Size(max = 100) val sku: String,
    @field:NotBlank @field:Size(max = 255) val name: String,
    @field:NotBlank @field:Size(max = 100) val category: String,
    @field:Valid val weight: WeightDto,
    @field:Valid val dimensions: DimensionsDto,
    val hazardous: Boolean            = false,
    val fragile: Boolean              = false,
    val temperatureSensitive: Boolean = false,
    val stackable: Boolean            = true,
    @field:Min(1) val palletQty: Int  = 1,
    @field:Valid val cost: MoneyDto,
    @field:Valid val msrp: MoneyDto,
    @field:PositiveOrZero val marketSize: BigDecimal?         = null,
    @field:PositiveOrZero val orderFrequency: BigDecimal?      = null,
    val categoryGrowth: BigDecimal?                            = null,
    @field:DecimalMin("0") @field:DecimalMax("10") val logisticsComplexity: BigDecimal? = null
)

data class ProductUpdateRequest(
    @field:NotBlank @field:Size(max = 100) val sku: String,
    @field:NotBlank @field:Size(max = 255) val name: String,
    @field:NotBlank @field:Size(max = 100) val category: String,
    @field:Valid val weight: WeightDto,
    @field:Valid val dimensions: DimensionsDto,
    val hazardous: Boolean            = false,
    val fragile: Boolean              = false,
    val temperatureSensitive: Boolean = false,
    val stackable: Boolean            = true,
    @field:Min(1) val palletQty: Int  = 1,
    @field:Valid val cost: MoneyDto,
    @field:Valid val msrp: MoneyDto,
    @field:PositiveOrZero val marketSize: BigDecimal?         = null,
    @field:PositiveOrZero val orderFrequency: BigDecimal?      = null,
    val categoryGrowth: BigDecimal?                            = null,
    @field:DecimalMin("0") @field:DecimalMax("10") val logisticsComplexity: BigDecimal? = null
)

data class ProductResponse(
    val id: UUID,
    val version: Long,
    val sku: String,
    val name: String,
    val category: String,
    val weight: WeightDto,
    val dimensions: DimensionsDto,
    val hazardous: Boolean,
    val fragile: Boolean,
    val temperatureSensitive: Boolean,
    val stackable: Boolean,
    val palletQty: Int,
    val cost: MoneyDto,
    val msrp: MoneyDto,
    val marketSize: BigDecimal?,
    val orderFrequency: BigDecimal?,
    val categoryGrowth: BigDecimal?,
    val logisticsComplexity: BigDecimal?,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(p: Product) = ProductResponse(
            id                   = p.id,
            version              = p.version,
            sku                  = p.sku,
            name                 = p.name,
            category             = p.category,
            weight               = WeightDto(p.weightLbs),
            dimensions           = DimensionsDto(p.lengthIn, p.widthIn, p.heightIn),
            hazardous            = p.hazardous,
            fragile              = p.fragile,
            temperatureSensitive = p.temperatureSensitive,
            stackable            = p.stackable,
            palletQty            = p.palletQty,
            cost                 = MoneyDto(p.costAmount, p.costCurrency),
            msrp                 = MoneyDto(p.msrpAmount, p.msrpCurrency),
            marketSize           = p.marketSize,
            orderFrequency       = p.orderFrequency,
            categoryGrowth       = p.categoryGrowth,
            logisticsComplexity  = p.logisticsComplexity,
            createdAt            = p.createdAt,
            updatedAt            = p.updatedAt
        )
    }
}
