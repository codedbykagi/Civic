# Graph Report - civic  (2026-10-08)

## Corpus Check
- Corpus is ~3,482 words - fits in a single context window. You may not need a graph.

## Summary
- 295 nodes · 479 edges · 18 communities (10 shown, 8 thin omitted)
- Extraction: 97% EXTRACTED · 3% INFERRED · 0% AMBIGUOUS · INFERRED: 12 edges (avg confidence: 0.84)
- Token cost: 51,861 input · 0 output

## Community Hubs (Navigation)
- Compose UI Screens
- Architecture Docs & Config
- App & Server Bootstrap
- Report Data Access
- Room Offline Storage
- Backend HTTP Routes
- Tests & Issue Categories
- Activity & Theme
- Camera & Location
- Feed ViewModel
- Navigation Destinations
- Issue Status
- Database Tables

## God Nodes (most connected - your core abstractions)
1. `Report` - 26 edges
2. `PlaceholderScreen()` - 13 edges
3. `IssueCategory` - 12 edges
4. `ReportEntity` - 11 edges
5. `ReportService` - 9 edges
6. `ReportDao` - 9 edges
7. `ReportRepository` - 9 edges
8. `CivicNavHost()` - 9 edges
9. `FeedViewModel` - 9 edges
10. `CreateReportRequest` - 9 edges

## Surprising Connections (you probably didn't know these)
- `ViewModel (UI state)` --conceptually_related_to--> `Frontend Android App (Kotlin, Jetpack Compose)`  [INFERRED]
  docs/ARCHITECTURE.md → README.md
- `ReportCard()` --references--> `Report`  [EXTRACTED]
  frontend/src/main/java/com/civic/app/ui/components/ReportCard.kt → shared/src/main/kotlin/com/civic/shared/model/Report.kt
- `FeedUiState` --references--> `Report`  [EXTRACTED]
  frontend/src/main/java/com/civic/app/ui/screens/feed/FeedViewModel.kt → shared/src/main/kotlin/com/civic/shared/model/Report.kt
- `Backend Layers (routes -> service -> repository -> db)` --conceptually_related_to--> `Backend REST API (Ktor + Exposed)`  [INFERRED]
  docs/ARCHITECTURE.md → README.md
- `Backend REST API (Ktor + Exposed)` --references--> `com.civic.backend.ApplicationKt.module`  [INFERRED]
  README.md → backend/src/main/resources/application.yaml

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Report posting flow (capture, locate, save locally, upload, publish)** — readme_camerax, docs_architecture_locationprovider, docs_architecture_reportentity, docs_api_media_endpoint, docs_api_reports_endpoint [EXTRACTED 1.00]
- **Shared domain models used by frontend and backend** — docs_architecture_report, docs_architecture_user, docs_architecture_comment, docs_architecture_issuecategory, shared_src_main_kotlin_com_civic_shared_api_apiroutes [EXTRACTED 1.00]
- **Local Postgres switch-over configuration** — backend_src_main_resources_application_database_config, infra_docker_compose_postgres, infra_docker_compose_civic_db [INFERRED 0.85]

## Communities (18 total, 8 thin omitted)

### Community 0 - "Compose UI Screens"
Cohesion: 0.07
Nodes (8): PlaceholderScreen(), ReportCard(), CivicNavHost(), CaptureScreen(), FeedScreen(), MapScreen(), ProfileScreen(), CreateReportScreen()

### Community 1 - "Architecture Docs & Config"
Cohesion: 0.08
Nodes (32): Database Config (H2 default, env-overridable), Ktor Deployment Config (port 8080), com.civic.backend.ApplicationKt.module, Storage uploadDir (uploads), Civic API v1, CreateReportRequest, GET /health, POST /api/v1/media (stub) (+24 more)

### Community 2 - "App & Server Bootstrap"
Cohesion: 0.09
Nodes (11): main(), module(), DatabaseFactory, configureDatabase(), configureMonitoring(), configureSerialization(), configureStatusPages(), AppContainer (+3 more)

### Community 3 - "Report Data Access"
Cohesion: 0.11
Nodes (7): InMemoryReportRepository, ReportRepository, ReportApi, Comment, CreateReportRequest, Report, User

### Community 4 - "Room Offline Storage"
Cohesion: 0.12
Nodes (4): CivicDatabase, ReportDao, ReportEntity, ReportRepository

### Community 5 - "Backend HTTP Routes"
Cohesion: 0.19
Nodes (6): configureRouting(), mediaRoutes(), reportRoutes(), userRoutes(), ReportService, ApiRoutes

### Community 6 - "Tests & Issue Categories"
Cohesion: 0.12
Nodes (10): ApplicationTest, ExampleUnitTest, IssueCategory, FALLEN_TREE, FIRE, FLOODING, GARBAGE, OTHER (+2 more)

### Community 10 - "Navigation Destinations"
Cohesion: 0.17
Nodes (6): Capture, CreateReport, Feed, Map, Profile, Screen

### Community 11 - "Issue Status"
Cohesion: 0.33
Nodes (5): IssueStatus, ACKNOWLEDGED, IN_PROGRESS, REPORTED, RESOLVED

### Community 12 - "Database Tables"
Cohesion: 0.70
Nodes (3): CommentsTable, ReportsTable, UsersTable

## Knowledge Gaps
- **22 isolated node(s):** `Feed`, `Map`, `Capture`, `Profile`, `CreateReport` (+17 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 88 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **8 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Report` connect `Report Data Access` to `Compose UI Screens`, `Room Offline Storage`, `Backend HTTP Routes`, `Tests & Issue Categories`, `Camera & Location`, `Feed ViewModel`, `Issue Status`?**
  _High betweenness centrality (0.435) - this node is a cross-community bridge._
- **What connects `Feed`, `Map`, `Capture` to the rest of the system?**
  _22 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Compose UI Screens` be split into smaller, more focused modules?**
  _Cohesion score 0.07137254901960784 - nodes in this community are weakly interconnected._
- **Why does `ApiRoutes` connect `Backend HTTP Routes` to `Architecture Docs & Config`, `Report Data Access`?**
  _High betweenness centrality (0.205) - this node is a cross-community bridge._
- **Should `Architecture Docs & Config` be split into smaller, more focused modules?**
  _Cohesion score 0.07777777777777778 - nodes in this community are weakly interconnected._
- **Why does `ReportCard()` connect `Compose UI Screens` to `Report Data Access`?**
  _High betweenness centrality (0.117) - this node is a cross-community bridge._
- **Should `App & Server Bootstrap` be split into smaller, more focused modules?**
  _Cohesion score 0.09090909090909091 - nodes in this community are weakly interconnected._