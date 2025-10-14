package com.mapshoppinglist.ui.placemanage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mapshoppinglist.MapShoppingListApplication
import com.mapshoppinglist.R
import com.mapshoppinglist.testtag.PlaceManagementTestTags
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceManagementRoute(
    onBack: () -> Unit,
    viewModel: PlaceManagementViewModel = defaultPlaceManagementViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is PlaceManagementEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(context.getString(event.messageResId))
                }
            }
        }
    }

    PlaceManagementScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onEdit = viewModel::onEditRequested,
        onDelete = viewModel::onDeleteRequested,
        onDialogDismiss = viewModel::onDialogDismiss,
        onEditNameChange = viewModel::onEditNameChange,
        onEditConfirm = viewModel::onEditConfirm,
        onDeleteConfirm = viewModel::onDeleteConfirm
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun defaultPlaceManagementViewModel(): PlaceManagementViewModel {
    val application = LocalContext.current.applicationContext as MapShoppingListApplication
    val factory = remember { PlaceManagementViewModelFactory(application) }
    return viewModel(factory = factory)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceManagementScreen(
    uiState: PlaceManagementUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onDialogDismiss: () -> Unit,
    onEditNameChange: (String) -> Unit,
    onEditConfirm: () -> Unit,
    onDeleteConfirm: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.place_management_title),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
        when {
            uiState.isLoading -> {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.errorMessage != null -> {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = uiState.errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            uiState.places.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.place_management_list_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                PlaceList(
                    places = uiState.places,
                    contentPadding = innerPadding,
                    onEdit = onEdit,
                    onDelete = onDelete
                )
            }
        }

        when (val dialog = uiState.dialogState) {
            is PlaceDialogState.Edit -> {
                EditPlaceDialog(
                    state = dialog,
                    onDismiss = onDialogDismiss,
                    onNameChange = onEditNameChange,
                    onConfirm = onEditConfirm
                )
            }
            is PlaceDialogState.DeleteConfirm -> {
                DeletePlaceDialog(
                    state = dialog,
                    onDismiss = onDialogDismiss,
                    onConfirm = onDeleteConfirm
                )
            }
            null -> Unit
        }
    }
}

@Composable
private fun PlaceList(
    places: List<ManagedPlaceUiModel>,
    contentPadding: PaddingValues,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .padding(contentPadding)
            .fillMaxSize()
            .testTag(PlaceManagementTestTags.PLACE_LIST),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(places, key = { it.id }) { place ->
            ManagedPlaceRow(
                place = place,
                onEdit = { onEdit(place.id) },
                onDelete = { onDelete(place.id) }
            )
        }
    }
}

@Composable
private fun ManagedPlaceRow(
    place: ManagedPlaceUiModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val rowTag = "${PlaceManagementTestTags.PLACE_ROW_PREFIX}${place.id}"
    val deleteTag = "${PlaceManagementTestTags.PLACE_DELETE_PREFIX}${place.id}"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(rowTag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                    Text(
                        text = stringResource(
                            R.string.place_management_coordinate,
                            place.latitude,
                            place.longitude
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = if (place.isSubscribed) {
                                    stringResource(R.string.place_management_status_active)
                                } else {
                                    stringResource(R.string.place_management_status_inactive)
                                }
                            )
                        },
                        enabled = false,
                        colors = AssistChipDefaults.assistChipColors(
                            disabledContainerColor = if (place.isSubscribed) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            disabledLabelColor = if (place.isSubscribed) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.place_management_edit)
                        )
                    }
                    IconButton(
                        modifier = Modifier.testTag(deleteTag),
                        onClick = onDelete
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.place_management_delete)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditPlaceDialog(
    state: PlaceDialogState.Edit,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.place_management_edit_dialog_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = state.nameInput,
                    onValueChange = onNameChange,
                    singleLine = true,
                    enabled = !state.isSaving,
                    label = { Text(stringResource(R.string.place_management_edit_dialog_name_hint)) }
                )
                if (state.errorMessage != null) {
                    Text(
                        text = stringResource(state.errorMessage),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !state.isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp))
                } else {
                    Text(text = stringResource(R.string.place_management_edit_dialog_save))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isSaving) {
                Text(text = stringResource(R.string.place_management_delete_dialog_cancel))
            }
        }
    )
}

@Composable
private fun DeletePlaceDialog(
    state: PlaceDialogState.DeleteConfirm,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.place_management_delete_dialog_title)) },
        text = {
            Text(text = stringResource(R.string.place_management_delete_dialog_message))
        },
        confirmButton = {
            Button(
                modifier = Modifier.testTag(PlaceManagementTestTags.DELETE_DIALOG_CONFIRM),
                onClick = onConfirm,
                enabled = !state.isProcessing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                if (state.isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp))
                } else {
                    Text(text = stringResource(R.string.place_management_delete_dialog_confirm))
                }
            }
        },
        dismissButton = {
            TextButton(
                modifier = Modifier.testTag(PlaceManagementTestTags.DELETE_DIALOG_CANCEL),
                onClick = onDismiss,
                enabled = !state.isProcessing
            ) {
                Text(text = stringResource(R.string.place_management_delete_dialog_cancel))
            }
        }
    )
}
