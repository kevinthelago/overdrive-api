package com.overdrive.routing.domain

data class Location(
    val countryCode: String,
    val region: String? = null,
    val portCode: String? = null
)
