package com.overdrive.catalog.domain.supplier

import com.overdrive.common.money.Money
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "supplier")
class Supplier(

    @Id val id: UUID = UUID.randomUUID(),

    @Version var version: Long = 0,

    @Column(nullable = false, length = 255)
    var name: String,

    @Column(nullable = false) var moq: Int = 1,
    @Column(nullable = false) var leadTimeDays: Int,

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "amount",   column = Column(name = "cost_amount",   precision = 18, scale = 4)),
        AttributeOverride(name = "currency", column = Column(name = "cost_currency", length = 3))
    )
    var cost: Money? = null,

    // Shipping origin geo
    @Column(length = 10) var originZip: String? = null,
    var originLat: Double? = null,
    var originLng: Double? = null,

    @Column(nullable = false, precision = 5, scale = 4)
    var volumeDiscountPct: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 5, scale = 4)
    var reliabilityScore: BigDecimal = BigDecimal.ONE,

    @Column(nullable = false) val createdAt: Instant = Instant.now(),
    @Column(nullable = false) var updatedAt: Instant = Instant.now()
)
