package com.civic.app.safety.zones

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Typeface
import android.view.MotionEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay
import kotlin.math.max

/**
 * Draws every [HeatZone] as a circle in one overlay. Stock osmdroid Polygons would cost 60 projected vertices
 * each per frame and swallow taps even when hidden or off-screen; this costs one projection per zone and only
 * consumes taps that land inside a drawn circle.
 *
 * Set [zones] in draw order (as [HeatZones.build] returns them) and call `mapView.invalidate()` afterwards.
 */
class HeatZoneOverlay(
    private val density: Float,
    private val onZoneTap: (HeatZone) -> Unit,
) : Overlay() {
    var zones: List<HeatZone> = emptyList()

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = ZoneStyle.HALO_WIDTH_DP * density
        color = ZoneStyle.HALO
        alpha = ZoneStyle.HALO_ALPHA
    }
    /** Indexed by [ZoneKind.ordinal]. */
    private val outlinePaints: Array<Paint> = Array(ZoneKind.entries.size) { index ->
        val kind = ZoneKind.entries[index]
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = ZoneStyle.OUTLINE_WIDTH_DP * density
            color = ZoneStyle.outlineColor(kind)
            ZoneStyle.dashIntervalsDp(ZoneStyle.outlineStyle(kind))?.let { intervals ->
                for (i in intervals.indices) intervals[i] *= density
                pathEffect = DashPathEffect(intervals, 0f)
            }
        }
    }
    private val labelFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = LABEL_FILL
        textSize = LABEL_TEXT_DP * density
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val labelOutline = Paint(labelFill).apply {
        style = Paint.Style.STROKE
        color = LABEL_OUTLINE
        strokeWidth = 3f * density
        strokeJoin = Paint.Join.ROUND
    }

    /** Moves the text baseline so the digits are vertically centred on the zone. */
    private val labelBaselineOffset = labelFill.fontMetrics.let { -(it.ascent + it.descent) / 2f }
    private val minRadiusPx = MIN_RADIUS_DP * density
    private val minLabelRadiusPx = MIN_LABEL_RADIUS_DP * density
    private val geoPoint = GeoPoint(0.0, 0.0)
    private val pixel = Point()

    override fun draw(canvas: Canvas, projection: Projection) {
        val screen = projection.screenRect // rotation-aware bounds in the same space as toPixels()
        val orientation = projection.orientation
        val current = zones
        for (i in 0 until current.size) { // index loop: no iterator allocation per frame
            val zone = current[i]
            val radius = project(zone, projection)
            val x = pixel.x.toFloat()
            val y = pixel.y.toFloat()
            if (x + radius < screen.left || x - radius > screen.right) continue
            if (y + radius < screen.top || y - radius > screen.bottom) continue

            fillPaint.color = ZoneStyle.fillColor(zone.kind)
            fillPaint.alpha = ZoneStyle.fillAlpha(zone.intensity)
            canvas.drawCircle(x, y, radius, fillPaint)
            canvas.drawCircle(x, y, radius, haloPaint)
            canvas.drawCircle(x, y, radius, outlinePaints[zone.kind.ordinal])

            if (zone.count >= 2 && radius >= minLabelRadiusPx) {
                val label = labelFor(zone.count)
                val rotated = orientation != 0f
                if (rotated) {
                    canvas.save()
                    canvas.rotate(-orientation, x, y) // keep the number upright on a rotated map
                }
                canvas.drawText(label, x, y + labelBaselineOffset, labelOutline)
                canvas.drawText(label, x, y + labelBaselineOffset, labelFill)
                if (rotated) canvas.restore()
            }
        }
    }

    override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
        // osmdroid's overlay manager dispatches taps to disabled overlays too.
        if (!isEnabled) return false
        val projection = mapView.projection
        // Last drawn = top-most, so it wins where circles overlap.
        val hit = zones.lastOrNull { zone ->
            val radius = project(zone, projection)
            val dx = e.x - pixel.x
            val dy = e.y - pixel.y
            dx * dx + dy * dy <= radius * radius
        } ?: return false
        onZoneTap(hit)
        return true
    }

    /** Projects the zone centre into [pixel] and returns the on-screen radius, never smaller than 6 dp. */
    private fun project(zone: HeatZone, projection: Projection): Float {
        geoPoint.setCoords(zone.latitude, zone.longitude)
        projection.toPixels(geoPoint, pixel)
        val metres = projection.metersToPixels(zone.radiusM.toFloat(), zone.latitude, projection.zoomLevel)
        return max(metres, minRadiusPx)
    }

    private companion object {
        const val MIN_RADIUS_DP = 6f
        const val MIN_LABEL_RADIUS_DP = 14f
        const val LABEL_TEXT_DP = 12f
        const val LABEL_FILL = 0xFFFFFFFF.toInt()
        const val LABEL_OUTLINE = 0xFF222222.toInt()

        /** Pre-built so drawing allocates no strings. */
        val LABELS = Array(100) { it.toString() }

        fun labelFor(count: Int): String = if (count < LABELS.size) LABELS[count] else "99+"
    }
}
