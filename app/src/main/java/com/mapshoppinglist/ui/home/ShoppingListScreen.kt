package com.mapshoppinglist.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mapshoppinglist.MapShoppingListApplication
import com.mapshoppinglist.R
import com.mapshoppinglist.testtag.ShoppingListTestTags
import com.mapshoppinglist.ui.theme.MapShoppingListTheme

@Composable
fun ShoppingListRoute(
    onAddPlaceViaSearch: () -> Unit = {},
    onAddPlaceViaRecent: () -> Unit = {},
    onItemClick: (Long) -> Unit = {},
    newPlaceId: Long? = null,
    onNewPlaceConsumed: () -> Unit = {},
    onManagePlaces: () -> Unit = {},
    onShowPrivacyPolicy: () -> Unit = {},
    onShowOssLicenses: () -> Unit = {}
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
        showBackgroundPrompt = !granted
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
                            backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
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
        onAddPlaceViaSearch = onAddPlaceViaSearch,
        onAddPlaceViaRecent = onAddPlaceViaRecent,
        onRemovePendingPlace = viewModel::onRemovePendingPlace,
        onItemClick = onItemClick,
        onManagePlaces = onManagePlaces,
        onShowPrivacyPolicy = onShowPrivacyPolicy,
        onShowOssLicenses = onShowOssLicenses,
        onTabSelected = viewModel::onTabSelected
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
    onAddPlaceViaSearch: () -> Unit,
    onAddPlaceViaRecent: () -> Unit,
    onRemovePendingPlace: (Long) -> Unit,
    onItemClick: (Long) -> Unit,
    onManagePlaces: () -> Unit,
    onShowPrivacyPolicy: () -> Unit,
    onShowOssLicenses: () -> Unit,
    onTabSelected: (ListTab) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.shopping_list_title),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.common_more_options),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    DropdownMenu(
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_manage_places)) },
                            onClick = {
                                menuExpanded = false
                                onManagePlaces()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_privacy_policy)) },
                            onClick = {
                                menuExpanded = false
                                onShowPrivacyPolicy()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_oss_licenses)) },
                            onClick = {
                                menuExpanded = false
                                onShowOssLicenses()
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    scrolledContainerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.testTag(ShoppingListTestTags.ADD_FAB),
                onClick = onAddItemClick,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiary,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(id = R.string.shopping_list_add_item)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = uiState.selectedTab == ListTab.PurchaseStatus,
                    onClick = { onTabSelected(ListTab.PurchaseStatus) },
                    text = { Text(stringResource(R.string.tab_purchase_status)) },
                    modifier = Modifier.testTag(ShoppingListTestTags.TAB_PURCHASE_STATUS)
                )
                Tab(
                    selected = uiState.selectedTab == ListTab.PlaceGroup,
                    onClick = { onTabSelected(ListTab.PlaceGroup) },
                    text = { Text(stringResource(R.string.tab_place_group)) },
                    modifier = Modifier.testTag(ShoppingListTestTags.TAB_PLACE_GROUP)
                )
            }

            when (uiState.selectedTab) {
                ListTab.PurchaseStatus -> {
                    ShoppingListContent(
                        uiState = uiState,
                        permissionPrompts = permissionPrompts,
                        contentPadding = PaddingValues(0.dp),
                        onTogglePurchased = onTogglePurchased,
                        onDeleteItem = onDeleteItem,
                        onItemClick = onItemClick
                    )
                }
                ListTab.PlaceGroup -> {
                    PlaceGroupContent(
                        uiState = uiState,
                        contentPadding = PaddingValues(0.dp),
                        onTogglePurchased = onTogglePurchased,
                        onDeleteItem = onDeleteItem,
                        onItemClick = onItemClick
                    )
                }
            }
        }
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
            onAddPlaceViaSearch = onAddPlaceViaSearch,
            onAddPlaceViaRecent = onAddPlaceViaRecent,
            pendingPlaces = uiState.pendingPlaces,
            onRemovePendingPlace = onRemovePendingPlace,
            recentPlaces = uiState.recentPlaces
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
            top = 8.dp,
            bottom = 80.dp
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
            .padding(vertical = 48.dp)
            .testTag(ShoppingListTestTags.EMPTY_STATE),
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
    val rowTag = if (model.isPurchased) {
        "${ShoppingListTestTags.ITEM_PURCHASED_PREFIX}${model.id}"
    } else {
        "${ShoppingListTestTags.ITEM_NOT_PURCHASED_PREFIX}${model.id}"
    }
    val checkboxTag = "${ShoppingListTestTags.ITEM_CHECKBOX_PREFIX}${model.id}"
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag(rowTag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (model.isPurchased) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        modifier = Modifier.testTag(checkboxTag),
                        checked = model.isPurchased,
                        onCheckedChange = onTogglePurchased,
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.outline,
                            checkmarkColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    Column {
                        Text(
                            text = model.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (model.isPurchased) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        if (!model.note.isNullOrBlank()) {
                            Text(
                                text = model.note,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
            FilledIconButton(
                modifier = modifier.align(Alignment.CenterVertically).testTag(
                    ShoppingListTestTags.ITEM_DELETE_PREFIX + model.id
                ),
                onClick = onDeleteItem,
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(id = R.string.shopping_list_delete_item)
                )
            }
        }
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = prompt.title, style = MaterialTheme.typography.titleMedium)
            Text(text = prompt.message, style = MaterialTheme.typography.bodyMedium)
            Button(
                onClick = prompt.onClick,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            ) {
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
    onAddPlaceViaSearch: () -> Unit,
    onAddPlaceViaRecent: () -> Unit,
    pendingPlaces: List<PendingPlaceUiModel>,
    onRemovePendingPlace: (Long) -> Unit,
    recentPlaces: List<RecentPlaceUiModel>
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
                    isError = showTitleValidationError,
                    modifier = Modifier.testTag(ShoppingListTestTags.ADD_ITEM_TITLE_INPUT)
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.shopping_list_add_place_section_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Button(
                        onClick = onAddPlaceViaSearch,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(R.string.item_detail_add_place_search))
                    }
                    Button(
                        onClick = onAddPlaceViaRecent,
                        enabled = recentPlaces.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(R.string.item_detail_add_place_recent))
                    }
                    if (recentPlaces.isEmpty()) {
                        Text(
                            text = stringResource(R.string.recent_places_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (pendingPlaces.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.shopping_list_linked_places_header),
                            style = MaterialTheme.typography.titleSmall
                        )
                        pendingPlaces.forEach { place ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = place.name, modifier = Modifier.weight(1f))
                                FilledIconButton(
                                    onClick = { onRemovePendingPlace(place.placeId) },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = MaterialTheme.colorScheme.tertiary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = stringResource(R.string.shopping_list_remove_place)
                                    )
                                }
                            }
                        }
                    }
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
            onAddPlaceViaSearch = {},
            onAddPlaceViaRecent = {},
            onRemovePendingPlace = {},
            onItemClick = {},
            onManagePlaces = {},
            onShowPrivacyPolicy = {},
            onShowOssLicenses = {},
            onTabSelected = {}
        )
    }
}

