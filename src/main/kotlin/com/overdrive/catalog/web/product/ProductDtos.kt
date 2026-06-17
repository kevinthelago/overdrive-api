package com.overdrive.catalog.web.product

import com.overdrive.catalog.domain.product.Product
import com.overdrive.common.measure.Dimensions
import com.overdrive.common.measure.Weight
import com.overdrive.common.money.Money
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class ProductCreateRequest(
    @field:NotBlank @field:Size(max = 100)  val sku: String,
    @field:NotBlank @field:Size(max = 255)  val name: String,
    @field:NotBlank @field:Size(max = 100)  val category: String,
    @field:Valid                             val weight: Weight,
    @field:Valid                             val dimensions: Dimensions,
    val hazardous: Boolean            = false,
    val fragile: Boolean              = false,
    val temperatureSensitive: Boolean = false,
    val stackable: Boolean            = true,
    @field:Min(1) val palletQty: Int  = 1,
    @field:Valid  val cost: Money,
    @field:Valid  val msrp: Money,
    @field:PositiveOrZero val marketSize: BigDecimal?          = null,
    @field:PositiveOrZero val orderFrequency: BigDecimal?       = null,
    @field:DecimalMin("0") @field:DecimalMax("10") val logisticsComplexity: BigDecimal? = null,
    val categoryGrowth: BigDecimal?   = null
)

data class ProductUpdateRequest(
    @field:NotBlank @field:Size(max = 100)  val sku: String,
    @field:NotBlank @field:Size(max = 255)  val name: String,
    @field:NotBlank @field:Size(max = 100)  val category: String,
    @field:Valid                             val weight: Weight,
    @field:Valid                             val dimensions: Dimensions,
    val hazardous: Boolean            = false,
    val fragile: Boolean              = false,
    val temperatureSensitive: Boolean = false,
    val stackable: Boolean            = true,
    @field:Min(1) val palletQty: Int  = 1,
    @field:Valid  val cost: Money,
    @field:Valid  val msrp: Money,
    @field:PositiveOrZero val marketSize: BigDecimal?          = null,
    @field:PositiveOrZero val orderFrequency: BigDecimal?       = null,
    @field:DecimalMin("0") @field:DecimalMax("10") val logisticsComplexity: BigDecimal? = null,
    val categoryGrowth: BigDecimal?   = null
)

data class ProductResponse(
    val id: UUID,
    val version: Long,
    val sku: String,
    val name: String,
    val category: String,
    val weight: Weight,
    val dimensions: Dimensions,
    val hazardous: Boolean,
    val fragile: Boolean,
    val temperatureSensitive: Boolean,
    val stackable: Boolean,
    val palletQty: Int,
    val cost: Money,
    val msrp: Money,
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
            weight               = p.weight,
            dimensions           = p.dimensions,
            hazardous            = p.hazardous,
            fragile              = p.fragile,
            temperatureSensitive = p.temperatureSensitive,
            stackable            = p.stackable,
            palletQty            = p.palletQty,
            cost                 = p.cost,
            msrp                 = p.msrp,
            marketSize           = p.marketSize,
            orderFrequency       = p.orderFrequency,
            categoryGrowth       = p.categoryGrowth,
            logisticsComplexity  = p.logisticsComplexity,
            createdAt            = p.createdAt,
            updatedAt            = p.updatedAt
        )
    }
}
