package com.civic.app.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NoPhotography
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.civic.app.data.repository.decodeTags
import com.civic.app.data.repository.encodeTags
import com.civic.app.safety.effectiveTimeOfDay
import com.civic.app.safety.isSafety
import com.civic.app.safety.issueCategory
import com.civic.app.ui.categoryName
import com.civic.app.ui.components.Avatar
import com.civic.app.ui.components.PlaceholderScreen
import com.civic.app.ui.components.SafetyTagPicker
import com.civic.app.ui.components.StatusBadge
import com.civic.app.ui.components.TimeOfDayPicker
import com.civic.app.ui.components.initialsOf
import com.civic.app.ui.components.relativeTime
import com.civic.app.ui.components.safetyIcon
import com.civic.app.ui.components.zoneColor
import com.civic.app.ui.displayName
import com.civic.app.ui.formatCoords
import com.civic.app.ui.formatTime
import com.civic.app.ui.openInMaps
import com.civic.app.ui.statusOf
import com.civic.shared.model.IssueCategory
import com.civic.shared.model.IssueStatus
import com.civic.shared.model.SafetyTag
import com.civic.shared.model.TimeOfDay
import java.io.File

/**
 * One report. Civic issues: status control, upvotes and comments.
 * Safety reports are private: no upvotes, comments or directions; instead their details can be edited.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(
    onBack: () -> Unit,
    onAddPhoto: (reportId: Long) -> Unit,
    viewModel: ReportDetailViewModel = viewModel(factory = ReportDetailViewModel.Factory),
) {
    val state by viewModel.state.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val account by viewModel.account.collectAsState()

    // Other people's comments only exist on the server; pull them once when the report is opened.
    LaunchedEffect(Unit) { viewModel.syncComments() }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(if ((state as? DetailState.Loaded)?.report?.isSafety == true) "Safety report" else "Report") },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            },
            windowInsets = WindowInsets(0), // the outer Scaffold already pads for the status bar
        )
        when (val s = state) {
            DetailState.Loading -> PlaceholderScreen("Loading…", "") { CircularProgressIndicator() }
            DetailState.NotFound -> PlaceholderScreen("Report not found", "It may have been deleted.")
            is DetailState.Loaded -> if (s.report.isSafety) {
                SafetyReportDetail(
                    report = s.report,
                    onSave = viewModel::updateDetails,
                    onAddPhoto = { onAddPhoto(s.report.localId) },
                    onDelete = { viewModel.delete(onDeleted = onBack) },
                )
            } else {
                CivicReportDetail(
                    report = s.report,
                    comments = comments,
                    onUpvote = viewModel::upvote,
                    onSetStatus = viewModel::setStatus,
                    onAddPhoto = { onAddPhoto(s.report.localId) },
                    onSendComment = viewModel::addComment,
                    canComment = account != null,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CivicReportDetail(
    report: ReportEntity,
    comments: List<CommentEntity>,
    onUpvote: () -> Unit,
    onSetStatus: (IssueStatus) -> Unit,
    onAddPhoto: () -> Unit,
    onSendComment: (String) -> Unit,
    canComment: Boolean,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { CivicHeader(report, onUpvote = onUpvote, onSetStatus = onSetStatus, onAddPhoto = onAddPhoto) }
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
    CommentInput(onSend = onSendComment, enabled = canComment)
}

@Composable
private fun ReportPhoto(report: ReportEntity, description: String, onAddPhoto: () -> Unit) {
    // Own file first, then the server copy: a report pulled from the feed has no file on this phone.
    val model: Any? = report.localImagePath?.let { File(it) } ?: report.imageUrl
    if (model != null) {
        AsyncImage(
            model = model,
            contentDescription = description,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(240.dp),
        )
        TextButton(onClick = onAddPhoto) {
            Icon(Icons.Filled.AddAPhoto, contentDescription = null)
            Text("  Replace photo")
        }
    } else {
        Box(
            Modifier.fillMaxWidth().heightIn(min = 72.dp).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.NoPhotography, contentDescription = null)
                Text("  No photo")
            }
        }
        OutlinedButton(onClick = onAddPhoto) {
            Icon(Icons.Filled.AddAPhoto, contentDescription = null)
            Text("  Add a photo")
        }
    }
}

@Composable
private fun CivicHeader(
    report: ReportEntity,
    onUpvote: () -> Unit,
    onSetStatus: (IssueStatus) -> Unit,
    onAddPhoto: () -> Unit,
) {
    val context = LocalContext.current
    val name = categoryName(report.category)
    val status = statusOf(report.status)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ReportPhoto(report, name, onAddPhoto)
        AuthorLine(report)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(name, style = MaterialTheme.typography.titleLarge)
            StatusBadge(status)
        }
        if (report.description.isNotBlank()) Text(report.description)
        Text("📍 ${formatCoords(report.latitude, report.longitude)}", style = MaterialTheme.typography.bodySmall)
        Text(
            "🕒 ${formatTime(report.capturedAt)} · ${report.effectiveTimeOfDay.displayName}",
            style = MaterialTheme.typography.bodySmall,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onUpvote) {
                Icon(
                    if (report.upvotedByMe) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                    contentDescription = if (report.upvotedByMe) "Remove upvote" else "Upvote",
                )
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
private fun SafetyReportDetail(
    report: ReportEntity,
    onSave: (IssueCategory, String, TimeOfDay?, Set<SafetyTag>) -> Unit,
    onAddPhoto: () -> Unit,
    onDelete: () -> Unit,
) {
    val category = report.issueCategory ?: IssueCategory.UNSAFE_GENERAL
    val tags = decodeTags(report.tags)
    var editing by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(category.safetyIcon, contentDescription = null, tint = category.zoneColor, modifier = Modifier.size(32.dp))
            Column(Modifier.padding(start = 12.dp)) {
                Text(category.displayName, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${report.effectiveTimeOfDay.displayName} (${report.effectiveTimeOfDay.hint})",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
            Text("  Private — only on this phone; adds to anonymous map zones", style = MaterialTheme.typography.bodySmall)
        }
        if (tags.isNotEmpty()) Text(tags.joinToString(" · ") { it.displayName })
        if (report.description.isNotBlank()) Text(report.description)
        Text(
            if (report.latitude != null) "📍 ${formatCoords(report.latitude, report.longitude)}"
            else "📍 No location — this report can't appear on the map",
            style = MaterialTheme.typography.bodySmall,
        )
        Text("🕒 ${formatTime(report.capturedAt)}", style = MaterialTheme.typography.bodySmall)

        OutlinedButton(onClick = { editing = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Edit, contentDescription = null)
            Text("  Add or edit details")
        }
        ReportPhoto(report, category.displayName, onAddPhoto)
        Text(
            "Photos are optional. Avoid capturing people's faces, especially children's.",
            style = MaterialTheme.typography.bodySmall,
        )
        TextButton(onClick = { confirmDelete = true }) {
            Icon(Icons.Filled.Delete, contentDescription = null)
            Text("  Delete report")
        }
    }

    if (editing) {
        EditSafetyDetailsDialog(
            report = report,
            category = category,
            onDismiss = { editing = false },
            onSave = { c, d, t, tg ->
                onSave(c, d, t, tg)
                editing = false
            },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this safety report?") },
            text = { Text("It will be removed from this phone and from the map zones.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun EditSafetyDetailsDialog(
    report: ReportEntity,
    category: IssueCategory,
    onDismiss: () -> Unit,
    onSave: (IssueCategory, String, TimeOfDay?, Set<SafetyTag>) -> Unit,
) {
    var newCategory by rememberSaveable { mutableStateOf(category) }
    var description by rememberSaveable { mutableStateOf(report.description) }
    // null = keep deriving it automatically, so later location refinement still updates it.
    var timeOverride by rememberSaveable { mutableStateOf(TimeOfDay.entries.firstOrNull { it.name == report.timeOfDay }) }
    var tagNames by rememberSaveable { mutableStateOf(report.tags.orEmpty()) }
    val tags = decodeTags(tagNames)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report details") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Felt unsafe — for whom?", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(IssueCategory.SAFETY) { c ->
                        FilterChip(
                            selected = c == newCategory,
                            onClick = { newCategory = c },
                            label = { Text(c.displayName) },
                        )
                    }
                }
                TimeOfDayPicker(
                    selected = timeOverride ?: report.effectiveTimeOfDay,
                    isAuto = timeOverride == null,
                    onSelect = { timeOverride = it },
                )
                SafetyTagPicker(
                    selected = tags,
                    onToggle = { tag -> tagNames = encodeTags(if (tag in tags) tags - tag else tags + tag).orEmpty() },
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 1000) description = it },
                    label = { Text("What happened? (optional)") },
                    supportingText = { Text("Describe the place, not people.") },
                    minLines = 2,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(newCategory, description, timeOverride, tags) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Who wrote the report, shown the same way as in the feed card. */
@Composable
private fun AuthorLine(report: ReportEntity) {
    val name = report.authorName ?: "Unknown" // reports written before accounts existed have no author
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Avatar(report.authorAvatar, initialsOf(name), size = 32.dp)
        Column {
            Text(name, style = MaterialTheme.typography.labelLarge)
            Text(relativeTime(report.capturedAt), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun CommentRow(comment: CommentEntity) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Avatar(comment.authorAvatar, initialsOf(comment.author), size = 32.dp)
        Column {
            Text(
                "${comment.author} · ${relativeTime(comment.createdAt)}",
                style = MaterialTheme.typography.labelSmall,
            )
            Text(comment.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun CommentInput(onSend: (String) -> Unit, enabled: Boolean) {
    var text by rememberSaveable { mutableStateOf("") }
    Row(
        modifier = Modifier.fillMaxWidth().imePadding().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { if (it.length <= 500) text = it },
            placeholder = { Text(if (enabled) "Add a comment" else "Set up a profile to comment") },
            enabled = enabled,
            modifier = Modifier.weight(1f),
            maxLines = 4,
        )
        IconButton(
            enabled = enabled && text.isNotBlank(),
            onClick = {
                onSend(text)
                text = ""
            },
        ) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send comment") }
    }
}
