package com.civic.shared.model

import kotlinx.serialization.Serializable

/**
 * What a report is about. Stored by name, so new values must only be appended.
 * The UNSAFE_* values are personal-safety reports ("I felt unsafe here"); a photo is optional for them.
 */
@Serializable
enum class IssueCategory(val displayName: String) {
    POTHOLE("Pothole"),
    FIRE("Fire"),
    FALLEN_TREE("Fallen tree"),
    STREETLIGHT("Broken streetlight"),
    GARBAGE("Garbage / dumping"),
    FLOODING("Flooding"),
    OTHER("Other"),
    UNSAFE_WOMEN("Unsafe for women"),
    UNSAFE_CHILDREN("Unsafe for children"),
    UNSAFE_GENERAL("Unsafe for everyone"),
    ;

    val isSafety: Boolean get() = this in SAFETY

    companion object {
        val SAFETY = listOf(UNSAFE_WOMEN, UNSAFE_CHILDREN, UNSAFE_GENERAL)
        val CIVIC = entries.filterNot { it in SAFETY }

        /** Safety categories first: the app's focus is women's and children's safety. */
        val DISPLAY_ORDER = SAFETY + CIVIC
    }
}
