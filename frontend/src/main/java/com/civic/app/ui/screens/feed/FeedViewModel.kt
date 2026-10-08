package com.civic.app.ui.screens.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.civic.app.CivicApplication
import com.civic.app.data.local.ReportEntity
import com.civic.app.data.repository.ReportRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FeedViewModel(private val repository: ReportRepository) : ViewModel() {

    /** null = still loading. */
    val reports: StateFlow<List<ReportEntity>?> = repository.observeLocalFeed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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
