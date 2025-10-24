package com.mapshoppinglist.testtag

/**
 * UIテストで利用するタグを画面単位でまとめる。
 */
object ShoppingListTestTags {
    const val EMPTY_STATE: String = "test_tag_shopping_list_empty_state"
    const val ADD_FAB: String = "test_tag_shopping_list_add_fab"
    const val ITEM_NOT_PURCHASED_PREFIX: String = "test_tag_shopping_list_item_not_purchased"
    const val ITEM_PURCHASED_PREFIX: String = "test_tag_shopping_list_item_purchased"
    const val ITEM_CHECKBOX_PREFIX: String = "test_tag_shopping_list_item_checkbox"
    const val ITEM_DELETE_PREFIX: String = "test_tag_shopping_list_item_delete"
    const val ADD_ITEM_TITLE_INPUT: String = "test_tag_add_item_title_input"
}

/**
 * アイテム詳細画面で利用するテストタグ。
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

/**
 * 地図から地点を選択する画面で利用するテストタグ。
 */
object PlacePickerTestTags {
    const val MAP: String = "test_tag_place_picker_map"
    const val LOCATION_PERMISSION_PLACEHOLDER: String = "test_tag_place_picker_location_placeholder"
}

/**
 * 最近使った地点画面で利用するテストタグ。
 */
object RecentPlacesTestTags {
    const val PLACE_LIST: String = "recent_places_list"
    const val PLACE_ROW_PREFIX: String = "recent_places_row_"
}

/**
 * 地点管理画面で利用するテストタグ。
 */
object PlaceManagementTestTags {
    const val PLACE_LIST: String = "place_management_list"
    const val PLACE_ROW_PREFIX: String = "place_management_row_"
    const val PLACE_DELETE_PREFIX: String = "place_management_delete_"
    const val DELETE_DIALOG_CONFIRM: String = "place_management_delete_confirm"
    const val DELETE_DIALOG_CANCEL: String = "place_management_delete_cancel"
}
