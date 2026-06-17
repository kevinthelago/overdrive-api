package com.overdrive.routing.service

import com.overdrive.routing.domain.Route
import com.overdrive.routing.domain.RoutingContext

/** Stub — real implementation owned by engine-core-api stream. */
interface RoutingService {
    fun findOptimalRoute(context: RoutingContext): Route
}
