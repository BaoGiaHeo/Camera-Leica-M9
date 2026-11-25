package com.example.camui

import java.util.concurrent.atomic.AtomicLong

/**
 * Lightweight process counters. PixelOsdLayer is the single Compose owner for
 * the canvas-rendered HUD and zoom bar, so these counters measure layer recompositions
 * while each region is active (not draw frames).
 */
internal object UiRecompositionCounters {
    private val hud = AtomicLong(0L)
    private val zoom = AtomicLong(0L)

    fun recordHudComposition() {
        hud.incrementAndGet()
    }

    fun recordZoomComposition() {
        zoom.incrementAndGet()
    }

    fun snapshot(): String =
        "PixelOsdLayer HUD compositions=${hud.get()}, zoom-bar-active compositions=${zoom.get()}"

    internal fun resetForTests() {
        hud.set(0L)
        zoom.set(0L)
    }
}
