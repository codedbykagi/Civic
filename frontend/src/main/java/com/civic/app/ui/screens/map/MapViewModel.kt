package com.civic.app.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.civic.app.CivicApplication
import com.civic.app.data.local.ReportEntity
import com.civic.app.data.repository.ReportRepository
import com.civic.app.data.repository.decodeTags
import com.civic.app.location.LocationProvider
import com.civic.app.safety.ZoneRelevance
import com.civic.app.safety.effectiveTimeOfDay
import com.civic.app.safety.issueCategory
import com.civic.app.safety.routing.RoutePlan
import com.civic.app.safety.routing.RoutingResult
import com.civic.app.safety.routing.SafeRoutePlanner
import com.civic.app.safety.time.TimeOfDayClassifier
import com.civic.app.safety.zones.HeatZone
import com.civic.app.safety.zones.HeatZones
import com.civic.app.safety.zones.ZoneInput
import com.civic.app.safety.zones.ZoneKind
import com.civic.shared.model.GeoLocation
import com.civic.shared.model.TimeOfDay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the map layers show. [timeFilter] null = all parts of the day. */
data class MapFilter(
    val timeFilter: TimeOfDay? = null,
    val kinds: Set<ZoneKind> = ZoneKind.entries.toSet(),
    val showCivicPins: Boolean = true,
)

data class MapContent(val zones: List<HeatZone>, val civicPins: List<ReportEntity>)

sealed interface RouteState {
    data object Idle : RouteState

    /** Destination chosen, waiting for the user to confirm (requests leave the phone only after that). */
    data class Destination(val destination: GeoLocation) : RouteState
    data class Planning(val destination: GeoLocation) : RouteState
    data class Planned(val origin: GeoLocation, val destination: GeoLocation, val plan: RoutePlan, val selected: Int) : RouteState
    data class Failed(val destination: GeoLocation, val message: String) : RouteState
}

class MapViewModel(
    private val repository: ReportRepository,
    private val locationProvider: LocationProvider,
    private val planner: SafeRoutePlanner,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _filter = MutableStateFlow(MapFilter())
    val filter: StateFlow<MapFilter> = _filter.asStateFlow()

    private val allReports = repository.observeAllReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** null = still loading. Safety reports only ever show up aggregated into zones, never as pins. */
    val content: StateFlow<MapContent?> = combine(allReports, _filter) { reports, filter ->
        reports ?: return@combine null
        MapContent(
            zones = HeatZones.build(zoneInputs(reports), clock(), filter.timeFilter, filter.kinds),
            civicPins = if (filter.showCivicPins) {
                reports.filter { it.issueCategory?.isSafety != true && it.latitude != null && it.longitude != null }
            } else {
                emptyList()
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The part of the day it is right now, by the sun where the most recent report is (else the clock). */
    val currentTimeOfDay: StateFlow<TimeOfDay> = allReports.map { reports ->
        val anchor = reports?.firstOrNull { it.latitude != null }
        TimeOfDayClassifier.classify(clock(), anchor?.latitude, anchor?.longitude)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimeOfDayClassifier.classify(clock(), null, null))

    private val _route = MutableStateFlow<RouteState>(RouteState.Idle)
    val route: StateFlow<RouteState> = _route.asStateFlow()
    private var planJob: Job? = null

    fun setTimeFilter(time: TimeOfDay?) = _filter.update { it.copy(timeFilter = time) }

    fun toggleKind(kind: ZoneKind) = _filter.update {
        it.copy(kinds = if (kind in it.kinds) it.kinds - kind else it.kinds + kind)
    }

    fun toggleCivicPins() = _filter.update { it.copy(showCivicPins = !it.showCivicPins) }

    fun chooseDestination(destination: GeoLocation) {
        planJob?.cancel()
        _route.value = RouteState.Destination(destination)
    }

    fun selectRoute(index: Int) = _route.update { if (it is RouteState.Planned) it.copy(selected = index) else it }

    fun clearRoute() {
        planJob?.cancel()
        _route.value = RouteState.Idle
    }

    /** Plans from the current GPS position (caller must hold location permission) around every reported zone. */
    fun planRoute() {
        val destination = when (val r = _route.value) {
            is RouteState.Destination -> r.destination
            is RouteState.Failed -> r.destination
            is RouteState.Planned -> r.destination
            else -> return
        }
        planJob?.cancel()
        _route.value = RouteState.Planning(destination)
        planJob = viewModelScope.launch {
            val origin = runCatching { locationProvider.currentLocation() }.getOrNull()
            if (origin == null) {
                _route.value = RouteState.Failed(destination, "Couldn't get your location. Turn on GPS and try again.")
                return@launch
            }
            val now = TimeOfDayClassifier.classify(clock(), origin.latitude, origin.longitude)
            // Route around every zone (not just the ones currently shown), weighted for this part of the day.
            val zones = HeatZones.build(zoneInputs(allReports.value.orEmpty()), clock())
            _route.value = when (val result = planner.plan(origin, destination, ZoneRelevance.toAvoidZones(zones, now))) {
                is RoutingResult.Success -> if (result.plan.options.isEmpty()) {
                    RouteState.Failed(destination, "No walking route found to that point.")
                } else {
                    RouteState.Planned(origin, destination, result.plan, selected = 0)
                }
                is RoutingResult.Failure -> RouteState.Failed(destination, result.message)
            }
        }
    }

    private fun zoneInputs(reports: List<ReportEntity>): List<ZoneInput> = reports.mapNotNull { r ->
        val lat = r.latitude ?: return@mapNotNull null
        val lon = r.longitude ?: return@mapNotNull null
        val category = r.issueCategory ?: return@mapNotNull null
        ZoneInput(r.localId, lat, lon, category, r.effectiveTimeOfDay, r.capturedAt, decodeTags(r.tags))
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as CivicApplication).container
                MapViewModel(c.reportRepository, c.locationProvider, c.routePlanner)
            }
        }
    }
}
