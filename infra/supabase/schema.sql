-- Civic — Supabase schema (Postgres 15+, hosted).
--
-- Paste this whole file into the Supabase SQL editor and run it.
-- It is idempotent: re-running it is safe and will not drop data.
--
-- Caveat on re-runs: `create table if not exists` does NOT alter an existing
-- table. If you change a column or a check constraint here, either apply that
-- change by hand or reset the project with rollback.sql first.
--
-- Setup walkthrough: docs/BACKEND_SETUP.md
-- Teardown:          infra/supabase/rollback.sql


-- ---------------------------------------------------------------------------
-- 0. Extensions
--    pgcrypto -> gen_random_uuid() for table defaults (usually already on).
--    vector   -> pgvector, used only by the AI seam at the bottom of this file.
-- ---------------------------------------------------------------------------
create extension if not exists pgcrypto;
create extension if not exists vector;

-- The Supabase dashboard installs extensions into the "extensions" schema; a
-- bare `create extension` puts them in "public". Put both on the search path
-- for this session so `vector(384)` resolves either way.
set search_path = public, extensions;


-- ---------------------------------------------------------------------------
-- 1. profiles — one row per auth user. Public, non-sensitive identity only.
--    Created automatically by the handle_new_user() trigger in section 6.
-- ---------------------------------------------------------------------------
create table if not exists public.profiles (
    id           uuid primary key references auth.users (id) on delete cascade,
    username     text unique not null,
    display_name text not null,
    avatar_url   text,
    bio          text,
    created_at   timestamptz not null default now(),
    -- 3-20 chars, lowercase letters / digits / underscore
    constraint profiles_username_format check (username ~ '^[a-z0-9_]{3,20}$')
);


-- ---------------------------------------------------------------------------
-- 2. reports — civic issues (public) and women's/child safety reports (private).
--    `visibility` is the privacy switch; RLS in section 5 enforces it.
--    `category`, `status` and `time_of_day` hold the app's enum names as text
--    (IssueCategory / IssueStatus / TimeOfDay in shared/) — kept as text so a
--    new enum value in the app does not need a migration.
-- ---------------------------------------------------------------------------
create table if not exists public.reports (
    id            uuid primary key default gen_random_uuid(),
    author_id     uuid not null references public.profiles (id) on delete cascade,
    category      text not null,
    description   text not null default '',
    image_url     text,
    latitude      double precision,
    longitude     double precision,
    captured_at   timestamptz not null,
    created_at    timestamptz not null default now(),
    status        text not null default 'REPORTED',
    upvotes       integer not null default 0,
    comment_count integer not null default 0,
    time_of_day   text,
    tags          text[] not null default '{}',
    visibility    text not null default 'public'
                  check (visibility in ('public', 'private')),

    -- Sync idempotency. The phone sends "<install-uuid>:<room localId>", so a
    -- retried upload updates the same row instead of creating a duplicate.
    client_id     text,

    -- AI columns. Nothing writes these yet; see section 8.
    ai_category_suggestion text,
    ai_confidence          real,
    ai_summary             text,
    embedding              vector(384),

    unique (author_id, client_id)
);

-- Feed and filter indexes.
create index if not exists reports_created_at_idx on public.reports (created_at desc);
create index if not exists reports_author_id_idx  on public.reports (author_id);
create index if not exists reports_category_idx   on public.reports (category);
create index if not exists reports_status_idx     on public.reports (status);

-- The main feed only ever reads public rows, so keep a partial index for it.
create index if not exists reports_public_created_at_idx
    on public.reports (created_at desc)
    where visibility = 'public';


-- ---------------------------------------------------------------------------
-- 3. comments — threaded under a report, oldest first.
-- ---------------------------------------------------------------------------
create table if not exists public.comments (
    id         uuid primary key default gen_random_uuid(),
    report_id  uuid not null references public.reports (id) on delete cascade,
    author_id  uuid not null references public.profiles (id) on delete cascade,
    text       text not null,
    created_at timestamptz not null default now()
);

create index if not exists comments_report_created_idx
    on public.comments (report_id, created_at);


