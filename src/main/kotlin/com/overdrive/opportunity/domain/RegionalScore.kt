package com.overdrive.opportunity.domain

import java.math.BigDecimal

/**
 * Opportunity score for one product in one US region, computed at a representative ZIP centroid.
 * These feed the heat-map endpoint.
 */
data class RegionalScore(
    val productId: Long,
    val region: String,
    val representativeZip: String,
    val score: BigDecimal,
    val savingsPct: BigDecimal,
)

/**
 * US regions with a representative ZIP centroid.
 * Centroids are selected from the catalog ZIP-centroid table by region name.
 */
enum class UsRegion(val label: String, val representativeZip: String) {
    NORTHEAST("Northeast", "10001"),
    SOUTHEAST("Southeast", "30301"),
    MIDWEST("Midwest", "60601"),
    SOUTH_CENTRAL("South Central", "73301"),
    MOUNTAIN("Mountain", "85001"),
    PACIFIC("Pacific", "90001"),
    NORTHWEST("Northwest", "98101"),
}
