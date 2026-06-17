package com.overdrive.explainability.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "engine_traces")
class EngineTrace(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Enumerated(EnumType.STRING)
    @Column(name = "engine_type", nullable = false, length = 50)
    val engineType: EngineType,

    @Column(name = "opportunity_id", nullable = false)
    val opportunityId: UUID,

    @Column(name = "scenario_context_id")
    val scenarioContextId: UUID? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "inputs", nullable = false)
    var inputs: String = "{}",

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "outputs", nullable = false)
    var outputs: String = "{}",

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "steps", nullable = false)
    var steps: String = "[]",

    @Column(name = "computed_at", nullable = false)
    val computedAt: Instant = Instant.now()
)
