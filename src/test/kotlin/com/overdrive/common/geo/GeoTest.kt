package com.overdrive.common.geo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

class GeoTest : StringSpec({

    "haversine distance NYC → LA is approximately 2445 miles" {
        val nyc = Geo(40.7128, -74.0060)
        val la = Geo(34.0522, -118.2437)
        nyc.distanceMilesTo(la) shouldBe (2_445.0 plusOrMinus 5.0)
    }

    "distance from a point to itself is zero" {
        val p = Geo(40.7128, -74.0060)
        p.distanceMilesTo(p) shouldBe (0.0 plusOrMinus 0.001)
    }

    "distance is symmetric" {
        val a = Geo(41.8781, -87.6298) // Chicago
        val b = Geo(29.7604, -95.3698) // Houston
        val ab = a.distanceMilesTo(b)
        val ba = b.distanceMilesTo(a)
        ab shouldBe (ba plusOrMinus 0.001)
    }

    "invalid latitude throws" {
        shouldThrow<IllegalArgumentException> { Geo(90.001, 0.0) }
        shouldThrow<IllegalArgumentException> { Geo(-90.001, 0.0) }
    }

    "invalid longitude throws" {
        shouldThrow<IllegalArgumentException> { Geo(0.0, 180.001) }
        shouldThrow<IllegalArgumentException> { Geo(0.0, -180.001) }
    }

    "Zone.fromDistanceMiles maps ranges correctly" {
        Zone.fromDistanceMiles(100.0) shouldBe Zone.ZONE_2
        Zone.fromDistanceMiles(149.9) shouldBe Zone.ZONE_2
        Zone.fromDistanceMiles(150.0) shouldBe Zone.ZONE_3
        Zone.fromDistanceMiles(299.9) shouldBe Zone.ZONE_3
        Zone.fromDistanceMiles(300.0) shouldBe Zone.ZONE_4
        Zone.fromDistanceMiles(599.9) shouldBe Zone.ZONE_4
        Zone.fromDistanceMiles(1_000.0) shouldBe Zone.ZONE_5
        Zone.fromDistanceMiles(1_400.0) shouldBe Zone.ZONE_6
        Zone.fromDistanceMiles(1_800.0) shouldBe Zone.ZONE_8
        Zone.fromDistanceMiles(3_000.0) shouldBe Zone.ZONE_8
    }

    "Zone.between derives zone from two coordinates" {
        val nyc = Geo(40.7128, -74.0060)
        val la = Geo(34.0522, -118.2437)
        Zone.between(nyc, la) shouldBe Zone.ZONE_8
    }
})
