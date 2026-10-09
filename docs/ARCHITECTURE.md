# Architecture

Room on the phone is the source of truth for every screen, and Supabase (hosted Postgres) is the shared copy.
That order matters: the app works with no signal, and syncing is something that happens *to* the local data
rather than something screens wait for.

```
 Android app (frontend)                              Supabase (hosted, free tier)
 ──────────────────────                              ────────────────────────────
 ui/screens  (Compose)                               GoTrue   /auth/v1    accounts
   │                                                 PostgREST /rest/v1   tables + RLS
 ViewModel  (state)                                  Storage  /storage/v1 report photos
   │
 data/repository ──► data/local (Room) ◄──┐
   │                      ▲               │
   │                      │          data/sync/SyncManager ──HTTPS──► data/cloud/*Api
 data/auth/AuthRepository ┘                              push pending, then pull feed
   │  (session in SharedPreferences, token refresh)
 ai/AiAssistant  (seam; no implementation yet)

      shared/  — Report, User, Comment, IssueCategory, SafetyTag, TimeOfDay
      backend/ — optional self-hosted Ktor server; not used by the app today
```

## Why Supabase rather than the Ktor backend in `backend/`
Row-level security is the reason. "A safety report is visible only to its author" is a sentence in
`infra/supabase/schema.sql`, enforced by Postgres on every query — the phone cannot leak what the server never
sends it. Writing that ourselves in `backend/` would mean writing auth, token refresh, storage and the policy
checks by hand, and paying to host it. The module stays in the repo for the day we need server-side jobs
(moderation, authority hand-off, the AI proxy); see `docs/BACKEND_SETUP.md`.

## Core flow: posting a report
1. **Capture** — CameraX takes a photo (optional for safety reports); `LocationProvider` gets GPS; timestamp recorded.
2. **Compose** — the user picks an `IssueCategory` and writes a description.
3. **Save locally** — `ReportRepository.addReport` stamps the author from `AuthRepository` and sets
   `visibility` (safety → `private`, always), then writes a `ReportEntity` to Room. The UI updates from Room, so
   this step is what "posting" means to the user — it never fails for being offline.
4. **Sync** — `SyncManager` uploads the photo to Storage, upserts the row by `client_id` (so a retry cannot
   duplicate it), and records the server id. Pushes happen before pulls, so local work is never overwritten.
5. **Feed** — the newest public rows are pulled into Room and rendered from there; the map reads the same table.

## Accounts
Two kinds of identity, and screens treat them the same:

| | Device profile | Cloud account |
|---|---|---|
| Created by | picking a display name | email + password via GoTrue |
| Id | `local:<install uuid>` | `auth.users` UUID |
| Reports | stay on the phone | sync, and survive a reinstall |
| Needs a backend | no | yes (`supabase.*` in `local.properties`) |

A build with no Supabase credentials still runs: `AuthRepository.isCloudAvailable` is false, the sign-in routes
are hidden, and everything works on-device. Signing in later re-attributes the reports written as a device
profile, so nothing written before an account existed is stranded.

## Roadmap
- Background sync (WorkManager) instead of sync on launch and on refresh
- Reverse-geocode coordinates to an address
- Cluster overlapping map pins; nearby query via `reports_nearby`
- AI: category suggestion, duplicate detection (`embedding` + `match_reports`), area summaries — behind a
  server-side proxy, never a key in the APK (see `ai/AiAssistant.kt`)
- Moderation / abuse reporting, and forwarding a report to a local authority
