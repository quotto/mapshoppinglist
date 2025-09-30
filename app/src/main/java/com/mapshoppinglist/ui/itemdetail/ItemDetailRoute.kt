package com.mapshoppinglist.ui.itemdetail

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mapshoppinglist.MapShoppingListApplication
import com.mapshoppinglist.R
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ItemDetailRoute(
    itemId: Long,
    linkedPlaceId: Long?,
    onLinkedPlaceConsumed: () -> Unit,
    onAddPlaceViaSearch: () -> Unit,
    onAddPlaceViaRecent: () -> Unit,
    onBack: () -> Unit,
    viewModel: ItemDetailViewModel = defaultItemDetailViewModel(itemId)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(linkedPlaceId) {
        if (linkedPlaceId != null) {
            viewModel.onPlaceLinked(linkedPlaceId)
            onLinkedPlaceConsumed()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ItemDetailEvent.PlaceLinked -> {
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.item_detail_place_linked)
                    )
                }
            }
        }
    }

    ItemDetailScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onTogglePurchased = viewModel::onTogglePurchased,
        onRemovePlace = viewModel::onRemovePlace,
        onAddPlaceViaSearch = onAddPlaceViaSearch,
        onAddPlaceViaRecent = onAddPlaceViaRecent
    )
}

@Composable
private fun defaultItemDetailViewModel(itemId: Long): ItemDetailViewModel {
    val application = androidx.compose.ui.platform.LocalContext.current.applicationContext as MapShoppingListApplication
    val factory = remember(itemId) { ItemDetailViewModelFactory(application, itemId) }
    return viewModel(factory = factory)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemDetailScreen(
    uiState: ItemDetailUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onTogglePurchased: (Boolean) -> Unit,
    onRemovePlace: (Long) -> Unit,
    onAddPlaceViaSearch: () -> Unit,
    onAddPlaceViaRecent: () -> Unit
) {
    var isAddPlaceDialogVisible by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.item_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                LoadingContent(modifier = Modifier.padding(innerPadding))
            }
            uiState.isNotFound -> {
                NotFoundContent(modifier = Modifier.padding(innerPadding))
            }
            else -> {
                ItemDetailContent(
                    uiState = uiState,
                    contentPadding = innerPadding,
                    onTogglePurchased = onTogglePurchased,
                    onRemovePlace = onRemovePlace,
                    onAddPlaceClick = { isAddPlaceDialogVisible = true }
                )
            }
        }
    }

    if (isAddPlaceDialogVisible) {
        AddPlaceOptionDialog(
            onDismiss = { isAddPlaceDialogVisible = false },
            onSearchSelected = {
                isAddPlaceDialogVisible = false
                onAddPlaceViaSearch()
            },
            onRecentSelected = {
                isAddPlaceDialogVisible = false
                onAddPlaceViaRecent()
            }
        )
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
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
private fun NotFoundContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = stringResource(R.string.item_detail_not_found))
    }
}

@Composable
private fun ItemDetailContent(
    uiState: ItemDetailUiState,
    contentPadding: PaddingValues,
    onTogglePurchased: (Boolean) -> Unit,
    onRemovePlace: (Long) -> Unit,
    onAddPlaceClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = uiState.title, style = MaterialTheme.typography.headlineSmall)
                uiState.note?.takeIf { it.isNotBlank() }?.let {
                    Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                RowWithToggle(
                    isChecked = uiState.isPurchased,
                    onToggle = onTogglePurchased
                )
            }
        }
        item {
            OutlinedButton(onClick = onAddPlaceClick, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.item_detail_add_place))
            }
        }
        if (uiState.linkedPlaces.isEmpty()) {
            item {
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = stringResource(R.string.item_detail_no_places),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(uiState.linkedPlaces, key = { it.placeId }) { place ->
                LinkedPlaceRow(place = place, onRemove = { onRemovePlace(place.placeId) })
            }
        }
    }
}

@Composable
private fun RowWithToggle(
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = stringResource(R.string.item_detail_purchased_toggle))
        Switch(
            checked = isChecked,
            onCheckedChange = onToggle
        )
    }
}

@Composable
private fun LinkedPlaceRow(
    place: LinkedPlaceUiModel,
    onRemove: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = place.name, style = MaterialTheme.typography.titleMedium)
                place.address?.takeIf { it.isNotBlank() }?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onRemove) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = stringResource(R.string.item_detail_remove_place))
            }
        }
    }
}

@Composable
private fun AddPlaceOptionDialog(
    onDismiss: () -> Unit,
    onSearchSelected: () -> Unit,
    onRecentSelected: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.item_detail_add_place_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSearchSelected, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.item_detail_add_place_search))
                }
                Button(onClick = onRecentSelected, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.item_detail_add_place_recent))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.common_close))
            }
        }
    )
}
