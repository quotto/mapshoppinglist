package com.mapshoppinglist.data.repository

import com.mapshoppinglist.data.local.dao.NotifyStateDao
import com.mapshoppinglist.data.local.entity.NotifyStateEntity
import com.mapshoppinglist.domain.model.NotificationState
import com.mapshoppinglist.domain.repository.NotificationStateRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DefaultNotificationStateRepository(
    private val notifyStateDao: NotifyStateDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : NotificationStateRepository {

    override suspend fun get(placeId: Long): NotificationState? = withContext(ioDispatcher) {
        notifyStateDao.findByPlaceId(placeId)?.toDomain()
    }

    override suspend fun upsert(state: NotificationState) = withContext(ioDispatcher) {
        val entity = NotifyStateEntity(
            placeId = state.placeId,
            lastNotifiedAt = state.lastNotifiedAt,
            snoozeUntil = state.snoozeUntil
        )
        notifyStateDao.upsert(entity)
    }

    override suspend fun clear(placeId: Long) = withContext(ioDispatcher) {
        notifyStateDao.deleteByPlaceId(placeId)
    }

    private fun NotifyStateEntity.toDomain(): NotificationState {
        return NotificationState(
            placeId = placeId,
            lastNotifiedAt = lastNotifiedAt,
            snoozeUntil = snoozeUntil
        )
    }
}

