package com.overdrive.catalog.domain.carrier

import com.overdrive.common.money.Money
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "carrier_lane",
    uniqueConstraints = [UniqueConstraint(columnNames = ["carrier_id", "service_level", "origin_zone", "dest_zone"])]
)
class CarrierLane(

    @Id val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrier_id", nullable = false)
    val carrier: Carrier,

    /** GROUND | EXPRESS | OVERNIGHT | LTL | FTL | ECONOMY */
    @Column(nullable = false, length = 20) var serviceLevel: String,

    @Column(length = 10) var originZone: String? = null,
    @Column(length = 10) var destZone: String? = null,
    @Column(length = 5)  var originZipPrefix: String? = null,
    @Column(length = 5)  var destZipPrefix: String? = null,

    @Column(nullable = false) var transitDays: Int,

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "amount",   column = Column(name = "base_rate_amount",   nullable = false, precision = 18, scale = 4)),
        AttributeOverride(name = "currency", column = Column(name = "base_rate_currency", nullable = false, length = 3))
    )
    var baseRate: Money,

    /** Per-lb incremental rate (parcel / LTL billable weight) */
    @Column(precision = 10, scale = 6) var perLbRate: BigDecimal? = null,

    /** Per-cwt rate for LTL class pricing */
    @Column(precision = 10, scale = 4) var perCwtRate: BigDecimal? = null,

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "amount",   column = Column(name = "min_charge_amount",   precision = 18, scale = 4)),
        AttributeOverride(name = "currency", column = Column(name = "min_charge_currency", length = 3))
    )
    var minCharge: Money? = null,

    @Column(nullable = false) val createdAt: Instant = Instant.now()
)
