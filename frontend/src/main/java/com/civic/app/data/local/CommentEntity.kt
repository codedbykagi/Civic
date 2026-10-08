package com.civic.app.data.local

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
    indices = [Index("reportId")],
)
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reportId: Long,
    val author: String,
    val text: String,
    val createdAt: Long,
)
