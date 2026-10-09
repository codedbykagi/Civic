# Civic — Project Context

Kept short on purpose. For code details, query the graph (see CLAUDE.md) instead of reading source files.

## What the app is
A social-style Android app for reporting local issues (potholes, small fires, fallen trees, etc.).
A user takes a photo; the app tags it with GPS location and time, saves a record on the device, and posts it to a public feed.

## In progress (2026-10-09, v0.3.0 — women & child safety; uncommitted)
- Built: one-tap "felt unsafe" (Women/Children/Everyone) + launcher shortcuts + Undo snackbar (`safety/QuickReporter`); optional photo; sun-based time-of-day (`safety/time`); safety tags; private safety reports (not in feed; `ui/screens/safety`); heat zones by colour (`safety/zones`, Tol colours); walking routes with fewer reported zones via FOSSGIS Valhalla, OSRM fallback (`safety/routing`) + Google Maps URL hand-off; helplines 112/181/1098; DB v3 (`MIGRATION_2_3`, schema exported to `frontend/schemas`).
- Verified: 95 unit tests pass, lint clean, 2→3 migration on emulator kept reports + comment; one-tap reports work.
- Not yet done: live route request on emulator; adversarial review (stopped; partial results in workflow journal wf_9386d87e-c68); wrap >120-char lines; copy APK to `releases/`; `graphify update .`.
- Known: backend `ApplicationTest.healthCheckReturnsOk` fails on HEAD too (no `/health` route). Research notes on Google Maps viability are in this session's scratchpad (`research_google-maps.md`) — pending the user's decision.

## Status (as of 2026-10-09)
- **Prototype APK built:** `releases/Civic-v0.2.0-prototype.apk` (debug-signed, versionCode 2). Install steps are in `releases/README.md`.
- **What the prototype does:** it works offline, with everything stored in Room on the phone (DB v2).
  - CameraX capture, tagged with GPS (fresh fix, falling back to last known position) and the time
  - Post form: category chips + description
  - Feed: photo, status badge, location, time, upvote, open in Maps, delete (confirmed). Status and category filter chips (DAO query `observeFiltered`).
  - Report detail (`report/{id}`): status chips (Reported → Resolved) and comments (`CommentEntity`, author "Guest", deleted along with the report via cascade).
  - Map tab: osmdroid / OpenStreetMap with a pin per report. Tapping a pin shows a card (View report / Directions). Pins at the same spot overlap.
  - Profile: report counts, plus a resolved count
- **DB migrations:** `CivicDatabase.MIGRATION_1_2` adds `reports.status` and the `comments` table. `fallbackToDestructiveMigration` was removed so user data is never wiped; every future schema change needs a Migration. Verified on the emulator: upgrading from 0.1.0 kept all reports.
- **Bug fixes (0.2.0):**
  - Maps intent no longer crashes when no maps app is installed.
  - The GPS task resumes when cancelled, and the last-known lookup has a timeout.
  - A camera bind failure shows a toast instead of crashing.
  - A failed save re-enables the Post button.
  - Abandoned draft photos are deleted.
  - After a permission denial, the button opens Settings.
  - Lint is clean.
- **Not wired yet:** backend sync, auth.
- **Build environment:**
  - Android Studio 2026.2 is installed. SDK is at `%LOCALAPPDATA%\Android\Sdk`; API 35 and build-tools 34 were auto-installed by AGP.
  - Build with `JAVA_HOME=C:\Users\ar0hu\.jdks\jbr-21.0.11` and `.\gradlew.bat :frontend:assembleDebug`. In Git Bash with `MSYS_NO_PATHCONV=1` (which adb needs), JAVA_HOME must use the Windows form `C:\...`, otherwise gradlew fails. The APK lands in `frontend/build/outputs/apk/debug/`; copy it into `releases/`.
  - The Gradle wrapper is now generated. Modules target Java 17 bytecode via `jvmTarget`; there's no toolchain, because no JDK 17 is installed.
- **Deleted:** the original empty `Test.txt`.

## UI revamp (2026-10-09, branch `UI`, uncommitted)
- Dark by default: true black + white + oxblood red `#A4161A`. Light: white + black + signal red `#E10600`. Tokens, type scale (heavy tight headings) and shapes live in `ui/theme/Theme.kt`.
- `ui/components/CivicHeader.kt`: header on the 4 main tabs (wordmark + red pin dot) with `ThemeSlideSwitch` at top right. The choice is saved in SharedPreferences (`ThemePreference`), and the status-bar icons follow it (`MainActivity`).
- Red nav indicator. Status badges: Reported = red, Resolved = inverse. Report cards are flat with an outline. The Feed shows no duplicate "Civic" title.
- Verified: build, lint (only the existing `CreateReportScreen.kt:87` warning) and unit tests pass; both modes checked on the emulator.
- Left as is: the Women/Children/Everyone and zone colours (they match the map legend); red text buttons are low-contrast on black in dark mode.

## Diagrams
- Architecture overview (archify, at commit 3a7c7ff): `.archify/architecture-civic-overview-20261009-142611/civic-overview.html`. Not gitignored.

## Layout (one Gradle build, three modules)
| Module | Stack | Status |
|---|---|---|
| `frontend/` (package `com.civic.app`) | Android app: Jetpack Compose, Room, Ktor client, CameraX, Fused Location | All screens work offline (Room). Screens: Feed, Map, Capture, CreateReport, ReportDetail, Profile. |
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
1. Upload pending reports (WorkManager `syncPending()`)
2. Reverse-geocode coordinates to an address
3. Image upload endpoint (`/api/v1/media`)
4. JWT auth and users
5. Exposed-backed repository to replace the in-memory one
6. Sync comments and status to the backend; nearby query; cluster overlapping map pins

## graphify (knowledge graph)
- **Built** 2026-10-08: 295 nodes, 479 edges, 18 communities. Cost: ~52k tokens, all spent reading the 5 doc files; code extraction is free.
- **Updated** 2026-10-08 with `/graphify . --update` after the prototype work: now 379 nodes, 671 edges, 27 communities. 9 edges loop back to their own node; nothing points to a missing node. This run cost ~3.5k tokens because the 3 changed docs were extracted inline, without a subagent. Most-connected nodes now: `ReportEntity`, `Report`, `IssueCategory`, `FeedViewModel`.
- **Updated** 2026-10-09 (v0.2.0 work): 494 nodes, 989 edges, 26 communities, 15 self-loops, nothing dangling. The 3 changed docs were extracted inline. Most-connected nodes: `ReportEntity`, `Report`, `FeedViewModel`, `ReportRepository`, `CommentEntity`.
- **Updated** 2026-10-09 (UI revamp): 1125 nodes, 2998 edges, 63 communities. Only `context.md` was extracted, inline. A new `.graphifyignore` skips `.agents/`, `.claude/`, `.archify/` and `skills-lock.json`.
- **Outputs:** `graphify-out/graph.html` (visual), `GRAPH_REPORT.md`, `graph.json`, plus a cache and a manifest so updates only re-process what changed.
- **Most-connected nodes:** `Report`, `PlaceholderScreen()`, `IssueCategory`, `ReportEntity`, `ReportService`.
- **Health warning:** 123 edges point to symbols outside this project (mostly library calls) and 5 edges loop back to their own node. Harmless.
- **Always-on:** `graphify claude install` was run. It added the graphify rules to `CLAUDE.md` and hooks to `.claude/settings.json`.
- **Keeping it current:** after code changes, run `graphify update .` (code only, no token cost). After changing docs, run `/graphify . --update` (this one costs tokens).
