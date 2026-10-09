package com.civic.app.ui.screens.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.civic.app.R
import com.civic.app.data.sync.SyncStatus
import com.civic.app.ui.components.PlaceholderScreen
import com.civic.app.ui.components.QuickUnsafePanel
import com.civic.app.ui.components.ReportCard
import com.civic.app.ui.components.rememberQuickReportAction
import com.civic.app.ui.displayName
import com.civic.shared.model.IssueCategory
import com.civic.shared.model.IssueStatus

/**
 * Home: the one-tap "felt unsafe" panel (so it is one tap from opening the app), then the civic issue feed,
 * filterable by status and category. Private safety reports are not listed here.
 */
@Composable
fun FeedScreen(
    onOpenReport: (Long) -> Unit,
    viewModel: FeedViewModel = viewModel(factory = FeedViewModel.Factory),
) {
    val reports by viewModel.reports.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val canSync by viewModel.canSync.collectAsState()
    val quickReport = rememberQuickReportAction()
    val list = reports

    if (list == null) {
        PlaceholderScreen("Loading…", "") { CircularProgressIndicator() }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
<<<<<<< HEAD
            // The header above already shows the wordmark, so the feed opens with the tagline.
            Text(
                stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
=======
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Civic", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        stringResource(R.string.app_tagline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (canSync) {
                    if (syncStatus is SyncStatus.Running) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = viewModel::refresh) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh feed")
                        }
                    }
                }
            }
            (syncStatus as? SyncStatus.Failed)?.let {
                Text(it.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
>>>>>>> remotes/origin/Basic_func
        }
        item { QuickUnsafePanel(onReport = quickReport, compact = true) }
        if (list.isEmpty() && !filter.isActive) {
            item {
                Text(
                    "No reports yet — tap Report to add the first.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
            return@LazyColumn
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Civic feed", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(IssueStatus.entries) { s ->
                        FilterChip(
                            selected = filter.status == s,
                            onClick = { viewModel.toggleStatus(s) },
                            label = { Text(s.displayName) },
                        )
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(IssueCategory.CIVIC) { c ->
                        FilterChip(
                            selected = filter.category == c,
                            onClick = { viewModel.toggleCategory(c) },
                            label = { Text(c.displayName) },
                        )
                    }
                }
            }
        }
        if (list.isEmpty()) {
            item {
                Column(Modifier.padding(vertical = 24.dp)) {
                    Text("No reports match this filter.", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = viewModel::clearFilter) { Text("Clear filters") }
                }
            }
        }
        items(list, key = { it.localId }) { report ->
            ReportCard(
                report = report,
                onUpvote = { viewModel.upvote(report) },
                onDelete = { viewModel.delete(report) },
                onOpen = { onOpenReport(report.localId) },
                syncIsPossible = canSync,
            )
        }
    }
}
