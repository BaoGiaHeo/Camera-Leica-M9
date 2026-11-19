package com.example.camui

/**
 * Limits updates that invalidate the surrounding Compose screen.
 *
 * Raw touch samples still go straight to the live shader. Only the parent
 * state publication is rate-limited; the final value is committed on release.
 */
internal class TonePadPublishGate(
    private val minimumIntervalMillis: Long = 250L
) {
    init {
        require(minimumIntervalMillis > 0L) {
            "minimumIntervalMillis must be positive"
        }
    }

    private var lastPublishedAtMillis = Long.MIN_VALUE

    @Synchronized
    fun shouldPublish(nowMillis: Long): Boolean {
        val previous = lastPublishedAtMillis
        if (previous != Long.MIN_VALUE &&
            nowMillis - previous < minimumIntervalMillis
        ) {
            return false
        }
        lastPublishedAtMillis = nowMillis
        return true
    }
}
