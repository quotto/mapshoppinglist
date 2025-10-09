package com.mapshoppinglist.ui.itemdetail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.OutlinedButton
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(linkedPlaceId) {
        if (linkedPlaceId != null) {
            viewModel.onPlaceLinked(linkedPlaceId)
            onLinkedPlaceConsumed()
        }
    }

    val handleBack: () -> Unit = remember(viewModel) {
        {
            coroutineScope.launch {
                when (viewModel.saveIfNeeded()) {
                    ItemDetailSaveResult.Success -> onBack()
                    ItemDetailSaveResult.ValidationError, ItemDetailSaveResult.Error -> Unit
                }
            }
        }
    }

    BackHandler(onBack = handleBack)

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ItemDetailEvent.PlaceLinked -> {
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.item_detail_place_linked)
                    )
                }
                ItemDetailEvent.ItemUpdated -> {
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.item_detail_updated_message)
                    )
                }
            }
        }
    }

    ItemDetailScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = handleBack,
        onRemovePlace = viewModel::onRemovePlace,
        onAddPlaceViaSearch = onAddPlaceViaSearch,
        onAddPlaceViaRecent = onAddPlaceViaRecent,
        onEditTitleChange = viewModel::onEditTitleChange,
        onEditNoteChange = viewModel::onEditNoteChange
    )
}

@Composable
private fun defaultItemDetailViewModel(itemId: Long): ItemDetailViewModel {
    val application = LocalContext.current.applicationContext as MapShoppingListApplication
    val factory = remember(itemId) { ItemDetailViewModelFactory(application, itemId) }
    return viewModel(factory = factory)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemDetailScreen(
    uiState: ItemDetailUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onRemovePlace: (Long) -> Unit,
    onAddPlaceViaSearch: () -> Unit,
    onAddPlaceViaRecent: () -> Unit,
    onEditTitleChange: (String) -> Unit,
    onEditNoteChange: (String) -> Unit
) {
    var isAddPlaceDialogVisible by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.item_detail_title),
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
                LoadingContent(modifier = Modifier.padding(innerPadding))
            }
            uiState.isNotFound -> {
                NotFoundContent(modifier = Modifier.padding(innerPadding))
            }
            else -> {
                ItemDetailContent(
                    uiState = uiState,
                    contentPadding = innerPadding,
                    onRemovePlace = onRemovePlace,
                    onAddPlaceClick = { isAddPlaceDialogVisible = true },
                    onEditTitleChange = onEditTitleChange,
                    onEditNoteChange = onEditNoteChange
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
    onRemovePlace: (Long) -> Unit,
    onAddPlaceClick: () -> Unit,
    onEditTitleChange: (String) -> Unit,
    onEditNoteChange: (String) -> Unit
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
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(ItemDetailTestTags.TITLE_INPUT),
                    value = uiState.titleInput,
                    onValueChange = onEditTitleChange,
                    label = { Text(text = stringResource(R.string.shopping_list_add_dialog_title_hint)) },
                    singleLine = true,
                    isError = uiState.showTitleValidationError,
                    enabled = !uiState.isSaving,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(ItemDetailTestTags.NOTE_INPUT),
                    value = uiState.noteInput,
                    onValueChange = onEditNoteChange,
                    label = { Text(text = stringResource(R.string.shopping_list_add_dialog_note_hint)) },
                    minLines = 2,
                    enabled = !uiState.isSaving,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
                if (uiState.showTitleValidationError) {
                    Text(
                        text = stringResource(R.string.shopping_list_validation_title_required),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                uiState.errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (uiState.isSaving) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
        item {
            Button(
                onClick = onAddPlaceClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ItemDetailTestTags.ADD_PLACE_BUTTON),
                enabled = !uiState.isSaving,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            ) {
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
private fun LinkedPlaceRow(
    place: LinkedPlaceUiModel,
    onRemove: () -> Unit
) {
    val rowTag = "${ItemDetailTestTags.LINKED_PLACE_PREFIX}${place.placeId}"
    val removeTag = "${ItemDetailTestTags.LINKED_PLACE_REMOVE_PREFIX}${place.placeId}"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag(rowTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = place.name, style = MaterialTheme.typography.titleMedium)
                place.address?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            FilledIconButton(
                modifier = Modifier.testTag(removeTag),
                onClick = onRemove,
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.item_detail_remove_place)
                )
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
                OutlinedButton (
                    onClick = onSearchSelected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(ItemDetailTestTags.ADD_PLACE_DIALOG_SEARCH),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimary,
                        contentColor = MaterialTheme.colorScheme.primary,
                    )
                ) {
                    Text(text = stringResource(R.string.item_detail_add_place_search))
                }
                OutlinedButton(
                    onClick = onRecentSelected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(ItemDetailTestTags.ADD_PLACE_DIALOG_RECENT),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimary,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
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

/**
 * UIテストで利用するタグをまとめる。
 */
object ItemDetailTestTags {
    const val TITLE_INPUT: String = "item_detail_title_input"
    const val NOTE_INPUT: String = "item_detail_note_input"
    const val ADD_PLACE_BUTTON: String = "item_detail_add_place_button"
    const val LINKED_PLACE_PREFIX: String = "item_detail_linked_place_"
    const val LINKED_PLACE_REMOVE_PREFIX: String = "item_detail_linked_place_remove_"
    const val ADD_PLACE_DIALOG_SEARCH: String = "item_detail_add_place_search"
    const val ADD_PLACE_DIALOG_RECENT: String = "item_detail_add_place_recent"
}
