package com.civic.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.ThumbUp
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
import com.civic.app.ui.categoryName
import com.civic.app.ui.displayName
import com.civic.app.ui.formatCoords
import com.civic.app.ui.formatTime
import com.civic.app.ui.openInMaps
import com.civic.app.ui.statusOf
import com.civic.shared.model.IssueStatus
import java.io.File

/** One post in the feed: photo, category, status, description, where and when, plus actions. Tap to open. */
@Composable
fun ReportCard(
    report: ReportEntity,
    onUpvote: () -> Unit,
    onDelete: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val categoryName = categoryName(report.category)
    var confirmDelete by remember { mutableStateOf(false) }

    Card(onClick = onOpen, modifier = modifier.fillMaxWidth()) {
        AsyncImage(
            model = File(report.localImagePath),
            contentDescription = categoryName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(220.dp),
        )
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(categoryName, style = MaterialTheme.typography.titleMedium)
                StatusBadge(statusOf(report.status))
            }
            if (report.description.isNotBlank()) {
                Text(report.description, style = MaterialTheme.typography.bodyMedium)
            }
            Text("📍 ${formatCoords(report.latitude, report.longitude)}", style = MaterialTheme.typography.bodySmall)
            Text("🕒 ${formatTime(report.capturedAt)}", style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onUpvote) {
                    Icon(Icons.Filled.ThumbUp, contentDescription = "Upvote")
                    Text("  ${report.upvotes}")
                }
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
