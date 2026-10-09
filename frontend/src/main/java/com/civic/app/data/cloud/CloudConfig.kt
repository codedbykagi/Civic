package com.civic.app.data.cloud

import com.civic.app.BuildConfig

/**
 * Where the shared backend lives, and whether there is one at all.
 *
 * The values come from the git-ignored `local.properties` at build time (see frontend/build.gradle.kts).
 * When they are blank the app is still fully usable: it keeps a device profile and a local feed, and every
 * cloud call is skipped rather than failing. That is what makes a no-backend APK shippable.
 */
object CloudConfig {
    val baseUrl: String = BuildConfig.SUPABASE_URL
    val anonKey: String = BuildConfig.SUPABASE_ANON_KEY

    /** True once a project URL and anon key are present, i.e. accounts and the shared feed can work. */
    val isConfigured: Boolean = baseUrl.isNotBlank() && anonKey.isNotBlank()

    /** GoTrue: sign-up, sign-in, token refresh. */
    val authUrl: String get() = "$baseUrl/auth/v1"

    /** PostgREST: one path per table. */
    val restUrl: String get() = "$baseUrl/rest/v1"

    /** Storage: report photos. */
    val storageUrl: String get() = "$baseUrl/storage/v1"

    /** Bucket created by infra/supabase/schema.sql. */
    const val PHOTO_BUCKET = "report-photos"
}
