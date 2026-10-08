package com.civic.app.ui.screens.safety

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.civic.app.CivicApplication
import com.civic.app.data.local.ReportEntity
import com.civic.app.data.repository.ReportRepository
import com.civic.app.data.repository.decodeTags
import com.civic.app.safety.effectiveTimeOfDay
import com.civic.app.safety.issueCategory
import com.civic.app.ui.categoryName
import com.civic.app.ui.components.PlaceholderScreen
import com.civic.app.ui.components.safetyIcon
import com.civic.app.ui.components.zoneColor
import com.civic.app.ui.formatTime
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SafetyReportsViewModel(repository: ReportRepository) : ViewModel() {
    /** null = still loading. */
    val reports: StateFlow<List<ReportEntity>?> = repository.observeSafetyReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        val Factory = viewModelFactory {
            initializer { SafetyReportsViewModel((this[APPLICATION_KEY] as CivicApplication).container.reportRepository) }
        }
    }
}

/** The user's own safety reports. Private: only listed here; everyone else would only ever see map zones. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyReportsScreen(
    onBack: () -> Unit,
    onOpenReport: (Long) -> Unit,
    viewModel: SafetyReportsViewModel = viewModel(factory = SafetyReportsViewModel.Factory),
) {
    val reports by viewModel.reports.collectAsState()
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("My safety reports") },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            },
            windowInsets = WindowInsets(0),
        )
        val list = reports
        when {
            list == null -> PlaceholderScreen("Loading…", "") { CircularProgressIndicator() }
            list.isEmpty() -> PlaceholderScreen(
                "No safety reports yet",
                "Use “I felt unsafe here” on the Report tab. Your reports stay on this phone.",
            )
            else -> LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                item {
                    Text(
                        "Private — kept on this phone. On the map they only appear inside anonymous zones.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
                items(list, key = { it.localId }) { r -> SafetyReportRow(r, onClick = { onOpenReport(r.localId) }) }
            }
        }
    }
}

@Composable
private fun SafetyReportRow(report: ReportEntity, onClick: () -> Unit) {
    val category = report.issueCategory
    val tags = decodeTags(report.tags)
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            category?.let { Icon(it.safetyIcon, contentDescription = null, tint = it.zoneColor) }
        },
        headlineContent = { Text("${categoryName(report.category)} · ${report.effectiveTimeOfDay.displayName}") },
        supportingContent = {
            Column {
                Text(formatTime(report.capturedAt), style = MaterialTheme.typography.bodySmall)
                if (tags.isNotEmpty()) {
                    Text(tags.joinToString(" · ") { it.displayName }, style = MaterialTheme.typography.bodySmall)
                }
                if (report.description.isNotBlank()) {
                    Text(report.description, maxLines = 2, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        trailingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (report.latitude == null) Icon(Icons.Filled.LocationOff, contentDescription = "No location")
                if (report.localImagePath != null) Icon(Icons.Filled.Photo, contentDescription = "Has photo")
            }
        },
    )
}
