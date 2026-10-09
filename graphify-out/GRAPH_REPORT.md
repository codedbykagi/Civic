# Graph Report - civic  (2026-10-09)

## Corpus Check
- 116 files · ~98,971 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 1125 nodes · 2998 edges · 63 communities (36 shown, 27 thin omitted)
- Extraction: 95% EXTRACTED · 5% INFERRED · 0% AMBIGUOUS · INFERRED: 158 edges (avg confidence: 0.86)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- Map ViewModel & Time of Day
- Map Screen & Heat Zones
- Main Screens
- Location & Prototype Docs
- Solar Time Tests
- Report Detail & Create
- Quick Unsafe Panel & Hub
- Navigation Host
- Route Scoring Tests
- Theme & Dark/Light Mode
- Geo & Route Scoring
- Camera Capture
- Shared Models & Service
- Report Card & Formatting
- Categories & Drafts
- Valhalla Router
- Route Planning Models
- Backend Routes
- HTTP Transport
- Room Report DAO
- Backend App Module
- Safety Reports Screen
- Safe Route Planner
- Navigation Routes
- Report Repository
- OSRM Foot Router
- Exclude Polygons Tests
- Comments & Test Fakes
- Quick Reporter
- Fake Report DAO
- App Container & API
- Project Context Doc
- Polyline Codec
- Quick Reporter Tests
- Safety Tags
- Feed Filters
- Report Detail ViewModel
- Issue Status
- Ktor API Client
- Comment DAO
- Frontend Design Skill
- Claude Instructions
- Women & Child Safety
- Exposed Tables
- Feed Filter Tests
- Postgres Infra
- Ktor Deploy Config
- Upload Storage

## God Nodes (most connected - your core abstractions)
1. `GeoLocation` - 99 edges
2. `IssueCategory` - 58 edges
3. `ReportEntity` - 57 edges
4. `TimeOfDay` - 45 edges
5. `geo()` - 41 edges
6. `AvoidZone` - 36 edges
7. `SafeRoutePlanner` - 33 edges
8. `ReportRepository` - 28 edges
9. `Report` - 25 edges
10. `IssueStatus` - 25 edges

## Surprising Connections (you probably didn't know these)
- `Archify architecture diagram` --conceptually_related_to--> `ReportRepository`  [INFERRED]
  context.md → frontend/src/main/java/com/civic/app/data/repository/ReportRepository.kt
- `Layout (one Gradle build, three modules)` --references--> `GeoLocation`  [INFERRED]
  context.md → shared/src/main/kotlin/com/civic/shared/model/GeoLocation.kt
- `graphify (knowledge graph)` --references--> `IssueCategory`  [INFERRED]
  context.md → shared/src/main/kotlin/com/civic/shared/model/IssueCategory.kt
- `Layout (one Gradle build, three modules)` --references--> `IssueCategory`  [INFERRED]
  context.md → shared/src/main/kotlin/com/civic/shared/model/IssueCategory.kt
- `Core flow: posting a report` --references--> `IssueCategory`  [INFERRED]
  docs/ARCHITECTURE.md → shared/src/main/kotlin/com/civic/shared/model/IssueCategory.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Local Postgres switch-over configuration** — backend_src_main_resources_application_database_config, infra_docker_compose_postgres, infra_docker_compose_civic_db [INFERRED 0.85]

## Communities (63 total, 27 thin omitted)

### Community 0 - "Map ViewModel & Time of Day"
Cohesion: 0.06
Nodes (28): TimeOfDayClassifier, ZoneRelevance, Cell, Group, HeatZone, HeatZones, Member, ZoneInput (+20 more)

### Community 1 - "Map Screen & Heat Zones"
Cohesion: 0.07
Nodes (20): HeatZoneOverlay, OutlineStyle, DASHED, DOTTED, SOLID, ZoneStyle, ColorDot(), createLayers() (+12 more)

### Community 3 - "Location & Prototype Docs"
Cohesion: 0.06
Nodes (15): OpenStreetMap Map Tab (osmdroid pins), Prototype v0.2.0 (offline, Room DB v2), v0.2.0 Bug Fixes, Architecture, Core flow: posting a report, Roadmap ideas, CivicDatabase, LocationProvider (+7 more)

### Community 4 - "Solar Time Tests"
Cohesion: 0.11
Nodes (5): SolarCalculator, SunPosition, SunTimes, SafetyModelTest, TimeOfDayClassifierTest

