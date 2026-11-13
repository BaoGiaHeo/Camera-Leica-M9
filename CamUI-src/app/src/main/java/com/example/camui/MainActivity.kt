package com.example.camui

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            CamTheme {
                var page by remember { mutableStateOf("camera") }
                var showGrid by remember { mutableStateOf(true) }
                var showHist by remember { mutableStateOf(true) }
                var focusPeaking by remember { mutableStateOf(false) }
                var ratioIdx by remember { mutableIntStateOf(1) }

                BackHandler(enabled = page != "camera") { page = "camera" }
                when (page) {
                    "settings" -> SettingsScreen(
                        onBack = { page = "camera" },
                        ratioIdx = ratioIdx,
                        onRatio = { ratioIdx = it },
                        showGrid = showGrid,
                        onShowGrid = { showGrid = it },
                        showHist = showHist,
                        onShowHist = { showHist = it },
                        focusPeaking = focusPeaking,
                        onFocusPeaking = { focusPeaking = it }
                    )
                    "toolbox" -> ToolboxScreen(
                        onBack = { page = "camera" },
                        showGrid = showGrid,
                        onShowGrid = { showGrid = it },
                        showHist = showHist,
                        onShowHist = { showHist = it },
                        focusPeaking = focusPeaking,
                        onFocusPeaking = { focusPeaking = it }
                    )
                    "contents" -> ContentManagementScreen(onBack = { page = "camera" })
                    else -> CameraScreen(
                        onOpenSettings = { page = "settings" },
                        onOpenToolbox = { page = "toolbox" },
                        onOpenContentManagement = { page = "contents" },
                        showGrid = showGrid,
                        onShowGrid = { showGrid = it },
                        showHist = showHist,
                        onShowHist = { showHist = it },
                        focusPeaking = focusPeaking,
                        onFocusPeaking = { focusPeaking = it },
                        ratioIdx = ratioIdx,
                        onRatio = { ratioIdx = it }
                    )
                }
            }
        }
    }
}
