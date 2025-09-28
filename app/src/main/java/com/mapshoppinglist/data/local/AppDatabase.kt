package com.mapshoppinglist.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.mapshoppinglist.data.local.dao.AppSettingsDao
import com.mapshoppinglist.data.local.dao.ItemPlaceDao
import com.mapshoppinglist.data.local.dao.GeofenceRegistryDao
import com.mapshoppinglist.data.local.dao.ItemsDao
import com.mapshoppinglist.data.local.dao.NotifyStateDao
import com.mapshoppinglist.data.local.dao.PlacesDao
import com.mapshoppinglist.data.local.entity.AppSettingEntity
import com.mapshoppinglist.data.local.entity.ItemEntity
import com.mapshoppinglist.data.local.entity.GeofenceRegistrationEntity
import com.mapshoppinglist.data.local.entity.ItemPlaceCrossRef
import com.mapshoppinglist.data.local.entity.NotifyStateEntity
import com.mapshoppinglist.data.local.entity.PlaceEntity

/**
 * Roomのメインデータベース定義。
 */
@Database(
    entities = [
        ItemEntity::class,
        PlaceEntity::class,
        ItemPlaceCrossRef::class,
        NotifyStateEntity::class,
        AppSettingEntity::class,
        GeofenceRegistrationEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun itemsDao(): ItemsDao

    abstract fun placesDao(): PlacesDao

    abstract fun itemPlaceDao(): ItemPlaceDao

    abstract fun notifyStateDao(): NotifyStateDao

    abstract fun appSettingsDao(): AppSettingsDao

    abstract fun geofenceRegistryDao(): GeofenceRegistryDao

    companion object {
        const val DATABASE_NAME: String = "shopping-list.db"

        /**
         * アプリ本番用のデータベースインスタンスを構築する。
         */
        fun build(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .openHelperFactory(FrameworkSQLiteOpenHelperFactory())
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .fallbackToDestructiveMigration()
                .build()
        }

        /**
         * テスト専用のインメモリデータベース。
         */
        fun buildInMemory(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AppDatabase::class.java
            )
                .openHelperFactory(FrameworkSQLiteOpenHelperFactory())
                .allowMainThreadQueries()
                .build()
        }
    }
}
