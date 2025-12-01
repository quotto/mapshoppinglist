package com.mapshoppinglist.domain.model

/**
 * 現在登録済みと認識しているジオフェンスの情報。
 */
data class GeofenceRegistration(val placeId: Long, val requestId: String)
