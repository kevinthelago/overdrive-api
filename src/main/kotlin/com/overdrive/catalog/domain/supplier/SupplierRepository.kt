package com.overdrive.catalog.domain.supplier

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface SupplierRepository : JpaRepository<Supplier, UUID> {

    @Query("""
        SELECT s FROM Supplier s
        WHERE (:search IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    fun search(@Param("search") search: String?, pageable: Pageable): Page<Supplier>
}

interface SupplierProductRepository : JpaRepository<SupplierProduct, SupplierProductId> {

    fun findBySupplierId(supplierId: UUID): List<SupplierProduct>
    fun findByProductId(productId: UUID): List<SupplierProduct>
    fun existsByProductId(productId: UUID): Boolean
}
