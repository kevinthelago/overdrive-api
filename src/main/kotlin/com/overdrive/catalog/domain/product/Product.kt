package com.overdrive.catalog.domain.product

import com.overdrive.common.geo.Geo
import com.overdrive.common.measure.Dimensions
import com.overdrive.common.measure.Weight
import com.overdrive.common.money.Money
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "product")
class Product(

    @Id
    val id: UUID = UUID.randomUUID(),

    @Version
    var version: Long = 0,

    @Column(nullable = false, length = 100)
    var sku: String,

    @Column(nullable = false, length = 255)
    var name: String,

    @Column(nullable = false, length = 100)
    var category: String,

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "value",  column = Column(name = "weight_value", nullable = false)),
        AttributeOverride(name = "unit",   column = Column(name = "weight_unit",  nullable = false, length = 10))
    )
    var weight: Weight,

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "lengthIn", column = Column(name = "length_in", nullable = false)),
        AttributeOverride(name = "widthIn",  column = Column(name = "width_in",  nullable = false)),
        AttributeOverride(name = "heightIn", column = Column(name = "height_in", nullable = false))
    )
    var dimensions: Dimensions,

    @Column(nullable = false) var hazardous: Boolean = false,
    @Column(nullable = false) var fragile: Boolean = false,
    @Column(nullable = false) var temperatureSensitive: Boolean = false,
    @Column(nullable = false) var stackable: Boolean = true,
    @Column(nullable = false) var palletQty: Int = 1,

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "amount",   column = Column(name = "cost_amount",   nullable = false, precision = 18, scale = 4)),
        AttributeOverride(name = "currency", column = Column(name = "cost_currency", nullable = false, length = 3))
    )
    var cost: Money,

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "amount",   column = Column(name = "msrp_amount",   nullable = false, precision = 18, scale = 4)),
        AttributeOverride(name = "currency", column = Column(name = "msrp_currency", nullable = false, length = 3))
    )
    var msrp: Money,

    // Demand / market-intelligence fields
    @Column(precision = 18, scale = 2) var marketSize: BigDecimal? = null,
    @Column(precision = 10, scale = 4) var orderFrequency: BigDecimal? = null,
    @Column(precision = 10, scale = 4) var categoryGrowth: BigDecimal? = null,
    @Column(precision = 5,  scale = 2) var logisticsComplexity: BigDecimal? = null,

    @Column(nullable = false) val createdAt: Instant = Instant.now(),
    @Column(nullable = false) var updatedAt: Instant = Instant.now()
)
