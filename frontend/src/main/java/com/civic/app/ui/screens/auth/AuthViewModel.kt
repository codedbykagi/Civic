package com.civic.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.civic.app.CivicApplication
import com.civic.app.data.auth.AuthRepository
import com.civic.app.data.auth.SignUpOutcome
import com.civic.app.data.cloud.CloudException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * [error] holds both client-side validation messages and the user-ready [CloudException.message] from the server;
 * [notice] is for things that are not failures ("check your email", "a reset link is on its way").
 */
data class AuthUiState(
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
)

/** Form state and the suspend auth calls for Welcome / Sign in / Sign up. */
class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    /** False when this build has no Supabase credentials: email accounts are impossible, so don't offer them. */
    val isCloudAvailable: Boolean get() = repository.isCloudAvailable

    fun signIn(email: String, password: String, onDone: () -> Unit) {
        val problem = validateEmail(email) ?: validatePassword(password)
        if (problem != null) return fail(problem)
        run { repository.signIn(email.trim(), password); onDone() }
    }

    fun signUp(
        email: String,
        password: String,
        displayName: String,
        onSignedIn: () -> Unit,
        onNeedsConfirmation: (String) -> Unit,
    ) {
        val problem = validateName(displayName) ?: validateEmail(email) ?: validatePassword(password)
        if (problem != null) return fail(problem)
        run {
            when (val outcome = repository.signUp(email.trim(), password, displayName.trim())) {
                SignUpOutcome.SignedIn -> onSignedIn()
                // Not an error: the account exists, it just can't be used until the link in the email is clicked.
                is SignUpOutcome.NeedsEmailConfirmation -> onNeedsConfirmation(outcome.email)
            }
        }
    }

    /** No network, no backend needed — this is the path that always works. */
    fun useDeviceProfile(name: String, onDone: () -> Unit) {
        val problem = validateName(name)
        if (problem != null) return fail(problem)
        repository.useDeviceProfile(name)
        onDone()
    }

    fun resetPassword(email: String) {
        val problem = validateEmail(email)
        if (problem != null) return fail(problem)
        run {
            repository.requestPasswordReset(email.trim())
            // Deliberately vague: confirming whether an address has an account would leak who is registered.
            _state.update { it.copy(notice = "If that address has an account, a reset link is on its way.") }
        }
    }

    fun dismissError() = _state.update { it.copy(error = null, notice = null) }

    /** Runs [block] with the busy flag set, turning a [CloudException] into the user-ready [AuthUiState.error]. */
    private fun run(block: suspend () -> Unit) {
        _state.update { it.copy(busy = true, error = null, notice = null) }
        viewModelScope.launch {
            try {
                block()
                _state.update { it.copy(busy = false) }
            } catch (e: CloudException) {
                // e.message is already written for end users; never dress it up or show a stack trace.
                _state.update { it.copy(busy = false, error = e.message) }
            }
        }
    }

    private fun fail(message: String) {
        _state.update { it.copy(busy = false, error = message, notice = null) }
    }

    companion object {
        /** Supabase's own default minimum. */
        private const val MIN_PASSWORD = 6

        private fun validateEmail(email: String): String? {
            val value = email.trim()
            val at = value.indexOf('@')
            val hasDotAfterAt = at > 0 && value.indexOf('.', at) > at + 1
            return if (hasDotAfterAt && !value.endsWith('.')) null else "Enter a valid email address."
        }

        private fun validatePassword(password: String): String? =
            if (password.length >= MIN_PASSWORD) null else "Password must be at least $MIN_PASSWORD characters."

        private fun validateName(name: String): String? =
            if (name.isNotBlank()) null else "Tell us what to call you."

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CivicApplication
                AuthViewModel(app.container.authRepository)
            }
        }
    }
}
