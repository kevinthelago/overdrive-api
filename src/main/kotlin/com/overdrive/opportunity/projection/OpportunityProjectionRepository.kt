package com.overdrive.opportunity.projection

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface OpportunityProjectionRepository : JpaRepository<OpportunityProjection, UUID> {

    fun findAllByScenarioIdIsNullOrderByScoreDesc(): List<OpportunityProjection>

    fun findAllByScenarioIdOrderByScoreDesc(scenarioId: UUID): List<OpportunityProjection>

    fun findByProductIdAndScenarioIdIsNull(productId: UUID): OpportunityProjection?

    fun findByProductIdAndScenarioId(productId: UUID, scenarioId: UUID): OpportunityProjection?

    fun findAllByCategoryAndScenarioIdIsNullOrderByScoreDesc(category: String): List<OpportunityProjection>

    @Modifying
    @Query(
        """
        DELETE FROM OpportunityProjection p
        WHERE p.productId = :productId
          AND ((:scenarioId IS NULL AND p.scenarioId IS NULL)
               OR p.scenarioId = :scenarioId)
        """,
    )
    fun deleteByProductIdAndScenarioId(
        @Param("productId") productId: UUID,
        @Param("scenarioId") scenarioId: UUID?,
    )
}
