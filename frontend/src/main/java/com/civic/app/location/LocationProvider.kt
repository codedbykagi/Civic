package com.civic.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.civic.shared.model.GeoLocation
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Gets the device's current GPS position for tagging a report. */
class LocationProvider(context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    /** Fresh fix if possible (up to [timeoutMs]), else last known position. Caller must hold location permission. */
    @SuppressLint("MissingPermission")
    suspend fun currentLocation(timeoutMs: Long = 8_000): GeoLocation? {
        val fresh = withTimeoutOrNull(timeoutMs) {
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).awaitOrNull()
        }
        val location = fresh ?: client.lastLocation.awaitOrNull()
        return location?.let { GeoLocation(it.latitude, it.longitude) }
    }

    private suspend fun Task<Location>.awaitOrNull(): Location? = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resume(null) }
    }

    // TODO: reverse-geocode to a readable address (android.location.Geocoder)
}
