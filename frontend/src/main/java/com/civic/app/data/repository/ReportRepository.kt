package com.civic.app.data.repository

import com.civic.app.data.local.CommentDao
import com.civic.app.data.local.CommentEntity
import com.civic.app.data.local.ReportDao
import com.civic.app.data.local.ReportEntity
import com.civic.app.data.remote.ReportApi
import com.civic.shared.model.IssueCategory
import com.civic.shared.model.IssueStatus
import com.civic.shared.model.Report
import kotlinx.coroutines.flow.Flow
import java.io.File

/** Single entry point for report data; screens never talk to the API or DB directly. */
class ReportRepository(
    private val api: ReportApi,
    private val dao: ReportDao,
    private val commentDao: CommentDao,
) {
    /** Prototype feed: reports stored on this device. */
    fun observeLocalFeed(): Flow<List<ReportEntity>> = dao.observeAll()

    /** Feed narrowed by category and/or status; null = any. */
    fun observeLocalFeed(category: IssueCategory?, status: IssueStatus?): Flow<List<ReportEntity>> =
        dao.observeFiltered(category?.name, status?.name)

    fun observeReport(id: Long): Flow<ReportEntity?> = dao.observeById(id)

    suspend fun addReport(report: ReportEntity): Long = dao.insert(report)

    suspend fun upvote(id: Long) = dao.upvote(id)

    suspend fun setStatus(id: Long, status: IssueStatus) = dao.setStatus(id, status.name)

    /** Comments are removed by the DB's ON DELETE CASCADE. */
    suspend fun delete(report: ReportEntity) {
        File(report.localImagePath).delete()
        dao.delete(report)
    }

    fun observeComments(reportId: Long): Flow<List<CommentEntity>> = commentDao.observeForReport(reportId)

    suspend fun addComment(reportId: Long, text: String) {
        // TODO: real author once accounts exist.
        commentDao.insert(CommentEntity(reportId = reportId, author = "Guest", text = text, createdAt = System.currentTimeMillis()))
    }

    /** Remote feed from the backend (not used by the prototype UI yet). */
    suspend fun getRemoteFeed(): List<Report> = api.getFeed()

    // TODO: syncPending() — upload image, POST report, mark isSynced (WorkManager job)
}
