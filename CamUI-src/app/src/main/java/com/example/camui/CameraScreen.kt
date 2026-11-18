package com.example.camui

import android.Manifest
import android.content.ContentValues
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.ContentUris
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Point
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaActionSound
import android.os.Build
import android.os.BatteryManager
import android.os.PowerManager
import android.os.SystemClock
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import android.view.HapticFeedbackConstants
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
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BurstMode
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterCenterFocus
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.geometry.Size as DrawSize
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume


private tailrec fun Context.findHostActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> null
}

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

private val meteringModes = listOf("Center", "Matrix", "Spot")
private val wbModes = listOf(
    "Auto" to CaptureRequest.CONTROL_AWB_MODE_AUTO,
    "Daylight" to CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT,
    "Cloudy" to CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT,
    "Tungsten" to CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT,
    "Fluorescent" to CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT
)

private val bundledShutterSoundAssets = mapOf(
    "digital" to "shutter-sounds/mixkit-camera-digital-shutter-1432.mp3",
    "lens" to "shutter-sounds/mixkit-camera-lens-shutter-1433.mp3",
    "click" to "shutter-sounds/mixkit-camera-shutter-click-1133.mp3",
    "mechanical" to "shutter-sounds/mixkit-camera-shutter-hard-click-1430.mp3",
    "classic" to "shutter-sounds/mixkit-classic-camera-click-1440.mp3",
    "nikon" to "shutter-sounds/mixkit-nikon-camera-shutter-1434.mp3",
    "vintage" to "shutter-sounds/mixkit-vintage-camera-shutter-1438.mp3"
)

private val systemShutterSound = MediaActionSound().apply {
    load(MediaActionSound.SHUTTER_CLICK)
}

private fun playSystemShutterFallback() {
    runCatching { systemShutterSound.play(MediaActionSound.SHUTTER_CLICK) }
        .onFailure { android.util.Log.w("FUJI_SHUTTER", "System shutter fallback failed", it) }
}

private fun playConfiguredShutterSound(context: Context) {
    val prefs = context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE)
    val customUri = prefs.getString("shutter_sound_uri", null)?.takeIf { it.isNotBlank() }
    val player = MediaPlayer()
    val prepared = runCatching {
        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        if (customUri != null) {
            player.setDataSource(context, Uri.parse(customUri))
        } else {
            val selectedSound = prefs.getString("shutter_sound_asset", "classic") ?: "classic"
            val assetPath = bundledShutterSoundAssets[selectedSound] ?: bundledShutterSoundAssets.getValue("classic")
            val soundFile = requireNotNull(CameraAssetStore.file(context, assetPath)) {
                "Bundled shutter sound is missing: $assetPath"
            }
            player.setDataSource(soundFile.absolutePath)
        }
        player.setOnPreparedListener { mediaPlayer ->
            runCatching { mediaPlayer.start() }.onFailure {
                android.util.Log.w("FUJI_SHUTTER", "Could not start selected shutter audio", it)
                mediaPlayer.release()
                playSystemShutterFallback()
            }
        }
        player.setOnCompletionListener { it.release() }
        player.setOnErrorListener { mediaPlayer, what, extra ->
            android.util.Log.w("FUJI_SHUTTER", "Selected shutter audio failed: $what/$extra")
            mediaPlayer.release()
            playSystemShutterFallback()
            true
        }
        player.prepareAsync()
    }.isSuccess
    if (!prepared) {
        runCatching { player.release() }
        android.util.Log.w("FUJI_SHUTTER", "Custom shutter audio unavailable; using Android camera shutter sound")
        playSystemShutterFallback()
    }
}

/**
 * Saves one native JPEG burst frame. A failed frame only returns null; it does not
 * cancel the remaining burst. The caller spaces requests by a best-effort 200 ms.
 */
private suspend fun saveBurstFrame(
    context: Context,
    capture: ImageCapture,
    executor: java.util.concurrent.Executor,
    sessionId: String,
    frameIndex: Int,
    folder: String = "Burst",
    filePrefix: String = "FUJI_BURST"
): Uri? = suspendCancellableCoroutine { continuation ->
    val name = "${filePrefix}_${sessionId}_${(frameIndex + 1).toString().padStart(2, '0')}.jpg"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FujifilmMode/$folder")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    val output = runCatching {
        ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ).build()
    }.getOrElse {
        android.util.Log.w("FUJI_BURST", "Could not prepare frame ${frameIndex + 1}", it)
        if (continuation.isActive) continuation.resume(null)
        return@suspendCancellableCoroutine
    }
    capture.takePicture(output, executor, object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(results: ImageCapture.OutputFileResults) {
            val savedUri = results.savedUri
            if (savedUri != null) {
                runCatching {
                    context.contentResolver.update(
                        savedUri,
                        ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                        null,
                        null
                    )
                }
            }
            if (continuation.isActive) continuation.resume(savedUri)
        }

        override fun onError(exception: androidx.camera.core.ImageCaptureException) {
            android.util.Log.w("FUJI_BURST", "Frame ${frameIndex + 1} failed; continuing burst", exception)
            if (continuation.isActive) continuation.resume(null)
        }
    })
}

/** Select the nearest supported native JPEG size around the 12.5MP Photo target. */
private fun preferredPhotoJpegSize(
    context: Context,
    cameraId: String?,
    targetAspectRatio: Float,
    targetPixels: Long = 12_500_000L
): Size? {
    if (cameraId.isNullOrBlank()) return null
    val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return null
    val sizes = runCatching {
        manager.getCameraCharacteristics(cameraId)
            .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?.getOutputSizes(ImageFormat.JPEG)
            ?.toList()
    }.getOrNull().orEmpty().filter { it.width > 0 && it.height > 0 }
    if (sizes.isEmpty()) return null
    val lowerOrEqual = sizes.filter { it.width.toLong() * it.height.toLong() <= targetPixels }
    val candidates = if (lowerOrEqual.isNotEmpty()) lowerOrEqual else sizes
    return candidates.minWithOrNull(
        compareBy<Size> {
            kotlin.math.abs(it.width.toLong() * it.height.toLong() - targetPixels)
        }.thenBy {
            kotlin.math.abs(it.width.toFloat() / it.height.toFloat() - targetAspectRatio)
        }
    )
}

