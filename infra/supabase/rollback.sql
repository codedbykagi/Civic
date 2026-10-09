-- ###########################################################################
-- ##  DESTRUCTIVE. This deletes every report, comment, upvote, profile and  ##
-- ##  every uploaded photo. There is no undo and no backup on the free      ##
-- ##  tier. Only run it to reset a project you are willing to lose.         ##
-- ##                                                                        ##
-- ##  Auth users are NOT deleted — remove those in Authentication → Users.  ##
-- ###########################################################################

-- Make sure the `vector` type resolves whichever schema pgvector lives in,
-- so the match_reports drop below can be parsed.
set search_path = public, extensions;

-- Trigger on auth.users first, so no new profile row can appear mid-teardown.
drop trigger if exists on_auth_user_created on auth.users;

-- Storage policies, then the bucket's objects, then the bucket.
drop policy if exists report_photos_select_public on storage.objects;
drop policy if exists report_photos_insert_own    on storage.objects;
drop policy if exists report_photos_update_own    on storage.objects;
drop policy if exists report_photos_delete_own    on storage.objects;

delete from storage.objects where bucket_id = 'report-photos';
delete from storage.buckets where id = 'report-photos';

-- Tables. `cascade` takes the table triggers, indexes and policies with them.
drop table if exists public.upvotes  cascade;
drop table if exists public.comments cascade;
drop table if exists public.reports  cascade;
drop table if exists public.profiles cascade;

-- Functions.
drop function if exists public.handle_new_user() cascade;
drop function if exists public.sync_report_upvotes() cascade;
drop function if exists public.sync_report_comment_count() cascade;
drop function if exists public.reports_nearby(double precision, double precision, double precision);
drop function if exists public.match_reports(vector, real, int);

-- Extensions are left in place: dropping pgcrypto or vector can break other
-- parts of the project. Drop them by hand if you really mean to.

-- Re-create everything with infra/supabase/schema.sql.
