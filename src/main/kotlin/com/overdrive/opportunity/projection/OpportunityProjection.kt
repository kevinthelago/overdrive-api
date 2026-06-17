package com.overdrive.opportunity.projection

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Persisted snapshot of an opportunity score computation.
 * Written by [com.overdrive.opportunity.service.OpportunityEngineService] after every
 * batch recompute. Stale projections are replaced (delete + insert) per productId + scenarioId.
 * A null [scenarioId] row is the baseline; non-null rows are scenario snapshots.
 */
@Entity
@Table(name = "opportunity_projection")
class OpportunityProjection(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, columnDefinition = "uuid")
    val productId: UUID,

    @Column(nullable = false, length = 100)
    val category: String,

    @Column(nullable = false, precision = 12, scale = 6)
    val score: BigDecimal,

    @Column(precision = 10, scale = 6)
    val savingsPct: BigDecimal?,

    @Column(precision = 20, scale = 2)
    val marketSizeUsd: BigDecimal?,

    @Column(precision = 10, scale = 6)
    val orderFrequency: BigDecimal?,

    @Column(precision = 10, scale = 6)
    val categoryGrowthPct: BigDecimal?,

    @Column(precision = 10, scale = 6)
    val supplierAvailability: BigDecimal?,

    @Column(precision = 10, scale = 6)
    val logisticsComplexity: BigDecimal?,

    @Column(columnDefinition = "jsonb")
    val regionBreakdownJson: String?,

    @Column(nullable = true, length = 64)
    val zeroReason: String?,

    @Column
    val scenarioId: UUID?,

    @Column(nullable = false)
    val computedAt: Instant = Instant.now(),
)
