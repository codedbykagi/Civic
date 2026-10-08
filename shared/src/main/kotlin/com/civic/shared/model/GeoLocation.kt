package com.civic.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class GeoLocation(
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
)
