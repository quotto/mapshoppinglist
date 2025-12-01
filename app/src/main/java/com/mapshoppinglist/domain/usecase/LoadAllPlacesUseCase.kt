package com.mapshoppinglist.domain.usecase

import com.mapshoppinglist.domain.model.Place
import com.mapshoppinglist.domain.repository.PlacesRepository

/**
 * 登録済みのお店一覧を取得するユースケース。
 */
class LoadAllPlacesUseCase(private val placesRepository: PlacesRepository) {
    suspend operator fun invoke(): List<Place> = placesRepository.loadAll()
}
