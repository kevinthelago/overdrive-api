package com.overdrive.competitor

import com.overdrive.catalog.domain.CompetitorProfile
import com.overdrive.catalog.domain.DistributionModel
import com.overdrive.catalog.domain.Product
import com.overdrive.common.money.Money
import com.overdrive.competitor.domain.CompetitorStrategy
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.bigdecimal.shouldBeGreaterThan
import io.kotest.matchers.bigdecimal.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.math.BigDecimal

class CompetitorStrategyTest : FreeSpec({

    val baseProduct = Product(
        id = 1L,
        sku = "TEST-001",
        name = "Test Widget",
        category = testCategory(),
        weight = testWeight(2.5),
        dimensions = testDimensions(),
        cost = Money.usd(BigDecimal("10.00")),
        msrp = Money.usd(BigDecimal("19.99")),
        marketSize = BigDecimal("500000"),
        orderFrequency = BigDecimal("24"),
        categoryGrowth = BigDecimal("0.08"),
        logisticsComplexity = BigDecimal("2.0"),
        hazardous = false,
        fragile = false,
        temperatureSensitive = false,
        stackable = true,
    )

    val baseProfile = CompetitorProfile(
        id = 1L,
        name = "Acme Co",
        distributionModel = DistributionModel.MARGIN_RESELLER,
        estimatedMarginPct = BigDecimal("0.30"),
        numberOfWarehouses = 5,
        deliverySpeedDays = 3,
        regionalPresence = setOf("Midwest", "Northeast"),
    )

    "MarginReseller" - {
        "selling price = cost / (1 - margin)" {
            val estimate = CompetitorStrategy.MarginReseller.estimateSellingPrice(baseProduct, baseProfile)
            // 10.00 / 0.70 ≈ 14.2857
            estimate.price.amount shouldBeGreaterThan BigDecimal("14.28")
            estimate.price.amount shouldBeLessThan BigDecimal("14.29")
        }

        "delivered cost includes freight for a 2.5-lb product" {
            val estimate = CompetitorStrategy.MarginReseller.estimateDeliveredCost(baseProduct, "60601", baseProfile)
            estimate.price.amount shouldBeGreaterThan BigDecimal("14.28")
            estimate.assumptions["estimatedFreight"] shouldNotBe null
        }

        "out-of-region product is not covered" {
            val covered = CompetitorStrategy.MarginReseller.isCovering(baseProduct, "Pacific", baseProfile)
            covered shouldBe false
        }

        "hazardous product is not covered" {
            val hazProduct = baseProduct.copy(hazardous = true)
            val covered = CompetitorStrategy.MarginReseller.isCovering(hazProduct, "Midwest", baseProfile)
            covered shouldBe false
        }
    }

    "BigBoxRetail" - {
        val bbProfile = baseProfile.copy(distributionModel = DistributionModel.BIG_BOX_RETAIL)

        "selling price is below MSRP" {
            val estimate = CompetitorStrategy.BigBoxRetail.estimateSellingPrice(baseProduct, bbProfile)
            estimate.price.amount shouldBeLessThan baseProduct.msrp.amount
        }

        "free shipping when cost exceeds threshold" {
            val highCostProduct = baseProduct.copy(cost = Money.usd(BigDecimal("40.00")),
                msrp = Money.usd(BigDecimal("79.99")))
            val estimate = CompetitorStrategy.BigBoxRetail.estimateDeliveredCost(highCostProduct, "10001", bbProfile)
            val freight = estimate.assumptions["estimatedFreight"] as BigDecimal
            freight.compareTo(BigDecimal.ZERO) shouldBe 0
        }
    }

    "IndustrialDistributor" - {
        val indProfile = baseProfile.copy(distributionModel = DistributionModel.INDUSTRIAL_DISTRIBUTOR)

        "selling price = cost × (1 + margin)" {
            val estimate = CompetitorStrategy.IndustrialDistributor.estimateSellingPrice(baseProduct, indProfile)
            // 10.00 × 1.30 = 13.00
            estimate.price.amount shouldBe BigDecimal("13.0000000000").setScale(10)
        }

        "delivered cost includes fuel surcharge" {
            val estimate = CompetitorStrategy.IndustrialDistributor.estimateDeliveredCost(baseProduct, "30301", indProfile)
            estimate.assumptions["fuelSurchargeMultiplier"] shouldNotBe null
        }
    }

    "WarehouseClub" - {
        val wcProfile = baseProfile.copy(
            distributionModel = DistributionModel.WAREHOUSE_CLUB,
            numberOfWarehouses = 5,
        )

        "uses thin fixed margin regardless of profile margin" {
            val estimate = CompetitorStrategy.WarehouseClub.estimateSellingPrice(baseProduct, wcProfile)
            // 10.00 × 1.14 = 11.40
            estimate.price.amount shouldBe BigDecimal("11.4000000000").setScale(10)
            estimate.assumptions["profileMarginIgnored"] shouldNotBe null
        }

        "not covered when warehouse count is too low" {
            val sparseProfile = wcProfile.copy(numberOfWarehouses = 2)
            CompetitorStrategy.WarehouseClub.isCovering(baseProduct, "Midwest", sparseProfile) shouldBe false
        }
    }

    "forModel" - {
        "returns the correct strategy for each distribution model" {
            DistributionModel.entries.forEach { model ->
                CompetitorStrategy.forModel(model).type shouldBe model
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
            "weight $weightLbs lbs → freight $expectedFreight" {
                val product = baseProduct.copy(weight = testWeight(weightLbs))
                val freight = CompetitorStrategy.freightByWeight(product)
                freight.amount shouldBe expectedFreight
            }
        }
    }
})
