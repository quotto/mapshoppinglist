package com.mapshoppinglist.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mapshoppinglist.MapShoppingListApplication
import com.mapshoppinglist.R
import com.mapshoppinglist.ui.theme.MapShoppingListTheme
import kotlinx.coroutines.launch
import androidx.core.content.ContextCompat

@Composable
fun ShoppingListRoute(
    onAddPlaceRequest: () -> Unit = {},
    onItemClick: (Long) -> Unit = {},
    newPlaceId: Long? = null,
    onNewPlaceConsumed: () -> Unit = {}
) {
    val application = LocalContext.current.applicationContext as MapShoppingListApplication
    val factory = remember(application) { ShoppingListViewModelFactory(application) }
    val viewModel: ShoppingListViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showBackgroundPrompt by remember {
        mutableStateOf(shouldRequestBackgroundLocation(context))
    }
    var showNotificationPrompt by remember {
        mutableStateOf(shouldRequestNotificationPermission(context))
    }

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        showBackgroundPrompt = !granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        showNotificationPrompt = !granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    val permissionPrompts = remember(showBackgroundPrompt, showNotificationPrompt, context) {
        buildList {
            if (showBackgroundPrompt) {
                add(
                    PermissionPromptUiModel(
                        key = "background",
                        title = context.getString(R.string.permission_background_title),
                        message = context.getString(R.string.permission_background_message),
                        actionLabel = context.getString(R.string.permission_background_button),
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            }
                        }
                    )
                )
            }
            if (showNotificationPrompt) {
                add(
                    PermissionPromptUiModel(
                        key = "notifications",
                        title = context.getString(R.string.permission_notifications_title),
                        message = context.getString(R.string.permission_notifications_message),
                        actionLabel = context.getString(R.string.permission_notifications_button),
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )
                )
            }
        }
    }

    LaunchedEffect(newPlaceId) {
        if (newPlaceId != null) {
            viewModel.onPlaceRegistered(newPlaceId)
            onNewPlaceConsumed()
        }
    }

    ShoppingListScreen(
        uiState = uiState,
        permissionPrompts = permissionPrompts,
        onAddItemClick = viewModel::onAddFabClick,
        onTogglePurchased = viewModel::onTogglePurchased,
        onDeleteItem = viewModel::onDeleteItem,
        onAddDialogDismiss = viewModel::onAddDialogDismiss,
        onAddDialogConfirm = viewModel::onAddConfirm,
        onTitleInputChange = viewModel::onTitleInputChange,
        onNoteInputChange = viewModel::onNoteInputChange,
        onAddPlaceRequest = {
            onAddPlaceRequest()
        },
        onRemovePendingPlace = viewModel::onRemovePendingPlace,
        onItemClick = onItemClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    uiState: ShoppingListUiState,
    permissionPrompts: List<PermissionPromptUiModel>,
    onAddItemClick: () -> Unit,
    onTogglePurchased: (itemId: Long, newState: Boolean) -> Unit,
    onDeleteItem: (itemId: Long) -> Unit,
    onAddDialogDismiss: () -> Unit,
    onAddDialogConfirm: () -> Unit,
    onTitleInputChange: (String) -> Unit,
    onNoteInputChange: (String) -> Unit,
    onAddPlaceRequest: () -> Unit,
    onRemovePendingPlace: (Long) -> Unit,
    onItemClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddItemClick) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(id = R.string.shopping_list_add_item)
                )
            }
        }
    ) { innerPadding ->
        ShoppingListContent(
            uiState = uiState,
            permissionPrompts = permissionPrompts,
            contentPadding = innerPadding,
            onTogglePurchased = onTogglePurchased,
            onDeleteItem = onDeleteItem,
            onItemClick = onItemClick
        )
    }

    if (uiState.isAddDialogVisible) {
        AddItemDialog(
            title = uiState.inputTitle,
            note = uiState.inputNote,
            showTitleValidationError = uiState.showTitleValidationError,
            errorMessage = uiState.addDialogErrorMessage,
            onTitleInputChange = onTitleInputChange,
            onNoteInputChange = onNoteInputChange,
            onDismiss = onAddDialogDismiss,
            onConfirm = onAddDialogConfirm,
            onAddPlaceRequest = onAddPlaceRequest,
            pendingPlaces = uiState.pendingPlaces,
            onRemovePendingPlace = onRemovePendingPlace
        )
    }
}

