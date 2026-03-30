package com.mapshoppinglist.nearby

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity

class NearbyActivityTransitionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val eventLogWriter = NearbyActivityEventLogWriter.fromContext(context)
        if (intent?.action != NearbyActivityTransitionPendingIntentProvider.ACTION_ACTIVITY_TRANSITION) {
            eventLogWriter.appendDiagnostic(intent?.action, "ignored_unexpected_action")
            Log.d(TAG, "Ignored broadcast action=${intent?.action}")
            return
        }
        if (!ActivityTransitionResult.hasResult(intent)) {
            eventLogWriter.appendDiagnostic(intent.action, "missing_transition_result")
            Log.d(TAG, "No ActivityTransitionResult found in intent")
            return
        }
        val result = ActivityTransitionResult.extractResult(intent)
        val events = result?.transitionEvents.orEmpty()
        eventLogWriter.appendTransitionEvents(intent.action, events)
        if (!containsStillEnterTransition(events)) {
            eventLogWriter.appendDiagnostic(intent.action, "no_still_enter_transition")
            Log.d(TAG, "No STILL enter transition found")
            return
        }

        eventLogWriter.appendDiagnostic(intent.action, "still_enter_enqueued")
        Log.d(TAG, "STILL enter transition received, enqueue nearby suggestion worker")
        NearbySuggestionTriggerWorker.enqueueNow(
            context = context,
            reason = NearbySuggestionTriggerWorker.REASON_ACTIVITY_STILL
        )
    }

    companion object {
        private const val TAG = "NearbyActTransitionRcvr"

        internal fun containsStillEnterTransition(events: List<ActivityTransitionEvent>): Boolean {
            return events.any { event ->
                event.activityType == DetectedActivity.STILL &&
                    event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER
            }
        }
    }
}
