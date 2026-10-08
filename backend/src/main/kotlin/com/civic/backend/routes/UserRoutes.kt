package com.civic.backend.routes

import com.civic.shared.api.ApiRoutes
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.userRoutes() {
    route(ApiRoutes.USERS) {
        // TODO: register, login (issue JWT), profile, user's own reports
        get("{id}") { call.respond(HttpStatusCode.NotImplemented) }
    }
}
