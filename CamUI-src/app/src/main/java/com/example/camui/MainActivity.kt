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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
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
        val builtInFilterCount = builtInCameraLookPresetDefinitions.size
        val builtInFilterCatalogValid = builtInCameraLookCatalogIsValid()
        android.util.Log.i(
            "FUJI_FILTER",
            "BUILT_IN_FILTER_CATALOG count=$builtInFilterCount expected=46 valid=$builtInFilterCatalogValid"
        )
        CrashContextDiagnostics.recordUiEvent(
            "BUILT_IN_FILTER_CATALOG count=$builtInFilterCount expected=46 valid=$builtInFilterCatalogValid"
        )
        if (!builtInFilterCatalogValid) {
            android.util.Log.e("FUJI_FILTER", "Built-in film catalog failed the 46-entry integrity check")
        }
        // Validate the real LUT loader away from the UI thread. A catalog definition
        // alone is not counted as loaded unless its LUT can actually be produced.
        Thread({
            var loadedCount = 0
            val failedIds = ArrayList<String>()
            builtInCameraLookPresetDefinitions.forEach { preset ->
                val lut = runCatching {
                    readCameraLookLut(applicationContext, preset.id)
                }.onFailure {
                    android.util.Log.e("FUJI_FILTER", "Built-in LUT load failed id=" + preset.id, it)
                }.getOrNull()
                if (lut != null) loadedCount++ else failedIds += preset.id
            }
            val report = "BUILT_IN_FILTERS_LOADED count=" + loadedCount +
                " expected=46 failures=" + failedIds.joinToString(",")
            if (loadedCount == 46 && failedIds.isEmpty()) {
                android.util.Log.i("FUJI_FILTER", report)
            } else {
                android.util.Log.e("FUJI_FILTER", report)
            }
            CrashContextDiagnostics.recordUiEvent(report)
        }, "FujiFilterCatalogValidation").apply {
            isDaemon = true
            start()
        }
        android.util.Log.i("FUJI_SCREEN_PROFILE", ScreenProfile.read(this).diagnosticLine())
        val settingsPrefs = getSharedPreferences("fujifilm_settings", MODE_PRIVATE)
        // 3.2.0 resets accidental legacy mode migration before Compose reads camera state.
        if (!settingsPrefs.getBoolean("fuji_320_defaults_migrated", false)) {
            settingsPrefs.edit()
                .putString("lunadream_mode", "off")
                .remove("hdmp_render_profile")
                .remove("hdmp_output_mp")
                .putBoolean("watermark_enabled", false)
                .putString("selected_filter", "None")
                .putBoolean("fuji_320_defaults_migrated", true)
                .putBoolean("fuji_320_preview_filter_selftest_pending", true)
                .apply()
            android.util.Log.i("FUJI_320_MIGRATION", "LUNADREAM=off WATERMARK=off FILTER=None")
        } else {
            if (!settingsPrefs.contains("lunadream_mode")) {
                settingsPrefs.edit().putString("lunadream_mode", "off").apply()
            }
            if (!settingsPrefs.contains("watermark_enabled")) {
                settingsPrefs.edit().putBoolean("watermark_enabled", false).apply()
            }
        }
        settingsPrefs.edit().putBoolean("first_activation_completed", true).apply()
        AppLanguageManager.setLanguage(this, "vi")
        // Allow portrait, landscape and both reverse orientations; configChanges keeps Activity/camera alive.
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
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
                val bootSplashAlpha = remember { Animatable(1f) }
                LaunchedEffect(Unit) {
                    // Keep the camera binding alive behind the exact white Fujifilm boot frame.
                    delay(1_000L)
                    bootSplashAlpha.animateTo(0f, tween(260, easing = FastOutSlowInEasing))
                    bootSplashVisible = false
                }
                var page by remember { mutableStateOf("camera") }
                val pageTransitionScope = rememberCoroutineScope()
                var pageCurtainMessage by remember { mutableStateOf("") }
                val pageCurtainAlpha = remember { Animatable(0f) }
                var pageCurtainJob by remember { mutableStateOf<Job?>(null) }
                fun openPageThroughCurtain(targetPage: String, message: String) {
                    pageCurtainJob?.cancel()
                    pageCurtainJob = pageTransitionScope.launch {
                        pageCurtainMessage = message
                        pageCurtainAlpha.snapTo(0f)
                        pageCurtainAlpha.animateTo(1f, tween(250, easing = FastOutSlowInEasing))
                        page = targetPage
                        // Keep the black screen visible at least 600 ms after the target page is requested.
                        delay(600L)
                        pageCurtainAlpha.animateTo(0f, tween(250, easing = FastOutSlowInEasing))
                        pageCurtainMessage = ""
                        pageCurtainJob = null
                    }
                }
                // Keep CameraScreen and its PreviewView alive; settings pages are a separate overlay.
                var overlayPage by remember { mutableStateOf("settings") }
                var overlayVisible by remember { mutableStateOf(false) }
                val overlayAlpha = remember { Animatable(0f) }
                val overlayScale = remember { Animatable(0.985f) }
                val overlayOffsetY = remember { Animatable(0f) }
                val overlayOffsetX = remember { Animatable(0f) }
                val overlayDimAlpha = remember { Animatable(0f) }
                val density = LocalDensity.current
                val overlayEnterOffsetY = with(density) { 8.dp.toPx() }
                val overlayTabOffsetX = with(density) { 12.dp.toPx() }

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
                LaunchedEffect(page, animationPerformanceMode, reducedMotion) {
                    if (page == "camera") {
                        if (overlayVisible) {
                            val duration = if (reducedMotion) 0 else if (animationPerformanceMode) 70 else Tuning.SETTINGS_EXIT_MS
                            coroutineScope {
                                launch { overlayAlpha.animateTo(0f, tween(duration, easing = FastOutSlowInEasing)) }
                                launch { overlayScale.animateTo(0.995f, tween(duration, easing = FastOutSlowInEasing)) }
                                launch { overlayOffsetY.animateTo(overlayEnterOffsetY, tween(duration, easing = FastOutSlowInEasing)) }
                                launch { overlayDimAlpha.animateTo(0f, tween(duration, easing = FastOutSlowInEasing)) }
                            }
                            overlayVisible = false
                        }
                    } else if (!overlayVisible) {
                        overlayPage = page
                        overlayVisible = true
                        overlayAlpha.snapTo(0f)
                        overlayScale.snapTo(0.985f)
                        overlayOffsetY.snapTo(overlayEnterOffsetY)
                        overlayOffsetX.snapTo(0f)
                        overlayDimAlpha.snapTo(0f)
                        val duration = if (reducedMotion) 0 else if (animationPerformanceMode) 90 else Tuning.SETTINGS_ENTER_MS
                        coroutineScope {
                            launch { overlayAlpha.animateTo(1f, tween(duration, easing = FastOutSlowInEasing)) }
                            launch { overlayScale.animateTo(1f, tween(duration, easing = FastOutSlowInEasing)) }
                            launch { overlayOffsetY.animateTo(0f, tween(duration, easing = FastOutSlowInEasing)) }
                            launch { overlayDimAlpha.animateTo(0.85f, tween(duration, easing = FastOutSlowInEasing)) }
                        }
                    } else if (overlayPage != page) {
                        val exitDuration = if (reducedMotion) 0 else if (animationPerformanceMode) 70 else 100
                        overlayAlpha.animateTo(0f, tween(exitDuration, easing = FastOutSlowInEasing))
                        overlayPage = page
                        overlayOffsetX.snapTo(overlayTabOffsetX)
                        val enterDuration = if (reducedMotion) 0 else if (animationPerformanceMode) 90 else Tuning.SETTINGS_TAB_MS
                        coroutineScope {
                            launch { overlayAlpha.animateTo(1f, tween(enterDuration, easing = FastOutSlowInEasing)) }
                            launch { overlayOffsetX.animateTo(0f, tween(enterDuration, easing = FastOutSlowInEasing)) }
                        }
                    }
                    CrashContextDiagnostics.updateRoute(page)
                }
                var showGrid by remember { mutableStateOf(settingsPrefs.getBoolean("viewfinder_grid_enabled", true)) }
                var showHist by remember { mutableStateOf(settingsPrefs.getBoolean("viewfinder_histogram_enabled", true)) }
                var focusPeaking by remember { mutableStateOf(settingsPrefs.getBoolean("viewfinder_focus_peaking_enabled", false)) }
                var ratioIdx by remember { mutableIntStateOf(1) }
                LaunchedEffect(Unit) {
                    settingsPrefs.edit()
                        .putInt("capture_ratio_index", 1)
                        .putBoolean("capture_ratio_index_v20202", true)
                        .putBoolean("capture_ratio_index_v20203", true)
                        .apply()
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
                        onRatio = { ratioIdx = 1; settingsPrefs.edit().putInt("capture_ratio_index", 1).apply() },
                        performanceMode = animationPerformanceMode,
                        onOpenWatermarkPanel = { settingsSubpageReturnTarget = "camera"; page = "watermark_panel" },
                        cameraScreenVisible = page == "camera"
                    )
                    }
                    if (overlayVisible) {
                        // The dim layer is independent from the panel; it never crossfades a full-screen camera.
                        Box(
                            Modifier.fillMaxSize()
                                .graphicsLayer { alpha = overlayDimAlpha.value }
                                .background(ComposeColor.Black)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { /* Consume taps outside the panel; never pass them to the camera preview. */ }
                        )
                    }
                    // Keep the last Settings composition alive while hidden. The first Settings
                    // open is an alpha/scale/translation animation rather than a cold menu build.
                    // Translate a hidden layer outside the LCD so it cannot intercept camera taps.
                    Box(
                        Modifier.fillMaxSize().graphicsLayer {
                            alpha = overlayAlpha.value
                            scaleX = overlayScale.value
                            scaleY = overlayScale.value
                            translationY = overlayOffsetY.value
                            translationX = if (overlayVisible) overlayOffsetX.value else size.width * 2f
                        }
                    ) {
                            when (overlayPage) {
                                "activation" -> ActivationScreen(
                                    onComplete = {
                                        settingsPrefs.edit().putBoolean("first_activation_completed", true).apply()
                                        page = "camera"
                                    }
                                )
                                "settings" -> androidx.compose.foundation.layout.BoxWithConstraints(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    // Landscape LCD bounds from the supplied 924×427 reference:
                                    // x=6–57%, y=16–92.5%. Keep the live camera rendered behind it.
                                    val settingsPanelModifier = Modifier.fillMaxSize().padding(12.dp)
                                    SettingsScreen(
                                        onBack = { page = "camera" },
                                        onOpenToolbox = { settingsSubpageReturnTarget = "settings"; page = "toolbox" },
                                        onOpenContentsManagement = { settingsSubpageReturnTarget = "settings"; page = "contents" },
                                        ratioIdx = ratioIdx,
                                        onRatio = {
                                            ratioIdx = 1
                                            settingsPrefs.edit()
                                                .putInt("capture_ratio_index", 1)
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
                                        modifier = settingsPanelModifier
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
                    if (bootSplashVisible) {
                        Box(Modifier.fillMaxSize().graphicsLayer { alpha = bootSplashAlpha.value }) {
                            FujifilmBootSplash()
                        }
                    }
                    if (pageCurtainMessage.isNotEmpty()) {
                        Box(
                            Modifier.fillMaxSize()
                                .graphicsLayer { alpha = pageCurtainAlpha.value }
                                .background(ComposeColor.Black)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                pageCurtainMessage,
                                color = ComposeColor.White,
                                fontSize = 13.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
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
            painter = painterResource(R.drawable.fujifilm_wordmark),
            contentDescription = "FUJIFILM — màn khởi động",
            modifier = Modifier.fillMaxWidth(0.34f).height(72.dp),
            contentScale = ContentScale.Fit
        )
    }
}
