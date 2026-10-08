package com.civic.app.ui.screens.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.civic.app.ui.components.PlaceholderScreen
import com.civic.app.ui.formatCoords
import com.civic.app.ui.formatTime
import com.civic.app.ui.openInMaps
import com.civic.app.ui.screens.feed.FeedViewModel
import com.civic.shared.model.IssueCategory

/** Prototype "map": every located report, tap to open it in the phone's maps app. TODO: embedded map with pins. */
@Composable
fun MapScreen(viewModel: FeedViewModel = viewModel(factory = FeedViewModel.Factory)) {
    val context = LocalContext.current
    val reports by viewModel.reports.collectAsState()
    val located = reports.orEmpty().filter { it.latitude != null && it.longitude != null }

    if (located.isEmpty()) {
        PlaceholderScreen("Nearby issues", "Reports with a GPS location will appear here.")
        return
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { Text("Issue locations", style = MaterialTheme.typography.headlineSmall) }
        items(located, key = { it.localId }) { r ->
            val name = runCatching { IssueCategory.valueOf(r.category).displayName }.getOrDefault(r.category)
            ListItem(
                modifier = Modifier.clickable { openInMaps(context, r.latitude!!, r.longitude!!, name) },
                leadingContent = { Icon(Icons.Filled.Place, contentDescription = null) },
                headlineContent = { Text(name) },
                supportingContent = { Text("${formatCoords(r.latitude, r.longitude)} · ${formatTime(r.capturedAt)}") },
            )
        }
    }
}
