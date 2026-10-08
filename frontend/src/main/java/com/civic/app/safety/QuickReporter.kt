package com.civic.app.safety

import com.civic.app.data.local.ReportEntity
import com.civic.app.data.repository.ReportRepository
import com.civic.app.location.LocationSource
import com.civic.app.safety.time.TimeOfDayClassifier
import com.civic.shared.model.IssueCategory
import com.civic.shared.model.TimeOfDay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** What happened to a one-tap report, for the confirmation snackbar. */
sealed interface QuickReportEvent {
    val reportId: Long

    /** Saved. [located] = it already has a position (a fresh fix may still refine it). */
    data class Saved(
        override val reportId: Long,
        val category: IssueCategory,
        val timeOfDay: TimeOfDay,
        val located: Boolean,
    ) : QuickReportEvent

    /** No position could be found at all; the report was kept without one. */
    data class NoLocation(override val reportId: Long) : QuickReportEvent
}

/**
 * One-tap "I felt unsafe here": saves instantly, no form. Speed matters more than precision for someone who
 * feels threatened, so it saves with a recent cached position right away and then refines it with a fresh GPS fix.
 * Runs in the app-wide [scope] so navigating away doesn't cancel the save or the location refinement.
 */
class QuickReporter(
    private val repository: ReportRepository,
    private val locationProvider: LocationSource,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    // A channel, not a SharedFlow: an event sent before the UI subscribes (e.g. a launcher-shortcut report fired
    // while the activity starts) is buffered and delivered once, instead of being dropped.
    private val _events = Channel<QuickReportEvent>(Channel.BUFFERED)
    val events: Flow<QuickReportEvent> = _events.receiveAsFlow()

    /** [hasLocationPermission] false: still saves the report (the record is useful), just without a position. */
    fun report(category: IssueCategory, hasLocationPermission: Boolean) {
        require(category.isSafety) { "Quick reports are for safety categories, got $category" }
        scope.launch {
            val now = clock()
            val cached = if (hasLocationPermission) {
                runCatching { locationProvider.lastKnown(maxAgeMs = CACHED_FIX_MAX_AGE_MS) }.getOrNull()
            } else {
                null
            }
            val id = repository.addReport(
                ReportEntity(
                    category = category.name,
                    description = "",
                    localImagePath = null,
                    latitude = cached?.latitude,
                    longitude = cached?.longitude,
                    capturedAt = now,
                ),
            )
            val timeOfDay = TimeOfDayClassifier.classify(now, cached?.latitude, cached?.longitude)
            _events.send(QuickReportEvent.Saved(id, category, timeOfDay, located = cached != null))

            if (!hasLocationPermission) {
                _events.send(QuickReportEvent.NoLocation(id))
                return@launch
            }
            val fresh = runCatching { locationProvider.freshLocation(FRESH_FIX_TIMEOUT_MS) }.getOrNull()
            when {
                fresh != null -> repository.setLocation(id, fresh) // no-op if the user already undid it
                cached == null -> _events.send(QuickReportEvent.NoLocation(id))
            }
        }
    }

    fun undo(reportId: Long) {
        scope.launch { repository.delete(reportId) }
    }

    companion object {
        /** A cached fix older than this may be from somewhere else entirely. */
        const val CACHED_FIX_MAX_AGE_MS = 2 * 60_000L
        const val FRESH_FIX_TIMEOUT_MS = 15_000L
    }
}