@Composable
private fun ShoppingListContent(
    uiState: ShoppingListUiState,
    permissionPrompts: List<PermissionPromptUiModel>,
    contentPadding: PaddingValues,
    onTogglePurchased: (Long, Boolean) -> Unit,
    onDeleteItem: (Long) -> Unit,
    onItemClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 80.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = permissionPrompts,
            key = { prompt: PermissionPromptUiModel -> prompt.key },
            itemContent = { prompt: PermissionPromptUiModel ->
                PermissionPromptCard(prompt)
            }
        )
        if (uiState.notPurchased.isEmpty()) {
            item(key = "empty") {
                EmptySection()
            }
        } else {
            items(
                items = uiState.notPurchased,
                key = { it.id }
            ) { item ->
                ShoppingListRow(
                    model = item,
                    onTogglePurchased = { onTogglePurchased(item.id, it) },
                    onDeleteItem = { onDeleteItem(item.id) },
                    onClick = { onItemClick(item.id) }
                )
            }
        }

        if (uiState.purchased.isNotEmpty()) {
            item(key = "purchased_header") {
                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = stringResource(id = R.string.shopping_list_purchased_header),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(
                items = uiState.purchased,
                key = { "purchased-${it.id}" }
            ) { item ->
                ShoppingListRow(
                    model = item,
                    onTogglePurchased = { onTogglePurchased(item.id, it) },
                    onDeleteItem = { onDeleteItem(item.id) },
                    onClick = { onItemClick(item.id) }
                )
            }
        }
    }
}

@Composable
private fun EmptySection(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(id = R.string.shopping_list_empty_title),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(id = R.string.shopping_list_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ShoppingListRow(
    model: ShoppingItemUiModel,
    onTogglePurchased: (Boolean) -> Unit,
    onDeleteItem: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = model.isPurchased,
                onCheckedChange = onTogglePurchased
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = model.title,
                    style = MaterialTheme.typography.titleLarge
                )
                if (!model.note.isNullOrBlank()) {
                    Text(
                        text = model.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDeleteItem) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(id = R.string.shopping_list_delete_item)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(
                id = R.string.shopping_list_linked_places_count,
                model.linkedPlaceCount
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

data class PermissionPromptUiModel(
    val key: String,
    val title: String,
    val message: String,
    val actionLabel: String,
    val onClick: () -> Unit
)

@Composable
private fun PermissionPromptCard(prompt: PermissionPromptUiModel, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = prompt.title, style = MaterialTheme.typography.titleMedium)
            Text(text = prompt.message, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = prompt.onClick) {
                Text(text = prompt.actionLabel)
            }
        }
    }
}

@Composable
private fun AddItemDialog(
    title: String,
    note: String,
    showTitleValidationError: Boolean,
    errorMessage: String?,
    onTitleInputChange: (String) -> Unit,
    onNoteInputChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onAddPlaceRequest: () -> Unit,
    pendingPlaces: List<PendingPlaceUiModel>,
    onRemovePendingPlace: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.shopping_list_add_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleInputChange,
                    label = { Text(text = stringResource(id = R.string.shopping_list_add_dialog_title_hint)) },
                    singleLine = true,
                    isError = showTitleValidationError
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = onNoteInputChange,
                    label = { Text(text = stringResource(id = R.string.shopping_list_add_dialog_note_hint)) },
                    singleLine = false,
                    minLines = 2
                )
                if (showTitleValidationError) {
                    Text(
                        text = stringResource(id = R.string.shopping_list_validation_title_required),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (pendingPlaces.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = stringResource(R.string.shopping_list_linked_places_header), style = MaterialTheme.typography.titleSmall)
                        pendingPlaces.forEach { place ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = place.name, modifier = Modifier.weight(1f))
                                TextButton(onClick = { onRemovePendingPlace(place.placeId) }) {
                                    Text(text = stringResource(R.string.shopping_list_remove_place))
                                }
                            }
                        }
                    }
                }
                TextButton(onClick = onAddPlaceRequest) {
                    Text(text = stringResource(id = R.string.shopping_list_add_place))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(id = R.string.shopping_list_add_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.shopping_list_add_dialog_cancel))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun ShoppingListScreenPreview() {
    val state = ShoppingListUiState(
        notPurchased = listOf(
            ShoppingItemUiModel(1, "牛乳", "2本買う", 2, false),
            ShoppingItemUiModel(2, "卵", "Lサイズ", 1, false)
        ),
        purchased = listOf(
            ShoppingItemUiModel(3, "掃除用具", "スポンジ", 3, true)
        )
    )
    MapShoppingListTheme {
        ShoppingListScreen(
            uiState = state,
            permissionPrompts = emptyList(),
            onAddItemClick = {},
            onTogglePurchased = { _, _ -> },
            onDeleteItem = {},
            onAddDialogDismiss = {},
            onAddDialogConfirm = {},
            onTitleInputChange = {},
            onNoteInputChange = {},
            onAddPlaceRequest = {},
            onRemovePendingPlace = {},
            onItemClick = {}
        )
    }
}

private fun shouldRequestBackgroundLocation(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
    val foregroundGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    if (!foregroundGranted) return false
    val backgroundGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    return !backgroundGranted
}

private fun shouldRequestNotificationPermission(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
    val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    return !granted
}
