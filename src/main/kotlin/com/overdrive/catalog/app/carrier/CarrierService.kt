package com.overdrive.catalog.app.carrier

import com.overdrive.catalog.domain.carrier.Carrier
import com.overdrive.catalog.domain.carrier.CarrierLane
import com.overdrive.catalog.domain.carrier.CarrierLaneRepository
import com.overdrive.catalog.domain.carrier.CarrierRepository
import com.overdrive.catalog.web.carrier.CarrierCreateRequest
import com.overdrive.catalog.web.carrier.CarrierLaneCreateRequest
import com.overdrive.catalog.web.carrier.CarrierUpdateRequest
import com.overdrive.catalog.app.CatalogReferentialIntegrityException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class CarrierService(
    private val carriers: CarrierRepository,
    private val lanes: CarrierLaneRepository
) {

    @Transactional(readOnly = true)
    fun list(pricingModel: String?, search: String?, pageable: Pageable): Page<Carrier> =
        carriers.search(pricingModel, search, pageable)

    @Transactional(readOnly = true)
    fun get(id: UUID): Carrier = carriers.findById(id)
        .orElseThrow { NoSuchElementException("Carrier $id not found") }

    @Transactional(readOnly = true)
    fun getLanes(carrierId: UUID): List<CarrierLane> = lanes.findByCarrierId(carrierId)

    fun create(req: CarrierCreateRequest): Carrier = carriers.save(
        Carrier(
            name                 = req.name,
            scac                 = req.scac,
            pricingModel         = req.pricingModel,
            liftgateSurcharge    = req.liftgateSurcharge,
            residentialSurcharge = req.residentialSurcharge,
            dimFactor            = req.dimFactor,
            fuelSurchargePct     = req.fuelSurchargePct
        )
    )

    fun update(id: UUID, req: CarrierUpdateRequest): Carrier {
        val c = get(id)
        c.name                 = req.name
        c.scac                 = req.scac
        c.pricingModel         = req.pricingModel
        c.liftgateSurcharge    = req.liftgateSurcharge
        c.residentialSurcharge = req.residentialSurcharge
        c.dimFactor            = req.dimFactor
        c.fuelSurchargePct     = req.fuelSurchargePct
        c.updatedAt            = Instant.now()
        return carriers.save(c)
    }

    fun delete(id: UUID) {
        if (lanes.existsByCarrierId(id)) {
            throw CatalogReferentialIntegrityException(
                "Carrier $id has lane entries; delete lanes first"
            )
        }
        carriers.delete(get(id))
    }

    fun addLane(carrierId: UUID, req: CarrierLaneCreateRequest): CarrierLane {
        val carrier = get(carrierId)
        return lanes.save(
            CarrierLane(
                carrier         = carrier,
                serviceLevel    = req.serviceLevel,
                originZone      = req.originZone,
                destZone        = req.destZone,
                originZipPrefix = req.originZipPrefix,
                destZipPrefix   = req.destZipPrefix,
                transitDays     = req.transitDays,
                baseRate        = req.baseRate,
                perLbRate       = req.perLbRate,
                perCwtRate      = req.perCwtRate,
                minCharge       = req.minCharge
            )
        )
    }

    fun deleteLane(laneId: UUID) {
        val lane = lanes.findById(laneId)
            .orElseThrow { NoSuchElementException("CarrierLane $laneId not found") }
        lanes.delete(lane)
    }
}
