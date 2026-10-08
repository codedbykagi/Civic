package com.civic.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.SystemClock
import com.civic.shared.model.GeoLocation
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Where reports get their position from; an interface so quick reporting can be tested without GPS. */
interface LocationSource {
    /** A new GPS fix, or null if none arrives within [timeoutMs]. */
    suspend fun freshLocation(timeoutMs: Long = 8_000): GeoLocation?

    /** The cached position, if it is at most [maxAgeMs] old. Instant: never waits for GPS. */
    suspend fun lastKnown(maxAgeMs: Long): GeoLocation?
}

/** Gets the device's GPS position for tagging a report. Callers must hold location permission. */
class LocationProvider(context: Context) : LocationSource {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    /** Fresh fix if possible (up to [timeoutMs]), else last known position. */
    suspend fun currentLocation(timeoutMs: Long = 8_000): GeoLocation? =
        freshLocation(timeoutMs) ?: lastKnown(maxAgeMs = Long.MAX_VALUE)

    @SuppressLint("MissingPermission")
    override suspend fun freshLocation(timeoutMs: Long): GeoLocation? {
        val cancel = CancellationTokenSource()
        val fresh = withTimeoutOrNull(timeoutMs) {
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancel.token).awaitOrNull()
        }
        if (fresh == null) cancel.cancel() // stop the GPS request we gave up on
        return fresh?.toGeo()
    }

    @SuppressLint("MissingPermission")
    override suspend fun lastKnown(maxAgeMs: Long): GeoLocation? {
        val location = withTimeoutOrNull(2_000) { client.lastLocation.awaitOrNull() } ?: return null
        val ageMs = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000
        return if (ageMs <= maxAgeMs) location.toGeo() else null
    }

    private fun Location.toGeo() = GeoLocation(latitude, longitude)

    private suspend fun Task<Location>.awaitOrNull(): Location? = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resume(null) }
        addOnCanceledListener { cont.resume(null) } // otherwise a cancelled task never resumes
    }

    // TODO: reverse-geocode to a readable address (android.location.Geocoder)
}
