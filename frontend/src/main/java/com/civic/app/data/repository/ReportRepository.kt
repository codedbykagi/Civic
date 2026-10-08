package com.civic.app.data.repository

import com.civic.app.data.local.ReportDao
import com.civic.app.data.local.ReportEntity
import com.civic.app.data.remote.ReportApi
import com.civic.shared.model.Report
import kotlinx.coroutines.flow.Flow
import java.io.File

/** Single entry point for report data; screens never talk to the API or DB directly. */
class ReportRepository(
    private val api: ReportApi,
    private val dao: ReportDao,
) {
    /** Prototype feed: reports stored on this device. */
    fun observeLocalFeed(): Flow<List<ReportEntity>> = dao.observeAll()

    suspend fun addReport(report: ReportEntity): Long = dao.insert(report)

    suspend fun upvote(id: Long) = dao.upvote(id)

    suspend fun delete(report: ReportEntity) {
        File(report.localImagePath).delete()
        dao.delete(report)
    }

    /** Remote feed from the backend (not used by the prototype UI yet). */
    suspend fun getRemoteFeed(): List<Report> = api.getFeed()

    // TODO: syncPending() — upload image, POST report, mark isSynced (WorkManager job)
}
