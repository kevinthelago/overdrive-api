package com.overdrive.common.geo

/**
 * Carrier rate zone derived from origin-to-destination great-circle distance.
 * Thresholds approximate UPS/FedEx domestic ground zone breakpoints.
 */
enum class Zone {
    ZONE_2, ZONE_3, ZONE_4, ZONE_5, ZONE_6, ZONE_7, ZONE_8;

    companion object {
        fun fromDistanceMiles(miles: Double): Zone = when {
            miles < 150 -> ZONE_2
            miles < 300 -> ZONE_3
            miles < 600 -> ZONE_4
            miles <= 1_000 -> ZONE_5
            miles <= 1_400 -> ZONE_6
            miles < 1_800 -> ZONE_7
            else -> ZONE_8
        }

        fun between(origin: Geo, destination: Geo): Zone =
            fromDistanceMiles(origin.distanceMilesTo(destination))
    }
}
