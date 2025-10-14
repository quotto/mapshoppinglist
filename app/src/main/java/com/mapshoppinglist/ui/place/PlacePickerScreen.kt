package com.mapshoppinglist.ui.place

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.mapshoppinglist.MapShoppingListApplication
import com.mapshoppinglist.R
import com.mapshoppinglist.testtag.PlacePickerTestTags
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

internal val PlacePickerCameraTargetKey = SemanticsPropertyKey<LatLng>("place_picker_camera_target")

@Composable
fun PlacePickerRoute(
    onPlaceRegistered: (Long) -> Unit,
    onClose: () -> Unit,
    viewModel: PlacePickerViewModel = defaultPlacePickerViewModel(),
    locationProvider: CurrentLocationProvider? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val resolvedLocationProvider = remember(context, locationProvider) {
        locationProvider ?: DefaultCurrentLocationProvider(context)
    }
    val coroutineScope = rememberCoroutineScope()
    var hasLocationPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasLocationPermission = granted
        if (!granted) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.permission_location_denied_message))
            }
        }
    }

    LaunchedEffect(hasLocationPermission, resolvedLocationProvider) {
        if (hasLocationPermission) {
            MapsInitializer.initialize(context)
            val location = resolvedLocationProvider.getLastLocation()
            location?.let {
                viewModel.updateCameraLocation(LatLng(it.latitude, it.longitude))
            }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

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
        snackbarHostState = snackbarHostState,
        hasLocationPermission = hasLocationPermission,
        onMapLongClick = viewModel::onMapLongClick,
        onPoiClick = viewModel::onPoiClick,
        onRequestLocationPermission = {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
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
    snackbarHostState: SnackbarHostState,
    hasLocationPermission: Boolean,
    onMapLongClick: (LatLng) -> Unit,
    onPoiClick: (PointOfInterest) -> Unit,
    onRequestLocationPermission: () -> Unit
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DEFAULT_LOCATION, 10f)
    }

    LaunchedEffect(uiState.cameraLocation) {
        if (uiState.selectedPlace == null) {
            cameraPositionState.animate(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(uiState.cameraLocation, 13f)))
        }
    }

    LaunchedEffect(uiState.selectedPlace?.latLng) {
        uiState.selectedPlace?.latLng?.let { latLng ->
            cameraPositionState.animate(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(latLng, 15f)))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.place_picker_title),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
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
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
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
                        Card(
                            onClick = { onPredictionSelected(prediction) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
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

            if (hasLocationPermission) {
                val mapProperties = MapProperties(isMyLocationEnabled = true)
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag(PlacePickerTestTags.MAP)
                        .semantics {
                            // UIテストからカメラ位置を検証できるようにする
                            this[PlacePickerCameraTargetKey] = cameraPositionState.position.target
                        },
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(zoomControlsEnabled = false),
                    properties = mapProperties,
                    onMapLongClick = onMapLongClick,
                    onPOIClick = onPoiClick
                ) {
                    selected?.let {
                        Marker(
                            state = MarkerState(position = it.latLng),
                            title = it.name,
                            snippet = it.address
                        )
                    }
                }
            } else {
                LocationPermissionPlaceholder(
                    onRequestPermission = onRequestLocationPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag(PlacePickerTestTags.LOCATION_PERMISSION_PLACEHOLDER)
                )
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
                    TextButton(
                        onClick = onClearSelection,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text(text = stringResource(R.string.place_picker_clear_selection))
                    }
                }
            }

            Button(
                onClick = onConfirm,
                enabled = selected != null && !uiState.isCreating,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
@Composable
private fun LocationPermissionPlaceholder(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.permission_location_denied_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(text = stringResource(R.string.permission_location_request_button))
            }
        }
    }
}

/**
 * 現在地を取得するためのシンプルな抽象化。
 */
fun interface CurrentLocationProvider {
    suspend fun getLastLocation(): Location?
}

private class DefaultCurrentLocationProvider(context: Context) : CurrentLocationProvider {
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    override suspend fun getLastLocation(): Location? {
        return fusedClient.lastLocation.await()
    }
}
