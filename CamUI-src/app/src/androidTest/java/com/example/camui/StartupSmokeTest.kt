package com.example.camui

import android.Manifest
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupSmokeTest {
    @get:Rule
    val cameraPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA)

    @Test
    fun coldStartRemainsResumedAfterCameraInitialization() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            // Give CameraX provider, ExtensionsManager and the first preview bind time to run.
            Thread.sleep(3_000L)
            scenario.onActivity { activity ->
                assertFalse("MainActivity unexpectedly finished during startup", activity.isFinishing)
                assertFalse("MainActivity was destroyed during startup", activity.isDestroyed)
                assertEquals(
                    "MainActivity must remain resumed after a cold launch",
                    Lifecycle.State.RESUMED,
                    activity.lifecycle.currentState
                )
            }
        } finally {
            scenario.close()
        }
    }
}
