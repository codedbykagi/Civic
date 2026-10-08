package com.civic.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.civic.shared.model.IssueStatus
import com.civic.shared.model.TimeOfDay

/** Locally saved report: keeps a record on-device and lets posts be queued offline. */
@Entity(tableName = "reports")
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
    /** [TimeOfDay] name the report refers to; null = derive from [capturedAt] and the location (see effectiveTimeOfDay). */
    val timeOfDay: String? = null,
    /** Comma-separated [com.civic.shared.model.SafetyTag] names; null/empty = none. Safety reports only. */
    val tags: String? = null,
)
