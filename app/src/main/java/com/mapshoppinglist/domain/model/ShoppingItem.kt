package com.mapshoppinglist.domain.model

/**
 * ドメイン層で扱う買い物アイテム。
 */
data class ShoppingItem(
    val id: Long,
    val title: String,
    val note: String?,
    val isPurchased: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val linkedPlaceCount: Int
)
