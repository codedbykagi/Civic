package com.civic.app.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Create a cloud account. [onNeedsConfirmation] is not a failure path: with Supabase's "Confirm email" on, the
 * account exists but has no session yet, so the user is sent to sign-in with the message to check their inbox.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onBack: () -> Unit,
    onSignedIn: () -> Unit,
    onSignIn: () -> Unit,
    onNeedsConfirmation: (String) -> Unit,
    /** See [SignInScreen]: null means "Skip for now" creates a device profile. */
    onSkip: (() -> Unit)? = null,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory),
) {
    val ui by viewModel.state.collectAsState()
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Create an account") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            windowInsets = WindowInsets(0),
        )
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; viewModel.dismissError() },
                label = { Text("Display name") },
                supportingText = { Text("This is what other people see on your reports.") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; viewModel.dismissError() },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            PasswordField(
                value = password,
                onValueChange = { password = it; viewModel.dismissError() },
                label = "Password",
                visible = showPassword,
                onToggleVisible = { showPassword = !showPassword },
                imeAction = ImeAction.Done,
                supportingText = "At least 6 characters.",
            )

            AuthMessages(error = ui.error, notice = ui.notice)

            AuthSubmitButton(
                label = "Create account",
                busy = ui.busy,
                onClick = {
                    viewModel.signUp(
                        email = email,
                        password = password,
                        displayName = name,
                        onSignedIn = onSignedIn,
                        onNeedsConfirmation = onNeedsConfirmation,
                    )
                },
            )
            TextButton(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) {
                Text("Already have an account? Sign in")
            }
            // Never trap the user behind a login wall: a device profile always works.
            TextButton(
                onClick = {
                    if (onSkip != null) onSkip() else viewModel.useDeviceProfile(DEFAULT_NAME) { onSignedIn() }
                },
                enabled = !ui.busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Skip for now") }
        }
    }
}
