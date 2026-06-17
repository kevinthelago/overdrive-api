package com.overdrive.opportunity.domain

import java.math.BigDecimal
import java.util.UUID

/**
 * Opportunity score for one product in one US region, computed at a representative ZIP centroid.
 * These feed the heat-map endpoint.
 */
data class RegionalScore(
    val productId: UUID,
    val region: String,
    val representativeZip: String,
    val score: BigDecimal,
    val savingsPct: BigDecimal,
)

/**
 * US regions mirroring the catalog ZIP-centroid [region] column values.
 * Representative ZIPs are chosen from the seed data for each region.
 */
enum class UsRegion(val regionLabel: String, val representativeZip: String) {
    NORTHEAST("NORTHEAST", "10001"),
    SOUTHEAST("SOUTHEAST", "30301"),
    MIDWEST("MIDWEST", "60601"),
    SOUTHWEST("SOUTHWEST", "73301"),
    WEST("WEST", "90001"),
}
