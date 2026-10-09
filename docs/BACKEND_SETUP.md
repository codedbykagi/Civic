# Backend setup (Supabase, free tier)

Civic's backend is a Supabase project: Postgres for data, Supabase Auth for
accounts, Supabase Storage for report photos. Everything below is on the free
tier and needs no credit card.

Takes about 15 minutes. You need a browser and this repo checked out.

Scripts referenced here:

| File | Purpose |
|------|---------|
| `infra/supabase/schema.sql`   | Tables, RLS policies, triggers, RPCs, storage bucket. Idempotent. |
| `infra/supabase/rollback.sql` | Destructive teardown, to reset a broken project. |

---

## 1. Create the account and project

1. Go to <https://supabase.com> and sign up. "Continue with GitHub" is the
   fastest route; no card is asked for.
2. Click **New project**. Pick (or create) an organisation on the **Free** plan.
3. Fill in:
   - **Name**: `civic` (anything works).
   - **Database password**: click *Generate*, then **save it in a password
     manager**. Supabase shows it once. You do not need it for the Android app,
     but you need it for any direct Postgres connection (`psql`, the Ktor
     backend in `backend/`, a migration tool), and resetting it is a chore.
   - **Region**: `ap-south-1` (Mumbai) if you are in India. If it is not
     offered, use Singapore (`ap-southeast-1`). Region choice is permanent and
     is most of your latency.
4. Click **Create new project** and wait ~2 minutes for provisioning.

## 2. Copy the two keys the app needs

Go to **Project Settings → API**. Two values matter:

| Value | Looks like | Used for |
|-------|-----------|----------|
| **Project URL** | `https://abcdefghijkl.supabase.co` | Base URL for all requests |
| **anon public** key | long `eyJ...` JWT | The key the Android app ships with |

The hard rule:

- The **anon** key is *designed* to be public. It is in every Supabase web and
  mobile client. It grants no privileges by itself — Row Level Security
  decides what each caller may read and write. Shipping it in the APK is fine.
- The **`service_role`** key on the same page bypasses RLS entirely. It must
  **never** go into the Android app, a client-side config, a screenshot, or
  git. It belongs only in server-side environments. If it ever leaks, rotate it
  immediately from that page.

## 3. Run the schema

1. Open **SQL Editor** in the left sidebar, then **New query**.
2. Paste the entire contents of `infra/supabase/schema.sql` and click **Run**.
3. It should finish with "Success. No rows returned". Some `NOTICE` lines are
   normal.

Confirm it worked:

- **Table Editor** lists four tables: `profiles`, `reports`, `comments`,
  `upvotes`.
- **Storage** lists a bucket named `report-photos`.
- **Database → Functions** lists `handle_new_user`, `sync_report_upvotes`,
  `sync_report_comment_count`, `reports_nearby`, `match_reports`.
- **Authentication → Policies** shows policies on all four tables, and the
  "RLS enabled" badge on each.

The script is idempotent, so re-running it is safe. It does *not* alter tables
that already exist, though — if you edit a column or a constraint in the file
later, apply that change by hand or run `rollback.sql` first and start clean.

## 4. Turn on email auth

1. **Authentication → Providers → Email**. Make sure it is **enabled** (it is
   by default).
2. For a demo or a hackathon, turn **Confirm email** *off*. Signup then returns
   a usable session immediately instead of waiting on an inbox.
   - Trade-off: anyone can sign up with an address they do not own, and the
     free tier's built-in mailer is rate-limited to a handful of messages per
     hour anyway.
   - **Turn it back on before any real launch**, and attach your own SMTP
     provider at that point.
3. Leave the other providers alone. Google / phone sign-in needs extra setup
   the app does not use.

When a user signs up, the `on_auth_user_created` trigger writes a matching
`profiles` row automatically: username from the email local part (sanitised to
`[a-z0-9_]{3,20}`, with a numeric suffix if taken), display name from the
`display_name` signup metadata if the app sent one.

## 5. Point the Android app at it

