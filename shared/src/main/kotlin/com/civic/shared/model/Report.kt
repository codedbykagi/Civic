package com.civic.shared.model

import kotlinx.serialization.Serializable

/** A single issue post: photo + category + where and when it was captured. */
@Serializable
data class Report(
    val id: String,
    val author: User,
    val category: IssueCategory,
    val description: String,
    val imageUrl: String,
    val location: GeoLocation,
    /** Capture time, epoch milliseconds (UTC). */
    val capturedAt: Long,
    val createdAt: Long,
    val status: IssueStatus = IssueStatus.REPORTED,
    val upvotes: Int = 0,
    val commentCount: Int = 0,
)

/** Body sent by the app when posting a new report (image uploaded separately). */
@Serializable
data class CreateReportRequest(
    val category: IssueCategory,
    val description: String,
    val imageUrl: String,
    val location: GeoLocation,
    val capturedAt: Long,
)
