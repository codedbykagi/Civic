package com.civic.app.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/** Email sign-in. Only reachable when the build has a server; see [AuthViewModel.isCloudAvailable]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    onBack: () -> Unit,
    onSignedIn: () -> Unit,
    onSignUp: () -> Unit,
    /** Carried in from sign-up ("check your email"): the ViewModel is per-destination, so state can't cross. */
    incomingNotice: String? = null,
    /**
     * Null (onboarding) means "Skip for now" creates a device profile. Inside the app the user already has an
     * identity, so the caller passes a plain dismiss instead — creating one would rename them to [DEFAULT_NAME].
     */
    onSkip: (() -> Unit)? = null,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory),
) {
    val ui by viewModel.state.collectAsState()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Sign in") },
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
            )

            TextButton(onClick = { viewModel.resetPassword(email) }, enabled = !ui.busy) {
                Text("Forgot password?")
            }

            AuthMessages(error = ui.error, notice = ui.notice ?: incomingNotice)

            AuthSubmitButton(
                label = "Sign in",
                busy = ui.busy,
                onClick = { viewModel.signIn(email, password) { onSignedIn() } },
            )
            TextButton(onClick = onSignUp, modifier = Modifier.fillMaxWidth()) {
                Text("New here? Create an account")
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

/** The display name a "Skip for now" profile gets, matching [com.civic.app.data.auth.AuthRepository]'s fallback. */
internal const val DEFAULT_NAME = "Neighbour"

/** Shared by the auth screens: an error in the error colour, or a neutral notice. */
@Composable
internal fun AuthMessages(error: String?, notice: String?) {
    if (error != null) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
    if (notice != null) {
        Card(Modifier.fillMaxWidth()) {
            Text(notice, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
        }
    }
}

/** Shared by the auth screens: a password field with a show/hide toggle. */
@Composable
internal fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    imeAction: ImeAction,
    supportingText: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        supportingText = supportingText?.let { { Text(it) } },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        trailingIcon = {
            IconButton(onClick = onToggleVisible) {
                Icon(
                    if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (visible) "Hide password" else "Show password",
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Shared by the auth screens: the primary button, swapping its label for a spinner while the call is in flight. */
@Composable
internal fun AuthSubmitButton(label: String, busy: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Text(label)
        }
    }
}
