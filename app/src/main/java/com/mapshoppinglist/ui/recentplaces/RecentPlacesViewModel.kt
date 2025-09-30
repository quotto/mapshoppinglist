package com.mapshoppinglist.ui.recentplaces

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapshoppinglist.domain.model.Place
import com.mapshoppinglist.domain.usecase.GetRecentPlacesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecentPlacesViewModel(
    private val getRecentPlacesUseCase: GetRecentPlacesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecentPlacesUiState())
    val uiState: StateFlow<RecentPlacesUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = RecentPlacesUiState(isLoading = true)
            try {
                val places = getRecentPlacesUseCase().map { it.toUiModel() }
                _uiState.value = RecentPlacesUiState(isLoading = false, places = places)
            } catch (error: Exception) {
                _uiState.value = RecentPlacesUiState(isLoading = false, errorMessage = error.message)
            }
        }
    }

    private fun Place.toUiModel(): RecentPlaceUiModel {
        return RecentPlaceUiModel(
            id = id,
            name = name
        )
    }
}

data class RecentPlacesUiState(
    val isLoading: Boolean = false,
    val places: List<RecentPlaceUiModel> = emptyList(),
    val errorMessage: String? = null
)

data class RecentPlaceUiModel(
    val id: Long,
    val name: String
)