-- ---------------------------------------------------------------------------
-- 4. upvotes — the composite primary key is what enforces one vote per user
--    per report; there is no "unvote twice" bug to write in the client.
-- ---------------------------------------------------------------------------
create table if not exists public.upvotes (
    report_id  uuid references public.reports (id) on delete cascade,
    voter_id   uuid references public.profiles (id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (report_id, voter_id)
);


-- ---------------------------------------------------------------------------
-- 5. Row Level Security
--    The app ships the anon key, so RLS is the only thing protecting data.
--    Private safety reports are invisible to everyone but their author, and
--    that is decided here in the database, not in the Kotlin code.
-- ---------------------------------------------------------------------------
alter table public.profiles enable row level security;
alter table public.reports  enable row level security;
alter table public.comments enable row level security;
alter table public.upvotes  enable row level security;

-- profiles: readable by anyone (needed to show author names in the feed);
-- writable only by the owner.
drop policy if exists profiles_select_all  on public.profiles;
drop policy if exists profiles_insert_self on public.profiles;
drop policy if exists profiles_update_self on public.profiles;

create policy profiles_select_all
    on public.profiles for select
    using (true);

create policy profiles_insert_self
    on public.profiles for insert
    with check (id = auth.uid());

create policy profiles_update_self
    on public.profiles for update
    using (id = auth.uid())
    with check (id = auth.uid());

-- reports: public rows are world-readable; private rows only by their author.
drop policy if exists reports_select_visible on public.reports;
drop policy if exists reports_insert_own     on public.reports;
drop policy if exists reports_update_own     on public.reports;
drop policy if exists reports_delete_own     on public.reports;

create policy reports_select_visible
    on public.reports for select
    using (visibility = 'public' or author_id = auth.uid());

create policy reports_insert_own
    on public.reports for insert
    with check (author_id = auth.uid());

create policy reports_update_own
    on public.reports for update
    using (author_id = auth.uid())
    with check (author_id = auth.uid());

create policy reports_delete_own
    on public.reports for delete
    using (author_id = auth.uid());

-- comments: visible only when the parent report is visible to the caller.
drop policy if exists comments_select_visible_report on public.comments;
drop policy if exists comments_insert_own           on public.comments;
drop policy if exists comments_delete_own           on public.comments;

create policy comments_select_visible_report
    on public.comments for select
    using (
        exists (
            select 1 from public.reports r
            where r.id = report_id
              and (r.visibility = 'public' or r.author_id = auth.uid())
        )
    );

create policy comments_insert_own
    on public.comments for insert
    with check (
        author_id = auth.uid()
        and exists (
            select 1 from public.reports r
            where r.id = report_id
              and (r.visibility = 'public' or r.author_id = auth.uid())
        )
    );

create policy comments_delete_own
    on public.comments for delete
    using (author_id = auth.uid());

-- upvotes: readable when the parent report is; written only by the voter.
drop policy if exists upvotes_select_visible_report on public.upvotes;
drop policy if exists upvotes_insert_own            on public.upvotes;
drop policy if exists upvotes_delete_own            on public.upvotes;

create policy upvotes_select_visible_report
    on public.upvotes for select
    using (
        exists (
            select 1 from public.reports r
            where r.id = report_id
              and (r.visibility = 'public' or r.author_id = auth.uid())
        )
    );

-- Voting requires both being the voter and being able to see the report. The
-- visibility half matters because a foreign-key check bypasses RLS: without it,
-- someone who guessed a private report's UUID could insert a vote row against
-- it (they still could not read the report, but they could nudge its counter).
create policy upvotes_insert_own
    on public.upvotes for insert
    with check (
        voter_id = auth.uid()
        and exists (
            select 1 from public.reports r
            where r.id = report_id
              and (r.visibility = 'public' or r.author_id = auth.uid())
        )
    );

create policy upvotes_delete_own
    on public.upvotes for delete
    using (voter_id = auth.uid());

-- Table-level grants. Supabase usually sets these by default privileges; they
-- are repeated here so a fresh project is not missing them. RLS still decides
-- which rows each role actually sees.
grant usage on schema public to anon, authenticated;
grant select on public.profiles, public.reports, public.comments, public.upvotes
    to anon, authenticated;
grant insert, update, delete on public.profiles, public.reports, public.comments, public.upvotes
    to authenticated;


-- ---------------------------------------------------------------------------
-- 6. Triggers and functions
-- ---------------------------------------------------------------------------

-- 6a. New auth user -> profile row.
-- Username comes from the email local part, sanitised to ^[a-z0-9_]{3,20}$,
-- with a numeric suffix when taken. Display name comes from signup metadata.
-- This must never fail a signup, so the whole body is wrapped in an exception
-- handler: a broken profile can be repaired later, a blocked signup cannot.
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $handle_new_user$
declare
    base_name text;
    candidate text;
    suffix    int := 0;
    display   text;
begin
    base_name := lower(split_part(coalesce(new.email, ''), '@', 1));
    base_name := regexp_replace(base_name, '[^a-z0-9_]', '', 'g');

    -- Guarantee the 3-char minimum even for odd or missing emails.
    if length(base_name) < 3 then
        base_name := 'civic' || base_name;
    end if;
    base_name := left(base_name, 20);

    display := nullif(trim(new.raw_user_meta_data ->> 'display_name'), '');

    candidate := base_name;
    -- Bounded loop: append 1, 2, 3 ... until free. Bounded so signup can never
    -- hang on a pathological collision run.
    while suffix < 50
          and exists (select 1 from public.profiles p where p.username = candidate)
    loop
        suffix    := suffix + 1;
        candidate := left(base_name, 20 - length(suffix::text)) || suffix::text;
    end loop;

    -- Still taken after 50 tries: fall back to the user id, which is unique.
    if exists (select 1 from public.profiles p where p.username = candidate) then
        candidate := left('u' || replace(new.id::text, '-', ''), 20);
    end if;

    insert into public.profiles (id, username, display_name)
    values (new.id, candidate, coalesce(display, candidate))
    on conflict (id) do nothing;

    return new;
exception
    when others then
        raise warning 'handle_new_user: profile not created for % (%)', new.id, sqlerrm;
        return new;
end;
$handle_new_user$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
    after insert on auth.users
    for each row execute function public.handle_new_user();

-- 6b. Keep reports.upvotes in step with the upvotes table.
create or replace function public.sync_report_upvotes()
returns trigger
language plpgsql
security definer
set search_path = public
as $sync_report_upvotes$
begin
    if tg_op = 'INSERT' then
        update public.reports
           set upvotes = upvotes + 1
         where id = new.report_id;
    elsif tg_op = 'DELETE' then
        update public.reports
           set upvotes = greatest(upvotes - 1, 0)
         where id = old.report_id;
    end if;
    return null;
end;
$sync_report_upvotes$;

drop trigger if exists upvotes_sync_count on public.upvotes;
create trigger upvotes_sync_count
    after insert or delete on public.upvotes
    for each row execute function public.sync_report_upvotes();

-- 6c. Keep reports.comment_count in step with the comments table.
create or replace function public.sync_report_comment_count()
returns trigger
language plpgsql
security definer
set search_path = public
as $sync_report_comment_count$
begin
    if tg_op = 'INSERT' then
        update public.reports
           set comment_count = comment_count + 1
         where id = new.report_id;
    elsif tg_op = 'DELETE' then
        update public.reports
           set comment_count = greatest(comment_count - 1, 0)
         where id = old.report_id;
    end if;
    return null;
end;
$sync_report_comment_count$;

drop trigger if exists comments_sync_count on public.comments;
create trigger comments_sync_count
    after insert or delete on public.comments
    for each row execute function public.sync_report_comment_count();


-- ---------------------------------------------------------------------------
-- 7. reports_nearby — public reports within `radius_m` metres, nearest first.
--    Plain haversine, no PostGIS needed. Called from the app as an RPC:
--      rpc("reports_nearby", { lat: .., lon: .., radius_m: .. })
--
--    This is a sequential scan plus trigonometry on every row. That is fine for
--    thousands of reports. Upgrade path when it gets slow: enable PostGIS, add
--    a `geography(Point, 4326)` column with a GiST index, and replace the body
--    with ST_DWithin / ST_Distance.
--
--    Deliberately NOT security definer: it runs with the caller's rights, so
--    the RLS policies in section 5 apply on top of the filter below.
-- ---------------------------------------------------------------------------
create or replace function public.reports_nearby(
    lat      double precision,
    lon      double precision,
    radius_m double precision
)
returns table (
    id            uuid,
    author_id     uuid,
    category      text,
    description   text,
    image_url     text,
    latitude      double precision,
    longitude     double precision,
    captured_at   timestamptz,
    created_at    timestamptz,
    status        text,
    upvotes       integer,
    comment_count integer,
    time_of_day   text,
    tags          text[],
    visibility    text,
    distance_m    double precision
)
language sql
stable
as $reports_nearby$
    with candidates as (
        select r.*,
               6371000 * 2 * asin(sqrt(
                   power(sin(radians(r.latitude - lat) / 2), 2)
                   + cos(radians(lat)) * cos(radians(r.latitude))
                     * power(sin(radians(r.longitude - lon) / 2), 2)
               )) as distance_m
          from public.reports r
         where r.visibility = 'public'
           and r.latitude is not null
           and r.longitude is not null
           -- Cheap bounding-box prefilter: 1 degree of latitude ~ 111320 m.
           and r.latitude between lat - (radius_m / 111320.0)
                             and lat + (radius_m / 111320.0)
    )
    select c.id,
           c.author_id,
           c.category,
           c.description,
           c.image_url,
           c.latitude,
           c.longitude,
           c.captured_at,
           c.created_at,
           c.status,
           c.upvotes,
           c.comment_count,
           c.time_of_day,
           c.tags,
           c.visibility,
           c.distance_m
      from candidates c
     where c.distance_m <= radius_m
     order by c.distance_m;
$reports_nearby$;

grant execute on function public.reports_nearby(double precision, double precision, double precision)
    to anon, authenticated;


-- ===========================================================================
-- 8. AI SEAM — not wired up yet.
--
--    reports.embedding is a vector(384) sized for a small sentence model
--    (e.g. all-MiniLM-L6-v2). NOTHING WRITES `embedding` TODAY, so
--    match_reports() returns no rows until something does.
--
--    Intended uses: duplicate detection when a new report is filed near an
--    existing one, and semantic search over descriptions.
-- ===========================================================================

-- Cosine similarity over report embeddings. `<=>` is pgvector's cosine
-- distance, so similarity = 1 - distance (1.0 = identical).
-- Invoker rights again, so RLS still hides private reports from other users.
create or replace function public.match_reports(
    query_embedding vector(384),
    match_threshold real,
    match_count     int
)
returns table (
    id          uuid,
    category    text,
    description text,
    similarity  real
)
language sql
stable
set search_path = public, extensions
as $match_reports$
    select r.id,
           r.category,
           r.description,
           (1 - (r.embedding <=> query_embedding))::real as similarity
      from public.reports r
     where r.embedding is not null
       and (1 - (r.embedding <=> query_embedding)) >= match_threshold
     order by r.embedding <=> query_embedding
     limit greatest(coalesce(match_count, 10), 0);
$match_reports$;

grant execute on function public.match_reports(vector, real, int)
    to anon, authenticated;

-- Approximate-nearest-neighbour index. Left commented out on purpose: ivfflat
-- builds its clusters from the rows present at creation time, so creating it on
-- an empty table gives bad recall. Create it once there are a few thousand
-- embeddings, with lists ~ sqrt(row count).
--
-- create index if not exists reports_embedding_cosine_idx
--     on public.reports using ivfflat (embedding vector_cosine_ops)
--     with (lists = 100);


-- ---------------------------------------------------------------------------
-- 9. Storage — the `report-photos` bucket holds report images.
--    Public read so the feed can show photos with no signed-URL round trip.
--    Writes are confined to a per-user folder: paths must start with the
--    uploader's auth.uid(), e.g. "<uid>/<report-id>.jpg".
-- ---------------------------------------------------------------------------
insert into storage.buckets (id, name, public)
values ('report-photos', 'report-photos', true)
on conflict (id) do nothing;

-- RLS is already enabled on storage.objects in a hosted Supabase project, and
-- the SQL editor role cannot enable it anyway, so only the policies go here.
drop policy if exists report_photos_select_public on storage.objects;
drop policy if exists report_photos_insert_own    on storage.objects;
drop policy if exists report_photos_update_own    on storage.objects;
drop policy if exists report_photos_delete_own    on storage.objects;

create policy report_photos_select_public
    on storage.objects for select
    using (bucket_id = 'report-photos');

create policy report_photos_insert_own
    on storage.objects for insert
    with check (
        bucket_id = 'report-photos'
        and (storage.foldername(name))[1] = auth.uid()::text
    );

create policy report_photos_update_own
    on storage.objects for update
    using (
        bucket_id = 'report-photos'
        and (storage.foldername(name))[1] = auth.uid()::text
    )
    with check (
        bucket_id = 'report-photos'
        and (storage.foldername(name))[1] = auth.uid()::text
    );

create policy report_photos_delete_own
    on storage.objects for delete
    using (
        bucket_id = 'report-photos'
        and (storage.foldername(name))[1] = auth.uid()::text
    );

-- Done. Check the Table editor for profiles / reports / comments / upvotes and
-- Storage for the report-photos bucket.
