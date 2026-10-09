package com.civic.app.data.sync

import com.civic.app.data.auth.AuthRepository
import com.civic.app.data.auth.AuthState
import com.civic.app.data.cloud.CloudException
import com.civic.app.data.cloud.CloudReportApi
import com.civic.app.data.cloud.CommentDto
import com.civic.app.data.cloud.ReportDto
import com.civic.app.data.cloud.StorageApi
import com.civic.app.data.local.CommentDao
import com.civic.app.data.local.CommentEntity
import com.civic.app.data.local.ReportDao
import com.civic.app.data.local.ReportEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.time.Instant

/** What the UI shows about syncing. */
sealed interface SyncStatus {
    data object Idle : SyncStatus

    /** No account signed in, or this build has no backend: the app is working purely on-device. */
    data object OfflineOnly : SyncStatus
    data object Running : SyncStatus
    data class Done(val atMillis: Long, val pushed: Int, val pulled: Int) : SyncStatus
    data class Failed(val message: String) : SyncStatus
}

/**
 * Moves reports and comments between Room and Postgres.
 *
 * Room stays the source of truth for the UI — every screen reads it, so the app works with no signal — and this
 * class is the only thing that talks to the server about reports. Push happens before pull, so a report written
 * offline is never overwritten by an older server state.
 *
 * Sync is opportunistic: on sign-in, on app start and on pull-to-refresh. There is deliberately no background
 * scheduler yet; adding WorkManager is the next step (see context.md TODOs).
 */
class SyncManager(
    private val auth: AuthRepository,
    private val api: CloudReportApi,
    private val storage: StorageApi,
    private val dao: ReportDao,
    private val commentDao: CommentDao,
) {
    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    /** One sync at a time: two overlapping runs would fight over the same rows. */
    private val lock = Mutex()

    /** True when there is a cloud account to sync as. */
    val canSync: Boolean
        get() = auth.isCloudAvailable && (auth.state.value as? AuthState.Active)?.isCloud == true

    /**
     * Pushes what is pending, then pulls the shared feed. Never throws: a sync failure is reported through
     * [status] because it must not break a screen the user is looking at.
     */
    suspend fun syncNow(): SyncStatus {
        if (!canSync) {
            _status.value = SyncStatus.OfflineOnly
            return _status.value
        }
        if (lock.isLocked) return _status.value
        return lock.withLock {
            _status.value = SyncStatus.Running
            val result = runCatching {
                val pushed = pushPendingReports() + pushPendingComments()
                val pulled = pullFeed()
                pushed to pulled
            }
            _status.value = result.fold(
                onSuccess = { (pushed, pulled) -> SyncStatus.Done(System.currentTimeMillis(), pushed, pulled) },
                onFailure = { SyncStatus.Failed((it as? CloudException)?.message ?: "Sync failed.") },
            )
            _status.value
        }
    }

    /** Pulls the comments of one report so a detail screen shows what other people wrote. */
    suspend fun syncComments(localReportId: Long) {
        if (!canSync) return
        val report = dao.getById(localReportId) ?: return
        val remoteId = report.remoteId ?: return
        runCatching {
            api.comments(remoteId).forEach { dto ->
                val id = dto.id ?: return@forEach
                if (commentDao.getByRemoteId(id) != null) return@forEach
                commentDao.insertIgnoring(
                    CommentEntity(
                        reportId = localReportId,
                        author = dto.profiles?.displayName ?: "Unknown",
                        authorId = dto.authorId,
                        authorAvatar = dto.profiles?.avatarUrl,
                        text = dto.text,
                        createdAt = dto.createdAt.toEpochMillis(),
                        remoteId = id,
                        isSynced = true,
                    ),
                )
            }
        }
    }

    /** Sends an upvote (or takes it back) for a report that exists on the server. */
    suspend fun pushUpvote(localReportId: Long, upvoted: Boolean) {
        if (!canSync) return
        val userId = auth.currentUserId ?: return
        val remoteId = dao.getById(localReportId)?.remoteId ?: return
        runCatching {
            if (upvoted) api.upvote(remoteId, userId) else api.removeUpvote(remoteId, userId)
        }
    }

    // ---------- push ----------

    private suspend fun pushPendingReports(): Int {
        val account = (auth.state.value as? AuthState.Active)?.account ?: return 0
        var pushed = 0
        for (report in dao.getPendingFor(account.id)) {
            val uploaded = report.uploadedPhotoUrl(account.id)
            val saved = api.upsert(report.toDto(account.id, uploaded, auth.installId))
            val remoteId = saved?.id ?: continue
            dao.markSynced(report.localId, remoteId, uploaded)
            pushed++
        }
        return pushed
    }

    /** Uploads the local photo once; a report already carrying a URL is left alone. */
    private suspend fun ReportEntity.uploadedPhotoUrl(userId: String): String? {
        if (imageUrl != null) return imageUrl
        val path = localImagePath ?: return null
        val file = File(path)
        if (!file.exists()) return null
        return runCatching { storage.uploadPhoto(userId, file) }.getOrNull()
    }

    private suspend fun pushPendingComments(): Int {
        val account = (auth.state.value as? AuthState.Active)?.account ?: return 0
        var pushed = 0
        for (comment in commentDao.getPendingFor(account.id)) {
            // The parent report has to exist on the server before its comments can.
            val parentRemoteId = dao.getById(comment.reportId)?.remoteId ?: continue
            val saved = api.addComment(
                CommentDto(reportId = parentRemoteId, authorId = account.id, text = comment.text),
            )
            val remoteId = saved?.id ?: continue
            commentDao.markSynced(comment.id, remoteId)
            pushed++
        }
        return pushed
    }

    // ---------- pull ----------

    /**
     * Caches the newest public reports, plus this user's own (which includes their private safety reports —
     * those are only ever readable by them, and the server enforces that, not this code).
     */
    private suspend fun pullFeed(): Int {
        val userId = auth.currentUserId ?: return 0
        val voted = runCatching { api.myUpvotes(userId) }.getOrDefault(emptyList()).toSet()
        val rows = api.feed() + runCatching { api.mine(userId) }.getOrDefault(emptyList())
        var changed = 0
        for (dto in rows.distinctBy { it.id }) {
            val remoteId = dto.id ?: continue
            val existing = dao.getByRemoteId(remoteId)
            val upvotedByMe = remoteId in voted
            if (existing == null) {
                dao.insertIgnoring(dto.toEntity(remoteId, upvotedByMe))
                changed++
            } else {
                dao.refreshFromServer(
                    id = existing.localId,
                    upvotes = dto.upvotes,
                    status = dto.status,
                    upvotedByMe = upvotedByMe,
                    imageUrl = dto.imageUrl ?: existing.imageUrl,
                    authorId = dto.authorId,
                    authorName = dto.profiles?.displayName ?: existing.authorName,
                    authorAvatar = dto.profiles?.avatarUrl ?: existing.authorAvatar,
                )
                changed++
            }
        }
        return changed
    }
}

