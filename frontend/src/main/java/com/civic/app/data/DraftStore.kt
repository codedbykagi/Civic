package com.civic.app.data

import com.civic.shared.model.GeoLocation

/** Photo + location + time captured on the camera screen, waiting to be turned into a post. */
data class Draft(
    val photoPath: String,
    val location: GeoLocation?,
    val capturedAt: Long,
)

/** Hands the draft from CaptureScreen to CreateReportScreen. */
class DraftStore {
    var current: Draft? = null
}
