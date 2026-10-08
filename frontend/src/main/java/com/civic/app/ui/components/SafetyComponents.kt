package com.civic.app.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Woman
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.civic.app.appContainer
import com.civic.app.safety.Helpline
import com.civic.app.safety.zones.ZoneKind
import com.civic.app.safety.zones.ZoneStyle
import com.civic.app.ui.dial
import com.civic.shared.model.IssueCategory

/** Short button label for each one-tap safety category. */
val IssueCategory.quickLabel: String
    get() = when (this) {
        IssueCategory.UNSAFE_WOMEN -> "Women"
        IssueCategory.UNSAFE_CHILDREN -> "Children"
        else -> "Everyone"
    }

val IssueCategory.safetyIcon: ImageVector
    get() = when (this) {
        IssueCategory.UNSAFE_WOMEN -> Icons.Filled.Woman
        IssueCategory.UNSAFE_CHILDREN -> Icons.Filled.ChildCare
        else -> Icons.Filled.Groups
    }

/** Same hue as the category's map zones, so buttons and zones read as one system. */
val IssueCategory.zoneColor: Color
    get() = Color(ZoneStyle.outlineColor(ZoneKind.of(this)))

/** Reports are only placed on the map with a precise fix; approximate (~3 km²) positions would invent zones. */
fun hasPreciseLocation(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

/**
 * Returns the one-tap report action. With location permission it saves at once; otherwise it asks first and
 * then saves either way (without a position if refused), so a tap is never silently lost.
 */
@Composable
fun rememberQuickReportAction(): (IssueCategory) -> Unit {
    val context = LocalContext.current
    val reporter = context.appContainer.quickReporter
    var pending by rememberSaveable { mutableStateOf<IssueCategory?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        pending?.let { reporter.report(it, hasPreciseLocation(context)) }
        pending = null
    }
    return remember(reporter, launcher) {
        { category ->
            if (hasPreciseLocation(context)) {
                reporter.report(category, hasLocationPermission = true)
            } else {
                pending = category
                launcher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                )
            }
        }
    }
}

/**
 * "I felt unsafe here": one button per group, each saves instantly with GPS and time.
 * [compact] is the slim variant for the top of the feed and the map.
 */
@Composable
fun QuickUnsafePanel(onReport: (IssueCategory) -> Unit, modifier: Modifier = Modifier, compact: Boolean = false) {
    Card(modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(if (compact) 12.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp),
        ) {
            Text(
                if (compact) "Felt unsafe here? Tap who for:" else "I felt unsafe here",
                style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (!compact) {
                Text(
                    "One tap saves this spot with your GPS location and the time. Add details later if you want.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IssueCategory.SAFETY.forEach { category ->
                    Button(
                        onClick = { onReport(category) },
                        modifier = Modifier.weight(1f).heightIn(min = if (compact) 44.dp else 56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = category.zoneColor, contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    ) {
                        Icon(category.safetyIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(" ${category.quickLabel}", maxLines = 1)
                    }
                }
            }
        }
    }
}

/** One-tap dialer buttons. The quick report itself never contacts anyone, so help must be one tap away. */
@Composable
fun HelplineRow(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Saving a report does not alert police. In danger? Call:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Helpline.entries.forEach { line ->
                OutlinedButton(
                    onClick = { dial(context, line.number) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                            Text(" ${line.number}", fontWeight = FontWeight.Bold)
                        }
                        Text(line.label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
