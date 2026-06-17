package com.overdrive.catalog.domain.competitor

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "competitor")
class Competitor(

    @Id val id: UUID = UUID.randomUUID(),
    @Version var version: Long = 0,

    @Column(nullable = false, length = 255) var name: String,

    @Column(precision = 5, scale = 4) var estimatedMargin: BigDecimal? = null,

    /** DIRECT | DISTRIBUTOR | HYBRID | MARKETPLACE */
    @Column(length = 30) var distributionModel: String? = null,

    var numWarehouses: Int? = null,
    var avgTransitDays: Int? = null,

    /** SAME_DAY | NEXT_DAY | TWO_DAY | STANDARD */
    @Column(length = 20) var deliverySpeed: String? = null,

    @Column(columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    var regionalPresence: Array<String> = emptyArray(),

    @Column(nullable = false) val createdAt: Instant = Instant.now(),
    @Column(nullable = false) var updatedAt: Instant = Instant.now()
)
