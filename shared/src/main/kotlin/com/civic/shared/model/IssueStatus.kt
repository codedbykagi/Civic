package com.civic.shared.model

import kotlinx.serialization.Serializable

@Serializable
enum class IssueStatus {
    REPORTED,
    ACKNOWLEDGED,
    IN_PROGRESS,
    RESOLVED,
}
