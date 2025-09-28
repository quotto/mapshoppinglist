package com.mapshoppinglist.domain.exception

/**
 * 同一地点の重複登録を示す例外。
 */
class DuplicatePlaceException : DomainException("同じ地点がお店として既に登録されています")
