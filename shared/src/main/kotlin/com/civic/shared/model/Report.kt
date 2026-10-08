package com.civic.shared.model

import kotlinx.serialization.Serializable

/** A single issue post: category + where and when it was captured, with an optional photo. */
@Serializable
data class Report(
    val id: String,
    val author: User,
    val category: IssueCategory,
    val description: String,
    /** Null for reports without a photo (e.g. a quick "I felt unsafe here"). */
    val imageUrl: String? = null,
    val location: GeoLocation,
    /** Capture time, epoch milliseconds (UTC). */
    val capturedAt: Long,
    /** Part of the day the report refers to; null = derive from [capturedAt]. */
    val timeOfDay: TimeOfDay? = null,
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
    val imageUrl: String? = null,
    val location: GeoLocation,
    val capturedAt: Long,
    val timeOfDay: TimeOfDay? = null,
)
