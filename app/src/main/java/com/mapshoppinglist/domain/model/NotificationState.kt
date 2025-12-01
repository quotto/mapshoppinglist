package com.mapshoppinglist.domain.model

data class NotificationState(val placeId: Long, val lastNotifiedAt: Long?, val snoozeUntil: Long?)