### Community 5 - "Report Detail & Create"
Cohesion: 0.12
Nodes (15): Local Comments (Guest author), decodeTags(), CategoryPicker(), SafetyTagPicker(), TimeOfDayPicker(), CivicReportDetail(), CommentInput(), CommentRow() (+7 more)

### Community 6 - "Quick Unsafe Panel & Hub"
Cohesion: 0.09
Nodes (6): hasPreciseLocation(), HelplineRow(), QuickUnsafePanel(), rememberQuickReportAction(), dial(), ReportHubScreen()

### Community 7 - "Navigation Host"
Cohesion: 0.08
Nodes (8): Theme slide switch (header, top right), CivicHeader(), ThemeSlideSwitch(), CivicNavHost(), navigateToTab(), QuickReportVisuals, FeedScreen(), ProfileScreen()

### Community 9 - "Route Scoring Tests"
Cohesion: 0.21
Nodes (5): AvoidZone, GoogleMapsHandoffTest, RouteScorerTest, geo(), path()

### Community 10 - "Theme & Dark/Light Mode"
Cohesion: 0.11
Nodes (3): MainActivity, CivicTheme(), ThemePreference

### Community 11 - "Geo & Route Scoring"
Cohesion: 0.19
Nodes (4): GoogleMapsHandoff, RouteScorer, ZonePass, GeoLocation

### Community 12 - "Camera Capture"
Cohesion: 0.09
Nodes (4): PhotoStorage, CameraPreview(), OnImageSavedCallback, CaptureScreen()

### Community 13 - "Shared Models & Service"
Cohesion: 0.13
Nodes (7): InMemoryReportRepository, ReportRepository, ReportService, API (v1), CreateReportRequest, Report, User

### Community 14 - "Report Card & Formatting"
Cohesion: 0.16
Nodes (11): ReportCard(), StatusBadge(), categoryName(), formatCoords(), formatTime(), openAppSettings(), openInMaps(), openUrl() (+3 more)

### Community 15 - "Categories & Drafts"
Cohesion: 0.11
Nodes (15): Draft, DraftStore, startReportWithoutPhoto(), ExampleUnitTest, IssueCategory, FALLEN_TREE, FIRE, FLOODING (+7 more)

### Community 16 - "Valhalla Router"
Cohesion: 0.17
Nodes (5): ValhallaRouter, FakeTransport, Request, valhallaBody(), ValhallaRouterTest

### Community 17 - "Route Planning Models"
Cohesion: 0.12
Nodes (13): Failure, RouteOption, RoutePlan, RouteRole, ALTERNATIVE, FEWER_REPORTS, SHORTEST, RoutingResult (+5 more)

### Community 18 - "Backend Routes"
Cohesion: 0.16
Nodes (5): mediaRoutes(), reportRoutes(), userRoutes(), ApplicationTest, ApiRoutes

### Community 19 - "HTTP Transport"
Cohesion: 0.13
Nodes (3): HttpResponseData, HttpTransport, KtorHttpTransport

### Community 21 - "Backend App Module"
Cohesion: 0.15
Nodes (8): main(), module(), DatabaseFactory, configureDatabase(), configureMonitoring(), configureRouting(), configureSerialization(), configureStatusPages()

### Community 22 - "Safety Reports Screen"
Cohesion: 0.13
Nodes (3): SafetyReportRow(), SafetyReportsScreen(), SafetyReportsViewModel

### Community 23 - "Safe Route Planner"
Cohesion: 0.35
Nodes (6): SafeRoutePlanner, offline(), osrmCoordinates(), valhallaError(), valhallaOk(), SafeRoutePlannerTest

### Community 24 - "Navigation Routes"
Cohesion: 0.11
Nodes (9): Capture, CreateReport, Feed, Map, Profile, ReportDetail, ReportHub, SafetyReports (+1 more)

### Community 26 - "OSRM Foot Router"
Cohesion: 0.20
Nodes (5): OsrmFootRouter, RouteCandidate, RouterException, OsrmFootRouterTest, osrmOk()

### Community 27 - "Exclude Polygons Tests"
Cohesion: 0.22
Nodes (3): ExcludePolygons, ExcludeSelection, ExcludePolygonsTest

### Community 30 - "Quick Reporter"
Cohesion: 0.19
Nodes (5): LocationSource, NoLocation, QuickReporter, QuickReportEvent, Saved

### Community 36 - "App Container & API"
Cohesion: 0.20
Nodes (4): AppContainer, CivicApplication, ReportApi, RoutingConfig

