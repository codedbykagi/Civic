package com.civic.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.civic.app.data.local.ReportEntity
import com.civic.app.ui.formatCoords
import com.civic.app.ui.formatTime
import com.civic.app.ui.openInMaps
import com.civic.shared.model.IssueCategory
import java.io.File

/** One post in the feed: photo, category, description, where and when, plus actions. */
@Composable
fun ReportCard(
    report: ReportEntity,
    onUpvote: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val categoryName = runCatching { IssueCategory.valueOf(report.category).displayName }.getOrDefault(report.category)

    Card(modifier = modifier.fillMaxWidth()) {
        AsyncImage(
            model = File(report.localImagePath),
            contentDescription = categoryName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(220.dp),
        )
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(categoryName, style = MaterialTheme.typography.titleMedium)
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
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
            }
        }
    }
}
