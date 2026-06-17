package com.overdrive.catalog.domain.warehouse

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface WarehouseRepository : JpaRepository<Warehouse, UUID> {

    @Query("""
        SELECT w FROM Warehouse w
        WHERE (:type   IS NULL OR w.type  = :type)
          AND (:state  IS NULL OR w.state = :state)
          AND (:search IS NULL OR LOWER(w.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
    """)
    fun search(
        @Param("type")   type: String?,
        @Param("state")  state: String?,
        @Param("search") search: String?,
        pageable: Pageable
    ): Page<Warehouse>
}
