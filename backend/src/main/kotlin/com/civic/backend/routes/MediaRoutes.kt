package com.civic.backend.routes

import com.civic.shared.api.ApiRoutes
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.mediaRoutes() {
    route(ApiRoutes.MEDIA) {
        // TODO: accept multipart image upload, store (disk / S3), return public URL
        post { call.respond(HttpStatusCode.NotImplemented) }
    }
}
