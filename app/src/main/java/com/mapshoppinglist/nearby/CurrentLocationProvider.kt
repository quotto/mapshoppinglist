package com.mapshoppinglist.nearby

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

fun interface CurrentLocationProvider {
    suspend fun getCurrentLocation(): Location?
}

class DefaultCurrentLocationProvider(context: Context) : CurrentLocationProvider {
    private val appContext = context.applicationContext
    private val fusedClient = LocationServices.getFusedLocationProviderClient(appContext)

    override suspend fun getCurrentLocation(): Location? {
        val hasFineLocation =
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation =
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFineLocation && !hasCoarseLocation) return null

        val cancellation = CancellationTokenSource()
        val currentLocation = fusedClient.getCurrentLocation(
            CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                .setMaxUpdateAgeMillis(2 * 60 * 1000L)
                .build(),
            cancellation.token
        ).await()
        return currentLocation ?: fusedClient.lastLocation.await()
    }
}
