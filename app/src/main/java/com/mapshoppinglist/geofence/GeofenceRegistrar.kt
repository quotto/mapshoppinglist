package com.mapshoppinglist.geofence

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.mapshoppinglist.domain.model.GeofenceSpec
import com.mapshoppinglist.domain.usecase.GeofenceSyncPlan
import kotlinx.coroutines.tasks.await

/**
 * GeofencingClient を利用して差分計画を適用するクラス。
 */
class GeofenceRegistrar(
    context: Context,
    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context),
    private val pendingIntentProvider: GeofencePendingIntentProvider = GeofencePendingIntentProvider(context)
) {

    @SuppressLint("MissingPermission")
    suspend fun applyPlan(plan: GeofenceSyncPlan) {
        if (plan.toRemoveRequestIds.isNotEmpty()) {
            Log.d("GeofenceRegistrar", "Removing geofences: ${plan.toRemoveRequestIds}")
            geofencingClient.removeGeofences(plan.toRemoveRequestIds).await()
        }
        if (plan.toAdd.isNotEmpty()) {
            val request = buildRequest(plan.toAdd)
            Log.d("GeofenceRegistrar", "Adding geofences: ${plan.toAdd.map { it.requestId }}")
            geofencingClient.addGeofences(request, pendingIntentProvider.get()).await()
        }
    }

    private fun buildRequest(specs: List<GeofenceSpec>): GeofencingRequest {
        val builder = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)

        specs.forEach { builder.addGeofence(toGeofence(it)) }
        return builder.build()
    }

    private fun toGeofence(spec: GeofenceSpec): Geofence = Geofence.Builder()
        .setRequestId(spec.requestId)
        .setCircularRegion(spec.latitude, spec.longitude, spec.radiusMeters)
        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
        .setExpirationDuration(Geofence.NEVER_EXPIRE)
        .build()
}
