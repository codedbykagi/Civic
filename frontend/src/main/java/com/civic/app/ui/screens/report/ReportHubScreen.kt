package com.civic.app.ui.screens.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.civic.app.R
import com.civic.app.ui.components.HelplineRow
import com.civic.app.ui.components.QuickUnsafePanel
import com.civic.app.ui.components.rememberQuickReportAction

/** The Report tab: one-tap "felt unsafe" first, helplines next to it, then full reports with or without a photo. */
@Composable
fun ReportHubScreen(
    onTakePhoto: () -> Unit,
    onReportWithoutPhoto: () -> Unit,
    onMySafetyReports: () -> Unit,
) {
    val quickReport = rememberQuickReportAction()
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text("Report", style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        QuickUnsafePanel(onReport = quickReport)
        HelplineRow()

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Report with details", style = MaterialTheme.typography.titleMedium)
                Text(
                    "A civic problem (pothole, broken streetlight…) or a place that felt unsafe. " +
                        "Location and time are recorded automatically; a photo is optional.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(onClick = onTakePhoto, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                    Text("  Take a photo")
                }
                OutlinedButton(onClick = onReportWithoutPhoto, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.EditNote, contentDescription = null)
                    Text("  No photo — mark the spot and describe it")
                }
            }
        }

        TextButton(onClick = onMySafetyReports) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Lock, contentDescription = null)
                Text("  My safety reports (private)  ")
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}
