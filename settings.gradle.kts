pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "civic"

include(":frontend") // Android app (Jetpack Compose)
include(":backend")  // Ktor REST API server
include(":shared")   // Data models shared by frontend and backend
