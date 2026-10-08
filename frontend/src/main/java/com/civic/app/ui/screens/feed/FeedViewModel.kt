package com.civic.app.ui.screens.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.civic.app.CivicApplication
import com.civic.app.data.local.ReportEntity
import com.civic.app.data.repository.ReportRepository
import com.civic.shared.model.IssueCategory
import com.civic.shared.model.IssueStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Feed filter; null fields mean "any". */
data class FeedFilter(val category: IssueCategory? = null, val status: IssueStatus? = null) {
    val isActive get() = category != null || status != null
}

class FeedViewModel(private val repository: ReportRepository) : ViewModel() {

    private val _filter = MutableStateFlow(FeedFilter())
    val filter: StateFlow<FeedFilter> = _filter.asStateFlow()

    /** null = still loading. Map and Profile never set a filter, so they see every report. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val reports: StateFlow<List<ReportEntity>?> = _filter
        .flatMapLatest { repository.observeLocalFeed(it.category, it.status) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Tapping the selected chip again clears it. */
    fun toggleCategory(category: IssueCategory) =
        _filter.update { it.copy(category = if (it.category == category) null else category) }

    fun toggleStatus(status: IssueStatus) =
        _filter.update { it.copy(status = if (it.status == status) null else status) }

    fun clearFilter() = _filter.update { FeedFilter() }

    fun upvote(report: ReportEntity) {
        viewModelScope.launch { repository.upvote(report.localId) }
    }

    fun delete(report: ReportEntity) {
        viewModelScope.launch { repository.delete(report) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CivicApplication
                FeedViewModel(app.container.reportRepository)
            }
        }
    }
}