### Community 37 - "Project Context Doc"
Cohesion: 0.20
Nodes (10): Civic Project Context (context.md), Archify architecture diagram, Civic — Project Context, Git Bash JAVA_HOME / MSYS_NO_PATHCONV gotcha, graphify (knowledge graph), Layout (one Gradle build, three modules), Next TODOs (marked with `TODO` in code), Status (as of 2026-10-08) (+2 more)

### Community 41 - "Safety Tags"
Cohesion: 0.20
Nodes (8): encodeTags(), SafetyTag, DESERTED, DRINKING, HARASSMENT, NO_FOOTPATH, NO_TRANSPORT, POOR_LIGHTING

### Community 42 - "Feed Filters"
Cohesion: 0.22
Nodes (3): Report Status + Feed Filters, FeedFilter, FeedViewModel

### Community 43 - "Report Detail ViewModel"
Cohesion: 0.22
Nodes (5): DetailState, Loaded, Loading, NotFound, ReportDetailViewModel

### Community 44 - "Issue Status"
Cohesion: 0.25
Nodes (5): IssueStatus, ACKNOWLEDGED, IN_PROGRESS, REPORTED, RESOLVED

### Community 47 - "Frontend Design Skill"
Cohesion: 0.29
Nodes (6): Design principles, Frontend Design, Ground your designs in the subject matter, More on writing in design, Process: plan, review against the brief, build, critique, Restraint and self-critique

### Community 48 - "Claude Instructions"
Cohesion: 0.33
Nodes (3): graphify, Start here, Token budget (user wants minimal token use)

### Community 49 - "Women & Child Safety"
Cohesion: 0.33
Nodes (5): Women & child safety (v0.3.0), Helpline, CHILDREN, EMERGENCY, WOMEN

### Community 50 - "Exposed Tables"
Cohesion: 0.70
Nodes (3): CommentsTable, ReportsTable, UsersTable

### Community 53 - "Postgres Infra"
Cohesion: 0.67
Nodes (3): Database Config (H2 default, env-overridable), civic-db Volume, Postgres 16 Service

## Knowledge Gaps
- **69 isolated node(s):** `CreateReport`, `Feed`, `Map`, `Profile`, `ReportHub` (+64 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 271 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **27 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `GeoLocation` connect `Geo & Route Scoring` to `Map ViewModel & Time of Day`, `Map Screen & Heat Zones`, `Main Screens`, `Location & Prototype Docs`, `Report Detail & Create`, `Route Scoring Tests`, `Camera Capture`, `Shared Models & Service`, `Categories & Drafts`, `Valhalla Router`, `Route Planning Models`, `Safe Route Planner`, `Report Repository`, `OSRM Foot Router`, `Exclude Polygons Tests`, `Comments & Test Fakes`, `Quick Reporter`, `Routing Geometry`, `Planner & Handoff Support`, `Zone Math`, `Project Context Doc`, `Polyline Codec`, `Quick Reporter Tests`, `Polyline Math`?**
  _High betweenness centrality (0.277) - this node is a cross-community bridge._
- **Are the 3 inferred relationships involving `IssueCategory` (e.g. with `graphify (knowledge graph)` and `Layout (one Gradle build, three modules)`) actually correct?**
  _`IssueCategory` has 3 INFERRED edges - model-reasoned connections that need verification._
- **What connects `CreateReport`, `Feed`, `Map` to the rest of the system?**
  _69 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Map ViewModel & Time of Day` be split into smaller, more focused modules?**
  _Cohesion score 0.059720869847452125 - nodes in this community are weakly interconnected._
- **Why does `IssueCategory` connect `Categories & Drafts` to `Map ViewModel & Time of Day`, `Main Screens`, `Location & Prototype Docs`, `Report Detail & Create`, `Quick Unsafe Panel & Hub`, `Navigation Host`, `Header & Form Components`, `Theme & Dark/Light Mode`, `Shared Models & Service`, `Report Card & Formatting`, `Report Repository`, `ViewModel Factories`, `Comments & Test Fakes`, `Quick Reporter`, `Zone Math`, `Test Assertions`, `Project Context Doc`, `Safety Tags`, `Feed Filters`, `Feed Filter Tests`?**
  _High betweenness centrality (0.105) - this node is a cross-community bridge._
- **Are the 37 inferred relationships involving `geo()` (e.g. with `ExcludePolygonsTest` and `.ignoresZonesFarFromEveryCandidate()`) actually correct?**
  _`geo()` has 37 INFERRED edges - model-reasoned connections that need verification._
- **Should `Map Screen & Heat Zones` be split into smaller, more focused modules?**
  _Cohesion score 0.07183673469387755 - nodes in this community are weakly interconnected._