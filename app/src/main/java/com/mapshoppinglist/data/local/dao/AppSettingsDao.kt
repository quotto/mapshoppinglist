package com.mapshoppinglist.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mapshoppinglist.data.local.entity.AppSettingEntity

/**
 * アプリ設定を扱うDAO。
 */
@Dao
interface AppSettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AppSettingEntity)

    @Update
    suspend fun update(entity: AppSettingEntity)

    @Query("SELECT * FROM app_settings WHERE `key` = :key")
    suspend fun findByKey(key: String): AppSettingEntity?

    @Query("DELETE FROM app_settings WHERE `key` = :key")
    suspend fun deleteByKey(key: String)
}
