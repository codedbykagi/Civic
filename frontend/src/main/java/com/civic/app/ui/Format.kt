package com.civic.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import com.civic.shared.model.IssueCategory
import com.civic.shared.model.IssueStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatTime(epochMillis: Long): String =
    SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(epochMillis))

fun formatCoords(lat: Double?, lng: Double?): String =
    if (lat == null || lng == null) "Location unavailable" else "%.5f, %.5f".format(lat, lng)

/** Display name for a stored category, tolerating unknown values. */
fun categoryName(category: String): String =
    runCatching { IssueCategory.valueOf(category).displayName }.getOrDefault(category)

val IssueStatus.displayName: String
    get() = when (this) {
        IssueStatus.REPORTED -> "Reported"
        IssueStatus.ACKNOWLEDGED -> "Acknowledged"
        IssueStatus.IN_PROGRESS -> "In progress"
        IssueStatus.RESOLVED -> "Resolved"
    }

fun statusOf(status: String): IssueStatus =
    runCatching { IssueStatus.valueOf(status) }.getOrDefault(IssueStatus.REPORTED)

/** Opens the location in Google Maps (or any maps app). Shows a toast if none is installed. */
fun openInMaps(context: Context, lat: Double, lng: Double, label: String) {
    val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No maps app installed", Toast.LENGTH_SHORT).show()
    }
}

/** For permissions the user permanently denied: they can only be granted from system settings. */
fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/** Opens the dialer with [number] filled in; the user still taps Call (no CALL_PHONE permission, no pocket-dials). */
fun dial(context: Context, number: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No phone app: call $number from another phone", Toast.LENGTH_LONG).show()
    }
}

/** Opens a web link (or the app that handles it, e.g. Google Maps for maps URLs). */
fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No app can open this link", Toast.LENGTH_SHORT).show()
    }
}

