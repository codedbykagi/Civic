# Civic — Project Context

Kept short on purpose. For code details, query the graph (see CLAUDE.md) instead of reading source files.

## What the app is
A social-style Android app for reporting local issues (potholes, small fires, fallen trees, etc.).
A user takes a photo; the app tags it with GPS location and time, saves a record on the device, and posts it to a public feed.

## Accounts + backend (2026-10-09, v0.4.0)
- **Database choice: Supabase** (hosted Postgres, free tier). Reasons: row-level security enforces "a safety report
  is visible only to its author" in the database rather than in app code; auth, photo storage and `pgvector` come
  with it; nothing to host or keep awake. The `backend/` Ktor module stays for future server-side jobs but the app
  does not use it. Rationale and the rejected alternatives are in `docs/ARCHITECTURE.md`.
- **No new Gradle dependencies.** Supabase is reached over the Ktor client the app already had (`data/cloud/`:
  GoTrue auth, PostgREST tables, Storage uploads), which keeps the APK lean and avoids SDK version risk.
- **Built:** two kinds of identity (`data/auth/`) — a device profile (name picked on the phone, works with no
  backend) and a cloud account (email + password, token refresh, profile edit). Onboarding / sign-in / sign-up
  screens (`ui/screens/auth/`), a real Profile screen with avatar, bio, stats and sync state, authored feed cards
  and comments, one-vote-per-user upvotes, and `data/sync/SyncManager` (push pending, then pull; idempotent via
  `client_id`). Credentials come from the git-ignored `local.properties` (see `local.properties.example`).
- **Graceful degradation is the design rule:** with no `supabase.*` keys, `AuthRepository.isCloudAvailable` is
  false, the sign-in routes are hidden and everything works on-device. That is what `releases/Civic-v0.4.0-accounts.apk`
  is. Signing in later adopts the reports written as a device profile, so nothing earlier is stranded.
- **Server side:** `infra/supabase/schema.sql` (tables, RLS, triggers, `reports_nearby`, storage bucket; idempotent),
  `infra/supabase/rollback.sql`, and a 15-minute walkthrough in `docs/BACKEND_SETUP.md`. The schema was executed and
  its policies exercised against a real `pgvector/pgvector:pg16` container, including cross-user privacy checks.
- **AI seam only, no implementation:** `ai/AiAssistant.kt` (category suggestion, photo description, duplicate
  detection, area summary) with `NoAiAssistant` wired in, plus `embedding vector(384)` + a `match_reports` RPC
  fenced off in the schema. Hard rule written into the interface: no model API key in the APK — calls go through a
  server-side proxy (`ai.proxyUrl`), e.g. a Supabase Edge Function. Nothing is built yet, by design.
- **DB v4** (`MIGRATION_3_4`, schema exported): authors on reports and comments, remote photo URL, `visibility`,
  `upvotedByMe`, unique `remoteId` indexes. Additive, so existing rows survive; old safety reports are marked
  private. Verified by replaying the migration against SQLite with v3 data (reports kept, comments kept, FKs clean,
  duplicate `remoteId` rejected, many NULL ones allowed).
- **Verified:** 126 frontend unit tests + 2 backend tests pass, `lintDebug` clean, debug APK builds. A test caught a
  real bug: `Char.isLetterOrDigit()` accepts 'ë' and Devanagari, so a derived username could violate the server's
  check constraint — usernames are now ASCII-only with a fallback.
- **Fixed:** `ApplicationTest.healthCheckReturnsOk`, which failed on HEAD because `testApplication` loaded
  `application.yaml` and tried to start a database. It now configures routing only.
