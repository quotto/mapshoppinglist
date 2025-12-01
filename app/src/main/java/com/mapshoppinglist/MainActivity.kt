package com.mapshoppinglist

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.mapshoppinglist.notification.NotificationActions
import com.mapshoppinglist.ui.theme.MapShoppingListTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val startItemIdState = MutableStateFlow<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        startItemIdState.value = extractItemId(intent)
        setContent {
            val startItemId by startItemIdState.collectAsState()
            MapShoppingListTheme {
                MapShoppingListApp(
                    startItemId = startItemId,
                    onDetailConsumed = { startItemIdState.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        startItemIdState.value = extractItemId(intent)
    }

    private fun extractItemId(intent: Intent?): Long? {
        val value = intent?.getLongExtra(NotificationActions.EXTRA_ITEM_ID, -1L) ?: -1L
        return value.takeIf { it > 0 }
    }
}
