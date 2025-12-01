package com.mapshoppinglist.domain.usecase

import com.mapshoppinglist.domain.model.ShoppingItem

/**
 * 通知用の文言を生成するユースケース。
 */
class BuildNotificationMessageUseCase {

    fun invoke(placeName: String, items: List<ShoppingItem>): NotificationMessage {
        require(items.isNotEmpty()) { "通知するアイテムが必要です" }
        val sortedItems = items.sortedBy { it.updatedAt }
        val title = "$placeName で買うもの"
        val lines = sortedItems.map { "・${it.title}" }
        val summary = if (items.size > 1) {
            "ほか${items.size - 1}件"
        } else {
            items.first().note ?: ""
        }
        return NotificationMessage(
            title = title,
            lines = lines,
            summary = summary.ifBlank { null }
        )
    }
}

/**
 * 通知文言をまとめたモデル。
 */
data class NotificationMessage(val title: String, val lines: List<String>, val summary: String?)
