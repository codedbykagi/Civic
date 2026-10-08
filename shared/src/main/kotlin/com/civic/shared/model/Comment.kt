package com.civic.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class Comment(
    val id: String,
    val reportId: String,
    val author: User,
    val text: String,
    val createdAt: Long,
)
