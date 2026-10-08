# Graph Report - civic  (2026-10-09)

## Corpus Check
- 26 files · ~9,839 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 494 nodes · 989 edges · 26 communities (14 shown, 12 thin omitted)
- Extraction: 97% EXTRACTED · 3% INFERRED · 0% AMBIGUOUS · INFERRED: 31 edges (avg confidence: 0.92)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- Compose UI Components
- Room Data Layer
- Backend Report Repository
- Camera & Platform Utils
- Backend App Bootstrap
- Feed & Detail ViewModels
- Project Docs & Agent Rules
- App Shell & Navigation
- Backend Routes
- Unit Tests
- Screen Routes
- Architecture & API Docs
- Backend DB Tables
- Map Event Handling
- Database Infra
- Ktor Deployment Config
- Upload Storage Config

## God Nodes (most connected - your core abstractions)
1. `ReportEntity` - 36 edges
2. `Report` - 22 edges
3. `FeedViewModel` - 20 edges
4. `ReportRepository` - 19 edges
5. `IssueCategory` - 15 edges
6. `CommentEntity` - 15 edges
7. `ReportDao` - 14 edges
8. `ReportDetailViewModel` - 14 edges
9. `ReportCard()` - 12 edges
10. `CreateReportRequest` - 11 edges

## Surprising Connections (you probably didn't know these)
- `API (v1)` --references--> `CreateReportRequest`  [INFERRED]
  docs/API.md → shared/src/main/kotlin/com/civic/shared/model/Report.kt
- `Next TODOs (marked with `TODO` in code)` --references--> `CaptureScreen()`  [INFERRED]
  context.md → frontend/src/main/java/com/civic/app/ui/screens/capture/CaptureScreen.kt
- `Core flow: posting a report` --references--> `IssueCategory`  [INFERRED]
  docs/ARCHITECTURE.md → shared/src/main/kotlin/com/civic/shared/model/IssueCategory.kt
- `Layout (one Gradle build, three modules)` --references--> `IssueStatus`  [INFERRED]
  context.md → shared/src/main/kotlin/com/civic/shared/model/IssueStatus.kt
- `Layout (one Gradle build, three modules)` --references--> `ApiRoutes`  [INFERRED]
  context.md → shared/src/main/kotlin/com/civic/shared/api/ApiRoutes.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Local Postgres switch-over configuration** — backend_src_main_resources_application_database_config, infra_docker_compose_postgres, infra_docker_compose_civic_db [INFERRED 0.85]
- **v0.2.0 feature set** — context_report_status_and_filters, context_comments_feature, context_osm_map, context_v0_2_0_bug_fixes [EXTRACTED 1.00]

## Communities (26 total, 12 thin omitted)

### Community 0 - "Compose UI Components"
Cohesion: 0.06
Nodes (15): ReportCard(), StatusBadge(), categoryName(), formatCoords(), formatTime(), statusOf(), CommentInput(), CommentRow() (+7 more)

### Community 1 - "Room Data Layer"
Cohesion: 0.07
Nodes (5): CommentDao, CommentEntity, ReportDao, ReportEntity, ReportRepository

### Community 2 - "Backend Report Repository"
Cohesion: 0.06
Nodes (23): InMemoryReportRepository, ReportRepository, ReportService, Civic — Project Context, graphify (knowledge graph), Layout (one Gradle build, three modules), Next TODOs (marked with `TODO` in code), Status (as of 2026-10-08) (+15 more)

### Community 3 - "Camera & Platform Utils"
Cohesion: 0.05
Nodes (8): PhotoStorage, Draft, DraftStore, openAppSettings(), openInMaps(), CameraPreview(), OnImageSavedCallback, CaptureScreen()

### Community 4 - "Backend App Bootstrap"
Cohesion: 0.06
Nodes (11): main(), module(), DatabaseFactory, configureDatabase(), configureMonitoring(), configureSerialization(), configureStatusPages(), AppContainer (+3 more)

### Community 5 - "Feed & Detail ViewModels"
Cohesion: 0.08
Nodes (8): Report Status + Feed Filters, DetailState, Loaded, Loading, NotFound, ReportDetailViewModel, FeedFilter, FeedViewModel

### Community 6 - "Project Docs & Agent Rules"
Cohesion: 0.07
Nodes (16): graphify, Start here, Token budget (user wants minimal token use), Civic Project Context (context.md), Local Comments (Guest author), OpenStreetMap Map Tab (osmdroid pins), Prototype v0.2.0 (offline, Room DB v2), v0.2.0 Bug Fixes (+8 more)

### Community 7 - "App Shell & Navigation"
Cohesion: 0.08
Nodes (5): MainActivity, CivicNavHost(), FeedScreen(), ProfileScreen(), CivicTheme()

### Community 8 - "Backend Routes"
Cohesion: 0.11
Nodes (7): configureRouting(), mediaRoutes(), reportRoutes(), userRoutes(), ApplicationTest, ReportApi, ApiRoutes

### Community 9 - "Unit Tests"
Cohesion: 0.13
Nodes (7): ExampleUnitTest, FeedFilterTest, IssueStatus, ACKNOWLEDGED, IN_PROGRESS, REPORTED, RESOLVED

### Community 10 - "Screen Routes"
Cohesion: 0.15
Nodes (7): Capture, CreateReport, Feed, Map, Profile, ReportDetail, Screen

### Community 11 - "Architecture & API Docs"
Cohesion: 0.20
Nodes (7): API (v1), Architecture, Core flow: posting a report, Roadmap ideas, Civic, Getting started, Project layout

### Community 12 - "Backend DB Tables"
Cohesion: 0.70
Nodes (3): CommentsTable, ReportsTable, UsersTable

### Community 15 - "Database Infra"
Cohesion: 0.67
Nodes (3): Database Config (H2 default, env-overridable), civic-db Volume, Postgres 16 Service

## Knowledge Gaps
- **35 isolated node(s):** `Database Config (H2 default, env-overridable)`, `civic-db Volume`, `Ktor Deployment Config (port 8080)`, `com.civic.backend.ApplicationKt.module`, `Storage uploadDir (uploads)` (+30 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 150 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **12 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `ReportEntity` connect `Room Data Layer` to `Compose UI Components`, `Backend Report Repository`, `Backend App Bootstrap`, `Feed & Detail ViewModels`, `Architecture & API Docs`?**
  _High betweenness centrality (0.241) - this node is a cross-community bridge._
- **Are the 3 inferred relationships involving `ReportEntity` (e.g. with `graphify (knowledge graph)` and `Report Status + Feed Filters`) actually correct?**
  _`ReportEntity` has 3 INFERRED edges - model-reasoned connections that need verification._
- **What connects `Database Config (H2 default, env-overridable)`, `civic-db Volume`, `Ktor Deployment Config (port 8080)` to the rest of the system?**
  _35 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Compose UI Components` be split into smaller, more focused modules?**
  _Cohesion score 0.05733397037744864 - nodes in this community are weakly interconnected._
- **Why does `AppContainer` connect `Backend App Bootstrap` to `Compose UI Components`, `Room Data Layer`, `Backend Report Repository`, `Camera & Platform Utils`, `Project Docs & Agent Rules`?**
  _High betweenness centrality (0.122) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `Report` (e.g. with `graphify (knowledge graph)` and `Layout (one Gradle build, three modules)`) actually correct?**
  _`Report` has 2 INFERRED edges - model-reasoned connections that need verification._
- **Should `Room Data Layer` be split into smaller, more focused modules?**
  _Cohesion score 0.07256894049346879 - nodes in this community are weakly interconnected._