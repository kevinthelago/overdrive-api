package com.overdrive.catalog.domain.product

import com.overdrive.common.measure.Dimensions
import com.overdrive.common.measure.Weight
import com.overdrive.common.money.Money
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.Currency
import java.util.UUID

@Entity
@Table(name = "product")
class Product(

    @Id
    val id: UUID = UUID.randomUUID(),

    @Version
    var version: Long = 0,

    @Column(nullable = false, length = 100) var sku: String,
    @Column(nullable = false, length = 255) var name: String,
    @Column(nullable = false, length = 100) var category: String,

    // Weight stored as pounds (Weight.pounds); Weight always normalises to lbs internally
    @Column(name = "weight_lbs", nullable = false, precision = 10, scale = 4)
    var weightLbs: BigDecimal,

    // Dimensions in inches
    @Column(name = "length_in", nullable = false, precision = 10, scale = 2) var lengthIn: BigDecimal,
    @Column(name = "width_in",  nullable = false, precision = 10, scale = 2) var widthIn: BigDecimal,
    @Column(name = "height_in", nullable = false, precision = 10, scale = 2) var heightIn: BigDecimal,

    @Column(nullable = false) var hazardous: Boolean = false,
    @Column(nullable = false) var fragile: Boolean = false,
    @Column(nullable = false) var temperatureSensitive: Boolean = false,
    @Column(nullable = false) var stackable: Boolean = true,
    @Column(nullable = false) var palletQty: Int = 1,

    // Cost (Money — amount + ISO-4217 currency code)
    @Column(name = "cost_amount",   nullable = false, precision = 18, scale = 4) var costAmount: BigDecimal,
    @Column(name = "cost_currency", nullable = false, length = 3)                var costCurrency: String = "USD",

    // MSRP (Money)
    @Column(name = "msrp_amount",   nullable = false, precision = 18, scale = 4) var msrpAmount: BigDecimal,
    @Column(name = "msrp_currency", nullable = false, length = 3)                var msrpCurrency: String = "USD",

    // Demand / market-intelligence fields
    @Column(precision = 18, scale = 2) var marketSize: BigDecimal? = null,
    @Column(precision = 10, scale = 4) var orderFrequency: BigDecimal? = null,
    @Column(precision = 10, scale = 4) var categoryGrowth: BigDecimal? = null,
    @Column(precision = 5,  scale = 2) var logisticsComplexity: BigDecimal? = null,

    @Column(nullable = false) val createdAt: Instant = Instant.now(),
    @Column(nullable = false) var updatedAt: Instant = Instant.now()
) {
    fun weight(): Weight = Weight.ofPounds(weightLbs)
    fun cost(): Money   = Money.of(costAmount, Currency.getInstance(costCurrency))
    fun msrp(): Money   = Money.of(msrpAmount, Currency.getInstance(msrpCurrency))
    fun dimensions(): Dimensions = Dimensions(lengthIn, widthIn, heightIn)
}
