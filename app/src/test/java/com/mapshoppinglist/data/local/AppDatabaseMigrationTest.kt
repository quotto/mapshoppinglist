package com.mapshoppinglist.data.local

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU], application = android.app.Application::class)
class AppDatabaseMigrationTest {

    private lateinit var context: Context
    private lateinit var helper: SupportSQLiteOpenHelper
    private val dbName = "migration-test.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
        helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(2) {
                        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) = Unit

                        override fun onUpgrade(
                            db: androidx.sqlite.db.SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int
                        ) = Unit
                    }
                )
                .build()
        )
    }

    @After
    fun tearDown() {
        helper.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun migration2To3CreatesNearbySuggestionStateTable() = runTest {
        val db = helper.writableDatabase

        AppDatabase.MIGRATION_2_3.migrate(db)

        val cursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='nearby_suggestion_state'"
        )
        cursor.use {
            assertNotNull(it)
            org.junit.Assert.assertTrue(it.moveToFirst())
        }
    }
}
