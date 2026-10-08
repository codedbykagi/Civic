package com.civic.app.safety.zones

/** Second visual cue besides colour (WCAG 1.4.1), so zone kinds stay distinguishable in greyscale too. */
enum class OutlineStyle { SOLID, DASHED, DOTTED }

/**
 * Map colours from Paul Tol's high-contrast scheme: colour-blind safe and still distinct on OSM's pale tiles.
 * Intensity is shown through opacity (and radius), never hue, because hue already encodes the [ZoneKind].
 * Plain ARGB Ints so this stays testable on the JVM.
 */
object ZoneStyle {
    /** Under every outline: lifts the darker outlines off busy tiles such as motorway pink. */
    const val HALO: Int = 0xFFFFFFFF.toInt()
    const val HALO_ALPHA: Int = 180
    const val HALO_WIDTH_DP: Float = 3f
    const val OUTLINE_WIDTH_DP: Float = 1.5f

    /** Near-black with a white casing so a route never reads as a zone; green is avoided because it reads as parks. */
    const val SAFER_ROUTE: Int = 0xFF222222.toInt()
    const val SAFER_ROUTE_CASING: Int = 0xFFFFFFFF.toInt()
    const val OTHER_ROUTE: Int = 0xFF8A8A8A.toInt()

    fun fillColor(kind: ZoneKind): Int = when (kind) {
        ZoneKind.WOMEN -> 0xFFBB5566.toInt()
        ZoneKind.CHILDREN -> 0xFF004488.toInt()
        ZoneKind.OTHER -> 0xFFDDAA33.toInt()
    }

    fun outlineColor(kind: ZoneKind): Int = when (kind) {
        ZoneKind.WOMEN -> 0xFF882255.toInt()
        ZoneKind.CHILDREN -> 0xFF004488.toInt()
        ZoneKind.OTHER -> 0xFF7A5A00.toInt()
    }

    fun outlineStyle(kind: ZoneKind): OutlineStyle = when (kind) {
        ZoneKind.WOMEN -> OutlineStyle.SOLID
        ZoneKind.CHILDREN -> OutlineStyle.DASHED
        ZoneKind.OTHER -> OutlineStyle.DOTTED
    }

    /** On/off lengths in dp for a dash effect, or null for a solid line. A fresh array each call. */
    fun dashIntervalsDp(style: OutlineStyle): FloatArray? = when (style) {
        OutlineStyle.SOLID -> null
        OutlineStyle.DASHED -> floatArrayOf(6f, 4f)
        OutlineStyle.DOTTED -> floatArrayOf(1.5f, 3f)
    }

    /** 0..255 fill alpha: about 63 for a single fresh report, up to 153 for a very hot zone. */
    fun fillAlpha(intensity: Double): Int = (255 * (0.15 + 0.45 * intensity.coerceIn(0.0, 1.0))).toInt()
}
