package com.mapshoppinglist.nearby

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.DetectedActivity
import com.mapshoppinglist.domain.model.NearbyStoreCandidate
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NearbyActivityEventLogWriter(
    private val filesDir: File,
    private val nowMillisProvider: () -> Long = System::currentTimeMillis,
    private val enabled: Boolean = true
) {

    fun appendTransitionEvents(action: String?, events: List<ActivityTransitionEvent>) {
        appendLines(
            events.map { event ->
                buildString {
                    append("kind=transition")
                    append(" action=").append(action ?: "null")
                    append(" activity=").append(activityName(event.activityType))
                    append(" transition=").append(transitionName(event.transitionType))
                    append(" elapsedNanos=").append(event.elapsedRealTimeNanos)
                }
            }
        )
    }

    fun appendSuggestionSearchResult(
        reason: String,
        itemTitle: String,
        candidates: List<NearbyStoreCandidate>,
        selectedCandidate: NearbyStoreCandidate?
    ) {
        appendLines(
            listOf(
                buildString {
                    append("kind=suggestion_search")
                    append(" reason=").append(sanitize(reason))
                    append(" item=").append(sanitize(itemTitle))
                    append(" selected=").append(formatCandidate(selectedCandidate))
                    append(" candidates=")
                    append(
                        candidates.joinToString(
                            prefix = "[",
                            postfix = "]"
                        ) { candidate -> formatCandidate(candidate) }
                    )
                }
            )
        )
    }

    fun appendSuggestionSearchQuery(reason: String, itemTitle: String, searchQuery: String) {
        appendLines(
            listOf(
                buildString {
                    append("kind=suggestion_query")
                    append(" reason=").append(sanitize(reason))
                    append(" item=").append(sanitize(itemTitle))
                    append(" query=").append(sanitize(searchQuery))
                }
            )
        )
    }

    fun appendDiagnostic(action: String?, message: String) {
        appendLines(
            listOf(
                buildString {
                    append("kind=diagnostic")
                    append(" action=").append(action ?: "null")
                    append(" message=").append(message)
                }
            )
        )
    }

    internal fun logFile(): File = File(filesDir, LOG_RELATIVE_PATH)

    private fun appendLines(messages: List<String>) {
        if (!enabled) return
        if (messages.isEmpty()) return

        val file = logFile().apply {
            parentFile?.mkdirs()
            if (!exists()) {
                createNewFile()
            }
        }
        val timestamp = timestamp(nowMillisProvider())

        runCatching {
            file.appendText(
                messages.joinToString(separator = "\n", postfix = "\n") { message ->
                    "[$timestamp] $message"
                }
            )
        }.onFailure { error ->
            Log.w(TAG, "Failed to append nearby activity event log", error)
        }
    }

    private fun timestamp(nowMillis: Long): String {
        return requireNotNull(DATE_FORMAT.get()).format(Date(nowMillis))
    }

    private fun formatCandidate(candidate: NearbyStoreCandidate?): String {
        if (candidate == null) return "none"
        return buildString {
            append(sanitize(candidate.placeId))
            append(":")
            append(sanitize(candidate.name))
            append(":")
            append(candidate.distanceMeters)
            append("m")
            append(":")
            append(sanitize(candidate.primaryType ?: "unknown"))
        }
    }

    private fun sanitize(value: String): String {
        return value.replace('\n', ' ').replace('\r', ' ').replace(' ', '_')
    }

    companion object {
        internal const val LOG_RELATIVE_PATH = "logs/nearby-activity-events.txt"
        private const val TAG = "NearbyActEventLog"

        private val DATE_FORMAT = object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue(): SimpleDateFormat {
                return SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
            }
        }

        internal fun activityName(activityType: Int): String {
            return when (activityType) {
                DetectedActivity.IN_VEHICLE -> "IN_VEHICLE"
                DetectedActivity.ON_BICYCLE -> "ON_BICYCLE"
                DetectedActivity.ON_FOOT -> "ON_FOOT"
                DetectedActivity.RUNNING -> "RUNNING"
                DetectedActivity.STILL -> "STILL"
                DetectedActivity.TILTING -> "TILTING"
                DetectedActivity.UNKNOWN -> "UNKNOWN"
                DetectedActivity.WALKING -> "WALKING"
                else -> "TYPE_$activityType"
            }
        }

        internal fun transitionName(transitionType: Int): String {
            return when (transitionType) {
                ActivityTransition.ACTIVITY_TRANSITION_ENTER -> "ENTER"
                ActivityTransition.ACTIVITY_TRANSITION_EXIT -> "EXIT"
                else -> "TRANSITION_$transitionType"
            }
        }

        fun fromContext(context: Context): NearbyActivityEventLogWriter {
            val isDebuggable =
                (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            return NearbyActivityEventLogWriter(
                filesDir = context.applicationContext.filesDir,
                enabled = isDebuggable
            )
        }
    }
}
