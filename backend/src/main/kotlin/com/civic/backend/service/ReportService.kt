package com.civic.backend.service

import com.civic.backend.repository.ReportRepository
import com.civic.shared.model.CreateReportRequest
import com.civic.shared.model.Report
import com.civic.shared.model.User
import java.util.UUID

/** Business logic for reports. Routes call this; this calls the repository. */
class ReportService(private val repository: ReportRepository) {

    fun getFeed(): List<Report> = repository.findAll().sortedByDescending { it.createdAt }

    fun getReport(id: String): Report? = repository.findById(id)

    fun createReport(request: CreateReportRequest): Report {
        require(request.description.length <= 1000) { "Description too long" }
        val report = Report(
            id = UUID.randomUUID().toString(),
            // TODO: take author from the authenticated JWT principal
            author = User(id = "anonymous", username = "anonymous", displayName = "Anonymous"),
            category = request.category,
            description = request.description,
            imageUrl = request.imageUrl,
            location = request.location,
            capturedAt = request.capturedAt,
            createdAt = System.currentTimeMillis(),
        )
        return repository.save(report)
    }
}
