# Architecture

```
 Android app (frontend)                 Ktor server (backend)            Storage
 ──────────────────────                 ─────────────────────            ───────
 ui/screens  (Compose)                  routes/      HTTP endpoints      Postgres (H2 in dev)
   │                                      │                              uploads/ (photos)
 ViewModel  (state)                     service/     business rules
   │                                      │
 data/repository ──► data/remote ─HTTP─► repository/  data access ─────► db/ (Exposed tables)
   │
 data/local (Room: on-device record, offline queue)

                 shared/  — Report, User, Comment, IssueCategory, ApiRoutes
                           (one definition used by both sides)
```

## Core flow: posting a report
1. **Capture**: CameraX takes a photo; `LocationProvider` gets GPS; timestamp recorded.
2. **Compose**: user picks an `IssueCategory` and writes a description.
3. **Save locally**: `ReportEntity` goes into Room so there's a record even when offline.
4. **Upload**: image → `POST /api/v1/media` → URL; then `POST /api/v1/reports`.
5. **Feed**: `GET /api/v1/reports` shows everyone's posts; map shows them by location.

## Roadmap ideas
- Auth (JWT) and user profiles
- Upvotes and comments
- Nearby query (lat/lng radius) and a map view
- Status updates (reported → resolved), and possibly forwarding to local authorities
- Moderation / abuse reporting
