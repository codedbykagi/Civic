package com.civic.app.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.civic.app.CivicApplication
import com.civic.app.data.auth.Account
import com.civic.app.data.auth.AuthRepository
import com.civic.app.data.auth.AuthState
import com.civic.app.data.local.CommentEntity
import com.civic.app.data.local.ReportEntity
import com.civic.app.data.repository.ReportRepository
import com.civic.app.data.sync.SyncManager
import com.civic.shared.model.IssueCategory
import com.civic.shared.model.IssueStatus
import com.civic.shared.model.SafetyTag
import com.civic.shared.model.TimeOfDay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DetailState {
    data object Loading : DetailState
    data object NotFound : DetailState
    data class Loaded(val report: ReportEntity) : DetailState
}

class ReportDetailViewModel(
    private val reportId: Long,
    private val repository: ReportRepository,
    private val syncManager: SyncManager,
    authRepository: AuthRepository,
) : ViewModel() {

    val state: StateFlow<DetailState> = repository.observeReport(reportId)
        .map { report -> if (report == null) DetailState.NotFound else DetailState.Loaded(report) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailState.Loading)

    val comments: StateFlow<List<CommentEntity>> = repository.observeComments(reportId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Null while the user has no profile at all: the composer is disabled then. */
    val account: StateFlow<Account?> = authRepository.state
        .map { (it as? AuthState.Active)?.account }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), authRepository.account)

    /** Pulls other people's comments once per visit. A no-op offline or for a report never pushed. */
    fun syncComments() {
        viewModelScope.launch { syncManager.syncComments(reportId) }
    }

    fun addComment(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.addComment(reportId, trimmed) }
    }

    fun setStatus(status: IssueStatus) {
        viewModelScope.launch { repository.setStatus(reportId, status) }
    }

    fun upvote() {
        viewModelScope.launch {
            val upvotedAfter = (state.value as? DetailState.Loaded)?.report?.upvotedByMe != true
            repository.toggleUpvote(reportId)
            syncManager.pushUpvote(reportId, upvotedAfter)
        }
    }

    /** [timeOfDay] null = keep deriving it from the capture time and place. */
    fun updateDetails(category: IssueCategory, description: String, timeOfDay: TimeOfDay?, tags: Set<SafetyTag>) {
        viewModelScope.launch { repository.updateDetails(reportId, category, description.trim(), timeOfDay, tags) }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.delete(reportId)
            onDeleted()
        }
    }

    companion object {
        const val ARG_ID = "id"

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CivicApplication
                val id = checkNotNull(createSavedStateHandle().get<Long>(ARG_ID)) { "report id missing" }
                ReportDetailViewModel(
                    reportId = id,
                    repository = app.container.reportRepository,
                    syncManager = app.container.syncManager,
                    authRepository = app.container.authRepository,
                )
            }
        }
    }
}
