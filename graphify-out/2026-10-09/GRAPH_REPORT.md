# Graph Report - civic  (2026-10-08)

## Corpus Check
- 21 files · ~7,036 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 379 nodes · 671 edges · 27 communities (14 shown, 13 thin omitted)
- Extraction: 96% EXTRACTED · 4% INFERRED · 0% AMBIGUOUS · INFERRED: 28 edges (avg confidence: 0.92)
- Token cost: 0 input · 3,500 output

## Community Hubs (Navigation)
- Compose UI Components
- Room DAO & Feed State
- Report Data Access
- Camera Capture & Project Docs
- App Container & Location
- Backend HTTP Routes
- Server Bootstrap & Plugins
- Architecture & API Docs
- Activity & Theme
- Navigation Destinations
- Context Doc Sections
- Database Setup
- Database Tables
- Prototype Release Notes
- CLAUDE.md Rules
- Local Postgres Setup
- Ktor Deployment Config (port 8080)
- Storage uploadDir (uploads)

## God Nodes (most connected - your core abstractions)
1. `ReportEntity` - 24 edges
2. `Report` - 22 edges
3. `IssueCategory` - 15 edges
4. `FeedViewModel` - 14 edges
5. `ReportRepository` - 12 edges
6. `CreateReportRequest` - 11 edges
7. `ReportDao` - 11 edges
8. `ReportService` - 10 edges
9. `Layout (one Gradle build, three modules)` - 10 edges
10. `CivicNavHost()` - 9 edges

## Surprising Connections (you probably didn't know these)
- `API (v1)` --references--> `CreateReportRequest`  [INFERRED]
  docs/API.md → shared/src/main/kotlin/com/civic/shared/model/Report.kt
- `Next TODOs (marked with `TODO` in code)` --references--> `CaptureScreen()`  [INFERRED]
  context.md → frontend/src/main/java/com/civic/app/ui/screens/capture/CaptureScreen.kt
- `graphify (knowledge graph)` --references--> `Report`  [INFERRED]
  context.md → shared/src/main/kotlin/com/civic/shared/model/Report.kt
- `graphify (knowledge graph)` --references--> `ReportService`  [INFERRED]
  context.md → backend/src/main/kotlin/com/civic/backend/service/ReportService.kt
- `Layout (one Gradle build, three modules)` --references--> `ApiRoutes`  [INFERRED]
  context.md → shared/src/main/kotlin/com/civic/shared/api/ApiRoutes.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Local Postgres switch-over configuration** — backend_src_main_resources_application_database_config, infra_docker_compose_postgres, infra_docker_compose_civic_db [INFERRED 0.85]
- **Prototype capture-to-feed flow** — frontend_src_main_java_com_civic_app_ui_screens_capture_capturescreen_capturescreen, frontend_src_main_java_com_civic_app_location_locationprovider_locationprovider, frontend_src_main_java_com_civic_app_data_draftstore_draftstore, frontend_src_main_java_com_civic_app_ui_screens_report_createreportscreen_createreportscreen, frontend_src_main_java_com_civic_app_ui_screens_feed_feedscreen_feedscreen [INFERRED 0.95]

## Communities (27 total, 13 thin omitted)

### Community 0 - "Compose UI Components"
Cohesion: 0.06
Nodes (8): ReportCard(), formatCoords(), formatTime(), CivicNavHost(), FeedScreen(), MapScreen(), ProfileScreen(), CreateReportScreen()

### Community 1 - "Room DAO & Feed State"
Cohesion: 0.08
Nodes (5): Next TODOs (sync, auth, real map), ReportDao, ReportEntity, ReportRepository, FeedViewModel

### Community 2 - "Report Data Access"
Cohesion: 0.08
Nodes (14): InMemoryReportRepository, ReportRepository, Layout (one Gradle build, three modules), ReportApi, Comment, GeoLocation, IssueStatus, ACKNOWLEDGED (+6 more)

