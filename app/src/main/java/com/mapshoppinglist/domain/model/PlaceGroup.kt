package com.mapshoppinglist.domain.model

/**
 * 地点ごとにグルーピングされたアイテムリスト（購入場所タブ用）
 */
data class PlaceGroup(val place: Place?, val items: List<ShoppingItem>)
