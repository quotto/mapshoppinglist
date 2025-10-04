package com.mapshoppinglist.domain.usecase

import com.mapshoppinglist.domain.repository.GeofenceRegistryRepository

/**
 * 既に登録済みのジオフェンス情報を取得するユースケース。
 */
class LoadRegisteredGeofencesUseCase(
    private val geofenceRegistryRepository: GeofenceRegistryRepository
) {
    suspend operator fun invoke(): List<Long> {
        return geofenceRegistryRepository.loadAll().map { it.placeId }
    }
}
