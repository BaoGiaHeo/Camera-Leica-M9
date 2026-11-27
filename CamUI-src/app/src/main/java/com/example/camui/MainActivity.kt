package com.example.camui

import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.StrictMode
import android.provider.MediaStore
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
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay

class MainActivity : AppCompatActivity() {
    private var frameMetricsDiagnostics: CameraFrameMetricsDiagnostics? = null

    override fun onDestroy() {
        frameMetricsDiagnostics?.close()
        frameMetricsDiagnostics = null
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        AppLanguageManager.refreshSystemLanguage(this)
        android.util.Log.i("FUJI_SCREEN_PROFILE", ScreenProfile.read(this).diagnosticLine())
        hideStatusBar()
    }


    /**
     * Existing installs are upgraded in place and must bypass the introduction.
     * A genuinely new package install shows it once; the completion flag then
     * lives in the app's private settings alongside the camera preferences.
     */
    private fun shouldShowFirstActivation(preferences: android.content.SharedPreferences): Boolean {
        if (preferences.contains("first_activation_completed")) {
            return !preferences.getBoolean("first_activation_completed", false)
        }
        val isFreshInstall = runCatching {
            val info = packageManager.getPackageInfo(packageName, 0)
            info.firstInstallTime >= info.lastUpdateTime - 5_000L
        }.getOrDefault(false)
        if (!isFreshInstall) {
            preferences.edit().putBoolean("first_activation_completed", true).apply()
            return false
        }
        return true
    }

