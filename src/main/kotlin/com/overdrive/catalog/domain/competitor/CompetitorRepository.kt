package com.overdrive.catalog.domain.competitor

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface CompetitorRepository : JpaRepository<Competitor, UUID> {

    @Query("""
        SELECT c FROM Competitor c
        WHERE (:distributionModel IS NULL OR c.distributionModel = :distributionModel)
          AND (:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    fun search(
        @Param("distributionModel") distributionModel: String?,
        @Param("search")            search: String?,
        pageable: Pageable
    ): Page<Competitor>
}
