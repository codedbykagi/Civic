package com.civic.app.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.civic.app.R

/**
 * First launch. The only thing the user has to do is pick a name: that gets them a device profile and working
 * app with no network at all. Accounts are an extra, offered underneath, and only when the build has a server.
 */
@Composable
fun WelcomeScreen(
    onSignedIn: () -> Unit,
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory),
) {
    val ui by viewModel.state.collectAsState()
    var name by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
        Text(
            "Report local issues — potholes, broken streetlights, fallen trees — and mark places that felt " +
                "unsafe, so your neighbourhood can see them.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; viewModel.dismissError() },
            label = { Text("What should we call you?") },
            supportingText = { Text("Shown on your reports. You can change it later.") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        AuthMessages(error = ui.error, notice = ui.notice)

        Button(
            onClick = { viewModel.useDeviceProfile(name) { onSignedIn() } },
            enabled = !ui.busy,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Start reporting") }

        if (viewModel.isCloudAvailable) {
            TextButton(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) {
                Text("Have an account? Sign in")
            }
            TextButton(onClick = onSignUp, modifier = Modifier.fillMaxWidth()) {
                Text("Create an account to sync across devices")
            }
        } else {
            // No Supabase credentials in this build, so there is nothing to sign in to. Say so instead of
            // showing buttons that could only fail.
            Card(Modifier.fillMaxWidth()) {
                Text(
                    "This build has no server yet, so your reports stay on this phone — see " +
                        "docs/BACKEND_SETUP.md to connect one.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}
