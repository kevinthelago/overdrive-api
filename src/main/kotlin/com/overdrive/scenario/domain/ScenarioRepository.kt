package com.overdrive.scenario.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ScenarioRepository : JpaRepository<Scenario, UUID> {
    fun existsByName(name: String): Boolean
    fun existsByNameAndIdNot(name: String, id: UUID): Boolean
}
