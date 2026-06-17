package com.overdrive.scenario.domain

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
    name = "scenario_override",
    uniqueConstraints = [UniqueConstraint(columnNames = ["scenario_id", "position"])]
)
class ScenarioOverrideEntity(

    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    var scenario: Scenario,

    @Enumerated(EnumType.STRING)
    @Column(name = "override_type", nullable = false, length = 50)
    val overrideType: OverrideType,

    @Column(nullable = false)
    val position: Int,

    /** Nullable — overrides that don't reference a single catalog entity omit this. */
    @Column(name = "entity_id")
    val entityId: UUID? = null,

    /**
     * Type-specific parameters serialised to JSONB.
     * Keys by override type:
     *   ADD_WAREHOUSE / REMOVE_WAREHOUSE: none (entity_id is the warehouseId)
     *   ADD_CARRIER / REMOVE_CARRIER: none (entity_id is the carrierId)
     *   SUPPLIER_PRICE_DELTA: { deltaPct: String }
     *   FEE_SCHEDULE_CHANGE: { entityType: String, newRate: String }
     *   DEMAND_FACTOR_CHANGE: { factorKey: String, newValue: String }
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    val params: Map<String, String> = emptyMap(),

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
