package com.civic.app.ui.screens.map

import android.Manifest
import android.content.Context
import android.graphics.Paint
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.civic.app.R
import com.civic.app.appContainer
import com.civic.app.data.local.ReportEntity
import com.civic.app.safety.routing.GoogleMapsHandoff
import com.civic.app.safety.routing.RouteOption
import com.civic.app.safety.routing.RouteRole
import com.civic.app.safety.zones.HeatZone
import com.civic.app.safety.zones.HeatZoneOverlay
import com.civic.app.safety.zones.ZoneKind
import com.civic.app.safety.zones.ZoneStyle
import com.civic.app.ui.categoryName
import com.civic.app.ui.components.LocalAppSnackbarHost
import com.civic.app.ui.components.PlaceholderScreen
import com.civic.app.ui.components.QuickUnsafePanel
import com.civic.app.ui.components.StatusBadge
import com.civic.app.ui.components.hasPreciseLocation
import com.civic.app.ui.components.rememberQuickReportAction
import com.civic.app.ui.formatTime
import com.civic.app.ui.openInMaps
import com.civic.app.ui.openUrl
import com.civic.app.ui.statusOf
import com.civic.shared.model.GeoLocation
import com.civic.shared.model.TimeOfDay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlin.math.roundToInt

/** Rough centre of India: where the map starts before there is anything to show. */
private val INDIA = GeoPoint(22.5, 79.0)

/** Room for a two-line snackbar with its action buttons. */
private val SNACKBAR_CLEARANCE = 88.dp

/**
 * Safety map: heat zones from reports (colour = who it felt unsafe for, size/opacity = how many reports),
 * filterable by part of the day, civic issue pins, and walking routes with fewer reported zones.
 * Long-press anywhere to plan a route there.
 */
