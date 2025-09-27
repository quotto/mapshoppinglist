package com.mapshoppinglist.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mapshoppinglist.R
import com.mapshoppinglist.ui.theme.MapShoppingListTheme

@Composable
fun ShoppingListRoute(
    viewModel: ShoppingListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ShoppingListScreen(
        uiState = uiState,
        onAddItemClick = { /* 追加導線はタスク4で実装 */ },
        onShowPurchasedToggle = { /* フィルター切り替えは後続タスク */ }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    uiState: ShoppingListUiState,
    onAddItemClick: () -> Unit,
    onShowPurchasedToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.app_name)) },
                actions = {
                    TextButton(onClick = onShowPurchasedToggle) {
                        Text(text = stringResource(id = R.string.shopping_list_filter_purchased))
                    }
                },
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
            contentPadding = innerPadding
        )
    }
}

@Composable
private fun ShoppingListContent(
    uiState: ShoppingListUiState,
    contentPadding: PaddingValues,
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
        if (uiState.notPurchased.isEmpty()) {
            item(key = "empty") {
                EmptySection()
            }
        } else {
            items(
                items = uiState.notPurchased,
                key = { it.id }
            ) { item ->
                ShoppingListRow(model = item)
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
                ShoppingListRow(model = item)
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
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

@Preview(showBackground = true)
@Composable
private fun ShoppingListScreenPreview() {
    MapShoppingListTheme {
        ShoppingListScreen(
            uiState = ShoppingListUiState(
                notPurchased = listOf(
                    ShoppingItemUiModel(1, "牛乳", "2本買う", 2, false),
                    ShoppingItemUiModel(2, "卵", "Lサイズ", 1, false)
                ),
                purchased = listOf(
                    ShoppingItemUiModel(3, "掃除用具", "スポンジ", 3, true)
                )
            ),
            onAddItemClick = {},
            onShowPurchasedToggle = {}
        )
    }
}

