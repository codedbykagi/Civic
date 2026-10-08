package com.civic.app.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.civic.app.CivicApplication
import com.civic.app.data.local.CommentEntity
import com.civic.app.data.local.ReportEntity
import com.civic.app.data.repository.ReportRepository
import com.civic.shared.model.IssueStatus
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

class ReportDetailViewModel(private val reportId: Long, private val repository: ReportRepository) : ViewModel() {

    val state: StateFlow<DetailState> = repository.observeReport(reportId)
        .map { report -> if (report == null) DetailState.NotFound else DetailState.Loaded(report) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailState.Loading)

    val comments: StateFlow<List<CommentEntity>> = repository.observeComments(reportId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addComment(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.addComment(reportId, trimmed) }
    }

    fun setStatus(status: IssueStatus) {
        viewModelScope.launch { repository.setStatus(reportId, status) }
    }

    fun upvote() {
        viewModelScope.launch { repository.upvote(reportId) }
    }

    companion object {
        const val ARG_ID = "id"

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CivicApplication
                val id = checkNotNull(createSavedStateHandle().get<Long>(ARG_ID)) { "report id missing" }
                ReportDetailViewModel(id, app.container.reportRepository)
            }
        }
    }
}
