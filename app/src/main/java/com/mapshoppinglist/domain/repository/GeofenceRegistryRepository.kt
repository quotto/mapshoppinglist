package com.mapshoppinglist.domain.repository

import com.mapshoppinglist.domain.model.GeofenceRegistration

interface GeofenceRegistryRepository {
    suspend fun loadAll(): List<GeofenceRegistration>
    suspend fun replaceAll(registrations: List<GeofenceRegistration>)
    suspend fun deleteByPlaceIds(placeIds: List<Long>)
}
