package com.overdrive.catalog.domain.rate

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "category_rate")
class CategoryRate(

    @Id
    @Column(length = 100)
    val category: String,

    /** Net-payment-terms discount rate applied to invoices in this category */
    @Column(nullable = false, precision = 5, scale = 4)
    var paymentRate: BigDecimal,

    /** Expected return fraction (reverse-logistics cost driver) */
    @Column(nullable = false, precision = 5, scale = 4)
    var returnRate: BigDecimal,

    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()
)
