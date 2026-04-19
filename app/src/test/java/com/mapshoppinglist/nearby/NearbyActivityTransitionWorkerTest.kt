package com.mapshoppinglist.nearby

import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.DetectedActivity
import org.junit.Assert.assertTrue
import org.junit.Test

class NearbyActivityTransitionWorkerTest {

    @Test
    fun `buildTransitionRequest includes still enter and moving transitions`() {
        val transitions = NearbyActivityTransitionWorker.buildTransitions()

        assertTrue(
            transitions.any {
                it.activityType == DetectedActivity.STILL &&
                    it.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER
            }
        )
        assertTrue(
            transitions.any {
                it.activityType == DetectedActivity.ON_FOOT &&
                    it.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER
            }
        )
        assertTrue(
            transitions.any {
                it.activityType == DetectedActivity.IN_VEHICLE &&
                    it.transitionType == ActivityTransition.ACTIVITY_TRANSITION_EXIT
            }
        )
    }
}
