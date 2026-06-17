package com.overdrive.competitor

import com.overdrive.catalog.domain.competitor.Competitor
import com.overdrive.competitor.domain.CompetitorStrategy
import com.overdrive.competitor.domain.CompetitorType
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.bigdecimal.shouldBeGreaterThan
import io.kotest.matchers.bigdecimal.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.math.BigDecimal
import java.util.UUID

class CompetitorStrategyTest : FreeSpec({

    val baseProduct = testProduct()

    val baseProfile = Competitor(
        id = UUID.randomUUID(),
        name = "Acme Co",
        distributionModel = "HYBRID",
        estimatedMargin = BigDecimal("0.30"),
        numWarehouses = 5,
        avgTransitDays = 3,
        regionalPresence = arrayOf("Midwest", "Northeast"),
    )

    "MarginReseller" - {
        "selling price = cost / (1 - margin)" {
            val estimate = CompetitorStrategy.MarginReseller.estimateSellingPrice(baseProduct, baseProfile)
            // 10.00 / 0.70 ≈ 14.2857
            estimate.price.amount shouldBeGreaterThan BigDecimal("14.28")
            estimate.price.amount shouldBeLessThan BigDecimal("14.30")
        }

        "delivered cost includes freight for a 2.5-lb product" {
            val estimate = CompetitorStrategy.MarginReseller.estimateDeliveredCost(baseProduct, "60601", baseProfile)
            // freight for 2.5 lbs = $9.99 (1–5 lb tier), total > selling price
            estimate.price.amount shouldBeGreaterThan BigDecimal("14.28")
            estimate.assumptions["estimatedFreight"] shouldNotBe null
        }

        "out-of-region product is not covered" {
            CompetitorStrategy.MarginReseller.isCovering(baseProduct, "Pacific", baseProfile) shouldBe false
        }

        "hazardous product is not covered" {
            val hazProduct = testProduct(hazardous = true)
            CompetitorStrategy.MarginReseller.isCovering(hazProduct, "Midwest", baseProfile) shouldBe false
        }
    }

    "BigBoxRetail" - {
        val bbProfile = baseProfile.copy(distributionModel = "DIRECT")

        "selling price is below MSRP" {
            val estimate = CompetitorStrategy.BigBoxRetail.estimateSellingPrice(baseProduct, bbProfile)
            estimate.price.amount shouldBeLessThan baseProduct.msrpAmount
        }

        "free shipping when cost exceeds threshold" {
            val highCostProduct = testProduct(costAmount = BigDecimal("40.00"), msrpAmount = BigDecimal("79.99"))
            val estimate = CompetitorStrategy.BigBoxRetail.estimateDeliveredCost(highCostProduct, "10001", bbProfile)
            val freight = estimate.assumptions["estimatedFreight"] as BigDecimal
            freight.compareTo(BigDecimal.ZERO) shouldBe 0
        }
    }

    "IndustrialDistributor" - {
        val indProfile = baseProfile.copy(distributionModel = "DISTRIBUTOR")

        "selling price = cost × (1 + margin)" {
            val estimate = CompetitorStrategy.IndustrialDistributor.estimateSellingPrice(baseProduct, indProfile)
            // 10.00 × 1.30 = 13.00
            estimate.price.amount.setScale(2).compareTo(BigDecimal("13.00")) shouldBe 0
        }

        "delivered cost includes fuel surcharge" {
            val estimate = CompetitorStrategy.IndustrialDistributor.estimateDeliveredCost(baseProduct, "30301", indProfile)
            estimate.assumptions["fuelSurchargeMultiplier"] shouldNotBe null
        }
    }

    "WarehouseClub" - {
        val wcProfile = baseProfile.copy(
            distributionModel = "MARKETPLACE",
            numWarehouses = 5,
        )

        "uses thin fixed margin regardless of profile margin" {
            val estimate = CompetitorStrategy.WarehouseClub.estimateSellingPrice(baseProduct, wcProfile)
            // 10.00 × 1.14 = 11.40
            estimate.price.amount.setScale(2).compareTo(BigDecimal("11.40")) shouldBe 0
            estimate.assumptions["profileMarginIgnored"] shouldNotBe null
        }

        "not covered when warehouse count is too low" {
            val sparseProfile = wcProfile.copy(numWarehouses = 2)
            CompetitorStrategy.WarehouseClub.isCovering(baseProduct, "Midwest", sparseProfile) shouldBe false
        }
    }

    "forModel" - {
        "returns the correct strategy for each catalog distribution model" {
            mapOf(
                "DIRECT" to CompetitorType.BIG_BOX_RETAIL,
                "DISTRIBUTOR" to CompetitorType.INDUSTRIAL_DISTRIBUTOR,
                "HYBRID" to CompetitorType.MARGIN_RESELLER,
                "MARKETPLACE" to CompetitorType.WAREHOUSE_CLUB,
                null to CompetitorType.MARGIN_RESELLER,
                "UNKNOWN" to CompetitorType.MARGIN_RESELLER,
            ).forEach { (model, expectedType) ->
                CompetitorStrategy.forModel(model).type shouldBe expectedType
            }
        }
    }

    "freightByWeight" - {
        mapOf(
            0.5 to BigDecimal("5.99"),
            2.5 to BigDecimal("9.99"),
            10.0 to BigDecimal("14.99"),
            50.0 to BigDecimal("34.99"),
            100.0 to BigDecimal("79.99"),
        ).forEach { (weightLbs, expectedFreight) ->
            "weight $weightLbs lbs → freight $$expectedFreight" {
                val product = testProduct(weightLbs = weightLbs)
                val freight = CompetitorStrategy.freightByWeight(product)
                freight.amount.compareTo(expectedFreight) shouldBe 0
            }
        }
    }
})
