package com.civic.app.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.civic.app.data.auth.Account
import com.civic.app.data.sync.SyncStatus
import com.civic.app.ui.components.Avatar
import com.civic.app.ui.components.HelplineRow
import com.civic.app.ui.components.relativeTime

/**
 * Your profile: who you are, **where your data lives**, and what you have reported.
 *
 * The account row is deliberately explicit about storage, because the app behaves very differently with and
 * without a backend and the user has no other way to tell.
 */
@Composable
fun ProfileScreen(
    onMySafetyReports: () -> Unit,
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory),
) {
    val account by viewModel.account.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val ui by viewModel.ui.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val canSync by viewModel.canSync.collectAsState()
    var editing by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ProfileHeader(account, onEdit = { editing = true })
        HorizontalDivider()
        AccountStateCard(
            account = account,
            isCloudAvailable = viewModel.isCloudAvailable,
            busy = ui.busy,
            onSignIn = onSignIn,
            onSignUp = onSignUp,
            onSignOut = viewModel::signOut,
        )
        if (canSync) {
            SyncRow(status = syncStatus, pending = stats.pending, onSyncNow = viewModel::syncNow)
        }
        ui.error?.takeIf { !editing }?.let { message ->
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        HorizontalDivider()
        Text("Civic reports: ${stats.civicCount}", style = MaterialTheme.typography.titleMedium)
        Text("Total upvotes: ${stats.upvotes}")
        Text("Resolved: ${stats.resolved}")
        stats.byCategory.forEach { (category, count) -> Text("${category.displayName}: $count") }
        HorizontalDivider()
        TextButton(onClick = onMySafetyReports) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Lock, contentDescription = null)
                Text("  My safety reports: ${stats.safetyCount} (private)")
            }
        }
        HorizontalDivider()
        HelplineRow()
    }

    if (editing) {
        EditProfileDialog(
            account = account,
            busy = ui.busy,
            error = ui.error,
            onDismiss = {
                editing = false
                viewModel.clearError()
            },
            onSave = { name, bio -> viewModel.saveProfile(name, bio) { editing = false } },
        )
    }
}

@Composable
private fun ProfileHeader(account: Account?, onEdit: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Avatar(account?.avatar, account?.initials ?: "?", size = 64.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(account?.displayName ?: "No profile yet", style = MaterialTheme.typography.headlineSmall)
            account?.username?.let { Text("@$it", style = MaterialTheme.typography.bodyMedium) }
            account?.bio?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (account != null) {
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit profile") }
        }
    }
}

/** The one place that says plainly whether anything leaves this phone. */
@Composable
private fun AccountStateCard(
    account: Account?,
    isCloudAvailable: Boolean,
    busy: Boolean,
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
    onSignOut: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                account?.isCloud == true -> {
                    Text("Signed in", style = MaterialTheme.typography.titleSmall)
                    account.email?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    Text("Your reports sync to the server.", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = onSignOut, enabled = !busy) { Text("Sign out") }
                }
                isCloudAvailable -> {
                    Text("Saved on this phone only.", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Sign in to back your reports up and see other people's. The reports you already " +
                            "wrote here come with you.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onSignIn, enabled = !busy) { Text("Sign in") }
                        OutlinedButton(onClick = onSignUp, enabled = !busy) { Text("Create account") }
                    }
                }
                // No backend in this build: showing a sign-in button here would be a button that cannot work.
                else -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.CloudOff, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("On this phone only", style = MaterialTheme.typography.titleSmall)
                    }
                    Text(
                        "This build has no server yet — everything stays on this phone.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "To turn on accounts and the shared feed, follow docs/BACKEND_SETUP.md.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncRow(status: SyncStatus, pending: Int, onSyncNow: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (status is SyncStatus.Running) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
        }
        Text(
            syncWords(status, pending),
            style = MaterialTheme.typography.bodySmall,
            color = if (status is SyncStatus.Failed) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onSyncNow, enabled = status !is SyncStatus.Running) { Text("Sync now") }
    }
}

/** The sync state in words a user can act on; [pending] is what is still queued locally. */
private fun syncWords(status: SyncStatus, pending: Int): String = when (status) {
    SyncStatus.Running -> "Syncing…"
    SyncStatus.OfflineOnly -> "Not syncing — no account signed in."
    is SyncStatus.Failed -> status.message
    is SyncStatus.Done -> if (pending > 0) pendingWords(pending) else "Synced ${relativeTime(status.atMillis)}"
    SyncStatus.Idle -> if (pending > 0) pendingWords(pending) else "Not synced yet"
}

private fun pendingWords(pending: Int): String =
    if (pending == 1) "1 report waiting to upload" else "$pending reports waiting to upload"

@Composable
private fun EditProfileDialog(
    account: Account?,
    busy: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(account?.displayName.orEmpty()) }
    var bio by rememberSaveable { mutableStateOf(account?.bio.orEmpty()) }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Edit profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 60) name = it },
                    label = { Text("Display name") },
                    enabled = !busy,
                    singleLine = true,
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { if (it.length <= 200) bio = it },
                    label = { Text("Bio (optional)") },
                    enabled = !busy,
                    minLines = 2,
                )
                // TODO: profile photo — route to CaptureScreen and pass the saved file to updateProfile(avatarFile=).
                error?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, bio) }, enabled = !busy && name.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } },
    )
}