- **Not yet done:** nothing is wired to a real Supabase project (needs the user's URL + anon key); no background
  sync (WorkManager); no release signing key; the sign-in flow has not been exercised against a live server; avatar
  photo capture is a TODO in the profile edit dialog; `relativeTime`/`initialsOf` live in `ui/components/ReportCard.kt`
  and would be better in `ui/Format.kt`; 2 pre-existing >120-char lines in `CivicNavHost.kt`.

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
- **Accounts and sync:** see the v0.4.0 section above.
- **Build environment:**
  - Android Studio 2026.2 is installed. SDK is at `%LOCALAPPDATA%\Android\Sdk`; API 35 and build-tools 34 were auto-installed by AGP.
  - Build with `JAVA_HOME=C:\Users\ar0hu\.jdks\jbr-21.0.11` and `.\gradlew.bat :frontend:assembleDebug`. In Git Bash with `MSYS_NO_PATHCONV=1` (which adb needs), JAVA_HOME must use the Windows form `C:\...`, otherwise gradlew fails. The APK lands in `frontend/build/outputs/apk/debug/`; copy it into `releases/`.
  - The Gradle wrapper is now generated. Modules target Java 17 bytecode via `jvmTarget`; there's no toolchain, because no JDK 17 is installed.
- **Deleted:** the original empty `Test.txt`.

## Diagrams
- Architecture overview (archify, at commit 3a7c7ff): `.archify/architecture-civic-overview-20261009-142611/civic-overview.html`. Not gitignored.

## Layout (one Gradle build, three modules)
| Module | Stack | Status |
|---|---|---|
| `frontend/` (package `com.civic.app`) | Android app: Jetpack Compose, Room, Ktor client, CameraX, Fused Location | All screens work offline (Room). Screens: Feed, Map, Capture, CreateReport, ReportDetail, Profile. |
| `backend/` (package `com.civic.backend`) | Ktor 3 server, Exposed, H2 (dev) / Postgres | **Not used by the app** — Supabase replaced it. Kept for future server-side jobs (moderation, authority hand-off, the AI proxy). Feed/report routes work against an in-memory store. |
| `shared/` (package `com.civic.shared`) | Plain Kotlin + kotlinx.serialization | Models: `Report`, `CreateReportRequest`, `User`, `Comment`, `GeoLocation`, `IssueCategory`, `IssueStatus`. `ApiRoutes` holds the endpoint paths. |
| `infra/` | docker-compose + SQL | Local Postgres 16; `infra/supabase/` holds the hosted schema, RLS policies and teardown |
| `docs/` | Markdown | `ARCHITECTURE.md`, `API.md`, `BACKEND_SETUP.md` (free Supabase setup) |

- **Library versions:** all in `gradle/libs.versions.toml` (AGP 8.7.3, Kotlin 2.1.0, Ktor 3.0.3, compileSdk 35, minSdk 26, JDK 17).
- **Architecture:**
  - App: Screen → ViewModel → Repository → (Ktor API | Room).
  - Server: routes → service → repository → db.
- **Dependency wiring:** done by hand in `AppContainer`, inside `CivicApplication.kt`. There's no DI framework.
- **Backend address:** the app reaches it at `http://10.0.2.2:8080`, the address the emulator uses for the host machine.
- **Database config:** `backend/src/main/resources/application.yaml`. It defaults to H2; the `DATABASE_*` environment variables switch it to Postgres.

## Next TODOs
1. Point the app at a real Supabase project (user supplies URL + anon key) and exercise sign-up end to end
2. Background sync with WorkManager, instead of sync on launch and on the refresh button
3. Release signing key, so builds are updatable and Play-Store-ready
4. Reverse-geocode coordinates to an address
5. Cluster overlapping map pins; use the `reports_nearby` RPC
6. AI: decide the provider and stand up the proxy (see `ai/AiAssistant.kt`) — discussed but not started

## graphify (knowledge graph)
- **Built** 2026-10-08: 295 nodes, 479 edges, 18 communities. Cost: ~52k tokens, all spent reading the 5 doc files; code extraction is free.
- **Updated** 2026-10-08 with `/graphify . --update` after the prototype work: now 379 nodes, 671 edges, 27 communities. 9 edges loop back to their own node; nothing points to a missing node. This run cost ~3.5k tokens because the 3 changed docs were extracted inline, without a subagent. Most-connected nodes now: `ReportEntity`, `Report`, `IssueCategory`, `FeedViewModel`.
- **Updated** 2026-10-09 (v0.2.0 work): 494 nodes, 989 edges, 26 communities, 15 self-loops, nothing dangling. The 3 changed docs were extracted inline. Most-connected nodes: `ReportEntity`, `Report`, `FeedViewModel`, `ReportRepository`, `CommentEntity`.
- **Outputs:** `graphify-out/graph.html` (visual), `GRAPH_REPORT.md`, `graph.json`, plus a cache and a manifest so updates only re-process what changed.
- **Most-connected nodes:** `Report`, `PlaceholderScreen()`, `IssueCategory`, `ReportEntity`, `ReportService`.
- **Health warning:** 123 edges point to symbols outside this project (mostly library calls) and 5 edges loop back to their own node. Harmless.
- **Always-on:** `graphify claude install` was run. It added the graphify rules to `CLAUDE.md` and hooks to `.claude/settings.json`.
- **Keeping it current:** after code changes, run `graphify update .` (code only, no token cost). After changing docs, run `/graphify . --update` (this one costs tokens).
