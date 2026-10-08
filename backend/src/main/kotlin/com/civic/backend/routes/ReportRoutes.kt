package com.civic.backend.routes

import com.civic.backend.service.ReportService
import com.civic.shared.api.ApiRoutes
import com.civic.shared.model.CreateReportRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.reportRoutes(service: ReportService) {
    route(ApiRoutes.REPORTS) {
        // GET /api/v1/reports — the social feed
        get { call.respond(service.getFeed()) }

        // GET /api/v1/reports/{id}
        get("{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val report = service.getReport(id) ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respond(report)
        }

        // POST /api/v1/reports — create a new post
        post {
            val request = call.receive<CreateReportRequest>()
            call.respond(HttpStatusCode.Created, service.createReport(request))
        }

        // TODO: POST {id}/upvote, GET/POST {id}/comments, GET nearby?lat=&lng=&radius=
    }
}
