package com.civic.app.data.repository

import com.civic.app.data.local.CommentDao
import com.civic.app.data.local.CommentEntity
import com.civic.app.data.local.ReportDao
import com.civic.app.data.local.ReportEntity
import com.civic.app.data.remote.ReportApi
import com.civic.shared.model.GeoLocation
import com.civic.shared.model.IssueCategory
import com.civic.shared.model.IssueStatus
import com.civic.shared.model.Report
import com.civic.shared.model.SafetyTag
import com.civic.shared.model.TimeOfDay
import kotlinx.coroutines.flow.Flow
import java.io.File

/** Single entry point for report data; screens never talk to the API or DB directly. */
class ReportRepository(
    private val api: ReportApi,
    private val dao: ReportDao,
    private val commentDao: CommentDao,
) {
    /** Every report on this device, civic and safety (the map aggregates safety ones into zones). */
    fun observeAllReports(): Flow<List<ReportEntity>> = dao.observeAll()

    /**
     * The public-style civic feed, narrowed by category and/or status (null = any).
     * Safety reports are private: they never appear here, only as aggregated map zones and in "My safety reports".
     */
    fun observeCivicFeed(category: IssueCategory?, status: IssueStatus?): Flow<List<ReportEntity>> =
        dao.observeFiltered(category?.name, status?.name, excluded = IssueCategory.SAFETY.map { it.name })

    /** The user's own private safety reports. */
    fun observeSafetyReports(): Flow<List<ReportEntity>> = dao.observeInCategories(IssueCategory.SAFETY.map { it.name })

    fun observeReport(id: Long): Flow<ReportEntity?> = dao.observeById(id)

    suspend fun addReport(report: ReportEntity): Long = dao.insert(report)

    suspend fun upvote(id: Long) = dao.upvote(id)

    suspend fun setStatus(id: Long, status: IssueStatus) = dao.setStatus(id, status.name)

    /** [timeOfDay] null = derive it from the capture time and place. */
    suspend fun updateDetails(
        id: Long,
        category: IssueCategory,
        description: String,
        timeOfDay: TimeOfDay?,
        tags: Set<SafetyTag>,
    ) = dao.updateDetails(id, category.name, description, timeOfDay?.name, encodeTags(tags))

    suspend fun setLocation(id: Long, location: GeoLocation) = dao.setLocation(id, location.latitude, location.longitude)

    /** Attaches (or replaces) a report's photo, deleting the old file. */
    suspend fun setPhoto(id: Long, path: String) {
        val old = dao.getById(id)?.localImagePath
        dao.setPhoto(id, path)
        if (old != null && old != path) File(old).delete()
    }

    /** Comments are removed by the DB's ON DELETE CASCADE. */
    suspend fun delete(report: ReportEntity) {
        report.localImagePath?.let { File(it).delete() }
        dao.delete(report)
    }

    suspend fun delete(id: Long) {
        dao.getById(id)?.let { delete(it) }
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

/** Tags are stored as comma-separated enum names; null when there are none. */
fun encodeTags(tags: Set<SafetyTag>): String? =
    tags.takeIf { it.isNotEmpty() }?.sortedBy { it.ordinal }?.joinToString(",") { it.name }

fun decodeTags(stored: String?): Set<SafetyTag> =
    stored.orEmpty().split(',').mapNotNull { name -> SafetyTag.entries.firstOrNull { it.name == name.trim() } }.toSet()
