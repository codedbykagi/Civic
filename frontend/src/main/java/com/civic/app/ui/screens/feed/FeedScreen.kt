package com.civic.app.ui.screens.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.civic.app.ui.components.PlaceholderScreen
import com.civic.app.ui.components.ReportCard

/** Social feed of issue reports (stored on this device in the prototype). */
@Composable
fun FeedScreen(viewModel: FeedViewModel = viewModel(factory = FeedViewModel.Factory)) {
    val reports by viewModel.reports.collectAsState()
    val list = reports

    when {
        list == null -> PlaceholderScreen("Loading…", "") { CircularProgressIndicator() }
        list.isEmpty() -> PlaceholderScreen(
            "No reports yet",
            "Tap the Report tab to photograph a pothole, fire, fallen tree or other issue.",
        )
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Text("Civic feed", style = MaterialTheme.typography.headlineSmall) }
            items(list, key = { it.localId }) { report ->
                ReportCard(
                    report = report,
                    onUpvote = { viewModel.upvote(report) },
                    onDelete = { viewModel.delete(report) },
                )
            }
        }
    }
}
