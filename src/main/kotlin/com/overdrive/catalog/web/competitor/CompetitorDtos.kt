package com.overdrive.catalog.web.competitor

import com.overdrive.catalog.domain.competitor.Competitor
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class CompetitorCreateRequest(
    @field:NotBlank @field:Size(max = 255) val name: String,
    @field:DecimalMin("0") @field:DecimalMax("1") val estimatedMargin: BigDecimal? = null,
    @field:Size(max = 30) val distributionModel: String? = null,
    @field:Min(0) val numWarehouses: Int? = null,
    @field:Min(0) val avgTransitDays: Int? = null,
    @field:Size(max = 20) val deliverySpeed: String? = null,
    val regionalPresence: Array<String> = emptyArray()
)

data class CompetitorUpdateRequest(
    @field:NotBlank @field:Size(max = 255) val name: String,
    @field:DecimalMin("0") @field:DecimalMax("1") val estimatedMargin: BigDecimal? = null,
    @field:Size(max = 30) val distributionModel: String? = null,
    @field:Min(0) val numWarehouses: Int? = null,
    @field:Min(0) val avgTransitDays: Int? = null,
    @field:Size(max = 20) val deliverySpeed: String? = null,
    val regionalPresence: Array<String> = emptyArray()
)

data class CompetitorResponse(
    val id: UUID,
    val version: Long,
    val name: String,
    val estimatedMargin: BigDecimal?,
    val distributionModel: String?,
    val numWarehouses: Int?,
    val avgTransitDays: Int?,
    val deliverySpeed: String?,
    val regionalPresence: Array<String>,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(c: Competitor) = CompetitorResponse(
            id                = c.id,
            version           = c.version,
            name              = c.name,
            estimatedMargin   = c.estimatedMargin,
            distributionModel = c.distributionModel,
            numWarehouses     = c.numWarehouses,
            avgTransitDays    = c.avgTransitDays,
            deliverySpeed     = c.deliverySpeed,
            regionalPresence  = c.regionalPresence,
            createdAt         = c.createdAt,
            updatedAt         = c.updatedAt
        )
    }
}
