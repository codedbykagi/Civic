package com.civic.app.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.civic.app.appContainer
import com.civic.app.ui.components.HelplineRow
import com.civic.app.ui.screens.feed.FeedViewModel
import com.civic.shared.model.IssueCategory
import com.civic.shared.model.IssueStatus

/** Prototype profile: stats about your reports. TODO: login/sign-up, user info. */
@Composable
fun ProfileScreen(
    onMySafetyReports: () -> Unit,
    viewModel: FeedViewModel = viewModel(factory = FeedViewModel.Factory),
) {
    val context = LocalContext.current
    val reports by viewModel.reports.collectAsState()
    val list = reports.orEmpty()
    val safetyFlow = remember { context.appContainer.reportRepository.observeSafetyReports() }
    val safetyCount = safetyFlow.collectAsState(initial = emptyList()).value.size

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Your profile", style = MaterialTheme.typography.headlineSmall)
        Text("Guest user (accounts coming soon)", style = MaterialTheme.typography.bodyMedium)
        HorizontalDivider()
        Text("Civic reports: ${list.size}", style = MaterialTheme.typography.titleMedium)
        Text("Total upvotes: ${list.sumOf { it.upvotes }}")
        Text("Resolved: ${list.count { it.status == IssueStatus.RESOLVED.name }}")
        IssueCategory.CIVIC.forEach { c ->
            val count = list.count { it.category == c.name }
            if (count > 0) Text("${c.displayName}: $count")
        }
        HorizontalDivider()
        TextButton(onClick = onMySafetyReports) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Lock, contentDescription = null)
                Text("  My safety reports: $safetyCount (private)")
            }
        }
        HorizontalDivider()
        HelplineRow()
    }
}
