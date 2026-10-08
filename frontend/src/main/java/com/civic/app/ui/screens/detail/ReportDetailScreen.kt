package com.civic.app.ui.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.civic.app.data.local.CommentEntity
import com.civic.app.data.local.ReportEntity
import com.civic.app.ui.categoryName
import com.civic.app.ui.components.PlaceholderScreen
import com.civic.app.ui.components.StatusBadge
import com.civic.app.ui.displayName
import com.civic.app.ui.formatCoords
import com.civic.app.ui.formatTime
import com.civic.app.ui.openInMaps
import com.civic.app.ui.statusOf
import com.civic.shared.model.IssueStatus
import java.io.File

/** One report: photo, details, status control and comments. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(
    onBack: () -> Unit,
    viewModel: ReportDetailViewModel = viewModel(factory = ReportDetailViewModel.Factory),
) {
    val state by viewModel.state.collectAsState()
    val comments by viewModel.comments.collectAsState()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Report") },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            },
            windowInsets = WindowInsets(0), // the outer Scaffold already pads for the status bar
        )
        when (val s = state) {
            DetailState.Loading -> PlaceholderScreen("Loading…", "") { CircularProgressIndicator() }
            DetailState.NotFound -> PlaceholderScreen("Report not found", "It may have been deleted.")
            is DetailState.Loaded -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { ReportHeader(s.report, onUpvote = viewModel::upvote, onSetStatus = viewModel::setStatus) }
                    item {
                        HorizontalDivider()
                        Text(
                            "Comments (${comments.size})",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                        if (comments.isEmpty()) {
                            Text("No comments yet.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    items(comments, key = { it.id }) { CommentRow(it) }
                }
                CommentInput(onSend = viewModel::addComment)
            }
        }
    }
}

@Composable
private fun ReportHeader(report: ReportEntity, onUpvote: () -> Unit, onSetStatus: (IssueStatus) -> Unit) {
    val context = LocalContext.current
    val name = categoryName(report.category)
    val status = statusOf(report.status)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AsyncImage(
            model = File(report.localImagePath),
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(260.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(name, style = MaterialTheme.typography.titleLarge)
            StatusBadge(status)
        }
        if (report.description.isNotBlank()) Text(report.description)
        Text("📍 ${formatCoords(report.latitude, report.longitude)}", style = MaterialTheme.typography.bodySmall)
        Text("🕒 ${formatTime(report.capturedAt)}", style = MaterialTheme.typography.bodySmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onUpvote) {
                Icon(Icons.Filled.ThumbUp, contentDescription = "Upvote")
                Text("  ${report.upvotes}")
            }
            if (report.latitude != null && report.longitude != null) {
                TextButton(onClick = { openInMaps(context, report.latitude, report.longitude, name) }) {
                    Icon(Icons.Filled.Map, contentDescription = null)
                    Text("  Open in Maps")
                }
            }
        }
        Text("Status", style = MaterialTheme.typography.titleSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(IssueStatus.entries) { s ->
                FilterChip(selected = s == status, onClick = { onSetStatus(s) }, label = { Text(s.displayName) })
            }
        }
    }
}

@Composable
private fun CommentRow(comment: CommentEntity) {
    Column {
        Text("${comment.author} · ${formatTime(comment.createdAt)}", style = MaterialTheme.typography.labelSmall)
        Text(comment.text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CommentInput(onSend: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    Row(
        modifier = Modifier.fillMaxWidth().imePadding().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { if (it.length <= 500) text = it },
            placeholder = { Text("Add a comment") },
            modifier = Modifier.weight(1f),
            maxLines = 4,
        )
        IconButton(
            enabled = text.isNotBlank(),
            onClick = {
                onSend(text)
                text = ""
            },
        ) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send comment") }
    }
}
