package com.mapshoppinglist.nearby

import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.DetectedActivity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NearbyActivityTransitionReceiverTest {

    @Test
    fun `containsStillEnterTransition returns true when still enter exists`() {
        val events = listOf(
            ActivityTransitionEvent(
                DetectedActivity.WALKING,
                ActivityTransition.ACTIVITY_TRANSITION_EXIT,
                100L
            ),
            ActivityTransitionEvent(
                DetectedActivity.STILL,
                ActivityTransition.ACTIVITY_TRANSITION_ENTER,
                200L
            )
        )

        assertTrue(NearbyActivityTransitionReceiver.containsStillEnterTransition(events))
    }

    @Test
    fun `containsStillEnterTransition returns false for non matching transitions`() {
        val events = listOf(
            ActivityTransitionEvent(
                DetectedActivity.STILL,
                ActivityTransition.ACTIVITY_TRANSITION_EXIT,
                100L
            ),
            ActivityTransitionEvent(
                DetectedActivity.ON_FOOT,
                ActivityTransition.ACTIVITY_TRANSITION_ENTER,
                200L
            )
        )

        assertFalse(NearbyActivityTransitionReceiver.containsStillEnterTransition(events))
    }
}
