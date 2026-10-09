package com.civic.app.data.repository

import com.civic.app.data.auth.Account
import com.civic.app.data.local.CommentDao
import com.civic.app.data.local.CommentEntity
import com.civic.app.data.local.ReportDao
import com.civic.app.data.local.ReportEntity
import com.civic.shared.model.GeoLocation
import com.civic.shared.model.IssueCategory
import com.civic.shared.model.IssueStatus
import com.civic.shared.model.SafetyTag
import com.civic.shared.model.TimeOfDay
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Single entry point for report data; screens never talk to the API or DB directly.
 *
 * [currentAccount] supplies the signed-in identity. Authorship is stamped here rather than at each call site, so
 * every way of creating a report (the post form, a one-tap safety report, a launcher shortcut) is attributed the
 * same way and cannot forget to do it.
 */
class ReportRepository(
    private val dao: ReportDao,
    private val commentDao: CommentDao,
    private val currentAccount: () -> Account?,
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

    /**
     * Saves a new report, filling in the author and whether it belongs in the public feed.
     * Safety reports are always private: that is the one rule that must not depend on a UI toggle.
     */
    suspend fun addReport(report: ReportEntity): Long {
        val account = currentAccount()
        val isSafety = IssueCategory.entries.firstOrNull { it.name == report.category }?.isSafety == true
        return dao.insert(
            report.copy(
                authorId = report.authorId ?: account?.id,
                authorName = report.authorName ?: account?.displayName,
                authorAvatar = report.authorAvatar ?: account?.avatar,
                visibility = if (isSafety) ReportEntity.VISIBILITY_PRIVATE else ReportEntity.VISIBILITY_PUBLIC,
            ),
        )
    }

    /** Toggles this user's upvote. One per person: tapping again takes it back. */
    suspend fun toggleUpvote(id: Long) {
        val current = dao.getById(id) ?: return
        val nowUpvoted = !current.upvotedByMe
        dao.setUpvoted(id, nowUpvoted, if (nowUpvoted) 1 else -1)
    }


    suspend fun setStatus(id: Long, status: IssueStatus) = dao.setStatus(id, status.name)

    /** [timeOfDay] null = derive it from the capture time and place. */
    suspend fun updateDetails(
        id: Long,
        category: IssueCategory,
        description: String,
        timeOfDay: TimeOfDay?,
        tags: Set<SafetyTag>,
    ) = dao.updateDetails(id, category.name, description, timeOfDay?.name, encodeTags(tags))

    suspend fun setLocation(id: Long, location: GeoLocation) =
        dao.setLocation(id, location.latitude, location.longitude)

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
        val account = currentAccount()
        commentDao.insert(
            CommentEntity(
                reportId = reportId,
                author = account?.displayName ?: "Unknown",
                authorId = account?.id,
                authorAvatar = account?.avatar,
                text = text,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    /** After a profile rename, this user's existing posts should show the new name. */
    suspend fun refreshAuthorDetails(account: Account) {
        dao.updateAuthorDetails(account.id, account.displayName, account.avatar)
        commentDao.updateAuthorDetails(account.id, account.displayName, account.avatar)
    }

    /** On sign-in, adopt anything written as a device profile so it is not stranded. */
    suspend fun adoptLocalReports(account: Account) =
        dao.reattributeLocalReports(account.id, account.displayName)

    // Cloud reads and writes live in com.civic.app.data.sync.SyncManager, not here: this class stays the
    // offline source of truth so every screen keeps working with no signal.
}

/** Tags are stored as comma-separated enum names; null when there are none. */
fun encodeTags(tags: Set<SafetyTag>): String? =
    tags.takeIf { it.isNotEmpty() }?.sortedBy { it.ordinal }?.joinToString(",") { it.name }

fun decodeTags(stored: String?): Set<SafetyTag> =
    stored.orEmpty().split(',').mapNotNull { name -> SafetyTag.entries.firstOrNull { it.name == name.trim() } }.toSet()
