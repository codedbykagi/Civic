package com.civic.backend.plugins

import com.civic.backend.db.DatabaseFactory
import io.ktor.server.application.Application

fun Application.configureDatabase() {
    val config = environment.config
    DatabaseFactory.init(
        url = config.property("database.url").getString(),
        driver = config.property("database.driver").getString(),
        user = config.property("database.user").getString(),
        password = config.property("database.password").getString(),
    )
}
