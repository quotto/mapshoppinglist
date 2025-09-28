package com.mapshoppinglist.data.repository

import com.mapshoppinglist.data.local.dao.GeofenceRegistryDao
import com.mapshoppinglist.data.local.entity.GeofenceRegistrationEntity
import com.mapshoppinglist.domain.model.GeofenceRegistration
import com.mapshoppinglist.domain.repository.GeofenceRegistryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DefaultGeofenceRegistryRepository(
    private val geofenceRegistryDao: GeofenceRegistryDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : GeofenceRegistryRepository {

    override suspend fun loadAll(): List<GeofenceRegistration> = withContext(ioDispatcher) {
        geofenceRegistryDao.loadAll().map { entity ->
            GeofenceRegistration(
                placeId = entity.placeId,
                requestId = entity.geofenceRequestId
            )
        }
    }

    override suspend fun replaceAll(registrations: List<GeofenceRegistration>) = withContext(ioDispatcher) {
        val entities = registrations.map { registration ->
            GeofenceRegistrationEntity(
                placeId = registration.placeId,
                geofenceRequestId = registration.requestId
            )
        }
        geofenceRegistryDao.replaceAll(entities)
    }

    override suspend fun deleteByPlaceIds(placeIds: List<Long>) = withContext(ioDispatcher) {
        geofenceRegistryDao.deleteByPlaceIds(placeIds)
    }
}
