package com.civic.app.camera

import android.content.Context
import java.io.File

/** Where captured photos are saved before upload. CameraX capture logic will live in this package. */
object PhotoStorage {
    fun newPhotoFile(context: Context): File {
        val dir = File(context.filesDir, "photos").apply { mkdirs() }
        return File(dir, "report_${System.currentTimeMillis()}.jpg")
    }
}