### Community 3 - "Camera Capture & Project Docs"
Cohesion: 0.06
Nodes (11): Build Environment (JDK 21 + gradlew assembleDebug), Offline-first Prototype (Room on device), Civic Project Context (context.md), PhotoStorage, CameraPreview(), OnImageSavedCallback, CaptureScreen(), Phone Install Steps (sideload debug APK) (+3 more)

### Community 4 - "App Container & Location"
Cohesion: 0.09
Nodes (6): AppContainer, CivicApplication, Draft, DraftStore, LocationProvider, openInMaps()

### Community 5 - "Backend HTTP Routes"
Cohesion: 0.13
Nodes (7): configureRouting(), mediaRoutes(), reportRoutes(), userRoutes(), ReportService, ApplicationTest, ApiRoutes

### Community 6 - "Server Bootstrap & Plugins"
Cohesion: 0.10
Nodes (8): main(), module(), DatabaseFactory, configureDatabase(), configureMonitoring(), configureSerialization(), configureStatusPages(), ApiClient

### Community 7 - "Architecture & API Docs"
Cohesion: 0.09
Nodes (16): API (v1), Architecture, Core flow: posting a report, Roadmap ideas, ExampleUnitTest, Civic, Getting started, Project layout (+8 more)

### Community 9 - "Navigation Destinations"
Cohesion: 0.17
Nodes (6): Capture, CreateReport, Feed, Map, Profile, Screen

### Community 10 - "Context Doc Sections"
Cohesion: 0.22
Nodes (6): Civic — Project Context, graphify (knowledge graph), Next TODOs (marked with `TODO` in code), Status (as of 2026-10-08), What the app is, PlaceholderScreen()

### Community 12 - "Database Tables"
Cohesion: 0.70
Nodes (3): CommentsTable, ReportsTable, UsersTable

### Community 13 - "Prototype Release Notes"
Cohesion: 0.40
Nodes (4): Civic: prototype builds, Install on your Android phone (Android 8.0+), Known limits (prototype), What to test

### Community 14 - "CLAUDE.md Rules"
Cohesion: 0.50
Nodes (3): graphify, Start here, Token budget (user wants minimal token use)

### Community 16 - "Local Postgres Setup"
Cohesion: 0.67
Nodes (3): Database Config (H2 default, env-overridable), civic-db Volume, Postgres 16 Service

## Knowledge Gaps
- **33 isolated node(s):** `Capture`, `CreateReport`, `Feed`, `Map`, `Profile` (+28 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 130 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **13 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `ReportEntity` connect `Room DAO & Feed State` to `Compose UI Components`, `Camera Capture & Project Docs`, `Architecture & API Docs`, `Context Doc Sections`, `Database Setup`?**
  _High betweenness centrality (0.185) - this node is a cross-community bridge._
- **Are the 3 inferred relationships involving `ReportEntity` (e.g. with `graphify (knowledge graph)` and `Offline-first Prototype (Room on device)`) actually correct?**
  _`ReportEntity` has 3 INFERRED edges - model-reasoned connections that need verification._
- **What connects `Capture`, `CreateReport`, `Feed` to the rest of the system?**
  _33 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Compose UI Components` be split into smaller, more focused modules?**
  _Cohesion score 0.0579476861167002 - nodes in this community are weakly interconnected._
- **Why does `AppContainer` connect `App Container & Location` to `Compose UI Components`, `Room DAO & Feed State`, `Report Data Access`, `Camera Capture & Project Docs`?**
  _High betweenness centrality (0.180) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `Report` (e.g. with `graphify (knowledge graph)` and `Layout (one Gradle build, three modules)`) actually correct?**
  _`Report` has 2 INFERRED edges - model-reasoned connections that need verification._
- **Should `Room DAO & Feed State` be split into smaller, more focused modules?**
  _Cohesion score 0.08130081300813008 - nodes in this community are weakly interconnected._