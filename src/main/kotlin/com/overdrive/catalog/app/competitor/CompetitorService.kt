package com.overdrive.catalog.app.competitor

import com.overdrive.catalog.domain.competitor.Competitor
import com.overdrive.catalog.domain.competitor.CompetitorRepository
import com.overdrive.catalog.web.competitor.CompetitorCreateRequest
import com.overdrive.catalog.web.competitor.CompetitorUpdateRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class CompetitorService(private val competitors: CompetitorRepository) {

    @Transactional(readOnly = true)
    fun list(distributionModel: String?, search: String?, pageable: Pageable): Page<Competitor> =
        competitors.search(distributionModel, search, pageable)

    @Transactional(readOnly = true)
    fun get(id: UUID): Competitor = competitors.findById(id)
        .orElseThrow { NoSuchElementException("Competitor $id not found") }

    fun create(req: CompetitorCreateRequest): Competitor = competitors.save(
        Competitor(
            name               = req.name,
            estimatedMargin    = req.estimatedMargin,
            distributionModel  = req.distributionModel,
            numWarehouses      = req.numWarehouses,
            avgTransitDays     = req.avgTransitDays,
            deliverySpeed      = req.deliverySpeed,
            regionalPresence   = req.regionalPresence
        )
    )

    fun update(id: UUID, req: CompetitorUpdateRequest): Competitor {
        val c = get(id)
        c.name              = req.name
        c.estimatedMargin   = req.estimatedMargin
        c.distributionModel = req.distributionModel
        c.numWarehouses     = req.numWarehouses
        c.avgTransitDays    = req.avgTransitDays
        c.deliverySpeed     = req.deliverySpeed
        c.regionalPresence  = req.regionalPresence
        c.updatedAt         = Instant.now()
        return competitors.save(c)
    }

    fun delete(id: UUID) {
        competitors.delete(get(id))
    }
}
