package com.civic.app.safety

import com.civic.app.data.local.ReportEntity
import com.civic.app.safety.time.TimeOfDayClassifier
import com.civic.shared.model.IssueCategory
import com.civic.shared.model.TimeOfDay

/** The user's explicit time-of-day tag, else the part of the day (by the sun at that place) it was captured in. */
val ReportEntity.effectiveTimeOfDay: TimeOfDay
    get() = timeOfDay?.let { tag -> TimeOfDay.entries.firstOrNull { it.name == tag } }
        ?: TimeOfDayClassifier.classify(capturedAt, latitude, longitude)

/** Category of a stored report, or null if the stored name is unknown (e.g. written by a newer app version). */
val ReportEntity.issueCategory: IssueCategory?
    get() = IssueCategory.entries.firstOrNull { it.name == category }

val ReportEntity.isSafety: Boolean
    get() = issueCategory?.isSafety == true
