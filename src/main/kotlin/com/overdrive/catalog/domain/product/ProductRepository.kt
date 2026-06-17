package com.overdrive.catalog.domain.product

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface ProductRepository : JpaRepository<Product, UUID> {

    fun findByCategory(category: String, pageable: Pageable): Page<Product>

    @Query("""
        SELECT p FROM Product p
        WHERE (:category IS NULL OR p.category = :category)
          AND (:search   IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                                 OR LOWER(p.sku)  LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
    """)
    fun search(
        @Param("category") category: String?,
        @Param("search")   search: String?,
        pageable: Pageable
    ): Page<Product>

    fun existsBySku(sku: String): Boolean

    fun existsBySkuAndIdNot(sku: String, id: java.util.UUID): Boolean
}
