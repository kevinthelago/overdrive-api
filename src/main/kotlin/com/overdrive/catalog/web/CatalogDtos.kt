package com.overdrive.catalog.web

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal

/** JSON-serializable wrapper for Money (amount + ISO-4217 currency code). */
data class MoneyDto(
    @field:DecimalMin("0") val amount: BigDecimal,
    @field:NotBlank @field:Size(min = 3, max = 3) val currency: String = "USD"
)

/** JSON-serializable wrapper for Weight (in pounds, matching Weight.pounds). */
data class WeightDto(
    @field:Positive val pounds: BigDecimal
)

/** JSON-serializable wrapper for Dimensions (in inches, matching Dimensions fields). */
data class DimensionsDto(
    @field:Positive val lengthIn: BigDecimal,
    @field:Positive val widthIn: BigDecimal,
    @field:Positive val heightIn: BigDecimal
)
