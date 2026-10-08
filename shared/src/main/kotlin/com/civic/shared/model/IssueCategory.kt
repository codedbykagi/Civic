package com.civic.shared.model

import kotlinx.serialization.Serializable

@Serializable
enum class IssueCategory(val displayName: String) {
    POTHOLE("Pothole"),
    FIRE("Fire"),
    FALLEN_TREE("Fallen tree"),
    STREETLIGHT("Broken streetlight"),
    GARBAGE("Garbage / dumping"),
    FLOODING("Flooding"),
    OTHER("Other"),
}
