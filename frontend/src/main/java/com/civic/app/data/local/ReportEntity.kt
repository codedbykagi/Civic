package com.civic.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Locally saved report: keeps a record on-device and lets posts be queued offline. */
@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val remoteId: String? = null,
    val category: String,
    val description: String,
    val localImagePath: String,
    val latitude: Double?,
    val longitude: Double?,
    val capturedAt: Long,
    val upvotes: Int = 0,
    val isSynced: Boolean = false,
)
