package com.civic.app.data

import com.civic.shared.model.GeoLocation
import java.io.File

/** Photo + location + time captured on the camera screen, waiting to be turned into a post. */
data class Draft(
    val photoPath: String,
    val location: GeoLocation?,
    val capturedAt: Long,
)

/** Hands the draft from CaptureScreen to CreateReportScreen. */
class DraftStore {
    var current: Draft? = null
        private set

    /** Sets a new draft, deleting the photo of an abandoned one so it doesn't pile up on disk. */
    fun replace(draft: Draft) {
        current?.let { if (it.photoPath != draft.photoPath) File(it.photoPath).delete() }
        current = draft
    }

    /** Call once the draft has been posted (its photo now belongs to the report). */
    fun clear() {
        current = null
    }
}
