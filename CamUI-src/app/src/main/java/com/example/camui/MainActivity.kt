package com.example.camui

import android.graphics.Color
import android.os.Bundle
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

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
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
                    transitionSpec = {
                        (fadeIn(tween(260)) + scaleIn(
                            initialScale = 0.86f,
                            animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f)
                        )) togetherWith (fadeOut(tween(150)) + scaleOut(
                            targetScale = 0.94f,
                            animationSpec = tween(170)
                        ))
                    },
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
                        onOpenAlbum = { page = "album" },
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
