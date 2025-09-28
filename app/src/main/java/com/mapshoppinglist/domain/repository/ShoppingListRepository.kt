package com.mapshoppinglist.domain.repository

import com.mapshoppinglist.domain.model.ShoppingItem
import kotlinx.coroutines.flow.Flow

/**
 * 買い物リストの永続化操作を抽象化するリポジトリ。
 */
interface ShoppingListRepository {
    /**
     * アイテム一覧を更新順で監視する。
     */
    fun observeAllItems(): Flow<List<ShoppingItem>>

    /**
     * 新しいアイテムを追加する。
     */
    suspend fun addItem(title: String, note: String?)

    /**
     * アイテムの購入済み状態を切り替える。
     */
    suspend fun updatePurchasedState(itemId: Long, isPurchased: Boolean)

    /**
     * アイテムを削除する。
     */
    suspend fun deleteItem(itemId: Long)

    /**
     * 指定したお店に紐づく未購入アイテムを取得する。
     */
    suspend fun getItemsForPlace(placeId: Long): List<ShoppingItem>
}
