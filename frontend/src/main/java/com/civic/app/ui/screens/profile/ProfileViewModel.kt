package com.civic.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.civic.app.CivicApplication
import com.civic.app.data.auth.Account
import com.civic.app.data.auth.AuthRepository
import com.civic.app.data.auth.AuthState
import com.civic.app.data.cloud.CloudException
import com.civic.app.data.local.ReportEntity
import com.civic.app.data.repository.ReportRepository
import com.civic.app.data.sync.SyncManager
import com.civic.app.data.sync.SyncStatus
import com.civic.shared.model.IssueCategory
import com.civic.shared.model.IssueStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the profile counts up. [pending] only matters while signed in: those are the rows waiting to upload. */
data class ProfileStats(
    val civicCount: Int = 0,
    val upvotes: Int = 0,
    val resolved: Int = 0,
    val byCategory: List<Pair<IssueCategory, Int>> = emptyList(),
    val safetyCount: Int = 0,
    val pending: Int = 0,
)

/** Transient state of the profile actions (edit, sign out, manual sync). */
data class ProfileUiState(val busy: Boolean = false, val error: String? = null)

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val syncManager: SyncManager,
    reportRepository: ReportRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(ProfileUiState())
    val ui: StateFlow<ProfileUiState> = _ui.asStateFlow()

    val account: StateFlow<Account?> = authRepository.state
        .map { (it as? AuthState.Active)?.account }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), authRepository.account)

    val syncStatus: StateFlow<SyncStatus> = syncManager.status

    /** True only with a cloud account: recomputed on every auth change, which is when it can flip. */
    val canSync: StateFlow<Boolean> = authRepository.state
        .map { syncManager.canSync }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), syncManager.canSync)

    /** Whether this build has a backend at all. Fixed for the process, so a plain value is enough. */
    val isCloudAvailable: Boolean get() = authRepository.isCloudAvailable

    val stats: StateFlow<ProfileStats> = combine(
        reportRepository.observeCivicFeed(null, null),
        reportRepository.observeSafetyReports(),
        authRepository.state,
    ) { civic, safety, auth ->
        val mine = (auth as? AuthState.Active)?.account?.id
        ProfileStats(
            civicCount = civic.size,
            upvotes = civic.sumOf { it.upvotes },
            resolved = civic.count { it.status == IssueStatus.RESOLVED.name },
            byCategory = IssueCategory.CIVIC
                .map { c -> c to civic.count { it.category == c.name } }
                .filter { it.second > 0 },
            safetyCount = safety.size,
            pending = (civic + safety).count { it.isPendingUpload(mine) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileStats())

    fun saveProfile(displayName: String, bio: String, onSaved: () -> Unit = {}) {
        if (_ui.value.busy) return
        _ui.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            val result = runCatching { authRepository.updateProfile(displayName = displayName, bio = bio) }
            _ui.update { it.copy(busy = false, error = result.userMessage()) }
            if (result.isSuccess) onSaved()
        }
    }

    fun signOut() {
        if (_ui.value.busy) return
        _ui.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            val result = runCatching { authRepository.signOut() }
            _ui.update { it.copy(busy = false, error = result.userMessage()) }
        }
    }

    fun syncNow() {
        viewModelScope.launch { syncManager.syncNow() } // failures surface through syncStatus, not the error state
    }

    fun clearError() = _ui.update { it.copy(error = null) }

    /** [CloudException] messages are already written for end users; anything else gets a generic line. */
    private fun Result<Unit>.userMessage(): String? = exceptionOrNull()?.let {
        (it as? CloudException)?.message ?: it.message ?: "Something went wrong."
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CivicApplication
                ProfileViewModel(
                    authRepository = app.container.authRepository,
                    syncManager = app.container.syncManager,
                    reportRepository = app.container.reportRepository,
                )
            }
        }
    }
}

private fun ReportEntity.isPendingUpload(myAccountId: String?): Boolean =
    !isSynced && myAccountId != null && authorId == myAccountId
