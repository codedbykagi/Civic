# API (v1)

Base URL (dev): `http://localhost:8080` — from the Android emulator use `http://10.0.2.2:8080`.
Paths are defined once in `shared/.../api/ApiRoutes.kt`.

| Method | Path                     | Status          | Description                     |
|--------|--------------------------|-----------------|---------------------------------|
| GET    | `/health`                | done            | Liveness check                  |
| GET    | `/api/v1/reports`        | done (in-memory)| Feed, newest first              |
| GET    | `/api/v1/reports/{id}`   | done (in-memory)| Single report                   |
| POST   | `/api/v1/reports`        | done (in-memory)| Create report (`CreateReportRequest`) |
| POST   | `/api/v1/media`          | stub            | Upload photo, returns URL       |
| GET    | `/api/v1/users/{id}`     | stub            | User profile                    |

Planned: upvote, comments, nearby search, auth (register/login).
