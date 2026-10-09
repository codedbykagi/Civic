package com.civic.app.ui.screens.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.civic.app.CivicApplication
import com.civic.app.data.auth.AuthRepository
import com.civic.app.data.local.ReportEntity
import com.civic.app.data.repository.ReportRepository
import com.civic.app.data.sync.SyncManager
import com.civic.app.data.sync.SyncStatus
import com.civic.shared.model.IssueCategory
import com.civic.shared.model.IssueStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Feed filter; null fields mean "any". */
data class FeedFilter(val category: IssueCategory? = null, val status: IssueStatus? = null) {
    val isActive get() = category != null || status != null
}

class FeedViewModel(
    private val repository: ReportRepository,
    private val syncManager: SyncManager,
    authRepository: AuthRepository,
) : ViewModel() {

    private val _filter = MutableStateFlow(FeedFilter())
    val filter: StateFlow<FeedFilter> = _filter.asStateFlow()

    /** Civic reports only (null = still loading); private safety reports never appear in the feed. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val reports: StateFlow<List<ReportEntity>?> = _filter
        .flatMapLatest { repository.observeCivicFeed(it.category, it.status) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val syncStatus: StateFlow<SyncStatus> = syncManager.status

    /** Whether there is a cloud account to sync as; it can only change when the auth state does. */
    val canSync: StateFlow<Boolean> = authRepository.state
        .map { syncManager.canSync }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), syncManager.canSync)

    /** Tapping the selected chip again clears it. */
    fun toggleCategory(category: IssueCategory) =
        _filter.update { it.copy(category = if (it.category == category) null else category) }

    fun toggleStatus(status: IssueStatus) =
        _filter.update { it.copy(status = if (it.status == status) null else status) }

    fun clearFilter() = _filter.update { FeedFilter() }

    /** Pulls the shared feed (and pushes anything pending). Failures surface through [syncStatus]. */
    fun refresh() {
        viewModelScope.launch { syncManager.syncNow() }
    }

    fun upvote(report: ReportEntity) {
        viewModelScope.launch {
            val upvotedAfter = !report.upvotedByMe
            repository.toggleUpvote(report.localId)
            syncManager.pushUpvote(report.localId, upvotedAfter)
        }
    }

    fun delete(report: ReportEntity) {
        viewModelScope.launch { repository.delete(report) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CivicApplication
                FeedViewModel(
                    repository = app.container.reportRepository,
                    syncManager = app.container.syncManager,
                    authRepository = app.container.authRepository,
                )
            }
        }
    }
}
