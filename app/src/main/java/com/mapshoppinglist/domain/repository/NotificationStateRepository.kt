package com.mapshoppinglist.domain.repository

import com.mapshoppinglist.domain.model.NotificationState

interface NotificationStateRepository {
    suspend fun get(placeId: Long): NotificationState?
    suspend fun upsert(state: NotificationState)
    suspend fun clear(placeId: Long)
}
