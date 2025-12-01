package com.mapshoppinglist.domain.usecase

import com.mapshoppinglist.domain.model.Place
import com.mapshoppinglist.domain.repository.PlacesRepository

class GetRecentPlacesUseCase(private val placesRepository: PlacesRepository) {

    suspend operator fun invoke(limit: Int = DEFAULT_LIMIT): List<Place> {
        require(limit > 0) { "limit must be positive" }
        return placesRepository.loadRecentPlaces(limit)
    }

    companion object {
        private const val DEFAULT_LIMIT = 20
    }
}
