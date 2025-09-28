package com.mapshoppinglist.domain.exception

/**
 * 登録できる地点の上限を超えた場合の例外。
 */
class PlaceLimitExceededException : DomainException("登録できるお店の上限(100件)を超えています")
