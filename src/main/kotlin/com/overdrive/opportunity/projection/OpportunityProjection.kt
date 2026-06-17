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
 * batch recompute. Stale projections are replaced in-place (upsert by productId + scenarioId).
 */
@Entity
@Table(name = "opportunity_projection")
class OpportunityProjection(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val productId: Long,

    @Column(nullable = false)
    val categoryId: Long,

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

    /** JSON array of { region, zip, score, savingsPct } objects. */
    @Column(columnDefinition = "jsonb")
    val regionBreakdownJson: String?,

    @Column(nullable = false)
    val zeroReason: String?,

    /** Null = baseline; non-null = scenario snapshot. */
    @Column
    val scenarioId: UUID?,

    @Column(nullable = false)
    val computedAt: Instant = Instant.now(),
)
