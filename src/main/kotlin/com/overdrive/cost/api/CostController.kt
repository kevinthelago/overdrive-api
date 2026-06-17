package com.overdrive.cost.api

import com.overdrive.cost.domain.CostCalculationContext
import com.overdrive.cost.domain.DeliveredCostResult
import com.overdrive.cost.service.CostCalculationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/cost")
class CostController(private val costCalculationService: CostCalculationService) {

    @PostMapping("/calculate")
    fun calculate(@RequestBody context: CostCalculationContext): ResponseEntity<DeliveredCostResult> =
        ResponseEntity.ok(costCalculationService.calculate(context))
}
