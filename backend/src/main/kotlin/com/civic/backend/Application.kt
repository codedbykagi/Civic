package com.civic.backend

import com.civic.backend.plugins.configureDatabase
import com.civic.backend.plugins.configureMonitoring
import com.civic.backend.plugins.configureRouting
import com.civic.backend.plugins.configureSerialization
import com.civic.backend.plugins.configureStatusPages
import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain

fun main(args: Array<String>) = EngineMain.main(args)

/** Entry module, referenced from application.yaml. */
fun Application.module() {
    configureSerialization()
    configureMonitoring()
    configureStatusPages()
    configureDatabase()
    // TODO: configureSecurity() — JWT auth for posting/commenting
    configureRouting()
}