    private fun mostRecentPhotoUri(): Uri? {
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? AND ${MediaStore.Images.Media.MIME_TYPE} = ?"
        val selectionArgs = arrayOf("Pictures/FujifilmMode/%", "image/jpeg")
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        return runCatching {
            contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                if (!cursor.moveToFirst()) null else {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                    ContentUris.withAppendedId(collection, id)
                }
            }
        }.getOrNull()
    }

    /**
     * Open one concrete image in the device photo viewer. CATEGORY_APP_GALLERY launches
     * the gallery home/feed, which is not the requested photo-view destination.
     */
    private fun openDeviceGallery(latestPhoto: Uri?): Boolean {
        val photoUri = latestPhoto ?: mostRecentPhotoUri()
        if (photoUri != null) {
            CrashContextDiagnostics.recordUiEvent("ALBUM_OPEN source=native-gallery image=latest")
            val imageIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(photoUri, "image/jpeg")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                startActivity(imageIntent)
                return true
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) {
            }
        }

        // With no photo, open the device's Gallery application rather than an in-app album.
        CrashContextDiagnostics.recordUiEvent("ALBUM_OPEN source=native-gallery image=none")
        val galleryIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_GALLERY)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(galleryIntent)
            return true
        } catch (_: ActivityNotFoundException) {
        } catch (_: SecurityException) {
        }

        // Some devices expose the gallery only through the media picker contract.
        val pickerIntent = Intent(Intent.ACTION_PICK).apply {
            setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            startActivity(pickerIntent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    /** Request the highest refresh mode advertised for the display's current resolution.
     * The system/HyperOS may still select a lower LTPO rate when appropriate.
     */
    @Suppress("DEPRECATION")
    private fun requestHighestSupportedRefreshRate() {
        runCatching {
            val activeDisplay = windowManager.defaultDisplay
            val currentMode = activeDisplay.mode
            val highestMode = activeDisplay.supportedModes
                .filter { it.physicalWidth == currentMode.physicalWidth && it.physicalHeight == currentMode.physicalHeight }
                .maxByOrNull { it.refreshRate }
                ?: return
            val params = window.attributes
            params.preferredDisplayModeId = highestMode.modeId
            params.preferredRefreshRate = highestMode.refreshRate
            window.attributes = params
            if (android.os.Build.VERSION.SDK_INT >= 35) {
                // Android 15+ high frame-rate category is a system preference, not a lock.
                window.decorView.setRequestedFrameRate(
                    android.view.View.REQUESTED_FRAME_RATE_CATEGORY_HIGH
                )
                window.decorView.invalidate()
            }
            getSharedPreferences("fujifilm_settings", MODE_PRIVATE).edit()
                .putFloat("preferred_refresh_rate", highestMode.refreshRate)
                .apply()
            android.util.Log.i("LUNADREAM_DISPLAY", "Requested ${highestMode.refreshRate}Hz at ${highestMode.physicalWidth}x${highestMode.physicalHeight}")
        }.onFailure {
            android.util.Log.w("LUNADREAM_DISPLAY", "Could not request maximum supported refresh mode; using system default", it)
        }
    }

    private fun hideStatusBar() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onResume() {
        super.onResume()
        hideStatusBar()
        requestHighestSupportedRefreshRate()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideStatusBar()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) {
            // Catch disk/network work and leaked framework objects during camera-screen iteration.
            // penaltyLog keeps this diagnostic-only; it never crashes the capture flow.
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build()
            )
            frameMetricsDiagnostics = CameraFrameMetricsDiagnostics.attach(window)
        }
        LegacyColorAdjustmentCleanup.migrateOnce(this)
        android.util.Log.i("FUJI_SCREEN_PROFILE", ScreenProfile.read(this).diagnosticLine())
        val settingsPrefs = getSharedPreferences("fujifilm_settings", MODE_PRIVATE)
        settingsPrefs.edit().putBoolean("first_activation_completed", true).apply()
        AppLanguageManager.setLanguage(this, "vi")
        // The camera UI and capture orientation are landscape-only.
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window.attributes = window.attributes.apply {
            rotationAnimation = WindowManager.LayoutParams.ROTATION_ANIMATION_SEAMLESS
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        hideStatusBar()
        requestHighestSupportedRefreshRate()
        setContent {
            // Explicitly expose the hosting Activity's registry to every nested ComposeView.
            // This prevents rememberLauncherForActivityResult from seeing a null owner when
            // the camera screen is hosted through an embedded ComposeView composition.
            CompositionLocalProvider(
                LocalActivityResultRegistryOwner provides this@MainActivity
            ) {
            CamTheme {
                var bootSplashVisible by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    // Keep the splash visible while the underlying camera composition starts.
                    delay(1_050L)
                    bootSplashVisible = false
                }
                var page by remember { mutableStateOf("camera") }
                LaunchedEffect(page) { CrashContextDiagnostics.updateRoute(page) }
                var settingsSubpageReturnTarget by remember { mutableStateOf("camera") }
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
                val reducedMotion = remember {
                    AnimaT.isSystemReducedMotion(this@MainActivity)
                }
                var showGrid by remember { mutableStateOf(settingsPrefs.getBoolean("viewfinder_grid_enabled", true)) }
                var showHist by remember { mutableStateOf(settingsPrefs.getBoolean("viewfinder_histogram_enabled", true)) }
                var focusPeaking by remember { mutableStateOf(settingsPrefs.getBoolean("viewfinder_focus_peaking_enabled", false)) }
                var ratioIdx by remember {
                    mutableIntStateOf(
                        if (settingsPrefs.getBoolean("capture_ratio_index_v20203", false)) {
                            settingsPrefs.getInt("capture_ratio_index", 0).coerceIn(0, 3)
                        } else {
                            val oldIndex = if (settingsPrefs.contains("capture_ratio_index")) {
                                settingsPrefs.getInt("capture_ratio_index", 0)
                            } else -1
                            val migrated = if (settingsPrefs.getBoolean("capture_ratio_index_v20202", false)) {
                                // Previous order: 9:16, 9:21, 3:4, 1:1.
                                when (oldIndex) {
                                    0 -> 0
                                    1 -> 3
                                    2 -> 1
                                    3 -> 2
                                    else -> 0
                                }
                            } else {
                                // Legacy five-choice order: 3:2, 4:3, 16:9, 1:1, 21:9.
                                when (oldIndex) {
                                    2 -> 0
                                    1 -> 1
                                    3 -> 2
                                    4 -> 3
                                    else -> 0
                                }
                            }
                            settingsPrefs.edit()
                                .putInt("capture_ratio_index", migrated)
                                .putBoolean("capture_ratio_index_v20202", true)
                                .putBoolean("capture_ratio_index_v20203", true)
                                .apply()
                            migrated
                        }
                    )
                }

                BackHandler(enabled = page != "camera" && page != "activation") {
                    page = when (page) {
                        "toolbox", "contents", "watermark_panel" -> settingsSubpageReturnTarget
                        else -> "camera"
                    }
                }
                Box(Modifier.fillMaxSize()) {
                    // Keep the camera composition and its CaptureSession alive beneath
                    // Settings, Toolbox and Contents. Closing these panels therefore does
                    // not dispose/rebind PreviewView or create a black preview interval.
                    if (page != "activation") {
                    CameraScreen(
                        onOpenSettings = { page = "settings" },
                        onOpenToolbox = { settingsSubpageReturnTarget = "camera"; page = "toolbox" },
                        onOpenContentManagement = { settingsSubpageReturnTarget = "camera"; page = "contents" },
                        onOpenAlbum = { latestPhoto ->
                            if (!openDeviceGallery(latestPhoto)) {
                                android.widget.Toast.makeText(
                                    this@MainActivity,
                                    getString(R.string.gallery_app_unavailable),
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        showGrid = showGrid,
                        onShowGrid = { showGrid = it; settingsPrefs.edit().putBoolean("viewfinder_grid_enabled", it).apply() },
                        showHist = showHist,
                        onShowHist = { showHist = it; settingsPrefs.edit().putBoolean("viewfinder_histogram_enabled", it).apply() },
                        focusPeaking = focusPeaking,
                        onFocusPeaking = { focusPeaking = it; settingsPrefs.edit().putBoolean("viewfinder_focus_peaking_enabled", it).apply() },
                        ratioIdx = ratioIdx,
                        onRatio = { ratioIdx = it; settingsPrefs.edit().putInt("capture_ratio_index", it).apply() },
                        performanceMode = animationPerformanceMode,
                        onOpenWatermarkPanel = { settingsSubpageReturnTarget = "camera"; page = "watermark_panel" }
                    )
                    }
                    AnimatedContent(
                        targetState = page,
                        modifier = Modifier.fillMaxSize().animaTPageRotation(
                            pageKey = page,
                            returningToCamera = page == "camera",
                            performanceMode = animationPerformanceMode,
                            reducedMotion = reducedMotion
                        ),
                        transitionSpec = {
                            AnimaT.enter(animationPerformanceMode, reducedMotion) togetherWith
                                AnimaT.exit(animationPerformanceMode, reducedMotion)
                        },
                        label = "FujiFilm Mode page transition"
                    ) { targetPage ->
                        when (targetPage) {
                            "activation" -> ActivationScreen(
                                onComplete = {
                                    settingsPrefs.edit().putBoolean("first_activation_completed", true).apply()
                                    page = "camera"
                                }
                            )
                            "camera" -> Spacer(Modifier.fillMaxSize())
                            "settings" -> Box(
                                Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                SettingsScreen(
                                onBack = { page = "camera" },
                                onOpenToolbox = { settingsSubpageReturnTarget = "settings"; page = "toolbox" },
                                onOpenContentsManagement = { settingsSubpageReturnTarget = "settings"; page = "contents" },
                                ratioIdx = ratioIdx,
                                onRatio = {
                                    ratioIdx = it.coerceIn(0, 3)
                                    settingsPrefs.edit()
                                        .putInt("capture_ratio_index", ratioIdx)
                                        .putBoolean("capture_ratio_index_v20202", true)
                                        .apply()
                                },
                                showGrid = showGrid,
                                onShowGrid = { showGrid = it; settingsPrefs.edit().putBoolean("viewfinder_grid_enabled", it).apply() },
                                showHist = showHist,
                                onShowHist = { showHist = it; settingsPrefs.edit().putBoolean("viewfinder_histogram_enabled", it).apply() },
                                focusPeaking = focusPeaking,
                                onFocusPeaking = { focusPeaking = it; settingsPrefs.edit().putBoolean("viewfinder_focus_peaking_enabled", it).apply() },
                                onReplayActivation = { },
                                onOpenWatermarkPanel = {
                                    settingsSubpageReturnTarget = "settings"
                                    page = "watermark_panel"
                                },
                                modifier = Modifier.fillMaxWidth(0.66f).fillMaxHeight(0.78f)
                            )
                            }
                            "toolbox" -> ToolboxScreen(
                                onBack = { page = settingsSubpageReturnTarget },
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
                                },
                            )
                            "watermark_panel" -> WatermarkPanelScreen(onBack = { page = settingsSubpageReturnTarget })
                            "contents" -> ContentManagementScreen(onBack = { page = settingsSubpageReturnTarget })
                            else -> Spacer(Modifier.fillMaxSize())
                        }
                    }
                    if (bootSplashVisible) FujifilmBootSplash()
                }
            }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun FujifilmBootSplash() {
    Box(
        Modifier.fillMaxSize().background(ComposeColor.White),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.fujifilm_logo),
            contentDescription = "FUJIFILM",
            modifier = Modifier.width(260.dp).height(58.dp),
            contentScale = ContentScale.Fit
        )
    }
}
