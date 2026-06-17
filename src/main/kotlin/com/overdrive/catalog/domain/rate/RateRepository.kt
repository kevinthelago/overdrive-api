package com.overdrive.catalog.domain.rate

import org.springframework.data.jpa.repository.JpaRepository

interface CategoryRateRepository : JpaRepository<CategoryRate, String>

interface ZipCentroidRepository : JpaRepository<ZipCentroid, String> {

    fun findByState(state: String): List<ZipCentroid>
    fun findByRegion(region: String): List<ZipCentroid>
}
