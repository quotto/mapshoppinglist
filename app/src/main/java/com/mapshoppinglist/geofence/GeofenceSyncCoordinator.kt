package com.mapshoppinglist.geofence

import com.mapshoppinglist.domain.repository.GeofenceRegistryRepository
import com.mapshoppinglist.domain.usecase.BuildGeofenceSyncPlanUseCase

class GeofenceSyncCoordinator(
    private val buildGeofenceSyncPlanUseCase: BuildGeofenceSyncPlanUseCase,
    private val geofenceRegistrar: GeofenceRegistrar,
    private val geofenceRegistryRepository: GeofenceRegistryRepository
) {

    suspend fun sync(forceRebuild: Boolean = false) {
        val plan = buildGeofenceSyncPlanUseCase(forceRebuild)
        geofenceRegistrar.applyPlan(plan)
        geofenceRegistryRepository.replaceAll(plan.targetRegistrations)
    }
}
