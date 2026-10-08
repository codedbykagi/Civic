# Civic

A social-style Android app for reporting local issues (potholes, small fires, fallen trees, etc.).
Users snap a photo; the app tags it with **location and time**, keeps a record, and posts it to a public feed.

## Project layout

```
civic/
├── frontend/   Android app (Kotlin, Jetpack Compose)
│   └── src/main/java/com/civic/app/
│       ├── ui/          screens (feed, capture, report, map, profile), components, navigation, theme
│       ├── data/        remote (Ktor client), local (Room), repository
│       ├── location/    GPS via Fused Location Provider
│       └── camera/      photo capture / storage (CameraX)
├── backend/    REST API (Kotlin, Ktor + Exposed)
│   └── src/main/kotlin/com/civic/backend/
│       ├── plugins/     serialization, logging, errors, database, routing setup
│       ├── routes/      HTTP endpoints (reports, users, media)
│       ├── service/     business logic
│       ├── repository/  data access
│       └── db/          table definitions
├── shared/     Kotlin data models + API paths used by both frontend and backend
├── infra/      docker-compose for local Postgres
├── docs/       architecture + API notes
└── gradle/     version catalog (libs.versions.toml) + wrapper config
```

## Getting started

**Requirements:** Android Studio (latest stable; ships with JDK 17+), Android SDK 35.

1. Open the `civic` folder in Android Studio and let Gradle sync.
   - The Gradle wrapper JAR isn't included yet. If Android Studio doesn't create it, run `gradle wrapper` once
     (or copy `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` from any new Android Studio project).
2. **Backend:** run `./gradlew :backend:run` → http://localhost:8080/health (uses in-memory H2 by default).
3. **App:** select the `frontend` run configuration and launch on an emulator. It calls the backend at `10.0.2.2:8080`.

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) and [docs/API.md](docs/API.md).
