package com.mapshoppinglist.domain.exception

/**
 * アイテムの重複登録を示す例外。
 */
class DuplicateItemException : DomainException("同じタイトルのアイテムが既に存在します")
