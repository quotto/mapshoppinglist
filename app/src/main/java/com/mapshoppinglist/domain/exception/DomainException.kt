package com.mapshoppinglist.domain.exception

/**
 * ドメイン層で発生する例外のベースクラス。
 */
open class DomainException(message: String) : RuntimeException(message)
