# Civic — Project Context

Kept short on purpose. For code details, query the graph (see CLAUDE.md) instead of reading source files.

## What the app is
A social-style Android app for reporting local issues (potholes, small fires, fallen trees, etc.).
A user takes a photo; the app tags it with GPS location and time, saves a record on the device, and posts it to a public feed.

## Status (as of 2026-10-08)
- **Prototype APK built:** `releases/Civic-v0.1.0-prototype.apk` (debug-signed). Install steps are in `releases/README.md`.
- **What the prototype does:** it works offline, with everything stored in Room on the phone.
  - CameraX capture, tagged with GPS (fresh fix, falling back to last known position) and the time
  - Post form: category chips + description
  - Feed: photo, location, time, upvote, open in Maps, delete
  - Map tab: list of located reports; tap opens Google Maps
  - Profile: report counts
- **Not wired yet:** backend sync, auth, comments, a real map.
- **Build environment:**
  - Android Studio 2026.2 is installed. SDK is at `%LOCALAPPDATA%\Android\Sdk`; API 35 and build-tools 34 were auto-installed by AGP.
  - Build with `JAVA_HOME=C:\Users\ar0hu\.jdks\jbr-21.0.11` and `.\gradlew.bat :frontend:assembleDebug`. The APK lands in `frontend/build/outputs/apk/debug/`; copy it into `releases/`.
  - The Gradle wrapper is now generated. Modules target Java 17 bytecode via `jvmTarget`; there's no toolchain, because no JDK 17 is installed.
- **Not set up yet:** git repo.
- **Deleted:** the original empty `Test.txt`.

## Layout (one Gradle build, three modules)
| Module | Stack | Status |
|---|---|---|
| `frontend/` (package `com.civic.app`) | Android app: Jetpack Compose, Room, Ktor client, CameraX, Fused Location | The feed screen works (loading, error and list states). Capture, CreateReport, Map and Profile are placeholder screens. |
| `backend/` (package `com.civic.backend`) | Ktor 3 server, Exposed, H2 (dev) / Postgres | GET feed, GET one report and POST report work, but data is kept in memory. `/media` and `/users` are stubs. DB tables are defined. |
| `shared/` (package `com.civic.shared`) | Plain Kotlin + kotlinx.serialization | Models: `Report`, `CreateReportRequest`, `User`, `Comment`, `GeoLocation`, `IssueCategory`, `IssueStatus`. `ApiRoutes` holds the endpoint paths. |
| `infra/` | docker-compose | Local Postgres 16 |
| `docs/` | Markdown | `ARCHITECTURE.md`, `API.md` |

- **Library versions:** all in `gradle/libs.versions.toml` (AGP 8.7.3, Kotlin 2.1.0, Ktor 3.0.3, compileSdk 35, minSdk 26, JDK 17).
- **Architecture:**
  - App: Screen → ViewModel → Repository → (Ktor API | Room).
  - Server: routes → service → repository → db.
- **Dependency wiring:** done by hand in `AppContainer`, inside `CivicApplication.kt`. There's no DI framework.
- **Backend address:** the app reaches it at `http://10.0.2.2:8080`, the address the emulator uses for the host machine.
- **Database config:** `backend/src/main/resources/application.yaml`. It defaults to H2; the `DATABASE_*` environment variables switch it to Postgres.

## Next TODOs (marked with `TODO` in code)
1. CameraX preview and capture in `CaptureScreen`; record GPS and time when the photo is taken
2. CreateReport form (category, description), saved to Room first, then uploaded
3. Image upload endpoint (`/api/v1/media`)
4. JWT auth and users
5. Exposed-backed repository to replace the in-memory one
6. Upvotes, comments, nearby query, map view

## graphify (knowledge graph)
- **Built** 2026-10-08: 295 nodes, 479 edges, 18 communities. Cost: ~52k tokens, all spent reading the 5 doc files; code extraction is free.
- **Updated** 2026-10-08 with `/graphify . --update` after the prototype work: now 379 nodes, 671 edges, 27 communities. 9 edges loop back to their own node; nothing points to a missing node. This run cost ~3.5k tokens because the 3 changed docs were extracted inline, without a subagent. Most-connected nodes now: `ReportEntity`, `Report`, `IssueCategory`, `FeedViewModel`.
- **Outputs:** `graphify-out/graph.html` (visual), `GRAPH_REPORT.md`, `graph.json`, plus a cache and a manifest so updates only re-process what changed.
- **Most-connected nodes:** `Report`, `PlaceholderScreen()`, `IssueCategory`, `ReportEntity`, `ReportService`.
- **Health warning:** 123 edges point to symbols outside this project (mostly library calls) and 5 edges loop back to their own node. Harmless.
- **Always-on:** `graphify claude install` was run. It added the graphify rules to `CLAUDE.md` and hooks to `.claude/settings.json`.
- **Keeping it current:** after code changes, run `graphify update .` (code only, no token cost). After changing docs, run `/graphify . --update` (this one costs tokens).
