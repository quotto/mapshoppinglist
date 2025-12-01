package com.mapshoppinglist.domain.model

/**
 * ジオフェンス登録に必要な情報。
 */
data class GeofenceSpec(val placeId: Long, val requestId: String, val latitude: Double, val longitude: Double, val radiusMeters: Float)
