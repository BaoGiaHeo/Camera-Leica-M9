package com.example.camui

import android.Manifest
import android.content.ContentValues
import android.content.ContentUris
import android.content.Context
import android.graphics.Point
import android.graphics.ImageFormat
import android.media.MediaActionSound
import android.media.MediaPlayer
import android.os.Build
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.RggbChannelVector
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Rational
import android.util.Size
import android.view.Surface
import android.view.OrientationEventListener
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as DrawSize
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun currentDisplayIsLandscape(context: Context): Boolean {
    val display = context.display
    if (display != null) {
        val size = Point()
        val hasDisplayBounds = runCatching {
            display.getRealSize(size)
            size.x > 0 && size.y > 0
        }.getOrDefault(false)
        if (hasDisplayBounds) return size.x > size.y
    }
    return context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
}

/**
 * Convert the physical device attitude reported by OrientationEventListener into the
 * corresponding CameraX target rotation. Do not rely on Display.rotation here: an app can
 * remain portrait-locked while the handset itself is physically landscape.
 */
private fun surfaceRotationFromDeviceOrientation(orientation: Int): Int {
    if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) return Surface.ROTATION_0
    return when (orientation) {
        in 45..134 -> Surface.ROTATION_270
        in 135..224 -> Surface.ROTATION_180
        in 225..314 -> Surface.ROTATION_90
        else -> Surface.ROTATION_0
    }
}

private fun isDeviceOrientationLandscape(orientation: Int): Boolean =
    orientation in 45..134 || orientation in 225..314

private val meteringModes = listOf("Center", "Matrix", "Spot")
private val wbModes = listOf(
    "Auto" to CaptureRequest.CONTROL_AWB_MODE_AUTO,
    "Daylight" to CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT,
    "Cloudy" to CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT,
    "Tungsten" to CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT,
    "Fluorescent" to CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT
)

private fun playConfiguredShutterSound(
    prefs: android.content.SharedPreferences,
    systemPlayer: MediaActionSound
) {
    val selected = prefs.getString("shutter_sound_selected", "system")
    val filePath = prefs.getString("shutter_sound_file_path", null)
    if (selected == "local" && !filePath.isNullOrBlank() && File(filePath).isFile) {
        val player = MediaPlayer()
        try {
            player.setDataSource(filePath)
            player.setOnPreparedListener { prepared -> prepared.start() }
            player.setOnCompletionListener { completed -> completed.release() }
            player.setOnErrorListener { failed, _, _ ->
                failed.release()
                true
            }
            player.prepareAsync()
            return
        } catch (failure: Throwable) {
            runCatching { player.release() }
            android.util.Log.w("FUJI_SHUTTER_SOUND", "Custom MP3 failed; using system shutter sound", failure)
        }
    }
    runCatching { systemPlayer.play(MediaActionSound.SHUTTER_CLICK) }
}

