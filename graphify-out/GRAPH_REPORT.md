# Graph Report - civic  (2026-10-09)

## Corpus Check
- 129 files · ~52,902 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 23 file(s) not represented in the graph (top: .xml 14, (none) 2, .properties 2)

## Summary
- 1419 nodes · 3903 edges · 72 communities (42 shown, 30 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 219 edges (avg confidence: 0.86)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `14f28acf`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- LocationProvider.kt
- ProfileScreen.kt
- Backend setup (Supabase, free tier)
- ReportDetailScreen.kt
- CivicNavHost.kt
- MapScreen.kt
- cloudCall
- geo
- MapScreen
- SignedInApp
- SafetyReportsScreen.kt
- HeatZoneOverlay
- ReportDao
- FeedViewModel.kt
- SyncManager
- GeoLocation
- RoutingFixtures.kt
- QuickReporterTest.kt
- RouteModels.kt
- CommentEntity
- MapViewModel.kt
- Report
- Account
- SafetyTag
- TimeOfDay
- FakeReportDao
- CloudHttp.kt
- SafeRoutePlanner
- AuthRepository
- CivicHeader.kt
- FakeTransport
- TimeOfDayClassifierTest
- ProfileViewModel.kt
- .build
- CivicDatabase
- Routing.kt
- HttpTransport.kt
- ZoneKind
- CloudDtos.kt
- Screen
- CommentDao
- .select
- IssueCategory
- ReportRepository
- ReportDetailViewModel
- RouteCandidate
- Format.kt
- CivicApplication.kt
- CreateReportScreen.kt
- Civic — Project Context
- Application.kt
- file
- ReportEntity
- friendlyMessage
- OnImageSavedCallback
- ApplicationTest.kt
- GoogleMapsHandoff
- .decode
- Tables.kt
- Helpline
- Postgres 16 Service
- Ktor Deployment Config (port 8080)
- Storage uploadDir (uploads)

## God Nodes (most connected - your core abstractions)
1. `GeoLocation` - 98 edges
2. `ReportEntity` - 74 edges
3. `IssueCategory` - 63 edges
4. `TimeOfDay` - 45 edges
5. `geo()` - 41 edges
6. `AvoidZone` - 36 edges
7. `SafeRoutePlanner` - 32 edges
8. `ReportDao` - 31 edges
9. `AuthRepository` - 30 edges
10. `ReportRepository` - 30 edges

## Surprising Connections (you probably didn't know these)
- `v0.2.0 Bug Fixes` --references--> `DraftStore`  [INFERRED]
  context.md → frontend/src/main/java/com/civic/app/data/DraftStore.kt
- `Core flow: posting a report` --references--> `AuthRepository`  [INFERRED]
  docs/ARCHITECTURE.md → frontend/src/main/java/com/civic/app/data/auth/AuthRepository.kt
- `DB Migration Policy (no destructive fallback)` --rationale_for--> `CivicDatabase`  [EXTRACTED]
  context.md → frontend/src/main/java/com/civic/app/data/local/CivicDatabase.kt
- `Report Status + Feed Filters` --references--> `ReportEntity`  [INFERRED]
  context.md → frontend/src/main/java/com/civic/app/data/local/ReportEntity.kt
- `Core flow: posting a report` --references--> `SyncManager`  [INFERRED]
  docs/ARCHITECTURE.md → frontend/src/main/java/com/civic/app/data/sync/SyncManager.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **v0.2.0 feature set** — context_report_status_and_filters, context_comments_feature, context_osm_map, context_v0_2_0_bug_fixes [EXTRACTED 1.00]
- **Local Postgres switch-over configuration** — backend_src_main_resources_application_database_config, infra_docker_compose_postgres, infra_docker_compose_civic_db [INFERRED 0.85]

## Communities (72 total, 30 thin omitted)

### Community 0 - "LocationProvider.kt"
Cohesion: 0.07
Nodes (18): graphify, Start here, Token budget (user wants minimal token use), Civic Project Context (context.md), Local Comments (Guest author), OpenStreetMap Map Tab (osmdroid pins), Prototype v0.2.0 (offline, Room DB v2), Report Status + Feed Filters (+10 more)

### Community 2 - "Backend setup (Supabase, free tier)"
Cohesion: 0.18
Nodes (10): 1. Create the account and project, 2. Copy the two keys the app needs, 3. Run the schema, 4. Turn on email auth, 5. Point the Android app at it, 6. Free-tier limits worth knowing, 7. When to outgrow this, 8. What we are not using, and why (+2 more)

### Community 3 - "ReportDetailScreen.kt"
Cohesion: 0.11
Nodes (22): AuthorLine(), initialsOf(), relativeTime(), ReportCard(), ReportPhotoStrip(), StatusBadge(), UpvoteButton(), categoryName() (+14 more)

### Community 5 - "MapScreen.kt"
Cohesion: 0.05
Nodes (8): hasPreciseLocation(), HelplineRow(), QuickUnsafePanel(), rememberQuickReportAction(), dial(), CameraPreview(), CaptureScreen(), ReportHubScreen()

### Community 6 - "cloudCall"
Cohesion: 0.24
Nodes (4): cloudAuth(), cloudCall(), CloudReportApi, ProfileApi

### Community 7 - "geo"
Cohesion: 0.25
Nodes (4): GoogleMapsHandoffTest, RouteScorerTest, geo(), path()

### Community 8 - "MapScreen"
Cohesion: 0.36
Nodes (9): ColorDot(), MapFilters(), MapScreen(), PinCard(), ReportsMap(), RouteCard(), RouteOptionRow(), whenLaidOut() (+1 more)

### Community 9 - "SignedInApp"
Cohesion: 0.07
Nodes (14): MainActivity, AuthFlow(), CivicNavHost(), navigateToTab(), SignedInApp(), AuthViewModel, AuthMessages(), AuthSubmitButton() (+6 more)

### Community 10 - "SafetyReportsScreen.kt"
Cohesion: 0.10
Nodes (5): PlaceholderScreen(), FeedScreen(), SafetyReportRow(), SafetyReportsScreen(), SafetyReportsViewModel

### Community 11 - "HeatZoneOverlay"
Cohesion: 0.08
Nodes (12): HeatZoneOverlay, OutlineStyle, DASHED, DOTTED, SOLID, ZoneStyle, createLayers(), MapEventsReceiver (+4 more)

### Community 13 - "FeedViewModel.kt"
Cohesion: 0.11
Nodes (7): FeedFilter, FeedViewModel, IssueStatus, ACKNOWLEDGED, IN_PROGRESS, REPORTED, RESOLVED

### Community 14 - "SyncManager"
Cohesion: 0.12
Nodes (11): CloudException, Done, Failed, Idle, OfflineOnly, Running, SyncManager, SyncStatus (+3 more)

### Community 15 - "GeoLocation"
Cohesion: 0.14
Nodes (8): ExcludeSelection, AvoidZone, RouteScorer, ZonePass, Provider, OSRM, VALHALLA, GeoLocation

### Community 17 - "QuickReporterTest.kt"
Cohesion: 0.12
Nodes (7): LocationSource, NoLocation, QuickReporter, QuickReportEvent, Saved, FakeLocation, QuickReporterTest

### Community 18 - "RouteModels.kt"
Cohesion: 0.27
Nodes (9): Failure, RouteOption, RoutePlan, RouteRole, ALTERNATIVE, FEWER_REPORTS, SHORTEST, RoutingResult (+1 more)

### Community 20 - "MapViewModel.kt"
Cohesion: 0.15
Nodes (10): ZoneInput, Destination, Failed, Idle, MapContent, MapFilter, MapViewModel, Planned (+2 more)

### Community 21 - "Report"
Cohesion: 0.14
Nodes (7): InMemoryReportRepository, ReportRepository, API (v1), Comment, CreateReportRequest, Report, User

### Community 22 - "Account"
Cohesion: 0.09
Nodes (12): Account, Active, CloudSession, SessionStore, toUsername(), AccountStateCard(), EditProfileDialog(), ProfileHeader() (+4 more)

### Community 23 - "SafetyTag"
Cohesion: 0.15
Nodes (10): decodeTags(), encodeTags(), SafetyModelTest, SafetyTag, DESERTED, DRINKING, HARASSMENT, NO_FOOTPATH (+2 more)

### Community 24 - "TimeOfDay"
Cohesion: 0.17
Nodes (10): TimeOfDayClassifier, ZoneRelevance, HeatZone, TimeOfDay, DAWN, EVENING, LATE_NIGHT, MIDDAY (+2 more)

### Community 26 - "CloudHttp.kt"
Cohesion: 0.13
Nodes (4): CloudConfig, decode(), ensureSuccess(), ApiClient

### Community 27 - "SafeRoutePlanner"
Cohesion: 0.32
Nodes (6): SafeRoutePlanner, offline(), osrmCoordinates(), valhallaError(), valhallaOk(), SafeRoutePlannerTest

### Community 29 - "AuthRepository"
Cohesion: 0.13
Nodes (8): AuthState, Loading, NeedsEmailConfirmation, NeedsOnboarding, SignedIn, SignUpOutcome, AuthRepository, toSession()

### Community 30 - "CivicHeader.kt"
Cohesion: 0.08
Nodes (3): Avatar(), CivicHeader(), ThemeSlideSwitch()

### Community 31 - "FakeTransport"
Cohesion: 0.19
Nodes (7): HttpResponseData, HttpTransport, KtorHttpTransport, ValhallaRouter, FakeTransport, Request, ValhallaRouterTest

### Community 32 - "TimeOfDayClassifierTest"
Cohesion: 0.11
Nodes (4): SolarCalculator, SunPosition, SunTimes, TimeOfDayClassifierTest

### Community 33 - "ProfileViewModel.kt"
Cohesion: 0.13
Nodes (5): AuthUiState, isPendingUpload(), ProfileStats, ProfileUiState, ProfileViewModel

### Community 36 - "Routing.kt"
Cohesion: 0.17
Nodes (6): configureRouting(), mediaRoutes(), reportRoutes(), userRoutes(), ReportService, ApiRoutes

### Community 38 - "ZoneKind"
Cohesion: 0.21
Nodes (8): Cell, Group, HeatZones, Member, ZoneKind, CHILDREN, OTHER, WOMEN

### Community 39 - "CloudDtos.kt"
Cohesion: 0.29
Nodes (13): AuthApi, CloudErrorBody, CommentDto, GoTrueUser, PasswordGrantBody, ProfileDto, ProfilePatch, RefreshGrantBody (+5 more)

### Community 40 - "Screen"
Cohesion: 0.10
Nodes (12): Capture, CreateReport, Feed, Map, Profile, ReportDetail, ReportHub, SafetyReports (+4 more)

### Community 43 - "IssueCategory"
Cohesion: 0.06
Nodes (20): AiAssistant, AiConfig, CategorySuggestion, NoAiAssistant, Draft, DraftStore, startReportWithoutPhoto(), SyncMappingTest (+12 more)

### Community 45 - "ReportDetailViewModel"
Cohesion: 0.17
Nodes (5): DetailState, Loaded, Loading, NotFound, ReportDetailViewModel

### Community 46 - "RouteCandidate"
Cohesion: 0.20
Nodes (5): OsrmFootRouter, RouteCandidate, RouterException, OsrmFootRouterTest, osrmOk()

### Community 48 - "CivicApplication.kt"
Cohesion: 0.21
Nodes (5): AppContainer, CivicApplication, CloudHttp, StorageApi, RoutingConfig

### Community 49 - "CreateReportScreen.kt"
Cohesion: 0.19
Nodes (6): CategoryPicker(), SafetyTagPicker(), TimeOfDayPicker(), CreateReportScreen(), LocationLine(), PhotoSection()

### Community 50 - "Civic — Project Context"
Cohesion: 0.22
Nodes (8): Accounts + backend (2026-10-09, v0.4.0), Civic — Project Context, Diagrams, graphify (knowledge graph), Layout (one Gradle build, three modules), Next TODOs, Status (as of 2026-10-09), What the app is

### Community 51 - "Application.kt"
Cohesion: 0.16
Nodes (7): main(), module(), DatabaseFactory, configureDatabase(), configureMonitoring(), configureSerialization(), configureStatusPages()

### Community 53 - "ReportEntity"
Cohesion: 0.18
Nodes (6): Accounts, Architecture, Core flow: posting a report, Roadmap, Why Supabase rather than the Ktor backend in `backend/`, ReportEntity

### Community 65 - "Tables.kt"
Cohesion: 0.70
Nodes (3): CommentsTable, ReportsTable, UsersTable

### Community 66 - "Helpline"
Cohesion: 0.40
Nodes (4): Helpline, CHILDREN, EMERGENCY, WOMEN

### Community 70 - "Postgres 16 Service"
Cohesion: 0.67
Nodes (3): Database Config (H2 default, env-overridable), civic-db Volume, Postgres 16 Service

## Knowledge Gaps
- **92 isolated node(s):** `AiConfig`, `Loading`, `NeedsOnboarding`, `SignedIn`, `NeedsEmailConfirmation` (+87 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 322 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **30 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `GeoLocation` connect `GeoLocation` to `LocationProvider.kt`, `MapScreen.kt`, `geo`, `MapScreen`, `HeatZoneOverlay`, `RoutingFixtures.kt`, `QuickReporterTest.kt`, `RouteModels.kt`, `MapViewModel.kt`, `Report`, `SafetyTag`, `TimeOfDay`, `SafeRoutePlanner`, `HeatZones.kt`, `.select`, `IssueCategory`, `ReportRepository`, `RouteCandidate`, `CreateReportScreen.kt`, `GoogleMapsHandoff`, `.decode`?**
  _High betweenness centrality (0.204) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `ReportEntity` (e.g. with `Report Status + Feed Filters` and `Core flow: posting a report`) actually correct?**
  _`ReportEntity` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `AiConfig`, `Loading`, `NeedsOnboarding` to the rest of the system?**
  _92 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `LocationProvider.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.06538461538461539 - nodes in this community are weakly interconnected._
- **Why does `ReportEntity` connect `ReportEntity` to `LocationProvider.kt`, `ReportDetailScreen.kt`, `MapScreen.kt`, `MapScreen`, `SafetyReportsScreen.kt`, `ReportDao`, `FeedViewModel.kt`, `SyncManager`, `QuickReporterTest.kt`, `CommentEntity`, `MapViewModel.kt`, `Account`, `SafetyTag`, `TimeOfDay`, `FakeReportDao`, `ProfileViewModel.kt`, `CivicDatabase`, `IssueCategory`, `ReportRepository`, `ReportDetailViewModel`, `CreateReportScreen.kt`?**
  _High betweenness centrality (0.119) - this node is a cross-community bridge._
- **Are the 37 inferred relationships involving `geo()` (e.g. with `ExcludePolygonsTest` and `.ignoresZonesFarFromEveryCandidate()`) actually correct?**
  _`geo()` has 37 INFERRED edges - model-reasoned connections that need verification._
- **Should `ProfileScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.14564564564564564 - nodes in this community are weakly interconnected._