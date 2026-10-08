package com.civic.backend.db

import org.jetbrains.exposed.sql.Table

object UsersTable : Table("users") {
    val id = varchar("id", 36)
    val username = varchar("username", 50).uniqueIndex()
    val displayName = varchar("display_name", 100)
    val avatarUrl = varchar("avatar_url", 500).nullable()
    val passwordHash = varchar("password_hash", 255)
    override val primaryKey = PrimaryKey(id)
}

object ReportsTable : Table("reports") {
    val id = varchar("id", 36)
    val authorId = varchar("author_id", 36).references(UsersTable.id)
    val category = varchar("category", 32)
    val description = text("description")
    val imageUrl = varchar("image_url", 500)
    val latitude = double("latitude")
    val longitude = double("longitude")
    val address = varchar("address", 255).nullable()
    val capturedAt = long("captured_at")
    val createdAt = long("created_at")
    val status = varchar("status", 32)
    val upvotes = integer("upvotes").default(0)
    override val primaryKey = PrimaryKey(id)
}

object CommentsTable : Table("comments") {
    val id = varchar("id", 36)
    val reportId = varchar("report_id", 36).references(ReportsTable.id)
    val authorId = varchar("author_id", 36).references(UsersTable.id)
    val text = text("text")
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(id)
}
