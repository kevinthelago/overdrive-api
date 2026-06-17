package com.overdrive.common.geo

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Immutable geographic coordinate (WGS-84). */
data class Geo(val lat: Double, val lon: Double) {

    init {
        require(lat in -90.0..90.0) { "Latitude must be in [-90, 90], got $lat" }
        require(lon in -180.0..180.0) { "Longitude must be in [-180, 180], got $lon" }
    }

    /**
     * Great-circle distance in statute miles using the haversine formula.
     * Deterministic for the same input pair — safe to cache.
     */
    fun distanceMilesTo(other: Geo): Double {
        val earthRadiusMiles = 3_958.8
        val dLat = Math.toRadians(other.lat - lat)
        val dLon = Math.toRadians(other.lon - lon)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat)) * cos(Math.toRadians(other.lat)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * asin(sqrt(a))
        return earthRadiusMiles * c
    }
}
