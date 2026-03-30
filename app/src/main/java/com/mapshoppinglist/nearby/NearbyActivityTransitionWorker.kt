package com.mapshoppinglist.nearby

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class NearbyActivityTransitionWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
    override suspend fun doWork(): Result {
        if (!ActivityRecognitionPermission.isGranted(applicationContext)) {
            Log.d(TAG, "Skipping transition registration: ACTIVITY_RECOGNITION not granted")
            return Result.success()
        }

        return try {
            ActivityRecognition.getClient(applicationContext)
                .requestActivityTransitionUpdates(
                    buildTransitionRequest(),
                    NearbyActivityTransitionPendingIntentProvider(applicationContext).get()
                )
                .await()
            Log.d(TAG, "Registered nearby activity transitions")
            Result.success()
        } catch (error: Exception) {
            Log.e(TAG, "Failed to register nearby activity transitions", error)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "NearbyActTransitionWkr"
        private const val WORK_NAME = "nearby_activity_transition_registration"

        fun enqueueNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<NearbyActivityTransitionWorker>()
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        internal fun buildTransitionRequest(): ActivityTransitionRequest {
            return ActivityTransitionRequest(buildTransitions())
        }

        internal fun buildTransitions(): List<ActivityTransition> {
            val movingTypes = listOf(
                DetectedActivity.IN_VEHICLE,
                DetectedActivity.ON_BICYCLE,
                DetectedActivity.ON_FOOT,
                DetectedActivity.WALKING,
                DetectedActivity.RUNNING
            )
            return buildList {
                movingTypes.forEach { type ->
                    add(type.enterTransition())
                    add(type.exitTransition())
                }
                add(DetectedActivity.STILL.enterTransition())
            }
        }

        private fun Int.enterTransition(): ActivityTransition = ActivityTransition.Builder()
            .setActivityType(this)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
            .build()

        private fun Int.exitTransition(): ActivityTransition = ActivityTransition.Builder()
            .setActivityType(this)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
            .build()
    }
}
