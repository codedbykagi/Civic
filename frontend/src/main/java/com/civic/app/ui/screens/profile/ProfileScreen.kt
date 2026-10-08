package com.civic.app.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.civic.app.ui.screens.feed.FeedViewModel
import com.civic.shared.model.IssueCategory

/** Prototype profile: stats about your reports. TODO: login/sign-up, user info. */
@Composable
fun ProfileScreen(viewModel: FeedViewModel = viewModel(factory = FeedViewModel.Factory)) {
    val reports by viewModel.reports.collectAsState()
    val list = reports.orEmpty()

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Your profile", style = MaterialTheme.typography.headlineSmall)
        Text("Guest user (accounts coming soon)", style = MaterialTheme.typography.bodyMedium)
        HorizontalDivider()
        Text("Reports submitted: ${list.size}", style = MaterialTheme.typography.titleMedium)
        Text("Total upvotes: ${list.sumOf { it.upvotes }}")
        HorizontalDivider()
        IssueCategory.entries.forEach { c ->
            val count = list.count { it.category == c.name }
            if (count > 0) Text("${c.displayName}: $count")
        }
    }
}
