package com.mapshoppinglist.domain.usecase

import com.mapshoppinglist.domain.exception.DuplicatePlaceException
import com.mapshoppinglist.domain.exception.PlaceLimitExceededException
import com.mapshoppinglist.domain.repository.PlacesRepository

/**
 * お店登録時のドメイン制約を検証するユースケース。
 */
class ValidatePlaceRegistrationUseCase(
    private val placesRepository: PlacesRepository,
    private val maxPlaces: Int = 100
) {
    suspend operator fun invoke(latE6: Int, lngE6: Int) {
        val currentCount = placesRepository.getTotalCount()
        if (currentCount >= maxPlaces) {
            throw PlaceLimitExceededException()
        }
        if (placesRepository.existsLocation(latE6, lngE6)) {
            throw DuplicatePlaceException()
        }
    }
}
