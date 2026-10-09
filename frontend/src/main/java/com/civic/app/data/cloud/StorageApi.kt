package com.civic.app.data.cloud

import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import java.io.File

/**
 * Report photos in Supabase Storage. Objects are stored under `<user id>/<file>`, which is what the storage
 * policies in schema.sql key on: a user can only write inside their own folder, while reads are public so any
 * phone can render the feed.
 */
class StorageApi(private val http: CloudHttp) {

    /** Uploads [file] and returns the public URL to store on the report row. */
    suspend fun uploadPhoto(userId: String, file: File): String = cloudCall {
        val objectPath = "$userId/${file.name}"
        http.client.post("${CloudConfig.storageUrl}/object/${CloudConfig.PHOTO_BUCKET}/$objectPath") {
            cloudAuth(http.bearer())
            contentType(ContentType.Image.JPEG)
            // Lets a retry of the same photo overwrite rather than fail with "already exists".
            header("x-upsert", "true")
            setBody(file.readBytes())
        }.ensureSuccess()
        publicUrl(objectPath)
    }

    fun publicUrl(objectPath: String): String =
        "${CloudConfig.storageUrl}/object/public/${CloudConfig.PHOTO_BUCKET}/$objectPath"
}
