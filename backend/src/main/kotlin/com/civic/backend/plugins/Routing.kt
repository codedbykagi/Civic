package com.civic.backend.plugins

import com.civic.backend.repository.InMemoryReportRepository
import com.civic.backend.routes.mediaRoutes
import com.civic.backend.routes.reportRoutes
import com.civic.backend.routes.userRoutes
import com.civic.backend.service.ReportService
import com.civic.shared.api.ApiRoutes
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    // TODO: swap in a database-backed repository / dependency injection (e.g. Koin)
    val reportService = ReportService(InMemoryReportRepository())

    routing {
        get(ApiRoutes.HEALTH) { call.respondText("OK") }
        reportRoutes(reportService)
        userRoutes()
        mediaRoutes()
    }
}
