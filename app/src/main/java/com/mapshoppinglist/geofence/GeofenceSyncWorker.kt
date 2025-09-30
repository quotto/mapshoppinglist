package com.mapshoppinglist.geofence

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mapshoppinglist.MapShoppingListApplication
import java.util.concurrent.TimeUnit

class GeofenceSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as MapShoppingListApplication
        val forceRebuild = inputData.getBoolean(KEY_FORCE_REBUILD, false)
        return try {
            app.geofenceSyncCoordinator.sync(forceRebuild)
            Result.success()
        } catch (error: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "geofence_sync"
        private const val KEY_FORCE_REBUILD = "force_rebuild"

        fun enqueueNow(context: Context, forceRebuild: Boolean = false) {
            val request = OneTimeWorkRequestBuilder<GeofenceSyncWorker>()
                .setInputData(androidx.work.Data.Builder().putBoolean(KEY_FORCE_REBUILD, forceRebuild).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
