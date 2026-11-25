package com.example.camui

import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale

/**
 * Small, process-local crash context. It intentionally stores no image, URI, EXIF,
 * location, camera ID, or user-entered text. The ring buffer is bounded to 50 UI events.
 */
internal object CrashContextDiagnostics {
    private const val MAX_EVENTS = 50
    private val lock = Any()
    private val recentEvents = ArrayDeque<String>()

    private var route = "camera"
    private var panel = "NONE"
    private var mode = "Photo"
    private var aspectRatio = "unknown"
    private var orientation = "unknown"
    private var language = "en"
    private var uiStyle = "LCD"

    fun updateRoute(value: String) {
        synchronized(lock) {
            if (route != value) {
                route = value
                addEventLocked("ROUTE=$value")
            }
        }
    }

    fun updateCameraState(
        mode: String,
        panel: String,
        aspectRatio: String,
        orientation: String,
        language: String,
        uiStyle: String
    ) {
        synchronized(lock) {
            val previousPanel = this.panel
            val previousMode = this.mode
            val previousRatio = this.aspectRatio
            this.panel = panel
            this.mode = mode
            this.aspectRatio = aspectRatio
            this.orientation = orientation
            this.language = language
            this.uiStyle = uiStyle
            if (previousPanel != panel) addEventLocked("PANEL=$panel")
            if (previousMode != mode) addEventLocked("MODE=$mode")
            if (previousRatio != aspectRatio) addEventLocked("ASPECT=$aspectRatio")
        }
    }

    fun recordUiEvent(event: String) {
        synchronized(lock) {
            addEventLocked(event)
        }
    }

    fun snapshot(): String = synchronized(lock) {
        buildString {
            appendLine("Route: $route")
            appendLine("Panel: $panel")
            appendLine("Mode: $mode")
            appendLine("Aspect ratio: $aspectRatio")
            appendLine("Orientation: $orientation")
            appendLine("Language: $language")
            appendLine("UI style: $uiStyle")
            appendLine("Recent UI events (oldest to newest, max $MAX_EVENTS):")
            if (recentEvents.isEmpty()) appendLine("(none)")
            else recentEvents.forEach { appendLine(it) }
        }
    }

    internal fun resetForTests() {
        synchronized(lock) {
            recentEvents.clear()
            route = "camera"
            panel = "NONE"
            mode = "Photo"
            aspectRatio = "unknown"
            orientation = "unknown"
            language = "en"
            uiStyle = "LCD"
        }
    }

    private fun addEventLocked(event: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        val safeEvent = event.replace('\n', ' ').take(160)
        recentEvents.addLast("[$timestamp] $safeEvent")
        while (recentEvents.size > MAX_EVENTS) recentEvents.removeFirst()
    }
}
