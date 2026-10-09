# Graph Report - civic  (2026-10-09)

## Corpus Check
- 102 files · ~32,465 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 19 file(s) not represented in the graph (top: .xml 12, .properties 2, (none) 1)

## Summary
- 1080 nodes · 2904 edges · 62 communities (36 shown, 26 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 167 edges (avg confidence: 0.86)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `2d01479a`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- ReportDetailScreen.kt
- ReportDao
- Layout (one Gradle build, three modules)
- CaptureScreen.kt
- Application.kt
- Prototype v0.2.0 (offline, Room DB v2)
- CivicNavHost.kt
- ReportRoutes.kt
- QuickReporterTest.kt
- Screen
- Core flow: posting a report
- Backend DB Tables
- HeatZoneOverlay
- Postgres 16 Service
- Ktor Deployment Config (port 8080)
- Storage uploadDir (uploads)
- ReportDetailViewModel
- FeedViewModel
- GeoLocation
- SafeRoutePlanner
- CreateReportScreen.kt
- TimeOfDayClassifierTest
- MapScreen.kt
- SafetyReportsScreen.kt
- HeatZones.kt
- SafetyTag
- ZoneKind
- ReportFields.kt
- .build
- ReportEntity
- MainActivity.kt
- TimeOfDay
- MapViewModel
- FakeReportDao
- Format.kt
- Report
- QuickReporter.kt
- CreateReportRequest
- IssueCategory
- CivicDatabase
- CommentEntity
- Routing.kt
- IssueStatus
- ApiClient.kt
- CivicApplication.kt
- DraftStore
- CommentDao
- LocationProvider
- CLAUDE.md
- ApplicationTest.kt
- Helpline
- OnImageSavedCallback

## God Nodes (most connected - your core abstractions)
1. `GeoLocation` - 99 edges
2. `IssueCategory` - 58 edges
3. `ReportEntity` - 57 edges
4. `TimeOfDay` - 45 edges
5. `geo()` - 41 edges
6. `AvoidZone` - 36 edges
7. `SafeRoutePlanner` - 32 edges
8. `ReportRepository` - 27 edges
9. `IssueStatus` - 25 edges
10. `Report` - 25 edges

## Surprising Connections (you probably didn't know these)
- `Next TODOs (marked with `TODO` in code)` --references--> `CaptureScreen()`  [INFERRED]
  context.md → frontend/src/main/java/com/civic/app/ui/screens/capture/CaptureScreen.kt
- `graphify (knowledge graph)` --references--> `ReportService`  [INFERRED]
  context.md → backend/src/main/kotlin/com/civic/backend/service/ReportService.kt
- `Layout (one Gradle build, three modules)` --references--> `AppContainer`  [INFERRED]
  context.md → frontend/src/main/java/com/civic/app/CivicApplication.kt
- `v0.2.0 Bug Fixes` --references--> `DraftStore`  [INFERRED]
  context.md → frontend/src/main/java/com/civic/app/data/DraftStore.kt
- `graphify (knowledge graph)` --references--> `ReportEntity`  [INFERRED]
  context.md → frontend/src/main/java/com/civic/app/data/local/ReportEntity.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **v0.2.0 feature set** — context_report_status_and_filters, context_comments_feature, context_osm_map, context_v0_2_0_bug_fixes [EXTRACTED 1.00]
- **Local Postgres switch-over configuration** — backend_src_main_resources_application_database_config, infra_docker_compose_postgres, infra_docker_compose_civic_db [INFERRED 0.85]

## Communities (62 total, 26 thin omitted)

### Community 0 - "ReportDetailScreen.kt"
Cohesion: 0.11
Nodes (12): ReportCard(), StatusBadge(), formatCoords(), formatTime(), statusOf(), CivicHeader(), CivicReportDetail(), CommentInput() (+4 more)

### Community 2 - "Layout (one Gradle build, three modules)"
Cohesion: 0.18
Nodes (8): Civic — Project Context, graphify (knowledge graph), Layout (one Gradle build, three modules), Next TODOs (marked with `TODO` in code), Status (as of 2026-10-08), What the app is, Comment, User

### Community 3 - "CaptureScreen.kt"
Cohesion: 0.11
Nodes (4): PhotoStorage, openAppSettings(), CameraPreview(), CaptureScreen()

### Community 4 - "Application.kt"
Cohesion: 0.16
Nodes (7): main(), module(), DatabaseFactory, configureDatabase(), configureMonitoring(), configureSerialization(), configureStatusPages()

### Community 6 - "Prototype v0.2.0 (offline, Room DB v2)"
Cohesion: 0.16
Nodes (12): Civic Project Context (context.md), Local Comments (Guest author), OpenStreetMap Map Tab (osmdroid pins), Prototype v0.2.0 (offline, Room DB v2), v0.2.0 Bug Fixes, Civic: prototype builds, Civic-v0.1.0-prototype.apk, Civic-v0.2.0-prototype.apk (+4 more)

### Community 7 - "CivicNavHost.kt"
Cohesion: 0.10
Nodes (3): CivicNavHost(), navigateToTab(), QuickReportVisuals

### Community 9 - "QuickReporterTest.kt"
Cohesion: 0.05
Nodes (6): PolylineCodec, ExampleUnitTest, FeedFilterTest, FakeLocation, QuickReporterTest, PolylineCodecTest

### Community 10 - "Screen"
Cohesion: 0.12
Nodes (9): Capture, CreateReport, Feed, Map, Profile, ReportDetail, ReportHub, SafetyReports (+1 more)

### Community 11 - "Core flow: posting a report"
Cohesion: 0.50
Nodes (3): Architecture, Core flow: posting a report, Roadmap ideas

### Community 12 - "Backend DB Tables"
Cohesion: 0.70
Nodes (3): CommentsTable, ReportsTable, UsersTable

### Community 13 - "HeatZoneOverlay"
Cohesion: 0.08
Nodes (12): HeatZoneOverlay, OutlineStyle, DASHED, DOTTED, SOLID, ZoneStyle, createLayers(), MapEventsReceiver (+4 more)

### Community 15 - "Postgres 16 Service"
Cohesion: 0.67
Nodes (3): Database Config (H2 default, env-overridable), civic-db Volume, Postgres 16 Service

### Community 21 - "ReportDetailViewModel"
Cohesion: 0.20
Nodes (5): DetailState, Loaded, Loading, NotFound, ReportDetailViewModel

### Community 22 - "FeedViewModel"
Cohesion: 0.20
Nodes (3): Report Status + Feed Filters, FeedFilter, FeedViewModel

### Community 23 - "GeoLocation"
Cohesion: 0.06
Nodes (31): ExcludePolygons, ExcludeSelection, GoogleMapsHandoff, OsrmFootRouter, AvoidZone, Failure, RouteCandidate, RouteOption (+23 more)

### Community 26 - "SafeRoutePlanner"
Cohesion: 0.07
Nodes (14): HttpResponseData, HttpTransport, KtorHttpTransport, SafeRoutePlanner, ValhallaRouter, FakeTransport, offline(), osrmCoordinates() (+6 more)

### Community 27 - "CreateReportScreen.kt"
Cohesion: 0.11
Nodes (6): hasPreciseLocation(), HelplineRow(), QuickUnsafePanel(), rememberQuickReportAction(), ProfileScreen(), ReportHubScreen()

### Community 28 - "TimeOfDayClassifierTest"
Cohesion: 0.10
Nodes (5): SolarCalculator, SunPosition, SunTimes, TimeOfDayClassifier, TimeOfDayClassifierTest

### Community 29 - "MapScreen.kt"
Cohesion: 0.10
Nodes (10): categoryName(), ColorDot(), MapFilters(), MapScreen(), PinCard(), ReportsMap(), RouteCard(), RouteOptionRow() (+2 more)

### Community 30 - "SafetyReportsScreen.kt"
Cohesion: 0.09
Nodes (5): PlaceholderScreen(), FeedScreen(), SafetyReportRow(), SafetyReportsScreen(), SafetyReportsViewModel

### Community 32 - "SafetyTag"
Cohesion: 0.15
Nodes (10): decodeTags(), encodeTags(), SafetyModelTest, SafetyTag, DESERTED, DRINKING, HARASSMENT, NO_FOOTPATH (+2 more)

### Community 33 - "ZoneKind"
Cohesion: 0.19
Nodes (9): Cell, Group, HeatZones, Member, ZoneKind, CHILDREN, OTHER, WOMEN (+1 more)

### Community 34 - "ReportFields.kt"
Cohesion: 0.18
Nodes (7): CategoryPicker(), SafetyTagPicker(), TimeOfDayPicker(), EditSafetyDetailsDialog(), CreateReportScreen(), LocationLine(), PhotoSection()

### Community 38 - "TimeOfDay"
Cohesion: 0.22
Nodes (9): ZoneRelevance, HeatZone, TimeOfDay, DAWN, EVENING, LATE_NIGHT, MIDDAY, MORNING (+1 more)

### Community 39 - "MapViewModel"
Cohesion: 0.15
Nodes (6): Failed, Idle, MapContent, MapViewModel, Planning, RouteState

### Community 41 - "Format.kt"
Cohesion: 0.18
Nodes (3): dial(), openInMaps(), openUrl()

### Community 42 - "Report"
Cohesion: 0.23
Nodes (3): InMemoryReportRepository, ReportRepository, Report

### Community 43 - "QuickReporter.kt"
Cohesion: 0.20
Nodes (5): LocationSource, NoLocation, QuickReporter, QuickReportEvent, Saved

### Community 44 - "CreateReportRequest"
Cohesion: 0.19
Nodes (3): API (v1), ReportApi, CreateReportRequest

### Community 45 - "IssueCategory"
Cohesion: 0.17
Nodes (11): IssueCategory, FALLEN_TREE, FIRE, FLOODING, GARBAGE, OTHER, POTHOLE, STREETLIGHT (+3 more)

### Community 48 - "Routing.kt"
Cohesion: 0.31
Nodes (5): configureRouting(), mediaRoutes(), reportRoutes(), userRoutes(), ReportService

### Community 50 - "IssueStatus"
Cohesion: 0.22
Nodes (5): IssueStatus, ACKNOWLEDGED, IN_PROGRESS, REPORTED, RESOLVED

### Community 52 - "CivicApplication.kt"
Cohesion: 0.36
Nodes (3): AppContainer, CivicApplication, RoutingConfig

### Community 53 - "DraftStore"
Cohesion: 0.43
Nodes (3): Draft, DraftStore, startReportWithoutPhoto()

### Community 56 - "CLAUDE.md"
Cohesion: 0.33
Nodes (3): graphify, Start here, Token budget (user wants minimal token use)

### Community 58 - "Helpline"
Cohesion: 0.40
Nodes (4): Helpline, CHILDREN, EMERGENCY, WOMEN

## Knowledge Gaps
- **65 isolated node(s):** `EMERGENCY`, `WOMEN`, `CHILDREN`, `FEWER_REPORTS`, `SHORTEST` (+60 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 245 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **26 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `GeoLocation` connect `GeoLocation` to `Layout (one Gradle build, three modules)`, `CaptureScreen.kt`, `MapViewModel.kt`, `QuickReporterTest.kt`, `HeatZoneOverlay`, `SafeRoutePlanner`, `CreateReportScreen.kt`, `MapScreen.kt`, `HeatZones.kt`, `SafetyTag`, `ReportFields.kt`, `ReportEntity`, `TimeOfDay`, `MapViewModel`, `Format.kt`, `Report`, `QuickReporter.kt`, `CreateReportRequest`, `LocationProvider.kt`, `DraftStore`, `LocationProvider`?**
  _High betweenness centrality (0.272) - this node is a cross-community bridge._
- **Are the 3 inferred relationships involving `IssueCategory` (e.g. with `graphify (knowledge graph)` and `Layout (one Gradle build, three modules)`) actually correct?**
  _`IssueCategory` has 3 INFERRED edges - model-reasoned connections that need verification._
- **What connects `EMERGENCY`, `WOMEN`, `CHILDREN` to the rest of the system?**
  _65 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `ReportDetailScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.10609756097560975 - nodes in this community are weakly interconnected._
- **Why does `IssueCategory` connect `IssueCategory` to `ReportDetailScreen.kt`, `Layout (one Gradle build, three modules)`, `MapViewModel.kt`, `CivicNavHost.kt`, `QuickReporterTest.kt`, `Core flow: posting a report`, `ReportDetailViewModel`, `FeedViewModel`, `CreateReportScreen.kt`, `TimeOfDayClassifierTest`, `SafetyReportsScreen.kt`, `HeatZones.kt`, `SafetyTag`, `ReportFields.kt`, `.build`, `ReportEntity`, `MainActivity.kt`, `TimeOfDay`, `Format.kt`, `Report`, `QuickReporter.kt`, `CreateReportRequest`, `DraftStore`?**
  _High betweenness centrality (0.129) - this node is a cross-community bridge._
- **Are the 3 inferred relationships involving `ReportEntity` (e.g. with `graphify (knowledge graph)` and `Report Status + Feed Filters`) actually correct?**
  _`ReportEntity` has 3 INFERRED edges - model-reasoned connections that need verification._
- **Should `CaptureScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.11067193675889328 - nodes in this community are weakly interconnected._