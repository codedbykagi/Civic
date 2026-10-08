package com.civic.app.data.remote

import com.civic.shared.api.ApiRoutes
import com.civic.shared.model.CreateReportRequest
import com.civic.shared.model.Report
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/** HTTP calls to the backend's report endpoints. */
class ReportApi(private val client: HttpClient) {

    suspend fun getFeed(): List<Report> = client.get(ApiRoutes.REPORTS).body()

    suspend fun createReport(request: CreateReportRequest): Report =
        client.post(ApiRoutes.REPORTS) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // TODO: uploadImage(file) -> URL via ApiRoutes.MEDIA (multipart)
}
