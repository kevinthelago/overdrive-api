package com.overdrive.catalog.domain.warehouse

import com.overdrive.common.money.Money
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "warehouse")
class Warehouse(

    @Id val id: UUID = UUID.randomUUID(),

    @Version var version: Long = 0,

    @Column(nullable = false, length = 255) var name: String,

    /** DC | FC | CROSS_DOCK | PL3 */
    @Column(nullable = false, length = 20) var type: String,

    @Column(nullable = false, length = 2)  var state: String,
    @Column(nullable = false, length = 10) var zip: String,
    @Column(nullable = false)              var lat: Double,
    @Column(nullable = false)              var lng: Double,

    @Column(precision = 6, scale = 2) var ceilingHeightFt: BigDecimal? = null,
    @Column(nullable = false)         var palletCapacity: Int,

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "amount",   column = Column(name = "pick_fee_amount",   nullable = false, precision = 18, scale = 4)),
        AttributeOverride(name = "currency", column = Column(name = "pick_fee_currency", nullable = false, length = 3))
    )
    var pickFee: Money,

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "amount",   column = Column(name = "receiving_fee_amount",   nullable = false, precision = 18, scale = 4)),
        AttributeOverride(name = "currency", column = Column(name = "receiving_fee_currency", nullable = false, length = 3))
    )
    var receivingFee: Money,

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "amount",   column = Column(name = "storage_fee_per_pallet_amount",   nullable = false, precision = 18, scale = 4)),
        AttributeOverride(name = "currency", column = Column(name = "storage_fee_per_pallet_currency", nullable = false, length = 3))
    )
    var storageFeePerPallet: Money,

    @Column(nullable = false) val createdAt: Instant = Instant.now(),
    @Column(nullable = false) var updatedAt: Instant = Instant.now()
)
