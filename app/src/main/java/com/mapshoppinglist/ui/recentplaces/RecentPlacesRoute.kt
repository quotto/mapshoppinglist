package com.mapshoppinglist.ui.recentplaces

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mapshoppinglist.MapShoppingListApplication
import com.mapshoppinglist.R

@Composable
fun RecentPlacesRoute(
    onPlaceSelected: (Long) -> Unit,
    onClose: () -> Unit,
    viewModel: RecentPlacesViewModel = defaultRecentPlacesViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    RecentPlacesScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onPlaceSelected = onPlaceSelected,
        onClose = onClose
    )
}

@Composable
private fun defaultRecentPlacesViewModel(): RecentPlacesViewModel {
    val application = androidx.compose.ui.platform.LocalContext.current.applicationContext as MapShoppingListApplication
    val factory = remember { RecentPlacesViewModelFactory(application) }
    return viewModel(factory = factory)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecentPlacesScreen(
    uiState: RecentPlacesUiState,
    snackbarHostState: SnackbarHostState,
    onPlaceSelected: (Long) -> Unit,
    onClose: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.recent_places_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Loading(modifier = Modifier.padding(innerPadding))
            }
            uiState.errorMessage != null -> {
                ErrorMessage(
                    modifier = Modifier.padding(innerPadding),
                    message = uiState.errorMessage
                )
            }
            uiState.places.isEmpty() -> {
                EmptyMessage(modifier = Modifier.padding(innerPadding))
            }
            else -> {
                PlaceList(
                    places = uiState.places,
                    contentPadding = innerPadding,
                    onPlaceSelected = onPlaceSelected
                )
            }
        }
    }
}

@Composable
private fun Loading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorMessage(modifier: Modifier = Modifier, message: String) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun EmptyMessage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = stringResource(R.string.recent_places_empty))
    }
}

@Composable
private fun PlaceList(
    places: List<RecentPlaceUiModel>,
    contentPadding: PaddingValues,
    onPlaceSelected: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(places, key = { it.id }) { place ->
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlaceSelected(place.id) }
                    .padding(12.dp),
                text = place.name,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