@Composable
fun CameraScreen(
    onOpenSettings: () -> Unit,
    onOpenToolbox: () -> Unit,
    onOpenContentManagement: () -> Unit,
    onOpenAlbum: () -> Unit,
    showGrid: Boolean,
    onShowGrid: (Boolean) -> Unit,
    showHist: Boolean,
    onShowHist: (Boolean) -> Unit,
    focusPeaking: Boolean,
    onFocusPeaking: (Boolean) -> Unit,
    ratioIdx: Int,
    onRatio: (Int) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    var targetRotation by remember {
        mutableIntStateOf(context.display?.rotation ?: Surface.ROTATION_0)
    }
    var cameraBindGeneration by remember { mutableIntStateOf(0) }
    // Read the current display bounds instead of relying only on Configuration.orientation.
    // Some vendor builds keep the activity's cached configuration in portrait while the display
    // has already rotated; using that stale value makes a landscape JPEG get normalized as portrait.
    var isLandscape by remember { mutableStateOf(currentDisplayIsLandscape(context)) }
    // Remember exactly which geometry is active in the bound CameraX UseCaseGroup.
    var boundCameraRotation by remember { mutableIntStateOf(targetRotation) }
    var boundCameraLandscape by remember { mutableStateOf(isLandscape) }
    var boundCameraRatioIdx by remember { mutableIntStateOf(ratioIdx) }
    // Physical device orientation is authoritative for capture geometry. Display.rotation can
    // stay ROTATION_0 while an app/activity is held in portrait, even when the handset is landscape.
    val orientationListener = remember(context) {
        object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                val sensorRotation = surfaceRotationFromDeviceOrientation(orientation)
                val sensorLandscape = isDeviceOrientationLandscape(orientation)
                if (sensorRotation != targetRotation) targetRotation = sensorRotation
                if (sensorLandscape != isLandscape) isLandscape = sensorLandscape
            }
        }
    }
    DisposableEffect(orientationListener) {
        if (orientationListener.canDetectOrientation()) orientationListener.enable()
        onDispose { orientationListener.disable() }
    }
    val focusPeakingEnabled = rememberUpdatedState(focusPeaking)

    var hasCam by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasCam = it }
    LaunchedEffect(Unit) {
        if (!hasCam) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val appPrefs = remember {
        context.getSharedPreferences("fujifilm_settings", 0).also { prefs ->
            if (!prefs.getBoolean("shutter_effect_v2_migrated", false)) {
                prefs.edit().putString("shutter_effect", "Film Wash")
                    .putBoolean("shutter_effect_v2_migrated", true).apply()
            }
        }
    }
    var mode by remember { mutableStateOf(appPrefs.getString("camera_mode", "Photo") ?: "Photo") }
    var front by remember { mutableStateOf(appPrefs.getBoolean("camera_front_facing", false)) }
    var flash by remember { mutableIntStateOf(appPrefs.getInt("camera_flash_mode", ImageCapture.FLASH_MODE_OFF)) }
    var timerSec by remember { mutableIntStateOf(appPrefs.getInt("camera_timer_seconds", 0).coerceIn(0, 10)) }
    var countdown by remember { mutableIntStateOf(0) }
    var showQuick by remember { mutableStateOf(false) }
    var zoomSel by remember { mutableFloatStateOf(appPrefs.getFloat("camera_zoom_ratio", 1f)) }
    var zoomAnimationJob by remember { mutableStateOf<Job?>(null) }
    var lensTransitionVisible by remember { mutableStateOf(false) }
    var lensTransitionLabel by remember { mutableStateOf("") }
    val lensTransitionPulse = remember { Animatable(0f) }
    var metering by remember { mutableIntStateOf(appPrefs.getInt("camera_metering_index", 0).coerceIn(0, 3)) }
    // Ultra HDR is hidden from the UI. Keep the legacy camera path but start disabled.
    var ultraHdr by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appPrefs.edit().putBoolean("ultra_hdr_enabled", false).apply() }
    var stabilization by remember { mutableStateOf(appPrefs.getBoolean("camera_stabilization_enabled", false)) }
    var renderEngine by remember { mutableStateOf(appPrefs.getString("camera_render_engine", "Performance") ?: "Performance") }
    var proSel by remember { mutableStateOf(appPrefs.getString("pro_selected_control", null)) }
    var focusLocked by remember { mutableStateOf(false) }
    var manualIsoEnabled by remember { mutableStateOf(appPrefs.getBoolean("pro_manual_iso_enabled", false)) }
    var manualShutterEnabled by remember { mutableStateOf(appPrefs.getBoolean("pro_manual_shutter_enabled", false)) }
    var manualColorTempEnabled by remember { mutableStateOf(appPrefs.getBoolean("pro_manual_wb_enabled", false)) }
    var manualColorTemp by remember { mutableIntStateOf(appPrefs.getInt("pro_manual_wb_kelvin", 5200).coerceIn(2000, 10000)) }
    var manualCaptureRevision by remember { mutableIntStateOf(0) }
    var shutterButtonBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var filterDialogVisible by remember { mutableStateOf(false) }
    var activeFilter by remember { mutableStateOf(context.getSharedPreferences("fujifilm_settings", 0).getString("selected_filter", "None") ?: "None") }
    var activeLutUri by remember { mutableStateOf(context.getSharedPreferences("fujifilm_settings", 0).getString("selected_lut_uri", "") ?: "") }
    var shutterEffectAlpha by remember { mutableFloatStateOf(0f) }
    val filmWashProgress = remember { Animatable(0f) }
    var isProcessing by remember { mutableStateOf(false) }
    var processingProgress by remember { mutableIntStateOf(0) }
    var noiseReductionProgress by remember { mutableIntStateOf(0) }
    var processingLabel by remember { mutableStateOf("") }
    var processingEtaSeconds by remember { mutableIntStateOf(-1) }
    val shutterSoundPlayer = remember {
        MediaActionSound().apply { load(MediaActionSound.SHUTTER_CLICK) }
    }
    DisposableEffect(shutterSoundPlayer) {
        onDispose { shutterSoundPlayer.release() }
    }
    var evIndex by remember { mutableIntStateOf(appPrefs.getInt("pro_ev_index", 0)) }
    // Keep the EV thumb independent from delayed per-frame Camera2 results while dragging.
    var evSliderPosition by remember { mutableFloatStateOf(evIndex.toFloat()) }
    var evUserAdjusting by remember { mutableStateOf(false) }
    var iso by remember { mutableIntStateOf(0) }
    var shutterNs by remember { mutableLongStateOf(0L) }
    var focusDiopter by remember { mutableFloatStateOf(0f) }
    var camera2Inventory by remember { mutableStateOf(emptyList<CamDetails>()) }
    var activeCameraDetails by remember { mutableStateOf<CamDetails?>(null) }
    var activePhysicalCameraDetails by remember { mutableStateOf(emptyList<CamDetails>()) }
    var manualExposure by remember { mutableStateOf(appPrefs.getBoolean("pro_manual_exposure_enabled", false)) }
    var manualIso by remember { mutableIntStateOf(appPrefs.getInt("pro_manual_iso", 400).coerceAtLeast(50)) }
    var manualShutterNs by remember { mutableLongStateOf(appPrefs.getLong("pro_manual_shutter_ns", 10_000_000L).coerceAtLeast(1_000_000L)) }
    var manualFocus by remember { mutableFloatStateOf(appPrefs.getFloat("pro_manual_focus", 0f).coerceAtLeast(0f)) }
    var hist by remember { mutableStateOf(IntArray(64)) }
    var peakPoints by remember { mutableStateOf(emptyList<Offset>()) }
    var lastUri by remember { mutableStateOf<Uri?>(null) }
    var thumb by remember { mutableStateOf<ImageBitmap?>(null) }
    var viewerBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var photoViewerVisible by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var captureSetupReady by remember { mutableStateOf(false) }
    var previewUseCase by remember { mutableStateOf<Preview?>(null) }
    var analysisUseCase by remember { mutableStateOf<ImageAnalysis?>(null) }
    val imageQuality = appPrefs.getString("image_quality", "Best") ?: "Best"
    val noiseReduction = appPrefs.getString("noise_reduction", "Medium") ?: "Medium"
    val rawCaptureMode = appPrefs.getString("raw_capture_mode", "jpeg") ?: "jpeg"
    var uhdCaptureEnabled by remember { mutableStateOf(appPrefs.getBoolean("uhd_capture_enabled", false)) }
    LaunchedEffect(uhdCaptureEnabled) {
        appPrefs.edit().putBoolean("uhd_capture_enabled", uhdCaptureEnabled).apply()
    }
    var hdrEnabled by remember {
        mutableStateOf(appPrefs.getBoolean("hdr_enabled", appPrefs.getBoolean("hdr_coming_soon", false)))
    }
    var preserveUltraHdrOutput by remember { mutableStateOf(false) }
    var softwareHdrFallback by remember { mutableStateOf(false) }
    var nightShotMode by remember {
        mutableStateOf(appPrefs.getString("nightshot_mode", "off") ?: "off")
    }
    var nightShotFrameCount by remember {
        mutableIntStateOf(appPrefs.getInt("nightshot_frame_count", 6).coerceIn(4, 12))
    }
    var starTrailExposureSeconds by remember {
        mutableIntStateOf(appPrefs.getInt("star_trails_exposure_seconds", 4).coerceIn(1, 8))
    }
    var nightExtensionActive by remember { mutableStateOf(false) }
    var nightShotCaptureInProgress by remember { mutableStateOf(false) }
    var starTrailCaptureInProgress by remember { mutableStateOf(false) }
    var starTrailStopRequested by remember { mutableStateOf(false) }
    var starTrailFrameCount by remember { mutableIntStateOf(0) }
    var starTrailStartedAtMillis by remember { mutableLongStateOf(0L) }
    var starTrailElapsedSeconds by remember { mutableIntStateOf(0) }
    var starTrailTimerJob by remember { mutableStateOf<Job?>(null) }
    var starTrailExposureStatus by remember { mutableStateOf("Đang kiểm tra hỗ trợ phơi sáng") }
    var lowLightDetected by remember { mutableStateOf(false) }
    var aiCompositionEnabled by remember {
        mutableStateOf(appPrefs.getBoolean("ai_composition_enabled", false))
    }
    var aiCompositionHint by remember { mutableStateOf("Chưa thấy khuôn mặt") }
    val faceDetector: FaceDetector? = remember(aiCompositionEnabled) {
        if (aiCompositionEnabled) {
            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .enableTracking()
                .build()
            FaceDetection.getClient(options)
        } else null
    }
    DisposableEffect(faceDetector) {
        onDispose { faceDetector?.close() }
    }
    LaunchedEffect(
        mode, front, flash, timerSec, ultraHdr, stabilization, renderEngine, metering,
        nightShotMode, nightShotFrameCount, starTrailExposureSeconds, rawCaptureMode,
        manualExposure, manualIsoEnabled, manualShutterEnabled, manualColorTempEnabled,
        manualColorTemp, manualIso, manualShutterNs, manualFocus, proSel, evIndex
    ) {
        appPrefs.edit()
            .putString("camera_mode", mode)
            .putBoolean("camera_front_facing", front)
            .putInt("camera_flash_mode", flash)
            .putInt("camera_timer_seconds", timerSec)
            .putBoolean("ultra_hdr_enabled", ultraHdr)
            .putBoolean("camera_stabilization_enabled", stabilization)
            .putString("camera_render_engine", renderEngine)
            .putInt("camera_metering_index", metering)
            .putString("nightshot_mode", nightShotMode)
            .putInt("nightshot_frame_count", nightShotFrameCount)
            .putInt("star_trails_exposure_seconds", starTrailExposureSeconds)
            .putBoolean("pro_manual_exposure_enabled", manualExposure)
            .putBoolean("pro_manual_iso_enabled", manualIsoEnabled)
            .putBoolean("pro_manual_shutter_enabled", manualShutterEnabled)
            .putBoolean("pro_manual_wb_enabled", manualColorTempEnabled)
            .putInt("pro_manual_wb_kelvin", manualColorTemp)
            .putInt("pro_manual_iso", manualIso)
            .putLong("pro_manual_shutter_ns", manualShutterNs)
            .putFloat("pro_manual_focus", manualFocus)
            .putString("pro_selected_control", proSel)
            .putInt("pro_ev_index", evIndex)
            .apply()
    }
    // NightShot must use normal auto exposure: lock EV at zero and do not expose
    // the regular EV slider while either NightShot capture mode is selected.
    LaunchedEffect(nightShotMode, mode, camera) {
        if (mode == "Photo" && nightShotMode != "off") {
            evUserAdjusting = false
            evIndex = 0
            evSliderPosition = 0f
            camera?.let { active ->
                runCatching {
                    Camera2CameraControl.from(active.cameraControl).clearCaptureRequestOptions()
                    active.cameraControl.setExposureCompensationIndex(0)
                }
            }
        }
    }
    LaunchedEffect(zoomSel) {
        delay(250)
        appPrefs.edit().putFloat("camera_zoom_ratio", zoomSel).apply()
    }
    val shutterButtonImageUri = appPrefs.getString("shutter_button_image_uri", null)
    LaunchedEffect(shutterButtonImageUri) {
        shutterButtonBitmap = if (shutterButtonImageUri.isNullOrBlank()) null else withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.loadThumbnail(Uri.parse(shutterButtonImageUri), Size(180, 180), null).asImageBitmap()
            }.getOrNull()
        }
    }

    // Recreate PreviewView when renderer changes; implementationMode is chosen before surface creation.
    val previewView = remember(context, renderEngine) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = if (renderEngine == "Performance") {
                PreviewView.ImplementationMode.PERFORMANCE
            } else {
                PreviewView.ImplementationMode.COMPATIBLE
            }
        }
    }
    LaunchedEffect(previewView, nightShotCaptureInProgress, starTrailCaptureInProgress, isProcessing) {
        previewView.keepScreenOn =
            nightShotCaptureInProgress || starTrailCaptureInProgress || isProcessing
    }

    // Preview and JPEG share one CameraX ViewPort. A generation guard prevents delayed
    // ProcessCameraProvider callbacks from applying geometry from an earlier rotation.
    LaunchedEffect(
        hasCam, front, ratioIdx, ultraHdr, isLandscape, targetRotation, renderEngine,
        manualCaptureRevision, uhdCaptureEnabled, hdrEnabled, nightShotMode,
        starTrailExposureSeconds, aiCompositionEnabled, mode
    ) {
        if (!hasCam) return@LaunchedEffect
        val discoveredCamera2 = if (camera2Inventory.isNotEmpty()) {
            camera2Inventory
        } else {
            withContext(Dispatchers.IO) {
                runCatching { Camera2Info.listCameras(context) }.getOrDefault(emptyList())
            }.also { camera2Inventory = it }
        }
        if (discoveredCamera2.isNotEmpty()) {
            android.util.Log.i(
                "FUJI_CAMERA2",
                "Discovered ${discoveredCamera2.size} logical/physical camera entries"
            )
        }
        val bindGeneration = ++cameraBindGeneration
        // Disable shutter immediately while the old UseCaseGroup is being replaced.
        camera = null
        imageCapture = null
        activeCameraDetails = null
        activePhysicalCameraDetails = emptyList()
        nightExtensionActive = false
        captureSetupReady = false
        preserveUltraHdrOutput = false
        previewUseCase = null
        analysisUseCase = null
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(providerListener@{
            if (bindGeneration != cameraBindGeneration) return@providerListener
            val provider = runCatching { future.get() }.getOrNull() ?: return@providerListener
            if (bindGeneration != cameraBindGeneration) return@providerListener
            val extensionsFuture = ExtensionsManager.getInstanceAsync(context, provider)
            extensionsFuture.addListener(extensionListener@{
            if (bindGeneration != cameraBindGeneration) return@extensionListener
            val extensionsManager = runCatching { extensionsFuture.get() }.getOrNull()
            val baseSelector = if (front) CameraSelector.DEFAULT_FRONT_CAMERA
                else CameraSelector.DEFAULT_BACK_CAMERA
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val activeFacingId = runCatching {
                baseSelector.filter(provider.availableCameraInfos)
                    .firstOrNull()
                    ?.let { Camera2CameraInfo.from(it).cameraId }
            }.getOrNull() ?: runCatching {
                cameraManager.cameraIdList.firstOrNull { id ->
                    cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) ==
                        if (front) CameraCharacteristics.LENS_FACING_FRONT else CameraCharacteristics.LENS_FACING_BACK
                }
            }.getOrNull()
            val selectedCameraInfo = provider.availableCameraInfos.firstOrNull { info ->
                runCatching { Camera2CameraInfo.from(info).cameraId == activeFacingId }.getOrDefault(false)
            }
            val rawSupportedFormats = selectedCameraInfo?.let { cameraInfo ->
                runCatching {
                    ImageCapture.getImageCaptureCapabilities(cameraInfo).supportedOutputFormats
                }.getOrDefault(emptySet())
            }.orEmpty()
            val rawCaptureRequested = mode == "Photo" && nightShotMode == "off" && rawCaptureMode != "jpeg"
            val requestedRawFormat = when (rawCaptureMode) {
                "raw" -> ImageCapture.OUTPUT_FORMAT_RAW
                "raw_plus" -> ImageCapture.OUTPUT_FORMAT_RAW_JPEG
                else -> ImageCapture.OUTPUT_FORMAT_JPEG
            }
            val rawCaptureFormat = if (
                rawCaptureRequested && requestedRawFormat in rawSupportedFormats
            ) requestedRawFormat else ImageCapture.OUTPUT_FORMAT_JPEG
            val rawCaptureActive = rawCaptureFormat == ImageCapture.OUTPUT_FORMAT_RAW ||
                rawCaptureFormat == ImageCapture.OUTPUT_FORMAT_RAW_JPEG
            if (rawCaptureRequested && !rawCaptureActive) {
                localizedToast(
                    context,
                    "Camera này không hỗ trợ RAW/RAW+. Đã chuyển về JPEG thường.",
                    Toast.LENGTH_LONG
                ).show()
            }
            val hardwareLevel = activeFacingId?.let { id ->
                runCatching {
                    cameraManager.getCameraCharacteristics(id)
                        .get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                }.getOrNull()
            }
            // HDR Extension is enabled only when the camera can also keep ImageAnalysis.
            // Limited/Legacy devices use the JPEG_R path instead so histogram/AI still work.
            val analysisCompatibleHardware = hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL ||
                hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3
            softwareHdrFallback = false
            // NightShot deliberately captures ordinary zero-EV auto-exposed frames and stacks them
            // in software. Do not enable the vendor NIGHT extension: some HALs turn it into a
            // single high-gain/long-shutter exposure, which is not the requested behaviour.
            nightExtensionActive = false
            val hdrCaptureRequested = hdrEnabled && mode == "Photo" &&
                nightShotMode == "off" && !rawCaptureActive &&
                !(manualExposure && (manualIsoEnabled || manualShutterEnabled))
            val hdrExtensionAvailable = hdrCaptureRequested && analysisCompatibleHardware && extensionsManager != null &&
                runCatching { extensionsManager.isExtensionAvailable(baseSelector, ExtensionMode.HDR) }
                    .getOrDefault(false)
            val hdrExtensionSelector = if (hdrExtensionAvailable) {
                runCatching {
                    extensionsManager!!.getExtensionEnabledCameraSelector(baseSelector, ExtensionMode.HDR)
                }.onFailure {
                    android.util.Log.w("FUJI_HDR", "HDR extension selector unavailable", it)
                }.getOrNull()
            } else null
            var hdrExtensionActive = hdrExtensionSelector != null
            var selector = hdrExtensionSelector ?: baseSelector
            if (hdrCaptureRequested && !hdrExtensionActive) {
                android.util.Log.i("FUJI_HDR", "Vendor HDR extension unavailable; checking Ultra HDR JPEG_R fallback")
            }
            provider.unbindAll()
            camera = null
            imageCapture = null

            val rotation = targetRotation
            val ratio = when (ratioIdx) {
                0 -> if (isLandscape) Rational(3, 2) else Rational(2, 3)
                1 -> if (isLandscape) Rational(4, 3) else Rational(3, 4)
                2 -> if (isLandscape) Rational(16, 9) else Rational(9, 16)
                3 -> Rational(1, 1)
                else -> if (isLandscape) Rational(21, 9) else Rational(9, 21)
            }
            // Updated after CameraX binds so live capture results use the actual logical camera,
            // not the first camera ID returned by CameraManager.
            var activeCameraCharacteristics: CameraCharacteristics? = null

            fun bindCapture(outputFormat: Int) {
                val previewBuilder = Preview.Builder().setTargetRotation(rotation)
                if (!hdrExtensionActive && !nightExtensionActive) Camera2Interop.Extender(previewBuilder).setSessionCaptureCallback(
                    object : CameraCaptureSession.CaptureCallback() {
                        override fun onCaptureCompleted(
                            session: CameraCaptureSession,
                            request: CaptureRequest,
                            result: TotalCaptureResult
                        ) {
                            val live = Camera2Info.readLive(result, activeCameraCharacteristics)
                            live.iso?.let { if (iso != it) iso = it }
                            live.shutterNs?.let { if (shutterNs != it) shutterNs = it }
                            live.focusDiopter?.let { if (focusDiopter != it) focusDiopter = it }
                            if (!evUserAdjusting) {
                                live.evIndex?.let {
                                    if (evIndex != it) evIndex = it
                                    evSliderPosition = it.toFloat()
                                }
                            }
                        }
                    }
                )
                val preview = previewBuilder.build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val captureBuilder = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .setTargetRotation(rotation)
                if (outputFormat != ImageCapture.OUTPUT_FORMAT_JPEG) {
                    captureBuilder.setOutputFormat(outputFormat)
                }
                captureBuilder.setJpegQuality(when (imageQuality) {
                    "Low" -> 60
                    "Medium" -> 78
                    "High" -> 92
                    else -> 100
                })
                if (mode == "Photo" && nightShotMode == "night") {
                    Camera2Interop.Extender(captureBuilder)
                        .setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                        .setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                        .setCaptureRequestOption(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 0)
                }
                if (!hdrExtensionActive && !nightExtensionActive && nightShotMode == "off" &&
                    manualExposure && (manualIsoEnabled || manualShutterEnabled)
                ) {
                    val captureIso = if (manualIsoEnabled) manualIso else iso.takeIf { it > 0 } ?: 400
                    val captureExposure = if (manualShutterEnabled) manualShutterNs
                        else shutterNs.takeIf { it > 0L } ?: 10_000_000L
                    // Keep manual exposure on the still-capture use case, not the preview repeating request.
                    Camera2Interop.Extender(captureBuilder)
                        .setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                        .setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
                        .setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, captureIso)
                        .setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, captureExposure)
                        .setCaptureRequestOption(CaptureRequest.SENSOR_FRAME_DURATION, maxOf(captureExposure, 10_000_000L))
                }
                if (imageQuality == "Best" || uhdCaptureEnabled) {
                    captureBuilder.setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                            .build()
                    )
                }
                // Off/Low/Medium/High use only the active camera's ISP noise reduction.
                // High+ adds the strongest NAFNet pass after capture while retaining light ISP NR.
                val availableNrModes = activeFacingId?.let { id ->
                    runCatching {
                        cameraManager.getCameraCharacteristics(id)
                            .get(CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES)?.toSet()
                    }.getOrNull()
                }.orEmpty()
                val preferredNrModes = when (noiseReduction) {
                    "Off" -> listOf(CaptureRequest.NOISE_REDUCTION_MODE_OFF)
                    "Low" -> listOf(
                        CaptureRequest.NOISE_REDUCTION_MODE_FAST,
                        CaptureRequest.NOISE_REDUCTION_MODE_MINIMAL
                    )
                    "Medium" -> listOf(
                        CaptureRequest.NOISE_REDUCTION_MODE_MINIMAL,
                        CaptureRequest.NOISE_REDUCTION_MODE_FAST
                    )
                    "High" -> listOf(
                        CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY,
                        CaptureRequest.NOISE_REDUCTION_MODE_MINIMAL,
                        CaptureRequest.NOISE_REDUCTION_MODE_FAST
                    )
                    "High+" -> listOf(
                        CaptureRequest.NOISE_REDUCTION_MODE_MINIMAL,
                        CaptureRequest.NOISE_REDUCTION_MODE_FAST
                    )
                    else -> listOf(
                        CaptureRequest.NOISE_REDUCTION_MODE_MINIMAL,
                        CaptureRequest.NOISE_REDUCTION_MODE_FAST,
                        CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY
                    )
                }
                val effectiveNrMode = preferredNrModes.firstOrNull { it in availableNrModes }
                if (effectiveNrMode != null && !hdrExtensionActive && !nightExtensionActive) {
                    Camera2Interop.Extender(captureBuilder).setCaptureRequestOption(
                        CaptureRequest.NOISE_REDUCTION_MODE, effectiveNrMode
                    )
                }
                if (!hdrExtensionActive && !nightExtensionActive &&
                    nightShotMode == "star_trails" && mode == "Photo"
                ) {
                    val characteristics = activeFacingId?.let { id ->
                        runCatching { cameraManager.getCameraCharacteristics(id) }.getOrNull()
                    }
                    val capabilities = characteristics?.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                    val exposureRange = characteristics?.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
                    val sensitivityRange = characteristics?.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
                    if (capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR) == true &&
                        exposureRange != null && sensitivityRange != null
                    ) {
                        val maxFrameDuration = characteristics.get(CameraCharacteristics.SENSOR_INFO_MAX_FRAME_DURATION)
                            ?: Long.MAX_VALUE
                        val maxSafeExposure = minOf(exposureRange.upper, maxFrameDuration)
                            .coerceAtLeast(exposureRange.lower)
                        val requestedExposure = starTrailExposureSeconds.toLong() * 1_000_000_000L
                        val exposureNs = requestedExposure.coerceIn(exposureRange.lower, maxSafeExposure)
                        val trailIso = 800.coerceIn(sensitivityRange.lower, sensitivityRange.upper)
                        Camera2Interop.Extender(captureBuilder)
                            .setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                            .setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
                            .setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, trailIso)
                            .setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureNs)
                            .setCaptureRequestOption(CaptureRequest.SENSOR_FRAME_DURATION, maxOf(exposureNs, 10_000_000L))
                        val minimumFocusDistance = characteristics.get(
                            CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE
                        ) ?: 0f
                        if (minimumFocusDistance > 0f) {
                            Camera2Interop.Extender(captureBuilder)
                                .setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
                                .setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, 0f)
                        }
                        android.util.Log.i(
                            "FUJI_STAR_TRAILS",
                            "Manual exposure configured: " + exposureNs + "ns, ISO=" + trailIso
                        )
                    } else {
                        android.util.Log.w("FUJI_STAR_TRAILS", "Manual sensor unavailable; using normal automatic exposure")
                    }
                }
                val capture = captureBuilder.build()
                capture.flashMode = flash

                val analysis = ImageAnalysis.Builder()
                    .setTargetRotation(rotation)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                previewUseCase = preview
                analysisUseCase = analysis
                var lastAnalysis = 0L
                var lastAiAnalysis = 0L
                analysis.setAnalyzer(analysisExecutor) { image ->
                    try {
                        val now = System.currentTimeMillis()
                        if (now - lastAnalysis > 140) {
                            lastAnalysis = now
                            val plane = image.planes[0]
                            val buffer = plane.buffer
                            val bins = IntArray(64)
                            val edges = ArrayList<Offset>(250)
                            val rowStride = plane.rowStride
                            val pixelStride = plane.pixelStride
                            var luminanceSum = 0L
                            var sampleCount = 0
                            var y = 8
                            while (y < image.height - 1) {
                                var x = 8
                                while (x < image.width - 1) {
                                    val index = y * rowStride + x * pixelStride
                                    val value = buffer.get(index).toInt() and 0xFF
                                    bins[value shr 2]++
                                    luminanceSum += value
                                    sampleCount++
                                    if (focusPeakingEnabled.value && x > 1 && y > 1 && index < buffer.limit()) {
                                        val leftIndex = index - pixelStride
                                        val upIndex = index - rowStride
                                        if (leftIndex >= 0 && upIndex >= 0 && upIndex < buffer.limit()) {
                                            val left = buffer.get(leftIndex).toInt() and 0xFF
                                            val up = buffer.get(upIndex).toInt() and 0xFF
                                            if (abs(value - left) + abs(value - up) > 30 && edges.size < 1400) {
                                                val px = x.toFloat() / image.width
                                                val py = y.toFloat() / image.height
                                                val oriented = when (image.imageInfo.rotationDegrees) {
                                                    90 -> Offset(1f - py, px)
                                                    180 -> Offset(1f - px, 1f - py)
                                                    270 -> Offset(py, 1f - px)
                                                    else -> Offset(px, py)
                                                }
                                                edges.add(if (front) Offset(1f - oriented.x, oriented.y) else oriented)
                                            }
                                        }
                                    }
                                    x += 8
                                }
                                y += 8
                            }
                            val meanLuma = if (sampleCount > 0) (luminanceSum / sampleCount).toInt() else 255
                            scope.launch(Dispatchers.Main) {
                                hist = bins
                                peakPoints = if (focusPeakingEnabled.value) edges else emptyList()
                                lowLightDetected = meanLuma < 52
                            }
                        }

                        if (aiCompositionEnabled && faceDetector != null && now - lastAiAnalysis > 550) {
                            lastAiAnalysis = now
                            val mediaImage = image.image
                            if (mediaImage != null) {
                                try {
                                    val input = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
                                    val faces = Tasks.await(faceDetector.process(input), 600, TimeUnit.MILLISECONDS)
                                    val largest = faces.maxByOrNull {
                                        it.boundingBox.width().toLong() * it.boundingBox.height().toLong()
                                    }
                                    val hint = if (largest == null) {
                                        "Chưa thấy khuôn mặt · thử đặt chủ thể vào 1/3"
                                    } else {
                                        val bounds = largest.boundingBox
                                        val rotation = image.imageInfo.rotationDegrees
                                        val centerX = when (rotation) {
                                            90 -> 1f - bounds.exactCenterY() / image.height.coerceAtLeast(1)
                                            180 -> 1f - bounds.exactCenterX() / image.width.coerceAtLeast(1)
                                            270 -> bounds.exactCenterY() / image.height.coerceAtLeast(1)
                                            else -> bounds.exactCenterX() / image.width.coerceAtLeast(1)
                                        }.coerceIn(0f, 1f)
                                        val visibleCenterX = if (front) 1f - centerX else centerX
                                        when {
                                            visibleCenterX < 0.38f -> "Chủ thể ở 1/3 trái · chừa khoảng trống bên phải"
                                            visibleCenterX > 0.62f -> "Chủ thể ở 1/3 phải · chừa khoảng trống bên trái"
                                            else -> "Chủ thể ở giữa · thử lệch nhẹ theo quy tắc 1/3"
                                        }
                                    }
                                    scope.launch(Dispatchers.Main) { aiCompositionHint = hint }
                                } catch (error: Exception) {
                                    android.util.Log.w("FUJI_AI_COMPOSITION", "Face analysis skipped", error)
                                }
                            }
                        }
                    } finally {
                        image.close()
                    }
                }

                // Use the exact ratio used by the Compose preview container. Reading
                // PreviewView.width/height here can pick up the previous layout during rotation.
                val viewPort = ViewPort.Builder(ratio, rotation)
                    .setScaleType(ViewPort.FILL_CENTER)
                    .build()
                val group = UseCaseGroup.Builder()
                    .setViewPort(viewPort)
                    .addUseCase(preview)
                    .addUseCase(capture)
                    .addUseCase(analysis)
                    .build()
                val bound = provider.bindToLifecycle(lifecycleOwner, selector, group)
                camera = bound
                imageCapture = capture

                val actualCamera2Info = runCatching {
                    Camera2CameraInfo.from(bound.cameraInfo)
                }.getOrNull()
                val actualCameraId = actualCamera2Info?.cameraId
                activeCameraCharacteristics = actualCameraId?.let { id ->
                    runCatching { cameraManager.getCameraCharacteristics(id) }.getOrNull()
                }
                activeCameraDetails = discoveredCamera2.firstOrNull {
                    it.id == actualCameraId && it.parentId == null
                }
                activePhysicalCameraDetails = discoveredCamera2.filter {
                    it.parentId == actualCameraId
                }
                android.util.Log.i(
                    "FUJI_CAMERA2_ACTIVE",
                    "CameraX bound cameraId=${actualCameraId ?: "unknown"}, " +
                        "hardware=${activeCameraDetails?.hardwareLevel ?: "unknown"}, " +
                        "physicalIds=${activeCameraDetails?.physicalIds ?: emptySet<String>()}"
                )
                boundCameraRotation = rotation
                boundCameraLandscape = isLandscape
                boundCameraRatioIdx = ratioIdx
                evIndex = appPrefs.getInt("pro_ev_index", 0)
                evSliderPosition = evIndex.toFloat()
                evUserAdjusting = false
                val zoomStateOnBind = bound.cameraInfo.zoomState.value
                val savedZoom = appPrefs.getFloat("camera_zoom_ratio", 1f)
                val restoredZoom = savedZoom.coerceIn(
                    zoomStateOnBind?.minZoomRatio ?: 1f,
                    min(2f, zoomStateOnBind?.maxZoomRatio ?: 1f)
                )
                zoomSel = restoredZoom
                runCatching { bound.cameraControl.setZoomRatio(restoredZoom) }
                val capabilities = runCatching {
                    Camera2CameraInfo.from(bound.cameraInfo).getCameraCharacteristic(
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES
                    )
                }.getOrNull()
                val supportsManualSensor = capabilities?.contains(
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR
                ) == true
                // White-balance warmth/coolness is a manual sensor color-correction request.
                if (manualColorTempEnabled) applyManualWhiteBalance(bound, manualColorTemp)
                else applyAutoWhiteBalance(bound)
                if (stabilization) applyStabilization(bound, true)
            }

            // When UHD sharpening is requested, prefer OEM HDR JPEG (or software HDR)
            // so the post-capture 2x sharpening can still run. Explicit Ultra HDR remains
            // higher priority because its gain map must be preserved without recompression.
            val preferJpegR = !rawCaptureActive &&
                (ultraHdr || (hdrCaptureRequested && !uhdCaptureEnabled)) &&
                Build.VERSION.SDK_INT >= 34
            preserveUltraHdrOutput = false
            try {
                val requestedFormat = when {
                    rawCaptureActive -> rawCaptureFormat
                    preferJpegR -> ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR
                    else -> ImageCapture.OUTPUT_FORMAT_JPEG
                }
                var actualCaptureFormat = requestedFormat
                try {
                    bindCapture(requestedFormat)
                    if (!preferJpegR && hdrCaptureRequested && !hdrExtensionActive) {
                        softwareHdrFallback = true
                        localizedToast(
                            context,
                            "Đang dùng Software HDR bằng CPU vì thiết bị không cung cấp HDR Extension.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (formatError: Exception) {
                    if (hdrExtensionActive && preferJpegR) {
                        android.util.Log.w("FUJI_HDR", "HDR extension does not accept JPEG_R; retrying as JPEG", formatError)
                        provider.unbindAll()
                        actualCaptureFormat = ImageCapture.OUTPUT_FORMAT_JPEG
                        bindCapture(actualCaptureFormat)
                    } else throw formatError
                }

                if (preferJpegR) {
                    val jpegRSupported = actualCaptureFormat == ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR &&
                        runCatching {
                            ImageCapture.getImageCaptureCapabilities(camera!!.cameraInfo)
                                .supportedOutputFormats
                                .contains(ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR)
                        }.getOrDefault(false)
                    if (jpegRSupported) {
                        preserveUltraHdrOutput = true
                    }
                    if (!jpegRSupported && !hdrExtensionActive) {
                        preserveUltraHdrOutput = false
                        ultraHdr = false
                        softwareHdrFallback = hdrCaptureRequested
                        localizedToast(
                            context,
                            if (hdrCaptureRequested) "Thiết bị không có HDR phần cứng; sẽ dùng Software HDR bằng CPU sau khi chụp."
                            else "Camera không hỗ trợ Ultra HDR; đã chuyển về JPEG thường.",
                            Toast.LENGTH_LONG
                        ).show()
                        provider.unbindAll()
                        bindCapture(ImageCapture.OUTPUT_FORMAT_JPEG)
                    } else if (!jpegRSupported && hdrExtensionActive) {
                        preserveUltraHdrOutput = false
                        localizedToast(context,
                            "HDR đa khung đang hoạt động; máy không hỗ trợ JPEG_R nên ảnh được lưu dạng JPEG.",
                            Toast.LENGTH_LONG).show()
                        provider.unbindAll()
                        bindCapture(ImageCapture.OUTPUT_FORMAT_JPEG)
                    }
                }
            } catch (e: Exception) {
                if (hdrExtensionActive) {
                    // Some FULL/LEVEL_3 implementations still reject the three-use-case
                    // combination. Drop only the OEM extension and retry the base camera.
                    android.util.Log.w("FUJI_HDR", "HDR extension bind failed; retrying base camera", e)
                    hdrExtensionActive = false
                    selector = baseSelector
                    provider.unbindAll()
                    var fallbackFormat = if (preferJpegR) ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR
                        else ImageCapture.OUTPUT_FORMAT_JPEG
                    var fallbackBound = runCatching { bindCapture(fallbackFormat) }
                    if (fallbackBound.isFailure && fallbackFormat == ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR) {
                        provider.unbindAll()
                        fallbackFormat = ImageCapture.OUTPUT_FORMAT_JPEG
                        fallbackBound = runCatching { bindCapture(fallbackFormat) }
                    }
                    if (fallbackBound.isSuccess && camera != null && imageCapture != null) {
                        val fallbackJpegRSupported =
                            fallbackFormat == ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR &&
                                runCatching {
                                    ImageCapture.getImageCaptureCapabilities(camera!!.cameraInfo)
                                        .supportedOutputFormats
                                        .contains(ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR)
                                }.getOrDefault(false)
                        preserveUltraHdrOutput = fallbackJpegRSupported
                        if (hdrCaptureRequested && !fallbackJpegRSupported) {
                            softwareHdrFallback = true
                            localizedToast(
                                context,
                                "HDR Extension không ghép được với camera này; đang chuyển sang Software HDR xử lý bằng CPU.",
                                Toast.LENGTH_LONG
                            ).show()
                        } else if (hdrCaptureRequested) {
                            localizedToast(
                                context,
                                "HDR Extension không thể ghép với luồng live; đang dùng Ultra HDR JPEG_R thay thế.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        camera = null
                        imageCapture = null
                        preserveUltraHdrOutput = false
                        localizedToast(
                            context,
                            "Không thể khởi tạo HDR hoặc camera: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else if (ultraHdr || hdrCaptureRequested) {
                    preserveUltraHdrOutput = false
                    ultraHdr = false
                    softwareHdrFallback = hdrCaptureRequested
                    if (hdrCaptureRequested) {
                        localizedToast(
                            context,
                            "Camera không hỗ trợ HDR phần cứng; đang dùng Software HDR bằng CPU.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    provider.unbindAll()
                    runCatching { bindCapture(ImageCapture.OUTPUT_FORMAT_JPEG) }
                        .onFailure {
                            camera = null
                            imageCapture = null
                            localizedToast(context, "Không thể khởi tạo HDR hoặc camera: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    camera = null
                    imageCapture = null
                    localizedToast(context, "Không mở được camera: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            captureSetupReady = camera != null && imageCapture != null
            }, mainExecutor)
        }, mainExecutor)
    }

    LaunchedEffect(flash, imageCapture) {
        imageCapture?.flashMode = flash
    }
    LaunchedEffect(targetRotation, imageCapture, previewUseCase, analysisUseCase) {
        imageCapture?.targetRotation = targetRotation
        previewUseCase?.targetRotation = targetRotation
        analysisUseCase?.targetRotation = targetRotation
    }

    // Restore the latest app-owned photo after relaunch, then refresh its thumbnail.
    LaunchedEffect(Unit) {
        if (lastUri == null) {
            val recent = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        arrayOf(MediaStore.Images.Media._ID),
                        "${MediaStore.Images.Media.RELATIVE_PATH} = ?",
                        arrayOf("Pictures/FujifilmMode/"),
                        "${MediaStore.Images.Media.DATE_ADDED} DESC"
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                cursor.getLong(0)
                            )
                        } else null
                    }
                }.getOrNull()
            }
            if (recent != null) lastUri = recent
        }
    }

    LaunchedEffect(lastUri) {
        val uri = lastUri ?: return@LaunchedEffect
        thumb = withContext(Dispatchers.IO) {
            runCatching {
                decodeUprightBitmap(context, uri, maxDimension = 320)?.asImageBitmap()
            }.getOrNull()
        }
    }

    LaunchedEffect(photoViewerVisible, lastUri) {
        if (!photoViewerVisible) return@LaunchedEffect
        val uri = lastUri ?: return@LaunchedEffect
        viewerBitmap = withContext(Dispatchers.IO) {
            runCatching {
                // Decode EXIF into the pixels instead of trusting a stale MediaStore thumbnail.
                decodeUprightBitmap(context, uri, maxDimension = 2048)?.asImageBitmap()
            }.getOrNull()
        }
    }

    fun openLastPhoto() {
        if (lastUri == null) {
            localizedToast(context, "Chưa có ảnh trong Pictures/FujifilmMode.", Toast.LENGTH_SHORT).show()
            return
        }
        photoViewerVisible = true
    }

    fun startStarTrailSession(activeCapture: ImageCapture, activeCamera: Camera) {
        val captureRotation = targetRotation
        val captureIsLandscape = isLandscape
        if (boundCameraRotation != captureRotation ||
            boundCameraLandscape != captureIsLandscape ||
            boundCameraRatioIdx != ratioIdx
        ) {
            targetRotation = captureRotation
            localizedToast(
                context,
                "Đang đồng bộ hướng và tỉ lệ khung ngắm, hãy chụp lại sau một chút.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        activeCapture.targetRotation = captureRotation
        previewUseCase?.targetRotation = captureRotation
        analysisUseCase?.targetRotation = captureRotation
        if (appPrefs.getBoolean("shutter_sound_enabled", true)) {
            playConfiguredShutterSound(appPrefs, shutterSoundPlayer)
        }

        val info = runCatching { Camera2CameraInfo.from(activeCamera.cameraInfo) }.getOrNull()
        val capabilities = info?.getCameraCharacteristic(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
        val exposureRange = info?.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        val sensitivityRange = info?.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val maxFrameDuration = info?.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_MAX_FRAME_DURATION)
            ?: Long.MAX_VALUE
        val manualSensorSupported = capabilities?.contains(
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR
        ) == true && exposureRange != null && sensitivityRange != null
        val effectiveExposureNs = if (manualSensorSupported) {
            val safeUpper = minOf(exposureRange!!.upper, maxFrameDuration)
                .coerceAtLeast(exposureRange.lower)
            (starTrailExposureSeconds.toLong() * 1_000_000_000L)
                .coerceIn(exposureRange.lower, safeUpper)
        } else 0L
        val effectiveIso = if (manualSensorSupported) 800.coerceIn(
            sensitivityRange!!.lower, sensitivityRange.upper
        ) else 0
        val appliedExposureLabel = if (manualSensorSupported) {
            "%.1fs · ISO %d · focus ∞".format(
                Locale.US, effectiveExposureNs / 1_000_000_000.0, effectiveIso
            )
        } else {
            "AE tự động · không hỗ trợ manual sensor"
        }
        starTrailExposureStatus = appliedExposureLabel
        activeCapture.flashMode = ImageCapture.FLASH_MODE_OFF

        starTrailStopRequested = false
        starTrailFrameCount = 0
        starTrailStartedAtMillis = System.currentTimeMillis()
        starTrailElapsedSeconds = 0
        starTrailCaptureInProgress = true
        starTrailTimerJob?.cancel()
        starTrailTimerJob = scope.launch {
            while (starTrailCaptureInProgress) {
                starTrailElapsedSeconds =
                    ((System.currentTimeMillis() - starTrailStartedAtMillis) / 1000L).toInt()
                delay(1000)
            }
        }
        localizedToast(
            context,
            if (manualSensorSupported) {
                "Star Trails đang ghi: $appliedExposureLabel. Nhấn nút chụp lần nữa để dừng và ghép vệt sao."
            } else {
                "Camera không hỗ trợ phơi sáng thủ công; Star Trails dùng AE tự động. Nhấn lần nữa để dừng và ghép."
            },
            Toast.LENGTH_LONG
        ).show()

        val sessionId = System.currentTimeMillis().toString()
        val focal = info?.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
            ?.firstOrNull()?.let { "%.1f mm".format(Locale.US, it) } ?: ""
        val aperture = info?.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)
            ?.firstOrNull()?.let { "f/%.1f".format(Locale.US, it) } ?: ""

        scope.launch {
            val frames = mutableListOf<File>()
            var outputUri: Uri? = null
            var stoppedForStorage = false
            try {
                while (!starTrailStopRequested) {
                    // Keep recording until the second shutter press. Stop early only to protect
                    // the device from filling its cache partition during a very long session.
                    if (context.cacheDir.usableSpace < 256L * 1024L * 1024L) {
                        stoppedForStorage = true
                        break
                    }
                    val frameFile = File(
                        context.cacheDir,
                        "fuji-star-" + sessionId + "-" + frames.size + ".jpg"
                    )
                    val captured = withContext(Dispatchers.IO) {
                        NightShotProcessor.captureFrameToFile(activeCapture, frameFile, mainExecutor)
                    }
                    if (!captured) {
                        runCatching { frameFile.delete() }
                        break
                    }
                    frames.add(frameFile)
                    starTrailFrameCount = frames.size
                    starTrailElapsedSeconds =
                        ((System.currentTimeMillis() - starTrailStartedAtMillis) / 1000L).toInt()
                    delay(120)
                }
                starTrailCaptureInProgress = false
                if (stoppedForStorage) {
                    localizedToast(
                        context,
                        "Star Trails tự dừng để bảo vệ bộ nhớ tạm. Đang ghép các khung đã ghi.",
                        Toast.LENGTH_LONG
                    ).show()
                }

                if (frames.size >= 2) {
                    isProcessing = true
                    processingProgress = 10
                    processingEtaSeconds = -1
                    processingLabel = "Đang ghép vệt sao · ${frames.size} khung hình"
                    val starUri = withContext(Dispatchers.IO) {
                        NightShotProcessor.saveStarTrails(context, frames) { mergeProgress ->
                            mainExecutor.execute {
                                processingLabel = "Đang ghép vệt sao · ${frames.size} khung hình"
                                processingProgress = (10 + mergeProgress * 0.75f)
                                    .toInt().coerceIn(10, 84)
                            }
                        }
                    }
                    if (starUri != null) {
                        outputUri = starUri
                        withContext(Dispatchers.Main) {
                            processingProgress = 86
                            processingLabel = "Đang hoàn tất ảnh Star Trails"
                        }
                        val postProcessed = withContext(Dispatchers.IO) {
                            val look = runCatching { applyCameraLook(context, starUri) }.getOrDefault(false)
                            val frame = runCatching { applyCameraFrameOverlay(context, starUri) }.getOrDefault(false)
                            val stamped = runCatching {
                                applyCameraWatermark(
                                    context,
                                    starUri,
                                    focal,
                                    aperture,
                                    if (manualSensorSupported) "%.1fs".format(
                                        Locale.US, effectiveExposureNs / 1_000_000_000.0
                                    ) else "AUTO",
                                    if (manualSensorSupported) effectiveIso.toString() else "AUTO"
                                )
                            }.getOrDefault(false)
                            val metadata = runCatching {
                                finalizeCapturedImageMetadata(context, starUri)
                            }.getOrDefault(false)
                            listOf(look, frame, stamped, metadata)
                        }
                        val previewThumb = withContext(Dispatchers.IO) {
                            runCatching {
                                decodeUprightBitmap(context, starUri, maxDimension = 512)?.asImageBitmap()
                            }.getOrNull()
                        }
                        withContext(Dispatchers.Main) {
                            lastUri = starUri
                            thumb = previewThumb
                            processingProgress = 100
                            localizedToast(
                                context,
                                "Đã lưu Star Trails từ " + frames.size + " khung hình" +
                                    if (postProcessed[3]) "." else " (cần kiểm tra lại metadata).",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        localizedToast(
                            context,
                            "Không thể ghép vệt sao từ phiên này. Hãy thử lại với máy ổn định hơn.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    localizedToast(
                        context,
                        "Cần ít nhất 2 khung hình để tạo vệt sao. Hãy thử lại.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (failure: Throwable) {
                android.util.Log.e("FUJI_STAR_TRAILS", "Star Trails session failed", failure)
                outputUri?.let { runCatching { context.contentResolver.delete(it, null, null) } }
                localizedToast(
                    context,
                    "Star Trails gặp lỗi: " + (failure.message ?: "không rõ nguyên nhân"),
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                activeCapture.flashMode = flash
                starTrailTimerJob?.cancel()
                starTrailTimerJob = null
                frames.forEach { runCatching { it.delete() } }
                starTrailCaptureInProgress = false
                starTrailStopRequested = false
                starTrailFrameCount = 0
                starTrailStartedAtMillis = 0L
                starTrailElapsedSeconds = 0
                delay(350)
                isProcessing = false
                processingProgress = 0
                processingEtaSeconds = -1
            }
        }
    }

    fun takePhoto() {
        if (nightShotMode == "star_trails" && starTrailCaptureInProgress) {
            starTrailStopRequested = true
            localizedToast(context, "Đang dừng Star Trails sau khung hình hiện tại…", Toast.LENGTH_SHORT).show()
            return
        }
        if (!captureSetupReady || camera == null || imageCapture == null ||
            isProcessing || nightShotCaptureInProgress || starTrailCaptureInProgress
        ) return
        if (mode == "Photo" && nightShotMode == "star_trails") {
            val activeCapture = imageCapture ?: return
            val activeCamera = camera ?: return
            startStarTrailSession(activeCapture, activeCamera)
            return
        }
        scope.launch {
            if (timerSec > 0) {
                for (s in timerSec downTo 1) {
                    countdown = s
                    delay(1000)
                }
                countdown = 0
            }
            // Resolve the active camera/use case after the timer, because the user may rotate
            // or switch lenses while the countdown is running.
            val capture = imageCapture ?: return@launch
            val activeCamera = camera ?: return@launch
            val captureIsUltraHdr = preserveUltraHdrOutput
            val captureUseSoftwareHdr = softwareHdrFallback && !captureIsUltraHdr
            val captureRawOnly = capture.outputFormat == ImageCapture.OUTPUT_FORMAT_RAW
            val captureRawPlus = capture.outputFormat == ImageCapture.OUTPUT_FORMAT_RAW_JPEG
            val captureUseUhdAi = uhdCaptureEnabled && !captureRawPlus
            val captureNoiseReduction = if (captureRawPlus) "Off" else noiseReduction
            val captureUseNightShot = mode == "Photo" && nightShotMode == "night"
            val captureNightShotExtension = nightExtensionActive
            val captureNightFrameCount = nightShotFrameCount.coerceIn(4, 12)
            if (captureUseNightShot) {
                localizedToast(
                    context,
                    "NightShot chụp nhiều khung với AE bình thường (EV 0). Hãy giữ máy ổn định.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            // ISO/shutter are applied only for the still shot, never continuously to the preview stream.
            if (appPrefs.getBoolean("shutter_sound_enabled", true)) {
                playConfiguredShutterSound(appPrefs, shutterSoundPlayer)
            }
            val shutterEffectType = appPrefs.getString("shutter_effect", "Film Wash")
            scope.launch {
                if (shutterEffectType == "Film Wash") {
                    // Warm emulsion wash: a soft amber veil and a moving light band.
                    filmWashProgress.snapTo(1f)
                    filmWashProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 1050, easing = FastOutSlowInEasing)
                    )
                } else {
                    shutterEffectAlpha = 0.72f
                    delay(70)
                    shutterEffectAlpha = 0.34f
                    delay(80)
                    shutterEffectAlpha = 0f
                }
            }
            // Freeze display rotation and the active camera's sensor-relative rotation for
            // this still capture. CameraX combines target rotation with SENSOR_ORIENTATION.
            // Freeze the physical handset orientation tracked by OrientationEventListener.
            // PreviewView/Display.rotation may remain portrait while the handset is landscape.
            val captureRotation = targetRotation
            val captureIsLandscape = isLandscape
            val selectedBaseAspectRatio = when (ratioIdx) {
                0 -> 3f / 2f
                1 -> 4f / 3f
                2 -> 16f / 9f
                3 -> 1f
                else -> 21f / 9f
            }
            val expectedAspectRatioForCapture = if (captureIsLandscape || ratioIdx == 3) {
                selectedBaseAspectRatio
            } else {
                1f / selectedBaseAspectRatio
            }
            if (captureRotation != targetRotation ||
                boundCameraRotation != captureRotation ||
                boundCameraLandscape != captureIsLandscape ||
                isLandscape != captureIsLandscape ||
                boundCameraRatioIdx != ratioIdx
            ) {
                // Never save a frame with the old orientation/crop after the display rotated.
                targetRotation = captureRotation
                isLandscape = captureIsLandscape
                localizedToast(context, "Đang đồng bộ hướng và tỉ lệ khung ngắm, hãy chụp lại sau một chút.", Toast.LENGTH_SHORT).show()
                return@launch
            }
            capture.targetRotation = captureRotation
            previewUseCase?.targetRotation = captureRotation
            analysisUseCase?.targetRotation = captureRotation
            val captureRotationDegrees = runCatching {
                activeCamera.cameraInfo.getSensorRotationDegrees(captureRotation)
            }.getOrDefault(0)
            val name = "FUJI_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$name.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FujifilmMode")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val rawValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$name.dng")
                put(MediaStore.Images.Media.MIME_TYPE, "image/x-adobe-dng")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FujifilmMode/RAW")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            if (captureUseNightShot) nightShotCaptureInProgress = true
            val output = ImageCapture.OutputFileOptions.Builder(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            ).build()
            val rawOutput = ImageCapture.OutputFileOptions.Builder(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                rawValues
            ).build()
            val imageSavedCallback = object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                        val saved = result.savedUri
                        val isRawCallback = result.imageFormat == ImageFormat.RAW_SENSOR
                        if (isRawCallback && saved != null) {
                            context.contentResolver.update(
                                saved,
                                ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                                null,
                                null
                            )
                            android.util.Log.i("FUJI_RAW", "DNG saved: $saved")
                            if (captureRawOnly) {
                                localizedToast(
                                    context,
                                    "Đã lưu ảnh RAW DNG gốc vào Pictures/FujifilmMode/RAW.",
                                    Toast.LENGTH_LONG
                                ).show()
                                isProcessing = false
                                processingProgress = 0
                                processingEtaSeconds = -1
                            }
                            return
                        }
                        if (captureRawOnly) {
                            localizedToast(context, "Camera không trả về dữ liệu RAW_SENSOR hợp lệ.", Toast.LENGTH_LONG).show()
                            isProcessing = false
                            return
                        }
                        if (captureNightShotExtension) {
                            nightShotCaptureInProgress = false
                        }
                        if (saved != null) {
                            val info = runCatching { Camera2CameraInfo.from(activeCamera.cameraInfo) }.getOrNull()
                            val focal = info?.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                                ?.firstOrNull()?.let { "%.1f mm".format(Locale.US, it) } ?: ""
                            val aperture = info?.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)
                                ?.firstOrNull()?.let { "f/%.1f".format(Locale.US, it) } ?: ""
                            val actualIso = if (manualIsoEnabled) manualIso else iso
                            val actualShutterNs = if (manualShutterEnabled) manualShutterNs else shutterNs
                            val shutterLabel = if (actualShutterNs > 0L) {
                                val seconds = actualShutterNs / 1_000_000_000.0
                                if (seconds >= 0.5) "%.1fs".format(Locale.US, seconds)
                                else "1/" + (1.0 / seconds).roundToInt() + "s"
                            } else ""
                            isProcessing = true
                            processingProgress = 4
                            processingEtaSeconds = -1
                            val supirRequested = appPrefs.getBoolean("uhd_coming_soon", false)
                            processingLabel = when {
                                captureIsUltraHdr -> "Đang giữ nguyên dữ liệu Ultra HDR"
                                captureUseSoftwareHdr -> "Đang xử lý Software HDR"
                                captureUseNightShot -> "NightShot đang xử lý, vui lòng giữ máy"
                                captureUseUhdAi -> "AI đang xử lý, vui lòng chờ"
                                supirRequested -> "SUPIR AI Render chưa tích hợp · lưu ảnh thường"
                                else -> "Ảnh đang được xử lý"
                            }
                            runCatching {
                                Camera2CameraControl.from(activeCamera.cameraControl).clearCaptureRequestOptions()
                                if (manualColorTempEnabled) applyManualWhiteBalance(activeCamera, manualColorTemp)
                                if (manualFocus > 0f) applyManualFocus(activeCamera, manualFocus)
                            }
                            scope.launch(Dispatchers.IO) {
                                var nightShotMerged = false
                                if (captureUseNightShot && !captureNightShotExtension) {
                                    val nightFrames = mutableListOf<File>()
                                    val firstFrame = File(context.cacheDir, "fuji-night-first-" + System.nanoTime() + ".jpg")
                                    try {
                                        if (NightShotProcessor.copyUriToFile(context, saved, firstFrame)) {
                                            nightFrames.add(firstFrame)
                                            for (index in 1 until captureNightFrameCount) {
                                                withContext(Dispatchers.Main) {
                                                    processingLabel = "NightShot: đang chụp khung " +
                                                        (index + 1) + "/" + captureNightFrameCount
                                                    processingProgress = (5 + index * 2).coerceAtMost(24)
                                                }
                                                val nextFrame = File(
                                                    context.cacheDir,
                                                    "fuji-night-" + System.nanoTime() + "-" + index + ".jpg"
                                                )
                                                val captured = NightShotProcessor.captureFrameToFile(
                                                    capture,
                                                    nextFrame,
                                                    mainExecutor
                                                )
                                                if (!captured) {
                                                    runCatching { nextFrame.delete() }
                                                    break
                                                }
                                                nightFrames.add(nextFrame)
                                                delay(100)
                                            }
                                            if (nightFrames.size >= 2) {
                                                withContext(Dispatchers.Main) {
                                                    processingLabel = "NightShot: đang gộp và giảm nhiễu"
                                                    processingProgress = 27
                                                }
                                                nightShotMerged = NightShotProcessor.mergeFramesIntoUri(
                                                    context,
                                                    nightFrames,
                                                    saved,
                                                    NightShotProcessor.MergeMode.NIGHT_AVERAGE
                                                ) { mergeProgress ->
                                                    mainExecutor.execute {
                                                        processingLabel = "NightShot: đang căn khung và giảm nhiễu"
                                                        processingProgress = (27 + mergeProgress * 0.55f)
                                                            .toInt().coerceIn(27, 82)
                                                    }
                                                }
                                            }
                                        }
                                    } finally {
                                        nightFrames.forEach { runCatching { it.delete() } }
                                        withContext(Dispatchers.Main) {
                                            nightShotCaptureInProgress = false
                                        }
                                    }
                                }
                                withContext(Dispatchers.Main) { processingProgress = 30 }
                                val orientationNormalized = if (captureIsUltraHdr) {
                                    // CameraX has already written the capture rotation. Do not
                                    // decode/recompress JPEG_R: Bitmap JPEG export discards the gain map.
                                    true
                                } else runCatching {
                                    normalizeCapturedImage(
                                        context,
                                        saved,
                                        captureRotationDegrees,
                                        expectedAspectRatioForCapture
                                    )
                                }.getOrDefault(false)
                                withContext(Dispatchers.Main) { processingProgress = 35 }
                                val aiDenoiseApplied = if (
                                    !captureIsUltraHdr && captureNoiseReduction == "High+"
                                ) {
                                    val sourceBytes = runCatching {
                                        context.contentResolver.openAssetFileDescriptor(saved, "r")?.use { it.length }
                                    }.getOrNull()?.takeIf { it > 0L } ?: 12L * 1024L * 1024L
                                    val estimatedDenoiseSeconds = (
                                        12.0 + sourceBytes.toDouble() / (1024.0 * 1024.0) * 1.6
                                    ).toInt().coerceIn(15, 180)
                                    val denoiseStartedAtMillis = System.currentTimeMillis()
                                    withContext(Dispatchers.Main) {
                                        processingLabel = "Nhiễu đang được xử lý"
                                        noiseReductionProgress = 0
                                        processingProgress = 36
                                        processingEtaSeconds = estimatedDenoiseSeconds
                                    }
                                    withContext(Dispatchers.IO) {
                                        runCatching {
                                            NafNetDenoiser.enhanceCapturedPhoto(context, saved, 1.0f) { progress ->
                                                mainExecutor.execute {
                                                    val safeProgress = progress.coerceIn(0, 100)
                                                    val elapsedSeconds = (
                                                        (System.currentTimeMillis() - denoiseStartedAtMillis) / 1000L
                                                    ).toInt()
                                                    val fraction = safeProgress / 100f
                                                    val remainingSeconds = when {
                                                        safeProgress >= 100 -> 0
                                                        safeProgress >= 3 -> (
                                                            elapsedSeconds / fraction * (1f - fraction)
                                                        ).toInt().coerceAtLeast(1)
                                                        else -> (estimatedDenoiseSeconds - elapsedSeconds).coerceAtLeast(1)
                                                    }
                                                    noiseReductionProgress = safeProgress
                                                    processingProgress = (36 + safeProgress * 0.19f)
                                                        .toInt().coerceIn(36, 55)
                                                    processingEtaSeconds = remainingSeconds.coerceIn(0, 600)
                                                    processingLabel = "Nhiễu đang được xử lý"
                                                }
                                            }
                                        }.onFailure {
                                            android.util.Log.e("FUJI_NAFNET", "NAFNet denoising failed; keep captured original", it)
                                        }.getOrDefault(false)
                                    }
                                } else false
                                withContext(Dispatchers.Main) {
                                    processingProgress = 56
                                    processingEtaSeconds = -1
                                    if (captureNoiseReduction == "High+" && !aiDenoiseApplied && !captureIsUltraHdr) {
                                        processingLabel = "NAFNet không khả dụng · giữ ảnh gốc"
                                    }
                                }
                                val softwareHdrApplied = if (
                                    !captureIsUltraHdr && captureUseSoftwareHdr
                                ) {
                                    runCatching {
                                        applySoftwarePhotoEnhancement(
                                            context,
                                            saved,
                                            softwareHdr = true,
                                            uhdSharpening = false
                                        )
                                    }.getOrDefault(false)
                                } else false
                                withContext(Dispatchers.Main) { processingProgress = 60 }
                                val lookApplied = if (captureIsUltraHdr) false else
                                    runCatching { applyCameraLook(context, saved) }.getOrDefault(false)
                                val frameApplied = if (captureIsUltraHdr) false else
                                    runCatching { applyCameraFrameOverlay(context, saved) }.getOrDefault(false)
                                withContext(Dispatchers.Main) { processingProgress = 72 }
                                val stamped = if (captureIsUltraHdr) false else runCatching {
                                    applyCameraWatermark(
                                        context, saved, focal, aperture, shutterLabel,
                                        if (actualIso > 0) actualIso.toString() else "AUTO"
                                    )
                                }.getOrDefault(false)

                                val aiSuperResolutionApplied = if (!captureIsUltraHdr && captureUseUhdAi) {
                                    withContext(Dispatchers.Main) {
                                        processingLabel = "AI đang xử lý, vui lòng chờ"
                                        processingProgress = 78
                                    }
                                    // Estimate duration from the captured JPEG size. This is an
                                    // on-device estimate, not a fixed promise: it adapts while the
                                    // native tiled inference is running and stops at 95% until done.
                                    val sourceBytes = runCatching {
                                        context.contentResolver.openAssetFileDescriptor(saved, "r")?.use { it.length }
                                    }.getOrNull()?.takeIf { it > 0L } ?: 12L * 1024L * 1024L
                                    val estimatedAiSeconds = (
                                        18.0 + sourceBytes.toDouble() / (1024.0 * 1024.0) * 1.25
                                    ).toInt().coerceIn(20, 120)
                                    var aiRenderFinished = false
                                    val progressTicker = scope.launch {
                                        val startedAt = System.currentTimeMillis()
                                        while (!aiRenderFinished) {
                                            val elapsedSeconds = ((System.currentTimeMillis() - startedAt) / 1000L).toInt()
                                            val fraction = (elapsedSeconds.toFloat() / estimatedAiSeconds).coerceIn(0f, 1f)
                                            processingProgress = (78 + fraction * 17f).toInt().coerceIn(78, 95)
                                            processingEtaSeconds = (estimatedAiSeconds - elapsedSeconds).coerceAtLeast(1)
                                            delay(500)
                                        }
                                    }
                                    val aiResult = try {
                                        runCatching {
                                            NcnnSuperResolution.enhanceCapturedPhoto(context, saved)
                                        }.onFailure {
                                            android.util.Log.e("FUJI_NCNN_SR", "AI enhancement failed", it)
                                        }.getOrDefault(false)
                                    } finally {
                                        aiRenderFinished = true
                                        progressTicker.cancel()
                                    }
                                    withContext(Dispatchers.Main) {
                                        processingProgress = 96
                                        processingEtaSeconds = 2
                                    }
                                    aiResult
                                } else false

                                // The existing GPU/CPU sharpener remains as a safe fallback if NCNN
                                // cannot initialize Vulkan, the model asset is unavailable, or inference fails.
                                val softwareSharpenFallback = if (
                                    !captureIsUltraHdr && captureUseUhdAi && !aiSuperResolutionApplied
                                ) {
                                    withContext(Dispatchers.Main) {
                                        processingLabel = "AI không khả dụng · chuyển sang làm nét GPU/CPU"
                                        processingProgress = 84
                                    processingEtaSeconds = 3
                                    }
                                    runCatching {
                                        applySoftwarePhotoEnhancement(
                                            context,
                                            saved,
                                            softwareHdr = false,
                                            uhdSharpening = true
                                        )
                                    }.getOrDefault(false)
                                } else false

                                val metadataPublished = runCatching {
                                    if (captureIsUltraHdr) publishUltraHdrCapture(context, saved)
                                    else finalizeCapturedImageMetadata(context, saved)
                                }.getOrDefault(false)
                                // Normalization runs before image looks and watermark drawing.
                                // No later rotation is allowed to move the frame away from the
                                // final upright pixel coordinate system.
                                val orientationLabelFixed = orientationNormalized && metadataPublished
                                withContext(Dispatchers.Main) {
                                    processingProgress = 98
                                    processingEtaSeconds = 1
                                }
                                withContext(Dispatchers.Main) {
                                    lastUri = saved
                                    processingProgress = 100
                                    val watermarkRequested = appPrefs.getBoolean("watermark_enabled", true)
                                    val message = when {
                                        captureUseNightShot && nightShotMerged && aiDenoiseApplied && orientationLabelFixed ->
                                            "Đã lưu NightShot · gộp nhiều khung và khử nhiễu AI NAFNet."
                                        aiDenoiseApplied && orientationLabelFixed ->
                                            "Đã lưu ảnh đúng hướng · AI NAFNet đã khử nhiễu ($captureNoiseReduction)."
                                        captureNoiseReduction != "Off" && !aiDenoiseApplied && !captureIsUltraHdr ->
                                            "Ảnh đã lưu · NAFNet không chạy được nên giữ ảnh chụp gốc, chưa áp dụng khử nhiễu AI."
                                        captureUseNightShot && nightShotMerged && orientationLabelFixed ->
                                            "NightShot đã gộp nhiều khung hình và giảm nhiễu; ảnh đã được lưu đúng hướng."
                                        captureUseNightShot && captureNightShotExtension && orientationLabelFixed ->
                                            "NightShot đã xử lý bằng chế độ NIGHT của camera."
                                        captureUseNightShot && orientationLabelFixed ->
                                            "NightShot không ghép đủ khung hình; ảnh chụp gốc vẫn được giữ."
                                        captureIsUltraHdr && orientationLabelFixed ->
                                            "Đã lưu Ultra HDR nguyên bản và giữ gain map. Watermark, LUT, frame và sharpen UHD hậu kỳ được bỏ qua để bảo toàn HDR."
                                        captureIsUltraHdr ->
                                            "Đã lưu tệp Ultra HDR nguyên bản; giữ nguyên gain map, vui lòng kiểm tra hướng ảnh trong thư viện."
                                        stamped && lookApplied && orientationLabelFixed -> "Đã lưu ảnh đúng tỉ lệ, đúng hướng, có màu và watermark."
                                        stamped && orientationLabelFixed -> "Đã lưu ảnh đúng tỉ lệ, đúng hướng và có watermark."
                                        stamped -> "Watermark đã đóng nhưng tỉ lệ/hướng ảnh chưa được xác minh."
                                        watermarkRequested && !stamped -> "Ảnh đã lưu nhưng watermark chưa được đóng. Kiểm tra lại mục Watermark."
                                        lookApplied && orientationLabelFixed -> "Đã lưu ảnh đúng tỉ lệ, đúng hướng và áp dụng bộ lọc/LUT."
                                        lookApplied -> "Đã áp dụng màu nhưng chưa xác minh được tỉ lệ/hướng ảnh."
                                        orientationLabelFixed && aiSuperResolutionApplied ->
                                            "Đã lưu ảnh đúng hướng · Real-ESRGAN NCNN x2 đã làm nét bằng GPU/CPU."
                                        orientationLabelFixed && (softwareHdrApplied || softwareSharpenFallback) ->
                                            "Đã lưu ảnh đúng hướng; đã áp dụng " + listOfNotNull(
                                                "Software HDR".takeIf { softwareHdrApplied },
                                                "GPU/CPU Sharpen 2×".takeIf { softwareSharpenFallback }
                                            ).joinToString(" + ") + "."
                                        orientationLabelFixed -> "Đã lưu ảnh đúng tỉ lệ và chuẩn hóa metadata vào album."
                                        else -> "Ảnh đã lưu nhưng chưa xác minh được tỉ lệ/hướng metadata. Hãy kiểm tra lại ảnh."
                                    }
                                    val finalMessage = if (captureIsUltraHdr) message
                                        else if (frameApplied) "$message Frame đã được áp dụng." else message
                                    localizedToast(context, finalMessage, Toast.LENGTH_SHORT).show()
                                }
                                delay(450)
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    processingProgress = 0
                                    processingEtaSeconds = -1
                                }
                            }
                        } else {
                            nightShotCaptureInProgress = false
                            localizedToast(context, "Ảnh đã chụp nhưng MediaStore không trả về URI.", Toast.LENGTH_LONG).show()
                        }
                    }
                    override fun onError(exception: androidx.camera.core.ImageCaptureException) {
                        nightShotCaptureInProgress = false
                        runCatching {
                            camera?.let { Camera2CameraControl.from(it.cameraControl).clearCaptureRequestOptions() }
                        }
                        localizedToast(context, "Lỗi chụp ảnh: ${exception.message}", Toast.LENGTH_LONG).show()
                    }
                }
            if (capture.outputFormat == ImageCapture.OUTPUT_FORMAT_RAW_JPEG) {
                capture.takePicture(
                    rawOutput,
                    output,
                    ContextCompat.getMainExecutor(context),
                    imageSavedCallback
                )
            } else {
                capture.takePicture(
                    if (capture.outputFormat == ImageCapture.OUTPUT_FORMAT_RAW) rawOutput else output,
                    ContextCompat.getMainExecutor(context),
                    imageSavedCallback
                )
            }
        }
    }

    fun animateZoomTo(targetRatio: Float) {
        val activeCamera = camera ?: return
        val state = activeCamera.cameraInfo.zoomState.value ?: return
        val safeTarget = targetRatio.coerceIn(
            state.minZoomRatio,
            min(2f, state.maxZoomRatio)
        )
        val start = state.zoomRatio
        val crossesLensBoundary = (start < 0.98f && safeTarget >= 0.98f) ||
            (start >= 0.98f && safeTarget < 0.98f)
        zoomAnimationJob?.cancel()
        if (crossesLensBoundary) {
            lensTransitionLabel = if (safeTarget < 0.98f) "SIÊU RỘNG" else "CAM THƯỜNG"
            lensTransitionVisible = true
            scope.launch {
                lensTransitionPulse.snapTo(0f)
                lensTransitionPulse.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 620, easing = FastOutSlowInEasing)
                )
            }
            scope.launch {
                delay(820)
                lensTransitionVisible = false
            }
        }
        zoomAnimationJob = scope.launch {
            val animatedZoom = Animatable(start)
            animatedZoom.animateTo(
                targetValue = safeTarget,
                animationSpec = tween(durationMillis = 620, easing = FastOutSlowInEasing)
            ) {
                activeCamera.cameraControl.setZoomRatio(value)
                zoomSel = value
            }
            appPrefs.edit().putFloat("camera_zoom_ratio", safeTarget).apply()
        }
    }

    val cameraInfo2 = camera?.cameraInfo?.let { runCatching { Camera2CameraInfo.from(it) }.getOrNull() }
    val requestCapabilities = cameraInfo2?.getCameraCharacteristic(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
    val manualSensorSupported = requestCapabilities?.contains(
        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR
    ) == true
    val isoRange = if (manualSensorSupported) cameraInfo2?.getCameraCharacteristic(
        CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE
    ) else null
    val shutterRange = if (manualSensorSupported) cameraInfo2?.getCameraCharacteristic(
        CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE
    ) else null
    val maxFocus = cameraInfo2?.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f
    val evRange = camera?.cameraInfo?.exposureState?.exposureCompensationRange
    val evStep = camera?.cameraInfo?.exposureState?.exposureCompensationStep?.toFloat() ?: 0f
    val evText = if (evStep > 0f) "%+.1f".format(Locale.US, evIndex * evStep) else "0.0"
    val currentIso = if (manualIsoEnabled) manualIso else iso
    val isoText = if (manualIsoEnabled) manualIso.toString() else "AUTO"
    val currentShutter = if (manualShutterEnabled) manualShutterNs else shutterNs
    val shutterText = if (currentShutter > 0) {
        val seconds = currentShutter / 1_000_000_000.0
        if (seconds >= 0.5) "%.1f\"".format(Locale.US, seconds)
        else "1/${(1.0 / seconds).roundToInt()}"
    } else "AUTO"
    val afText = if (focusLocked) "LOCK" else if (manualFocus > 0f) "%.1f D".format(Locale.US, manualFocus) else "AF"
    val toneText = if (manualColorTempEnabled) "${manualColorTemp}K" else "AUTO"


    // Animate the preview's ratio when the device changes orientation.
    val basePreviewRatio = aspectRatios[ratioIdx.coerceIn(0, aspectRatios.lastIndex)].second
    val targetPreviewRatio = if (isLandscape) 1f / basePreviewRatio else basePreviewRatio
    // Change crop ratio atomically with CameraX's ViewPort. The surrounding layout still
    // receives a subtle orientation animation below, without a temporary preview/photo mismatch.
    val previewAspectRatio = targetPreviewRatio
    val orientationProgress by animateFloatAsState(
        targetValue = if (isLandscape) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 360f),
        label = "camera orientation transition"
    )

    val toolbarContent: @Composable (Modifier) -> Unit = { toolbarModifier ->
                    Row(toolbarModifier, horizontalArrangement = Arrangement.SpaceBetween) {
                        CircleBtn(
                            icon = when (flash) {
                                ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                                ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                                else -> Icons.Default.FlashOff
                            },
                            desc = "Flash",
                            onClick = {
                                flash = when (flash) {
                                    ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
                                    ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                                    else -> ImageCapture.FLASH_MODE_OFF
                                }
                            }
                        )
                        CircleBtn(
                            Icons.Default.Timer, "Timer",
                            onClick = { timerSec = when (timerSec) { 0 -> 3; 3 -> 10; else -> 0 } },
                            tint = if (timerSec > 0) CamColors.Yellow else Color.White
                        )
                        CircleBtn(
                            Icons.Default.Tune, "Metering",
                            onClick = { metering = (metering + 1) % meteringModes.size },
                            tint = if (metering > 0) CamColors.Yellow else Color.White
                        )
                        CircleBtn(
                            Icons.Default.BarChart, "Histogram",
                            onClick = { onShowHist(!showHist) },
                            tint = if (showHist) CamColors.Yellow else Color.White
                        )
                        CircleBtn(
                            Icons.Default.FilterCenterFocus, "Focus Peaking",
                            onClick = { onFocusPeaking(!focusPeaking) },
                            tint = if (focusPeaking) Color(0xFF00FFB0) else Color.White
                        )
                        CircleBtn(
                            Icons.Default.Settings, "Quick settings",
                            onClick = { showQuick = !showQuick },
                            tint = if (showQuick) CamColors.Yellow else Color.White
                        )
                        if (mode == "Photo" && nightShotMode != "off") {
                            CircleBtn(
                                Icons.Default.Bedtime,
                                if (nightShotMode == "star_trails") "Star Trails · nhấn chụp để bắt đầu/dừng"
                                else if (nightExtensionActive) "NightShot · Camera NIGHT"
                                else "NightShot · gộp nhiều khung",
                                onClick = onOpenSettings,
                                tint = CamColors.Yellow
                            )
                        }
                    }
    }

    val previewContent: @Composable (Modifier) -> Unit = { previewModifier ->
                    Box(previewModifier, contentAlignment = Alignment.Center) {
                        Box(
                            Modifier
                                .aspectRatio(previewAspectRatio)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black)
                                .pointerInput(camera) {
                                    detectTransformGestures { _, _, zoomChange, _ ->
                                        zoomAnimationJob?.cancel()
                                        val zoomState = camera?.cameraInfo?.zoomState?.value ?: return@detectTransformGestures
                                        val maxZoom = min(2f, zoomState.maxZoomRatio)
                                        val nextZoom = (zoomState.zoomRatio * zoomChange)
                                            .coerceIn(zoomState.minZoomRatio, maxZoom)
                                        camera?.cameraControl?.setZoomRatio(nextZoom)
                                        zoomSel = nextZoom
                                        appPrefs.edit().putFloat("camera_zoom_ratio", nextZoom).apply()
                                    }
                                }
                                .pointerInput(camera, metering, mode) {
                                    detectTapGestures(
                                        onTap = { offset ->
                                            val active = camera ?: return@detectTapGestures
                                            focusLocked = false
                                            if (manualFocus > 0f) {
                                                manualFocus = 0f
                                                runCatching { applyAutoFocus(active) }
                                            }
                                            val area = when (metering) { 0 -> 0.38f; 1 -> 0.70f; else -> 0.12f }
                                            val point = previewView.meteringPointFactory.createPoint(offset.x, offset.y, area)
                                            val action = FocusMeteringAction.Builder(
                                                point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                                            ).setAutoCancelDuration(3, TimeUnit.SECONDS).build()
                                            active.cameraControl.startFocusAndMetering(action)
                                        },
                                        onLongPress = { offset ->
                                            val active = camera ?: return@detectTapGestures
                                            if (manualFocus > 0f) {
                                                localizedToast(context, "Chuyển AF sang Auto trước khi khóa nét.", Toast.LENGTH_SHORT).show()
                                            } else {
                                                val point = previewView.meteringPointFactory.createPoint(offset.x, offset.y, 0.22f)
                                                val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                                                    // Keep the tapped AF region active until the next user tap.
                                                    .disableAutoCancel()
                                                    .build()
                                                active.cameraControl.startFocusAndMetering(action)
                                                focusLocked = true
                                                localizedToast(context, "Đã khóa nét · chạm lại để lấy nét mới.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                        ) {
                            key(renderEngine) {
                                AndroidView(
                                    factory = { previewView },
                                    modifier = Modifier.fillMaxSize().graphicsLayer {
                                        val pulse = kotlin.math.sin(lensTransitionPulse.value * Math.PI).toFloat()
                                        scaleX = 1f + pulse * 0.045f
                                        scaleY = 1f + pulse * 0.045f
                                        alpha = 1f - pulse * 0.14f
                                    }
                                )
                            }
                            val previewTint = when {
                                activeLutUri.isNotBlank() -> Color(0xFFB4A7C8)
                                activeFilter.contains("Mono") || activeFilter == "Acros" -> Color(0xFFBFC3C8)
                                activeFilter.contains("Warm") || activeFilter.contains("Portra") ||
                                    activeFilter == "Astia" || activeFilter == "Kodak Gold" -> Color(0xFFE0A46B)
                                activeFilter.contains("Cool") || activeFilter == "Sony S-Log3" -> Color(0xFF6D9BC8)
                                activeFilter != "None" -> Color(0xFFB8A66A)
                                else -> Color.Transparent
                            }
                            if (activeFilter != "None" || activeLutUri.isNotBlank()) {
                                Canvas(Modifier.fillMaxSize()) {
                                    drawRect(previewTint.copy(alpha = 0.105f))
                                }
                            }
                            if (shutterEffectAlpha > 0f || filmWashProgress.value > 0f) {
                                Canvas(Modifier.fillMaxSize()) {
                                    if (shutterEffectAlpha > 0f) {
                                        drawRect(Color.White.copy(alpha = shutterEffectAlpha))
                                    }
                                    val wash = filmWashProgress.value
                                    if (wash > 0f) {
                                        drawRect(Color(0xFFFFC078).copy(alpha = 0.24f * wash))
                                        val bandTop = size.height * (1f - wash * 0.88f)
                                        drawRect(
                                            Color(0xFFFFF0D2).copy(alpha = 0.38f * wash),
                                            Offset(0f, bandTop),
                                            DrawSize(size.width, size.height * 0.12f)
                                        )
                                        drawRect(
                                            Color(0xFFFFE5B2).copy(alpha = 0.20f * wash),
                                            Offset(0f, bandTop + size.height * 0.13f),
                                            DrawSize(size.width, size.height * 0.025f)
                                        )
                                        drawRect(
                                            Color(0xFF5A2915).copy(alpha = 0.10f * wash),
                                            Offset(0f, size.height * 0.84f),
                                            DrawSize(size.width, size.height * 0.16f)
                                        )
                                    }
                                }
                            }
                            if (aiCompositionEnabled) {
                                Text(
                                    "AI BỐ CỤC · $aiCompositionHint",
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)
                                        .clip(RoundedCornerShape(7.dp)).background(Color(0xAA000000))
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                            FilmWashLabel(
                                progress = filmWashProgress.value,
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp)
                            )
                            if (showGrid) {
                                Canvas(Modifier.fillMaxSize()) {
                                    val c = Color(0x66FFFFFF)
                                    for (i in 1..2) {
                                        val x = size.width * i / 3f
                                        val y = size.height * i / 3f
                                        drawLine(c, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
                                        drawLine(c, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                                    }
                                }
                            }
                            if (focusPeaking && peakPoints.isNotEmpty()) {
                                Canvas(Modifier.fillMaxSize()) {
                                    peakPoints.forEach { p ->
                                        drawCircle(Color(0xFF00FFB0), 2.dp.toPx(),
                                            Offset(p.x * size.width, p.y * size.height))
                                    }
                                }
                            }
                            if (focusLocked) {
                                Text("AF LOCK", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
                                        .clip(RoundedCornerShape(8.dp)).background(CamColors.Yellow).padding(horizontal = 9.dp, vertical = 5.dp))
                            }
                            Text(
                                if (focusLocked) "AF LOCK" else "TAP AF · HOLD LOCK",
                                color = if (focusLocked) CamColors.Yellow else Color.White.copy(alpha = 0.72f),
                                fontSize = 8.sp,
                                modifier = Modifier.align(Alignment.TopEnd).padding(7.dp)
                                    .clip(RoundedCornerShape(6.dp)).background(Color(0x66000000))
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                            LensTransitionOverlay(
                                visible = lensTransitionVisible,
                                lensLabel = lensTransitionLabel,
                                modifier = Modifier.align(Alignment.Center)
                            )
                            if (showHist) {
                                Histogram(hist, Modifier.align(Alignment.TopStart).padding(8.dp).size(106.dp, 52.dp))
                            }
                            if (!hasCam) {
                                Text(
                                    "Cần cấp quyền Camera\n(chạm để cấp)",
                                    color = Color.White, textAlign = TextAlign.Center,
                                    modifier = Modifier.align(Alignment.Center)
                                        .clickable { permissionLauncher.launch(Manifest.permission.CAMERA) }
                                )
                            }
                        }
                    }
    }

    val controlsContent: @Composable (Modifier) -> Unit = { controlsModifier ->
        Column(controlsModifier) {
                        // Lens quick-select values come from the physical cameras exposed by
                        // this logical CameraX camera, then are clamped to its real zoom range.
                        val zoomStateForUi = camera?.cameraInfo?.zoomState?.value
                        val zoomMaxForUi = min(2f, zoomStateForUi?.maxZoomRatio ?: 1f)
                        val zoomMinForUi = zoomStateForUi?.minZoomRatio ?: 1f
                        val zoomSteps = Camera2Info.opticalZoomStops(
                            logical = activeCameraDetails,
                            physical = activePhysicalCameraDetails,
                            minZoom = zoomMinForUi,
                            maxZoom = zoomMaxForUi
                        )
                        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically) {
                                zoomSteps.forEach { z ->
                                    val selectedZoom = abs(zoomSel - z) < 0.08f
                                    val label = if (z < 1f || abs(z - z.toInt()) > 0.01f) {
                                        String.format(Locale.US, "%.1f×", z)
                                    } else {
                                        "${z.toInt()}×"
                                    }
                                    Text(label,
                                        color = if (selectedZoom) CamColors.Yellow else CamColors.TextColor,
                                        fontWeight = if (selectedZoom) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                val next = z.coerceIn(zoomMinForUi, zoomMaxForUi)
                                                animateZoomTo(next)
                                            }.padding(horizontal = 5.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
            
                        val selected = proSel
                        if (selected != null && mode == "Pro") {
                            Row(Modifier.fillMaxWidth().padding(bottom = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(selected, color = CamColors.Yellow, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(42.dp), fontSize = 12.sp)
                                when (selected) {
                                    "EV" -> {
                                        val range = evRange
                                        if (range != null && range.upper > range.lower) {
                                            Slider(
                                                value = evIndex.toFloat().coerceIn(range.lower.toFloat(), range.upper.toFloat()),
                                                onValueChange = {
                                                    manualExposure = false
                                                    val value = it.roundToInt()
                                                    evIndex = value
                                                    camera?.let { c ->
                                                        runCatching { c.cameraControl.setExposureCompensationIndex(value) }
                                                    }
                                                },
                                                valueRange = range.lower.toFloat()..range.upper.toFloat(),
                                                steps = (range.upper - range.lower - 1).coerceAtLeast(0),
                                                colors = cameraSliderColors()
                                            )
                                        } else Text("EV không được hỗ trợ", color = CamColors.Sub, fontSize = 11.sp)
                                    }
                                    "ISO" -> {
                                        val range = isoRange
                                        Column(Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (manualSensorSupported && range != null && range.upper > range.lower) {
                                                    val lo = range.lower
                                                    val hi = range.upper.coerceAtMost(2400).coerceAtLeast(range.lower)
                                                    Slider(
                                                        modifier = Modifier.weight(1f),
                                                        value = ((manualIso.coerceIn(lo, hi) - lo).toFloat() / (hi - lo).coerceAtLeast(1)).coerceIn(0f, 1f),
                                                        onValueChange = { f ->
                                                            manualIso = (lo + f * (hi - lo)).roundToInt().coerceIn(lo, hi)
                                                            manualIsoEnabled = true
                                                            manualExposure = true
                                                            camera?.let { active ->
                                                                val previewExposure = if (manualShutterEnabled) {
                                                                    manualShutterNs
                                                                } else {
                                                                    shutterNs.takeIf { it > 0L } ?: 16_666_667L
                                                                }
                                                                runCatching { applyManualExposure(active, manualIso, previewExposure) }
                                                            }
                                                        },
                                                        onValueChangeFinished = { manualCaptureRevision++ },
                                                        colors = cameraSliderColors()
                                                    )
                                                } else Spacer(Modifier.weight(1f))
                                                Text("AUTO", color = if (manualIsoEnabled) CamColors.Sub else CamColors.Yellow,
                                                    fontSize = 10.sp, modifier = Modifier.padding(start = 8.dp).clickable {
                                                        manualIsoEnabled = false
                                                        manualExposure = manualShutterEnabled
                                                        manualCaptureRevision++
                                                    })
                                            }
                                            Text(if (manualIsoEnabled) "ISO $manualIso" else "ISO Auto",
                                                color = CamColors.Sub, fontSize = 9.sp)
                                        }
                                    }
                                    "Tv" -> {
                                        val range = shutterRange
                                        Column(Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (manualSensorSupported && range != null && range.upper > range.lower) {
                                                    val lo = range.lower
                                                    val hi = range.upper.coerceAtMost(1_000_000_000L).coerceAtLeast(range.lower)
                                                    Slider(
                                                        modifier = Modifier.weight(1f),
                                                        value = shutterFraction(manualShutterNs.coerceAtMost(1_000_000_000L), lo, hi),
                                                        onValueChange = { f ->
                                                            manualShutterNs = shutterFromFraction(f, lo, hi).coerceAtMost(1_000_000_000L)
                                                            manualShutterEnabled = true
                                                            manualExposure = true
                                                            camera?.let { active ->
                                                                val previewIso = if (manualIsoEnabled) manualIso else iso.takeIf { it > 0 } ?: 400
                                                                runCatching { applyManualExposure(active, previewIso, manualShutterNs) }
                                                            }
                                                        },
                                                        onValueChangeFinished = { manualCaptureRevision++ },
                                                        colors = cameraSliderColors()
                                                    )
                                                } else Spacer(Modifier.weight(1f))
                                                Text("AUTO", color = if (manualShutterEnabled) CamColors.Sub else CamColors.Yellow,
                                                    fontSize = 10.sp, modifier = Modifier.padding(start = 8.dp).clickable {
                                                        manualShutterEnabled = false
                                                        manualExposure = manualIsoEnabled
                                                        manualCaptureRevision++
                                                    })
                                            }
                                            Text(if (manualShutterEnabled) shutterText else "Shutter Auto",
                                                color = CamColors.Sub, fontSize = 9.sp)
                                        }
                                    }
                                    "AF" -> {
                                        Column(Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (maxFocus > 0f) {
                                                    Slider(
                                                        modifier = Modifier.weight(1f),
                                                        value = (manualFocus / maxFocus).coerceIn(0f, 1f),
                                                        onValueChange = { f ->
                                                            manualFocus = f * maxFocus
                                                            focusLocked = false
                                                            camera?.let { runCatching { applyManualFocus(it, manualFocus) } }
                                                        },
                                                        colors = cameraSliderColors()
                                                    )
                                                } else Text("AF tự động", color = CamColors.Sub, fontSize = 10.sp, modifier = Modifier.weight(1f))
                                                Text("AUTO", color = CamColors.Yellow, fontSize = 10.sp,
                                                    modifier = Modifier.padding(start = 8.dp).clickable {
                                                        manualFocus = 0f
                                                        focusLocked = false
                                                        camera?.let { runCatching { applyAutoFocus(it) } }
                                                    })
                                            }
                                            Text(if (focusLocked) "Focus locked" else afText, color = CamColors.Sub, fontSize = 9.sp)
                                        }
                                    }
                                    "Tone" -> {
                                        Column(Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("Ấm", color = CamColors.Sub, fontSize = 9.sp)
                                                Slider(
                                                    modifier = Modifier.weight(1f),
                                                    value = ((manualColorTemp - 2500) / 5500f).coerceIn(0f, 1f),
                                                    onValueChange = { fraction ->
                                                        manualColorTemp = (2500 + fraction * 5500).roundToInt()
                                                        manualColorTempEnabled = true
                                                        camera?.let { runCatching { applyManualWhiteBalance(it, manualColorTemp) } }
                                                    },
                                                    colors = cameraSliderColors()
                                                )
                                                Text("Lạnh", color = CamColors.Sub, fontSize = 9.sp)
                                                Text("AUTO", color = if (manualColorTempEnabled) CamColors.Sub else CamColors.Yellow,
                                                    fontSize = 10.sp, modifier = Modifier.padding(start = 8.dp).clickable {
                                                        manualColorTempEnabled = false
                                                        camera?.let { runCatching { applyAutoWhiteBalance(it) } }
                                                    })
                                            }
                                            Text(if (manualColorTempEnabled) "Tông màu $manualColorTemp K" else "Tông màu tự động",
                                                color = CamColors.Sub, fontSize = 9.sp)
                                        }
                                    }
                                }
                            }
                        }
            
                        if (mode == "Pro") {
                            val proItems = listOf(
                                Triple("EV", evText, "EV"),
                                Triple("Tv", shutterText, "Tv"),
                                Triple("ISO", isoText, "ISO"),
                                Triple("AF", if (manualFocus > 0f) "%.1fD".format(Locale.US, manualFocus) else "AF", "AF"),
                                Triple("Tone", toneText, "WB"),
                                Triple("AUTO", "AUTO", "RESET")
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                proItems.forEach { (key, value, label) ->
                                    val active = proSel == key
                                    Column(
                                        Modifier.weight(1f).clip(RoundedCornerShape(11.dp)).background(CamColors.Glass)
                                            .border(0.5.dp, if (active) CamColors.Yellow else CamColors.Stroke, RoundedCornerShape(11.dp))
                                            .clickable {
                                                if (key == "AUTO") {
                                                    manualExposure = false
                                                    manualIsoEnabled = false
                                                    manualShutterEnabled = false
                                                    manualColorTempEnabled = false
                                                    manualFocus = 0f
                                                    focusLocked = false
                                                    evIndex = 0
                                                    manualCaptureRevision++
                                                    camera?.let { c ->
                                                        runCatching {
                                                            Camera2CameraControl.from(c.cameraControl).clearCaptureRequestOptions()
                                                            c.cameraControl.setExposureCompensationIndex(0)
                                                            applyAutoFocus(c)
                                                            applyAutoWhiteBalance(c)
                                                        }
                                                    }
                                                    proSel = null
                                                } else {
                                                    if (key == "EV" && manualExposure) {
                                                        manualIsoEnabled = false
                                                        manualShutterEnabled = false
                                                        manualExposure = false
                                                        manualCaptureRevision++
                                                        camera?.let { runCatching { it.cameraControl.setExposureCompensationIndex(evIndex) } }
                                                    }
                                                    if (key == "AF" && manualFocus > 0f) {
                                                        manualFocus = 0f
                                                        focusLocked = false
                                                        camera?.let { runCatching { applyAutoFocus(it) } }
                                                    }
                                                    proSel = if (active) null else key
                                                }
                                            }.padding(vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(value, color = if (active) CamColors.Yellow else Color.White,
                                            fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                                        Text(label, color = CamColors.Sub, fontSize = 9.sp)
                                    }
                                }
                            }
                        } else if (mode == "Photo" && nightShotMode != "off") {
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (nightShotMode == "night") "NightShot" else "Star Trails",
                                    color = CamColors.Yellow,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    if (nightShotMode == "night") "AUTO · EV 0.0 · MULTI-FRAME"
                                    else "${starTrailExposureSeconds}s · ISO 800 · EV 0.0",
                                    color = CamColors.Sub,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp
                                )
                            }
                        } else {
                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("EV", color = CamColors.Yellow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                val range = evRange
                                if (range != null && range.upper > range.lower) {
                                    Slider(
                                        modifier = Modifier.weight(1f),
                                        value = evSliderPosition.coerceIn(range.lower.toFloat(), range.upper.toFloat()),
                                        onValueChange = {
                                            evUserAdjusting = true
                                            evSliderPosition = it
                                            val value = it.roundToInt()
                                            evIndex = value
                                            camera?.let { c -> runCatching { c.cameraControl.setExposureCompensationIndex(value) } }
                                        },
                                        onValueChangeFinished = {
                                            // Camera2 reports exposure asynchronously; keep the user's
                                            // position stable until the new exposure has propagated.
                                            scope.launch {
                                                delay(500)
                                                evUserAdjusting = false
                                            }
                                        },
                                        valueRange = range.lower.toFloat()..range.upper.toFloat(),
                                        steps = (range.upper - range.lower - 1).coerceAtLeast(0),
                                        colors = cameraSliderColors()
                                    )
                                } else {
                                    Text("EV không được hỗ trợ", color = CamColors.Sub, fontSize = 10.sp,
                                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                                }
                                Text(evText, color = CamColors.TextColor, fontSize = 11.sp)
                                Text("AUTO", color = CamColors.Sub, fontSize = 9.sp,
                                    modifier = Modifier.padding(start = 10.dp).clickable {
                                        evUserAdjusting = false
                                        evIndex = 0
                                        evSliderPosition = 0f.coerceIn(
                                            (evRange?.lower ?: 0).toFloat(),
                                            (evRange?.upper ?: 0).toFloat()
                                        )
                                        camera?.let { runCatching { it.cameraControl.setExposureCompensationIndex(0) } }
                                    })
                            }
                        }
            
                        Row(Modifier.fillMaxWidth().padding(vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(52.dp).clip(RoundedCornerShape(13.dp)).background(CamColors.Glass)
                                .clickable { onOpenAlbum() }, contentAlignment = Alignment.Center) {
                                val image = thumb
                                if (image != null) Image(image, "Mở ảnh vừa chụp", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                else Icon(Icons.Default.PhotoLibrary, "Mở album", tint = Color.White)
                            }
                            Box(Modifier.size(72.dp).border(4.dp, Color.White, CircleShape)
                                .padding(7.dp).clip(CircleShape)
                                .background(if (starTrailCaptureInProgress) Color(0xFFE53935) else CamColors.Shutter)
                                .clickable { takePhoto() },
                                contentAlignment = Alignment.Center) {
                                if (starTrailCaptureInProgress) {
                                    Icon(Icons.Default.Close, "Dừng Star Trails",
                                        tint = Color.White, modifier = Modifier.size(28.dp))
                                } else if (shutterButtonBitmap != null) {
                                    Image(shutterButtonBitmap!!, "Custom shutter button",
                                        Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                                }
                            }
                            CircleBtn(Icons.Default.FlipCameraAndroid, "Đổi camera",
                                onClick = { front = !front; manualFocus = 0f }, size = 52.dp)
                        }
            
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            listOf("Pro", "Photo").forEach { item ->
                                val active = mode == item
                                Box(Modifier.padding(horizontal = 4.dp).width(92.dp).height(36.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(if (active) CamColors.Yellow else CamColors.Glass)
                                    .clickable {
                                        mode = item
                                        if (item == "Photo") {
                                            proSel = null
                                            manualExposure = false
                                            manualIsoEnabled = false
                                            manualShutterEnabled = false
                                            manualColorTempEnabled = false
                                            manualFocus = 0f
                                            focusLocked = false
                                            manualCaptureRevision++
                                            camera?.let { runCatching { applyAutoWhiteBalance(it); applyAutoFocus(it) } }
                                        }
                                    },
                                    contentAlignment = Alignment.Center) {
                                    Text(item, color = if (active) Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 5.dp, bottom = 2.dp),
                            horizontalArrangement = Arrangement.Center) {
                            Box(Modifier.clip(RoundedCornerShape(16.dp)).background(CamColors.Glass)
                                .clickable { filterDialogVisible = true }
                                .padding(horizontal = 18.dp, vertical = 7.dp)) {
                                Text(
                                    if (activeFilter != "None") "Bộ lọc: $activeFilter" else "Bộ lọc",
                                    color = if (activeFilter != "None") CamColors.Yellow else Color.White,
                                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Box(Modifier.clip(RoundedCornerShape(16.dp)).background(CamColors.Glass)
                                .clickable { filterDialogVisible = true }
                                .padding(horizontal = 18.dp, vertical = 7.dp)) {
                                Text(
                                    if (activeLutUri.isNotBlank()) "LUT: " + lutDisplayName(context, activeLutUri) else "LUT",
                                    color = if (activeLutUri.isNotBlank()) CamColors.Yellow else Color.White,
                                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Box(Modifier.clip(RoundedCornerShape(16.dp))
                                .background(if (aiCompositionEnabled) Color(0x66F5C842) else CamColors.Glass)
                                .clickable {
                                    val enableAi = !aiCompositionEnabled
                                    aiCompositionEnabled = enableAi
                                    appPrefs.edit().putBoolean("ai_composition_enabled", enableAi).apply()
                                    if (enableAi) {
                                        localizedToast(
                                            context,
                                            "AI vẫn đang phát triển trong quá trình dùng chức năng có thể phát sinh lỗi hoặc không đúng như người dùng mong muốn",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                                .padding(horizontal = 13.dp, vertical = 7.dp)) {
                                Text(if (aiCompositionEnabled) "AI Bố cục ✓" else "AI Bố cục",
                                    color = if (aiCompositionEnabled) CamColors.Yellow else CamColors.TextColor,
                                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
        }
    }

    Box(Modifier.fillMaxSize().background(CamColors.Bg).systemBarsPadding()) {
        // A subtle scale/fade pulse makes the reflow feel deliberate without rotating text.
        val orientationMotion = Modifier.graphicsLayer {
            val pulse = kotlin.math.sin(orientationProgress * Math.PI).toFloat()
            scaleX = 1f + pulse * 0.018f
            scaleY = 1f + pulse * 0.018f
            alpha = 1f - pulse * 0.10f
        }
        if (isLandscape) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp).then(orientationMotion),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    toolbarContent(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(5.dp))
                    previewContent(Modifier.weight(1f).fillMaxWidth())
                }
                controlsContent(
                    Modifier.width(248.dp).fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 8.dp)
                )
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 7.dp).then(orientationMotion)
            ) {
                toolbarContent(Modifier.fillMaxWidth())
                Spacer(Modifier.height(7.dp))
                previewContent(Modifier.weight(1f).fillMaxWidth())
                controlsContent(Modifier.fillMaxWidth())
            }
        }
        if (showQuick) {
            QuickPanel(
                ratioIdx = ratioIdx,
                onRatio = onRatio,
                ultraHdr = ultraHdr,
                onUltraHdr = {
                    val activeCamera = camera
                    val supported = Build.VERSION.SDK_INT >= 34 && activeCamera != null && runCatching {
                        ImageCapture.getImageCaptureCapabilities(activeCamera.cameraInfo)
                            .supportedOutputFormats.contains(ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR)
                    }.getOrDefault(false)
                    if (!ultraHdr && !supported) {
                        localizedToast(context, "Camera/API này không hỗ trợ chụp JPEG Ultra HDR.", Toast.LENGTH_LONG).show()
                    } else {
                        ultraHdr = !ultraHdr
                    }
                },
                uhdCaptureEnabled = uhdCaptureEnabled,
                onUhdCapture = { uhdCaptureEnabled = !uhdCaptureEnabled },
                stabilization = stabilization,
                onStabilization = {
                    stabilization = !stabilization
                    camera?.let { runCatching { applyStabilization(it, stabilization) } }
                },
                metering = meteringModes[metering],
                onMetering = { metering = (metering + 1) % meteringModes.size },
                renderEngine = renderEngine,
                onRenderEngine = { renderEngine = if (renderEngine == "Performance") "Compatible" else "Performance" },
                onToolbox = { showQuick = false; onOpenToolbox() },
                onContentManagement = { showQuick = false; onOpenContentManagement() },
                onSettings = { showQuick = false; onOpenSettings() },
                modifier = Modifier.align(Alignment.TopCenter).padding(start = 10.dp, end = 10.dp, top = 64.dp)
            )
        }

        if (countdown > 0) {
            Text("$countdown", color = Color.White, fontSize = 88.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
        }
        if (starTrailCaptureInProgress) {
            Box(
                Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(top = 86.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                        .background(Color(0xE61A1A1A))
                        .padding(horizontal = 26.dp, vertical = 24.dp)
                ) {
                    Icon(Icons.Default.Bedtime, contentDescription = null,
                        tint = CamColors.Yellow, modifier = Modifier.size(38.dp))
                    Text("ĐANG GHI STAR TRAILS", color = Color.White, fontSize = 16.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
                    Text(
                        starTrailFrameCount.toString() + " khung hình đã chụp",
                        color = CamColors.Yellow,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        "%02d:%02d".format(Locale.US, starTrailElapsedSeconds / 60, starTrailElapsedSeconds % 60) +
                            " · " + starTrailExposureStatus,
                        color = CamColors.Sub,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                    Text("Giữ máy cố định · nhấn nút chụp lần nữa để dừng sau khung hiện tại",
                        color = CamColors.Sub, fontSize = 11.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                    Text("Khuyên dùng tripod; camera hỗ trợ manual sensor mới dùng được 1–8 giây thật",
                        color = CamColors.Sub, fontSize = 10.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        if (nightShotCaptureInProgress) {
            Box(
                Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(horizontal = 28.dp, vertical = 86.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clip(RoundedCornerShape(18.dp))
                        .background(CamColors.Panel).padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Text("NightShot · ĐANG CHỤP", color = CamColors.Yellow,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Giữ máy ổn định · màn hình sẽ luôn sáng đến khi lưu ảnh xong",
                        color = CamColors.Sub, fontSize = 11.sp,
                        modifier = Modifier.padding(top = 5.dp))
                }
            }
        }
        if (isProcessing) {
            Column(Modifier.align(Alignment.BottomCenter).padding(horizontal = 28.dp, vertical = 18.dp)
                .fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CamColors.Panel).padding(14.dp)) {
                val visibleProgress = if (processingLabel == "Nhiễu đang được xử lý") {
                    noiseReductionProgress
                } else processingProgress
                val etaText = when {
                    processingEtaSeconds < 0 -> ""
                    processingEtaSeconds >= 60 -> "Còn ${processingEtaSeconds / 60}p ${processingEtaSeconds % 60}s"
                    else -> "Còn ${processingEtaSeconds}s"
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(processingLabel, color = Color.White, fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(
                        if (etaText.isNotEmpty()) "$" + "{etaText} · $" + "{visibleProgress}%" else "$" + "{visibleProgress}%",
                        color = CamColors.Yellow, fontSize = 11.sp, fontWeight = FontWeight.Bold
                    )
                }
                Canvas(Modifier.fillMaxWidth().padding(top = 9.dp).height(5.dp)) {
                    drawRoundRect(Color(0xFF454545), cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f))
                    drawRoundRect(CamColors.Yellow, size = DrawSize(size.width * visibleProgress / 100f, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f))
                }
            }
        }

        if (filterDialogVisible) {
            FilterLookDialog(
                previewImage = thumb,
                activeFilter = activeFilter,
                activeLutUri = activeLutUri,
                onDismiss = { filterDialogVisible = false },
                onSelectFilter = { selected ->
                    activeFilter = selected
                    activeLutUri = ""
                    appPrefs.edit().putString("selected_filter", selected).remove("selected_lut_uri").apply()
                },
                onSelectLut = { uri ->
                    activeLutUri = uri
                    activeFilter = "None"
                    appPrefs.edit().putString("selected_lut_uri", uri).putString("selected_filter", "None").apply()
                },
                onOpenContent = {
                    filterDialogVisible = false
                    onOpenContentManagement()
                }
            )
        }

        if (photoViewerVisible) {
            Dialog(
                onDismissRequest = { photoViewerVisible = false; viewerBitmap = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    val image = viewerBitmap ?: thumb
                    if (image != null) {
                        Image(
                            bitmap = image,
                            contentDescription = "Ảnh vừa chụp",
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text("Đang tải ảnh...", color = Color.White, modifier = Modifier.align(Alignment.Center))
                    }
                    Box(Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                        CircleBtn(
                            Icons.Default.Close, "Đóng ảnh",
                            onClick = { photoViewerVisible = false; viewerBitmap = null },
                            size = 44.dp
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun FilmWashLabel(progress: Float, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = progress > 0.08f,
        modifier = modifier,
        enter = fadeIn(tween(120)) + scaleIn(initialScale = 0.92f),
        exit = fadeOut(tween(260))
    ) {
        Text(
            "FILM WASH",
            color = Color(0xFFFFE8BD).copy(alpha = progress.coerceIn(0f, 1f)),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                .background(Color(0x99351B10)).padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun LensTransitionOverlay(
    visible: Boolean,
    lensLabel: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(110)) + scaleIn(
            initialScale = 0.78f,
            animationSpec = tween(250, easing = FastOutSlowInEasing)
        ),
        exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.90f, animationSpec = tween(190))
    ) {
        Column(
            Modifier.clip(RoundedCornerShape(16.dp))
                .background(Color(0xD9000000))
                .border(1.dp, Color(0x88FFFFFF), RoundedCornerShape(16.dp))
                .padding(horizontal = 22.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("LENS TRANSITION", color = Color(0xFFBDBDBD), fontSize = 9.sp,
                letterSpacing = 1.5.sp)
            Text(lensLabel, color = CamColors.Yellow,
                fontSize = 17.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp))
        }
    }
}

private val cameraLookGroups = listOf(
    "Fujifilm" to listOf("Provia", "Velvia", "Astia", "Classic Neg", "Eterna", "Acros"),
    "Cổ điển" to listOf("Kodak Portra 400", "Agfa", "Kodak Gold", "Fuji 400"),
    "Leica" to listOf("Leica Vivid", "Leica Natural", "Leica Mono"),
    "Hasselblad" to listOf("Hasselblad HNCS"),
    "Zeiss" to listOf("Zeiss Cool", "Zeiss Warm"),
    "Sony" to listOf("Sony S-Cinetone", "Sony S-Log3"),
    "Log" to listOf("Cinematic Log", "Sony S-Log3")
)

private fun lutDisplayName(context: Context, uriText: String): String {
    val uri = Uri.parse(uriText)
    return runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }
    }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/') ?: "LUT đã nhập"
}

@Composable
private fun FilterLookDialog(
    previewImage: ImageBitmap?,
    activeFilter: String,
    activeLutUri: String,
    onDismiss: () -> Unit,
    onSelectFilter: (String) -> Unit,
    onSelectLut: (String) -> Unit,
    onOpenContent: () -> Unit
) {
    val context = LocalContext.current
    var category by remember { mutableStateOf("Tất cả") }
    val categories = listOf("Tất cả") + cameraLookGroups.map { it.first } + "LUT"
    val importedLuts = remember {
        context.getSharedPreferences("fujifilm_contents", Context.MODE_PRIVATE)
            .getStringSet("luts", emptySet())?.toList()?.sorted() ?: emptyList()
    }
    val allPresets = cameraLookGroups.flatMap { it.second }
    val visiblePresets = if (category == "Tất cả") allPresets
        else cameraLookGroups.firstOrNull { it.first == category }?.second.orEmpty()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxWidth(0.96f).heightIn(max = 650.dp)
                .clip(RoundedCornerShape(22.dp)).background(CamColors.Bg).padding(15.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Bộ lọc & LUT", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("Đóng", color = CamColors.Yellow, modifier = Modifier.clickable(onClick = onDismiss)
                    .padding(6.dp), fontSize = 12.sp)
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                categories.forEach { item ->
                    MiniChip(item, category == item) { category = item }
                }
            }
            Text("Không dùng bộ lọc · Ảnh gốc", color = CamColors.Yellow, fontSize = 11.sp,
                modifier = Modifier.clickable { onSelectFilter("None") }.padding(bottom = 8.dp))

            if (category == "LUT") {
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("LUT .cube của bạn", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("Nhập LUT", color = CamColors.Yellow, fontSize = 11.sp,
                        modifier = Modifier.clickable(onClick = onOpenContent).padding(4.dp))
                }
                if (importedLuts.isEmpty()) {
                    Text("Chưa có LUT. Mở Contents Management để nhập tệp .cube.",
                        color = CamColors.Sub, fontSize = 12.sp)
                    Box(Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(12.dp))
                        .background(CamColors.Card).clickable(onClick = onOpenContent).padding(13.dp),
                        contentAlignment = Alignment.Center) {
                        Text("Mở Contents Management", color = CamColors.Yellow)
                    }
                } else {
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        importedLuts.forEach { uri ->
                            val selected = activeLutUri == uri
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(if (selected) Color(0xFF333027) else CamColors.Card)
                                .clickable { onSelectLut(uri) }.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(54.dp, 42.dp).clip(RoundedCornerShape(7.dp))
                                    .background(Color(0xFF6D8D9A)), contentAlignment = Alignment.Center) {
                                    if (previewImage != null) Image(previewImage, "LUT preview",
                                        Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    Text("LUT", color = Color.White, fontSize = 10.sp,
                                        modifier = Modifier.background(Color(0x88000000)).padding(3.dp))
                                }
                                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                    Text(lutDisplayName(context, uri), color = Color.White, fontSize = 13.sp)
                                    Text("Nhấn để chọn áp dụng sau khi chụp", color = CamColors.Sub, fontSize = 10.sp)
                                }
                                Text(if (selected) "ĐANG DÙNG" else "CHỌN",
                                    color = if (selected) CamColors.Yellow else CamColors.Sub, fontSize = 9.sp)
                            }
                        }
                    }
                }
            } else {
                Text(category, color = CamColors.Sub, fontSize = 11.sp, modifier = Modifier.padding(bottom = 7.dp))
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    visiblePresets.chunked(2).forEach { pair ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pair.forEach { preset ->
                                val selected = activeFilter == preset && activeLutUri.isBlank()
                                val swatch = when {
                                    preset.contains("Mono") || preset == "Acros" -> Color(0xFF8D9298)
                                    preset.contains("Warm") || preset.contains("Portra") || preset == "Astia" ||
                                        preset == "Kodak Gold" -> Color(0xFFB9855C)
                                    preset.contains("Cool") || preset.contains("S-Log") -> Color(0xFF648CA8)
                                    preset.contains("Velvia") || preset.contains("Vivid") -> Color(0xFF5C8A5C)
                                    else -> Color(0xFF9B9671)
                                }
                                Column(Modifier.weight(1f).padding(bottom = 8.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) Color(0xFF393326) else CamColors.Card)
                                    .clickable { onSelectFilter(preset) }.padding(7.dp)) {
                                    Box(Modifier.fillMaxWidth().height(66.dp).clip(RoundedCornerShape(8.dp))
                                        .background(swatch)) {
                                        if (previewImage != null) Image(previewImage, "$preset preview",
                                            Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                        Box(Modifier.fillMaxSize().background(swatch.copy(alpha = 0.20f)))
                                        if (selected) Text("✓ ĐANG DÙNG", color = Color.White, fontSize = 9.sp,
                                            modifier = Modifier.align(Alignment.BottomStart)
                                                .background(Color(0xAA000000)).padding(4.dp))
                                    }
                                    Text(preset, color = Color.White, fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 7.dp))
                                    Text("Xem trước · áp dụng ảnh", color = CamColors.Sub, fontSize = 9.sp)
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            Text(
                if (category == "LUT") "LUT .cube dùng nội suy 3D khi lưu ảnh."
                else "Chọn bộ lọc để xem tông màu; bộ lọc được xử lý trên ảnh sau khi chụp.",
                color = CamColors.Sub, fontSize = 10.sp, modifier = Modifier.padding(top = 9.dp)
            )
        }
    }
}

private fun applyManualExposure(camera: Camera, iso: Int, exposureNs: Long) {
    val options = CaptureRequestOptions.Builder()
        .setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
        .setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
        .setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, iso)
        .setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureNs)
        .setCaptureRequestOption(CaptureRequest.SENSOR_FRAME_DURATION, maxOf(exposureNs, 10_000_000L))
        .build()
    Camera2CameraControl.from(camera.cameraControl).setCaptureRequestOptions(options)
}

private fun applyManualFocus(camera: Camera, diopters: Float) {
    val options = CaptureRequestOptions.Builder()
        .setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
        .setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
        .setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, diopters)
        .build()
    Camera2CameraControl.from(camera.cameraControl).setCaptureRequestOptions(options)
}

private fun applyAutoFocus(camera: Camera) {
    val options = CaptureRequestOptions.Builder()
        .setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
        .setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
        .build()
    Camera2CameraControl.from(camera.cameraControl).setCaptureRequestOptions(options)
}

private fun applyAutoWhiteBalance(camera: Camera) {
    val options = CaptureRequestOptions.Builder()
        .setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
        .setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
        .build()
    Camera2CameraControl.from(camera.cameraControl).setCaptureRequestOptions(options)
}

private fun applyManualWhiteBalance(camera: Camera, kelvin: Int) {
    // Approximate correlated color temperature with Camera2 sensor color-correction gains.
    val warmth = ((6500f - kelvin.coerceIn(2500, 8000)) / 4000f).coerceIn(-0.85f, 1f)
    val redGain = (1f + warmth * 0.42f).coerceIn(0.65f, 2.0f)
    val blueGain = (1f - warmth * 0.38f).coerceIn(0.65f, 2.0f)
    val gains = RggbChannelVector(redGain, 1f, 1f, blueGain)
    val options = CaptureRequestOptions.Builder()
        .setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
        .setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
        .setCaptureRequestOption(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_FAST)
        .setCaptureRequestOption(CaptureRequest.COLOR_CORRECTION_GAINS, gains)
        .build()
    Camera2CameraControl.from(camera.cameraControl).setCaptureRequestOptions(options)
}

private fun applyStabilization(camera: Camera, enabled: Boolean) {
    val options = CaptureRequestOptions.Builder()
        .setCaptureRequestOption(
            CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
            if (enabled) CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON
            else CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF
        ).build()
    Camera2CameraControl.from(camera.cameraControl).setCaptureRequestOptions(options)
}

private fun shutterFraction(value: Long, minValue: Long, maxValue: Long): Float {
    if (minValue <= 0L || maxValue <= minValue) return 0.5f
    val minLog = kotlin.math.ln(minValue.toDouble())
    val maxLog = kotlin.math.ln(maxValue.toDouble())
    return ((kotlin.math.ln(value.coerceIn(minValue, maxValue).toDouble()) - minLog) /
        (maxLog - minLog)).toFloat().coerceIn(0f, 1f)
}

private fun shutterFromFraction(value: Float, minValue: Long, maxValue: Long): Long {
    if (minValue <= 0L || maxValue <= minValue) return 10_000_000L
    val minLog = kotlin.math.ln(minValue.toDouble())
    val maxLog = kotlin.math.ln(maxValue.toDouble())
    return kotlin.math.exp(minLog + (maxLog - minLog) * value).toLong().coerceIn(minValue, maxValue)
}

@Composable
private fun cameraSliderColors() = SliderDefaults.colors(
    thumbColor = CamColors.Yellow,
    activeTrackColor = CamColors.Yellow,
    inactiveTrackColor = Color(0xFF3A3A3A)
)

@Composable
private fun MiniChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(7.dp))
        .background(if (selected) CamColors.Yellow else CamColors.Chip)
        .clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 7.dp)) {
        Text(label, color = if (selected) Color.Black else Color.White,
            fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Histogram(bins: IntArray, modifier: Modifier = Modifier) {
    Canvas(modifier.background(Color(0x66000000), RoundedCornerShape(6.dp))) {
        val maxValue = (bins.maxOrNull() ?: 1).coerceAtLeast(1)
        val barWidth = size.width / bins.size
        bins.forEachIndexed { index, value ->
            val height = size.height * value / maxValue
            drawRect(Color(0xCCFFFFFF), Offset(index * barWidth, size.height - height),
                DrawSize(barWidth, height))
        }
    }
}
