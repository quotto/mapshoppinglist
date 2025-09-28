package com.mapshoppinglist.ui.place

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.mapshoppinglist.MapShoppingListApplication
import com.mapshoppinglist.R
import kotlinx.coroutines.flow.collectLatest

@Composable
fun PlacePickerRoute(
    onPlaceRegistered: (Long) -> Unit,
    onClose: () -> Unit,
    viewModel: PlacePickerViewModel = defaultPlacePickerViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is PlacePickerEvent.PlaceRegistered -> {
                    onPlaceRegistered(event.placeId)
                }
            }
        }
    }

    PlacePickerScreen(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onPredictionSelected = viewModel::onPredictionSelected,
        onConfirm = viewModel::confirmSelection,
        onClearSelection = viewModel::onClearSelection,
        onClose = onClose,
        snackbarHostState = snackbarHostState
    )
}

@Composable
private fun defaultPlacePickerViewModel(): PlacePickerViewModel {
    val context = LocalContext.current
    val application = context.applicationContext as MapShoppingListApplication
    val factory = remember { PlacePickerViewModelFactory(application) }
    return viewModel(factory = factory)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacePickerScreen(
    uiState: PlacePickerUiState,
    onQueryChange: (String) -> Unit,
    onPredictionSelected: (PlacePredictionUiModel) -> Unit,
    onConfirm: () -> Unit,
    onClearSelection: () -> Unit,
    onClose: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DEFAULT_LOCATION, 10f)
    }

    LaunchedEffect(uiState.selectedPlace?.latLng) {
        uiState.selectedPlace?.latLng?.let { latLng ->
            cameraPositionState.animate(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(latLng, 15f)))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.place_picker_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = stringResource(R.string.place_picker_search_hint)) },
                singleLine = true
            )

            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            uiState.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (uiState.predictions.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(uiState.predictions) { prediction ->
                        TextButton(
                            onClick = { onPredictionSelected(prediction) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(text = prediction.primaryText, style = MaterialTheme.typography.titleMedium)
                                prediction.secondaryText?.let {
                                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            val selected = uiState.selectedPlace
            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = false)
            ) {
                selected?.let {
                    Marker(
                        state = MarkerState(position = it.latLng),
                        title = it.name,
                        snippet = it.address
                    )
                }
            }

            if (selected != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = selected.name, style = MaterialTheme.typography.titleMedium)
                    selected.address?.let {
                        Text(text = it, style = MaterialTheme.typography.bodyMedium)
                    }
                    TextButton(onClick = onClearSelection) {
                        Text(text = stringResource(R.string.place_picker_clear_selection))
                    }
                }
            }

            Button(
                onClick = onConfirm,
                enabled = selected != null && !uiState.isCreating,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isCreating) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                } else {
                    Text(text = stringResource(R.string.place_picker_confirm))
                }
            }
        }
    }
}

private val DEFAULT_LOCATION = LatLng(35.681236, 139.767125)
