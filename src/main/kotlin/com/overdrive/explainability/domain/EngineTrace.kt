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
class EngineTrace {

    @Id
    var id: UUID = UUID.randomUUID()

    @Enumerated(EnumType.STRING)
    @Column(name = "engine_type", nullable = false, length = 50)
    var engineType: EngineType = EngineType.COST

    @Column(name = "opportunity_id", nullable = false)
    var opportunityId: UUID = UUID.randomUUID()

    @Column(name = "scenario_context_id")
    var scenarioContextId: UUID? = null

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "inputs", nullable = false, columnDefinition = "jsonb")
    var inputs: String = "{}"

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "outputs", nullable = false, columnDefinition = "jsonb")
    var outputs: String = "{}"

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "steps", nullable = false, columnDefinition = "jsonb")
    var steps: String = "[]"

    @Column(name = "computed_at", nullable = false)
    var computedAt: Instant = Instant.now()
}