private fun shouldRequestBackgroundLocation(context: android.content.Context): Boolean {
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

@Composable
private fun PlaceGroupContent(
    uiState: ShoppingListUiState,
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
            top = 8.dp,
            bottom = 80.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (uiState.placeGroups.isEmpty()) {
            item(key = "empty_place_group") {
                EmptySection()
            }
        } else {
            uiState.placeGroups.forEach { group ->
                item(key = "header_${group.placeId ?: "unset"}") {
                    PlaceGroupHeader(
                        placeName = group.placeName,
                        itemCount = group.items.size,
                        placeId = group.placeId
                    )
                }
                items(
                    items = group.items,
                    key = { "place_${group.placeId}_item_${it.id}" }
                ) { item ->
                    ShoppingListRow(
                        model = item,
                        onTogglePurchased = { onTogglePurchased(item.id, it) },
                        onDeleteItem = { onDeleteItem(item.id) },
                        onClick = { onItemClick(item.id) },
                        modifier = Modifier.testTag(
                            ShoppingListTestTags.PLACE_GROUP_ITEM_PREFIX +
                                "${group.placeId ?: "unset"}_${item.id}"
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceGroupHeader(placeName: String, itemCount: Int, placeId: Long?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag(ShoppingListTestTags.PLACE_GROUP_HEADER_PREFIX + (placeId?.toString() ?: "unset")),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "📍 $placeName",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "($itemCount)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
