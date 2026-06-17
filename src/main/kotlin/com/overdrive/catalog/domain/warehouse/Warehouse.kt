package com.overdrive.catalog.domain.warehouse

import com.overdrive.common.money.Money
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.Currency
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
    @Column(nullable = false) var lat: Double,
    @Column(nullable = false) var lng: Double,

    @Column(precision = 6, scale = 2) var ceilingHeightFt: BigDecimal? = null,
    @Column(nullable = false) var palletCapacity: Int,

    // Pick fee (Money)
    @Column(name = "pick_fee_amount",   nullable = false, precision = 18, scale = 4) var pickFeeAmount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "pick_fee_currency", nullable = false, length = 3)                var pickFeeCurrency: String = "USD",

    // Receiving fee (Money)
    @Column(name = "receiving_fee_amount",   nullable = false, precision = 18, scale = 4) var receivingFeeAmount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "receiving_fee_currency", nullable = false, length = 3)                var receivingFeeCurrency: String = "USD",

    // Storage fee per pallet (Money)
    @Column(name = "storage_fee_per_pallet_amount",   nullable = false, precision = 18, scale = 4) var storageFeePerPalletAmount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "storage_fee_per_pallet_currency", nullable = false, length = 3)                var storageFeePerPalletCurrency: String = "USD",

    @Column(nullable = false) val createdAt: Instant = Instant.now(),
    @Column(nullable = false) var updatedAt: Instant = Instant.now()
) {
    fun pickFee(): Money             = Money.of(pickFeeAmount,             Currency.getInstance(pickFeeCurrency))
    fun receivingFee(): Money        = Money.of(receivingFeeAmount,        Currency.getInstance(receivingFeeCurrency))
    fun storageFeePerPallet(): Money = Money.of(storageFeePerPalletAmount, Currency.getInstance(storageFeePerPalletCurrency))
}
