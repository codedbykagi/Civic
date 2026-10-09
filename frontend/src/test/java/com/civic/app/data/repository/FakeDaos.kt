package com.civic.app.data.repository

import com.civic.app.data.local.CommentDao
import com.civic.app.data.local.CommentEntity
import com.civic.app.data.local.ReportDao
import com.civic.app.data.local.ReportEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory stand-ins for Room, so repository rules can be tested without a device. */
class FakeReportDao(seed: List<ReportEntity> = emptyList()) : ReportDao {

    private val rows = MutableStateFlow(seed)
    private var nextId = (seed.maxOfOrNull { it.localId } ?: 0L) + 1

    val all: List<ReportEntity> get() = rows.value

    fun row(id: Long): ReportEntity? = rows.value.firstOrNull { it.localId == id }

    private fun mutate(id: Long, change: (ReportEntity) -> ReportEntity) {
        rows.value = rows.value.map { if (it.localId == id) change(it) else it }
    }

    override fun observeAll(): Flow<List<ReportEntity>> = rows

    override fun observeFiltered(category: String?, status: String?, excluded: List<String>) = rows.map { list ->
        list.filter { (category == null || it.category == category) }
            .filter { (status == null || it.status == status) }
            .filterNot { it.category in excluded }
    }

    override fun observeInCategories(categories: List<String>) =
        rows.map { list -> list.filter { it.category in categories } }

    override fun observeById(id: Long) = rows.map { list -> list.firstOrNull { it.localId == id } }

    override suspend fun getUnsynced() = rows.value.filterNot { it.isSynced }

    override suspend fun upvote(id: Long) = mutate(id) { it.copy(upvotes = it.upvotes + 1) }

    override suspend fun setStatus(id: Long, status: String) = mutate(id) { it.copy(status = status) }

    override suspend fun updateDetails(
        id: Long,
        category: String,
        description: String,
        timeOfDay: String?,
        tags: String?,
    ) = mutate(id) { it.copy(category = category, description = description, timeOfDay = timeOfDay, tags = tags) }

    override suspend fun setLocation(id: Long, latitude: Double, longitude: Double) =
        mutate(id) { it.copy(latitude = latitude, longitude = longitude) }

    override suspend fun setPhoto(id: Long, path: String?) = mutate(id) { it.copy(localImagePath = path) }

    override suspend fun getById(id: Long) = row(id)

    override suspend fun getPendingFor(authorId: String) =
        rows.value.filter { !it.isSynced && it.authorId == authorId }

    override suspend fun getByRemoteId(remoteId: String) = rows.value.firstOrNull { it.remoteId == remoteId }

    override suspend fun markSynced(id: Long, remoteId: String, imageUrl: String?) =
        mutate(id) { it.copy(remoteId = remoteId, imageUrl = imageUrl, isSynced = true) }

    override suspend fun refreshFromServer(
        id: Long,
        upvotes: Int,
        status: String,
        upvotedByMe: Boolean,
        imageUrl: String?,
        authorId: String?,
        authorName: String?,
        authorAvatar: String?,
    ) = mutate(id) {
        it.copy(
            upvotes = upvotes,
            status = status,
            upvotedByMe = upvotedByMe,
            imageUrl = imageUrl,
            authorId = authorId,
            authorName = authorName,
            authorAvatar = authorAvatar,
        )
    }

    override suspend fun setUpvoted(id: Long, upvoted: Boolean, delta: Int) =
        mutate(id) { it.copy(upvotedByMe = upvoted, upvotes = maxOf(0, it.upvotes + delta)) }

    override suspend fun reattributeLocalReports(newAuthorId: String, newAuthorName: String) {
        rows.value = rows.value.map {
            if (it.authorId == null || it.authorId.startsWith("local:")) {
                it.copy(authorId = newAuthorId, authorName = newAuthorName, isSynced = false)
            } else {
                it
            }
        }
    }

    override suspend fun updateAuthorDetails(authorId: String, name: String, avatar: String?) {
        rows.value = rows.value.map {
            if (it.authorId == authorId) it.copy(authorName = name, authorAvatar = avatar) else it
        }
    }

    override suspend fun insert(report: ReportEntity): Long {
        val id = nextId++
        rows.value = rows.value + report.copy(localId = id)
        return id
    }

    override suspend fun insertIgnoring(report: ReportEntity): Long {
        if (report.remoteId != null && rows.value.any { it.remoteId == report.remoteId }) return -1
        return insert(report)
    }

    override suspend fun update(report: ReportEntity) = mutate(report.localId) { report }

    override suspend fun delete(report: ReportEntity) {
        rows.value = rows.value.filterNot { it.localId == report.localId }
    }
}

class FakeCommentDao : CommentDao {

    private val rows = MutableStateFlow(emptyList<CommentEntity>())
    private var nextId = 1L

    val all: List<CommentEntity> get() = rows.value

    override fun observeForReport(reportId: Long) =
        rows.map { list -> list.filter { it.reportId == reportId }.sortedBy { it.createdAt } }

    override suspend fun insert(comment: CommentEntity): Long {
        val id = nextId++
        rows.value = rows.value + comment.copy(id = id)
        return id
    }

    override suspend fun getPendingFor(authorId: String) =
        rows.value.filter { !it.isSynced && it.authorId == authorId }

    override suspend fun getByRemoteId(remoteId: String) = rows.value.firstOrNull { it.remoteId == remoteId }

    override suspend fun markSynced(id: Long, remoteId: String) {
        rows.value = rows.value.map { if (it.id == id) it.copy(remoteId = remoteId, isSynced = true) else it }
    }

    override suspend fun insertIgnoring(comment: CommentEntity): Long {
        if (comment.remoteId != null && rows.value.any { it.remoteId == comment.remoteId }) return -1
        return insert(comment)
    }

    override suspend fun updateAuthorDetails(authorId: String, name: String, avatar: String?) {
        rows.value = rows.value.map {
            if (it.authorId == authorId) it.copy(author = name, authorAvatar = avatar) else it
        }
    }
}
