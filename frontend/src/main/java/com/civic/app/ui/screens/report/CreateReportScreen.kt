package com.civic.app.ui.screens.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.civic.app.appContainer
import com.civic.app.data.local.ReportEntity
import com.civic.app.ui.components.PlaceholderScreen
import com.civic.app.ui.formatCoords
import com.civic.app.ui.formatTime
import com.civic.shared.model.IssueCategory
import kotlinx.coroutines.launch
import java.io.File

/** Post form shown after taking a photo: category + description, location/time are automatic. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateReportScreen(onPosted: () -> Unit) {
    val context = LocalContext.current
    val container = context.appContainer
    val draft = container.draftStore.current
    val scope = rememberCoroutineScope()

    if (draft == null) {
        PlaceholderScreen("No photo", "Take a photo first from the Report tab.")
        return
    }

    var category by remember { mutableStateOf(IssueCategory.POTHOLE) }
    var description by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("New report", style = MaterialTheme.typography.headlineSmall)
        AsyncImage(
            model = File(draft.photoPath),
            contentDescription = "Captured photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(240.dp),
        )
        Text("📍 ${formatCoords(draft.location?.latitude, draft.location?.longitude)}")
        Text("🕒 ${formatTime(draft.capturedAt)}")

        Text("What's the issue?", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IssueCategory.entries.forEach { c ->
                FilterChip(selected = c == category, onClick = { category = c }, label = { Text(c.displayName) })
            }
        }

        OutlinedTextField(
            value = description,
            onValueChange = { if (it.length <= 1000) description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )

        Button(
            enabled = !saving,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                saving = true
                scope.launch {
                    container.reportRepository.addReport(
                        ReportEntity(
                            category = category.name,
                            description = description.trim(),
                            localImagePath = draft.photoPath,
                            latitude = draft.location?.latitude,
                            longitude = draft.location?.longitude,
                            capturedAt = draft.capturedAt,
                        ),
                    )
                    container.draftStore.current = null
                    onPosted()
                }
            },
        ) { Text("Post") }
    }
}
