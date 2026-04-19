package com.mapshoppinglist.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.mapshoppinglist.data.local.dao.AppSettingsDao
import com.mapshoppinglist.data.local.dao.GeofenceRegistryDao
import com.mapshoppinglist.data.local.dao.ItemPlaceDao
import com.mapshoppinglist.data.local.dao.ItemsDao
import com.mapshoppinglist.data.local.dao.NearbySuggestionStateDao
import com.mapshoppinglist.data.local.dao.NotifyStateDao
import com.mapshoppinglist.data.local.dao.PlacesDao
import com.mapshoppinglist.data.local.entity.AppSettingEntity
import com.mapshoppinglist.data.local.entity.GeofenceRegistrationEntity
import com.mapshoppinglist.data.local.entity.ItemEntity
import com.mapshoppinglist.data.local.entity.ItemPlaceCrossRef
import com.mapshoppinglist.data.local.entity.NearbySuggestionStateEntity
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
        NearbySuggestionStateEntity::class,
        AppSettingEntity::class,
        GeofenceRegistrationEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun itemsDao(): ItemsDao

    abstract fun placesDao(): PlacesDao

    abstract fun itemPlaceDao(): ItemPlaceDao

    abstract fun notifyStateDao(): NotifyStateDao

    abstract fun nearbySuggestionStateDao(): NearbySuggestionStateDao

    abstract fun appSettingsDao(): AppSettingsDao

    abstract fun geofenceRegistryDao(): GeofenceRegistryDao

    companion object {
        const val DATABASE_NAME: String = "shopping-list.db"

        /**
         * アプリ本番用のデータベースインスタンスを構築する。
         */
        fun build(context: Context): AppDatabase = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            DATABASE_NAME
        )
            .openHelperFactory(FrameworkSQLiteOpenHelperFactory())
            .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
            .addMigrations(MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()

        /**
         * テスト専用のインメモリデータベース。
         */
        fun buildInMemory(context: Context): AppDatabase = Room.inMemoryDatabaseBuilder(
            context.applicationContext,
            AppDatabase::class.java
        )
            .openHelperFactory(FrameworkSQLiteOpenHelperFactory())
            .allowMainThreadQueries()
            .build()

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `nearby_suggestion_state` (" +
                        "`item_id` INTEGER NOT NULL, " +
                        "`candidate_place_id` TEXT NOT NULL, " +
                        "`candidate_place_name` TEXT, " +
                        "`last_notified_at` INTEGER, " +
                        "`last_notified_lat_e6` INTEGER, " +
                        "`last_notified_lng_e6` INTEGER, " +
                        "PRIMARY KEY(`item_id`, `candidate_place_id`))"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_nearby_suggestion_state_item_id` " +
                        "ON `nearby_suggestion_state` (`item_id`)"
                )
            }
        }
    }
}
