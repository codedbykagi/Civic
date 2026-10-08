package com.civic.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatTime(epochMillis: Long): String =
    SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(epochMillis))

fun formatCoords(lat: Double?, lng: Double?): String =
    if (lat == null || lng == null) "Location unavailable" else "%.5f, %.5f".format(lat, lng)

/** Opens the location in Google Maps (or any maps app). */
fun openInMaps(context: Context, lat: Double, lng: Double, label: String) {
    val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})")
    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
}
