package com.civic.backend.db

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init(url: String, driver: String, user: String, password: String) {
        Database.connect(url = url, driver = driver, user = user, password = password)
        transaction {
            // TODO: replace with proper migrations (e.g. Flyway) before production
            SchemaUtils.create(UsersTable, ReportsTable, CommentsTable)
        }
    }
}
