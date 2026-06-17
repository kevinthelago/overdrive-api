package com.overdrive.competitor

import com.overdrive.catalog.domain.competitor.Competitor
import com.overdrive.catalog.domain.product.Product
import java.math.BigDecimal
import java.util.UUID

/**
 * Constructs a minimal [Product] for unit tests.
 * Uses the flat-column Product constructor (weightLbs, costAmount, msrpAmount, etc.)
 */
fun testProduct(
    id: UUID = UUID.randomUUID(),
    sku: String = "TEST-001",
    name: String = "Test Widget",
    category: String = "Hardware",
    weightLbs: Double = 2.5,
    costAmount: BigDecimal = BigDecimal("10.00"),
    msrpAmount: BigDecimal = BigDecimal("19.99"),
    marketSize: BigDecimal? = BigDecimal("500000"),
    orderFrequency: BigDecimal? = BigDecimal("24"),
    categoryGrowth: BigDecimal? = BigDecimal("0.08"),
    logisticsComplexity: BigDecimal? = BigDecimal("2.0"),
    hazardous: Boolean = false,
    fragile: Boolean = false,
    temperatureSensitive: Boolean = false,
    stackable: Boolean = true,
): Product = Product(
    id = id,
    sku = sku,
    name = name,
    category = category,
    weightLbs = BigDecimal(weightLbs.toString()),
    lengthIn = BigDecimal("10.0"),
    widthIn = BigDecimal("8.0"),
    heightIn = BigDecimal("4.0"),
    hazardous = hazardous,
    fragile = fragile,
    temperatureSensitive = temperatureSensitive,
    stackable = stackable,
    costAmount = costAmount,
    msrpAmount = msrpAmount,
    marketSize = marketSize,
    orderFrequency = orderFrequency,
    categoryGrowth = categoryGrowth,
    logisticsComplexity = logisticsComplexity,
)

fun testCompetitor(
    id: UUID = UUID.randomUUID(),
    name: String = "Acme Co",
    distributionModel: String = "HYBRID",
    estimatedMargin: BigDecimal = BigDecimal("0.30"),
    numWarehouses: Int = 5,
    avgTransitDays: Int = 3,
    regionalPresence: Array<String> = arrayOf("Midwest", "Northeast"),
): Competitor = Competitor(
    id = id,
    name = name,
    distributionModel = distributionModel,
    estimatedMargin = estimatedMargin,
    numWarehouses = numWarehouses,
    avgTransitDays = avgTransitDays,
    regionalPresence = regionalPresence,
)
