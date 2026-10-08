package com.civic.app.ui.screens.map

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.civic.app.data.local.ReportEntity
import com.civic.app.ui.categoryName
import com.civic.app.ui.components.PlaceholderScreen
import com.civic.app.ui.components.StatusBadge
import com.civic.app.ui.formatTime
import com.civic.app.ui.openInMaps
import com.civic.app.ui.screens.feed.FeedViewModel
import com.civic.app.ui.statusOf
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

/** In-app map (OpenStreetMap) with a pin per located report. Tap a pin for a summary card. */
@Composable
fun MapScreen(
    onOpenReport: (Long) -> Unit,
    viewModel: FeedViewModel = viewModel(factory = FeedViewModel.Factory),
) {
    val reports by viewModel.reports.collectAsState()
    val all = reports

    if (all == null) {
        PlaceholderScreen("Loading…", "") { CircularProgressIndicator() }
        return
    }
    val located = all.filter { it.latitude != null && it.longitude != null }
    if (located.isEmpty()) {
        PlaceholderScreen("Nearby issues", "Reports with a GPS location will appear here as pins.")
        return
    }

    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selected = located.firstOrNull { it.localId == selectedId }

    Box(Modifier.fillMaxSize()) {
        ReportsMap(
            reports = located,
            onPinClick = { selectedId = it },
            onMapClick = { selectedId = null },
            modifier = Modifier.fillMaxSize(),
        )
        if (selected != null) {
            SelectedReportCard(
                report = selected,
                onOpen = { onOpenReport(selected.localId) },
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            )
        }
    }
}

@Composable
private fun ReportsMap(
    reports: List<ReportEntity>,
    onPinClick: (Long) -> Unit,
    onMapClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { createMapView(context, onMapClick) }
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

    // Rebuild pins whenever the reports change (new post, deletion, status change).
    LaunchedEffect(reports) {
        mapView.overlays.removeAll { it is Marker }
        reports.forEach { r ->
            mapView.overlays.add(
                Marker(mapView).apply {
                    position = GeoPoint(r.latitude!!, r.longitude!!)
                    title = categoryName(r.category)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    setOnMarkerClickListener { _, _ ->
                        onPinClick(r.localId)
                        true // consume: we show our own card instead of osmdroid's info window
                    }
                },
            )
        }
        mapView.invalidate()

        // Frame all pins once, on first show; afterwards keep the user's pan/zoom.
        if (!fitted) {
            fitted = true
            val box = BoundingBox.fromGeoPointsSafe(reports.map { GeoPoint(it.latitude!!, it.longitude!!) })
            val fit = {
                // A zero-size box (one pin, or pins at the same spot) can't be "fitted": just center on it.
                // (Not longitudeSpanWithDateLine: it reports 360° when east == west.)
                if (box.latitudeSpan < 0.002 && box.lonEast - box.lonWest < 0.002) {
                    mapView.controller.setZoom(16.0)
                    mapView.controller.setCenter(GeoPoint(box.centerLatitude, box.centerLongitude))
                } else {
                    mapView.zoomToBoundingBox(box.increaseByScale(1.3f), false)
                }
            }
            if (mapView.isLayoutOccurred) fit() else mapView.addOnFirstLayoutListener { _, _, _, _, _ -> fit() }
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

private fun createMapView(context: Context, onMapClick: () -> Unit): MapView {
    // Required by the OpenStreetMap tile usage policy: identify the app.
    Configuration.getInstance().apply {
        load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        userAgentValue = context.packageName
    }
    return MapView(context).apply {
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(true)
        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        minZoomLevel = 3.0
        controller.setZoom(3.0)
        overlays.add(CopyrightOverlay(context)) // "© OpenStreetMap contributors"
        overlays.add(
            0,
            MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                    onMapClick()
                    return false
                }

                override fun longPressHelper(p: GeoPoint?): Boolean = false
            }),
        )
    }
}

@Composable
private fun SelectedReportCard(report: ReportEntity, onOpen: () -> Unit, modifier: Modifier = Modifier) {
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
                    Icon(Icons.Filled.Map, contentDescription = null)
                    Text("  Directions")
                }
            }
        }
    }
}
