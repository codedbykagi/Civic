package com.civic.backend.repository

import com.civic.shared.model.Report
import java.util.concurrent.ConcurrentHashMap

interface ReportRepository {
    fun findAll(): List<Report>
    fun findById(id: String): Report?
    fun save(report: Report): Report
}

/** Temporary storage for development. Replace with an Exposed-backed implementation. */
class InMemoryReportRepository : ReportRepository {
    private val reports = ConcurrentHashMap<String, Report>()

    override fun findAll(): List<Report> = reports.values.toList()
    override fun findById(id: String): Report? = reports[id]
    override fun save(report: Report): Report = report.also { reports[it.id] = it }
}
