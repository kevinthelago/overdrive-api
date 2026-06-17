package com.overdrive.catalog.domain.carrier

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface CarrierRepository : JpaRepository<Carrier, UUID> {

    @Query("""
        SELECT c FROM Carrier c
        WHERE (:pricingModel IS NULL OR c.pricingModel = :pricingModel)
          AND (:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
                               OR LOWER(c.scac) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    fun search(
        @Param("pricingModel") pricingModel: String?,
        @Param("search")       search: String?,
        pageable: Pageable
    ): Page<Carrier>
}

interface CarrierLaneRepository : JpaRepository<CarrierLane, UUID> {

    fun findByCarrierId(carrierId: UUID): List<CarrierLane>

    fun findByCarrierIdAndServiceLevel(carrierId: UUID, serviceLevel: String): List<CarrierLane>

    @Query("""
        SELECT l FROM CarrierLane l
        WHERE l.carrier.id = :carrierId
          AND (:originZone IS NULL OR l.originZone = :originZone)
          AND (:destZone   IS NULL OR l.destZone   = :destZone)
          AND (:service    IS NULL OR l.serviceLevel = :service)
    """)
    fun findLanes(
        @Param("carrierId")   carrierId: UUID,
        @Param("originZone")  originZone: String?,
        @Param("destZone")    destZone: String?,
        @Param("service")     service: String?
    ): List<CarrierLane>

    fun existsByCarrierId(carrierId: UUID): Boolean
}
