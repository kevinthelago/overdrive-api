package com.overdrive.routing.api

import com.overdrive.routing.domain.Route
import com.overdrive.routing.domain.RoutingContext
import com.overdrive.routing.service.RoutingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/routing")
class RoutingController(private val routingService: RoutingService) {

    @PostMapping("/find")
    fun findOptimalRoute(@RequestBody context: RoutingContext): ResponseEntity<Route> =
        ResponseEntity.ok(routingService.findOptimalRoute(context))

    @PostMapping("/find-all")
    fun findAllRoutes(@RequestBody context: RoutingContext): ResponseEntity<List<Route>> =
        ResponseEntity.ok(routingService.findAllRoutes(context))
}