/** Chooses a real RAW_SENSOR output size near 50MP; never interpolates a DNG. */
private fun preferredPhotoRawSize(
    context: Context,
    cameraId: String?,
    targetAspectRatio: Float,
    targetPixels: Long = 50_000_000L
): Size? {
    if (cameraId.isNullOrBlank()) return null
    val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return null
    val sizes = runCatching {
        manager.getCameraCharacteristics(cameraId)
            .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?.getOutputSizes(ImageFormat.RAW_SENSOR)
            ?.toList()
    }.getOrNull().orEmpty().filter { it.width > 0 && it.height > 0 }
    if (sizes.isEmpty()) return null
    val atOrBelowTarget = sizes.filter { it.width.toLong() * it.height.toLong() <= targetPixels }
    val candidates = if (atOrBelowTarget.isNotEmpty()) atOrBelowTarget else sizes
    return candidates.minWithOrNull(
        compareBy<Size> {
            kotlin.math.abs(it.width.toLong() * it.height.toLong() - targetPixels)
        }.thenBy {
            kotlin.math.abs(it.width.toFloat() / it.height.toFloat() - targetAspectRatio)
        }
    )
}
@Composable
fun CameraScreen(
    onOpenSettings: () -> Unit,
    onOpenToolbox: () -> Unit,
    onOpenContentManagement: () -> Unit,
    onOpenAlbum: (Uri?) -> Unit,
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
    val configuration = LocalConfiguration.current
    val localView = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    var targetRotation by remember { mutableIntStateOf(Surface.ROTATION_0) }
    var cameraBindGeneration by remember { mutableIntStateOf(0) }
    // Initialize from current display, then follow physical rotation even if the window
    // configuration lags behind the camera sensor orientation.
    var isLandscape by remember { mutableStateOf(currentDisplayIsLandscape(context)) }
    // The camera controls stay in portrait layout; only the live preview and camera use-cases
    // follow the physical handset orientation reported by OrientationEventListener.
    val uiIsLandscape = false
    var orientationSensorAvailable by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        val orientationListener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                val nextRotation = surfaceRotationFromDeviceOrientation(orientation)
                if (targetRotation != nextRotation) targetRotation = nextRotation
                when (orientation) {
                    in 55..125, in 235..305 -> isLandscape = true
                    in 0..35, in 145..215, in 325..359 -> isLandscape = false
                }
            }
        }
        orientationSensorAvailable = orientationListener.canDetectOrientation()
        if (orientationSensorAvailable) orientationListener.enable()
        onDispose { orientationListener.disable() }
    }
    // Use display configuration only when the device has no usable orientation sensor.
    LaunchedEffect(configuration.orientation, orientationSensorAvailable) {
        if (!orientationSensorAvailable) {
            isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE ||
                currentDisplayIsLandscape(context)
        }
    }
    // Track camera use-case geometry so the shutter waits until the landscape binding is current.
    var boundCameraRotation by remember { mutableIntStateOf(targetRotation) }
    var boundCameraLandscape by remember { mutableStateOf(isLandscape) }
    var boundCameraRatioIdx by remember { mutableIntStateOf(ratioIdx) }
    val focusPeakingEnabled = rememberUpdatedState(focusPeaking)
    var faceTrackingEnabled by remember {
        mutableStateOf(context.getSharedPreferences("fujifilm_settings", 0)
            .getBoolean("face_tracking_enabled", false))
    }
    val faceTrackingEnabledState = rememberUpdatedState(faceTrackingEnabled)
    var faceTrackCenter by remember { mutableStateOf<Offset?>(null) }
    val sensorManager = remember(context) {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }
    val levelSensor = remember(sensorManager) {
        sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }
    var levelEnabled by remember {
        mutableStateOf(context.getSharedPreferences("fujifilm_settings", 0)
            .getBoolean("viewfinder_level_enabled", false))
    }
    var horizonRollDegrees by remember { mutableFloatStateOf(0f) }
    DisposableEffect(sensorManager, levelSensor, levelEnabled) {
        if (sensorManager == null || levelSensor == null || !levelEnabled) {
            onDispose { }
        } else {
            val rotationMatrix = FloatArray(9)
            val orientationAngles = FloatArray(3)
            val listener = object : SensorEventListener {
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

                override fun onSensorChanged(event: SensorEvent?) {
                    val values = event ?: return
                    val rollDegrees = if (values.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, values.values)
                        SensorManager.getOrientation(rotationMatrix, orientationAngles)
                        Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
                    } else {
                        Math.toDegrees(kotlin.math.atan2(
                            values.values[0].toDouble(),
                            values.values[1].toDouble()
                        )).toFloat()
                    }
                    horizonRollDegrees = ((rollDegrees + 180f) % 360f) - 180f
                }
            }
            sensorManager.registerListener(listener, levelSensor, SensorManager.SENSOR_DELAY_UI)
            onDispose { sensorManager.unregisterListener(listener) }
        }
    }
    var zebraEnabled by remember { mutableStateOf(context.getSharedPreferences("fujifilm_settings", 0).getBoolean("viewfinder_zebra_enabled", false)) }
    val zebraEnabledState = rememberUpdatedState(zebraEnabled)
    var rgbHistogramEnabled by remember { mutableStateOf(context.getSharedPreferences("fujifilm_settings", 0).getBoolean("viewfinder_rgb_histogram_enabled", false)) }
    val rgbHistogramEnabledState = rememberUpdatedState(rgbHistogramEnabled)

    var batteryPercent by remember { mutableIntStateOf(-1) }
    DisposableEffect(context) {
        fun updateBattery(intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            if (level >= 0 && scale > 0) {
                batteryPercent = (level * 100f / scale).roundToInt().coerceIn(0, 100)
            }
        }
        val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                updateBattery(intent)
            }
        }
        val sticky = ContextCompat.registerReceiver(
            context,
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        updateBattery(sticky)
        onDispose { runCatching { context.unregisterReceiver(batteryReceiver) } }
    }

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
    var livePhotoEnabled by remember {
        mutableStateOf(appPrefs.getBoolean("live_photo_enabled", false))
    }
    var nativeImageProcessingEnabled by remember {
        mutableStateOf(appPrefs.getBoolean("native_image_processing_enabled", false) && !livePhotoEnabled)
    }
    LaunchedEffect(livePhotoEnabled) {
        appPrefs.edit().putBoolean("live_photo_enabled", livePhotoEnabled).apply()
    }
    LaunchedEffect(nativeImageProcessingEnabled) {
        appPrefs.edit()
            .putBoolean("native_image_processing_enabled", nativeImageProcessingEnabled)
            .apply()
    }
    var lensCorrectionEnabled by remember {
        mutableStateOf(appPrefs.getBoolean("lens_correction_enabled", true))
    }
    var hudAutoHideEnabled by remember {
        mutableStateOf(appPrefs.getBoolean("hud_auto_hide_enabled", true))
    }
    var hudVisible by remember { mutableStateOf(true) }
    var hudActivityToken by remember { mutableIntStateOf(0) }
    var proHudShowEv by remember { mutableStateOf(appPrefs.getBoolean("pro_hud_show_ev", true)) }
    var proHudShowShutter by remember { mutableStateOf(appPrefs.getBoolean("pro_hud_show_shutter", true)) }
    var proHudShowIso by remember { mutableStateOf(appPrefs.getBoolean("pro_hud_show_iso", true)) }
    var proHudShowFocus by remember { mutableStateOf(appPrefs.getBoolean("pro_hud_show_focus", true)) }
    var proHudShowWhiteBalance by remember { mutableStateOf(appPrefs.getBoolean("pro_hud_show_white_balance", true)) }
    var mode by remember { mutableStateOf(appPrefs.getString("camera_mode", "Photo") ?: "Photo") }
    var burstMode by remember {
        mutableStateOf(appPrefs.getBoolean("burst_capture_mode_enabled", false))
    }
    var burstCaptureActive by remember { mutableStateOf(false) }
    var burstStopRequested by remember { mutableStateOf(false) }
    var burstCompletedFrames by remember { mutableIntStateOf(0) }
    var hdrBracketMode by remember {
        mutableStateOf(appPrefs.getBoolean("hdr_bracketing_enabled", false))
    }
    var focusBracketMode by remember {
        mutableStateOf(appPrefs.getBoolean("focus_bracketing_enabled", false))
    }
    var bracketCaptureActive by remember { mutableStateOf(false) }
    var bracketCompletedFrames by remember { mutableIntStateOf(0) }
    LaunchedEffect(hdrBracketMode, focusBracketMode) {
        appPrefs.edit()
            .putBoolean("hdr_bracketing_enabled", hdrBracketMode)
            .putBoolean("focus_bracketing_enabled", focusBracketMode)
            .apply()
    }
    LaunchedEffect(burstMode) {
        appPrefs.edit().putBoolean("burst_capture_mode_enabled", burstMode).apply()
    }
    var front by remember { mutableStateOf(appPrefs.getBoolean("camera_front_facing", false)) }
    var flash by remember {
        mutableIntStateOf(
            if (appPrefs.getInt("camera_flash_mode", ImageCapture.FLASH_MODE_OFF) == ImageCapture.FLASH_MODE_ON) {
                ImageCapture.FLASH_MODE_ON
            } else {
                ImageCapture.FLASH_MODE_OFF
            }
        )
    }
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
    LaunchedEffect(Unit) { appPrefs.edit().remove("camera_stabilization_enabled").apply() }
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
    var filterPreviewSourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val fallbackFilterPreviewBitmap = remember { createFilmLookPreviewSample() }
    var shutterEffectAlpha by remember { mutableFloatStateOf(0f) }
    val filmWashProgress = remember { Animatable(0f) }
    var frozenPreviewBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var previewFreezeEffect by remember { mutableStateOf("off") }
    var previewFreezeVisible by remember { mutableStateOf(false) }
    val previewFreezeAlpha = remember { Animatable(0f) }
    val previewEffectPosition = remember { Animatable(-0.35f) }
    val previewFreezeBlockInteraction = remember { MutableInteractionSource() }
    var isProcessing by remember { mutableStateOf(false) }
    var livePhotoCaptureInProgress by remember { mutableStateOf(false) }
    var processingProgress by remember { mutableIntStateOf(0) }
    var noiseReductionProgress by remember { mutableIntStateOf(0) }
    var processingLabel by remember { mutableStateOf("") }
    var processingEtaSeconds by remember { mutableIntStateOf(-1) }
    var multiFrameHoldActive by remember { mutableStateOf(false) }
    var multiFrameRenderStarted by remember { mutableStateOf(false) }
    var processingElapsedSeconds by remember { mutableIntStateOf(0) }
    var measuredBatteryTempC by remember { mutableIntStateOf(-1) }
    var thermalStatusCode by remember { mutableIntStateOf(PowerManager.THERMAL_STATUS_NONE) }
    var lunaDreamMode by remember {
        mutableStateOf(
            when (
                appPrefs.getString("lunadream_mode", null)
                    ?: if (appPrefs.getString("hdmp_render_profile", null) == "luna") "luna20" else "off"
            ) {
                "luna15" -> "luna15"
                "luna20", "luna" -> "luna20"
                else -> "off"
            }
        )
    }
    var processingPanelOffset by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(lunaDreamMode) {
        appPrefs.edit().putString("lunadream_mode", lunaDreamMode).apply()
    }
    LaunchedEffect(Unit) {
        appPrefs.edit()
            .remove("hdmp_render_profile")
            .remove("hdmp_output_mp")
            .putBoolean("uhd_capture_enabled", false)
            .putBoolean("fhd_capture_enabled", false)
            .putBoolean("uhd_plus_capture_enabled", false)
            .apply()
    }
    LaunchedEffect(isProcessing) {
        if (isProcessing) {
            val startedAt = SystemClock.elapsedRealtime()
            processingElapsedSeconds = 0
            while (isProcessing) {
                processingElapsedSeconds =
                    ((SystemClock.elapsedRealtime() - startedAt) / 1_000L).toInt().coerceAtLeast(0)
                delay(1_000L)
            }
        } else {
            processingElapsedSeconds = 0
        }
    }
    LaunchedEffect(previewFreezeEffect, isProcessing) {
        val activeEffect = previewFreezeEffect
        if (activeEffect == "off") {
            previewFreezeVisible = false
            previewFreezeAlpha.snapTo(0f)
            return@LaunchedEffect
        }
        if (!isProcessing) {
            previewFreezeAlpha.animateTo(
                0f,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            )
            previewFreezeVisible = false
            frozenPreviewBitmap = null
            previewFreezeEffect = "off"
            return@LaunchedEffect
        }
        previewFreezeVisible = true
        previewFreezeAlpha.snapTo(0f)
        previewFreezeAlpha.animateTo(
            1f,
            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
        )
        val passDuration = if (activeEffect == "night") 6_500 else 8_500
        while (isProcessing && previewFreezeEffect == activeEffect) {
            previewEffectPosition.snapTo(-0.35f)
            previewEffectPosition.animateTo(
                1.35f,
                animationSpec = tween(durationMillis = passDuration, easing = LinearEasing)
            )
        }
    }
    LaunchedEffect(isProcessing) {
        if (isProcessing) {
            while (isProcessing) {
                val batteryIntent = runCatching {
                    context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                }.getOrNull()
                val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
                measuredBatteryTempC = if (tempTenths > 0) tempTenths / 10 else -1
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                thermalStatusCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    runCatching { powerManager?.currentThermalStatus ?: PowerManager.THERMAL_STATUS_NONE }
                        .getOrDefault(PowerManager.THERMAL_STATUS_NONE)
                } else {
                    PowerManager.THERMAL_STATUS_NONE
                }
                delay(1_000L)
            }
        } else {
            measuredBatteryTempC = -1
            thermalStatusCode = PowerManager.THERMAL_STATUS_NONE
            processingPanelOffset = Offset.Zero
        }
    }
    LaunchedEffect(hudAutoHideEnabled, hudActivityToken, isProcessing) {
        if (!hudAutoHideEnabled) {
            hudVisible = true
            return@LaunchedEffect
        }
        hudVisible = true
        if (isProcessing) return@LaunchedEffect
        delay(5_000L)
        hudVisible = false
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
    var activePhysicalCameraId by remember { mutableStateOf<String?>(null) }
    var manualExposure by remember { mutableStateOf(appPrefs.getBoolean("pro_manual_exposure_enabled", false)) }
    var manualIso by remember { mutableIntStateOf(appPrefs.getInt("pro_manual_iso", 400).coerceAtLeast(50)) }
    var manualShutterNs by remember { mutableLongStateOf(appPrefs.getLong("pro_manual_shutter_ns", 10_000_000L).coerceAtLeast(1_000_000L)) }
    var manualFocus by remember { mutableFloatStateOf(appPrefs.getFloat("pro_manual_focus", 0f).coerceAtLeast(0f)) }
    val manualFocusState = rememberUpdatedState(manualFocus)
    val focusLockedState = rememberUpdatedState(focusLocked)
    var milkyWayInfinityFocusEnabled by remember { mutableStateOf(false) }
    var hist by remember { mutableStateOf(IntArray(64)) }
    var rgbRedHistogram by remember { mutableStateOf(IntArray(32)) }
    var rgbGreenHistogram by remember { mutableStateOf(IntArray(32)) }
    var rgbBlueHistogram by remember { mutableStateOf(IntArray(32)) }
    var peakPoints by remember { mutableStateOf(emptyList<Offset>()) }
    var zebraPoints by remember { mutableStateOf(emptyList<Offset>()) }
    var lastUri by remember { mutableStateOf<Uri?>(null) }
    var thumb by remember { mutableStateOf<ImageBitmap?>(null) }
    var viewerBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var viewerOriginalBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var viewerShowsOriginal by remember { mutableStateOf(false) }
    var photoViewerVisible by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCaptureUseCase by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var captureSetupReady by remember { mutableStateOf(false) }
    var previewUseCase by remember { mutableStateOf<Preview?>(null) }
    var analysisUseCase by remember { mutableStateOf<ImageAnalysis?>(null) }
    val imageQuality = appPrefs.getString("image_quality", "Best") ?: "Best"
    val noiseReduction = appPrefs.getString("noise_reduction", "Medium") ?: "Medium"
    val rawCaptureMode = when (appPrefs.getString("raw_capture_mode", "jpeg") ?: "jpeg") {
        "raw_plus", "raw_dream" -> "raw_dream"
        "raw" -> "raw"
        else -> "jpeg"
    }
    var uhdCaptureEnabled by remember { mutableStateOf(false) }
    var fhdCaptureEnabled by remember { mutableStateOf(false) }
    var uhdPlusCaptureEnabled by remember { mutableStateOf(false) }
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
    var nightShotCapturedFrameCount by remember { mutableIntStateOf(0) }
    var nightShotTargetFrameCount by remember { mutableIntStateOf(30) }
    var nightShotDurationSeconds by remember {
        mutableIntStateOf(appPrefs.getInt("nightshot_duration_seconds", 5).let { if (it in listOf(5, 10, 3)) it else 5 })
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
    var milkyWayStackEnabled by remember {
        mutableStateOf(appPrefs.getBoolean("milky_way_stack_enabled", false))
    }
    var showLunaDream by remember { mutableStateOf(false) }
    LaunchedEffect(
        mode, front, flash, timerSec, ultraHdr, renderEngine, metering,
        nightShotMode, nightShotFrameCount, nightShotDurationSeconds, starTrailExposureSeconds, rawCaptureMode,
        manualExposure, manualIsoEnabled, manualShutterEnabled, manualColorTempEnabled,
        manualColorTemp, manualIso, manualShutterNs, manualFocus, proSel, evIndex,
        milkyWayStackEnabled
    ) {
        appPrefs.edit()
            .putString("camera_mode", mode)
            .putBoolean("camera_front_facing", front)
            .putInt("camera_flash_mode", flash)
            .putInt("camera_timer_seconds", timerSec)
            .putBoolean("ultra_hdr_enabled", ultraHdr)
            .putString("camera_render_engine", renderEngine)
            .putInt("camera_metering_index", metering)
            .putString("nightshot_mode", nightShotMode)
            .putInt("nightshot_frame_count", nightShotFrameCount)
            .putInt("nightshot_duration_seconds", nightShotDurationSeconds)
            .putInt("star_trails_exposure_seconds", starTrailExposureSeconds)
            .putBoolean("pro_manual_exposure_enabled", manualExposure)
            .putBoolean("pro_manual_iso_enabled", manualIsoEnabled)
            .putBoolean("pro_manual_shutter_enabled", manualShutterEnabled)
            .putBoolean("pro_manual_wb_enabled", manualColorTempEnabled)
            .putInt("pro_manual_wb_kelvin", manualColorTemp)
            .putInt("pro_manual_iso", manualIso)
            .putLong("pro_manual_shutter_ns", manualShutterNs)
            .putFloat("pro_manual_focus", manualFocus)
            .putBoolean("milky_way_stack_enabled", milkyWayStackEnabled)
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

    // CameraX owns the preview transform. Portrait fills the selected frame; landscape
    // fits the complete sensor image so physical rotation never crops or zooms the preview.
    val previewView = remember(context, renderEngine) {
        PreviewView(context).apply {
            // Fit the entire sensor image into the selected frame; never crop or enlarge it.
            scaleType = PreviewView.ScaleType.FIT_CENTER
            setBackgroundColor(android.graphics.Color.rgb(38, 43, 51))
            // COMPATIBLE uses a TextureView-backed preview so PreviewView.bitmap can
            // preserve the exact last displayed frame during computational capture.
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    // Keep FIT_CENTER in both orientations so the preview never auto-crops or zooms.
    LaunchedEffect(previewView, nightShotCaptureInProgress, starTrailCaptureInProgress, burstCaptureActive, bracketCaptureActive, isProcessing) {
        previewView.keepScreenOn =
            nightShotCaptureInProgress || starTrailCaptureInProgress || burstCaptureActive || bracketCaptureActive || isProcessing
    }

    // Preview and JPEG share one CameraX ViewPort. A generation guard prevents delayed
    // ProcessCameraProvider callbacks from applying geometry from an earlier rotation.
    LaunchedEffect(
        hasCam, front, ratioIdx, ultraHdr, isLandscape, targetRotation, renderEngine,
        nativeImageProcessingEnabled, livePhotoEnabled, manualCaptureRevision, uhdCaptureEnabled, fhdCaptureEnabled, uhdPlusCaptureEnabled, hdrEnabled, nightShotMode,
        starTrailExposureSeconds, mode, burstMode, hdrBracketMode, focusBracketMode
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
        videoCaptureUseCase = null
        activeCameraDetails = null
        activePhysicalCameraDetails = emptyList()
        nightExtensionActive = false
        captureSetupReady = false
        preserveUltraHdrOutput = false
        previewUseCase = null
        analysisUseCase = null
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(providerListener@{
            try {
            if (bindGeneration != cameraBindGeneration) return@providerListener
            val provider = runCatching { future.get() }.getOrNull() ?: return@providerListener
            if (bindGeneration != cameraBindGeneration) return@providerListener
            val extensionsFuture = ExtensionsManager.getInstanceAsync(context, provider)
            extensionsFuture.addListener(extensionListener@{
                try {
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
            val rawDreamRequested = rawCaptureMode == "raw_dream" &&
                !nativeImageProcessingEnabled && !livePhotoEnabled
            val rawCaptureRequested = !nativeImageProcessingEnabled && !livePhotoEnabled &&
                mode == "Photo" && nightShotMode == "off" &&
                (lunaDreamMode == "off" || rawDreamRequested) &&
                !uhdCaptureEnabled && !fhdCaptureEnabled && !uhdPlusCaptureEnabled && rawCaptureMode != "jpeg"
            val requestedRawFormat = when (rawCaptureMode) {
                "raw" -> ImageCapture.OUTPUT_FORMAT_RAW
                "raw_dream" -> ImageCapture.OUTPUT_FORMAT_RAW_JPEG
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
                    "Camera này không hỗ trợ RAW/RawDream 2.0 đồng thời. Không thể dùng chế độ RAW đã chọn.",
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
            // Limited/Legacy devices use the JPEG_R path instead so histogram and metering remain available.
            val analysisCompatibleHardware = hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL ||
                hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3
            softwareHdrFallback = false
            // Prefer the OEM AUTO extension, then HDR, while Native Image Processing is active.
            // CameraX applies the selected vendor extension to Preview and ImageCapture.
            val nativeProcessingExtensionMode = if (
                nativeImageProcessingEnabled && mode == "Photo" && nightShotMode == "off" &&
                !rawCaptureActive && !burstMode && !hdrBracketMode && !focusBracketMode
            ) {
                listOf(ExtensionMode.AUTO, ExtensionMode.HDR).firstOrNull { extensionMode ->
                    runCatching {
                        extensionsManager?.isExtensionAvailable(baseSelector, extensionMode) == true
                    }.getOrDefault(false)
                }
            } else null
            val nativeProcessingExtensionSelector = nativeProcessingExtensionMode?.let { extensionMode ->
                runCatching {
                    extensionsManager!!.getExtensionEnabledCameraSelector(baseSelector, extensionMode)
                }.onFailure {
                    android.util.Log.w("FUJI_NATIVE_IMAGE", "OEM extension selector unavailable", it)
                }.getOrNull()
            }
            var nativeProcessingExtensionActive = nativeProcessingExtensionSelector != null
            val nativeProcessingAnalysisSupported = nativeProcessingExtensionMode?.let { extensionMode ->
                runCatching {
                    extensionsManager?.isImageAnalysisSupported(baseSelector, extensionMode) == true
                }.getOrDefault(false)
            } ?: true
            if (nativeImageProcessingEnabled && nativeProcessingExtensionActive) {
                android.util.Log.i(
                    "FUJI_NATIVE_IMAGE",
                    "Using OEM CameraX extension mode=" + nativeProcessingExtensionMode
                )
            } else if (nativeImageProcessingEnabled) {
                android.util.Log.i(
                    "FUJI_NATIVE_IMAGE",
                    "No OEM AUTO/HDR extension exposed; using the device camera's default ISP JPEG pipeline"
                )
            }
            // NightShot deliberately captures ordinary zero-EV auto-exposed frames and stacks them
            // in software. Do not enable the vendor NIGHT extension: some HALs turn it into a
            // single high-gain/long-shutter exposure, which is not the requested behaviour.
            nightExtensionActive = false
            val hdrCaptureRequested = !nativeImageProcessingEnabled && !livePhotoEnabled &&
                hdrEnabled && mode == "Photo" &&
                nightShotMode == "off" && !rawCaptureActive &&
                !(manualExposure && (manualIsoEnabled || manualShutterEnabled))
            // HDRbyHDMP is the common software path for all devices and HDMP levels.
            // Do not delegate to vendor HDR Extension here; it can bypass the dedicated engine.
            val hdrExtensionAvailable = false
            val hdrExtensionSelector = if (hdrExtensionAvailable) {
                runCatching {
                    extensionsManager!!.getExtensionEnabledCameraSelector(baseSelector, ExtensionMode.HDR)
                }.onFailure {
                    android.util.Log.w("FUJI_HDR", "HDR extension selector unavailable", it)
                }.getOrNull()
            } else null
            var hdrExtensionActive = hdrExtensionSelector != null
            var selector = nativeProcessingExtensionSelector ?: hdrExtensionSelector ?: baseSelector
            if (hdrCaptureRequested && !hdrExtensionActive) {
                android.util.Log.i("FUJI_HDR", "HDRbyHDMP software engine selected for this camera")
            }
            provider.unbindAll()
            camera = null
            imageCapture = null

            val rotation = targetRotation
            // Match CameraX's viewport to the selected preview frame in the current orientation.
            // FIT below preserves the entire sensor field of view with letterboxing where needed.
            val selectedHeightOverWidth = aspectRatioForOrientation(ratioIdx, isLandscape)
            val selectedWidthOverHeight = 1f / selectedHeightOverWidth
            val ratio = Rational((selectedWidthOverHeight * 10_000f).roundToInt(), 10_000)
            // Updated after CameraX binds so live capture results use the actual logical camera,
            // not the first camera ID returned by CameraManager.
            var activeCameraCharacteristics: CameraCharacteristics? = null
            var faceSensorToDisplayRotation = 0

            fun bindCapture(
                outputFormat: Int,
                livePhotoResolutionPixels: Long = 48_000_000L
            ) {
                var lastFaceTrackAtMs = 0L
                var lastFaceTrackCenter: Offset? = null
                val distortionModes = activeFacingId?.let { id ->
                    runCatching {
                        cameraManager.getCameraCharacteristics(id)
                            .get(CameraCharacteristics.DISTORTION_CORRECTION_AVAILABLE_MODES)
                            ?.toSet()
                    }.getOrDefault(emptySet())
                }.orEmpty()
                val distortionCorrectionMode = when {
                    nativeProcessingExtensionActive -> null
                    !lensCorrectionEnabled -> CaptureRequest.DISTORTION_CORRECTION_MODE_OFF
                    distortionModes.contains(CaptureRequest.DISTORTION_CORRECTION_MODE_HIGH_QUALITY) ->
                        CaptureRequest.DISTORTION_CORRECTION_MODE_HIGH_QUALITY
                    distortionModes.contains(CaptureRequest.DISTORTION_CORRECTION_MODE_FAST) ->
                        CaptureRequest.DISTORTION_CORRECTION_MODE_FAST
                    else -> null
                }
                val aberrationModes = activeFacingId?.let { id ->
                    runCatching {
                        cameraManager.getCameraCharacteristics(id)
                            .get(CameraCharacteristics.COLOR_CORRECTION_AVAILABLE_ABERRATION_MODES)
                            ?.toSet()
                    }.getOrDefault(emptySet())
                }.orEmpty()
                val aberrationCorrectionMode = when {
                    nativeProcessingExtensionActive -> null
                    !lensCorrectionEnabled -> CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE_OFF
                    aberrationModes.contains(CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY) ->
                        CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY
                    aberrationModes.contains(CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE_FAST) ->
                        CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE_FAST
                    else -> null
                }
                val shadingModes = activeFacingId?.let { id ->
                    runCatching {
                        cameraManager.getCameraCharacteristics(id)
                            .get(CameraCharacteristics.SHADING_AVAILABLE_MODES)
                            ?.toSet()
                    }.getOrDefault(emptySet())
                }.orEmpty()
                val shadingCorrectionMode = when {
                    nativeProcessingExtensionActive -> null
                    !lensCorrectionEnabled -> CaptureRequest.SHADING_MODE_OFF
                    shadingModes.contains(CaptureRequest.SHADING_MODE_HIGH_QUALITY) ->
                        CaptureRequest.SHADING_MODE_HIGH_QUALITY
                    shadingModes.contains(CaptureRequest.SHADING_MODE_FAST) ->
                        CaptureRequest.SHADING_MODE_FAST
                    else -> null
                }
                val previewBuilder = Preview.Builder().setTargetRotation(rotation)
                if (distortionCorrectionMode != null &&
                    distortionModes.contains(distortionCorrectionMode)
                ) {
                    Camera2Interop.Extender(previewBuilder)
                        .setCaptureRequestOption(CaptureRequest.DISTORTION_CORRECTION_MODE, distortionCorrectionMode)
                }
                if (aberrationCorrectionMode != null &&
                    aberrationModes.contains(aberrationCorrectionMode)
                ) {
                    Camera2Interop.Extender(previewBuilder)
                        .setCaptureRequestOption(CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE, aberrationCorrectionMode)
                }
                if (shadingCorrectionMode != null && shadingModes.contains(shadingCorrectionMode)) {
                    Camera2Interop.Extender(previewBuilder)
                        .setCaptureRequestOption(CaptureRequest.SHADING_MODE, shadingCorrectionMode)
                }
                if (!hdrExtensionActive && !nightExtensionActive && !nativeProcessingExtensionActive) Camera2Interop.Extender(previewBuilder).setSessionCaptureCallback(
                    object : CameraCaptureSession.CaptureCallback() {
                        override fun onCaptureCompleted(
                            session: CameraCaptureSession,
                            request: CaptureRequest,
                            result: TotalCaptureResult
                        ) {
                            val live = Camera2Info.readLive(result, activeCameraCharacteristics)
                            if (faceTrackingEnabledState.value) {
                                val detectedFaces = result.get(CaptureResult.STATISTICS_FACES)
                                val bestFace = detectedFaces
                                    ?.filter { it.score >= 50 }
                                    ?.maxByOrNull { it.bounds.width().toLong() * it.bounds.height().toLong() }
                                val crop = result.get(CaptureResult.SCALER_CROP_REGION)
                                    ?: activeCameraCharacteristics?.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
                                if (bestFace != null && crop != null && crop.width() > 0 && crop.height() > 0) {
                                    val faceBounds = bestFace.bounds
                                    val normalizedX = ((faceBounds.centerX() - crop.left).toFloat() /
                                        crop.width()).coerceIn(0f, 1f)
                                    val normalizedY = ((faceBounds.centerY() - crop.top).toFloat() /
                                        crop.height()).coerceIn(0f, 1f)
                                    val orientedCenter = when (faceSensorToDisplayRotation) {
                                        90 -> Offset(1f - normalizedY, normalizedX)
                                        180 -> Offset(1f - normalizedX, 1f - normalizedY)
                                        270 -> Offset(normalizedY, 1f - normalizedX)
                                        else -> Offset(normalizedX, normalizedY)
                                    }.let { point ->
                                        if (front) Offset(1f - point.x, point.y) else point
                                    }
                                    val now = SystemClock.elapsedRealtime()
                                    val previous = lastFaceTrackCenter
                                    val movedEnough = previous == null ||
                                        kotlin.math.abs(previous.x - orientedCenter.x) +
                                        kotlin.math.abs(previous.y - orientedCenter.y) > 0.055f
                                    if (movedEnough || now - lastFaceTrackAtMs >= 1_200L) {
                                        lastFaceTrackAtMs = now
                                        lastFaceTrackCenter = orientedCenter
                                        scope.launch(Dispatchers.Main) {
                                            faceTrackCenter = orientedCenter
                                            val previewWidth = previewView.width
                                            val previewHeight = previewView.height
                                            if (previewWidth > 0 && previewHeight > 0 &&
                                                !focusLockedState.value && manualFocusState.value <= 0f
                                            ) {
                                                runCatching {
                                                    val point = previewView.meteringPointFactory.createPoint(
                                                        orientedCenter.x * previewWidth,
                                                        orientedCenter.y * previewHeight,
                                                        0.18f
                                                    )
                                                    val action = FocusMeteringAction.Builder(
                                                        point,
                                                        FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                                                    ).setAutoCancelDuration(1, TimeUnit.SECONDS).build()
                                                    camera?.cameraControl?.startFocusAndMetering(action)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    lastFaceTrackCenter = null
                                    scope.launch(Dispatchers.Main) { faceTrackCenter = null }
                                }
                            } else {
                                lastFaceTrackCenter = null
                                scope.launch(Dispatchers.Main) { faceTrackCenter = null }
                            }
                            live.activePhysicalCameraId?.let { id ->
                                if (activePhysicalCameraId != id) activePhysicalCameraId = id
                            }
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
                    .also { builder ->
                        if (distortionCorrectionMode != null &&
                            distortionModes.contains(distortionCorrectionMode)
                        ) {
                            Camera2Interop.Extender(builder)
                                .setCaptureRequestOption(CaptureRequest.DISTORTION_CORRECTION_MODE, distortionCorrectionMode)
                        }
                        if (aberrationCorrectionMode != null &&
                            aberrationModes.contains(aberrationCorrectionMode)
                        ) {
                            Camera2Interop.Extender(builder)
                                .setCaptureRequestOption(CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE, aberrationCorrectionMode)
                        }
                        if (shadingCorrectionMode != null && shadingModes.contains(shadingCorrectionMode)) {
                            Camera2Interop.Extender(builder)
                                .setCaptureRequestOption(CaptureRequest.SHADING_MODE, shadingCorrectionMode)
                        }
                    }
                    .setCaptureMode(
                        if (mode == "Photo" && (nightShotMode == "night" || burstMode)) ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                        else ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
                    )
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
                if (!hdrExtensionActive && !nightExtensionActive && !nativeProcessingExtensionActive && nightShotMode == "off" &&
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
                val lunaDreamRequestedInPrefs = !nativeImageProcessingEnabled && !livePhotoEnabled &&
                    appPrefs.getString("lunadream_mode", "off") != "off"
                val isComputationalHighResolution = !livePhotoEnabled &&
                    (lunaDreamRequestedInPrefs || uhdCaptureEnabled || fhdCaptureEnabled ||
                        uhdPlusCaptureEnabled || (mode == "Photo" && nightShotMode == "night"))
                val isOrdinaryPhotoJpeg = mode == "Photo" &&
                    (nightShotMode == "off" || livePhotoEnabled) &&
                    !rawCaptureActive && !isComputationalHighResolution
                if (rawCaptureActive) {
                    val rawAspect = ratio.toFloat().let { if (it < 1f) 1f / it else it }
                    val nativeRawSize = preferredPhotoRawSize(context, activeFacingId, rawAspect)
                    if (nativeRawSize != null) {
                        captureBuilder.setResolutionSelector(
                            ResolutionSelector.Builder()
                                .setResolutionStrategy(
                                    ResolutionStrategy(
                                        nativeRawSize,
                                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                                    )
                                )
                                .build()
                        )
                        val rawMp = nativeRawSize.width.toLong() * nativeRawSize.height.toLong() / 1_000_000L
                        android.util.Log.i("FUJI_RAW", "Selected native RAW_SENSOR ${nativeRawSize.width}x${nativeRawSize.height} (~${rawMp}MP)")
                        if (rawMp < 48L) {
                            android.util.Log.w("FUJI_RAW", "Native RAW output is below 50MP; no DNG interpolation will be applied")
                        }
                    } else {
                        android.util.Log.w("FUJI_RAW", "No RAW_SENSOR output size exposed by this camera; keeping CameraX native RAW default")
                    }
                } else if (isOrdinaryPhotoJpeg) {
                    val nativePhotoAspect = ratio.toFloat().let { if (it < 1f) 1f / it else it }
                    val nativePhotoSize = preferredPhotoJpegSize(
                        context,
                        activeFacingId,
                        nativePhotoAspect,
                        targetPixels = if (livePhotoEnabled) livePhotoResolutionPixels else 12_500_000L
                    )
                    if (nativePhotoSize != null) {
                        captureBuilder.setResolutionSelector(
                            ResolutionSelector.Builder()
                                .setResolutionStrategy(
                                    ResolutionStrategy(
                                        nativePhotoSize,
                                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                                    )
                                )
                                .build()
                        )
                        android.util.Log.i(
                            "FUJI_PHOTO",
                            "Ordinary Photo selected native JPEG " + nativePhotoSize.width + "x" + nativePhotoSize.height
                        )
                    } else {
                        android.util.Log.w("FUJI_PHOTO", "12.5MP JPEG size unavailable; using CameraX default")
                    }
                } else if (imageQuality == "Best" || isComputationalHighResolution) {
                    captureBuilder.setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                            .build()
                    )
                }
                // Off/Low/Medium/High use only the active camera's ISP noise reduction.
                // LunaDream uses multi-frame fusion; the camera ISP may still apply its standard noise reduction.
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
                // Torch illumination is independent from still-capture flash; do not strobe on shutter.
                capture.flashMode = ImageCapture.FLASH_MODE_OFF

                val livePhotoVideoCapture = if (livePhotoEnabled) {
                    val recorder = Recorder.Builder()
                        .setQualitySelector(
                            QualitySelector.from(
                                Quality.HD,
                                FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
                            )
                        )
                        .build()
                    VideoCapture.withOutput(recorder).also { it.targetRotation = rotation }
                } else null

                val analysis = ImageAnalysis.Builder()
                    .setTargetRotation(rotation)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                previewUseCase = preview
                analysisUseCase = if (livePhotoVideoCapture == null &&
                    (!nativeProcessingExtensionActive || nativeProcessingAnalysisSupported)
                ) analysis else null
                var lastAnalysis = 0L
                var lastLensCheckAt = 0L
                var consecutiveLensWarnings = 0
                var lastLensWarningAt = 0L
                analysis.setAnalyzer(analysisExecutor) { image ->
                    try {
                        val now = System.currentTimeMillis()
                        if (now - lastAnalysis > 140) {
                            lastAnalysis = now
                            if (now - lastLensCheckAt >= 2_000L) {
                                lastLensCheckAt = now
                                if (LensDirtDetector.mayBeObscured(image)) consecutiveLensWarnings++
                                else consecutiveLensWarnings = 0
                                if (consecutiveLensWarnings >= 4 && now - lastLensWarningAt >= 45_000L) {
                                    consecutiveLensWarnings = 0
                                    lastLensWarningAt = now
                                    mainExecutor.execute {
                                        localizedToast(
                                            context,
                                            "Hình xem trước có dấu hiệu mờ. Có thể ống kính bị bẩn hoặc che khuất; hãy kiểm tra và lau kính camera.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                            val plane = image.planes[0]
                            val buffer = plane.buffer
                            val bins = IntArray(64)
                            val redBins = IntArray(32)
                            val greenBins = IntArray(32)
                            val blueBins = IntArray(32)
                            val uPlane = image.planes.getOrNull(1)
                            val vPlane = image.planes.getOrNull(2)
                            val uBuffer = if (rgbHistogramEnabledState.value) uPlane?.buffer else null
                            val vBuffer = if (rgbHistogramEnabledState.value) vPlane?.buffer else null
                            val edges = ArrayList<Offset>(250)
                            val overexposed = ArrayList<Offset>(700)
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
                                    if (rgbHistogramEnabledState.value && uBuffer != null && vBuffer != null &&
                                        uPlane != null && vPlane != null
                                    ) {
                                        val chromaX = x / 2
                                        val chromaY = y / 2
                                        val uIndex = chromaY * uPlane.rowStride + chromaX * uPlane.pixelStride
                                        val vIndex = chromaY * vPlane.rowStride + chromaX * vPlane.pixelStride
                                        if (uIndex in 0 until uBuffer.limit() && vIndex in 0 until vBuffer.limit()) {
                                            val u = (uBuffer.get(uIndex).toInt() and 0xFF) - 128
                                            val v = (vBuffer.get(vIndex).toInt() and 0xFF) - 128
                                            val red = (value + 1.402f * v).roundToInt().coerceIn(0, 255)
                                            val green = (value - 0.344136f * u - 0.714136f * v).roundToInt().coerceIn(0, 255)
                                            val blue = (value + 1.772f * u).roundToInt().coerceIn(0, 255)
                                            redBins[red shr 3]++
                                            greenBins[green shr 3]++
                                            blueBins[blue shr 3]++
                                        }
                                    }
                                    luminanceSum += value
                                    sampleCount++
                                    if (zebraEnabledState.value && value >= 242 && ((x + y) / 8) % 2 == 0 &&
                                        overexposed.size < 700
                                    ) {
                                        val px = x.toFloat() / image.width
                                        val py = y.toFloat() / image.height
                                        val oriented = when (image.imageInfo.rotationDegrees) {
                                            90 -> Offset(1f - py, px)
                                            180 -> Offset(1f - px, 1f - py)
                                            270 -> Offset(py, 1f - px)
                                            else -> Offset(px, py)
                                        }
                                        overexposed.add(if (front) Offset(1f - oriented.x, oriented.y) else oriented)
                                    }
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
                                rgbRedHistogram = if (rgbHistogramEnabledState.value) redBins else IntArray(32)
                                rgbGreenHistogram = if (rgbHistogramEnabledState.value) greenBins else IntArray(32)
                                rgbBlueHistogram = if (rgbHistogramEnabledState.value) blueBins else IntArray(32)
                                peakPoints = if (focusPeakingEnabled.value) edges else emptyList()
                                zebraPoints = if (zebraEnabledState.value) overexposed else emptyList()
                                lowLightDetected = meanLuma < 52
                            }
                        }

                    } catch (analysisError: Exception) {
                        android.util.Log.w(
                            "FUJI_ANALYSIS",
                            "Skipping an invalid preview-analysis frame",
                            analysisError
                        )
                    } finally {
                        image.close()
                    }
                }

                // Use the exact ratio used by the Compose preview container. Reading
                // PreviewView.width/height here can pick up the previous layout during rotation.
                val viewPort = ViewPort.Builder(ratio, rotation)
                    .setScaleType(ViewPort.FIT)
                    .build()
                val groupBuilder = UseCaseGroup.Builder()
                    .setViewPort(viewPort)
                    .addUseCase(preview)
                    .addUseCase(capture)
                if (livePhotoVideoCapture != null) {
                    groupBuilder.addUseCase(livePhotoVideoCapture)
                } else if (!nativeProcessingExtensionActive || nativeProcessingAnalysisSupported) {
                    groupBuilder.addUseCase(analysis)
                }
                val group = groupBuilder.build()
                val bound = provider.bindToLifecycle(lifecycleOwner, selector, group)
                camera = bound
                imageCapture = capture
                videoCaptureUseCase = livePhotoVideoCapture

                val actualCamera2Info = runCatching {
                    Camera2CameraInfo.from(bound.cameraInfo)
                }.getOrNull()
                val actualCameraId = actualCamera2Info?.cameraId
                activeCameraCharacteristics = actualCameraId?.let { id ->
                    runCatching { cameraManager.getCameraCharacteristics(id) }.getOrNull()
                }
                faceSensorToDisplayRotation = runCatching {
                    bound.cameraInfo.getSensorRotationDegrees(rotation)
                }.getOrDefault(0)
                val availableFaceModes = activeCameraCharacteristics?.get(
                    CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES
                ) ?: intArrayOf()
                val faceDetectMode = when {
                    availableFaceModes.contains(CaptureRequest.STATISTICS_FACE_DETECT_MODE_FULL) ->
                        CaptureRequest.STATISTICS_FACE_DETECT_MODE_FULL
                    availableFaceModes.contains(CaptureRequest.STATISTICS_FACE_DETECT_MODE_SIMPLE) ->
                        CaptureRequest.STATISTICS_FACE_DETECT_MODE_SIMPLE
                    else -> null
                }
                if (faceTrackingEnabled && faceDetectMode != null) {
                    runCatching {
                        val faceOptions = CaptureRequestOptions.Builder()
                            .setCaptureRequestOption(CaptureRequest.STATISTICS_FACE_DETECT_MODE, faceDetectMode)
                            .build()
                        Camera2CameraControl.from(bound.cameraControl).setCaptureRequestOptions(faceOptions)
                    }.onFailure {
                        android.util.Log.w("FUJI_FACE_TRACK", "Could not enable Camera2 face detection", it)
                    }
                } else {
                    faceTrackCenter = null
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
                val maxSelectableZoom = Camera2Info.maxSelectableZoom(
                    activeCameraDetails,
                    activePhysicalCameraDetails,
                    zoomStateOnBind?.maxZoomRatio ?: 1f
                )
                val restoredZoom = savedZoom.coerceIn(
                    zoomStateOnBind?.minZoomRatio ?: 1f,
                    maxSelectableZoom
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
                // Preserve user-selected WB and focus intent across logical-camera rebinds.
                if (manualColorTempEnabled) applyManualWhiteBalance(bound, manualColorTemp)
                else applyAutoWhiteBalance(bound)
                val focusModes = runCatching {
                    Camera2CameraInfo.from(bound.cameraInfo)
                        .getCameraCharacteristic(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
                }.getOrNull()
                val focusDistance = activeCameraDetails?.minFocusDiopter ?: 0f
                if (focusDistance > 0f && focusModes?.contains(CameraCharacteristics.CONTROL_AF_MODE_OFF) == true) {
                    when {
                        milkyWayInfinityFocusEnabled -> runCatching { applyManualFocus(bound, 0f) }
                        manualFocus > 0f -> runCatching {
                            applyManualFocus(bound, manualFocus.coerceIn(0f, focusDistance))
                        }
                    }
                }
            }

            // When UHD sharpening is requested, prefer OEM HDR JPEG (or software HDR)
            // so the post-capture 2x sharpening can still run. Explicit Ultra HDR remains
            // higher priority because its gain map must be preserved without recompression.
            val preferJpegR = !rawCaptureActive && !livePhotoEnabled &&
                ultraHdr &&
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
                if (nativeProcessingExtensionActive) {
                    android.util.Log.w(
                        "FUJI_NATIVE_IMAGE",
                        "OEM extension bind failed; retrying with the device's default ISP pipeline",
                        e
                    )
                    nativeProcessingExtensionActive = false
                    selector = baseSelector
                    provider.unbindAll()
                    val fallbackBound = runCatching { bindCapture(ImageCapture.OUTPUT_FORMAT_JPEG) }
                    if (fallbackBound.isSuccess && camera != null && imageCapture != null) {
                        localizedToast(
                            context,
                            "Camera OEM Extension không khởi chạy được; đang dùng pipeline ISP mặc định của thiết bị.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        camera = null
                        imageCapture = null
                        localizedToast(
                            context,
                            "Không thể khởi tạo camera theo pipeline gốc của thiết bị.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else if (hdrExtensionActive) {
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
                } else if (livePhotoEnabled) {
                    android.util.Log.w(
                        "FUJI_LIVE_PHOTO",
                        "48MP still + video combination failed; retrying Live Photo at 12.5MP",
                        e
                    )
                    provider.unbindAll()
                    val livePhotoRetry = runCatching {
                        bindCapture(ImageCapture.OUTPUT_FORMAT_JPEG, 12_500_000L)
                    }
                    if (livePhotoRetry.isSuccess && camera != null &&
                        imageCapture != null && videoCaptureUseCase != null
                    ) {
                        localizedToast(
                            context,
                            "Live Photo dùng 12.5MP trên tổ hợp camera hiện tại để giữ được clip chuyển động.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        provider.unbindAll()
                        livePhotoEnabled = false
                        appPrefs.edit().putBoolean("live_photo_enabled", false).apply()
                        videoCaptureUseCase = null
                        val normalRetry = runCatching {
                            bindCapture(ImageCapture.OUTPUT_FORMAT_JPEG, 12_500_000L)
                        }
                        if (normalRetry.isFailure) {
                            camera = null
                            imageCapture = null
                            videoCaptureUseCase = null
                        }
                        localizedToast(
                            context,
                            "Camera này không hỗ trợ Live Photo đồng thời với ảnh tĩnh; đã tắt Live Photo và khôi phục chế độ chụp thường.",
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
            captureSetupReady = camera != null && imageCapture != null &&
                (!livePhotoEnabled || videoCaptureUseCase != null)
                } catch (cameraInitError: Exception) {
                    android.util.Log.e(
                        "FUJI_STARTUP",
                        "CameraX startup callback failed; keeping FujiFilm Mode open",
                        cameraInitError
                    )
                    runCatching { provider.unbindAll() }
                    camera = null
                    imageCapture = null
                    videoCaptureUseCase = null
                    previewUseCase = null
                    analysisUseCase = null
                    captureSetupReady = false
                    runCatching {
                        localizedToast(
                            context,
                            "Không thể khởi tạo camera. Giao diện vẫn được giữ mở: ${cameraInitError.message ?: "lỗi thiết bị"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }, mainExecutor)
            } catch (providerInitError: Exception) {
                android.util.Log.e(
                    "FUJI_STARTUP",
                    "CameraX provider initialization failed; keeping FujiFilm Mode open",
                    providerInitError
                )
                camera = null
                imageCapture = null
                captureSetupReady = false
                runCatching {
                    localizedToast(
                        context,
                        "Không thể khởi tạo dịch vụ camera: ${providerInitError.message ?: "lỗi thiết bị"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }, mainExecutor)
    }

    LaunchedEffect(flash, camera, front) {
        val activeCamera = camera ?: return@LaunchedEffect
        val hasTorch = !front && activeCamera.cameraInfo.hasFlashUnit()
        if (flash == ImageCapture.FLASH_MODE_ON && !hasTorch) {
            flash = ImageCapture.FLASH_MODE_OFF
            if (!front) {
                localizedToast(context, "Camera đang chọn không có đèn LED hỗ trợ Torch.", Toast.LENGTH_LONG).show()
            }
            return@LaunchedEffect
        }
        runCatching {
            activeCamera.cameraControl.enableTorch(flash == ImageCapture.FLASH_MODE_ON && hasTorch)
        }.onFailure {
            android.util.Log.w("FUJI_TORCH", "Could not change continuous LED torch state", it)
            if (flash == ImageCapture.FLASH_MODE_ON) {
                flash = ImageCapture.FLASH_MODE_OFF
                localizedToast(context, "Không thể bật đèn LED liên tục trên camera này.", Toast.LENGTH_LONG).show()
            }
        }
    }
    DisposableEffect(camera) {
        val activeCamera = camera
        onDispose {
            runCatching { activeCamera?.cameraControl?.enableTorch(false) }
        }
    }
    LaunchedEffect(imageCapture) {
        // The shutter must never pulse the LED: illumination is controlled only by Torch Mode.
        imageCapture?.flashMode = ImageCapture.FLASH_MODE_OFF
    }
    LaunchedEffect(targetRotation, imageCapture, previewUseCase, analysisUseCase, videoCaptureUseCase) {
        imageCapture?.targetRotation = targetRotation
        previewUseCase?.targetRotation = targetRotation
        analysisUseCase?.targetRotation = targetRotation
        videoCaptureUseCase?.targetRotation = targetRotation
        // The viewfinder layout stays portrait; this only marks the camera's latest sensor rotation as ready.
        boundCameraRotation = targetRotation
        boundCameraLandscape = isLandscape
        boundCameraRatioIdx = ratioIdx
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
        if (!photoViewerVisible) {
            viewerOriginalBitmap = null
            viewerShowsOriginal = false
            return@LaunchedEffect
        }
        val uri = lastUri ?: return@LaunchedEffect
        val previews = withContext(Dispatchers.IO) {
            val processed = runCatching {
                // Decode EXIF into the pixels instead of trusting a stale MediaStore thumbnail.
                decodeUprightBitmap(context, uri, maxDimension = 2048)?.asImageBitmap()
            }.getOrNull()
            val original: ImageBitmap? = null
            processed to original
        }
        viewerBitmap = previews.first
        viewerOriginalBitmap = previews.second
        viewerShowsOriginal = false
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
            boundCameraLandscape != isLandscape ||
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
        localView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        activeCapture.targetRotation = captureRotation
        previewUseCase?.targetRotation = captureRotation
        analysisUseCase?.targetRotation = captureRotation
        if (appPrefs.getBoolean("shutter_sound_enabled", true)) {
            playConfiguredShutterSound(context)
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
                            val unfilteredPreview = runCatching {
                                decodeUprightBitmap(context, starUri, maxDimension = 640)
                            }.getOrNull()
                            if (unfilteredPreview != null) {
                                withContext(Dispatchers.Main) { filterPreviewSourceBitmap = unfilteredPreview }
                            }
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
                                    if (manualSensorSupported) effectiveIso.toString() else "AUTO",
                                    preservePhotoAspectRatio = true
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
                activeCapture.flashMode = ImageCapture.FLASH_MODE_OFF
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
        val rawModeConflictsWithComputationalCapture = !nativeImageProcessingEnabled && (
            (mode == "Photo" && rawCaptureMode == "raw" &&
                (nightShotMode != "off" || lunaDreamMode != "off" || uhdCaptureEnabled || fhdCaptureEnabled ||
                    uhdPlusCaptureEnabled || burstMode || hdrBracketMode || focusBracketMode)) ||
            (mode == "Photo" && rawCaptureMode == "raw_dream" &&
                (nightShotMode != "off" || uhdCaptureEnabled || fhdCaptureEnabled ||
                    uhdPlusCaptureEnabled || burstMode || hdrBracketMode || focusBracketMode))
        )
        if (!nativeImageProcessingEnabled && rawCaptureMode == "raw_dream" &&
            imageCapture?.outputFormat != ImageCapture.OUTPUT_FORMAT_RAW_JPEG
        ) {
            localizedToast(
                context,
                "Camera không hỗ trợ RAW + JPEG đồng thời. RawDream 2.0 chưa chụp; ảnh không bị chuyển âm thầm sang JPEG thường.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        if (rawModeConflictsWithComputationalCapture) {
            localizedToast(
                context,
                "RAW không kết hợp với LunaDream, NightShotMode, Burst hoặc Bracketing. Hãy tắt chế độ tính toán hoặc chuyển định dạng về JPEG.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        if (bracketCaptureActive) {
            localizedToast(context, "Đang hoàn tất chuỗi Bracketing hiện tại…", Toast.LENGTH_SHORT).show()
            return
        }
        if (hdrBracketMode && focusBracketMode) {
            localizedToast(context, "Chọn HDR Bracketing hoặc Focus Bracketing, không bật đồng thời.", Toast.LENGTH_LONG).show()
            return
        }
        if (!livePhotoEnabled && (hdrBracketMode || focusBracketMode) &&
            mode == "Photo" && nightShotMode == "off" &&
            rawCaptureMode == "jpeg" && lunaDreamMode == "off" &&
            !uhdCaptureEnabled && !fhdCaptureEnabled && !uhdPlusCaptureEnabled
        ) {
            if (!captureSetupReady || camera == null || imageCapture == null || isProcessing ||
                nightShotCaptureInProgress || starTrailCaptureInProgress || burstCaptureActive
            ) return
            val activeCamera = camera ?: return
            val activeCapture = imageCapture ?: return
            val cameraInfo = runCatching { Camera2CameraInfo.from(activeCamera.cameraInfo) }.getOrNull()
            val minimumFocusDistance = cameraInfo?.getCameraCharacteristic(
                CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE
            ) ?: 0f
            val afModes = cameraInfo?.getCameraCharacteristic(
                CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES
            )
            val exposureRange = activeCamera.cameraInfo.exposureState.exposureCompensationRange
            val exposureStep = activeCamera.cameraInfo.exposureState.exposureCompensationStep.toFloat()
            if (focusBracketMode && (minimumFocusDistance <= 0f ||
                afModes?.contains(CameraCharacteristics.CONTROL_AF_MODE_OFF) != true)
            ) {
                localizedToast(context, "Camera không công bố hỗ trợ lấy nét thủ công để Focus Bracketing.", Toast.LENGTH_LONG).show()
                return
            }
            if (hdrBracketMode && (exposureStep <= 0f || exposureRange.lower == exposureRange.upper)) {
                localizedToast(context, "Camera không công bố dải bù trừ phơi sáng cho HDR Bracketing.", Toast.LENGTH_LONG).show()
                return
            }
            localView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            val useHdrBracket = hdrBracketMode
            scope.launch {
                bracketCaptureActive = true
                bracketCompletedFrames = 0
                val bracketSavedUris = ArrayList<Uri>()
                val sessionId = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
                val originalEv = activeCamera.cameraInfo.exposureState.exposureCompensationIndex
                var focusRestoreRequired = false
                var bracketTargetFrames = if (useHdrBracket) 3 else 5
                try {
                    if (timerSec > 0) {
                        for (seconds in timerSec downTo 1) {
                            countdown = seconds
                            delay(1000)
                        }
                        countdown = 0
                    }
                    val positions: List<Float> = if (useHdrBracket) {
                        listOf(-2f, 0f, 2f).map { ev ->
                            val offset = (ev / exposureStep).roundToInt()
                            (originalEv + offset).coerceIn(exposureRange.lower, exposureRange.upper).toFloat()
                        }.distinct()
                    } else {
                        listOf(
                            minimumFocusDistance,
                            minimumFocusDistance * 0.75f,
                            minimumFocusDistance * 0.50f,
                            minimumFocusDistance * 0.25f,
                            0f
                        )
                    }
                    bracketTargetFrames = positions.size
                    for ((index, value) in positions.withIndex()) {
                        if (useHdrBracket) {
                            activeCamera.cameraControl.setExposureCompensationIndex(value.roundToInt())
                            delay(280)
                        } else {
                            focusRestoreRequired = true
                            val focused = runCatching { applyManualFocus(activeCamera, value) }.isSuccess
                            if (!focused) {
                                localizedToast(context, "Không thể đặt khoảng nét ${index + 1}; dừng Focus Bracketing.", Toast.LENGTH_LONG).show()
                                break
                            }
                            delay(420)
                        }
                        val uri = runCatching {
                            saveBurstFrame(
                                context = context,
                                capture = activeCapture,
                                executor = mainExecutor,
                                sessionId = sessionId,
                                frameIndex = index,
                                folder = if (useHdrBracket) "HDR-Bracketing" else "Focus-Bracketing",
                                filePrefix = if (useHdrBracket) "FUJI_HDR_BRACKET" else "FUJI_FOCUS_BRACKET"
                            )
                        }.onFailure {
                            android.util.Log.w("FUJI_BRACKET", "Bracket frame ${index + 1} failed; continuing", it)
                        }.getOrNull()
                        if (uri != null) { bracketSavedUris += uri; bracketCompletedFrames++ }
                    }
                } finally {
                    if (useHdrBracket) {
                        runCatching { activeCamera.cameraControl.setExposureCompensationIndex(originalEv) }
                    }
                    if (focusRestoreRequired) {
                        runCatching { applyAutoFocus(activeCamera) }
                        manualFocus = 0f
                        milkyWayInfinityFocusEnabled = false
                    }
                    countdown = 0
                    bracketCaptureActive = false
                }
                var mergedBracketUri: Uri? = null
                if (bracketSavedUris.size >= 2) {
                    isProcessing = true
                    processingProgress = 0
                    processingLabel = if (useHdrBracket) "Đang ghép HDR Bracketing" else "Đang ghép Focus Stack"
                    try {
                        mergedBracketUri = withContext(Dispatchers.IO) {
                            NightShotProcessor.mergeBracketUrisIntoNewImage(
                                context = context,
                                frameUris = bracketSavedUris,
                                mode = if (useHdrBracket) NightShotProcessor.MergeMode.HDR_BRACKET
                                    else NightShotProcessor.MergeMode.FOCUS_STACK,
                                onProgress = { progress ->
                                    mainExecutor.execute {
                                        processingProgress = progress.coerceIn(0, 100)
                                        processingLabel = if (useHdrBracket) {
                                            "HDR Fusion · đang ghép vùng phơi sáng"
                                        } else {
                                            "Focus Stack · đang chọn vùng nét"
                                        }
                                    }
                                }
                            )
                        }
                    } finally {
                        isProcessing = false
                        processingProgress = 0
                    }
                }
                if (mergedBracketUri != null) {
                    lastUri = mergedBracketUri
                    thumb = withContext(Dispatchers.IO) {
                        runCatching {
                            decodeUprightBitmap(context, mergedBracketUri!!, maxDimension = 320)?.asImageBitmap()
                        }.getOrNull()
                    }
                }
                val mergeMessage = when {
                    bracketSavedUris.size < 2 -> "Không đủ khung hình để ghép; các ảnh chụp thành công vẫn được giữ."
                    mergedBracketUri != null -> "Đã ghép ảnh tổng hợp và vẫn giữ ${bracketSavedUris.size} ảnh gốc."
                    else -> "Ghép ảnh chưa thành công; toàn bộ ảnh gốc vẫn được giữ."
                }
                localizedToast(
                    context,
                    "${if (useHdrBracket) "HDR Bracketing" else "Focus Bracketing"}: đã lưu $bracketCompletedFrames/$bracketTargetFrames khung. $mergeMessage",
                    Toast.LENGTH_LONG
                ).show()
            }
            return
        }
        if (burstCaptureActive) {
            burstStopRequested = true
            localizedToast(context, "Đang dừng Burst sau khung hình hiện tại…", Toast.LENGTH_SHORT).show()
            return
        }
        if (!livePhotoEnabled && burstMode && mode == "Photo" &&
            nightShotMode == "off" && rawCaptureMode == "jpeg" &&
            lunaDreamMode == "off" && !uhdCaptureEnabled &&
            !fhdCaptureEnabled && !uhdPlusCaptureEnabled
        ) {
            if (!captureSetupReady || camera == null || imageCapture == null || isProcessing ||
                nightShotCaptureInProgress || starTrailCaptureInProgress
            ) return
            localView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            scope.launch {
                val activeCapture = imageCapture ?: return@launch
                burstCaptureActive = true
                burstStopRequested = false
                burstCompletedFrames = 0
                var lastSuccessfulUri: Uri? = null
                val sessionId = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
                try {
                    if (timerSec > 0) {
                        for (seconds in timerSec downTo 1) {
                            countdown = seconds
                            delay(1000)
                        }
                        countdown = 0
                    }
                    activeCapture.targetRotation = targetRotation
                    previewUseCase?.targetRotation = targetRotation
                    analysisUseCase?.targetRotation = targetRotation
                    for (frameIndex in 0 until 25) {
                        if (burstStopRequested) break
                        val frameStartedAt = SystemClock.elapsedRealtime()
                        val savedUri = runCatching {
                            saveBurstFrame(
                                context,
                                activeCapture,
                                mainExecutor,
                                sessionId,
                                frameIndex
                            )
                        }.onFailure {
                            android.util.Log.w("FUJI_BURST", "Could not enqueue frame ${frameIndex + 1}", it)
                        }.getOrNull()
                        if (savedUri != null) {
                            lastSuccessfulUri = savedUri
                            burstCompletedFrames += 1
                        }
                        // Best effort: do not add delay when the camera itself takes longer than 200 ms.
                        val elapsed = SystemClock.elapsedRealtime() - frameStartedAt
                        val remaining = 200L - elapsed
                        if (remaining > 0L && frameIndex < 24 && !burstStopRequested) delay(remaining)
                    }
                } finally {
                    countdown = 0
                    burstCaptureActive = false
                    burstStopRequested = false
                }
                if (lastSuccessfulUri != null) {
                    lastUri = lastSuccessfulUri
                    thumb = withContext(Dispatchers.IO) {
                        runCatching {
                            decodeUprightBitmap(context, lastSuccessfulUri!!, maxDimension = 320)?.asImageBitmap()
                        }.getOrNull()
                    }
                }
                localizedToast(
                    context,
                    "Burst hoàn tất · đã lưu $burstCompletedFrames/25 ảnh JPEG gốc. Tốc độ phụ thuộc camera và bộ nhớ.",
                    Toast.LENGTH_LONG
                ).show()
            }
            return
        }
        if (nightShotMode == "star_trails" && starTrailCaptureInProgress) {
            starTrailStopRequested = true
            localizedToast(context, "Đang dừng Star Trails sau khung hình hiện tại…", Toast.LENGTH_SHORT).show()
            return
        }
        if (!captureSetupReady || camera == null || imageCapture == null ||
            isProcessing || livePhotoCaptureInProgress ||
            nightShotCaptureInProgress || starTrailCaptureInProgress
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
            val captureUseDeviceProcessing = nativeImageProcessingEnabled &&
                !captureIsUltraHdr && capture.outputFormat != ImageCapture.OUTPUT_FORMAT_RAW &&
                capture.outputFormat != ImageCapture.OUTPUT_FORMAT_RAW_JPEG
            val captureUseLivePhoto = livePhotoEnabled
            val captureRawOnly = capture.outputFormat == ImageCapture.OUTPUT_FORMAT_RAW
            val captureRawPlus = capture.outputFormat == ImageCapture.OUTPUT_FORMAT_RAW_JPEG
            val captureUseRawDream20 = !nativeImageProcessingEnabled && !livePhotoEnabled &&
                mode == "Photo" && rawCaptureMode == "raw_dream"
            val lunaDreamSelected = !nativeImageProcessingEnabled && !livePhotoEnabled &&
                mode == "Photo" && (lunaDreamMode != "off" || captureUseRawDream20)
            val captureUseLunaDream15 = lunaDreamSelected && lunaDreamMode == "luna15" &&
                rawCaptureMode == "jpeg" && !captureRawPlus && !captureRawOnly &&
                !captureIsUltraHdr && !burstMode && !hdrBracketMode && !focusBracketMode
            val captureUseLunaDream20 = lunaDreamSelected && lunaDreamMode == "luna20" &&
                rawCaptureMode == "jpeg" && !captureRawPlus && !captureRawOnly &&
                !captureIsUltraHdr && !burstMode && !hdrBracketMode && !focusBracketMode
            val captureUseLunaDream = captureUseLunaDream15 || captureUseLunaDream20 || captureUseRawDream20
            val captureUseLunaDream20Stack = captureUseLunaDream20 || captureUseRawDream20
            if (lunaDreamSelected && !captureUseLunaDream) {
                localizedToast(
                    context,
                    "LunaDream cần Photo/JPEG; RawDream 2.0 cần RAW + JPEG được camera hỗ trợ. Hãy tắt Ultra HDR, Burst và Bracketing.",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }
            val captureUseNightShot = !nativeImageProcessingEnabled && !livePhotoEnabled &&
                mode == "Photo" && nightShotMode == "night" && !captureUseLunaDream
            val captureUseMilkyWayStack = captureUseNightShot && !front &&
                kotlin.math.abs(zoomSel - 1f) < 0.08f && milkyWayStackEnabled &&
                rawCaptureMode == "jpeg" && !captureRawPlus && !captureRawOnly &&
                !burstMode && !hdrBracketMode && !focusBracketMode
            if (milkyWayStackEnabled && captureUseNightShot && !captureUseMilkyWayStack) {
                localizedToast(
                    context,
                    "Milky Way Stack cần Photo/JPEG 1x; hãy tắt RAW, UHD, FHD, Burst và Bracketing.",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }
            val captureUseUhdPlusAlgorithm = false
            val captureUseUhdAlgorithm = false
            val captureUseFhdAlgorithm = false
            val captureAtOneX = captureUseLunaDream ||
                kotlin.math.abs(zoomSel - 1f) < 0.08f
            val captureNightShotExtension = nightExtensionActive
            val captureNightDurationSeconds = nightShotDurationSeconds.coerceIn(3, 10)
            val captureNightTargetFrames = NightShotFramePlan.targetFrames(captureNightDurationSeconds)
            val captureNightStartedAtNanos = SystemClock.elapsedRealtimeNanos()
            // LunaDream 1.5 is a bounded five-frame stack. Version 2.0 and RawDream
            // keep their existing eight-frame stack depth.
            val captureUseFiveFrameStack = false
            // Haptic feedback is synchronous and does not wait for camera capture.
            localView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            if (appPrefs.getBoolean("shutter_sound_enabled", true)) {
                playConfiguredShutterSound(context)
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
            // One height/width ratio source is shared by the Preview frame and saved-photo crop.
            // Landscape deliberately skips the output crop so the complete sensor frame is preserved.
            val selectedFrameHeightOverWidth =
                aspectRatioForOrientation(ratioIdx, captureIsLandscape)
            val outputHeightOverWidth = selectedFrameHeightOverWidth.takeUnless { captureIsLandscape }
            // Orientation detection uses the camera's neutral width/height ratio and never crops to it.
            val expectedOrientationAspectRatio = if (captureIsLandscape) 4f / 3f else 3f / 4f
            if (captureRotation != targetRotation ||
                boundCameraRotation != captureRotation ||
                boundCameraLandscape != isLandscape ||
                boundCameraRatioIdx != ratioIdx
            ) {
                // Never save a frame with the old orientation/crop after the display rotated.
                targetRotation = captureRotation
                localizedToast(context, "Đang đồng bộ hướng cảm biến, hãy chụp lại sau một chút.", Toast.LENGTH_SHORT).show()
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
            if (captureUseNightShot) {
                nightShotCaptureInProgress = true
                nightShotCapturedFrameCount = 0
                nightShotTargetFrameCount = captureNightTargetFrames
            }
            if (captureUseLunaDream || captureUseNightShot) {
                frozenPreviewBitmap = runCatching { previewView.bitmap?.asImageBitmap() }.getOrNull()
                previewFreezeEffect = if (captureUseNightShot) "night" else "luna"
                if (frozenPreviewBitmap == null) {
                    android.util.Log.w(
                        "FUJI_PREVIEW_FREEZE",
                        "PreviewView could not provide a frame snapshot; processing effects will still be shown."
                    )
                }
                multiFrameHoldActive = true
                multiFrameRenderStarted = false
                isProcessing = true
                processingProgress = 0
                processingEtaSeconds = -1
                processingLabel = when {
                    captureUseLunaDream15 -> "LunaDream 1.5 · đang chụp khung đầu tiên"
                    captureUseRawDream20 -> "RawDream 2.0 · đang chụp khung đầu tiên"
                    captureUseLunaDream20 -> "LunaDream 2.0 · đang chụp khung đầu tiên"
                    else -> "NightShotMode · đang chụp khung đầu tiên"
                }
            }
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
            var activeLivePhotoRecording: Recording? = null
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
                            if (captureNightShotExtension && captureUseNightShot) {
                                multiFrameHoldActive = false
                                multiFrameRenderStarted = false
                            }
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
                                captureUseLunaDream15 -> "LunaDream 1.5 · chuẩn bị ghép tối đa 5 khung"
                                captureUseRawDream20 -> "RawDream 2.0 · chuẩn bị ghép 8 khung, giữ DNG gốc"
                                captureUseLunaDream20 -> "LunaDream 2.0 · chuẩn bị ghép 8 khung"
                                captureUseMilkyWayStack -> "NightShotMode Milky Way · chuẩn bị căn chỉnh 8 khung"
                                captureUseNightShot -> "NightShotMode · chuẩn bị căn chỉnh và ghép nhiều khung"
                                captureUseSoftwareHdr -> "Đang xử lý Software HDR"
                                else -> "Ảnh đang được xử lý"
                            }
                            runCatching {
                                Camera2CameraControl.from(activeCamera.cameraControl).clearCaptureRequestOptions()
                                if (manualColorTempEnabled) applyManualWhiteBalance(activeCamera, manualColorTemp)
                                if (manualFocus > 0f) applyManualFocus(activeCamera, manualFocus)
                            }
                            scope.launch(Dispatchers.IO) {
                                var nightShotMerged = false
                                var lunaDreamCapturedFrameCount = 0
                                if ((captureUseNightShot || captureUseFiveFrameStack ||
                                        captureUseLunaDream15 || captureUseLunaDream20Stack) &&
                                    (!captureNightShotExtension || captureUseLunaDream15 || captureUseLunaDream20Stack)
                                ) {
                                    val stackedFrames = mutableListOf<File>()
                                    val firstFrame = File(context.cacheDir, "fuji-stack-first-" + System.nanoTime() + ".jpg")
                                    try {
                                        if (NightShotProcessor.copyUriToFile(context, saved, firstFrame)) {
                                            stackedFrames.add(firstFrame)
                                            val maxFrames = when {
                                                captureUseNightShot -> captureNightTargetFrames
                                                captureUseLunaDream20Stack -> 8
                                                captureUseLunaDream15 -> 5
                                                else -> 5
                                            }
                                            if (captureUseNightShot) {
                                                withContext(Dispatchers.Main) {
                                                    nightShotCapturedFrameCount = stackedFrames.size
                                                    nightShotTargetFrameCount = maxFrames
                                                }
                                            }
                                            // Keep two still requests in flight. CameraX still serializes sensor
                                            // exposures internally, but a small bounded queue overlaps its
                                            // JPEG/file callback work and avoids an idle gap between requests.
                                            // The device's actual sensor/ISP throughput remains authoritative.
                                            var consecutiveCaptureFailures = 0
                                            var stoppedForStorage = false
                                            var stopSubmitting = false
                                            var nextFrameNumber = stackedFrames.size + 1
                                            val pendingCaptures = ArrayDeque<Deferred<Triple<Int, File, Boolean>>>()
                                            // LunaDream 1.5 captures serially to avoid overlapping
                                            // CameraX requests on devices whose HAL cannot queue stills
                                            // reliably. LunaDream 2.0 retains its known-good two-request queue.
                                            val maxInFlightCaptures = when {
                                                captureUseLunaDream15 -> 1
                                                captureUseNightShot || captureUseLunaDream20Stack -> 2
                                                else -> 1
                                            }

                                            while (stackedFrames.size < maxFrames || pendingCaptures.isNotEmpty()) {
                                                while (!stopSubmitting &&
                                                    pendingCaptures.size < maxInFlightCaptures &&
                                                    stackedFrames.size + pendingCaptures.size < maxFrames
                                                ) {
                                                    if ((captureUseNightShot || captureUseMilkyWayStack) &&
                                                        context.cacheDir.usableSpace < 256L * 1024L * 1024L
                                                    ) {
                                                        stoppedForStorage = true
                                                        stopSubmitting = true
                                                        break
                                                    }
                                                    val frameNumber = nextFrameNumber++
                                                    val nextFrame = File(
                                                        context.cacheDir,
                                                        "fuji-stack-" + System.nanoTime() + "-" + frameNumber + ".jpg"
                                                    )
                                                    pendingCaptures.addLast(async(Dispatchers.IO) {
                                                        val captured = NightShotProcessor.captureFrameToFile(
                                                            capture, nextFrame, mainExecutor
                                                        )
                                                        Triple(frameNumber, nextFrame, captured)
                                                    })
                                                    withContext(Dispatchers.Main) {
                                                        val expectedFrame = stackedFrames.size + pendingCaptures.size
                                                        processingLabel = when {
                                                            captureUseLunaDream15 ->
                                                                "LunaDream 1.5: thu khung $expectedFrame/$maxFrames"
                                                            captureUseRawDream20 ->
                                                                "RawDream 2.0: thu khung $expectedFrame/$maxFrames"
                                                            captureUseLunaDream20 ->
                                                                "LunaDream 2.0: thu khung $expectedFrame/$maxFrames"
                                                            captureUseMilkyWayStack ->
                                                                "NightShotMode Milky Way: thu khung $expectedFrame/$maxFrames"
                                                            captureUseNightShot ->
                                                                "NightShotMode: thu khung $expectedFrame/$maxFrames · hàng đợi 2"
                                                            else -> "Multi-frame: chụp khung $expectedFrame/5"
                                                        }
                                                        processingProgress = if (captureUseNightShot) {
                                                            (5 + stackedFrames.size * 19 / maxFrames).coerceAtMost(24)
                                                        } else if (captureUseMilkyWayStack || captureUseLunaDream15 ||
                                                            captureUseLunaDream20Stack
                                                        ) {
                                                            (8 + stackedFrames.size * 16 / maxFrames).coerceAtMost(24)
                                                        } else (8 + stackedFrames.size * 3).coerceAtMost(24)
                                                    }
                                                }

                                                if (pendingCaptures.isEmpty()) break
                                                val (frameNumber, nextFrame, captured) = pendingCaptures.removeFirst().await()
                                                if (!captured) {
                                                    runCatching { nextFrame.delete() }
                                                    consecutiveCaptureFailures++
                                                    android.util.Log.w(
                                                        "FUJI_NIGHTSHOT",
                                                        "Capture attempt failed (frame " + frameNumber + ", " +
                                                            consecutiveCaptureFailures + "/3); keeping " +
                                                            stackedFrames.size + " valid frames"
                                                    )
                                                    if (consecutiveCaptureFailures >= 3) stopSubmitting = true
                                                    continue
                                                }

                                                consecutiveCaptureFailures = 0
                                                stackedFrames.add(nextFrame)
                                                if (captureUseNightShot) {
                                                    withContext(Dispatchers.Main) {
                                                        nightShotCapturedFrameCount = stackedFrames.size
                                                    }
                                                }
                                            }

                                            val burstElapsedMs = ((SystemClock.elapsedRealtimeNanos() - captureNightStartedAtNanos) / 1_000_000L).coerceAtLeast(1L)
                                            if (captureUseNightShot) {
                                                val achievedFps = stackedFrames.size * 1000.0 / burstElapsedMs
                                                android.util.Log.i(
                                                    "FUJI_NIGHTSHOT",
                                                    "Collected " + stackedFrames.size + "/" + maxFrames +
                                                        " frames in " + burstElapsedMs + "ms; approx " +
                                                        "%.1f".format(Locale.US, achievedFps) +
                                                        " FPS; bounded in-flight queue=" + maxInFlightCaptures
                                                )
                                            }
                                            if (stoppedForStorage) {
                                                android.util.Log.w("FUJI_NIGHTSHOT", "Stopped early to protect free cache storage; processing " + stackedFrames.size + " frames")
                                            }
                                            if (captureUseNightShot && stackedFrames.size < maxFrames) {
                                                android.util.Log.w(
                                                    "FUJI_NIGHTSHOT",
                                                    "Incomplete stack " + stackedFrames.size + "/" + maxFrames +
                                                        "; merge/render is blocked and the original frame will be preserved"
                                                )
                                            }
                                            // 1.5 aims for five frames, but a three-frame stack is
                                            // enough to complete a valid multi-frame result when the
                                            // camera HAL rejects a late request. 2.0 keeps its prior rule.
                                            val minimumFramesRequired = when {
                                                captureUseNightShot -> maxFrames
                                                captureUseLunaDream15 -> 3
                                                else -> 2
                                            }
                                            lunaDreamCapturedFrameCount = stackedFrames.size
                                            if (stackedFrames.size >= minimumFramesRequired) {
                                                withContext(Dispatchers.Main) {
                                                    processingLabel = when {
                                                        captureUseLunaDream15 ->
                                                            "LunaDream 1.5: căn chỉnh và khử nhiễu " + stackedFrames.size + "/5 khung"
                                                        captureUseRawDream20 ->
                                                            "RawDream 2.0: căn chỉnh và khử nhiễu " + stackedFrames.size + "/8 khung"
                                                        captureUseLunaDream20 ->
                                                            "LunaDream 2.0: căn chỉnh và khử nhiễu " + stackedFrames.size + "/8 khung"
                                                        captureUseMilkyWayStack ->
                                                            "NightShotMode Milky Way: căn chỉnh và gộp " + stackedFrames.size + "/8 khung"
                                                        captureUseNightShot ->
                                                            "NightShotMode: căn chỉnh và gộp " + stackedFrames.size + " khung"
                                                        else -> "Multi-frame: căn chỉnh và gộp " + stackedFrames.size + "/5 khung"
                                                    }
                                                    processingProgress = 27
                                                }
                                                nightShotMerged = NightShotProcessor.mergeFramesIntoUri(
                                                    context, stackedFrames, saved,
                                                    when {
                                                        captureUseRawDream20 || captureUseLunaDream20 ->
                                                            NightShotProcessor.MergeMode.LUNA_DREAM_2_0
                                                        captureUseLunaDream15 ->
                                                            NightShotProcessor.MergeMode.LUNA_DREAM_1_5
                                                        captureUseMilkyWayStack ->
                                                            NightShotProcessor.MergeMode.MILKY_WAY_STACK
                                                        else -> NightShotProcessor.MergeMode.NIGHT_AVERAGE
                                                    }
                                                ) { mergeProgress ->
                                                    mainExecutor.execute {
                                                        processingLabel = when {
                                                            captureUseLunaDream15 && mergeProgress >= 90 ->
                                                                "LunaDream 1.5 Render · khử nhiễu và phục hồi chi tiết"
                                                            captureUseLunaDream15 ->
                                                                "LunaDream 1.5 · căn chỉnh và ghép ${stackedFrames.size}/5 khung"
                                                            captureUseRawDream20 && mergeProgress >= 90 ->
                                                                "RawDream 2.0 Render · hoàn thiện ảnh JPEG"
                                                            captureUseRawDream20 ->
                                                                "RawDream 2.0 · căn chỉnh và ghép 8 khung"
                                                            captureUseLunaDream20 && mergeProgress >= 90 ->
                                                                "LunaDream 2.0 Render · khử nhiễu và phục hồi chi tiết"
                                                            captureUseLunaDream20 ->
                                                                "LunaDream 2.0 · căn chỉnh và ghép 8 khung"
                                                            captureUseNightShot -> "NightShotMode: căn khung và giảm nhiễu"
                                                            captureUseMilkyWayStack -> "Milky Way: căn chỉnh sao và giảm nhiễu"
                                                            else -> "Multi-frame: ghép 5 khung"
                                                        }
                                                        processingProgress = maxOf(
                                                            processingProgress,
                                                            (27 + mergeProgress * 0.55f).toInt().coerceIn(27, 82)
                                                        )
                                                    }
                                                }
                                            }
                                            android.util.Log.i(
                                                "FUJI_FRAME_STACK",
                                                "mode=" + (when {
                                                    captureUseLunaDream15 -> "LunaDream-1.5"
                                                    captureUseRawDream20 -> "RawDream-2.0"
                                                    captureUseLunaDream20 -> "LunaDream-2.0"
                                                    captureUseNightShot -> "NightShotMode-" + captureNightDurationSeconds + "s"
                                                    captureUseMilkyWayStack -> "MilkyWay-8"
                                                    captureUseUhdPlusAlgorithm -> "UHD+"
                                                    captureUseUhdAlgorithm -> "UHD"
                                                    else -> "FHD"
                                                }) + " actualFrames=" + stackedFrames.size
                                            )
                                        }
                                    } finally {
                                        stackedFrames.forEach { runCatching { it.delete() } }
                                        withContext(Dispatchers.Main) {
                                            if (captureUseNightShot) nightShotCaptureInProgress = false
                                            if (captureUseLunaDream || captureUseNightShot) {
                                                // Keep the device steady until the Render stage visibly starts.
                                                multiFrameHoldActive = nightShotMerged
                                                multiFrameRenderStarted = false
                                            }
                                        }
                                    }
                                }
                                if (captureUseNightShot && !nightShotMerged && !captureNightShotExtension) {
                                    val preserved = runCatching {
                                        context.contentResolver.update(
                                            saved,
                                            ContentValues().apply {
                                                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FujifilmMode/Unprocessed/NightShot")
                                                put(MediaStore.Images.Media.IS_PENDING, 0)
                                            },
                                            null,
                                            null
                                        ) > 0
                                    }.getOrDefault(false)
                                    val originalThumbnail = if (preserved) {
                                        runCatching {
                                            decodeUprightBitmap(context, saved, maxDimension = 320)?.asImageBitmap()
                                        }.getOrNull()
                                    } else null
                                    withContext(Dispatchers.Main) {
                                        if (preserved) {
                                            lastUri = saved
                                            thumb = originalThumbnail
                                        }
                                        nightShotCaptureInProgress = false
                                        multiFrameHoldActive = false
                                        multiFrameRenderStarted = false
                                        isProcessing = false
                                        processingProgress = 0
                                        processingEtaSeconds = -1
                                        processingLabel = ""
                                    }
                                    android.util.Log.w(
                                        "FUJI_NIGHTSHOT",
                                        "NightShot was not rendered because the complete target frame count was not reached; originalPreserved=$preserved"
                                    )
                                    return@launch
                                }
                                withContext(Dispatchers.Main) { processingProgress = maxOf(processingProgress, 30) }
                                val orientationNormalized = if (captureIsUltraHdr) {
                                    // CameraX has already written the capture rotation. Do not
                                    // decode/recompress JPEG_R: Bitmap JPEG export discards the gain map.
                                    true
                                } else runCatching {
                                    normalizeCapturedImage(
                                        context,
                                        saved,
                                        captureRotationDegrees,
                                        expectedOrientationAspectRatio
                                    )
                                }.getOrDefault(false)
                                withContext(Dispatchers.Main) { processingProgress = maxOf(processingProgress, 35) }
                                // RawDream preserves the first sensor DNG as the true RAW master. Its JPEG
                                // companion is fused from eight JPEG frames using LunaDream 2.0.
                                // LunaDream 1.5 is successful only after its bounded stack merges.
                                val rawDream20Processed = captureUseRawDream20 && nightShotMerged
                                val lunaDream15Processed = captureUseLunaDream15 && nightShotMerged
                                val lunaDreamApplied = when {
                                    captureUseLunaDream15 -> lunaDream15Processed
                                    captureUseRawDream20 -> rawDream20Processed
                                    captureUseLunaDream20 -> nightShotMerged
                                    else -> false
                                }
                                withContext(Dispatchers.Main) {
                                    if (captureUseLunaDream || captureUseNightShot) {
                                        multiFrameHoldActive = false
                                        multiFrameRenderStarted = lunaDreamApplied ||
                                            (captureUseNightShot && (nightShotMerged || captureNightShotExtension))
                                    }
                                    processingLabel = when {
                                        captureUseLunaDream15 && lunaDreamApplied ->
                                            "LunaDream Render · 1.5 đang phục hồi chi tiết"
                                        captureUseRawDream20 && lunaDreamApplied ->
                                            "LunaDream Render · RawDream 2.0 đang xử lý JPEG ghép"
                                        captureUseLunaDream20 && lunaDreamApplied ->
                                            "LunaDream Render · 2.0 đang phục hồi chi tiết"
                                        captureUseNightShot && (nightShotMerged || captureNightShotExtension) ->
                                            "NightShot Render · đang hoàn tất ảnh đã ghép"
                                        captureUseLunaDream -> "LunaDream · xử lý chưa hoàn tất, giữ ảnh gốc"
                                        else -> "Đang hoàn tất ảnh"
                                    }
                                    processingProgress = if (multiFrameRenderStarted) {
                                        maxOf(processingProgress, 83)
                                    } else {
                                        maxOf(processingProgress, 60)
                                    }
                                    processingEtaSeconds = -1
                                }
                                val softwareHdrProfile = 2
                                val softwareHdrApplied = if (
                                    !captureIsUltraHdr && captureUseSoftwareHdr
                                ) {
                                    runCatching {
                                        HdrByHdmpEngine.process(
                                            context,
                                            saved,
                                            qualityMultiplier = softwareHdrProfile
                                        ) { progress ->
                                            mainExecutor.execute {
                                                processingLabel = "HDRbyHDMP · profile ${softwareHdrProfile}x · $progress%"
                                                processingProgress = maxOf(
                                                    processingProgress,
                                                    (60 + progress * 0.06f).roundToInt().coerceIn(60, 66)
                                                )
                                            }
                                        }
                                    }.getOrDefault(false)
                                } else false
                                if (captureUseDeviceProcessing) {
                                    withContext(Dispatchers.Main) {
                                        processingLabel = "Xử lý ảnh gốc thiết bị · tối ưu tông sáng và chi tiết"
                                        processingProgress = maxOf(processingProgress, 67)
                                    }
                                }
                                val deviceProcessingApplied = if (captureUseDeviceProcessing) {
                                    withContext(Dispatchers.IO) {
                                        runCatching {
                                            applySoftwarePhotoEnhancement(
                                                context,
                                                saved,
                                                softwareHdr = true,
                                                uhdSharpening = true
                                            )
                                        }.onFailure {
                                            android.util.Log.e(
                                                "FUJI_NATIVE_IMAGE",
                                                "Device image enhancement pass failed; the original JPEG is retained",
                                                it
                                            )
                                        }.getOrDefault(false)
                                    }
                                } else false
                                if (captureUseDeviceProcessing) {
                                    android.util.Log.i(
                                        "FUJI_NATIVE_IMAGE",
                                        "Post-capture device enhancement applied=$deviceProcessingApplied"
                                    )
                                }
                                withContext(Dispatchers.Main) { processingProgress = maxOf(processingProgress, 70) }
                                // Keep a clean pre-filter image as the reference for every picker preview.
                                val unfilteredPreview = if (captureIsUltraHdr) null else runCatching {
                                    decodeUprightBitmap(context, saved, maxDimension = 640)
                                }.getOrNull()
                                if (unfilteredPreview != null) {
                                    withContext(Dispatchers.Main) { filterPreviewSourceBitmap = unfilteredPreview }
                                }
                                val lookApplied = if (captureIsUltraHdr) false else
                                    runCatching { applyCameraLook(context, saved) }.getOrDefault(false)
                                withContext(Dispatchers.Main) { processingProgress = maxOf(processingProgress, 72) }

                                val outputResolutionResult = if (
                                    !captureIsUltraHdr &&
                                    !captureRawOnly && (!captureRawPlus || captureUseRawDream20)
                                ) {
                                    val requestedMp = when {
                                        captureUseNightShot -> 60_000_000L
                                        captureUseLunaDream15 -> 50_000_000L
                                        captureUseLunaDream -> 108_000_000L
                                        else -> null
                                    }
                                    withContext(Dispatchers.Main) {
                                        processingLabel = if (requestedMp != null) {
                                            "LunaDream đang tối ưu kích thước ${requestedMp / 1_000_000L}MP"
                                        } else {
                                            "Đang giữ trọn khung ảnh theo tỷ lệ đã chọn"
                                        }
                                        processingProgress = 84
                                        processingEtaSeconds = -1
                                    }
                                    withContext(Dispatchers.IO) {
                                        runCatching {
                                            if (requestedMp != null) {
                                                PhotoOutputResolution.interpolateToMp(
                                                    context,
                                                    saved,
                                                    requestedMp,
                                                    outputHeightOverWidth
                                                )
                                            } else {
                                                PhotoOutputResolution.applyAspectRatio(
                                                    context,
                                                    saved,
                                                    outputHeightOverWidth
                                                )
                                            }
                                        }.onFailure {
                                            android.util.Log.e(
                                                "FUJI_OUTPUT",
                                                "Final aspect-ratio/MP output failed; original retained",
                                                it
                                            )
                                        }.getOrNull()
                                    }
                                } else null
                                val outputResolutionApplied = outputResolutionResult != null

                                if (captureUseLunaDream && lunaDreamApplied && !outputResolutionApplied) {
                                    // Do not present an undersized JPEG as a completed 50/108MP LunaDream.
                                    runCatching {
                                        context.contentResolver.update(
                                            saved,
                                            ContentValues().apply {
                                                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FujifilmMode/Unprocessed")
                                                put(MediaStore.Images.Media.IS_PENDING, 0)
                                            },
                                            null,
                                            null
                                        )
                                    }
                                    withContext(Dispatchers.Main) {
                                        lastUri = null
                                        thumb = null
                                        photoViewerVisible = false
                                        isProcessing = false
                                        processingProgress = 0
                                        processingEtaSeconds = -1
                                        multiFrameHoldActive = false
                                        multiFrameRenderStarted = false
                                        localizedToast(
                                            context,
                                            "LunaDream đã ghép ảnh nhưng không đạt độ phân giải yêu cầu. Ảnh được giữ riêng trong Pictures/FujifilmMode/Unprocessed; không xuất như ảnh 50/108MP.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                    return@launch
                                }

                                // Render the optional frame after final MP sizing too, so its
                                // borders and labels are crisp at the actual output resolution.
                                val frameApplied = if (captureIsUltraHdr) false else
                                    runCatching { applyCameraFrameOverlay(context, saved) }.getOrDefault(false)

                                // Apply watermark only after final MP resampling so text,
                                // logos and spacing are drawn at the final pixel density, not enlarged.
                                withContext(Dispatchers.Main) {
                                    processingProgress = maxOf(processingProgress, 90)
                                    processingLabel = "Đang đóng watermark ở độ phân giải cuối"
                                    processingEtaSeconds = -1
                                }
                                val stamped = if (captureIsUltraHdr || (captureUseLunaDream && !lunaDreamApplied)) false else runCatching {
                                    applyCameraWatermark(
                                        context, saved, focal, aperture, shutterLabel,
                                        if (actualIso > 0) actualIso.toString() else "AUTO",
                                        preservePhotoAspectRatio = true
                                    )
                                }.getOrDefault(false)

                                if (captureUseLunaDream && !lunaDreamApplied) {
                                    // Never expose the untouched JPEG as a successful LunaDream result.
                                    // Preserve it in a separate recovery folder so the user does not lose the shot.
                                    val moved = runCatching {
                                        context.contentResolver.update(
                                            saved,
                                            ContentValues().apply {
                                                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FujifilmMode/Unprocessed")
                                                put(MediaStore.Images.Media.IS_PENDING, 0)
                                            },
                                            null,
                                            null
                                        ) > 0
                                    }.getOrDefault(false)
                                    if (!moved) {
                                        runCatching {
                                            context.contentResolver.update(
                                                saved,
                                                ContentValues().apply {
                                                    put(MediaStore.Images.Media.IS_PENDING, 0)
                                                },
                                                null,
                                                null
                                            )
                                        }
                                    }
                                    withContext(Dispatchers.Main) {
                                        lastUri = null
                                        thumb = null
                                        photoViewerVisible = false
                                        isProcessing = false
                                        processingProgress = 0
                                        processingEtaSeconds = -1
                                        multiFrameHoldActive = false
                                        multiFrameRenderStarted = false
                                        localizedToast(
                                            context,
                                            "LunaDream chưa xử lý hoàn tất. Ảnh gốc được giữ riêng trong Pictures/FujifilmMode/Unprocessed, không xuất như ảnh LunaDream.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                    return@launch
                                }

                                val metadataPublished = runCatching {
                                    if (captureIsUltraHdr) publishUltraHdrCapture(context, saved)
                                    else finalizeCapturedImageMetadata(context, saved)
                                }.getOrDefault(false)
                                // Normalization runs before image looks and watermark drawing.
                                // No later rotation is allowed to move the frame away from the
                                // final upright pixel coordinate system.
                                val orientationLabelFixed = orientationNormalized && metadataPublished
                                val inspectorLunaDreamStatus = when {
                                    captureUseLunaDream15 && lunaDreamApplied -> "LunaDream 1.5 · ghép " + lunaDreamCapturedFrameCount + "/5 khung"
                                    captureUseRawDream20 && lunaDreamApplied -> "RawDream 2.0 · DNG gốc + JPEG ghép " + lunaDreamCapturedFrameCount + "/8 khung bằng LunaDream 2.0"
                                    captureUseLunaDream20 && lunaDreamApplied -> "LunaDream 2.0 · ghép đa khung và phục hồi chi tiết"
                                    captureUseLunaDream -> "LunaDream chưa hoàn tất · giữ ảnh gốc"
                                    captureUseNightShot && nightShotMerged -> "NightShotMode · ghép đa khung"
                                    captureUseNightShot -> "NightShotMode · chụp đêm"
                                    captureUseDeviceProcessing && deviceProcessingApplied -> "Xử lý ảnh gốc thiết bị · ISP + tối ưu tông/chi tiết"
                                    captureUseDeviceProcessing -> "Xử lý ảnh gốc thiết bị chưa hoàn tất · giữ ảnh JPEG"
                                    else -> "Chụp ảnh thường"
                                }
                                val inspectorOutputSize = outputResolutionResult?.let {
                                    "${it.width}×${it.height} · ${(it.actualPixels + 500_000L) / 1_000_000L}MP"
                                } ?: "Không nội suy độ phân giải"
                                appPrefs.edit()
                                    .putString("photo_lunadream_status:$saved", inspectorLunaDreamStatus)
                                    .putString("photo_camera_id:$saved", activeCameraDetails?.id ?: "Không công bố")
                                    .putString("photo_physical_camera_id:$saved", activePhysicalCameraId ?: "HAL không công bố")
                                    .putString("photo_output_size:$saved", inspectorOutputSize)
                                    .apply()
                                withContext(Dispatchers.Main) {
                                    processingProgress = 98
                                    processingEtaSeconds = 1
                                }
                                withContext(Dispatchers.Main) {
                                    lastUri = saved
                                    processingProgress = 100
                                    val watermarkRequested = appPrefs.getBoolean("watermark_enabled", true)
                                    val actualOutputMp = outputResolutionResult?.let {
                                        ((it.actualPixels + 500_000L) / 1_000_000L).toInt()
                                    } ?: 0
                                    val message = when {
                                        captureUseLunaDream15 && lunaDreamApplied && outputResolutionApplied && orientationLabelFixed ->
                                            if (outputResolutionResult!!.reachedRequestedSize) "Đã lưu LunaDream 1.5 · 50MP."
                                            else "Đã lưu LunaDream 1.5 · ${actualOutputMp}MP theo giới hạn bộ nhớ."
                                        captureUseRawDream20 && lunaDreamApplied && outputResolutionApplied && orientationLabelFixed ->
                                            if (outputResolutionResult!!.reachedRequestedSize) "Đã lưu RawDream 2.0 · DNG gốc + JPEG LunaDream 2.0 108MP."
                                            else "Đã lưu RawDream 2.0 · DNG gốc + JPEG LunaDream 2.0 ${actualOutputMp}MP theo giới hạn bộ nhớ."
                                        captureUseLunaDream20 && lunaDreamApplied && outputResolutionApplied && orientationLabelFixed ->
                                            if (outputResolutionResult!!.reachedRequestedSize) "Đã lưu LunaDream 2.0 · 108MP."
                                            else "Đã lưu LunaDream 2.0 · ${actualOutputMp}MP theo giới hạn bộ nhớ."
                                        captureUseLunaDream && !lunaDreamApplied ->
                                            "LunaDream chưa xử lý hoàn tất. Ảnh gốc được giữ lại; chế độ LunaDream vẫn được chọn."
                                        captureUseRawDream20 && lunaDreamApplied && !outputResolutionApplied ->
                                            "RawDream 2.0 đã giữ DNG gốc và ghép JPEG bằng LunaDream 2.0, nhưng chưa thể nâng kích thước JPEG an toàn."
                                        captureUseLunaDream && lunaDreamApplied && !outputResolutionApplied ->
                                            "LunaDream đã ghép và phục hồi ảnh nhưng không thể đạt kích thước đầu ra an toàn; ảnh đã xử lý được giữ lại."
                                        captureUseNightShot && nightShotMerged && orientationLabelFixed ->
                                            "Đã lưu NightShotMode · ${nightShotCapturedFrameCount} khung thực tế."
                                        captureUseNightShot && captureNightShotExtension && orientationLabelFixed ->
                                            "NightShotMode đã xử lý bằng chế độ NIGHT của camera."
                                        captureUseNightShot && orientationLabelFixed ->
                                            "NightShotMode không ghép đủ khung hình; ảnh gốc vẫn được giữ."
                                        captureIsUltraHdr && orientationLabelFixed -> "Đã lưu Ultra HDR nguyên bản và giữ gain map."
                                        captureIsUltraHdr -> "Đã lưu tệp Ultra HDR nguyên bản; giữ nguyên gain map."
                                        stamped && lookApplied && orientationLabelFixed -> "Đã lưu ảnh đúng tỉ lệ, đúng hướng, có màu và watermark."
                                        stamped && orientationLabelFixed -> "Đã lưu ảnh đúng tỉ lệ, đúng hướng và có watermark."
                                        stamped -> "Watermark đã đóng nhưng tỉ lệ/hướng ảnh chưa được xác minh."
                                        watermarkRequested && !stamped -> "Ảnh đã lưu nhưng watermark chưa được đóng. Kiểm tra lại mục Watermark."
                                        lookApplied && orientationLabelFixed -> "Đã lưu ảnh đúng tỉ lệ, đúng hướng và áp dụng bộ lọc màu."
                                        lookApplied -> "Đã áp dụng màu nhưng chưa xác minh được tỉ lệ/hướng ảnh."
                                        orientationLabelFixed && outputResolutionApplied -> "Đã lưu ảnh đã tối ưu kích thước đầu ra."
                                        orientationLabelFixed && softwareHdrApplied -> "Đã lưu ảnh đúng hướng; đã áp dụng Software HDR."
                                        orientationLabelFixed -> "Đã lưu ảnh đúng tỉ lệ và chuẩn hóa metadata vào album."
                                        else -> "Ảnh đã lưu nhưng chưa xác minh được tỉ lệ/hướng metadata. Hãy kiểm tra lại ảnh."
                                    }
                                    val finalMessage = if (captureIsUltraHdr) message
                                        else if (frameApplied) "$message Frame đã được áp dụng." else message
                                    val deviceProcessingSuffix = when {
                                        !captureUseDeviceProcessing -> ""
                                        deviceProcessingApplied -> " Xử lý ảnh gốc thiết bị đã hoàn tất: tối ưu dải sáng và độ sắc nét."
                                        else -> " Bước xử lý ảnh gốc thiết bị không hoàn tất; ảnh JPEG gốc vẫn được giữ."
                                    }
                                    val lunaDreamStatusSuffix = when {
                                        captureUseLunaDream15 && lunaDreamApplied -> " LunaDream 1.5 đã ghép " + lunaDreamCapturedFrameCount + " khung hình."
                                        captureUseRawDream20 && lunaDreamApplied -> " RawDream 2.0 giữ DNG cảm biến gốc và ghép " + lunaDreamCapturedFrameCount + " JPEG bằng thuật toán LunaDream 2.0."
                                        captureUseLunaDream20 && lunaDreamApplied -> " LunaDream 2.0 đã ghép và phục hồi chi tiết đa khung."
                                        captureUseLunaDream -> " LunaDream vẫn được giữ làm chế độ đã chọn."
                                        else -> ""
                                    }
                                    android.util.Log.i("FUJI_CAPTURE", finalMessage + deviceProcessingSuffix + lunaDreamStatusSuffix)
                                }
                                delay(450)
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    processingProgress = 0
                                    processingEtaSeconds = -1
                                    multiFrameHoldActive = false
                                    multiFrameRenderStarted = false
                                }
                            }
                        } else {
                            nightShotCaptureInProgress = false
                            multiFrameHoldActive = false
                            multiFrameRenderStarted = false
                            isProcessing = false
                            processingProgress = 0
                            localizedToast(context, "Ảnh đã chụp nhưng MediaStore không trả về URI.", Toast.LENGTH_LONG).show()
                        }
                    }
                    override fun onError(exception: androidx.camera.core.ImageCaptureException) {
                        activeLivePhotoRecording?.stop()
                        nightShotCaptureInProgress = false
                        multiFrameHoldActive = false
                        multiFrameRenderStarted = false
                        isProcessing = false
                        processingProgress = 0
                        runCatching {
                            camera?.let { Camera2CameraControl.from(it.cameraControl).clearCaptureRequestOptions() }
                        }
                        localizedToast(context, "Lỗi chụp ảnh: ${exception.message}", Toast.LENGTH_LONG).show()
                    }
                }
            if (captureUseLivePhoto) {
                val liveVideoCapture = videoCaptureUseCase
                if (liveVideoCapture == null) {
                    localizedToast(
                        context,
                        "Live Photo chưa sẵn sàng: camera chưa tạo được luồng video. Ảnh chưa được chụp.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }
                livePhotoCaptureInProgress = true
                liveVideoCapture.targetRotation = captureRotation
                val liveVideoName = "${name}_LIVE"
                val videoValues = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, "$liveVideoName.mp4")
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/FujifilmMode/LivePhoto")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
                val videoOptions = MediaStoreOutputOptions.Builder(
                    context.contentResolver,
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                ).setContentValues(videoValues).build()
                val recordingStarted = CompletableDeferred<Boolean>()
                try {
                    activeLivePhotoRecording = liveVideoCapture.output
                        .prepareRecording(context, videoOptions)
                        .start(mainExecutor) { event ->
                            when (event) {
                                is VideoRecordEvent.Start -> {
                                    if (!recordingStarted.isCompleted) recordingStarted.complete(true)
                                    android.util.Log.i("FUJI_LIVE_PHOTO", "Motion clip started: $liveVideoName")
                                }
                                is VideoRecordEvent.Finalize -> {
                                    if (!recordingStarted.isCompleted) recordingStarted.complete(false)
                                    val videoUri = event.outputResults.outputUri
                                    val published = if (!event.hasError() && videoUri != Uri.EMPTY) {
                                        runCatching {
                                            context.contentResolver.update(
                                                videoUri,
                                                ContentValues().apply {
                                                    put(MediaStore.Video.Media.IS_PENDING, 0)
                                                },
                                                null,
                                                null
                                            ) > 0
                                        }.getOrDefault(false)
                                    } else false
                                    if (!published && videoUri != Uri.EMPTY) {
                                        runCatching { context.contentResolver.delete(videoUri, null, null) }
                                    }
                                    android.util.Log.i(
                                        "FUJI_LIVE_PHOTO",
                                        "Motion clip finalized uri=$videoUri success=$published error=${event.error}"
                                    )
                                    mainExecutor.execute {
                                        livePhotoCaptureInProgress = false
                                        localizedToast(
                                            context,
                                            if (published) {
                                                "Đã lưu Live Photo: ảnh $name.jpg và clip $liveVideoName.mp4 trong Movies/FujifilmMode/LivePhoto."
                                            } else {
                                                "Không lưu được clip Live Photo; ảnh tĩnh vẫn được giữ nếu đã chụp thành công."
                                            },
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        }
                    val didStart = withTimeoutOrNull(2_500L) { recordingStarted.await() } == true
                    if (!didStart) {
                        activeLivePhotoRecording?.stop()
                        activeLivePhotoRecording = null
                        livePhotoCaptureInProgress = false
                        localizedToast(
                            context,
                            "Camera không bắt đầu được clip Live Photo. Ảnh chưa được chụp.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }
                    delay(700L)
                } catch (livePhotoError: Exception) {
                    activeLivePhotoRecording?.stop()
                    activeLivePhotoRecording = null
                    livePhotoCaptureInProgress = false
                    android.util.Log.e("FUJI_LIVE_PHOTO", "Could not start motion clip", livePhotoError)
                    localizedToast(
                        context,
                        "Live Photo không khởi động được: ${livePhotoError.message ?: "lỗi camera"}. Ảnh chưa được chụp.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
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
            if (captureUseLivePhoto && activeLivePhotoRecording != null) {
                delay(1_000L)
                runCatching { activeLivePhotoRecording?.stop() }
                    .onFailure {
                        android.util.Log.w("FUJI_LIVE_PHOTO", "Could not stop motion clip cleanly", it)
                    }
            }
        }
    }

    fun animateZoomTo(targetRatio: Float) {
        val activeCamera = camera ?: return
        val state = activeCamera.cameraInfo.zoomState.value ?: return
        val selectableMax = Camera2Info.maxSelectableZoom(
            activeCameraDetails, activePhysicalCameraDetails, state.maxZoomRatio
        )
        val safeTarget = targetRatio.coerceIn(state.minZoomRatio, selectableMax)
        val start = state.zoomRatio
        val crossesLensBoundary = (start < 0.98f && safeTarget >= 0.98f) ||
            (start >= 0.98f && safeTarget < 0.98f)
        zoomAnimationJob?.cancel()
        if (crossesLensBoundary) {
            localView.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
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
    LaunchedEffect(captureSetupReady, manualSensorSupported, front, milkyWayStackEnabled) {
        if (captureSetupReady && milkyWayStackEnabled && (front || !manualSensorSupported)) {
            milkyWayStackEnabled = false
            localizedToast(
                context,
                "Đã tắt Milky Way Stack: camera đang chọn không có Manual Sensor phù hợp.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    val isoRange = if (manualSensorSupported) cameraInfo2?.getCameraCharacteristic(
        CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE
    ) else null
    val shutterRange = if (manualSensorSupported) cameraInfo2?.getCameraCharacteristic(
        CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE
    ) else null
    val maxFocus = cameraInfo2?.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f
    val availableAfModes = cameraInfo2?.getCameraCharacteristic(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
    val manualInfinityFocusSupported = maxFocus >= 8f &&
        availableAfModes?.contains(CameraCharacteristics.CONTROL_AF_MODE_OFF) == true
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
    val afText = if (focusLocked) "LOCK" else if (milkyWayInfinityFocusEnabled) "∞" else if (manualFocus > 0f) "%.1f D".format(Locale.US, manualFocus) else "AF"
    val toneText = if (manualColorTempEnabled) "${manualColorTemp}K" else "AUTO"

    // Milky Way setup advice is derived from the active camera's public Camera2 metadata
    // and the live preview's measured luminance. It is advisory; it cannot detect clouds,
    // moonlight or light pollution without external sky/location data.
    val skyEquivalentFocal = activeCameraDetails?.eq35Mm?.takeIf { it.isFinite() && it > 0f }
    val sensorExposureMaximumSeconds = activeCameraDetails?.exposureRange
        ?.substringAfter("..")
        ?.toFloatOrNull()
        ?.takeIf { it.isFinite() && it > 0f }
    val recommendedStarShutterSeconds = skyEquivalentFocal?.let { equivalent ->
        minOf(500f / equivalent, 15f, sensorExposureMaximumSeconds ?: 15f).coerceAtLeast(0.5f)
    }
    val advertisedIsoBounds = activeCameraDetails?.isoRange?.split("..")
    val advertisedIsoMinimum = advertisedIsoBounds?.getOrNull(0)?.toIntOrNull() ?: 50
    val advertisedIsoMaximum = advertisedIsoBounds?.getOrNull(1)?.toIntOrNull()
    val recommendedStarIso = advertisedIsoMaximum?.let { upper ->
        1600.coerceIn(advertisedIsoMinimum.coerceAtMost(upper), minOf(upper, 3200).coerceAtLeast(advertisedIsoMinimum.coerceAtMost(upper)))
    }
    val showMilkyWayAdvisor = false


    // The shared ratio is height / width. Compose aspectRatio expects width / height,
    // so invert it exactly once for container measurement. CameraX handles preview scaling.
    val previewHeightOverWidth = aspectRatioForOrientation(ratioIdx, isLandscape)
    val orientationProgress by animateFloatAsState(
        targetValue = if (isLandscape) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 360f),
        label = "camera orientation transition"
    )

    val toolbarContent: @Composable (Modifier) -> Unit = { toolbarModifier ->
                    val toolbarButtonSize = if (uiIsLandscape) 46.dp else 36.dp
                    Row(toolbarModifier, horizontalArrangement = Arrangement.SpaceBetween) {
                        CircleBtn(
                            icon = if (flash == ImageCapture.FLASH_MODE_ON) {
                                Icons.Default.FlashOn
                            } else {
                                Icons.Default.FlashOff
                            },
                            desc = "Đèn LED liên tục",
                            onClick = {
                                flash = if (flash == ImageCapture.FLASH_MODE_OFF) {
                                    ImageCapture.FLASH_MODE_ON
                                } else {
                                    ImageCapture.FLASH_MODE_OFF
                                }
                            }
                        , size = toolbarButtonSize)
                        CircleBtn(
                            Icons.Default.Timer, "Timer",
                            onClick = { timerSec = when (timerSec) { 0 -> 3; 3 -> 10; else -> 0 } },
                            tint = if (timerSec > 0) CamColors.Yellow else Color.White
                        , size = toolbarButtonSize)
                        if (mode == "Photo" && nightShotMode == "off" && lunaDreamMode == "off" &&
                            rawCaptureMode == "jpeg" && !uhdCaptureEnabled && !fhdCaptureEnabled && !uhdPlusCaptureEnabled
                        ) {
                            CircleBtn(
                                Icons.Default.BurstMode, "Burst ×25",
                                onClick = { burstMode = !burstMode },
                                tint = if (burstMode) CamColors.Yellow else Color.White
                            , size = toolbarButtonSize)
                        }
                        CircleBtn(
                            Icons.Default.Tune, "Metering",
                            onClick = { metering = (metering + 1) % meteringModes.size },
                            tint = if (metering > 0) CamColors.Yellow else Color.White
                        , size = toolbarButtonSize)

                        CircleBtn(
                            Icons.Default.BarChart, "Histogram",
                            onClick = { onShowHist(!showHist) },
                            tint = if (showHist) CamColors.Yellow else Color.White
                        , size = toolbarButtonSize)
                        CircleBtn(
                            Icons.Default.FilterCenterFocus, "Focus Peaking",
                            onClick = { onFocusPeaking(!focusPeaking) },
                            tint = if (focusPeaking) Color(0xFF00FFB0) else Color.White
                        , size = toolbarButtonSize)
                        CircleBtn(
                            Icons.Default.Settings, "Quick settings",
                            onClick = { showQuick = !showQuick },
                            tint = if (showQuick) CamColors.Yellow else Color.White
                        , size = toolbarButtonSize)
                        if (mode == "Photo" && nightShotMode == "star_trails") {
                            CircleBtn(
                                Icons.Default.Bedtime,
                                "Star Trails · nhấn chụp để bắt đầu/dừng",
                                onClick = onOpenSettings,
                                tint = CamColors.Yellow
                            , size = toolbarButtonSize)
                        }
                    }
    }

    val previewContent: @Composable (Modifier) -> Unit = { previewModifier ->
                    BoxWithConstraints(
                        previewModifier.background(Color(0xFF262B33)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Keep the selected aspect ratio, but fit the frame inside the actual
                        // preview slot. This prevents tall ratios and small screens from overflowing.
                        val safeHeightOverWidth = previewHeightOverWidth
                            .takeIf { it.isFinite() && it > 0f } ?: (4f / 3f)
                        val frameWidth = minOf(maxWidth, maxHeight / safeHeightOverWidth)
                        val frameHeight = frameWidth * safeHeightOverWidth
                        Box(
                            Modifier
                                .width(frameWidth)
                                .height(frameHeight)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF262B33))
                                .pointerInput(camera, uhdCaptureEnabled, fhdCaptureEnabled, uhdPlusCaptureEnabled) {
                                    detectTransformGestures { _, _, zoomChange, _ ->
                                        zoomAnimationJob?.cancel()
                                        val zoomState = camera?.cameraInfo?.zoomState?.value ?: return@detectTransformGestures
                                        val maxZoom = Camera2Info.maxSelectableZoom(
                                            activeCameraDetails, activePhysicalCameraDetails, zoomState.maxZoomRatio
                                        )
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
                                            if (manualFocus > 0f || milkyWayInfinityFocusEnabled) {
                                                manualFocus = 0f
                                                milkyWayInfinityFocusEnabled = false
                                                   runCatching { applyAutoFocus(active) }
                                            }
                                            val area = when (metering) { 0 -> 0.38f; 1 -> 0.70f; else -> 0.12f }
                                            val point = previewView.meteringPointFactory.createPoint(offset.x, offset.y, area)
                                            val action = FocusMeteringAction.Builder(
                                                point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                                            )
                                                 // Keep the tapped AF/AE region active until the next touch.
                                                 .disableAutoCancel()
                                                 .build()
                                            active.cameraControl.startFocusAndMetering(action)
                                        },
                                        onLongPress = { offset ->
                                            val active = camera ?: return@detectTapGestures
                                            if (manualFocus > 0f || milkyWayInfinityFocusEnabled) {
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
                                    modifier = Modifier.fillMaxSize()
                                )
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
                            if (faceTrackingEnabled && faceTrackCenter != null) {
                                val center = faceTrackCenter!!
                                Canvas(Modifier.fillMaxSize()) {
                                    val point = Offset(center.x * size.width, center.y * size.height)
                                    drawCircle(
                                        color = Color(0xFF58D6FF),
                                        radius = 14.dp.toPx(),
                                        center = point,
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                    drawLine(
                                        color = Color(0xFF58D6FF),
                                        start = Offset(point.x - 20.dp.toPx(), point.y),
                                        end = Offset(point.x + 20.dp.toPx(), point.y),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                    drawLine(
                                        color = Color(0xFF58D6FF),
                                        start = Offset(point.x, point.y - 20.dp.toPx()),
                                        end = Offset(point.x, point.y + 20.dp.toPx()),
                                        strokeWidth = 1.dp.toPx()
                                    )
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
                            if (levelEnabled) {
                                val levelColor = if (kotlin.math.abs(horizonRollDegrees) < 1.2f) {
                                    Color(0xFF00E676)
                                } else {
                                    Color(0xFFFFC857)
                                }
                                Box(
                                    Modifier.align(Alignment.Center).width(104.dp).height(2.dp)
                                        .graphicsLayer { rotationZ = -horizonRollDegrees }
                                        .background(levelColor)
                                )
                                Text(
                                    String.format(Locale.US, "%+.1f°", horizonRollDegrees),
                                    color = levelColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.align(Alignment.Center).padding(top = 13.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0x88000000))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                            if (zebraEnabled && zebraPoints.isNotEmpty()) {
                                Canvas(Modifier.fillMaxSize()) {
                                    val stroke = 1.5.dp.toPx()
                                    val span = 3.dp.toPx()
                                    zebraPoints.forEach { p ->
                                        val center = Offset(p.x * size.width, p.y * size.height)
                                        drawLine(
                                            color = Color(0xFFFFD54F).copy(alpha = 0.88f),
                                            start = Offset(center.x - span, center.y + span),
                                            end = Offset(center.x + span, center.y - span),
                                            strokeWidth = stroke
                                        )
                                    }
                                }
                            }
                            if (bracketCaptureActive) {
                                Text(
                                    "BRACKETING $bracketCompletedFrames",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
                                        .clip(RoundedCornerShape(8.dp)).background(Color(0xAA000000))
                                        .padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                            if (burstCaptureActive) {
                                Text(
                                    "BURST $burstCompletedFrames/25",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
                                        .clip(RoundedCornerShape(8.dp)).background(Color(0xAA000000))
                                        .padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                            if (focusLocked) {
                                Text("AF LOCK", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
                                        .clip(RoundedCornerShape(8.dp)).background(CamColors.Yellow).padding(horizontal = 9.dp, vertical = 5.dp))
                            }
                            if (showMilkyWayAdvisor) {
                                Column(
                                    Modifier.align(Alignment.TopCenter).padding(top = 35.dp, start = 8.dp, end = 8.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xD91A1D24))
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        "MILKY WAY INTELLIGENCE · CẢNH TỐI",
                                        color = CamColors.Yellow,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "ISO ${recommendedStarIso?.toString() ?: "Auto"} · " +
                                            (recommendedStarShutterSeconds?.let {
                                                if (it >= 1f) String.format(Locale.US, "%.1fs", it)
                                                else "1/${(1f / it).roundToInt()}s"
                                            } ?: "Shutter Auto") +
                                            " · WB gợi ý 3,800K",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(top = 3.dp)
                                    )
                                    Text(
                                        if (manualSensorSupported) {
                                            "Tripod · nét vô cực · Astro Stack ×8 nếu Manual Sensor hỗ trợ"
                                        } else {
                                            "HAL không công bố Manual Sensor · chụp Auto riêng lẻ, Astro Stack không khả dụng"
                                        },
                                        color = CamColors.Sub,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    Text(
                                        "Gợi ý từ độ sáng preview và thông số ống kính; không đo được mây / trăng / ô nhiễm sáng.",
                                        color = CamColors.Sub,
                                        fontSize = 8.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    if (manualSensorSupported && recommendedStarIso != null &&
                                        recommendedStarShutterSeconds != null && shutterRange != null
                                    ) {
                                        Text(
                                            if (milkyWayStackEnabled) "TẮT MILKY WAY STACK ×8"
                                            else "ÁP DỤNG GỢI Ý + BẬT ASTRO STACK ×8",
                                            color = CamColors.Yellow,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(top = 7.dp).clickable {
                                                val active = camera
                                                if (milkyWayStackEnabled) {
                                                    milkyWayStackEnabled = false
                                                    manualIsoEnabled = false
                                                    manualShutterEnabled = false
                                                    manualExposure = false
                                                    manualColorTempEnabled = false
                                                    active?.let {
                                                        runCatching {
                                                            Camera2CameraControl.from(it.cameraControl).clearCaptureRequestOptions()
                                                            applyAutoWhiteBalance(it)
                                                        }
                                                    }
                                                    manualCaptureRevision++
                                                    localizedToast(context, "Đã tắt Milky Way Stack và trả phơi sáng về Auto.", Toast.LENGTH_SHORT).show()
                                                } else if (active == null) {
                                                    localizedToast(context, "Camera chưa sẵn sàng.", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    val isoBounds = isoRange
                                                    val isoValue = if (isoBounds != null) {
                                                        recommendedStarIso.coerceIn(isoBounds.lower, isoBounds.upper)
                                                    } else recommendedStarIso
                                                    val exposureNs = (recommendedStarShutterSeconds * 1_000_000_000f)
                                                        .toLong().coerceIn(shutterRange.lower, shutterRange.upper)
                                                    val applied = runCatching {
                                                        applyManualExposure(active, isoValue, exposureNs)
                                                        applyManualWhiteBalance(active, 3800)
                                                    }.isSuccess
                                                    if (applied) {
                                                        manualIso = isoValue
                                                        manualShutterNs = exposureNs
                                                        manualIsoEnabled = true
                                                        manualShutterEnabled = true
                                                        manualExposure = true
                                                        manualColorTemp = 3800
                                                        manualColorTempEnabled = true
                                                        milkyWayStackEnabled = true
                                                        manualCaptureRevision++
                                                        localizedToast(
                                                            context,
                                                            "Đã áp dụng ISO $isoValue · ${(exposureNs / 1_000_000_000.0).let { String.format(Locale.US, "%.1fs", it) }} · WB 3800K. Chụp 8 ảnh để căn chỉnh và giảm nhiễu.",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    } else {
                                                        localizedToast(context, "Không thể áp dụng Manual Sensor/WB trên camera này.", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                        )
                                    } else {
                                        Text(
                                            "Astro Stack ×8 cần Manual Sensor, ISO và thời gian phơi sáng do Camera2 công bố.",
                                            color = CamColors.Sub,
                                            fontSize = 8.sp,
                                            modifier = Modifier.padding(top = 6.dp)
                                        )
                                    }
                                    if (manualInfinityFocusSupported) {
                                        Text(
                                            if (milkyWayInfinityFocusEnabled) "ĐANG KHÓA NÉT VÔ CỰC · CHẠM ĐỂ TRỞ VỀ AF"
                                            else "ĐẶT LẤY NÉT VÔ CỰC",
                                            color = CamColors.Yellow,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(top = 7.dp)
                                                .clickable {
                                                    val active = camera
                                                    if (active != null) {
                                                        if (milkyWayInfinityFocusEnabled) {
                                                            milkyWayInfinityFocusEnabled = false
                                                            runCatching { applyAutoFocus(active) }
                                                            localizedToast(context, "Đã trở về AF tự động.", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            val applied = runCatching { applyManualFocus(active, 0f) }.isSuccess
                                                            if (applied) {
                                                                manualFocus = 0f
                                                                milkyWayInfinityFocusEnabled = true
                                                                focusLocked = false
                                                                localizedToast(context, "Đã đặt nét vô cực theo Camera2.", Toast.LENGTH_SHORT).show()
                                                            } else {
                                                                localizedToast(context, "Camera không chấp nhận lấy nét vô cực.", Toast.LENGTH_LONG).show()
                                                            }
                                                        }
                                                    }
                                                }
                                        )
                                    }
                                }
                            }
                            Text(
                                if (focusLocked) "AF LOCK" else "TAP AF · HOLD LOCK",
                                color = if (focusLocked) CamColors.Yellow else Color.White.copy(alpha = 0.72f),
                                fontSize = 8.sp,
                                modifier = Modifier.align(Alignment.TopEnd).padding(7.dp)
                                    .clip(RoundedCornerShape(6.dp)).background(Color(0x66000000))
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                            if (batteryPercent >= 0) {
                                Row(
                                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 29.dp, end = 7.dp)
                                        .clip(RoundedCornerShape(6.dp)).background(Color(0x66000000))
                                        .padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Canvas(Modifier.size(width = 14.dp, height = 10.dp)) {
                                        val bodyWidth = size.width * 0.82f
                                        val inset = 1.dp.toPx()
                                        val radius = androidx.compose.ui.geometry.CornerRadius(1.5.dp.toPx())
                                        drawRoundRect(
                                            color = Color.White.copy(alpha = 0.92f),
                                            topLeft = Offset.Zero,
                                            size = DrawSize(bodyWidth, size.height),
                                            cornerRadius = radius,
                                            style = Stroke(width = 1.dp.toPx())
                                        )
                                        val innerWidth = ((bodyWidth - inset * 3f).coerceAtLeast(0f)) *
                                            (batteryPercent / 100f)
                                        drawRoundRect(
                                            color = Color.White,
                                            topLeft = Offset(inset, inset),
                                            size = DrawSize(innerWidth, (size.height - inset * 2f).coerceAtLeast(0f)),
                                            cornerRadius = radius
                                        )
                                        drawRoundRect(
                                            color = Color.White.copy(alpha = 0.92f),
                                            topLeft = Offset(size.width * 0.86f, size.height * 0.31f),
                                            size = DrawSize(size.width * 0.12f, size.height * 0.38f),
                                            cornerRadius = radius
                                        )
                                    }
                                    Text(
                                        "$batteryPercent%",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        lineHeight = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            LensTransitionOverlay(
                                visible = lensTransitionVisible,
                                lensLabel = lensTransitionLabel,
                                modifier = Modifier.align(Alignment.Center)
                            )
                            if (showHist) {
                                Histogram(hist, Modifier.align(Alignment.TopStart).padding(8.dp).size(106.dp, 52.dp))
                            }
                            if (rgbHistogramEnabled) {
                                val rgbTop = if (showHist) 68.dp else 8.dp
                                RgbHistogram(
                                    rgbRedHistogram, rgbGreenHistogram, rgbBlueHistogram,
                                    Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = rgbTop).size(106.dp, 52.dp)
                                )
                            }
                            if (!hasCam) {
                                Text(
                                    "Cần cấp quyền Camera\n(chạm để cấp)",
                                    color = Color.White, textAlign = TextAlign.Center,
                                    modifier = Modifier.align(Alignment.Center)
                                        .animaTClickable { permissionLauncher.launch(Manifest.permission.CAMERA) }
                                )
                            }
                            if (previewFreezeVisible && previewFreezeEffect != "off") {
                                val effectMode = previewFreezeEffect
                                val overlayAlpha = previewFreezeAlpha.value
                                Box(Modifier.fillMaxSize()) {
                                    frozenPreviewBitmap?.let { frame ->
                                        Image(
                                            bitmap = frame,
                                            contentDescription = "Khung hình Preview được đóng băng",
                                            modifier = Modifier.fillMaxSize()
                                                .blur(16.dp)
                                                .graphicsLayer { alpha = overlayAlpha },
                                            contentScale = ContentScale.FillBounds
                                        )
                                    }
                                    Box(
                                        Modifier.fillMaxSize()
                                            .background(Color(0x55080D18))
                                            .graphicsLayer { alpha = overlayAlpha }
                                    )
                                    Canvas(
                                        Modifier.fillMaxSize().graphicsLayer { alpha = overlayAlpha }
                                    ) {
                                        val position = previewEffectPosition.value
                                        if (effectMode == "luna") {
                                            val rainbow = listOf(
                                                Color(0xFFFF668A), Color(0xFFFFB86B), Color(0xFFFFE58A),
                                                Color(0xFF78E8B2), Color(0xFF70D4FF), Color(0xFF9C8BFF),
                                                Color(0xFFE6A0FF)
                                            )
                                            val drift = ((position + 0.35f) / 1.70f).coerceIn(0f, 1f)
                                            val shortSide = minOf(size.width, size.height)
                                            rainbow.forEachIndexed { index, color ->
                                                val phase = (drift * 0.58f + index * 0.173f) % 1.35f
                                                val centerX = (phase - 0.16f) * size.width
                                                val centerY = (((index * 0.23f + drift * 0.42f) % 1.28f) - 0.12f) * size.height
                                                val center = Offset(centerX, centerY)
                                                drawCircle(
                                                    color = color.copy(alpha = 0.095f),
                                                    radius = shortSide * 0.24f,
                                                    center = center
                                                )
                                                drawLine(
                                                    color = color.copy(alpha = 0.075f),
                                                    start = Offset(centerX - size.width * 0.62f, centerY + size.height * 0.28f),
                                                    end = Offset(centerX + size.width * 0.62f, centerY - size.height * 0.28f),
                                                    strokeWidth = shortSide * 0.22f,
                                                    cap = StrokeCap.Round
                                                )
                                            }
                                        } else {
                                            val headX = size.width * position
                                            val headY = size.height * (0.48f - position * 0.10f)
                                            val head = Offset(headX, headY)
                                            val tail = Offset(headX - size.width * 0.24f, headY + size.height * 0.10f)
                                            drawLine(
                                                color = Color(0xFF8EDBFF).copy(alpha = 0.36f),
                                                start = tail,
                                                end = head,
                                                strokeWidth = 18.dp.toPx(),
                                                cap = StrokeCap.Round
                                            )
                                            drawLine(
                                                color = Color.White.copy(alpha = 0.90f),
                                                start = tail,
                                                end = head,
                                                strokeWidth = 4.dp.toPx(),
                                                cap = StrokeCap.Round
                                            )
                                            drawCircle(Color(0xFFFFF4C2).copy(alpha = 0.34f), 15.dp.toPx(), head)
                                            drawCircle(Color.White, 5.dp.toPx(), head)
                                        }
                                    }
                                    Box(
                                        Modifier.fillMaxSize().clickable(
                                            interactionSource = previewFreezeBlockInteraction,
                                            indication = null
                                        ) {}
                                    )
                                    Text(
                                        if (effectMode == "luna") "LUNADREAM · ĐANG XỬ LÝ" else "NIGHT SHOT · ĐANG GHÉP ẢNH",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.align(Alignment.Center)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0xAA111722))
                                            .padding(horizontal = 15.dp, vertical = 9.dp)
                                            .graphicsLayer { alpha = overlayAlpha }
                                    )
                                }
                            }
                        }
                    }
    }

    val controlsContent: @Composable (Modifier) -> Unit = { controlsModifier ->
        Column(controlsModifier) {
                        // Lens quick-select values come from the physical cameras exposed by
                        // this logical CameraX camera, then are clamped to its real zoom range.
                        val zoomStateForUi = camera?.cameraInfo?.zoomState?.value
                        val zoomMinForUi = zoomStateForUi?.minZoomRatio ?: 1f
                        val zoomMaxForUi = Camera2Info.maxSelectableZoom(
                            activeCameraDetails,
                            activePhysicalCameraDetails,
                            zoomStateForUi?.maxZoomRatio ?: 1f
                        )
                        val zoomSteps = Camera2Info.opticalZoomStops(
                            logical = activeCameraDetails,
                            physical = activePhysicalCameraDetails,
                            minZoom = zoomMinForUi,
                            maxZoom = zoomMaxForUi,
                            quickMaxZoom = zoomMaxForUi
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
                                            .animaTClickable {
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
                                                    fontSize = 10.sp, modifier = Modifier.padding(start = 8.dp).animaTClickable {
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
                                                    fontSize = 10.sp, modifier = Modifier.padding(start = 8.dp).animaTClickable {
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
                                                            milkyWayInfinityFocusEnabled = false
                                                            focusLocked = false
                                                            camera?.let { runCatching { applyManualFocus(it, manualFocus) } }
                                                        },
                                                        colors = cameraSliderColors()
                                                    )
                                                } else Text("AF tự động", color = CamColors.Sub, fontSize = 10.sp, modifier = Modifier.weight(1f))
                                                Text("AUTO", color = CamColors.Yellow, fontSize = 10.sp,
                                                    modifier = Modifier.padding(start = 8.dp).animaTClickable {
                                                        manualFocus = 0f
                                                        milkyWayInfinityFocusEnabled = false
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
                                                    fontSize = 10.sp, modifier = Modifier.padding(start = 8.dp).animaTClickable {
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
                            val proItems = buildList {
                                if (proHudShowEv) add(Triple("EV", evText, "EV"))
                                if (proHudShowShutter) add(Triple("Tv", shutterText, "Tv"))
                                if (proHudShowIso) add(Triple("ISO", isoText, "ISO"))
                                if (proHudShowFocus) {
                                    add(Triple("AF", if (manualFocus > 0f) "%.1fD".format(Locale.US, manualFocus) else "AF", "AF"))
                                }
                                if (proHudShowWhiteBalance) add(Triple("Tone", toneText, "WB"))
                                add(Triple("AUTO", "AUTO", "RESET"))
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                proItems.forEach { (key, value, label) ->
                                    val active = proSel == key
                                    Column(
                                        Modifier.weight(1f).clip(RoundedCornerShape(11.dp)).background(CamColors.Glass)
                                            .border(0.5.dp, if (active) CamColors.Yellow else CamColors.Stroke, RoundedCornerShape(11.dp))
                                            .animaTClickable {
                                                if (key == "AUTO") {
                                                    manualExposure = false
                                                    manualIsoEnabled = false
                                                    manualShutterEnabled = false
                                                    manualColorTempEnabled = false
                                                    manualFocus = 0f
                                                    milkyWayInfinityFocusEnabled = false
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
                                                        milkyWayInfinityFocusEnabled = false
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
                                    if (nightShotMode == "night") "NightShotMode" else "Star Trails",
                                    color = CamColors.Yellow,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    if (nightShotMode == "night") "AUTO · EV 0.0 · ${nightShotDurationSeconds}s · MULTI-FRAME"
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
                                    modifier = Modifier.padding(start = 10.dp).animaTClickable {
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
                                .animaTClickable { onOpenAlbum(lastUri) }, contentAlignment = Alignment.Center) {
                                val image = thumb
                                if (image != null) Image(image, "Mở ảnh vừa chụp", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                else Icon(Icons.Default.PhotoLibrary, "Mở album", tint = Color.White)
                            }
                            Box(Modifier.size(72.dp).border(4.dp, Color.White.copy(alpha = if (isProcessing) 0.45f else 1f), CircleShape)
                                .padding(7.dp).clip(CircleShape)
                                .background(
                                    when {
                                        isProcessing -> Color(0xFF59616D)
                                        starTrailCaptureInProgress || burstCaptureActive || bracketCaptureActive || livePhotoCaptureInProgress -> Color(0xFFE53935)
                                        else -> CamColors.Shutter
                                    }
                                )
                                .graphicsLayer { alpha = if (isProcessing) 0.55f else 1f }
                                .animaTClickable { if (!isProcessing) takePhoto() },
                                contentAlignment = Alignment.Center) {
                                if (starTrailCaptureInProgress || burstCaptureActive || bracketCaptureActive) {
                                    Icon(
                                        Icons.Default.Close,
                                        if (bracketCaptureActive) "Bracketing" else if (burstCaptureActive) "Dừng Burst" else "Dừng Star Trails",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
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
                                    .animaTClickable {
                                        mode = item
                                        if (item == "Photo") {
                                            proSel = null
                                            manualExposure = false
                                            manualIsoEnabled = false
                                            manualShutterEnabled = false
                                            manualColorTempEnabled = false
                                            manualFocus = 0f
                                            milkyWayInfinityFocusEnabled = false
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
                                .animaTClickable {
                                    if (activeFilter != "None") {
                                        activeFilter = "None"
                                        appPrefs.edit().putString("selected_filter", "None").apply()
                                    } else {
                                        activeFilter = appPrefs.getString("selected_filter", "None") ?: "None"
                                        filterDialogVisible = true
                                    }
                                }
                                .padding(horizontal = 18.dp, vertical = 7.dp)) {
                                Text(
                                    if (activeFilter != "None") "Bộ lọc: " + cameraLookDisplayName(context, activeFilter) else "Bộ lọc",
                                    color = if (activeFilter != "None") CamColors.Yellow else Color.White,
                                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Box(Modifier.clip(RoundedCornerShape(16.dp))
                                .background(
                                    when {
                                        livePhotoEnabled || nativeImageProcessingEnabled -> Color(0x553A3A3A)
                                        lunaDreamMode != "off" || nightShotMode == "night" -> Color(0x66F5C842)
                                        else -> CamColors.Glass
                                    }
                                )
                                .animaTClickable {
                                    when {
                                        livePhotoEnabled -> localizedToast(
                                            context,
                                            "Vui lòng tắt Live Photo",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        nativeImageProcessingEnabled -> localizedToast(
                                            context,
                                            "Vui lòng tắt Xử lý ảnh gốc thiết bị để sử dụng LunaDream.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        else -> showLunaDream = true
                                    }
                                }
                                .padding(horizontal = 15.dp, vertical = 7.dp)) {
                                Text("LunaDream",
                                    color = if (livePhotoEnabled || nativeImageProcessingEnabled) Color(0xFF777777)
                                        else if (lunaDreamMode != "off" || nightShotMode == "night") CamColors.Yellow
                                        else CamColors.TextColor,
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
        }
    }

    Box(
        Modifier.fillMaxSize().background(CamColors.Bg).navigationBarsPadding().animaTContent()
            .pointerInput(hudAutoHideEnabled) {
                if (hudAutoHideEnabled) {
                    awaitPointerEventScope {
                        var wasPressed = false
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val pressed = event.changes.any { it.pressed }
                            if (pressed && !wasPressed) hudActivityToken++
                            wasPressed = pressed
                        }
                    }
                }
            }
    ) {
        Column(
            Modifier.fillMaxSize().padding(
                horizontal = if (uiIsLandscape) 10.dp else 12.dp,
                vertical = 7.dp
            )
        ) {
            AnimatedVisibility(
                visible = hudVisible,
                enter = fadeIn(tween(180)) + slideInVertically { -it / 12 },
                exit = fadeOut(tween(170)) + slideOutVertically { -it / 16 }
            ) {
                toolbarContent(Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(if (uiIsLandscape) 5.dp else 7.dp))
            previewContent(Modifier.weight(1f).fillMaxWidth())
            AnimatedVisibility(
                visible = hudVisible,
                enter = fadeIn(tween(180)) + slideInVertically { it / 14 },
                exit = fadeOut(tween(170)) + slideOutVertically { it / 18 }
            ) {
                controlsContent(
                    Modifier.fillMaxWidth()
                        .then(if (uiIsLandscape) Modifier.heightIn(max = 148.dp) else Modifier)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = if (uiIsLandscape) 4.dp else 0.dp)
                )
            }
        }
        if (showQuick) {
            QuickPanel(
                ratioIdx = ratioIdx,
                onRatio = onRatio,
                deviceLandscape = uiIsLandscape,
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
                metering = meteringModes[metering],
                onMetering = { metering = (metering + 1) % meteringModes.size },
                renderEngine = renderEngine,
                onRenderEngine = { renderEngine = if (renderEngine == "Performance") "Compatible" else "Performance" },
                livePhotoEnabled = livePhotoEnabled,
                onLivePhotoChanged = { enabled ->
                    livePhotoEnabled = enabled
                    if (enabled) {
                        nativeImageProcessingEnabled = false
                        appPrefs.edit().putBoolean("native_image_processing_enabled", false).apply()
                        showLunaDream = false
                        burstMode = false
                        hdrBracketMode = false
                        focusBracketMode = false
                        if (nightShotMode != "off") {
                            nightShotMode = "off"
                            appPrefs.edit().putString("nightshot_mode", "off").apply()
                        }
                    }
                },
                nativeImageProcessingEnabled = nativeImageProcessingEnabled,
                onNativeImageProcessingChanged = { enabled ->
                    nativeImageProcessingEnabled = enabled
                    if (enabled) {
                        livePhotoEnabled = false
                        appPrefs.edit().putBoolean("live_photo_enabled", false).apply()
                        showLunaDream = false
                        // NightShot and Star Trails use the app's own multi-frame pipeline, not the OEM still-photo path.
                        if (nightShotMode != "off") {
                            nightShotMode = "off"
                            appPrefs.edit().putString("nightshot_mode", "off").apply()
                        }
                    }
                },
                onToolbox = { showQuick = false; onOpenToolbox() },
                onContentManagement = { showQuick = false; onOpenContentManagement() },
                onSettings = { showQuick = false; onOpenSettings() },
                modifier = Modifier.align(if (uiIsLandscape) Alignment.TopEnd else Alignment.TopCenter)
                    .padding(start = 10.dp, end = 10.dp, top = if (uiIsLandscape) 8.dp else 64.dp)
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
                    Text(
                        if (milkyWayStackEnabled) "NightShotMode · MILKY WAY" else "NightShotMode · ĐANG CHỤP",
                        color = CamColors.Yellow, fontSize = 14.sp, fontWeight = FontWeight.Bold
                    )
                    Text(
                        "$nightShotCapturedFrameCount/$nightShotTargetFrameCount khung hình · đủ khung mới render",
                        color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 7.dp)
                    )
                    Text("Giữ máy ổn định đến khi thu và ghép xong. Khi LunaDream bắt đầu xử lý, có thể di chuyển tự do.",
                        color = CamColors.Sub, fontSize = 11.sp,
                        modifier = Modifier.padding(top = 5.dp))
                }
            }
        }
        if (isProcessing) {
            Column(
                Modifier.align(Alignment.BottomCenter)
                    .offset { IntOffset(processingPanelOffset.x.roundToInt(), processingPanelOffset.y.roundToInt()) }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            processingPanelOffset = Offset(
                                (processingPanelOffset.x + dragAmount.x).coerceIn(-220f, 220f),
                                (processingPanelOffset.y + dragAmount.y).coerceIn(-260f, 260f)
                            )
                        }
                    }
                    .padding(horizontal = 28.dp, vertical = 18.dp)
                    .fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(CamColors.Panel).padding(14.dp)
            ) {
                val visibleProgress = if (processingLabel == "Nhiễu đang được xử lý") {
                    noiseReductionProgress
                } else processingProgress
                val etaText = when {
                    processingEtaSeconds < 0 -> ""
                    processingEtaSeconds >= 60 -> "Còn ${processingEtaSeconds / 60}p ${processingEtaSeconds % 60}s"
                    else -> "Còn ${processingEtaSeconds}s"
                }
                val elapsedText = when {
                    processingElapsedSeconds >= 60 ->
                        "${processingElapsedSeconds / 60}p ${processingElapsedSeconds % 60}s"
                    else -> "${processingElapsedSeconds}s"
                }
                val captureStatus = when {
                    multiFrameHoldActive || nightShotCaptureInProgress || starTrailCaptureInProgress ||
                        burstCaptureActive || bracketCaptureActive ->
                        "Giữ thiết bị ổn định"
                    multiFrameRenderStarted ->
                        "Bạn có thể di chuyển thiết bị tự do"
                    else -> ""
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(processingLabel, color = Color.White, fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(
                        if (etaText.isNotEmpty()) "${etaText} · ${visibleProgress}%" else "${visibleProgress}%",
                        color = CamColors.Yellow, fontSize = 11.sp, fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    "Đã xử lý: $elapsedText",
                    color = CamColors.Sub,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 5.dp)
                )
                if (captureStatus.isNotEmpty()) {
                    Text(
                        captureStatus,
                        color = CamColors.Sub,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
                val androidThermalText = when (thermalStatusCode) {
                    PowerManager.THERMAL_STATUS_LIGHT -> "Nhẹ"
                    PowerManager.THERMAL_STATUS_MODERATE -> "Trung bình"
                    PowerManager.THERMAL_STATUS_SEVERE -> "Cao"
                    PowerManager.THERMAL_STATUS_CRITICAL -> "Nghiêm trọng"
                    PowerManager.THERMAL_STATUS_EMERGENCY -> "Khẩn cấp"
                    PowerManager.THERMAL_STATUS_SHUTDOWN -> "Ngưỡng tắt bảo vệ"
                    else -> "Bình thường / chưa báo"
                }
                val thermalLoadText = when {
                    measuredBatteryTempC >= 43 -> "Giảm tải mạnh"
                    measuredBatteryTempC >= 41 -> "Đang điều tiết tải"
                    measuredBatteryTempC > 40 -> "Đang giảm tải"
                    measuredBatteryTempC >= 0 -> "Bình thường"
                    else -> "Không có dữ liệu pin"
                }
                Text(
                    "Nhiệt pin: ${if (measuredBatteryTempC >= 0) "$measuredBatteryTempC°C" else "--"} · $thermalLoadText",
                    color = if (measuredBatteryTempC > 40) CamColors.Yellow else CamColors.Sub,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 5.dp)
                )
                Text(
                    "Trạng thái nhiệt Android: $androidThermalText",
                    color = CamColors.Sub,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 3.dp)
                )
                Canvas(Modifier.fillMaxWidth().padding(top = 9.dp).height(5.dp)) {
                    drawRoundRect(Color(0xFF454545), cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f))
                    drawRoundRect(CamColors.Yellow, size = DrawSize(size.width * visibleProgress / 100f, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f))
                }
            }
        }

        if (filterDialogVisible) {
            FilterLookDialog(
                previewSource = filterPreviewSourceBitmap ?: fallbackFilterPreviewBitmap,
                activeFilter = activeFilter,
                onDismiss = { filterDialogVisible = false },
                onSelectFilter = { selected ->
                    val nextFilter = if (selected == activeFilter) "None" else selected
                    activeFilter = nextFilter
                    appPrefs.edit().putString("selected_filter", nextFilter).apply()
                }
            )
        }

        if (showLunaDream && !nativeImageProcessingEnabled && !livePhotoEnabled) {
            LunaDreamBottomSheet(
                onDismiss = { showLunaDream = false },
                nightShotEnabled = nightShotMode == "night",
                nightShotDurationSeconds = nightShotDurationSeconds,
                onCycleNightShot = {
                    if (nightShotMode != "night") {
                        nightShotMode = "night"
                        nightShotDurationSeconds = 5
                    } else {
                        when (nightShotDurationSeconds) {
                            5 -> nightShotDurationSeconds = 10
                            10 -> nightShotDurationSeconds = 3
                            else -> {
                                nightShotMode = "off"
                                nightShotDurationSeconds = 5
                            }
                        }
                    }
                },
                selectedMode = lunaDreamMode,
                onSelectedModeChange = {
                    lunaDreamMode = it
                    appPrefs.edit().putString("lunadream_mode", it).apply()
                }
            )
        }

        if (photoViewerVisible) {
            Dialog(
                onDismissRequest = { photoViewerVisible = false; viewerBitmap = null; viewerOriginalBitmap = null; viewerShowsOriginal = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    val image = if (viewerShowsOriginal) viewerOriginalBitmap else (viewerBitmap ?: thumb)
                    if (image != null) {
                        Image(
                            bitmap = image,
                            contentDescription = if (viewerShowsOriginal) "Ảnh gốc trước LunaDream" else "Ảnh sau LunaDream",
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text("Đang tải ảnh...", color = Color.White, modifier = Modifier.align(Alignment.Center))
                    }
                    if (viewerOriginalBitmap != null) {
                        Column(
                            Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                if (viewerShowsOriginal) "ẢNH GỐC · TRƯỚC LUNADREAM" else "ẢNH ĐÃ XỬ LÝ · LUNADREAM",
                                color = CamColors.Yellow,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                Modifier.clip(RoundedCornerShape(24.dp)).background(Color(0xDD252525))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(false to "Sau LunaDream", true to "Ảnh gốc").forEach { (showOriginal, label) ->
                                    Text(
                                        label,
                                        color = if (viewerShowsOriginal == showOriginal) Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clip(RoundedCornerShape(18.dp))
                                            .background(if (viewerShowsOriginal == showOriginal) CamColors.Yellow else Color.Transparent)
                                            .clickable { viewerShowsOriginal = showOriginal }
                                            .padding(horizontal = 14.dp, vertical = 9.dp)
                                    )
                                }
                            }
                        }
                    }
                    Box(Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                        CircleBtn(
                            Icons.Default.Close, "Đóng ảnh",
                            onClick = { photoViewerVisible = false; viewerBitmap = null; viewerOriginalBitmap = null; viewerShowsOriginal = false },
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

@Composable
private fun FilterLookPreviewImage(
    source: Bitmap,
    preset: String,
    modifier: Modifier,
    maxDimension: Int
) {
    val context = LocalContext.current
    val renderedState by produceState(
        initialValue = Pair(false, null as ImageBitmap?),
        source,
        preset,
        maxDimension
    ) {
        val rendered = withContext(Dispatchers.Default) {
            renderCameraLookPreview(context, source, preset, maxDimension)
        }
        value = true to rendered?.asImageBitmap()
    }
    val preview = renderedState.second
    when {
        preview != null -> {
            Image(
                bitmap = preview,
                contentDescription = "$preset preview",
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
        }
        renderedState.first -> {
            Box(
                modifier.clip(RoundedCornerShape(8.dp)).background(CamColors.Card),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Không thể tạo preview cho bộ lọc này",
                    color = CamColors.Sub,
                    fontSize = 10.sp
                )
            }
        }
        else -> {
            Box(modifier) {
                Image(
                    bitmap = source.asImageBitmap(),
                    contentDescription = "Preview source while rendering $preset",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Text(
                    "Đang tạo preview…",
                    color = Color.White,
                    fontSize = 9.sp,
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .background(Color(0xAA000000))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun FilterLookDialog(
    previewSource: Bitmap,
    activeFilter: String,
    onDismiss: () -> Unit,
    onSelectFilter: (String) -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    val dialogMaxHeight = (configuration.screenHeightDp * if (isLandscape) 0.94f else 0.84f).dp
    val previewHeight = if (isLandscape) 112.dp else 150.dp
    val scope = rememberCoroutineScope()
    var importedLuts by remember { mutableStateOf(loadCustomCameraLuts(context)) }
    var filterSearch by remember { mutableStateOf("") }
    var selectedLookCategory by remember { mutableStateOf("Tất cả") }
    var activeLutIds by remember { mutableStateOf(activeCustomCameraLutIds(context)) }
    var isImportingLut by remember { mutableStateOf(false) }

    val lutPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                isImportingLut = true
                try {
                    val imported = withContext(Dispatchers.IO) {
                        importCustomCameraLuts(context, uri)
                    }
                    importedLuts = loadCustomCameraLuts(context)
                    activeLutIds = activeCustomCameraLutIds(context)
                    Toast.makeText(
                        context,
                        "Đã nhập " + imported.size + " LUT. Vào Content Manager để Add LUT vào bộ lọc.",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (failure: Throwable) {
                    Toast.makeText(
                        context,
                        "Import LUT thất bại: " + (failure.message ?: "không rõ lỗi"),
                        Toast.LENGTH_LONG
                    ).show()
                } finally {
                    isImportingLut = false
                }
            }
        }
    }

    // Log-input transforms are intentionally kept in LUT Manage; they must not be
    // applied to an ordinary sRGB camera JPEG as if they were creative photo filters.
    val activeCustomLuts = importedLuts.filter {
        activeLutIds.contains(it.id) && !it.category.startsWith("RAW/Log")
    }
    val allLookEntries = cameraLookGroups.flatMap { (category, presets) ->
        presets.map { category to it }
    } + activeCustomLuts.map { it.category to customCameraLutPreset(it.id) }
    val lookCategories = listOf("Tất cả") + allLookEntries.map { it.first }.distinct()
    val visiblePresets = allLookEntries
        .filter { selectedLookCategory == "Tất cả" || it.first == selectedLookCategory }
        .map { it.second }
        .filter {
            filterSearch.isBlank() ||
                cameraLookDisplayName(context, it).contains(filterSearch.trim(), ignoreCase = true)
        }
    val activeFilterName = cameraLookDisplayName(context, activeFilter)

    val previewPane: @Composable (Modifier) -> Unit = { paneModifier ->
        Box(
            paneModifier.fillMaxWidth().height(previewHeight)
                .clip(RoundedCornerShape(12.dp)).background(CamColors.Card)
        ) {
            FilterLookPreviewImage(
                source = previewSource,
                preset = activeFilter,
                modifier = Modifier.fillMaxSize(),
                maxDimension = 720
            )
            if (activeFilter != "None") {
                Text(
                    activeFilterName,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.BottomStart)
                        .background(Color(0xAA000000)).padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }
    }
    val searchPane: @Composable (Modifier) -> Unit = { paneModifier ->
        Box(
            paneModifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(CamColors.Card).padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            if (filterSearch.isBlank()) {
                Text("Tìm bộ lọc…", color = CamColors.Sub, fontSize = 12.sp)
            }
            BasicTextField(
                value = filterSearch,
                onValueChange = { filterSearch = it },
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    val categoryPane: @Composable (Modifier) -> Unit = { paneModifier ->
        Row(
            paneModifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(top = 7.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            lookCategories.forEach { category ->
                val selected = selectedLookCategory == category
                Box(
                    Modifier.clip(RoundedCornerShape(14.dp))
                        .background(if (selected) CamColors.Yellow else CamColors.Card)
                        .animaTClickable { selectedLookCategory = category }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        category,
                        color = if (selected) Color.Black else CamColors.TextColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
    val presetGrid: @Composable (Modifier) -> Unit = { listModifier ->
        LazyColumn(listModifier, verticalArrangement = Arrangement.spacedBy(0.dp)) {
            items(visiblePresets.chunked(2), key = { pair -> pair.firstOrNull() ?: "empty" }) { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { preset ->
                        val selected = activeFilter == preset
                        val itemName = cameraLookDisplayName(context, preset)
                        Column(
                            Modifier.weight(1f).padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) Color(0xFF393326) else CamColors.Card)
                                .animaTClickable { onSelectFilter(preset) }.padding(7.dp)
                        ) {
                            Box(Modifier.fillMaxWidth().height(76.dp).clip(RoundedCornerShape(8.dp))) {
                                FilterLookPreviewImage(
                                    source = previewSource,
                                    preset = preset,
                                    modifier = Modifier.fillMaxSize(),
                                    maxDimension = 192
                                )
                                if (selected) {
                                    Text(
                                        "✓",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        modifier = Modifier.align(Alignment.BottomStart)
                                            .background(Color(0xAA000000)).padding(4.dp)
                                    )
                                }
                            }
                            Text(
                                itemName,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 7.dp)
                            )
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxWidth(0.96f)
                .then(if (isLandscape) Modifier.fillMaxHeight(0.94f) else Modifier.heightIn(max = dialogMaxHeight))
                .clip(RoundedCornerShape(22.dp)).background(CamColors.Bg).padding(15.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Bộ lọc phim", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Đóng",
                    color = CamColors.Yellow,
                    modifier = Modifier.animaTClickable(onClick = onDismiss).padding(6.dp),
                    fontSize = 12.sp
                )
            }
            if (isLandscape) {
                Row(
                    Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(
                        Modifier.weight(0.92f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            if (activeFilter == "None") "Preview · Ảnh gốc" else "Preview · " + activeFilterName,
                            color = CamColors.Yellow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        previewPane(Modifier)
                        searchPane(Modifier)
                        categoryPane(Modifier)
                        Text("${visiblePresets.size} bộ lọc", color = CamColors.Sub, fontSize = 10.sp)
                    }
                    presetGrid(Modifier.weight(1.28f).fillMaxHeight())
                }
            } else {
                Text(
                    if (activeFilter == "None") "Preview · Ảnh gốc" else "Preview · " + activeFilterName,
                    color = CamColors.Yellow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                previewPane(Modifier)
                searchPane(Modifier)
                categoryPane(Modifier)
                Text(
                    "${visiblePresets.size} bộ lọc",
                    color = CamColors.Sub,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(bottom = 5.dp)
                )
                presetGrid(Modifier.weight(1f))
            }
        }
    }
}

// Filter groups include the original brand/film looks which were previously hidden
// when the picker was flattened into a single "Bộ lọc" category.
private val cameraLookGroups = listOf(
    "Fujifilm" to listOf(
        "Provia", "Velvia", "Astia", "Classic Chrome", "Classic Neg", "Eterna",
        "Pro Neg Std", "Pro Neg Hi", "Nostalgic Neg", "Reala Ace", "Bleach Bypass", "Acros"
    ),
    "Phim màu" to listOf(
        "Portra 160", "Portra 400", "Portra 800", "Gold 200", "Ektar 100",
        "Kodachrome 64", "ColorPlus 200", "CineStill 800T", "Vision3 250D",
        "Vision3 500T", "400H", "Ektachrome 100"
    ),
    "Cổ điển" to listOf("Kodak Portra 400", "Agfa", "Kodak Gold", "Fuji 400"),
    "Đen trắng" to listOf("Tri-X 400", "T-Max 400", "Monochrome"),
    "Leica" to listOf("Leica Vivid", "Leica Natural", "Leica Mono"),
    "Hasselblad" to listOf("Hasselblad HNCS"),
    "Zeiss" to listOf("Zeiss Cool", "Zeiss Warm"),
    "Sony" to listOf("Sony S-Cinetone", "Sony S-Log3"),
    "Điện ảnh / Log" to listOf("Cine Tone", "Cinematic Log", "Soft Log"),
    "Cơ bản" to listOf("Vivid", "Natural", "Cool", "Warm")
)

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
        .animaTClickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 7.dp)) {
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

@Composable
private fun RgbHistogram(
    red: IntArray,
    green: IntArray,
    blue: IntArray,
    modifier: Modifier = Modifier
) {
    Canvas(modifier.background(Color(0x77000000), RoundedCornerShape(6.dp))) {
        val maxValue = maxOf(
            red.maxOrNull() ?: 0,
            green.maxOrNull() ?: 0,
            blue.maxOrNull() ?: 0,
            1
        ).toFloat()
        val stroke = 1.25.dp.toPx()
        fun drawSeries(values: IntArray, color: Color) {
            if (values.size < 2) return
            for (index in 0 until values.lastIndex) {
                val x1 = size.width * index / (values.size - 1f)
                val x2 = size.width * (index + 1) / (values.size - 1f)
                val y1 = size.height * (1f - values[index] / maxValue)
                val y2 = size.height * (1f - values[index + 1] / maxValue)
                drawLine(color, Offset(x1, y1), Offset(x2, y2), strokeWidth = stroke)
            }
        }
        drawSeries(red, Color(0xFFFF5252))
        drawSeries(green, Color(0xFF69F0AE))
        drawSeries(blue, Color(0xFF448AFF))
    }
}
