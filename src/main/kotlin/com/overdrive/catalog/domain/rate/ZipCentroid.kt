package com.overdrive.catalog.domain.rate

import jakarta.persistence.*

@Entity
@Table(name = "zip_centroid")
class ZipCentroid(

    @Id
    @Column(length = 10)
    val zip: String,

    @Column(nullable = false) val lat: Double,
    @Column(nullable = false) val lng: Double,

    @Column(length = 100) val city: String? = null,

    @Column(nullable = false, length = 2)  val state: String,

    /** NORTHEAST | SOUTHEAST | MIDWEST | SOUTHWEST | WEST */
    @Column(nullable = false, length = 20) val region: String
)
