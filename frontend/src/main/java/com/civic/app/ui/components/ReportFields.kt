package com.civic.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.civic.shared.model.IssueCategory
import com.civic.shared.model.SafetyTag
import com.civic.shared.model.TimeOfDay

/** Category chips in two groups: personal safety first (the app's focus), then civic issues. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryPicker(selected: IssueCategory, onSelect: (IssueCategory) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Felt unsafe — for whom?", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IssueCategory.SAFETY.forEach { c ->
                FilterChip(
                    selected = c == selected,
                    onClick = { onSelect(c) },
                    label = { Text(c.displayName) },
                    leadingIcon = { Icon(c.safetyIcon, contentDescription = null, Modifier.size(18.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = c.zoneColor,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White,
                    ),
                )
            }
        }
        Text("Or a civic problem", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IssueCategory.CIVIC.forEach { c ->
                FilterChip(selected = c == selected, onClick = { onSelect(c) }, label = { Text(c.displayName) })
            }
        }
    }
}

/** When it felt unsafe. [isAuto] = still the value derived from the capture time and place. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimeOfDayPicker(
    selected: TimeOfDay,
    isAuto: Boolean,
    onSelect: (TimeOfDay) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            if (isAuto) "Time of day (set from when and where you are — change it if it happened earlier)" else "Time of day",
            style = MaterialTheme.typography.labelLarge,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimeOfDay.entries.forEach { t ->
                FilterChip(selected = t == selected, onClick = { onSelect(t) }, label = { Text(t.displayName) })
            }
        }
        Text("${selected.displayName}: ${selected.hint}", style = MaterialTheme.typography.bodySmall)
    }
}

/** Optional "what made it feel unsafe?" tags. They describe the place, never people. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SafetyTagPicker(selected: Set<SafetyTag>, onToggle: (SafetyTag) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("What made it feel unsafe? (optional)", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SafetyTag.entries.forEach { tag ->
                val on = tag in selected
                FilterChip(
                    selected = on,
                    onClick = { onToggle(tag) },
                    label = { Text(tag.displayName) },
                    leadingIcon = if (on) {
                        { Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(18.dp)) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}
