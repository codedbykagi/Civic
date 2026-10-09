package com.civic.app.data.cloud

import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Reports, comments and upvotes over PostgREST.
 *
 * Nothing here filters for privacy: row-level security does that server-side, so a private safety report simply
 * is not in the response for anyone but its author. The client cannot leak what it never receives.
 */
class CloudReportApi(private val http: CloudHttp) {

    /** `profiles(...)` is a PostgREST embed: one round trip returns each report with its author. */
    private val reportSelect = "*,profiles(id,username,display_name,avatar_url,bio)"

    /** Newest public reports — the shared feed. */
    suspend fun feed(limit: Int = 100): List<ReportDto> = cloudCall {
        http.client.get("${CloudConfig.restUrl}/reports") {
            cloudAuth(http.bearer())
            url.parameters.append("select", reportSelect)
            url.parameters.append("visibility", "eq.public")
            url.parameters.append("order", "created_at.desc")
            url.parameters.append("limit", limit.toString())
        }.decode()
    }

    /** Everything the signed-in user wrote, private safety reports included. */
    suspend fun mine(userId: String, limit: Int = 200): List<ReportDto> = cloudCall {
        http.client.get("${CloudConfig.restUrl}/reports") {
            cloudAuth(http.bearer())
            url.parameters.append("select", reportSelect)
            url.parameters.append("author_id", "eq.$userId")
            url.parameters.append("order", "created_at.desc")
            url.parameters.append("limit", limit.toString())
        }.decode()
    }

    /**
     * Inserts, or updates in place when this device already sent that report: `client_id` is unique per author,
     * so a retry after a dropped connection cannot create a duplicate.
     */
    suspend fun upsert(report: ReportDto): ReportDto? = cloudCall {
        http.client.post("${CloudConfig.restUrl}/reports") {
            cloudAuth(http.bearer())
            url.parameters.append("on_conflict", "author_id,client_id")
            contentType(ContentType.Application.Json)
            header("Prefer", "return=representation,resolution=merge-duplicates")
            setBody(report)
        }.decode<List<ReportDto>>().firstOrNull()
    }

    suspend fun setStatus(reportId: String, status: String) = cloudCall {
        http.client.patch("${CloudConfig.restUrl}/reports") {
            cloudAuth(http.bearer())
            url.parameters.append("id", "eq.$reportId")
            contentType(ContentType.Application.Json)
            setBody(mapOf("status" to status))
        }.ensureSuccess()
    }

    suspend fun delete(reportId: String) = cloudCall {
        http.client.delete("${CloudConfig.restUrl}/reports") {
            cloudAuth(http.bearer())
            url.parameters.append("id", "eq.$reportId")
        }.ensureSuccess()
    }

    suspend fun comments(reportId: String): List<CommentDto> = cloudCall {
        http.client.get("${CloudConfig.restUrl}/comments") {
            cloudAuth(http.bearer())
            url.parameters.append("select", "*,profiles(id,username,display_name,avatar_url,bio)")
            url.parameters.append("report_id", "eq.$reportId")
            url.parameters.append("order", "created_at.asc")
        }.decode()
    }

    suspend fun addComment(comment: CommentDto): CommentDto? = cloudCall {
        http.client.post("${CloudConfig.restUrl}/comments") {
            cloudAuth(http.bearer())
            contentType(ContentType.Application.Json)
            header("Prefer", "return=representation")
            setBody(comment)
        }.decode<List<CommentDto>>().firstOrNull()
    }

    /** One row per (report, voter): the primary key is what stops a user upvoting twice. */
    suspend fun upvote(reportId: String, voterId: String) = cloudCall {
        http.client.post("${CloudConfig.restUrl}/upvotes") {
            cloudAuth(http.bearer())
            contentType(ContentType.Application.Json)
            header("Prefer", "resolution=ignore-duplicates")
            setBody(UpvoteDto(reportId, voterId))
        }.ensureSuccess()
    }

    suspend fun removeUpvote(reportId: String, voterId: String) = cloudCall {
        http.client.delete("${CloudConfig.restUrl}/upvotes") {
            cloudAuth(http.bearer())
            url.parameters.append("report_id", "eq.$reportId")
            url.parameters.append("voter_id", "eq.$voterId")
        }.ensureSuccess()
    }

    /** Which reports the user has already upvoted, so the feed can show the button as used. */
    suspend fun myUpvotes(voterId: String): List<String> = cloudCall {
        http.client.get("${CloudConfig.restUrl}/upvotes") {
            cloudAuth(http.bearer())
            url.parameters.append("voter_id", "eq.$voterId")
            url.parameters.append("select", "report_id,voter_id")
        }.decode<List<UpvoteDto>>().map { it.reportId }
    }
}
