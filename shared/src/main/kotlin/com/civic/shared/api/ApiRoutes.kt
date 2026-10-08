package com.civic.shared.api

/** Single source of truth for endpoint paths used by both client and server. */
object ApiRoutes {
    const val API_PREFIX = "/api/v1"

    const val HEALTH = "/health"
    const val REPORTS = "$API_PREFIX/reports"
    const val USERS = "$API_PREFIX/users"
    const val MEDIA = "$API_PREFIX/media"
}
