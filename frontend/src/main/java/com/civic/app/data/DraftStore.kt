package com.civic.app.data

import com.civic.shared.model.GeoLocation
import com.civic.shared.model.IssueCategory
import java.io.File

/** Photo (optional) + location + time for a new report, waiting to be turned into a post. */
data class Draft(
    /** Null when reporting without a photo. */
    val photoPath: String?,
    val location: GeoLocation?,
    val capturedAt: Long,
    /** Category the form starts on (e.g. "unsafe" when the user chose to report without a photo). */
    val suggestedCategory: IssueCategory = IssueCategory.POTHOLE,
)

/** Hands the draft between the report hub, the camera and the post form. */
class DraftStore {
    var current: Draft? = null
        private set

    /** Starts a new draft, deleting the photo of an abandoned one so it doesn't pile up on disk. */
    fun replace(draft: Draft) {
        current?.photoPath?.let { if (it != draft.photoPath) File(it).delete() }
        current = draft
    }

    /** Adds or replaces the current draft's photo (deleting the old file); keeps its time and category. */
    fun setPhoto(path: String, location: GeoLocation?) {
        val draft = current ?: return replace(Draft(path, location, System.currentTimeMillis()))
        draft.photoPath?.let { if (it != path) File(it).delete() }
        current = draft.copy(photoPath = path, location = location ?: draft.location)
    }

    /** Remembers a position found by the form, so it survives a trip to the camera and back. */
    fun setLocation(location: GeoLocation) {
        current = current?.copy(location = location)
    }

    /** Call once the draft has been posted (its photo now belongs to the report). */
    fun clear() {
        current = null
    }
}