@Composable
fun MapScreen(
    onOpenReport: (Long) -> Unit,
    viewModel: MapViewModel = viewModel(factory = MapViewModel.Factory),
) {
    val context = LocalContext.current
    val content by viewModel.content.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val route by viewModel.route.collectAsState()
    val now by viewModel.currentTimeOfDay.collectAsState()
    val quickReport = rememberQuickReportAction()
    val scope = rememberCoroutineScope()

    var selectedZoneKey by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedPinId by rememberSaveable { mutableStateOf<Long?>(null) }
    var centerOn by remember { mutableStateOf<GeoLocation?>(null) }

    // Location is needed to plan from where the user is and to centre the map on them.
    var afterPermission by remember { mutableStateOf<(() -> Unit)?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (hasPreciseLocation(context)) afterPermission?.invoke()
        else Toast.makeText(context, "Location permission is needed for this", Toast.LENGTH_LONG).show()
        afterPermission = null
    }
    fun withLocation(action: () -> Unit) {
        if (hasPreciseLocation(context)) {
            action()
        } else {
            afterPermission = action
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
        }
    }

    val c = content
    if (c == null) {
        PlaceholderScreen("Loading…", "") { CircularProgressIndicator() }
        return
    }
    val selectedZone = c.zones.firstOrNull { it.key == selectedZoneKey }
    val selectedPin = c.civicPins.firstOrNull { it.localId == selectedPinId }

    Box(Modifier.fillMaxSize()) {
        ReportsMap(
            zones = c.zones,
            pins = c.civicPins,
            route = route,
            centerOn = centerOn,
            onZoneTap = {
                selectedZoneKey = it.key
                selectedPinId = null
            },
            onPinTap = {
                selectedPinId = it
                selectedZoneKey = null
            },
            onMapTap = {
                selectedZoneKey = null
                selectedPinId = null
            },
            onLongPress = { point ->
                selectedZoneKey = null
                selectedPinId = null
                viewModel.chooseDestination(point)
            },
            onRouteTap = viewModel::selectRoute,
            modifier = Modifier.fillMaxSize(),
        )

        MapFilters(
            filter = filter,
            now = now,
            onTime = viewModel::setTimeFilter,
            onKind = viewModel::toggleKind,
            onPins = viewModel::toggleCivicPins,
            onMyLocation = {
                withLocation {
                    scope.launch {
                        val here = runCatching { context.appContainer.locationProvider.currentLocation() }.getOrNull()
                        if (here != null) centerOn = here
                        else Toast.makeText(context, "Couldn't get your location", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.align(Alignment.TopCenter).padding(8.dp),
        )

        // Lift the bottom controls above a showing snackbar so the quick-report buttons stay tappable.
        val snackbarShowing = LocalAppSnackbarHost.current.currentSnackbarData != null
        val bottom = Modifier.align(Alignment.BottomCenter).padding(12.dp)
            .padding(bottom = if (snackbarShowing) SNACKBAR_CLEARANCE else 0.dp)
        when {
            route !is RouteState.Idle -> RouteCard(
                state = route,
                onPlan = { withLocation(viewModel::planRoute) },
                onSelect = viewModel::selectRoute,
                onClear = viewModel::clearRoute,
                modifier = bottom,
            )
            selectedZone != null -> ZoneCard(selectedZone, onClose = { selectedZoneKey = null }, modifier = bottom)
            selectedPin != null -> PinCard(selectedPin, onOpen = { onOpenReport(selectedPin.localId) }, modifier = bottom)
            else -> Column(bottom, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(shape = MaterialTheme.shapes.small, tonalElevation = 2.dp) {
                    Text(
                        if (c.zones.isEmpty()) "No reported zones here yet. Long-press the map to plan a walking route."
                        else "Tap a zone for details · Long-press the map to plan a route with fewer unsafe reports",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
                QuickUnsafePanel(onReport = quickReport, compact = true)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Map view (osmdroid)

/** osmdroid layers, created once and stacked bottom → top; data changes only replace layer contents. */
private class MapLayers(
    val mapView: MapView,
    val heat: HeatZoneOverlay,
    val routes: FolderOverlay,
    val pins: FolderOverlay,
    val marks: FolderOverlay,
)

@Composable
private fun ReportsMap(
    zones: List<HeatZone>,
    pins: List<ReportEntity>,
    route: RouteState,
    centerOn: GeoLocation?,
    onZoneTap: (HeatZone) -> Unit,
    onPinTap: (Long) -> Unit,
    onMapTap: () -> Unit,
    onLongPress: (GeoLocation) -> Unit,
    onRouteTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val lifecycleOwner = LocalLifecycleOwner.current
    // Overlays are created once, so they must call the latest callbacks rather than the first ones.
    val zoneTap by rememberUpdatedState(onZoneTap)
    val pinTap by rememberUpdatedState(onPinTap)
    val mapTap by rememberUpdatedState(onMapTap)
    val longPress by rememberUpdatedState(onLongPress)
    val routeTap by rememberUpdatedState(onRouteTap)
    val layers = remember {
        createLayers(
            context = context,
            density = density,
            onZoneTap = { zoneTap(it) },
            onMapTap = { mapTap() },
            onLongPress = { longPress(it) },
        )
    }
    val mapView = layers.mapView
    var fitted by remember { mutableStateOf(false) }

    // osmdroid needs the host lifecycle forwarded to pause tile loading and release resources.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    LaunchedEffect(zones) {
        layers.heat.zones = zones
        mapView.invalidate()
    }

    LaunchedEffect(pins) {
        layers.pins.items.clear()
        pins.forEach { r ->
            layers.pins.add(
                Marker(mapView).apply {
                    position = GeoPoint(r.latitude!!, r.longitude!!)
                    title = categoryName(r.category)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    setOnMarkerClickListener { _, _ ->
                        pinTap(r.localId)
                        true // consume: we show our own card instead of osmdroid's info window
                    }
                },
            )
        }
        mapView.invalidate()
    }

    // Frame everything once, on first show; afterwards keep the user's pan/zoom.
    LaunchedEffect(zones, pins) {
        if (fitted) return@LaunchedEffect
        val points = zones.map { GeoPoint(it.latitude, it.longitude) } +
            pins.map { GeoPoint(it.latitude!!, it.longitude!!) }
        fitted = true
        mapView.whenLaidOut {
            if (points.isEmpty()) {
                mapView.controller.setZoom(5.0)
                mapView.controller.setCenter(INDIA)
            } else {
                // The 5-argument overload copes with a single point (zero-size box) by using maxZoom.
                mapView.zoomToBoundingBox(BoundingBox.fromGeoPointsSafe(points), false, (64 * density).toInt(), 16.0, null)
            }
        }
    }

    LaunchedEffect(centerOn) {
        val target = centerOn ?: return@LaunchedEffect
        mapView.controller.setZoom(16.0)
        mapView.controller.setCenter(GeoPoint(target.latitude, target.longitude))
    }

    LaunchedEffect(route) {
        drawRoute(layers, route, density, onRouteTap = { routeTap(it) })
        if (route is RouteState.Planned) {
            val points = route.plan.options.flatMap { o -> o.points.map { GeoPoint(it.latitude, it.longitude) } }
            if (points.isNotEmpty()) {
                mapView.whenLaidOut {
                    mapView.zoomToBoundingBox(BoundingBox.fromGeoPointsSafe(points), true, (72 * density).toInt(), 17.0, 600L)
                }
            }
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

private fun MapView.whenLaidOut(action: () -> Unit) {
    if (isLayoutOccurred) action() else addOnFirstLayoutListener { _, _, _, _, _ -> action() }
}

private fun createLayers(
    context: Context,
    density: Float,
    onZoneTap: (HeatZone) -> Unit,
    onMapTap: () -> Unit,
    onLongPress: (GeoLocation) -> Unit,
): MapLayers {
    // Required by the OpenStreetMap tile usage policy: identify the app.
    Configuration.getInstance().apply {
        load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        userAgentValue = context.packageName
    }
    val mapView = MapView(context).apply {
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(true)
        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        minZoomLevel = 3.0
    }
    val layers = MapLayers(
        mapView = mapView,
        heat = HeatZoneOverlay(density, onZoneTap),
        routes = FolderOverlay(),
        pins = FolderOverlay(),
        marks = FolderOverlay(),
    )
    mapView.overlays.apply {
        // Bottom → top. Taps go top → bottom, so the map-events layer only sees taps nothing else took.
        add(
            MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                    onMapTap()
                    return false
                }

                override fun longPressHelper(p: GeoPoint?): Boolean {
                    p ?: return false
                    onLongPress(GeoLocation(p.latitude, p.longitude))
                    return true
                }
            }),
        )
        add(layers.heat)
        add(layers.routes)
        add(layers.pins)
        add(layers.marks)
        add(CopyrightOverlay(context)) // "© OpenStreetMap contributors"
    }
    return layers
}

private fun drawRoute(layers: MapLayers, route: RouteState, density: Float, onRouteTap: (Int) -> Unit) {
    val mapView = layers.mapView
    layers.routes.items.clear()
    layers.marks.items.clear()
    val destination = when (route) {
        is RouteState.Destination -> route.destination
        is RouteState.Planning -> route.destination
        is RouteState.Planned -> route.destination
        is RouteState.Failed -> route.destination
        RouteState.Idle -> null
    }
    if (route is RouteState.Planned) {
        // Unselected options first (thin grey), then the selected one on top with a white casing.
        val order = route.plan.options.indices.sortedBy { if (it == route.selected) 1 else 0 }
        for (i in order) {
            val points = route.plan.options[i].points.map { GeoPoint(it.latitude, it.longitude) }
            val selected = i == route.selected
            if (selected) {
                layers.routes.add(routeLine(mapView, points, ZoneStyle.SAFER_ROUTE_CASING, 10f * density) { onRouteTap(i) })
            }
            val color = if (selected) ZoneStyle.SAFER_ROUTE else ZoneStyle.OTHER_ROUTE
            layers.routes.add(routeLine(mapView, points, color, (if (selected) 6f else 4f) * density) { onRouteTap(i) })
        }
        layers.marks.add(markAt(mapView, route.origin, R.drawable.ic_map_origin, Marker.ANCHOR_CENTER))
    }
    destination?.let { layers.marks.add(markAt(mapView, it, R.drawable.ic_map_destination, Marker.ANCHOR_BOTTOM)) }
    mapView.invalidate()
}

private fun routeLine(mapView: MapView, points: List<GeoPoint>, color: Int, width: Float, onTap: () -> Unit) =
    Polyline(mapView).apply {
        setPoints(points)
        outlinePaint.color = color
        outlinePaint.strokeWidth = width
        outlinePaint.strokeCap = Paint.Cap.ROUND
        outlinePaint.strokeJoin = Paint.Join.ROUND
        setInfoWindow(null)
        setOnClickListener { _, _, _ ->
            onTap()
            true
        }
    }

private fun markAt(mapView: MapView, at: GeoLocation, icon: Int, anchorV: Float) = Marker(mapView).apply {
    position = GeoPoint(at.latitude, at.longitude)
    this.icon = ContextCompat.getDrawable(mapView.context, icon)
    setAnchor(Marker.ANCHOR_CENTER, anchorV)
    setInfoWindow(null)
    setOnMarkerClickListener { _, _ -> true }
}

// ---------------------------------------------------------------------------------------------------------------
// Overlay controls and cards

@Composable
private fun MapFilters(
    filter: MapFilter,
    now: TimeOfDay,
    onTime: (TimeOfDay?) -> Unit,
    onKind: (ZoneKind) -> Unit,
    onPins: () -> Unit,
    onMyLocation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, tonalElevation = 3.dp, shadowElevation = 2.dp) {
        Column(Modifier.padding(vertical = 4.dp)) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                item {
                    IconButton(onClick = onMyLocation) { Icon(Icons.Filled.MyLocation, contentDescription = "My location") }
                }
                item { FilterChip(selected = filter.timeFilter == null, onClick = { onTime(null) }, label = { Text("All times") }) }
                items(TimeOfDay.entries) { t ->
                    FilterChip(
                        selected = filter.timeFilter == t,
                        onClick = { onTime(if (filter.timeFilter == t) null else t) },
                        label = { Text(if (t == now) "${t.displayName} · now" else t.displayName) },
                    )
                }
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                items(ZoneKind.entries) { kind ->
                    FilterChip(
                        selected = kind in filter.kinds,
                        onClick = { onKind(kind) },
                        label = { Text(kind.label) },
                        leadingIcon = { ColorDot(Color(ZoneStyle.fillColor(kind))) },
                    )
                }
                item {
                    FilterChip(selected = filter.showCivicPins, onClick = onPins, label = { Text("Issue pins") })
                }
            }
        }
    }
}

@Composable
private fun ColorDot(color: Color) {
    Box(Modifier.size(12.dp).background(color, CircleShape))
}

@Composable
private fun ZoneCard(zone: HeatZone, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ColorDot(Color(ZoneStyle.fillColor(zone.kind)))
                Text(
                    "  ${zone.kind.label}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close") }
            }
            Text(
                if (zone.kind == ZoneKind.OTHER) "${zone.count} report${if (zone.count == 1) "" else "s"} here"
                else "${zone.count} report${if (zone.count == 1) "" else "s"} of feeling unsafe here",
                fontWeight = FontWeight.SemiBold,
            )
            val times = zone.byTimeOfDay.entries.sortedByDescending { it.value }
            if (times.isNotEmpty()) {
                Text("When: " + times.joinToString(" · ") { "${it.key.displayName} (${it.value})" })
            }
            if (zone.kind == ZoneKind.OTHER) {
                Text(zone.byCategory.entries.sortedByDescending { it.value }.joinToString(" · ") { "${it.key.displayName} (${it.value})" })
            }
            val tags = zone.tagCounts.entries.sortedByDescending { it.value }.take(3)
            if (tags.isNotEmpty()) Text("Reported: " + tags.joinToString(" · ") { "${it.key.displayName} (${it.value})" })
            Text("Latest: ${formatTime(zone.latestAt)}", style = MaterialTheme.typography.bodySmall)
            Text(
                "Community reports, not verified. No zone doesn't mean a place is safe — only that nobody reported it.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun PinCard(report: ReportEntity, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val name = categoryName(report.category)
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                StatusBadge(statusOf(report.status))
            }
            if (report.description.isNotBlank()) {
                Text(report.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            }
            Text("🕒 ${formatTime(report.capturedAt)}", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onOpen) { Text("View report") }
                TextButton(onClick = { openInMaps(context, report.latitude!!, report.longitude!!, name) }) {
                    Text("Directions")
                }
            }
        }
    }
}

@Composable
private fun RouteCard(
    state: RouteState,
    onPlan: () -> Unit,
    onSelect: (Int) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null)
                Text(
                    "  Walking route with fewer unsafe reports",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClear) { Icon(Icons.Filled.Close, contentDescription = "Close route") }
            }
            when (state) {
                is RouteState.Destination -> {
                    Text(
                        "From your location to the flag. Your start and end points are sent to OpenStreetMap's free " +
                            "routing service (FOSSGIS) to calculate it.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(onClick = onPlan, modifier = Modifier.fillMaxWidth()) { Text("Find route") }
                }
                is RouteState.Planning -> {
                    Text("Finding routes and checking them against reported zones…")
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                is RouteState.Failed -> {
                    Text(state.message)
                    OutlinedButton(onClick = onPlan) { Text("Try again") }
                }
                is RouteState.Planned -> {
                    state.plan.options.forEachIndexed { i, option ->
                        RouteOptionRow(option, selected = i == state.selected, onClick = { onSelect(i) })
                    }
                    state.plan.notices.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                    val chosen = state.plan.options[state.selected]
                    Button(
                        onClick = { openUrl(context, GoogleMapsHandoff.walkingNavigationUrl(state.destination, chosen.points)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Navigation, contentDescription = null)
                        Text("  Navigate in Google Maps")
                    }
                    Text(
                        "Google Maps follows a few points of this route but may choose its own path between them. " +
                            "Community reports aren't verified; stay alert.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "${state.plan.attribution} · Fix the map",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { openUrl(context, "https://www.openstreetmap.org/fixthemap") },
                    )
                }
                RouteState.Idle -> Unit
            }
        }
    }
}

@Composable
private fun RouteOptionRow(option: RouteOption, selected: Boolean, onClick: () -> Unit) {
    val label = when (option.role) {
        RouteRole.FEWER_REPORTS -> "Fewer unsafe reports"
        RouteRole.SHORTEST -> "Shortest"
        RouteRole.ALTERNATIVE -> "Alternative"
    }
    val km = option.distanceM / 1000
    val minutes = (option.durationS / 60).roundToInt()
    val zones = when (option.zonesCrossed) {
        0 -> "no reported zones"
        1 -> "passes 1 reported zone"
        else -> "passes ${option.zonesCrossed} reported zones"
    }
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Column {
            Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            Text("%.1f km · %d min · %s".format(km, minutes, zones), style = MaterialTheme.typography.bodySmall)
        }
    }
}
