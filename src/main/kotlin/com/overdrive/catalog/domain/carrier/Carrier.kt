package com.overdrive.catalog.domain.carrier

import com.overdrive.common.money.Money
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "carrier")
class Carrier(

    @Id val id: UUID = UUID.randomUUID(),

    @Version var version: Long = 0,

    @Column(nullable = false, length = 255) var name: String,
    @Column(length = 10, unique = true)     var scac: String? = null,

    /** PARCEL | LTL | FTL */
    @Column(nullable = false, length = 20) var pricingModel: String,

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "amount",   column = Column(name = "liftgate_surcharge_amount",   nullable = false, precision = 18, scale = 4)),
        AttributeOverride(name = "currency", column = Column(name = "liftgate_surcharge_currency", nullable = false, length = 3))
    )
    var liftgateSurcharge: Money,

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "amount",   column = Column(name = "residential_surcharge_amount",   nullable = false, precision = 18, scale = 4)),
        AttributeOverride(name = "currency", column = Column(name = "residential_surcharge_currency", nullable = false, length = 3))
    )
    var residentialSurcharge: Money,

    /** Dimensional weight divisor in in³/lb (e.g. 139 for domestic ground parcel) */
    @Column(precision = 10, scale = 4) var dimFactor: BigDecimal? = null,

    @Column(nullable = false, precision = 5, scale = 4)
    var fuelSurchargePct: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false) val createdAt: Instant = Instant.now(),
    @Column(nullable = false) var updatedAt: Instant = Instant.now()
)
