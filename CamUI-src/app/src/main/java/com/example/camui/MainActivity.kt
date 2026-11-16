package com.example.camui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.view.OrientationEventListener
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay

class MainActivity : AppCompatActivity() {
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        hideStatusBar()
    }

    private fun openDeviceGallery(latestPhoto: Uri?): Boolean {
        val galleryIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_GALLERY)
        }
        try {
            startActivity(galleryIntent)
            return true
        } catch (_: ActivityNotFoundException) {
        } catch (_: SecurityException) {
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
            } catch (_: SecurityException) {
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
        // The app starts in portrait. Sensor rotation only offers a button; it never rotates the UI itself.
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
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
                var manualAnimationPerformanceMode by remember {
                    mutableStateOf(settingsPrefs.getBoolean("animation_performance_mode_enabled", false))
                }
                var thermalStatus by remember { mutableIntStateOf(PowerManager.THERMAL_STATUS_NONE) }
                DisposableEffect(Unit) {
                    val powerManager = this@MainActivity.getSystemService(Context.POWER_SERVICE) as? PowerManager
                    val listener = PowerManager.OnThermalStatusChangedListener { status ->
                        thermalStatus = status
                    }
                    thermalStatus = runCatching {
                        powerManager?.currentThermalStatus ?: PowerManager.THERMAL_STATUS_NONE
                    }.getOrDefault(PowerManager.THERMAL_STATUS_NONE)
                    runCatching { powerManager?.addThermalStatusListener(listener) }
                    onDispose { runCatching { powerManager?.removeThermalStatusListener(listener) } }
                }
                val animationPerformanceMode = manualAnimationPerformanceMode ||
                    thermalStatus >= PowerManager.THERMAL_STATUS_MODERATE
                var showGrid by remember { mutableStateOf(settingsPrefs.getBoolean("viewfinder_grid_enabled", true)) }
                var showHist by remember { mutableStateOf(settingsPrefs.getBoolean("viewfinder_histogram_enabled", true)) }
                var focusPeaking by remember { mutableStateOf(settingsPrefs.getBoolean("viewfinder_focus_peaking_enabled", false)) }
                var ratioIdx by remember { mutableIntStateOf(settingsPrefs.getInt("capture_ratio_index", 1).coerceIn(0, 4)) }

                BackHandler(enabled = page != "camera") { page = "camera" }
                Box(Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = page,
                        modifier = Modifier.fillMaxSize(),
                        transitionSpec = { AnimaT.enter(animationPerformanceMode) togetherWith AnimaT.exit(animationPerformanceMode) },
                        label = "FujiFilm Mode page transition"
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
                                onFocusPeaking = { focusPeaking = it; settingsPrefs.edit().putBoolean("viewfinder_focus_peaking_enabled", it).apply() },
                                animationPerformanceMode = manualAnimationPerformanceMode,
                                onAnimationPerformanceModeChanged = {
                                    manualAnimationPerformanceMode = it
                                    settingsPrefs.edit().putBoolean("animation_performance_mode_enabled", it).apply()
                                }
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
}