Add two lines to **`local.properties`** at the repo root — the same file that
holds `sdk.dir`. It is git-ignored, so the keys stay off GitHub:

```properties
supabase.url=https://abcdefghijkl.supabase.co
supabase.anonKey=eyJhbGciOiJI...
```

Then **Sync Project with Gradle Files** and rebuild. Gradle reads these at
build time and bakes them into the app's config.

If these two lines are absent (a fresh clone, a CI build, a teammate without a
project), **the app still runs** — it falls back to on-device guest mode: Room
stores reports locally, nothing syncs, no sign-in. That is the intended
default, not an error state. Don't add placeholder values to "fix" it; a
half-valid URL produces confusing network errors instead of a clean fallback.

## 6. Free-tier limits worth knowing

| Limit | Free tier |
|-------|-----------|
| Database | 500 MB |
| Storage | 1 GB total (plenty of report photos if you compress on-device) |
| Monthly active users | 50,000 |
| Projects | 2 active, per organisation |
| Backups | none — treat free-tier data as expendable |
| Egress | 5 GB / month |

The one that will actually bite you: **a free project is paused after about a
week with no API activity.** A paused project returns connection errors, so the
app silently looks broken. Restore it from the dashboard — open the project and
click **Restore project**; it comes back with its data in a couple of minutes.
If you are demoing after a quiet stretch, open the project and check it is
running the day before.

## 7. When to outgrow this

Reasons to move: you want backups and no auto-pause, you are over 500 MB, or
you need server-side logic that does not fit in Postgres functions.

- **Supabase Pro** — daily backups, no pausing, bigger quotas. Same code, same
  keys; it is a billing change, not a migration.
- **Self-host the Ktor backend in `backend/`** against the same Postgres. The
  app already speaks a REST API shaped like `docs/API.md`, so the server can
  take over writes while Supabase keeps providing Postgres, Auth and Storage.
  `infra/docker-compose.yml` runs a local Postgres for developing that path.

## 8. What we are not using, and why

- **Firebase Storage** — now requires a billing account on the project even to
  stay inside the free quota. Supabase Storage does not.
- **Render / Fly.io free web services** — they sleep when idle and cold-start
  in 30+ seconds, which reads as a hang on a phone. Supabase's Postgres and
  Storage are not request-scheduled that way (the week-long pause in section 6
  is a different, slower clock).
- **A VM with Postgres on it** — nothing is free for long, and someone has to
  patch it.

## 9. Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| `new row violates row-level security policy for table "reports"` | Insert ran with `author_id` not equal to the signed-in user, or with no session at all (anon key only). | Sign in first, and set `author_id` to the current user's id. The same error on `profiles` usually means the signup trigger did not run — re-run section 3. |
| `401` / `JWT expired` / `invalid JWT` | Stale or wrong token: the anon key was copied from a different project, got truncated on paste, or the saved session expired. | Re-copy the **anon public** key from Project Settings → API (it is long — check the tail matches), sign out and in again. Never substitute `service_role` to make this go away. |
| Confirmation email never arrives | Free-tier mailer is rate-limited and often spam-filed. | Check spam; wait a few minutes; or just turn **Confirm email** off (section 4) for development. Already-registered addresses get no new mail. |
| Everything times out / "connection refused", app falls back to guest mode | Project auto-paused after ~1 week idle. | Open the dashboard and **Restore project**. |
| Storage upload returns `403` / `new row violates row-level security policy` on `storage.objects` | The object path does not start with the user's own id, or the upload was unauthenticated. | Upload to `"<auth.uid()>/<filename>"`. The first folder segment must equal the signed-in user's id — that is exactly what the policy checks. |
| `type "vector" does not exist` while running `schema.sql` | pgvector not enabled. | The script's `create extension if not exists vector;` handles it; if it failed, enable **vector** under Database → Extensions and re-run. |
| Reports post but nobody else sees them | They were created with `visibility = 'private'` (the default for safety reports). | Intentional. Private rows are readable only by their author, enforced by the `reports_select_visible` policy. |
