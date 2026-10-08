package com.civic.app.ui.screens.report

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NoPhotography
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.civic.app.appContainer
import com.civic.app.data.Draft
import com.civic.app.data.DraftStore
import com.civic.app.data.local.ReportEntity
import com.civic.app.data.repository.decodeTags
import com.civic.app.data.repository.encodeTags
import com.civic.app.safety.time.TimeOfDayClassifier
import com.civic.app.ui.components.CategoryPicker
import com.civic.app.ui.components.PlaceholderScreen
import com.civic.app.ui.components.SafetyTagPicker
import com.civic.app.ui.components.TimeOfDayPicker
import com.civic.app.ui.components.hasPreciseLocation
import com.civic.app.ui.formatCoords
import com.civic.app.ui.formatTime
import com.civic.shared.model.GeoLocation
import com.civic.shared.model.IssueCategory
import com.civic.shared.model.TimeOfDay
import kotlinx.coroutines.launch
import java.io.File

/**
 * Report form. The photo is optional: someone can just mark "I felt unsafe here" with a description.
 * Location and time are recorded automatically; time of day and tags can be corrected.
 */
@Composable
fun CreateReportScreen(onPosted: (isSafety: Boolean) -> Unit, onTakePhoto: () -> Unit) {
    val context = LocalContext.current
    val container = context.appContainer
    val draft = container.draftStore.current
    val scope = rememberCoroutineScope()

    if (draft == null) {
        PlaceholderScreen("Nothing to report yet", "Start from the Report tab.")
        return
    }

    var category by rememberSaveable { mutableStateOf(draft.suggestedCategory) }
    var description by rememberSaveable { mutableStateOf("") }
    var timeOverride by rememberSaveable { mutableStateOf<TimeOfDay?>(null) }
    var tagNames by rememberSaveable { mutableStateOf("") } // encoded like the DB column, so it is saveable
    var saving by remember { mutableStateOf(false) }
    var location by remember { mutableStateOf(draft.location) }
    var locating by remember { mutableStateOf(false) }
    var locationTries by remember { mutableStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        locationTries++
    }
    // Auto-record the position when the draft has none (e.g. reporting without a photo).
    LaunchedEffect(locationTries) {
        if (location != null) return@LaunchedEffect
        if (!hasPreciseLocation(context)) {
            if (locationTries == 0) {
                permissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                )
            }
            return@LaunchedEffect
        }
        locating = true
        val found = runCatching { container.locationProvider.currentLocation() }.getOrNull()
        locating = false
        if (found != null) {
            location = found
            container.draftStore.setLocation(found)
        }
    }

    val autoTime = TimeOfDayClassifier.classify(draft.capturedAt, location?.latitude, location?.longitude)
    val timeOfDay = timeOverride ?: autoTime
    val tags = decodeTags(tagNames)

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("New report", style = MaterialTheme.typography.headlineSmall)

        PhotoSection(draft.photoPath, isSafety = category.isSafety, onTakePhoto = onTakePhoto)

        LocationLine(
            location = location,
            locating = locating,
            onRetry = { locationTries++ },
        )
        Text("🕒 ${formatTime(draft.capturedAt)}")

        CategoryPicker(selected = category, onSelect = { category = it })

        TimeOfDayPicker(selected = timeOfDay, isAuto = timeOverride == null, onSelect = { timeOverride = it })

        if (category.isSafety) {
            SafetyTagPicker(
                selected = tags,
                onToggle = { tag -> tagNames = encodeTags(if (tag in tags) tags - tag else tags + tag).orEmpty() },
            )
        }

        OutlinedTextField(
            value = description,
            onValueChange = { if (it.length <= 1000) description = it },
            label = { Text(if (category.isSafety) "What happened? (optional)" else "Description") },
            supportingText = if (category.isSafety) {
                { Text("Describe the place, not people. For a child in danger call 1098 or 112.") }
            } else {
                null
            },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )

        if (category.isSafety) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(
                    "  Private: kept on this phone. It only adds to anonymous map zones.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Button(
            enabled = !saving,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                saving = true
                scope.launch {
                    try {
                        container.reportRepository.addReport(
                            ReportEntity(
                                category = category.name,
                                description = description.trim(),
                                localImagePath = draft.photoPath,
                                latitude = location?.latitude,
                                longitude = location?.longitude,
                                capturedAt = draft.capturedAt,
                                timeOfDay = timeOverride?.name,
                                tags = if (category.isSafety) encodeTags(tags) else null,
                            ),
                        )
                        container.draftStore.clear()
                        onPosted(category.isSafety)
                    } catch (e: Exception) {
                        // Re-enable the button so the user can retry instead of being stuck.
                        saving = false
                        Toast.makeText(context, "Couldn't save report: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            },
        ) { Text(if (category.isSafety) "Save report" else "Post") }
    }
}

@Composable
private fun PhotoSection(photoPath: String?, isSafety: Boolean, onTakePhoto: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (photoPath != null) {
            AsyncImage(
                model = File(photoPath),
                contentDescription = "Captured photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(220.dp),
            )
            TextButton(onClick = onTakePhoto) {
                Icon(Icons.Filled.AddAPhoto, contentDescription = null)
                Text("  Retake photo")
            }
        } else {
            Box(
                Modifier.fillMaxWidth().height(96.dp).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.NoPhotography, contentDescription = null)
                    Text("  No photo (optional)")
                }
            }
            OutlinedButton(onClick = onTakePhoto) {
                Icon(Icons.Filled.AddAPhoto, contentDescription = null)
                Text("  Add a photo")
            }
        }
        if (isSafety) {
            Text(
                "Photos are optional. Avoid capturing people's faces, especially children's.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun LocationLine(location: GeoLocation?, locating: Boolean, onRetry: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        when {
            location != null -> Text("📍 ${formatCoords(location.latitude, location.longitude)}")
            locating -> {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Text("  Getting your location…")
            }
            else -> {
                Text("📍 Location not found  ")
                TextButton(onClick = onRetry) { Text("Try again") }
            }
        }
    }
}

/** Starts a report without a photo; the form records the location itself. */
fun startReportWithoutPhoto(draftStore: DraftStore, category: IssueCategory) {
    draftStore.replace(
        Draft(
            photoPath = null,
            location = null,
            capturedAt = System.currentTimeMillis(),
            suggestedCategory = category,
        ),
    )
}
