package com.example.camui

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView

/**
 * Debug-build-only entry point used by CI to exercise the uncaught-exception report flow.
 * This source set is excluded from release APKs.
 */
class CrashTestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            text = "Crash report self-test is running…"
            textSize = 20f
        })

        Handler(Looper.getMainLooper()).postDelayed({
            throw IllegalStateException("CRASH_REPORT_TEST_SENTINEL")
        }, 1_000L)
    }
}
