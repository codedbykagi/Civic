# Graph Report - Civic  (2026-10-09)

## Corpus Check
- 102 files · ~51,706 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 1345 nodes · 3272 edges · 83 communities (61 shown, 15 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 211 edges (avg confidence: 0.85)
- Token cost: 24,000 input · 2,100 output

## Community Hubs (Navigation)
- Location & Draft Capture
- Profile & Auth Screens
- Supabase Backend & Schema
- Report Detail & Cards
- Navigation & Auth Gate
- Create Report & Camera
- Supabase HTTP Clients
- Routing Tests & Fixtures
- Map Screen & Theme
- Formatting & Activity Entry
- Feed & Safety Report Lists
- Heat Zone Overlay
- Report Storage (Room)
- Feed & Detail ViewModels
- Cloud Sync
- Route Scoring
- Test Assertions
- One-Tap Quick Reporting
- Safe Route Planning
- Comment Storage
- Map ViewModel & Routes
- Shared Report Model
- Account & Session
- Safety Tags
- Time of Day & Zone Relevance
- Fake DAOs for Tests
- Cloud Http
- Http Response Data
- Solar Calculator
- Auth Repository
- Zone Kind
- Fake Transport
- Time Of Day Classifier Test
- Profile View Model
- Heat Zones Test
- Civic Database
- Routing
- Http Transport
- Heat Zones
- Issue Category
- Screen
- Routing Fixtures
- Exclude Polygons
- Ai Assistant
- Report Repository
- Report Detail View Model
- Osrm Foot Router
- Auth View Model
- Civic Application
- Report Fields
- repo
- Application
- Exclude Polygons Test
- Issue Status
- friendly Message
- Solar Calculator
- Sync Mapping Test
- Polyline Codec
- Application Test
- Frontend  Design
- Safe Route Planner
- Frontend  Design
- Ktor Http Transport
- Google Maps Handoff
- Polyline Codec Test
- Tables
- Helpline
- Feed Filter Test
- Account Test
- gradlew
- Postgres 16  Service
- Ktor  Deployment  Config (port 8080
- API
- Storage upload Dir (uploads
- Http Client
- Report Repository

## God Nodes (most connected - your core abstractions)
1. `ReportEntity` - 67 edges
2. `IssueCategory` - 52 edges
3. `geo()` - 41 edges
4. `TimeOfDay` - 39 edges
5. `FakeReportDao` - 38 edges
6. `AvoidZone` - 33 edges
7. `ReportRepository` - 29 edges
8. `SafeRoutePlanner` - 29 edges
9. `ReportDao` - 28 edges
10. `AppContainer` - 25 edges

## Surprising Connections (you probably didn't know these)
- `Report Status + Feed Filters` --references--> `ReportEntity`  [INFERRED]
  context.md → frontend/src/main/java/com/civic/app/data/local/ReportEntity.kt
- `Report Status + Feed Filters` --references--> `FeedFilter`  [INFERRED]
  context.md → frontend/src/main/java/com/civic/app/ui/screens/feed/FeedViewModel.kt
- `OpenStreetMap Map Tab (osmdroid pins)` --references--> `MapScreen()`  [INFERRED]
  context.md → frontend/src/main/java/com/civic/app/ui/screens/map/MapScreen.kt
- `v0.2.0 Bug Fixes` --references--> `openInMaps()`  [INFERRED]
  context.md → frontend/src/main/java/com/civic/app/ui/Format.kt
- `Room is the source of truth` --shapes--> `SyncManager`  [EXTRACTED]
  docs/ARCHITECTURE.md → frontend/src/main/java/com/civic/app/data/sync/SyncManager.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **v0.2.0 feature set** — context_report_status_and_filters, context_comments_feature, context_osm_map, context_v0_2_0_bug_fixes [EXTRACTED 1.00]
- **Local Postgres switch-over configuration** — backend_src_main_resources_application_database_config, infra_docker_compose_postgres, infra_docker_compose_civic_db [INFERRED 0.85]

## Communities (83 total, 15 thin omitted)

### Community 0 - "Location & Draft Capture"
Cohesion: 0.06
Nodes (35): cancellationtokensource, graphify, Start here, Token Budget Rules, Token budget (user wants minimal token use), Update context.md + graph after every change, Civic Project Context (context.md), Git Bash JAVA_HOME / MSYS_NO_PATHCONV gotcha (+27 more)

### Community 1 - "Profile & Auth Screens"
Cohesion: 0.10
Nodes (42): alertdialog, arrowback, arrowforward, card, circularprogressindicator, collectasstate, edit, editnote (+34 more)

### Community 2 - "Supabase Backend & Schema"
Cohesion: 0.05
Nodes (38): auth.users, anon key ships, service_role never does, Cloud account, Device profile, Free Supabase projects pause when idle, Runs with no backend configured, Safety reports are author-only (RLS), Supabase backend (free tier) (+30 more)

### Community 3 - "Report Detail & Cards"
Cohesion: 0.12
Nodes (38): alignment, asyncimage, contentscale, Local Comments (Guest author), Dp, Avatar(), Modifier, AuthorLine() (+30 more)

### Community 4 - "Navigation & Auth Gate"
Cohesion: 0.08
Nodes (35): box, compositionlocalprovider, consumewindowinsets, currentbackstackentryasstate, findstartdestination, hasPreciseLocation(), HelplineRow(), Context (+27 more)

### Community 5 - "Create Report & Camera"
Cohesion: 0.08
Nodes (33): activityresultcontracts, addaphoto, androidview, background, button, buttondefaults, cameraselector, childcare (+25 more)

### Community 6 - "Supabase HTTP Clients"
Cohesion: 0.13
Nodes (16): AuthApi, CloudErrorBody, CommentDto, GoTrueUser, PasswordGrantBody, ProfileDto, RefreshGrantBody, ReportDto (+8 more)

### Community 7 - "Routing Tests & Fixtures"
Cohesion: 0.14
Nodes (6): AvoidZone, ExcludePolygonsTest, GoogleMapsHandoffTest, RouteScorerTest, geo(), path()

### Community 8 - "Map Screen & Theme"
Cohesion: 0.07
Nodes (29): boundingbox, circleshape, close, Color, configuration, copyrightoverlay, customzoombuttonscontroller, darkcolorscheme (+21 more)

### Community 9 - "Formatting & Activity Entry"
Cohesion: 0.09
Nodes (23): activitynotfoundexception, Bundle, ComponentActivity, date, enableedgetoedge, MainActivity, dial(), Context (+15 more)

### Community 10 - "Feed & Safety Report Lists"
Cohesion: 0.11
Nodes (27): arrangement, clickable, column, displayname, effectivetimeofday, experimentalmaterial3api, fillmaxsize, filterchip (+19 more)

### Community 11 - "Heat Zone Overlay"
Cohesion: 0.12
Nodes (23): Canvas, dashpatheffect, HeatZoneOverlay, MapView, createLayers(), MapEventsReceiver, drawRoute(), Context (+15 more)

### Community 12 - "Report Storage (Room)"
Cohesion: 0.11
Nodes (3): Flow, ReportDao, ReportEntity

### Community 13 - "Feed & Detail ViewModels"
Cohesion: 0.11
Nodes (18): addcircle, application_key, asstateflow, createsavedstatehandle, flatmaplatest, FeedViewModel, IssueStatus, StateFlow (+10 more)

### Community 14 - "Cloud Sync"
Cohesion: 0.11
Nodes (17): Idempotent sync via client_id, file, Context, PhotoStorage, Done, Failed, Idle, StateFlow (+9 more)

### Community 15 - "Route Scoring"
Cohesion: 0.20
Nodes (7): RouteRole, ALTERNATIVE, FEWER_REPORTS, SHORTEST, GeoLocation, RouteScorer, ZonePass

### Community 16 - "Test Assertions"
Cohesion: 0.15
Nodes (14): assertequals, assertfalse, assertnull, assertsame, asserttrue, dispatchers, fail, ExampleUnitTest (+6 more)

### Community 17 - "One-Tap Quick Reporting"
Cohesion: 0.17
Nodes (11): channel, Flow, NoLocation, QuickReporter, QuickReportEvent, Saved, FakeLocation, GeoLocation (+3 more)

### Community 18 - "Safe Route Planning"
Cohesion: 0.19
Nodes (13): Failure, RouteCandidate, RouteOption, RoutePlan, RoutingResult, Success, GeoLocation, T (+5 more)

### Community 19 - "Comment Storage"
Cohesion: 0.13
Nodes (5): CommentDao, Flow, CommentEntity, FakeCommentDao, Flow

### Community 20 - "Map ViewModel & Routes"
Cohesion: 0.14
Nodes (14): ZoneInput, Destination, Failed, Idle, GeoLocation, StateFlow, ViewModel, MapContent (+6 more)

### Community 21 - "Shared Report Model"
Cohesion: 0.14
Nodes (7): InMemoryReportRepository, ReportRepository, concurrenthashmap, ReportApi, CreateReportRequest, Report, User

### Community 22 - "Account & Session"
Cohesion: 0.11
Nodes (9): Account, CloudSession, NeedsEmailConfirmation, SignedIn, SignUpOutcome, SessionStore, toUsername(), SharedPreferences (+1 more)

### Community 23 - "Safety Tags"
Cohesion: 0.12
Nodes (11): decodeTags(), encodeTags(), GeoLocation, SafetyModelTest, SafetyTag, DESERTED, DRINKING, HARASSMENT (+3 more)

### Community 24 - "Time of Day & Zone Relevance"
Cohesion: 0.15
Nodes (11): TimeOfDayClassifier, ZoneRelevance, HeatZone, GeoLocation, TimeOfDay, DAWN, EVENING, LATE_NIGHT (+3 more)

### Community 26 - "Cloud Http"
Cohesion: 0.13
Nodes (16): body, buildconfig, contentnegotiation, defaultrequest, CloudConfig, decode(), ensureSuccess(), T (+8 more)

### Community 27 - "Http Response Data"
Cohesion: 0.27
Nodes (5): HttpResponseData, HttpTransport, valhallaError(), valhallaOk(), SafeRoutePlannerTest

### Community 28 - "Solar Calculator"
Cohesion: 0.16
Nodes (15): abs, acos, asin, assertnotnull, atan2, ceil, duration, hypot (+7 more)

### Community 29 - "Auth Repository"
Cohesion: 0.17
Nodes (7): AuthRepository, StateFlow, toSession(), ProfilePatch, CloudException, CloudHttp, Exception

### Community 30 - "Zone Kind"
Cohesion: 0.14
Nodes (10): FloatArray, ZoneKind, CHILDREN, OTHER, WOMEN, OutlineStyle, DASHED, DOTTED (+2 more)

### Community 31 - "Fake Transport"
Cohesion: 0.20
Nodes (5): GeoLocation, ValhallaRouter, FakeTransport, Request, ValhallaRouterTest

### Community 33 - "Profile View Model"
Cohesion: 0.17
Nodes (11): combine, Active, AuthState, Loading, NeedsOnboarding, isPendingUpload(), StateFlow, ViewModel (+3 more)

### Community 35 - "Civic Database"
Cohesion: 0.14
Nodes (10): DatabaseFactory, configureDatabase(), Room is the source of truth, DB Migration Policy (no destructive fallback), CivicDatabase, migration, RoomDatabase, schemautils (+2 more)

### Community 36 - "Routing"
Cohesion: 0.26
Nodes (11): configureRouting(), mediaRoutes(), reportRoutes(), userRoutes(), ReportService, httpstatuscode, receive, respond (+3 more)

### Community 37 - "Http Transport"
Cohesion: 0.27
Nodes (9): contenttype, delay, get, header, HttpClient, post, setbody, textcontent (+1 more)

### Community 38 - "Heat Zones"
Cohesion: 0.28
Nodes (5): E, Cell, Group, HeatZones, Member

### Community 39 - "Issue Category"
Cohesion: 0.14
Nodes (14): exp, floor, random, IssueCategory, FALLEN_TREE, FIRE, FLOODING, GARBAGE (+6 more)

### Community 40 - "Screen"
Cohesion: 0.12
Nodes (12): Capture, CreateReport, Feed, Map, Profile, ReportDetail, ReportHub, SafetyReports (+4 more)

### Community 41 - "Routing Fixtures"
Cohesion: 0.19
Nodes (11): addjsonobject, buildjsonobject, GeoLocation, offline(), osrmCoordinates(), valhallaBody(), intornull, ioexception (+3 more)

### Community 42 - "Exclude Polygons"
Cohesion: 0.26
Nodes (7): add, addjsonarray, buildjsonarray, ExcludePolygons, ExcludeSelection, GeoLocation, JsonArray

### Community 43 - "Ai Assistant"
Cohesion: 0.18
Nodes (5): No model API key in the APK, AiAssistant, AiConfig, CategorySuggestion, NoAiAssistant

### Community 44 - "Report Repository"
Cohesion: 0.21
Nodes (3): Flow, IssueStatus, ReportRepository

### Community 45 - "Report Detail View Model"
Cohesion: 0.15
Nodes (8): DetailState, IssueStatus, StateFlow, ViewModel, Loaded, Loading, NotFound, ReportDetailViewModel

### Community 46 - "Osrm Foot Router"
Cohesion: 0.27
Nodes (6): GeoLocation, OsrmFootRouter, Exception, RouterException, OsrmFootRouterTest, osrmOk()

### Community 47 - "Auth View Model"
Cohesion: 0.28
Nodes (4): AuthUiState, AuthViewModel, StateFlow, ViewModel

### Community 48 - "Civic Application"
Cohesion: 0.27
Nodes (7): Application, AppContainer, CivicApplication, StorageApi, RoutingConfig, room, supervisorjob

### Community 49 - "Report Fields"
Cohesion: 0.26
Nodes (11): check, experimentallayoutapi, filterchipdefaults, flowrow, CategoryPicker(), Modifier, SafetyTagPicker(), TimeOfDayPicker() (+3 more)

### Community 51 - "Application"
Cohesion: 0.24
Nodes (8): module(), configureMonitoring(), configureSerialization(), configureStatusPages(), calllogging, enginemain, install, level

### Community 52 - "Exclude Polygons Test"
Cohesion: 0.22
Nodes (7): contentornull, cos, double, JsonObject, jsonprimitive, pi, tan

### Community 53 - "Issue Status"
Cohesion: 0.20
Nodes (7): foreignkey, index, IssueStatus, ACKNOWLEDGED, IN_PROGRESS, REPORTED, RESOLVED

### Community 55 - "Solar Calculator"
Cohesion: 0.36
Nodes (3): SolarCalculator, SunPosition, SunTimes

### Community 57 - "Polyline Codec"
Cohesion: 0.36
Nodes (4): GeoLocation, PolylineCodec, pow, roundtolong

### Community 58 - "Application Test"
Cohesion: 0.32
Nodes (3): ApplicationTest, bodyastext, testapplication

### Community 59 - "Frontend  Design"
Cohesion: 0.29
Nodes (6): Design principles, Frontend Design, Ground your designs in the subject matter, More on writing in design, Process: plan, review against the brief, build, critique, Restraint and self-critique

### Community 60 - "Safe Route Planner"
Cohesion: 0.29
Nodes (6): cancellationexception, currentcoroutinecontext, ensureactive, min, roundtoint, withcontext

### Community 61 - "Frontend  Design"
Cohesion: 0.29
Nodes (6): Design principles, Frontend Design, Ground your designs in the subject matter, More on writing in design, Process: plan, review against the brief, build, critique, Restraint and self-critique

### Community 63 - "Google Maps Handoff"
Cohesion: 0.52
Nodes (3): GoogleMapsHandoff, GeoLocation, treeset

### Community 65 - "Tables"
Cohesion: 0.70
Nodes (4): CommentsTable, ReportsTable, UsersTable, Table

### Community 66 - "Helpline"
Cohesion: 0.40
Nodes (4): Helpline, CHILDREN, EMERGENCY, WOMEN

### Community 69 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 70 - "Postgres 16  Service"
Cohesion: 0.67
Nodes (3): Database Config (H2 default, env-overridable), civic-db Volume, Postgres 16 Service

## Knowledge Gaps
- **114 isolated node(s):** `Comment`, `GeoLocation`, `User`, `Database Config (H2 default, env-overridable)`, `civic-db Volume` (+109 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 363 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **15 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `AppContainer` connect `Civic Application` to `Location & Draft Capture`, `Navigation & Auth Gate`, `Create Report & Camera`, `Supabase HTTP Clients`, `Map Screen & Theme`, `Ai Assistant`, `Report Repository`, `Cloud Sync`, `One-Tap Quick Reporting`, `Safe Route Planning`, `Account & Session`, `Auth Repository`, `Ktor Http Transport`?**
  _High betweenness centrality (0.134) - this node is a cross-community bridge._
- **Why does `ReportEntity` connect `Report Storage (Room)` to `Location & Draft Capture`, `Supabase Backend & Schema`, `Report Detail & Cards`, `Create Report & Camera`, `Map Screen & Theme`, `Feed & Safety Report Lists`, `Heat Zone Overlay`, `Feed & Detail ViewModels`, `Cloud Sync`, `Test Assertions`, `One-Tap Quick Reporting`, `Comment Storage`, `Map ViewModel & Routes`, `Safety Tags`, `Time of Day & Zone Relevance`, `Fake DAOs for Tests`, `Profile View Model`, `Report Repository`, `Report Fields`, `repo`, `Issue Status`, `Sync Mapping Test`?**
  _High betweenness centrality (0.110) - this node is a cross-community bridge._
- **Why does `IssueCategory` connect `Issue Category` to `Location & Draft Capture`, `Report Detail & Cards`, `Navigation & Auth Gate`, `Create Report & Camera`, `Formatting & Activity Entry`, `Feed & Safety Report Lists`, `Feed & Detail ViewModels`, `Test Assertions`, `One-Tap Quick Reporting`, `Map ViewModel & Routes`, `Safety Tags`, `Time of Day & Zone Relevance`, `Profile View Model`, `Heat Zones Test`, `Heat Zones`, `Ai Assistant`, `Report Repository`, `Report Detail View Model`, `Report Fields`, `repo`, `Feed Filter Test`?**
  _High betweenness centrality (0.100) - this node is a cross-community bridge._
- **Are the 37 inferred relationships involving `geo()` (e.g. with `ExcludePolygonsTest` and `.ignoresZonesFarFromEveryCandidate()`) actually correct?**
  _`geo()` has 37 INFERRED edges - model-reasoned connections that need verification._
- **What connects `Comment`, `GeoLocation`, `User` to the rest of the system?**
  _114 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Location & Draft Capture` be split into smaller, more focused modules?**
  _Cohesion score 0.05959183673469388 - nodes in this community are weakly interconnected._
- **Should `Profile & Auth Screens` be split into smaller, more focused modules?**
  _Cohesion score 0.09990749306197964 - nodes in this community are weakly interconnected._