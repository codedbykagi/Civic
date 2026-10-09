package com.civic.app.ai

import com.civic.app.BuildConfig
import com.civic.shared.model.IssueCategory
import java.io.File

/**
 * The seam for AI features. Nothing here is implemented yet — this file exists so the features we have discussed
 * have one obvious place to land, and so the data they need (a photo, a description, a location) is already
 * flowing to a single interface instead of being threaded through screens later.
 *
 * **Rule for whoever implements this: no model API key ships in the APK.** Anyone can unzip an APK and read its
 * strings, and a leaked key is billed to us. Calls go to a server we control — a Supabase Edge Function is the
 * free option already in the stack — which holds the key and forwards to the model. That server URL is
 * [AiConfig.proxyUrl], set from `local.properties` at build time.
 */
interface AiAssistant {

    /** Suggests a category (and how sure it is) from the photo and what the user typed, to pre-fill the form. */
    suspend fun suggestCategory(photo: File?, description: String): CategorySuggestion?

    /** One-line description of a photo, for accessibility and for reports filed without any text. */
    suspend fun describePhoto(photo: File): String?

    /**
     * Finds reports that look like the same real-world issue, so the feed can group "12 people reported this
     * pothole" instead of showing twelve cards. The server side is the `match_reports` function and the
     * `embedding` column in infra/supabase/schema.sql.
     */
    suspend fun findDuplicates(reportId: String): List<String>

    /** Plain-language summary of what is being reported in an area, for the map and for authorities. */
    suspend fun summarizeArea(descriptions: List<String>): String?
}

/** [confidence] is 0..1. The UI should pre-select rather than auto-submit below ~0.7. */
data class CategorySuggestion(val category: IssueCategory, val confidence: Float, val reason: String? = null)

/** Where the AI proxy lives, and whether there is one. */
object AiConfig {
    val proxyUrl: String = BuildConfig.AI_PROXY_URL
    val isConfigured: Boolean = proxyUrl.isNotBlank()
}

/**
 * The default: every call declines. Having a real object rather than a nullable [AiAssistant] means call sites
 * do not need null checks, and an AI feature that is switched off simply does not appear in the UI.
 */
object NoAiAssistant : AiAssistant {
    override suspend fun suggestCategory(photo: File?, description: String): CategorySuggestion? = null
    override suspend fun describePhoto(photo: File): String? = null
    override suspend fun findDuplicates(reportId: String): List<String> = emptyList()
    override suspend fun summarizeArea(descriptions: List<String>): String? = null
}
