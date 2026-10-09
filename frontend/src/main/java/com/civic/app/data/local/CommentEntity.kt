package com.civic.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A comment on a locally saved report. Deleted together with its report. */
@Entity(
    tableName = "comments",
    foreignKeys = [
        ForeignKey(
            entity = ReportEntity::class,
            parentColumns = ["localId"],
            childColumns = ["reportId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("reportId"), Index(value = ["remoteId"], unique = true)],
)
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reportId: Long,
    /** Author's display name at the time of writing. */
    val author: String,
    val text: String,
    val createdAt: Long,
    /** Author's account id; null for comments written before accounts existed. */
    val authorId: String? = null,
    val authorAvatar: String? = null,
    /** Server id once pushed. */
    val remoteId: String? = null,
    @ColumnInfo(defaultValue = "0") val isSynced: Boolean = false,
)
