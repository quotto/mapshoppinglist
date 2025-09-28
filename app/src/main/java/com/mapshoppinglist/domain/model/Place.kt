package com.mapshoppinglist.domain.model

/**
 * ドメイン層で扱うお店。
 */
data class Place(
    val id: Long,
    val name: String,
    val latitudeE6: Int,
    val longitudeE6: Int,
    val isActive: Boolean
)