// ---------- mapping ----------

internal fun ReportEntity.toDto(authorId: String, photoUrl: String?, installId: String) = ReportDto(
    authorId = authorId,
    category = category,
    description = description,
    imageUrl = photoUrl,
    latitude = latitude,
    longitude = longitude,
    capturedAt = Instant.ofEpochMilli(capturedAt).toString(),
    status = status,
    timeOfDay = timeOfDay,
    tags = tags?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
    visibility = visibility,
    // Unique per author, so re-sending after a dropped connection updates rather than duplicates.
    clientId = "$installId:$localId",
)

internal fun ReportDto.toEntity(remoteId: String, upvotedByMe: Boolean) = ReportEntity(
    remoteId = remoteId,
    category = category,
    description = description,
    localImagePath = null,
    imageUrl = imageUrl,
    latitude = latitude,
    longitude = longitude,
    capturedAt = capturedAt.toEpochMillis(),
    upvotes = upvotes,
    isSynced = true,
    status = status,
    timeOfDay = timeOfDay,
    tags = tags.takeIf { it.isNotEmpty() }?.joinToString(","),
    authorId = authorId,
    authorName = profiles?.displayName,
    authorAvatar = profiles?.avatarUrl,
    visibility = visibility,
    upvotedByMe = upvotedByMe,
)

/** Postgres timestamptz comes back as ISO-8601; an unparseable value falls back to "now" rather than crashing. */
internal fun String?.toEpochMillis(): Long = this?.let {
    runCatching { Instant.parse(it).toEpochMilli() }
        .getOrElse { _ -> runCatching { Instant.parse(it.replace(' ', 'T') + "Z").toEpochMilli() }.getOrNull() }
} ?: System.currentTimeMillis()
