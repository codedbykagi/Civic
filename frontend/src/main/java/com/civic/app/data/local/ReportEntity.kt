package com.civic.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.civic.shared.model.IssueStatus
import com.civic.shared.model.TimeOfDay

/**
 * Locally saved report: keeps a record on-device, lets posts be queued offline, and caches reports pulled from
 * the shared feed. [remoteId] is unique so pulling the same server row twice updates it instead of duplicating it.
 */
@Entity(tableName = "reports", indices = [Index(value = ["remoteId"], unique = true)])
data class ReportEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val remoteId: String? = null,
    val category: String,
    val description: String,
    /** Null for reports without a photo (e.g. a quick "I felt unsafe here"). */
    val localImagePath: String?,
    val latitude: Double?,
    val longitude: Double?,
    val capturedAt: Long,
    val upvotes: Int = 0,
    val isSynced: Boolean = false,
    /** [IssueStatus] name. */
    @ColumnInfo(defaultValue = "REPORTED") val status: String = IssueStatus.REPORTED.name,
    /**
     * [TimeOfDay] name the report refers to; null = derive it from [capturedAt] and the location
     * (see effectiveTimeOfDay).
     */
    val timeOfDay: String? = null,
    /** Comma-separated [com.civic.shared.model.SafetyTag] names; null/empty = none. Safety reports only. */
    val tags: String? = null,
    /** Author's account id: a Supabase user UUID, or "local:<install id>" for a device profile. */
    val authorId: String? = null,
    /** Author's display name, denormalised so the feed renders offline without a second table. */
    val authorName: String? = null,
    /** Author's avatar: a local file path (device profile) or a public URL (cloud account). */
    val authorAvatar: String? = null,
    /** Photo URL for a report pulled from the server; [localImagePath] is this device's own copy. */
    val imageUrl: String? = null,
    /** "public" (in the shared feed) or "private" (safety reports: author-only, enforced by RLS server-side). */
    @ColumnInfo(defaultValue = "public") val visibility: String = VISIBILITY_PUBLIC,
    /** Whether this user already upvoted. The server's one-row-per-voter table is the real guard. */
    @ColumnInfo(defaultValue = "0") val upvotedByMe: Boolean = false,
) {
    companion object {
        const val VISIBILITY_PUBLIC = "public"
        const val VISIBILITY_PRIVATE = "private"
    }
}
