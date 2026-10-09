package com.civic.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.civic.app.data.local.ReportEntity
import com.civic.app.safety.effectiveTimeOfDay
import com.civic.app.ui.categoryName
import com.civic.app.ui.displayName
import com.civic.app.ui.formatCoords
import com.civic.app.ui.formatTime
import com.civic.app.ui.openInMaps
import com.civic.app.ui.statusOf
import com.civic.shared.model.IssueStatus
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * One post in the feed: who wrote it, photo, category, status, description, where and when, plus actions.
 * Tap to open.
 *
 * [syncIsPossible] is passed in rather than read from the container, so the card stays a pure function of its
 * inputs: with no backend there is nothing to be "not synced" with, and the cloud badge would only confuse.
 */
@Composable
fun ReportCard(
    report: ReportEntity,
    onUpvote: () -> Unit,
    onDelete: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    syncIsPossible: Boolean = false,
) {
    val context = LocalContext.current
    val categoryName = categoryName(report.category)
    var confirmDelete by remember { mutableStateOf(false) }

    Card(onClick = onOpen, modifier = modifier.fillMaxWidth()) {
        ReportPhotoStrip(report, categoryName)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AuthorLine(report, syncIsPossible)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(categoryName, style = MaterialTheme.typography.titleMedium)
                StatusBadge(statusOf(report.status))
            }
            if (report.description.isNotBlank()) {
                Text(report.description, style = MaterialTheme.typography.bodyMedium)
            }
            Text("📍 ${formatCoords(report.latitude, report.longitude)}", style = MaterialTheme.typography.bodySmall)
            Text(
                "🕒 ${formatTime(report.capturedAt)} · ${report.effectiveTimeOfDay.displayName}",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                UpvoteButton(upvoted = report.upvotedByMe, count = report.upvotes, onClick = onUpvote)
                if (report.latitude != null && report.longitude != null) {
                    TextButton(onClick = { openInMaps(context, report.latitude, report.longitude, categoryName) }) {
                        Icon(Icons.Filled.Map, contentDescription = null)
                        Text("  Map")
                    }
                }
                IconButton(onClick = onOpen) { Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "Comments") }
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete report?") },
            text = { Text("The photo and its comments will be removed from this device.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

/** Own photo first, then the server copy: a report pulled from the feed has no file on this phone. */
@Composable
private fun ReportPhotoStrip(report: ReportEntity, categoryName: String) {
    val model: Any? = report.localImagePath?.let { File(it) } ?: report.imageUrl
    if (model != null) {
        AsyncImage(
            model = model,
            contentDescription = categoryName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(220.dp),
        )
    }
}

@Composable
private fun AuthorLine(report: ReportEntity, syncIsPossible: Boolean) {
    val name = report.authorName ?: "Unknown" // reports written before accounts existed have no author
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Avatar(report.authorAvatar, initialsOf(name), size = 32.dp)
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.labelLarge)
            Text(relativeTime(report.capturedAt), style = MaterialTheme.typography.labelSmall)
        }
        if (!report.isSynced && syncIsPossible) {
            Icon(
                Icons.Filled.CloudOff,
                contentDescription = "Not uploaded yet",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun UpvoteButton(upvoted: Boolean, count: Int, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Icon(
            if (upvoted) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
            contentDescription = if (upvoted) "Remove upvote" else "Upvote",
        )
        Text("  $count")
    }
}

/** Up to two initials from any display name; the same fallback [com.civic.app.data.auth.Account] uses. */
fun initialsOf(name: String): String = name.trim().split(' ', limit = 3)
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { it.first().uppercase() }
    .ifEmpty { "?" }

/**
 * "just now" / "5 min ago" / "3 h ago" / "2 d ago", falling back to the absolute date after a week.
 * Lives here because the feed, the comments and the sync row all show the same kind of timestamp.
 */
fun relativeTime(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
    val millis = now - epochMillis
    if (millis < 0) return formatTime(epochMillis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val days = TimeUnit.MILLISECONDS.toDays(millis)
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours h ago"
        days <= 7 -> "$days d ago"
        else -> formatTime(epochMillis)
    }
}

/** Small colored pill showing the report's status. */
@Composable
fun StatusBadge(status: IssueStatus, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val (bg, fg) = when (status) {
        IssueStatus.RESOLVED -> colors.primaryContainer to colors.onPrimaryContainer
        IssueStatus.IN_PROGRESS, IssueStatus.ACKNOWLEDGED -> colors.tertiaryContainer to colors.onTertiaryContainer
        IssueStatus.REPORTED -> colors.surfaceVariant to colors.onSurfaceVariant
    }
    Surface(color = bg, contentColor = fg, shape = MaterialTheme.shapes.small, modifier = modifier) {
        Text(
            status.displayName,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
