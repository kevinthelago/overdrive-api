package com.overdrive.scenario.domain

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "scenario")
class Scenario(

    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true, length = 255)
    var name: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Version
    val version: Long = 0,

    @OneToMany(
        mappedBy = "scenario",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.EAGER,
    )
    @OrderBy("position ASC")
    val overrides: MutableList<ScenarioOverrideEntity> = mutableListOf(),

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
) {

    fun addOverride(entity: ScenarioOverrideEntity) {
        entity.scenario = this
        overrides.add(entity)
        updatedAt = OffsetDateTime.now()
    }

    fun clearOverrides() {
        overrides.clear()
        updatedAt = OffsetDateTime.now()
    }

    fun nextPosition(): Int = (overrides.maxOfOrNull { it.position } ?: 0) + 1
}
