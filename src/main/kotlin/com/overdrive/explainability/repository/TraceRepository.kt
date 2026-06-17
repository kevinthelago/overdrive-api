package com.overdrive.explainability.repository

import com.overdrive.explainability.domain.EngineTrace
import com.overdrive.explainability.domain.EngineType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TraceRepository : JpaRepository<EngineTrace, UUID> {
    fun findByOpportunityId(opportunityId: UUID): List<EngineTrace>
    fun findByEngineTypeAndOpportunityId(engineType: EngineType, opportunityId: UUID): List<EngineTrace>
}
