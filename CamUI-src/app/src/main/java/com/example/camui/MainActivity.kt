package com.example.camui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : AppCompatActivity() {
    /**
     * The manifest opts this Activity into handling orientation/size changes. Keeping the
     * Activity and its Compose composition alive prevents rememberCoroutineScope work in
     * CameraScreen (including an active HDMP render) from being cancelled by rotation.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        hideStatusBar()
    }

    /**
     * Open the device gallery. Fall back to the system image viewer, then to the
     * in-app album on devices that do not expose either external activity.
     */
    private fun openDeviceGallery(latestPhoto: Uri?): Boolean {
        val galleryIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_GALLERY)
        }
        try {
            startActivity(galleryIntent)
            return true
        } catch (_: ActivityNotFoundException) {
            // Some OEM gallery apps do not publish CATEGORY_APP_GALLERY.
        } catch (_: SecurityException) {
            // Continue with the content-URI image viewer fallback.
        }

        if (latestPhoto != null) {
            val imageIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(latestPhoto, "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                startActivity(imageIntent)
                return true
            } catch (_: ActivityNotFoundException) {
                // No compatible external image viewer is installed.
            } catch (_: SecurityException) {
                // Fall through to the in-app album.
            }
        }
        return false
    }

    private fun hideStatusBar() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.statusBars())
        }
    }

    override fun onResume() {
        super.onResume()
        hideStatusBar()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideStatusBar()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Request Android's camera-friendly seamless rotation transition (API 26+).
        // The system may fall back to crossfade when a seamless surface rotation is unavailable.
        window.attributes = window.attributes.apply {
            rotationAnimation = WindowManager.LayoutParams.ROTATION_ANIMATION_SEAMLESS
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        hideStatusBar()
        val settingsPrefs = getSharedPreferences("fujifilm_settings", MODE_PRIVATE)
        setContent {
            CamTheme {
                var page by remember { mutableStateOf("camera") }
                var showGrid by remember { mutableStateOf(settingsPrefs.getBoolean("viewfinder_grid_enabled", true)) }
                var showHist by remember { mutableStateOf(settingsPrefs.getBoolean("viewfinder_histogram_enabled", true)) }
                var focusPeaking by remember { mutableStateOf(settingsPrefs.getBoolean("viewfinder_focus_peaking_enabled", false)) }
                var ratioIdx by remember { mutableIntStateOf(settingsPrefs.getInt("capture_ratio_index", 1).coerceIn(0, 4)) }

                BackHandler(enabled = page != "camera") { page = "camera" }
                AnimatedContent(
                    targetState = page,
                    modifier = Modifier.fillMaxSize(),
                    // Each destination rises from a slightly smaller card and expands to full screen.
                    transitionSpec = { AnimaT.enter() togetherWith AnimaT.exit() },
                    label = "Fujifilm Mode page transition"
                ) { targetPage ->
                when (targetPage) {
                    "album" -> AlbumScreen(onBack = { page = "camera" })
                    "settings" -> SettingsScreen(
                        onBack = { page = "camera" },
                        ratioIdx = ratioIdx,
                        onRatio = { ratioIdx = it; settingsPrefs.edit().putInt("capture_ratio_index", it).apply() },
                        showGrid = showGrid,
                        onShowGrid = { showGrid = it; settingsPrefs.edit().putBoolean("viewfinder_grid_enabled", it).apply() },
                        showHist = showHist,
                        onShowHist = { showHist = it; settingsPrefs.edit().putBoolean("viewfinder_histogram_enabled", it).apply() },
                        focusPeaking = focusPeaking,
                        onFocusPeaking = { focusPeaking = it; settingsPrefs.edit().putBoolean("viewfinder_focus_peaking_enabled", it).apply() }
                    )
                    "toolbox" -> ToolboxScreen(
                        onBack = { page = "camera" },
                        showGrid = showGrid,
                        onShowGrid = { showGrid = it; settingsPrefs.edit().putBoolean("viewfinder_grid_enabled", it).apply() },
                        showHist = showHist,
                        onShowHist = { showHist = it; settingsPrefs.edit().putBoolean("viewfinder_histogram_enabled", it).apply() },
                        focusPeaking = focusPeaking,
                        onFocusPeaking = { focusPeaking = it; settingsPrefs.edit().putBoolean("viewfinder_focus_peaking_enabled", it).apply() }
                    )
                    "contents" -> ContentManagementScreen(onBack = { page = "camera" })
                    else -> CameraScreen(
                        onOpenSettings = { page = "settings" },
                        onOpenToolbox = { page = "toolbox" },
                        onOpenContentManagement = { page = "contents" },
                        onOpenAlbum = { latestPhoto ->
                            if (!openDeviceGallery(latestPhoto)) page = "album"
                        },
                        showGrid = showGrid,
                        onShowGrid = { showGrid = it; settingsPrefs.edit().putBoolean("viewfinder_grid_enabled", it).apply() },
                        showHist = showHist,
                        onShowHist = { showHist = it; settingsPrefs.edit().putBoolean("viewfinder_histogram_enabled", it).apply() },
                        focusPeaking = focusPeaking,
                        onFocusPeaking = { focusPeaking = it; settingsPrefs.edit().putBoolean("viewfinder_focus_peaking_enabled", it).apply() },
                        ratioIdx = ratioIdx,
                        onRatio = { ratioIdx = it; settingsPrefs.edit().putInt("capture_ratio_index", it).apply() }
                    )
                }
                }
            }
        }
    }
}
