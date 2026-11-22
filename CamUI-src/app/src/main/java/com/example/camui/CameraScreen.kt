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
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.HapticFeedbackConstants
import android.view.OrientationEventListener
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.geometry.Size as DrawSize
import androidx.compose.ui.graphics.nativeCanvas
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Observer
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
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
 * Tracks only the two physical landscape quadrants while the app remains landscape-only.
 * The circular low-pass filter and hysteresis prevent rotation oscillation near boundaries.
 */
private class SensorOrientationManager {
    private var smoothedDegrees: Float? = null
    private var lastValidRotation: Int? = null

    fun update(rawDegrees: Int): Int? {
        if (rawDegrees == OrientationEventListener.ORIENTATION_UNKNOWN) return null
        val raw = ((rawDegrees % 360) + 360) % 360
        val previous = smoothedDegrees
        val filtered = if (previous == null) {
            raw.toFloat()
        } else {
            val delta = ((raw - previous + 540f) % 360f) - 180f
            (previous + delta * 0.28f + 360f) % 360f
        }
        smoothedDegrees = filtered

        // Only landscape quadrants are accepted. Portrait quadrants return the last
        // landscape orientation, preventing portrait ImageCapture target rotation.
        val candidate = LandscapeRotationPolicy.landscapeCandidate(filtered) ?: return lastValidRotation
        val current = lastValidRotation
        if (current == null) {
            lastValidRotation = candidate
            return candidate
        }
        if (candidate != current) {
            val center = when (candidate) {
                Surface.ROTATION_0 -> 0f
                Surface.ROTATION_90 -> 270f
                Surface.ROTATION_180 -> 180f
                else -> 90f
            }
            val difference = kotlin.math.abs(((filtered - center + 540f) % 360f) - 180f)
            if (difference <= 33f) lastValidRotation = candidate
        }
        return lastValidRotation
    }
}

private const val DEBUG_FUJI_LAYOUT = false

private enum class RatioUiTransitionState { Idle, FadingOut, Swapping, FadingIn }

private val meteringModes = listOf("Center", "Matrix", "Spot")
private val wbModes = listOf(
    "Auto" to CaptureRequest.CONTROL_AWB_MODE_AUTO,
    "Daylight" to CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT,
    "Cloudy" to CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT,
    "Tungsten" to CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT,
    "Fluorescent" to CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT
)

private val systemShutterSound = MediaActionSound().apply {
    load(MediaActionSound.SHUTTER_CLICK)
}

private fun playSystemShutterFallback() {
    runCatching { systemShutterSound.play(MediaActionSound.SHUTTER_CLICK) }
        .onFailure { android.util.Log.w("FUJI_SHUTTER", "System shutter fallback failed", it) }
}

private fun playConfiguredShutterSound(context: Context) {
    val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    val player = runCatching {
        MediaPlayer.create(
            context,
            R.raw.lunadream_shutter,
            attributes,
            android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
        )
    }.getOrNull()
    if (player == null) {
        android.util.Log.w("LUNADREAM_SHUTTER", "Bundled shutter WAV could not be opened; using system fallback")
        playSystemShutterFallback()
        return
    }
    player.setOnCompletionListener { it.release() }
    player.setOnErrorListener { mediaPlayer, what, extra ->
        android.util.Log.w("LUNADREAM_SHUTTER", "Shutter WAV playback failed: $what/$extra")
        mediaPlayer.release()
        playSystemShutterFallback()
        true
    }
    runCatching { player.start() }.onFailure {
        android.util.Log.w("LUNADREAM_SHUTTER", "Could not start bundled shutter WAV", it)
        runCatching { player.release() }
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
    onRatio: (Int) -> Unit,
    performanceMode: Boolean = false,
    onOpenWatermarkPanel: () -> Unit = {}
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val localView = LocalView.current
    val screenProfile = rememberScreenProfile()
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val faceDetector = remember {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setMinFaceSize(0.08f)
                .enableTracking()
                .build()
        )
    }
    val faceDetectionBusy = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    DisposableEffect(faceDetector) {
        onDispose { runCatching { faceDetector.close() } }
    }
    val initialLandscapeRotation = remember(localView) {
        val displayRotation = localView.display?.rotation ?: Surface.ROTATION_90
        if (displayRotation == Surface.ROTATION_270) Surface.ROTATION_270 else Surface.ROTATION_90
    }
    var targetRotation by remember { mutableIntStateOf(initialLandscapeRotation) }
    var iconRotationEnabled by remember {
        mutableStateOf(
            context.getSharedPreferences("fujifilm_settings", 0)
                .getBoolean("icon_rotation_enabled", true)
        )
    }
    var iconRotationDegrees by remember { mutableIntStateOf(0) }
    var cameraBindGeneration by remember { mutableIntStateOf(0) }
    // Activity orientation and CameraX capture geometry are both landscape-only.
    // A portrait sensor reading keeps the most recent landscape rotation.
    var isLandscape by remember { mutableStateOf(true) }
    val uiIsLandscape = true
    DisposableEffect(context) {
        val orientationManager = SensorOrientationManager()
        var lastIconQuadrant: Int? = null
        val orientationListener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                val nextRotation = orientationManager.update(orientation)
                if (nextRotation != null && targetRotation != nextRotation) {
                    targetRotation = nextRotation
                }
                val iconQuadrant = snapIconOrientationQuadrant(
                    rawDegrees = orientation,
                    previousQuadrant = lastIconQuadrant
                )
                if (iconQuadrant != null) {
                    lastIconQuadrant = iconQuadrant
                    val relativeRotation = relativeIconRotationDegrees(
                        deviceQuadrantDegrees = iconQuadrant,
                        displayRotation = localView.display?.rotation ?: targetRotation
                    )
                    iconRotationDegrees = relativeRotation
                    FujiIconRotationState.degrees = relativeRotation
                }
            }
        }
        if (orientationListener.canDetectOrientation()) orientationListener.enable()
        onDispose { orientationListener.disable() }
    }
    // Fade only the live viewfinder while its geometry changes; the hardware UI stays landscape.
    val previewOrientationAlpha = remember { Animatable(1f) }
    val ratioUiAlpha = remember { Animatable(1f) }
    var ratioTransitionState by remember { mutableStateOf(RatioUiTransitionState.Idle) }
    val ratioTransitionInProgress = ratioTransitionState != RatioUiTransitionState.Idle

    fun changeAspectRatio(nextIndex: Int) {
        val safeIndex = nextIndex.coerceIn(0, aspectRatios.lastIndex)
        if (safeIndex == ratioIdx || ratioTransitionState != RatioUiTransitionState.Idle) return
        scope.launch {
            try {
                ratioTransitionState = RatioUiTransitionState.FadingOut
                ratioUiAlpha.animateTo(0f, animationSpec = tween(200, easing = FastOutSlowInEasing))
                ratioTransitionState = RatioUiTransitionState.Swapping
                onRatio(safeIndex)
                delay(24L)
                ratioTransitionState = RatioUiTransitionState.FadingIn
                ratioUiAlpha.animateTo(1f, animationSpec = tween(250, easing = FastOutSlowInEasing))
            } finally {
                withContext(NonCancellable) {
                    ratioUiAlpha.snapTo(1f)
                    ratioTransitionState = RatioUiTransitionState.Idle
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(100L)
            if (ratioUiAlpha.value <= 0.001f && ratioTransitionState != RatioUiTransitionState.Swapping) {
                delay(600L)
                if (ratioUiAlpha.value <= 0.001f && ratioTransitionState != RatioUiTransitionState.Swapping) {
                    withContext(NonCancellable) {
                        ratioUiAlpha.snapTo(1f)
                        ratioTransitionState = RatioUiTransitionState.Idle
                    }
                    Log.w("LUNA_LAYOUT", "Ratio UI watchdog restored chrome alpha to 1")
                }
            }
        }
    }
    // Track camera use-case geometry so the shutter waits until the landscape binding is current.
    var boundCameraRotation by remember { mutableIntStateOf(targetRotation) }
    var boundCameraLandscape by remember { mutableStateOf(isLandscape) }
    val focusPeakingEnabled = rememberUpdatedState(focusPeaking)
    val facePreferences = remember { context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE) }
    var faceTrackingMode by remember {
        mutableStateOf(
            facePreferences.getString(
                "face_tracking_mode",
                if (facePreferences.contains("face_tracking_enabled") &&
                    !facePreferences.getBoolean("face_tracking_enabled", true)
                ) "off" else "face_eye"
            )?.takeIf { it in setOf("off", "face", "face_eye") } ?: "face_eye"
        )
    }
    val faceTrackingEnabled = faceTrackingMode != "off"
    val faceTrackingEnabledState = rememberUpdatedState(faceTrackingEnabled)
    val faceTrackingModeState = rememberUpdatedState(faceTrackingMode)
    var hardwareFaceDetectionAvailable by remember { mutableStateOf(false) }
    val hardwareFaceDetectionAvailableState = rememberUpdatedState(hardwareFaceDetectionAvailable)
    val useMlFaceDetectionState = rememberUpdatedState(
        faceTrackingEnabled && shouldUseMlKitFaceDetection(
            faceTrackingMode,
            hardwareFaceDetectionAvailable
        )
    )
    val faceTrackSmoother = remember { FaceTrackSmoother() }
    val primaryEyeSelector = remember { PrimaryEyeSelector() }
    val eyeSwitchRateTracker = remember { EyeSwitchRateTracker() }
    var faceTrackCenter by remember { mutableStateOf<Offset?>(null) }
    var faceTrackSize by remember { mutableStateOf<Offset?>(null) }
    var faceEyeCenter by remember { mutableStateOf<Offset?>(null) }
    var faceLeftEyeCenter by remember { mutableStateOf<Offset?>(null) }
    var faceRightEyeCenter by remember { mutableStateOf<Offset?>(null) }
    var preferredFaceCenter by remember { mutableStateOf<Offset?>(null) }
    val preferredFaceCenterState = rememberUpdatedState(preferredFaceCenter)
    var manualAfOverrideUntilMs by remember { mutableLongStateOf(0L) }
    val manualAfOverrideUntilMsState = rememberUpdatedState(manualAfOverrideUntilMs)
    var centerAfPoint by remember { mutableStateOf(Offset(0.5f, 0.5f)) }
    var centerAfStatus by remember { mutableIntStateOf(0) }
    LaunchedEffect(faceTrackingMode) {
        facePreferences.edit()
            .putString("face_tracking_mode", faceTrackingMode)
            .putBoolean("face_tracking_enabled", faceTrackingMode != "off")
            .apply()
        faceTrackSmoother.reset()
        primaryEyeSelector.reset()
        if (faceTrackingMode == "off") {
            faceTrackCenter = null
            faceTrackSize = null
            faceEyeCenter = null
            faceLeftEyeCenter = null
            faceRightEyeCenter = null
            preferredFaceCenter = null
            centerAfStatus = 0
        }
    }
    val sensorManager = remember(context) {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }
    val levelSensor = remember(sensorManager) {
        sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }
    var levelEnabled by remember {
        mutableStateOf(context.getSharedPreferences("fujifilm_settings", 0)
            .getBoolean("viewfinder_level_enabled", true))
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
    var batteryCharging by remember { mutableStateOf(false) }
    var batteryWarningState by remember { mutableStateOf(BatteryWarningState()) }
    var batteryLowBlinkOn by remember { mutableStateOf(true) }
    LaunchedEffect(batteryWarningState.blinking, batteryWarningState.showLowNotice) {
        if (batteryWarningState.blinking || batteryWarningState.showLowNotice) {
            while (true) {
                batteryLowBlinkOn = true
                delay(500L)
                batteryLowBlinkOn = false
                delay(500L)
            }
        } else {
            batteryLowBlinkOn = true
        }
    }
    DisposableEffect(context) {
        fun updateBattery(intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            batteryCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            if (level >= 0 && scale > 0) {
                batteryPercent = (level * 100f / scale).roundToInt().coerceIn(0, 100)
                batteryWarningState = updateBatteryWarningState(
                    previous = batteryWarningState,
                    batteryPercent = batteryPercent,
                    charging = batteryCharging
                )
            } else if (batteryCharging) {
                batteryWarningState = BatteryWarningState()
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

    val appPrefs = remember { context.getSharedPreferences("fujifilm_settings", 0) }
    var gridOverlayMode by remember {
        mutableStateOf(appPrefs.getString("grid_overlay_mode", if (showGrid) "3x3" else "off") ?: "off")
    }
    var gridOverlayColor by remember {
        mutableStateOf(appPrefs.getString("grid_overlay_color", "auto") ?: "auto")
    }
    var gridOverlayOpacity by remember {
        mutableIntStateOf(appPrefs.getInt("grid_overlay_opacity", 50).coerceIn(30, 70))
    }
    DisposableEffect(appPrefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { preferences, key ->
            when (key) {
                "grid_overlay_mode" -> gridOverlayMode = preferences?.getString("grid_overlay_mode", "off") ?: "off"
                "grid_overlay_color" -> gridOverlayColor = preferences?.getString("grid_overlay_color", "auto") ?: "auto"
                "grid_overlay_opacity" -> gridOverlayOpacity = (preferences?.getInt("grid_overlay_opacity", 50) ?: 50).coerceIn(30, 70)
            }
        }
        appPrefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { appPrefs.unregisterOnSharedPreferenceChangeListener(listener) }
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
    // Chrome remains visible until the user explicitly presses DISP/BACK.
    var lcdDisplayMode by remember { mutableStateOf(CameraLcdDisplayMode.STANDARD) }
    val hudVisible = lcdDisplayMode != CameraLcdDisplayMode.CLEAN
    val dispReducedMotion = remember(context) { AnimaT.isSystemReducedMotion(context) }
    val animatedIconRotationDegrees by animateFloatAsState(
        targetValue = if (iconRotationEnabled) iconRotationDegrees.toFloat() else 0f,
        animationSpec = tween(
            durationMillis = if (dispReducedMotion) 0 else 250,
            easing = FastOutSlowInEasing
        ),
        label = "viewfinder icon orientation"
    )
    val dispOsdAlpha = remember { Animatable(1f) }
    var dispTransitionInProgress by remember { mutableStateOf(false) }

    fun cycleDispDisplayMode() {
        if (dispTransitionInProgress) return
        if (dispReducedMotion || performanceMode) {
            lcdDisplayMode = lcdDisplayMode.next()
            return
        }
        scope.launch {
            dispTransitionInProgress = true
            try {
                dispOsdAlpha.animateTo(0f, tween(75, easing = FastOutSlowInEasing))
                lcdDisplayMode = lcdDisplayMode.next()
                dispOsdAlpha.animateTo(1f, tween(135, easing = FastOutSlowInEasing))
            } finally {
                withContext(NonCancellable) {
                    dispOsdAlpha.snapTo(1f)
                    dispTransitionInProgress = false
                }
            }
        }
    }
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
    val previewFpsEstimator = remember { PreviewFpsEstimator() }
    var measuredPreviewFps by remember { mutableFloatStateOf(Float.NaN) }
    LaunchedEffect(Unit) {
        previewFpsEstimator.reset()
        measuredPreviewFps = Float.NaN
    }
    var previewFpsMode by remember {
        mutableStateOf(
            if (appPrefs.getBoolean("preview_fps_keep", false)) {
                appPrefs.getString("preview_fps_mode", "60")
                    ?.takeIf { it in setOf("60", "30", "24", "AUTO") } ?: "60"
            } else "60"
        )
    }
    var previewFpsTarget by remember { mutableFloatStateOf(60f) }
    LaunchedEffect(Unit) {
        if (!appPrefs.getBoolean("preview_fps_keep", false)) {
            appPrefs.edit().putString("preview_fps_mode", "60").apply()
            previewFpsMode = "60"
        }
    }
    var actualZoomRatio by remember { mutableFloatStateOf(zoomSel) }
    var zoomFromCaptureResult by remember { mutableFloatStateOf(Float.NaN) }
    var digitalZoomCropScale by remember { mutableFloatStateOf(1f) }
    val digitalZoomCropScaleState = rememberUpdatedState(digitalZoomCropScale)
    val zoomDisplayValue by animateFloatAsState(
        targetValue = actualZoomRatio,
        animationSpec = tween(durationMillis = if (performanceMode) 90 else 120, easing = FastOutSlowInEasing),
        label = "zoom OSD interpolation"
    )
    var zoomButtonsSwapped by remember {
        mutableStateOf(appPrefs.getString("zoom_buttons_mode", "standard") == "swapped")
    }
    var zoomBarPosition by remember {
        mutableStateOf(appPrefs.getString("zoom_bar_position", "top_center")?.let { if (it == "top_left") "middle_left" else it } ?: "top_center")
    }
    var previewBoundsText by remember { mutableStateOf("unmeasured") }
    val previewBoundsTextState = rememberUpdatedState(previewBoundsText)
    var previewTransformText by remember { mutableStateOf("unmeasured") }
    val previewTransformTextState = rememberUpdatedState(previewTransformText)
    DisposableEffect(appPrefs) {
        val preferenceListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { preferences, key ->
            when (key) {
                "zoom_buttons_mode" -> zoomButtonsSwapped = preferences?.getString(key, "standard") == "swapped"
                "zoom_bar_position" -> zoomBarPosition = preferences?.getString(key, "top_center")?.let { if (it == "top_left") "middle_left" else it } ?: "top_center"
                "preview_fps_mode" -> previewFpsMode = preferences?.getString(key, "60")
                    ?.takeIf { it in setOf("60", "30", "24", "AUTO") } ?: "60"
                "icon_rotation_enabled" -> {
                    iconRotationEnabled = preferences?.getBoolean(key, true) ?: true
                    FujiIconRotationState.enabled = iconRotationEnabled
                    if (!iconRotationEnabled) iconRotationDegrees = 0
                }
            }
        }
        appPrefs.registerOnSharedPreferenceChangeListener(preferenceListener)
        onDispose { appPrefs.unregisterOnSharedPreferenceChangeListener(preferenceListener) }
    }
    var zoomOverlayVisible by remember { mutableStateOf(false) }
    var zoomInteractionGeneration by remember { mutableIntStateOf(0) }
    val zoomOverlayAlpha by animateFloatAsState(
        targetValue = if (zoomOverlayVisible) 1f else 0f,
        animationSpec = tween(durationMillis = if (zoomOverlayVisible) 120 else 250),
        label = "zoom OSD fade"
    )
    LaunchedEffect(zoomInteractionGeneration) {
        val observedGeneration = zoomInteractionGeneration
        if (observedGeneration == 0) return@LaunchedEffect
        if (!zoomOverlayVisible) {
            android.util.Log.d("FUJI_ZOOM_OSD", "SHOW reason=W_T generation=$observedGeneration")
        }
        zoomOverlayVisible = true
        delay(1_200L)
        if (zoomInteractionGeneration == observedGeneration) {
            zoomOverlayVisible = false
            android.util.Log.d("FUJI_ZOOM_OSD", "HIDE idleMs=1200 generation=$observedGeneration")
        }
    }
    var zoomHoldDirection by remember { mutableIntStateOf(0) }
    var zoomAnimationJob by remember { mutableStateOf<Job?>(null) }
    var lensTransitionVisible by remember { mutableStateOf(false) }
    var lensTransitionLabel by remember { mutableStateOf("") }
    var metering by remember { mutableIntStateOf(appPrefs.getInt("camera_metering_index", 0).coerceIn(0, 3)) }
    // Ultra HDR is hidden from the UI. Keep the legacy camera path but start disabled.
    var ultraHdr by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appPrefs.edit().putBoolean("ultra_hdr_enabled", false).apply() }
    LaunchedEffect(Unit) { appPrefs.edit().remove("camera_stabilization_enabled").apply() }
    var renderEngine by remember { mutableStateOf(appPrefs.getString("camera_render_engine", "Performance") ?: "Performance") }
    var proSel by remember { mutableStateOf(appPrefs.getString("pro_selected_control", null)?.takeUnless { it == "AUTO" }) }
    var proAfMode by remember {
        mutableStateOf(
            appPrefs.getString("pro_af_mode", "AF-S")?.takeIf { it in setOf("AF-S", "AF-C", "MF") } ?: "AF-S"
        )
    }
    val proAfModeState = rememberUpdatedState(proAfMode)
    var touchAfEnabled by remember { mutableStateOf(appPrefs.getBoolean("pro_touch_af_enabled", false)) }
    LaunchedEffect(touchAfEnabled) { appPrefs.edit().putBoolean("pro_touch_af_enabled", touchAfEnabled).apply() }
    BackHandler(enabled = proSel != null && mode == "Pro") {
        android.util.Log.i("FUJI_PRO_SCALE", "BACK source=system selected=$proSel")
        proSel = null
    }
    var proAdjustmentContent: (@Composable (Modifier) -> Unit)? = null
    var focusLocked by remember { mutableStateOf(false) }
    var focusLockRequestGeneration by remember { mutableIntStateOf(0) }
    fun invalidatePendingFocusLock(activeCamera: Camera? = null) {
        focusLockRequestGeneration++
        focusLocked = false
        activeCamera?.let { cameraToUnlock ->
            runCatching { applyCameraAeLock(cameraToUnlock, locked = false) }
        }
    }
    var manualIsoEnabled by remember { mutableStateOf(appPrefs.getBoolean("pro_manual_iso_enabled", false)) }
    var manualShutterEnabled by remember { mutableStateOf(appPrefs.getBoolean("pro_manual_shutter_enabled", false)) }
    var manualColorTempEnabled by remember { mutableStateOf(appPrefs.getBoolean("pro_manual_wb_enabled", false)) }
    var manualColorTemp by remember { mutableIntStateOf(appPrefs.getInt("pro_manual_wb_kelvin", 5200).coerceIn(2000, 10000)) }
    var shutterButtonBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var filterDialogVisible by remember { mutableStateOf(false) }
    var activeFilter by remember { mutableStateOf(context.getSharedPreferences("fujifilm_settings", 0).getString("selected_filter", "None") ?: "None") }
    var filterPreviewSourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val fallbackFilterPreviewBitmap = remember(context) { createFilmLookPreviewSample(context) }
    val shutterEffectProgress = remember { Animatable(0f) }
    var frozenPreviewBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var previewFreezeEffect by remember { mutableStateOf("off") }
    var previewFreezeVisible by remember { mutableStateOf(false) }
    val previewFreezeAlpha = remember { Animatable(0f) }
    val previewEffectPosition = remember { Animatable(-0.35f) }
    val lunaActivationProgress = remember { Animatable(1f) }
    var previewEffectBloom by remember { mutableStateOf(false) }
    val previewFreezeBlockInteraction = remember { MutableInteractionSource() }
    var isProcessing by remember { mutableStateOf(false) }
    var livePhotoCaptureInProgress by remember { mutableStateOf(false) }
    var processingProgress by remember { mutableIntStateOf(0) }
    var noiseReductionProgress by remember { mutableIntStateOf(0) }
    var processingLabel by remember { mutableStateOf("") }
    var processingEtaSeconds by remember { mutableIntStateOf(-1) }
    var multiFrameHoldActive by remember { mutableStateOf(false) }
    var multiFrameRenderStarted by remember { mutableStateOf(false) }
    var multiFrameCapturedFrames by remember { mutableIntStateOf(0) }
    var multiFrameTargetFrames by remember { mutableIntStateOf(5) }
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
    LaunchedEffect(lunaDreamMode, dispReducedMotion) {
        if (lunaDreamMode == "off") {
            lunaActivationProgress.snapTo(1f)
        } else if (dispReducedMotion) {
            lunaActivationProgress.snapTo(1f)
        } else {
            lunaActivationProgress.snapTo(0f)
            lunaActivationProgress.animateTo(
                1f,
                animationSpec = tween(durationMillis = 600, easing = LinearEasing)
            )
        }
    }
    LaunchedEffect(previewFreezeEffect, isProcessing) {
        val activeEffect = previewFreezeEffect
        if (activeEffect == "off") {
            previewFreezeVisible = false
            previewEffectBloom = false
            previewFreezeAlpha.snapTo(0f)
            return@LaunchedEffect
        }
        if (!isProcessing) {
            previewEffectBloom = true
            previewFreezeAlpha.animateTo(
                1f,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
            delay(300L)
            previewEffectBloom = false
            previewFreezeAlpha.animateTo(
                0f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
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
        }
    }
    // EV is a command value. Do not overwrite it with delayed per-frame Camera2 results.
    var evIndex by remember { mutableIntStateOf(appPrefs.getInt("pro_ev_index", 0)) }
    val evDisplayIndex by animateFloatAsState(
        targetValue = evIndex.toFloat(),
        animationSpec = spring(dampingRatio = 0.82f, stiffness = if (performanceMode) 720f else 520f),
        label = "EV OSD pointer interpolation"
    )
    var evHoldDirection by remember { mutableIntStateOf(0) }
    var iso by remember { mutableIntStateOf(0) }
    var shutterNs by remember { mutableLongStateOf(0L) }
    var apertureValue by remember { mutableFloatStateOf(0f) }
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
    var thumbUri by remember { mutableStateOf<Uri?>(null) }
    var viewerBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var viewerOriginalBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var viewerShowsOriginal by remember { mutableStateOf(false) }
    var photoViewerVisible by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCaptureUseCase by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var captureSetupReady by remember { mutableStateOf(false) }

    LaunchedEffect(faceTrackingMode, camera, activeCameraDetails) {
        val active = camera
        if (faceTrackingMode == "off") {
            faceTrackCenter = null
            faceTrackSize = null
            faceEyeCenter = null
            hardwareFaceDetectionAvailable = false
            active?.let { bound ->
                runCatching {
                    Camera2CameraControl.from(bound.cameraControl).addCaptureRequestOptions(
                        CaptureRequestOptions.Builder()
                            .setCaptureRequestOption(
                                CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                                CaptureRequest.STATISTICS_FACE_DETECT_MODE_OFF
                            )
                            .build()
                    )
                }
            }
            return@LaunchedEffect
        }
        if (active == null) return@LaunchedEffect

        // Camera2 Face returns eye centres only; it has no per-eye contour area.
        // FACE+EYE therefore uses ML Kit contours so the 25%-larger/500ms switch
        // rule works consistently on every device, including Camera2 FULL devices.
        if (faceTrackingMode == "face_eye") {
            hardwareFaceDetectionAvailable = false
            runCatching {
                Camera2CameraControl.from(active.cameraControl).addCaptureRequestOptions(
                    CaptureRequestOptions.Builder()
                        .setCaptureRequestOption(
                            CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                            CaptureRequest.STATISTICS_FACE_DETECT_MODE_OFF
                        )
                        .build()
                )
            }.onFailure {
                android.util.Log.w("FUJI_FACE_TRACK", "Could not disable Camera2 face detection for contour-based FACE+EYE.", it)
            }
            android.util.Log.i(
                "FUJI_FACE_TRACK",
                "FACE+EYE uses ML Kit 640x480 contours for per-eye area hysteresis (Camera2 eye centres have no contour size)."
            )
            return@LaunchedEffect
        }

        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = activeCameraDetails?.id
            ?: runCatching { Camera2CameraInfo.from(active.cameraInfo).cameraId }.getOrNull()
        if (cameraId.isNullOrBlank()) return@LaunchedEffect
        val availableModes = runCatching {
            manager.getCameraCharacteristics(cameraId)
                .get(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES)
                ?: intArrayOf()
        }.getOrDefault(intArrayOf())
        val selectedMode = when {
            availableModes.contains(CaptureRequest.STATISTICS_FACE_DETECT_MODE_FULL) ->
                CaptureRequest.STATISTICS_FACE_DETECT_MODE_FULL
            availableModes.contains(CaptureRequest.STATISTICS_FACE_DETECT_MODE_SIMPLE) ->
                CaptureRequest.STATISTICS_FACE_DETECT_MODE_SIMPLE
            else -> null
        }
        hardwareFaceDetectionAvailable = selectedMode != null
        if (selectedMode == null) {
            android.util.Log.i("FUJI_FACE_TRACK", "No Camera2 FULL/SIMPLE face mode; selecting ML Kit fallback.")
            return@LaunchedEffect
        }
        runCatching {
            Camera2CameraControl.from(active.cameraControl).addCaptureRequestOptions(
                CaptureRequestOptions.Builder()
                    .setCaptureRequestOption(CaptureRequest.STATISTICS_FACE_DETECT_MODE, selectedMode)
                    .build()
            )
        }.onFailure {
            hardwareFaceDetectionAvailable = false
            android.util.Log.w("FUJI_FACE_TRACK", "Could not enable Camera2 face detection; selecting ML Kit fallback.", it)
        }
    }

    // The camera screen and capture output remain landscape. Portrait sensor readings are ignored.
    LaunchedEffect(targetRotation) {
        val nextLandscape = targetRotation == Surface.ROTATION_90 ||
            targetRotation == Surface.ROTATION_270
        if (nextLandscape == isLandscape) return@LaunchedEffect

        val previousBindGeneration = cameraBindGeneration
        previewOrientationAlpha.animateTo(
            0f,
            animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)
        )
        isLandscape = nextLandscape

        val waitStartedAt = SystemClock.elapsedRealtime()
        while (SystemClock.elapsedRealtime() - waitStartedAt < 2_800L) {
            val newBindingStarted = cameraBindGeneration > previousBindGeneration
            if (newBindingStarted && captureSetupReady && camera != null &&
                boundCameraLandscape == nextLandscape
            ) {
                break
            }
            delay(24L)
        }

        previewOrientationAlpha.animateTo(
            1f,
            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
        )
    }

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
    LaunchedEffect(mode, ratioIdx, showQuick, filterDialogVisible, showLunaDream) {
        val activePanel = when {
            showQuick -> "Q_MENU"
            filterDialogVisible -> "FILM_FILTERS"
            showLunaDream -> "LUNADREAM"
            else -> "NONE"
        }
        CrashContextDiagnostics.updateCameraState(
            mode = mode,
            panel = activePanel,
            aspectRatio = "ratio_index=$ratioIdx",
            orientation = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) "LANDSCAPE" else "PORTRAIT",
            language = AppLanguageManager.currentLanguage,
            uiStyle = if (appPrefs.getBoolean("simple_ui_mode_enabled", false)) "SIMPLE" else "LCD"
        )
    }
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
            evIndex = 0
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

    LaunchedEffect(zoomHoldDirection, camera, activeCameraDetails, activePhysicalCameraDetails, camera2Inventory) {
        val direction = zoomHoldDirection
        val active = camera
        if (direction == 0 || active == null) return@LaunchedEffect
        // A short press is handled once by FujiKey.onClick; don't start the
        // continuous camera command loop until the shared hold threshold elapses.
        delay(FUJI_KEY_HOLD_THRESHOLD_MS)
        if (zoomHoldDirection != direction) return@LaunchedEffect
        var lastZoomFuture: com.google.common.util.concurrent.ListenableFuture<*>? = null
        var targetZoom = zoomSel
        var previousFrame = 0L
        val holdStartedAtMs = SystemClock.elapsedRealtime()
        var zoomFrameIndex = 0L
        var lastDiagnosticAt = SystemClock.uptimeMillis()
        var tickCount = 0
        android.util.Log.d("FUJI_ZOOM", "PRESS_START direction=$direction zoom=$targetZoom")
        while (true) {
            val frame = withFrameNanos { it }
            val dt = if (previousFrame == 0L) 0f else
                ((frame - previousFrame) / 1_000_000_000f).coerceIn(0f, 0.05f)
            previousFrame = frame
            if (dt <= 0f) continue
            zoomFrameIndex++
            val state = active.cameraInfo.zoomState.value ?: continue
            val minimum = state.minZoomRatio
            val cameraReportedMaximum = Camera2Info.cameraReportedZoomMax(
                activeCameraDetails,
                state.maxZoomRatio
            )
            val zoomAssessment = Camera2Info.opticalZoomAssessment(activeCameraDetails, activePhysicalCameraDetails, camera2Inventory)
            val maximum = maxTotalSelectableZoom(cameraReportedMaximum, zoomAssessment)
            val traversalSeconds = if (maximum <= 2.001f) 1.2f else 6.5f
            val next = advanceLogarithmicZoomRatio(
                currentZoom = targetZoom,
                direction = direction,
                minimumRatio = minimum,
                maximumRatio = maximum,
                deltaSeconds = dt,
                traversalSeconds = traversalSeconds
            )
            if (kotlin.math.abs(next - targetZoom) > 0.0005f) {
                if (appPrefs.getBoolean("haptic_feedback_enabled", true)) {
                    zoomHapticMilestonesCrossed(targetZoom, next).forEach { milestone ->
                        localView.performHapticFeedback(
                            if (milestone == 1f) HapticFeedbackConstants.CONTEXT_CLICK
                            else HapticFeedbackConstants.CLOCK_TICK
                        )
                    }
                }
                targetZoom = next
                val plan = resolveZoomExecutionPlan(
                    requestedZoomRatio = next,
                    minimumCameraRatio = minimum,
                    cameraReportedMaximum = cameraReportedMaximum,
                    assessment = zoomAssessment
                )
                zoomSel = plan.effectiveZoomRatio
                actualZoomRatio = plan.effectiveZoomRatio
                digitalZoomCropScale = plan.digitalCropRatio
                zoomInteractionGeneration++
                if (abs(state.zoomRatio - plan.cameraControlZoomRatio) > 0.002f) {
                    lastZoomFuture = active.cameraControl.setZoomRatio(plan.cameraControlZoomRatio)
                }
                tickCount++
            }
            if (SystemClock.elapsedRealtime() - holdStartedAtMs <= 3_000L) {
                val capturedZoom = zoomFromCaptureResult.takeIf { it.isFinite() }?.toString() ?: "NO_CAPTURE_RESULT"
                android.util.Log.d(
                    "FUJI_ZOOM_FRAME",
                    "frame=$zoomFrameIndex direction=${if (direction > 0) "IN" else "OUT"} dt=$dt " +
                        "zoomTarget=$targetZoom zoomFromCaptureResult=$capturedZoom " +
                        "activePhysicalCameraId=$activePhysicalCameraId " +
                        "previewTransform=${previewTransformTextState.value} " +
                        "previewRect=${previewBoundsTextState.value}"
                )
            }
            val now = SystemClock.uptimeMillis()
            if (now - lastDiagnosticAt >= 1_000L) {
                val commandState = when {
                    lastZoomFuture == null -> "NO_COMMAND"
                    !lastZoomFuture!!.isDone -> "PENDING"
                    else -> runCatching {
                        lastZoomFuture!!.get()
                        "COMPLETED"
                    }.getOrElse { "FAILED:${it.cause?.javaClass?.simpleName ?: it.javaClass.simpleName}" }
                }
                android.util.Log.d(
                    "FUJI_ZOOM",
                    "HOLD_TICK direction=${if (direction > 0) "IN" else "OUT"} ticks=$tickCount zoomTarget=$targetZoom cameraZoom=${state.zoomRatio} command=$commandState"
                )
                lastDiagnosticAt = now
                tickCount = 0
            }
        }
    }
    LaunchedEffect(evHoldDirection, camera, mode, proSel) {
        val direction = evHoldDirection
        val active = camera
        val range = active?.cameraInfo?.exposureState?.exposureCompensationRange
        if (direction == 0 || active == null || range == null) return@LaunchedEffect
        delay(FUJI_KEY_HOLD_THRESHOLD_MS)
        if (evHoldDirection != direction) return@LaunchedEffect
        var lastEvFuture: com.google.common.util.concurrent.ListenableFuture<*>? = null
        var targetIndex = evIndex.coerceIn(range.lower, range.upper)
        // A hold crossing the key threshold gets one immediate EV step; a short tap
        // is handled only by FujiKey.onClick and never enters this repeating loop.
        val firstHeldIndex = (targetIndex + direction).coerceIn(range.lower, range.upper)
        if (firstHeldIndex != targetIndex) {
            targetIndex = firstHeldIndex
            evIndex = targetIndex
            manualExposure = false
            lastEvFuture = active.cameraControl.setExposureCompensationIndex(targetIndex)
        }
        var previousFrame = 0L
        var heldSeconds = 0f
        var accumulator = 0f
        var lastDiagnosticAt = SystemClock.uptimeMillis()
        var tickCount = 0
        android.util.Log.d("FUJI_EV", "PRESS_START direction=$direction index=$targetIndex")
        while (true) {
            val frame = withFrameNanos { it }
            val dt = if (previousFrame == 0L) 0f else
                ((frame - previousFrame) / 1_000_000_000f).coerceIn(0f, 0.05f)
            previousFrame = frame
            if (dt <= 0f) continue
            heldSeconds += dt
            accumulator += dt
            val interval = (0.24f - heldSeconds * 0.055f).coerceAtLeast(0.08f)
            if (accumulator >= interval) {
                accumulator -= interval
                val next = (targetIndex + direction).coerceIn(range.lower, range.upper)
                if (next == targetIndex) break
                targetIndex = next
                evIndex = next
                manualExposure = false
                lastEvFuture = active.cameraControl.setExposureCompensationIndex(next)
                tickCount++
            }
            val now = SystemClock.uptimeMillis()
            if (now - lastDiagnosticAt >= 1_000L) {
                val commandState = when {
                    lastEvFuture == null -> "NO_COMMAND"
                    !lastEvFuture!!.isDone -> "PENDING"
                    else -> runCatching {
                        lastEvFuture!!.get()
                        "COMPLETED"
                    }.getOrElse { "FAILED:${it.cause?.javaClass?.simpleName ?: it.javaClass.simpleName}" }
                }
                android.util.Log.d(
                    "FUJI_EV",
                    "HOLD_TICK direction=$direction ticks=$tickCount index=$targetIndex cameraIndex=${active.cameraInfo.exposureState.exposureCompensationIndex} command=$commandState"
                )
                lastDiagnosticAt = now
                tickCount = 0
            }
        }
    }

    val shutterButtonImageUri = appPrefs.getString("shutter_button_image_uri", null)
    LaunchedEffect(shutterButtonImageUri) {
        shutterButtonBitmap = if (shutterButtonImageUri.isNullOrBlank()) null else withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.loadThumbnail(Uri.parse(shutterButtonImageUri), Size(180, 180), null).asImageBitmap()
            }.getOrNull()
        }
    }

    // Fill the selected viewfinder ratio without stretching or changing the camera zoom.
    // PreviewView crops only the displayed preview bounds when the selected ratio differs
    // from the sensor stream, so 16:9, 1:1 and 21:9 do not remain visibly 4:3.
    val previewView = remember(context, renderEngine) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FIT_CENTER
            setBackgroundColor(android.graphics.Color.BLACK)
            // COMPATIBLE uses a TextureView-backed preview so PreviewView.bitmap can
            // preserve the exact last displayed frame during computational capture.
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val digitalZoomPreviewEffect = remember(previewView) { DigitalZoomPreviewEffect() }
    DisposableEffect(previewView, digitalZoomPreviewEffect) {
        onDispose { digitalZoomPreviewEffect.clear(previewView) }
    }
    LaunchedEffect(previewView, digitalZoomCropScale, digitalZoomPreviewEffect) {
        val safeDigitalScale = digitalZoomCropScale.takeIf { it.isFinite() && it >= 1f } ?: 1f
        previewView.clipToOutline = true
        previewView.outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
        val renderMethod = digitalZoomPreviewEffect.apply(previewView, safeDigitalScale)
        val matrixValues = FloatArray(9)
        previewView.matrix.getValues(matrixValues)
        previewTransformText = String.format(
            Locale.US,
            "render=%s view=%dx%d scale=(%.4f,%.4f) translation=(%.2f,%.2f) matrix=[%.4f,%.4f,%.4f;%.4f,%.4f,%.4f;%.4f,%.4f,%.4f] digitalCrop=%.4f",
            renderMethod,
            previewView.width,
            previewView.height,
            previewView.scaleX,
            previewView.scaleY,
            previewView.translationX,
            previewView.translationY,
            matrixValues[0], matrixValues[1], matrixValues[2],
            matrixValues[3], matrixValues[4], matrixValues[5],
            matrixValues[6], matrixValues[7], matrixValues[8],
            safeDigitalScale
        )
        android.util.Log.d("FUJI_ZOOM_PREVIEW_TRANSFORM", previewTransformText)
    }
    var previewStreamState by remember(previewView) {
        mutableStateOf(
            previewView.previewStreamState.value ?: PreviewView.StreamState.IDLE
        )
    }
    var previewGapStartedAtMs by remember(previewView) { mutableLongStateOf(0L) }
    val frozenPreviewAlpha by animateFloatAsState(
        targetValue = if (
            previewFreezeEffect == "off" &&
            !isProcessing &&
            frozenPreviewBitmap != null &&
            previewStreamState != PreviewView.StreamState.STREAMING
        ) 1f else 0f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "frozen preview crossfade"
    )
    val previewBindGenerationState = rememberUpdatedState(cameraBindGeneration)
    LaunchedEffect(previewStreamState, frozenPreviewBitmap, previewFreezeEffect, isProcessing) {
        if (
            previewFreezeEffect == "off" &&
            !isProcessing &&
            previewStreamState == PreviewView.StreamState.STREAMING &&
            frozenPreviewBitmap != null
        ) {
            delay(150L)
            if (
                previewFreezeEffect == "off" &&
                !isProcessing &&
                previewStreamState == PreviewView.StreamState.STREAMING
            ) {
                frozenPreviewBitmap = null
            }
        }
    }
    DisposableEffect(previewView, lifecycleOwner) {
        val observer = Observer<PreviewView.StreamState> { state ->
            val nextState = state ?: PreviewView.StreamState.IDLE
            val previousState = previewStreamState
            val nowMs = SystemClock.elapsedRealtime()
            if (previousState == PreviewView.StreamState.STREAMING &&
                nextState != PreviewView.StreamState.STREAMING
            ) {
                previewGapStartedAtMs = nowMs
                android.util.Log.w(
                    "FUJI_PREVIEW_GAP",
                    "START from=$previousState to=$nextState bindGeneration=${previewBindGenerationState.value} cameraId=${activeCameraDetails?.id ?: "unknown"}"
                )
            } else if (previousState != PreviewView.StreamState.STREAMING &&
                nextState == PreviewView.StreamState.STREAMING
            ) {
                val gapMs = if (previewGapStartedAtMs > 0L) nowMs - previewGapStartedAtMs else -1L
                android.util.Log.i(
                    "FUJI_PREVIEW_GAP",
                    "END durationMs=$gapMs previous=$previousState bindGeneration=${previewBindGenerationState.value} cameraId=${activeCameraDetails?.id ?: "unknown"}"
                )
                previewGapStartedAtMs = 0L
            }
            previewStreamState = nextState
        }
        previewView.previewStreamState.observe(lifecycleOwner, observer)
        onDispose {
            val nowMs = SystemClock.elapsedRealtime()
            if (previewGapStartedAtMs > 0L) {
                android.util.Log.w(
                    "FUJI_PREVIEW_GAP",
                    "DISPOSE while preview unavailable durationMs=${nowMs - previewGapStartedAtMs} bindGeneration=${previewBindGenerationState.value}"
                )
                previewGapStartedAtMs = 0L
            }
            previewView.previewStreamState.removeObserver(observer)
            previewStreamState = PreviewView.StreamState.IDLE
        }
    }
    LaunchedEffect(previewView, previewStreamState, hasCam, cameraBindGeneration) {
        if (hasCam && previewStreamState != PreviewView.StreamState.STREAMING) {
            delay(1_500L)
            if (previewView.previewStreamState.value != PreviewView.StreamState.STREAMING) {
                android.util.Log.w(
                    "FUJI_PREVIEW_GAP",
                    "WATCHDOG no STREAMING frame after 1500ms state=${previewView.previewStreamState.value} bindGeneration=$cameraBindGeneration cameraId=${activeCameraDetails?.id ?: "unknown"}"
                )
            }
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
        nativeImageProcessingEnabled, livePhotoEnabled, uhdCaptureEnabled, fhdCaptureEnabled, uhdPlusCaptureEnabled, hdrEnabled, nightShotMode,
        starTrailExposureSeconds, mode, burstMode, hdrBracketMode, focusBracketMode,
        previewFpsMode
    ) {
        if (!hasCam) return@LaunchedEffect
        if (!isProcessing && previewFreezeEffect == "off" &&
            previewStreamState == PreviewView.StreamState.STREAMING &&
            previewView.width > 0 && previewView.height > 0
        ) {
            runCatching { previewView.bitmap?.asImageBitmap() }
                .getOrNull()
                ?.let { frozenPreviewBitmap = it }
            android.util.Log.d(
                "FUJI_PREVIEW_GAP",
                "FROZEN_FRAME captured=${frozenPreviewBitmap != null} view=${previewView.width}x${previewView.height} bindGeneration=$cameraBindGeneration"
            )
        }
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
        android.util.Log.i(
            "FUJI_CAMERA_REBIND",
            "START generation=$bindGeneration facing=${if (front) "FRONT" else "BACK"} " +
                "mode=$mode ratioIndex=$ratioIdx rotation=$targetRotation renderEngine=$renderEngine " +
                "livePhoto=$livePhotoEnabled nightShot=$nightShotMode burst=$burstMode"
        )
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
            // Match CameraX Preview/ImageCapture crop geometry to the selected frame ratio.
            // aspectRatioForOrientation returns image height / width for the current grip.
            val selectedHeightOverWidth = aspectRatioForOrientation(ratioIdx, isLandscape)
            val ratio = Rational(
                1000,
                (selectedHeightOverWidth * 1000f).roundToInt().coerceAtLeast(1)
            )
            // Updated after CameraX binds so live capture results use the actual logical camera,
            // not the first camera ID returned by CameraManager.
            var activeCameraCharacteristics: CameraCharacteristics? = null
            var faceSensorToDisplayRotation = 0

            fun bindCapture(
                outputFormat: Int,
                livePhotoResolutionPixels: Long = 48_000_000L
            ) {
                var lastFaceTrackAtMs = 0L
                var lastFaceAfAtMs = 0L
                var lastFaceAfTarget: Offset? = null

                fun publishDetectedFace(
                    center: Offset,
                    size: Offset,
                    eyeCenter: Offset?,
                    leftEyeCenter: Offset?,
                    rightEyeCenter: Offset?,
                    now: Long,
                    trackingId: Int? = null
                ) {
                    // Tracking continuity and face selection remain in camera-normalized space.
                    // Only the overlay is transformed to match the additional digital crop.
                    val digitalCropScale = digitalZoomCropScaleState.value
                    val smoothed = faceTrackSmoother.onFaceDetected(
                        center, size, eyeCenter, trackingId, now
                    ) ?: return
                    val displayedCenter = cameraPointToDigitalPreview(smoothed.center, digitalCropScale)
                    val displayedSize = cameraBoxSizeToDigitalPreview(smoothed.size, digitalCropScale)
                    val displayedEye = smoothed.eyeCenter?.let {
                        cameraPointToDigitalPreview(it, digitalCropScale)
                    }
                    val displayedLeftEye = leftEyeCenter?.let {
                        cameraPointToDigitalPreview(it, digitalCropScale)
                    }
                    val displayedRightEye = rightEyeCenter?.let {
                        cameraPointToDigitalPreview(it, digitalCropScale)
                    }
                    val uiCenterMoved = now - lastFaceTrackAtMs >= 40L
                    if (uiCenterMoved) {
                        lastFaceTrackAtMs = now
                        scope.launch(Dispatchers.Main) {
                            faceTrackCenter = displayedCenter
                            faceTrackSize = displayedSize
                            faceEyeCenter = if (faceTrackingModeState.value == "face_eye") displayedEye else null
                            faceLeftEyeCenter = if (faceTrackingModeState.value == "face_eye") displayedLeftEye else null
                            faceRightEyeCenter = if (faceTrackingModeState.value == "face_eye") displayedRightEye else null
                        }
                    }
                    val focusTargetCamera = if (faceTrackingModeState.value == "face_eye") {
                        smoothed.eyeCenter ?: smoothed.center
                    } else smoothed.center
                    val focusTargetDisplay = cameraPointToDigitalPreview(
                        focusTargetCamera,
                        digitalCropScale
                    )
                    if (now < manualAfOverrideUntilMsState.value) {
                        lastFaceAfTarget = null
                        return
                    }
                    val previousTarget = lastFaceAfTarget
                    val movedEnough = previousTarget == null ||
                        kotlin.math.abs(previousTarget.x - focusTargetCamera.x) +
                        kotlin.math.abs(previousTarget.y - focusTargetCamera.y) > 0.025f
                    if (movedEnough && now - lastFaceAfAtMs >= 200L &&
                        !focusLockedState.value && manualFocusState.value <= 0f && proAfModeState.value != "MF"
                    ) {
                        lastFaceAfAtMs = now
                        lastFaceAfTarget = focusTargetCamera
                        scope.launch(Dispatchers.Main) {
                            val previewWidth = previewView.width
                            val previewHeight = previewView.height
                            if (previewWidth > 0 && previewHeight > 0) {
                                centerAfStatus = 0
                                runCatching {
                                    val afPointSize = CameraAfAreaSize.fromPreference(
                                        appPrefs.getString("af_area_size", "m")
                                    ).focusPointSize
                                    val point = previewView.meteringPointFactory.createPoint(
                                        focusTargetCamera.x * previewWidth,
                                        focusTargetCamera.y * previewHeight,
                                        afPointSize
                                    )
                                    android.util.Log.d(
                                        "FUJI_AF_TRACE",
                                        "faceTargetDisplay=${focusTargetDisplay.x},${focusTargetDisplay.y} " +
                                            "cameraTarget=${focusTargetCamera.x},${focusTargetCamera.y} " +
                                            "digitalCrop=$digitalCropScale"
                                    )
                                    val action = FocusMeteringAction.Builder(
                                        point,
                                        FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                                    ).disableAutoCancel().build()
                                    val future = camera?.cameraControl?.startFocusAndMetering(action)
                                    future?.addListener({
                                        val success = runCatching { future.get().isFocusSuccessful }.getOrDefault(false)
                                        centerAfStatus = if (success) 1 else -1
                                        if (!success) {
                                            scope.launch(Dispatchers.Main) {
                                                withFrameNanos { }
                                                withFrameNanos { }
                                                if (centerAfStatus == -1) centerAfStatus = 0
                                            }
                                        }
                                    }, mainExecutor)
                                }
                            }
                        }
                    }
                }

                fun publishMissingFace() {
                    if (faceTrackSmoother.onNoFace()) {
                        primaryEyeSelector.reset()
                        lastFaceAfTarget = null
                        scope.launch(Dispatchers.Main) {
                            faceTrackCenter = null
                            faceTrackSize = null
                            faceEyeCenter = null
                            faceLeftEyeCenter = null
                            faceRightEyeCenter = null
                            centerAfStatus = 0
                        }
                    }
                }
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
                val advertisedPreviewFpsRanges = activeFacingId?.let { id ->
                    runCatching {
                        cameraManager.getCameraCharacteristics(id)
                            .get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
                            ?.map { range -> PreviewFpsRange(range.lower, range.upper) }
                            .orEmpty()
                    }.onFailure {
                        android.util.Log.w("FUJI_PREVIEW_FPS", "Could not read Camera2 AE FPS ranges", it)
                    }.getOrDefault(emptyList())
                }.orEmpty()
                val fpsSelection = selectPreviewFpsRange(previewFpsMode, advertisedPreviewFpsRanges)
                fpsSelection.range?.let { selectedRange ->
                    Camera2Interop.Extender(previewBuilder).setCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                        android.util.Range(selectedRange.lower, selectedRange.upper)
                    )
                }
                scope.launch(Dispatchers.Main.immediate) {
                    previewFpsTarget = fpsSelection.targetFps?.toFloat() ?: Float.NaN
                    android.util.Log.i(
                        "FUJI_PREVIEW_FPS",
                        "mode=" + previewFpsMode +
                            " selected=" + (fpsSelection.range?.let { it.lower.toString() + ".." + it.upper } ?: "none") +
                            " target=" + (fpsSelection.targetFps?.toString() ?: "none") +
                            " fallback=" + fpsSelection.fellBack +
                            " reason=" + fpsSelection.reason
                    )
                }
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
                var lastAfTraceAtMs = 0L
                if (!hdrExtensionActive && !nightExtensionActive && !nativeProcessingExtensionActive) Camera2Interop.Extender(previewBuilder).setSessionCaptureCallback(
                    object : CameraCaptureSession.CaptureCallback() {
                        override fun onCaptureCompleted(
                            session: CameraCaptureSession,
                            request: CaptureRequest,
                            result: TotalCaptureResult
                        ) {
                            val live = Camera2Info.readLive(result, activeCameraCharacteristics)
                            val sensorTimestampNs = result.get(CaptureResult.SENSOR_TIMESTAMP)
                            if (sensorTimestampNs != null) {
                                val measuredFps = previewFpsEstimator.add(
                                    sensorTimestampNs,
                                    SystemClock.elapsedRealtime()
                                )
                                if (measuredFps != null) {
                                    scope.launch(Dispatchers.Main.immediate) {
                                        measuredPreviewFps = measuredFps
                                        android.util.Log.d(
                                            "FUJI_PREVIEW_FPS",
                                            "sensorTimestampNs=" + sensorTimestampNs +
                                                " fps=" + String.format(java.util.Locale.US, "%.2f", measuredFps)
                                        )
                                    }
                                }
                            }
                            val afRegions = request.get(CaptureRequest.CONTROL_AF_REGIONS).orEmpty()
                            val afState = result.get(CaptureResult.CONTROL_AF_STATE)
                            val afNowMs = SystemClock.elapsedRealtime()
                            if (afNowMs - lastAfTraceAtMs >= 250L &&
                                (afRegions.isNotEmpty() || afState != null)
                            ) {
                                lastAfTraceAtMs = afNowMs
                                val activeArray = activeCameraCharacteristics
                                    ?.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
                                val regionText = afRegions.joinToString(";") { region ->
                                    "${region.rect.toShortString()}@w${region.meteringWeight}"
                                }.ifBlank { "none" }
                                val stateText = when (afState) {
                                    CaptureResult.CONTROL_AF_STATE_INACTIVE -> "INACTIVE"
                                    CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN -> "PASSIVE_SCAN"
                                    CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED -> "PASSIVE_FOCUSED"
                                    CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN -> "ACTIVE_SCAN"
                                    CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED -> "FOCUSED_LOCKED"
                                    CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED -> "NOT_FOCUSED_LOCKED"
                                    CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED -> "PASSIVE_UNFOCUSED"
                                    else -> "UNKNOWN"
                                }
                                android.util.Log.d(
                                    "FUJI_AF_TRACE",
                                    "cameraId=${activeCameraDetails?.id ?: "unknown"} " +
                                        "activeArray=${activeArray?.toShortString() ?: "unknown"} " +
                                        "regions=$regionText afState=$stateText"
                                )
                            }
                            val resultControlZoomRatio = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                result.get(CaptureResult.CONTROL_ZOOM_RATIO)
                            } else {
                                null
                            }
                            val resultCropRegion = result.get(CaptureResult.SCALER_CROP_REGION)
                            val activeArrayWidth = activeCameraCharacteristics
                                ?.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
                                ?.width()
                            val cameraXZoomFallback = camera?.cameraInfo?.zoomState?.value?.zoomRatio
                            val reportedZoomRatio = resolveCaptureZoomRatio(
                                controlZoomRatio = resultControlZoomRatio,
                                activeArrayWidth = activeArrayWidth,
                                cropRegionWidth = resultCropRegion?.width(),
                                fallbackZoomRatio = cameraXZoomFallback
                            )
                            if (reportedZoomRatio != null) {
                                scope.launch(Dispatchers.Main.immediate) {
                                    zoomFromCaptureResult = reportedZoomRatio
                                }
                            }
                            val shouldUseCamera2Faces = faceTrackingEnabledState.value &&
                                faceTrackingModeState.value == "face" &&
                                hardwareFaceDetectionAvailableState.value
                            if (shouldUseCamera2Faces) {
                                val detectedFaces = result.get(CaptureResult.STATISTICS_FACES)
                                val crop = result.get(CaptureResult.SCALER_CROP_REGION)
                                    ?: activeCameraCharacteristics?.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
                                val candidates = if (crop != null && crop.width() > 0 && crop.height() > 0) {
                                    detectedFaces.orEmpty().filter { it.score >= 50 }.map { face ->
                                        val bounds = face.bounds
                                        val center = rotateFacePoint(
                                            (bounds.centerX() - crop.left).toFloat() / crop.width(),
                                            (bounds.centerY() - crop.top).toFloat() / crop.height(),
                                            faceSensorToDisplayRotation,
                                            front
                                        )
                                        val size = rotateFaceSize(
                                            bounds.width().toFloat() / crop.width(),
                                            bounds.height().toFloat() / crop.height(),
                                            faceSensorToDisplayRotation
                                        )
                                        FaceTrackCandidate(
                                            value = face,
                                            center = center,
                                            size = size,
                                            area = bounds.width().toFloat() * bounds.height(),
                                            trackingId = face.id.takeIf { it >= 0 }
                                        )
                                    }
                                } else emptyList()
                                val bestCandidate = chooseFaceTrackCandidate(
                                    candidates,
                                    faceTrackSmoother.current(),
                                    preferredFaceCenterState.value
                                )
                                val bestFace = bestCandidate?.value
                                if (bestFace != null && bestCandidate != null &&
                                    crop != null && crop.width() > 0 && crop.height() > 0
                                ) {
                                    val center = bestCandidate.center
                                    val size = bestCandidate.size
                                    val faceTimestamp = SystemClock.elapsedRealtime()
                                    // Camera2 is used for FACE-only mode; FACE+EYE takes the
                                    // ML Kit contour path above, where individual eye areas exist.
                                    publishDetectedFace(
                                        center,
                                        size,
                                        null,
                                        null,
                                        null,
                                        faceTimestamp,
                                        bestCandidate.trackingId
                                    )
                                } else {
                                    publishMissingFace()
                                }
                            } else if (!faceTrackingEnabledState.value) {
                                faceTrackSmoother.reset()
                            }
                            live.activePhysicalCameraId?.let { id ->
                                if (activePhysicalCameraId != id) activePhysicalCameraId = id
                            }
                            live.iso?.let { if (iso != it) iso = it }
                            live.shutterNs?.let { if (shutterNs != it) shutterNs = it }
                            live.aperture?.let { if (apertureValue != it) apertureValue = it }
                            live.focusDiopter?.let { if (focusDiopter != it) focusDiopter = it }
                            // Camera2 results can arrive after the user's command; keep evIndex authoritative.
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
                // Pro exposure is sent through dynamic CameraControl options. CameraX copies
                // repeating options into one-shot still captures; avoid stale builder values.
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
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setResolutionStrategy(
                                ResolutionStrategy(
                                    Size(640, 480),
                                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                                )
                            )
                            .build()
                    )
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                previewUseCase = preview
                analysisUseCase = if (livePhotoVideoCapture == null &&
                    (!nativeProcessingExtensionActive || nativeProcessingAnalysisSupported)
                ) analysis else null
                var lastAnalysis = 0L
                var lastFaceDetectionStartAtMs = 0L
                var lastLensCheckAt = 0L
                var consecutiveLensWarnings = 0
                var lastLensWarningAt = 0L
                analysis.setAnalyzer(analysisExecutor) { image ->
                    var imageReleasedByFaceDetector = false
                    try {
                        val faceDetectionNowMs = SystemClock.elapsedRealtime()
                        if (useMlFaceDetectionState.value &&
                            shouldRunFaceDetectionFrame(
                                nowMs = faceDetectionNowMs,
                                lastStartedAtMs = lastFaceDetectionStartAtMs,
                                maxFramesPerSecond = 24
                            ) &&
                            faceDetectionBusy.compareAndSet(false, true)
                        ) {
                            lastFaceDetectionStartAtMs = faceDetectionNowMs
                            val mediaImage = image.image
                            if (mediaImage != null) {
                                val faceTask = runCatching {
                                    faceDetector.process(
                                        InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
                                    )
                                }.getOrNull()
                                if (faceTask != null) {
                                    imageReleasedByFaceDetector = true
                                    faceTask.addOnSuccessListener(analysisExecutor) { faces ->
                                        val rotation = image.imageInfo.rotationDegrees
                                        val cropRect = image.cropRect
                                        val orientedCrop = orientImageCrop(
                                            image.width,
                                            image.height,
                                            cropRect.left,
                                            cropRect.top,
                                            cropRect.right,
                                            cropRect.bottom,
                                            rotation
                                        )
                                        val frameLeft = orientedCrop?.left ?: 0
                                        val frameTop = orientedCrop?.top ?: 0
                                        val frameWidth = orientedCrop?.width ?: 0
                                        val frameHeight = orientedCrop?.height ?: 0
                                        val candidates = if (frameWidth > 0 && frameHeight > 0) {
                                            faces.mapNotNull { face ->
                                                val bounds = face.boundingBox
                                                if (bounds.width() <= 0 || bounds.height() <= 0) return@mapNotNull null
                                                val center = rotateFacePoint(
                                                    (bounds.centerX() - frameLeft) / frameWidth.toFloat(),
                                                    (bounds.centerY() - frameTop) / frameHeight.toFloat(),
                                                    0,
                                                    front
                                                )
                                                val size = rotateFaceSize(
                                                    bounds.width().toFloat() / frameWidth,
                                                    bounds.height().toFloat() / frameHeight,
                                                    0
                                                )
                                                FaceTrackCandidate(
                                                    value = face,
                                                    center = center,
                                                    size = size,
                                                    area = bounds.width().toFloat() * bounds.height(),
                                                    trackingId = face.trackingId
                                                )
                                            }
                                        } else emptyList()
                                        val bestCandidate = chooseFaceTrackCandidate(
                                            candidates,
                                            faceTrackSmoother.current(),
                                            preferredFaceCenterState.value
                                        )
                                        val best = bestCandidate?.value
                                        if (best != null && bestCandidate != null &&
                                            frameWidth > 0 && frameHeight > 0
                                        ) {
                                            val center = bestCandidate.center
                                            val size = bestCandidate.size
                                            val leftEye = best.getLandmark(FaceLandmark.LEFT_EYE)?.position
                                            val rightEye = best.getLandmark(FaceLandmark.RIGHT_EYE)?.position
                                            val faceTimestamp = SystemClock.elapsedRealtime()
                                            val leftEyeArea = eyeContourArea(
                                                best.getContour(FaceContour.LEFT_EYE)?.points?.map { Offset(it.x, it.y) }
                                            )
                                            val rightEyeArea = eyeContourArea(
                                                best.getContour(FaceContour.RIGHT_EYE)?.points?.map { Offset(it.x, it.y) }
                                            )
                                            val previousPrimaryEye = primaryEyeSelector.currentSide()
                                            val primaryEye = primaryEyeSelector.select(
                                                leftEye = leftEye?.let { Offset(it.x, it.y) },
                                                rightEye = rightEye?.let { Offset(it.x, it.y) },
                                                headYawDegrees = best.headEulerAngleY,
                                                nowMs = faceTimestamp,
                                                trackingId = bestCandidate.trackingId,
                                                leftEyeArea = leftEyeArea,
                                                rightEyeArea = rightEyeArea,
                                                leftConfidence = if (leftEyeArea != null) 1f else 0f,
                                                rightConfidence = if (rightEyeArea != null) 1f else 0f
                                            )
                                            fun mapMlKitEye(point: Offset?): Offset? = point?.let {
                                                rotateFacePoint(
                                                    (it.x - frameLeft) / frameWidth.toFloat(),
                                                    (it.y - frameTop) / frameHeight.toFloat(),
                                                    0,
                                                    front
                                                )
                                            }
                                            val selectedPrimaryEye = primaryEyeSelector.currentSide()
                                            if (previousPrimaryEye != null && selectedPrimaryEye != null &&
                                                previousPrimaryEye != selectedPrimaryEye
                                            ) {
                                                val switchRate = eyeSwitchRateTracker.recordSwitch(faceTimestamp)
                                                android.util.Log.i(
                                                    "FUJI_FACE_TRACK",
                                                    "PRIMARY_EYE_SWITCH source=MLKIT from=$previousPrimaryEye to=$selectedPrimaryEye switchesLast60s=$switchRate"
                                                )
                                            }
                                            val eye = mapMlKitEye(primaryEye)
                                            val leftEyeDisplay = mapMlKitEye(
                                                leftEye?.let { Offset(it.x, it.y) }
                                            )
                                            val rightEyeDisplay = mapMlKitEye(
                                                rightEye?.let { Offset(it.x, it.y) }
                                            )
                                            publishDetectedFace(
                                                center,
                                                size,
                                                eye,
                                                leftEyeDisplay,
                                                rightEyeDisplay,
                                                faceTimestamp,
                                                bestCandidate.trackingId
                                            )
                                        } else {
                                            publishMissingFace()
                                        }
                                    }
                                    faceTask.addOnFailureListener(analysisExecutor) {
                                        android.util.Log.w("FUJI_FACE_TRACK", "ML Kit face detector failed.", it)
                                        publishMissingFace()
                                    }
                                    faceTask.addOnCompleteListener(analysisExecutor) {
                                        faceDetectionBusy.set(false)
                                        image.close()
                                    }
                                } else {
                                    faceDetectionBusy.set(false)
                                }
                            } else {
                                faceDetectionBusy.set(false)
                            }
                        }
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
                        if (!imageReleasedByFaceDetector) image.close()
                    }
                }

                // Use the exact ratio used by the Compose preview container. Reading
                // PreviewView.width/height here can pick up the previous layout during rotation.
                val viewPort = ViewPort.Builder(ratio, rotation)
                    .setScaleType(ViewPort.FILL_CENTER)
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
                android.util.Log.i(
                    "FUJI_CAMERA_REBIND",
                    "BOUND generation=$bindGeneration cameraId=${runCatching { Camera2CameraInfo.from(bound.cameraInfo).cameraId }.getOrNull() ?: "unknown"}"
                )
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
                val faceDetectMode = if (faceTrackingMode == "face") {
                    when {
                        availableFaceModes.contains(CaptureRequest.STATISTICS_FACE_DETECT_MODE_FULL) ->
                            CaptureRequest.STATISTICS_FACE_DETECT_MODE_FULL
                        availableFaceModes.contains(CaptureRequest.STATISTICS_FACE_DETECT_MODE_SIMPLE) ->
                            CaptureRequest.STATISTICS_FACE_DETECT_MODE_SIMPLE
                        else -> null
                    }
                } else null
                hardwareFaceDetectionAvailable = faceDetectMode != null
                if (faceTrackingEnabled) {
                    runCatching {
                        val faceOptions = CaptureRequestOptions.Builder()
                            .setCaptureRequestOption(
                                CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                                faceDetectMode ?: CaptureRequest.STATISTICS_FACE_DETECT_MODE_OFF
                            )
                            .build()
                        Camera2CameraControl.from(bound.cameraControl).addCaptureRequestOptions(faceOptions)
                        if (faceDetectMode != null) {
                            android.util.Log.i(
                                "FUJI_FACE_TRACK",
                                "mode=face; camera2FaceMode=${if (faceDetectMode == CaptureRequest.STATISTICS_FACE_DETECT_MODE_FULL) "FULL" else "SIMPLE"}"
                            )
                        } else {
                            android.util.Log.i(
                                "FUJI_FACE_TRACK",
                                "mode=face_eye; Camera2 face stats OFF; ML Kit eye contours enabled at 640x480/24fps."
                            )
                        }
                    }.onFailure {
                        android.util.Log.w("FUJI_FACE_TRACK", "Could not configure Camera2 face detection", it)
                        hardwareFaceDetectionAvailable = false
                    }
                } else {
                    hardwareFaceDetectionAvailable = false
                    faceTrackCenter = null
                    faceTrackSize = null
                    faceEyeCenter = null
                    faceTrackSmoother.reset()
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
                evIndex = appPrefs.getInt("pro_ev_index", 0)
                val zoomStateOnBind = bound.cameraInfo.zoomState.value
                val savedZoom = appPrefs.getFloat("camera_zoom_ratio", 1f)
                val cameraXZoomMaximum = zoomStateOnBind?.maxZoomRatio ?: 1f
                val zoomAssessment = Camera2Info.opticalZoomAssessment(activeCameraDetails, activePhysicalCameraDetails, camera2Inventory)
                val cameraReportedZoomMaximum = Camera2Info.cameraReportedZoomMax(activeCameraDetails, cameraXZoomMaximum)
                val maxSelectableZoom = Camera2Info.maxSelectableZoom(
                    activeCameraDetails,
                    activePhysicalCameraDetails,
                    cameraXZoomMaximum,
                    camera2Inventory
                )
                val maxTotalZoom = maxTotalSelectableZoom(cameraReportedZoomMaximum, zoomAssessment)
                val lensEquivalentSummary = buildList {
                    activeCameraDetails?.eq35Mm?.let { add("logical=" + String.format(Locale.US, "%.1f", it)) }
                    activePhysicalCameraDetails.forEach { lens ->
                        lens.eq35Mm?.let { add(lens.id + "=" + String.format(Locale.US, "%.1f", it)) }
                    }
                }.joinToString()
                android.util.Log.i(
                    "FUJI_ZOOM_CAPABILITY",
                    "cameraId=" + actualCameraId + " lenses35mm=[" + lensEquivalentSummary + "] main35mm=" +
                        zoomAssessment.mainEquivalent35Mm + " longest35mm=" + zoomAssessment.longestEquivalent35Mm +
                        " opticalTele=" + zoomAssessment.hasOpticalTelephoto + " opticalRatio=" + zoomAssessment.opticalRatio +
                        " CONTROL_ZOOM_RATIO_RANGE=" + activeCameraDetails?.controlZoomRatioMin + ".." + activeCameraDetails?.controlZoomRatioMax +
                        " cameraXMax=" + cameraXZoomMaximum + " reportedMax=" + cameraReportedZoomMaximum +
                        " cameraControlMax=" + maxSelectableZoom + " totalMax=" + maxTotalZoom +
                        " digitalZoomAvailable=" + (maxTotalZoom > maxSelectableZoom) +
                        " min=" + (zoomStateOnBind?.minZoomRatio ?: 1f)
                )
                val restoredPlan = resolveZoomExecutionPlan(
                    requestedZoomRatio = savedZoom,
                    minimumCameraRatio = zoomStateOnBind?.minZoomRatio ?: 1f,
                    cameraReportedMaximum = cameraReportedZoomMaximum,
                    assessment = zoomAssessment
                )
                val restoredZoom = restoredPlan.effectiveZoomRatio
                zoomSel = restoredZoom
                actualZoomRatio = restoredZoom
                digitalZoomCropScale = restoredPlan.digitalCropRatio
                zoomFromCaptureResult = Float.NaN
                runCatching { bound.cameraControl.setZoomRatio(restoredPlan.cameraControlZoomRatio) }
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
    LaunchedEffect(targetRotation, isLandscape, imageCapture, previewUseCase, analysisUseCase, videoCaptureUseCase) {
        imageCapture?.targetRotation = targetRotation
        previewUseCase?.targetRotation = targetRotation
        analysisUseCase?.targetRotation = targetRotation
        videoCaptureUseCase?.targetRotation = targetRotation
        // Record the latest physical orientation after both sensor rotation and preview geometry update.
        boundCameraRotation = targetRotation
        boundCameraLandscape = isLandscape
    }

    // Restore the latest app-owned photo after relaunch, then refresh its thumbnail.
    LaunchedEffect(Unit) {
        if (lastUri == null) {
            val recent = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        arrayOf(MediaStore.Images.Media._ID),
                        "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? AND ${MediaStore.Images.Media.MIME_TYPE} = ?",
                        arrayOf("Pictures/FujifilmMode/%", "image/jpeg"),
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

    LaunchedEffect(lastUri, thumbUri) {
        val uri = lastUri
        if (uri == null) {
            thumb = null
            thumbUri = null
            return@LaunchedEffect
        }
        if (thumbUri == uri && thumb != null) return@LaunchedEffect
        val decodedThumbnail = withContext(Dispatchers.IO) {
            runCatching {
                decodeUprightBitmap(context, uri, maxDimension = 320)?.asImageBitmap()
            }.getOrNull()
        }
        thumb = decodedThumbnail
        thumbUri = uri.takeIf { decodedThumbnail != null }
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
        val uri = lastUri
        if (uri == null) {
            localizedToast(context, "Chưa có ảnh trong Pictures/FujifilmMode.", Toast.LENGTH_SHORT).show()
            return
        }
        // Open the saved MediaStore item in the device's native photo/gallery app.
        // The in-app viewer is retained only as a fallback when no external handler exists.
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            context.startActivity(viewIntent)
        }.onFailure { failure ->
            android.util.Log.w(
                "FUJI_GALLERY",
                "No native image viewer could open the latest photo; using the built-in viewer.",
                failure
            )
            photoViewerVisible = true
        }
    }

    fun startStarTrailSession(activeCapture: ImageCapture, activeCamera: Camera) {
        val captureRotation = targetRotation
        val captureIsLandscape = isLandscape
        if (boundCameraRotation != captureRotation ||
            boundCameraLandscape != isLandscape
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
            var stoppedForBattery = false
            try {
                while (!starTrailStopRequested) {
                    if (!batteryCharging && batteryPercent in 0..5) {
                        stoppedForBattery = true
                        break
                    }
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

                if (stoppedForBattery) {
                    android.util.Log.w(
                        "FUJI_NIGHTSHOT",
                        "Star Trails stopped at critical battery; retainedFrames=" + frames.size
                    )
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
                            listOf(look, stamped, metadata)
                        }
                        val previewThumb = withContext(Dispatchers.IO) {
                            runCatching {
                                decodeUprightBitmap(context, starUri, maxDimension = 512)?.asImageBitmap()
                            }.getOrNull()
                        }
                        withContext(Dispatchers.Main) {
                            lastUri = starUri
                            thumb = previewThumb
                            thumbUri = starUri.takeIf { previewThumb != null }
                            processingProgress = 100
                            localizedToast(
                                context,
                                "Đã lưu Star Trails từ " + frames.size + " khung hình" +
                                    if (postProcessed.last()) "." else " (cần kiểm tra lại metadata).",
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
        val needsCriticalBatteryProtection = !batteryCharging &&
            batteryPercent in 0..5 &&
            (nightShotMode != "off" || lunaDreamMode == "luna20" || starTrailCaptureInProgress)
        if (needsCriticalBatteryProtection) {
            localizedToast(
                context,
                AppLanguageManager.localizedContext(context).getString(R.string.battery_processing_blocked),
                Toast.LENGTH_LONG
            ).show()
            return
        }
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
                    thumbUri = mergedBracketUri.takeIf { thumb != null }
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
                    thumbUri = lastSuccessfulUri.takeIf { thumb != null }
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
            val captureZoomState = activeCamera.cameraInfo.zoomState.value
            val captureZoomAssessment = Camera2Info.opticalZoomAssessment(
                activeCameraDetails,
                activePhysicalCameraDetails,
                camera2Inventory
            )
            val captureCameraReportedMaximum = Camera2Info.cameraReportedZoomMax(
                activeCameraDetails,
                captureZoomState?.maxZoomRatio ?: 1f
            )
            val captureZoomPlan = resolveZoomExecutionPlan(
                requestedZoomRatio = zoomSel,
                minimumCameraRatio = captureZoomState?.minZoomRatio ?: 1f,
                cameraReportedMaximum = captureCameraReportedMaximum,
                assessment = captureZoomAssessment
            )
            val captureRequestedZoomRatio = captureZoomPlan.effectiveZoomRatio
            val captureActualCameraZoomRatio = zoomFromCaptureResult
                .takeIf { it.isFinite() && it > 0f }
                ?: captureZoomState?.zoomRatio
                ?: captureZoomPlan.cameraControlZoomRatio
            val captureDigitalCropRatio = if (captureZoomPlan.digitalCropRatio > 1.001f) {
                (captureRequestedZoomRatio / captureActualCameraZoomRatio.coerceAtLeast(0.01f))
                    .coerceAtLeast(1f)
            } else {
                1f
            }
            android.util.Log.i(
                "FUJI_ZOOM_CAPTURE_PLAN",
                "requestedTotal=$captureRequestedZoomRatio plannedCamera=${captureZoomPlan.cameraControlZoomRatio} " +
                    "captureResultCamera=$captureActualCameraZoomRatio digitalCrop=$captureDigitalCropRatio " +
                    "cameraMax=${captureZoomPlan.cameraControlMaximum} totalMax=${captureZoomPlan.totalSelectableMaximum}"
            )
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
                LunaDreamDiagnostics.record(
                    context,
                    "CAPTURE_REJECTED",
                    "selectedMode=$lunaDreamMode; rawMode=$rawCaptureMode; photoMode=$mode; " +
                        "ultraHdr=$captureIsUltraHdr; burst=$burstMode; hdrBracket=$hdrBracketMode; focusBracket=$focusBracketMode"
                )
                localizedToast(
                    context,
                    "LunaDream cần Photo/JPEG; RawDream 2.0 cần RAW + JPEG được camera hỗ trợ. Hãy tắt Ultra HDR, Burst và Bracketing.",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }
            if (captureUseLunaDream) {
                LunaDreamDiagnostics.record(
                    context,
                    "CAPTURE_REQUESTED",
                    "mode=" + (when {
                        captureUseRawDream20 -> "RawDream 2.0"
                        captureUseLunaDream15 -> "LunaDream 1.5"
                        else -> "LunaDream 2.0"
                    }) + "; zoom=$zoomSel; rawMode=$rawCaptureMode; ultraHdr=$captureIsUltraHdr"
                )
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
            // Mechanical shutter curtain: close from the top, hold briefly, then reopen.
            // A black curtain is used instead of a white flash or color wash.
            scope.launch {
                shutterEffectProgress.snapTo(0f)
                shutterEffectProgress.animateTo(1f, tween(40, easing = FastOutSlowInEasing))
                val shutterForCurtainNs = if (manualShutterEnabled) manualShutterNs else shutterNs
                if (shutterForCurtainNs > 33_333_333L) delay(60L) else delay(30L)
                shutterEffectProgress.animateTo(0f, tween(60, easing = FastOutSlowInEasing))
            }
            // Freeze display rotation and the active camera's sensor-relative rotation for
            // this still capture. CameraX combines target rotation with SENSOR_ORIENTATION.
            // Freeze the physical handset orientation tracked by OrientationEventListener.
            // PreviewView/Display.rotation may remain portrait while the handset is landscape.
            val captureRotation = targetRotation
            val captureIsLandscape = isLandscape
            // One height/width ratio source is shared by the Preview frame and saved-photo crop.
            // Landscape uses the reciprocal height/width ratio; the selected crop still applies.
            val selectedFrameHeightOverWidth =
                aspectRatioForOrientation(ratioIdx, captureIsLandscape)
            val outputHeightOverWidth = selectedFrameHeightOverWidth
            // Orientation detection uses the camera's neutral width/height ratio and never crops to it.
            val expectedOrientationAspectRatio = if (captureIsLandscape) 4f / 3f else 3f / 4f
            if (captureRotation != targetRotation ||
                boundCameraRotation != captureRotation ||
                boundCameraLandscape != isLandscape
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
                multiFrameCapturedFrames = 0
                multiFrameTargetFrames = when {
                    captureUseNightShot -> captureNightTargetFrames
                    captureUseLunaDream15 -> 5
                    else -> 8
                }
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
                            LunaDreamProcessingScope.launch {
                                try {
                                // Publish a small preview of the first valid JPEG immediately,
                                // before multi-frame stacking, AI render, HDR or watermark work.
                                val initialThumbnail = withContext(Dispatchers.IO) {
                                    runCatching {
                                        decodeUprightBitmap(context, saved, maxDimension = 320)?.asImageBitmap()
                                    }.getOrNull()
                                }
                                withContext(Dispatchers.Main.immediate) {
                                    lastUri = saved
                                    if (initialThumbnail != null) {
                                        thumb = initialThumbnail
                                        thumbUri = saved
                                    }
                                }
                                android.util.Log.d(
                                    "FUJI_THUMBNAIL",
                                    "Initial capture thumbnail published=${initialThumbnail != null} uri=$saved before post-processing"
                                )
                                var nightShotMerged = false
                                var lunaDreamCapturedFrameCount = 0
                                var lunaDreamFailureReason: String? = null
                                var lunaDream20FallbackApplied = false
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
                                            withContext(Dispatchers.Main.immediate) {
                                                multiFrameCapturedFrames = stackedFrames.size
                                                multiFrameTargetFrames = maxFrames
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
                                            var stoppedForBattery = false
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
                                                    if ((captureUseNightShot || captureUseMilkyWayStack ||
                                                            captureUseLunaDream20Stack) &&
                                                        !batteryCharging && batteryPercent in 0..5
                                                    ) {
                                                        stoppedForBattery = true
                                                        stopSubmitting = true
                                                        break
                                                    }
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
                                                    if (captureUseLunaDream) {
                                                        LunaDreamDiagnostics.record(
                                                            context,
                                                            "FRAME_CAPTURE_FAILED",
                                                            "frameAttempt=$frameNumber; validFrames=${stackedFrames.size}; " +
                                                                "mode=" + if (captureUseLunaDream15) "LunaDream 1.5" else "LunaDream 2.0"
                                                        )
                                                    }
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
                                                withContext(Dispatchers.Main.immediate) {
                                                    multiFrameCapturedFrames = stackedFrames.size
                                                }
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
                                            if (stoppedForBattery) {
                                                android.util.Log.w(
                                                    "FUJI_NIGHTSHOT",
                                                    "Stopped frame submission at critical battery; retainedFrames=" + stackedFrames.size
                                                )
                                            }
                                            if (captureUseNightShot && stackedFrames.size < maxFrames && !stoppedForBattery) {
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
                                                captureUseNightShot && !stoppedForBattery -> maxFrames
                                                captureUseLunaDream15 -> 3
                                                else -> 2
                                            }
                                            lunaDreamCapturedFrameCount = stackedFrames.size
                                            if (captureUseLunaDream) {
                                                LunaDreamDiagnostics.record(
                                                    context,
                                                    "FRAME_CAPTURE_SUMMARY",
                                                    "mode=" + (if (captureUseLunaDream15) "LunaDream 1.5" else if (captureUseRawDream20) "RawDream 2.0" else "LunaDream 2.0") +
                                                        "; validFrames=${stackedFrames.size}; targetFrames=$maxFrames; minimumRequired=$minimumFramesRequired; " +
                                                        "captureElapsed=${burstElapsedMs}ms; stoppedForStorage=$stoppedForStorage"
                                                )
                                            }
                                            if (stackedFrames.size < minimumFramesRequired && captureUseLunaDream) {
                                                lunaDreamFailureReason =
                                                    "Chỉ thu được ${stackedFrames.size}/$minimumFramesRequired khung hình hợp lệ"
                                                android.util.Log.e("FUJI_LUNADREAM", lunaDreamFailureReason!!)
                                                LunaDreamDiagnostics.record(
                                                    context,
                                                    "FRAME_COUNT_INSUFFICIENT",
                                                    "mode=" + (if (captureUseLunaDream15) "LunaDream 1.5" else "LunaDream 2.0") +
                                                        "; validFrames=${stackedFrames.size}; required=$minimumFramesRequired; reason=${lunaDreamFailureReason}"
                                                )
                                            }
                                            if (stackedFrames.size >= minimumFramesRequired) {
                                                withContext(Dispatchers.Main.immediate) {
                                                    multiFrameHoldActive = false
                                                    multiFrameRenderStarted = true
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
                                                    },
                                                    onFailure = { reason, failure ->
                                                        lunaDreamFailureReason = reason
                                                        if (captureUseLunaDream) {
                                                            LunaDreamDiagnostics.record(
                                                                context,
                                                                "MERGE_CALLBACK_FAILURE",
                                                                "mode=" + (if (captureUseLunaDream15) "LunaDream 1.5" else if (captureUseRawDream20) "RawDream 2.0" else "LunaDream 2.0") +
                                                                    "; reason=$reason; validFrames=${stackedFrames.size}",
                                                                failure
                                                            )
                                                        }
                                                        android.util.Log.e(
                                                            "FUJI_LUNADREAM",
                                                            "Merge failed; preserving original JPEG as fallback: $reason",
                                                            failure
                                                        )
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
                                                if (!nightShotMerged && captureUseLunaDream20Stack) {
                                                    val primaryFailure = lunaDreamFailureReason
                                                        ?: "LunaDream 2.0 merge returned false without a failure callback"
                                                    if (stackedFrames.size >= 3) {
                                                        var fallbackFailure: String? = null
                                                        LunaDreamDiagnostics.record(
                                                            context,
                                                            "FALLBACK_20_TO_15_START",
                                                            "originalMode=2.0; validFrames=${stackedFrames.size}; primaryFailure=$primaryFailure"
                                                        )
                                                        android.util.Log.w(
                                                            "FUJI_LUNADREAM",
                                                            "LunaDream 2.0 merge failed; retrying the same ${stackedFrames.size} frames with the 1.5 fallback. reason=$primaryFailure"
                                                        )
                                                        nightShotMerged = NightShotProcessor.mergeFramesIntoUri(
                                                            context,
                                                            stackedFrames,
                                                            saved,
                                                            NightShotProcessor.MergeMode.LUNA_DREAM_1_5,
                                                            onFailure = { reason, failure ->
                                                                fallbackFailure = reason
                                                                android.util.Log.e(
                                                                    "FUJI_LUNADREAM",
                                                                    "LunaDream 1.5 fallback merge failed: $reason",
                                                                    failure
                                                                )
                                                            },
                                                            onProgress = { fallbackProgress ->
                                                                mainExecutor.execute {
                                                                    processingLabel = "LunaDream 1.5 fallback · $fallbackProgress%"
                                                                    processingProgress = maxOf(processingProgress, fallbackProgress.coerceIn(0, 100))
                                                                }
                                                            }
                                                        )
                                                        if (nightShotMerged) {
                                                            LunaDreamDiagnostics.record(
                                                                context,
                                                                "FALLBACK_20_TO_15_SUCCESS",
                                                                "validFrames=${stackedFrames.size}; primaryFailure=$primaryFailure"
                                                            )
                                                            lunaDream20FallbackApplied = true
                                                            lunaDreamFailureReason = primaryFailure
                                                            android.util.Log.w(
                                                                "FUJI_LUNADREAM",
                                                                "LunaDream 1.5 fallback succeeded after 2.0 failure; frames=${stackedFrames.size}; reason=$primaryFailure"
                                                            )
                                                        } else {
                                                            lunaDreamFailureReason =
                                                                "LunaDream 2.0 failed: $primaryFailure; LunaDream 1.5 fallback failed: ${fallbackFailure ?: "merge returned false"}"
                                                            LunaDreamDiagnostics.record(
                                                                context,
                                                                "FALLBACK_20_TO_15_FAILED",
                                                                "validFrames=${stackedFrames.size}; primaryFailure=$primaryFailure; fallbackFailure=${fallbackFailure ?: "merge returned false"}"
                                                            )
                                                        }
                                                    } else {
                                                        lunaDreamFailureReason =
                                                            "LunaDream 2.0 failed: $primaryFailure; 1.5 fallback requires at least 3 valid frames, but only ${stackedFrames.size} were captured"
                                                        LunaDreamDiagnostics.record(
                                                            context,
                                                            "FALLBACK_20_TO_15_SKIPPED",
                                                            "validFrames=${stackedFrames.size}; requiredAtLeast=3; primaryFailure=$primaryFailure"
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
                                        if (captureUseLunaDream && !nightShotMerged && lunaDreamFailureReason == null) {
                                            lunaDreamFailureReason = "Could not copy the first captured JPEG into the processing cache"
                                        }
                                    } finally {
                                        stackedFrames.forEach { runCatching { it.delete() } }
                                        withContext(Dispatchers.Main) {
                                            if (captureUseNightShot) nightShotCaptureInProgress = false
                                            if (captureUseLunaDream || captureUseNightShot) {
                                                // Frame acquisition has ended; merge/render now runs in the background.
                                                multiFrameHoldActive = false
                                                multiFrameRenderStarted = true
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
                                            thumbUri = saved.takeIf { originalThumbnail != null }
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
                                if (captureUseLunaDream) {
                                    LunaDreamDiagnostics.record(
                                        context,
                                        if (lunaDreamApplied) "CAPTURE_PROCESSING_COMPLETE" else "CAPTURE_PROCESSING_INCOMPLETE",
                                        "mode=" + (if (captureUseLunaDream15) "LunaDream 1.5" else if (captureUseRawDream20) "RawDream 2.0" else "LunaDream 2.0") +
                                            "; validFrames=$lunaDreamCapturedFrameCount; fallbackApplied=$lunaDream20FallbackApplied; " +
                                            "merged=$nightShotMerged; reason=${lunaDreamFailureReason ?: "none"}"
                                    )
                                }
                                withContext(Dispatchers.Main) {
                                    if (captureUseLunaDream || captureUseNightShot) {
                                        multiFrameHoldActive = false
                                        multiFrameRenderStarted = true
                                    }
                                    processingLabel = when {
                                        captureUseLunaDream15 && lunaDreamApplied ->
                                            "LunaDream Render · 1.5 đang phục hồi chi tiết"
                                        captureUseRawDream20 && lunaDream20FallbackApplied ->
                                            "RawDream 2.0 · đang hoàn tất JPEG bằng dự phòng LunaDream 1.5"
                                        captureUseLunaDream20 && lunaDream20FallbackApplied ->
                                            "LunaDream 2.0 · đang hoàn tất bằng dự phòng 1.5"
                                        captureUseRawDream20 && lunaDreamApplied ->
                                            "LunaDream Render · RawDream 2.0 đang xử lý JPEG ghép"
                                        captureUseLunaDream20 && lunaDreamApplied ->
                                            "LunaDream Render · 2.0 đang phục hồi chi tiết"
                                        captureUseNightShot && (nightShotMerged || captureNightShotExtension) ->
                                            "NightShot Render · đang hoàn tất ảnh đã ghép"
                                        captureUseLunaDream ->
                                            localizedUiText(context, "LunaDream không thể hoàn tất; ảnh gốc đã được giữ. Lý do:") +
                                                " " + (lunaDreamFailureReason ?: "No valid multi-frame result was produced")
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
                                        captureUseLunaDream15 && lunaDreamApplied -> 50_000_000L
                                        captureUseLunaDream20Stack && lunaDream20FallbackApplied -> 50_000_000L
                                        captureUseLunaDream && lunaDreamApplied -> 108_000_000L
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
                                        thumbUri = null
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

                                val digitalZoomResult = if (
                                    !captureIsUltraHdr &&
                                    !captureRawOnly &&
                                    captureDigitalCropRatio > 1.001f
                                ) {
                                    withContext(Dispatchers.Main) {
                                        processingProgress = maxOf(processingProgress, 87)
                                        processingLabel = context.getString(R.string.digital_zoom_processing)
                                    }
                                    withContext(Dispatchers.IO) {
                                        DigitalZoomProcessor.applyLanczosCropToUri(
                                            context = context,
                                            uri = saved,
                                            digitalCropRatio = captureDigitalCropRatio
                                        )
                                    }
                                } else null
                                val digitalZoomApplied = digitalZoomResult != null
                                if (captureDigitalCropRatio > 1.001f && !digitalZoomApplied) {
                                    android.util.Log.e(
                                        "FUJI_DIGITAL_ZOOM",
                                        "Requested total zoom=$captureRequestedZoomRatio but photo crop failed; " +
                                            "camera-only JPEG is retained and must not be labeled DIGITAL"
                                    )
                                }

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
                                    val reason = lunaDreamFailureReason
                                        ?.takeIf { it.isNotBlank() }
                                        ?: "Không thu đủ dữ liệu hoặc engine không trả kết quả ghép"
                                    runCatching {
                                        context.contentResolver.update(
                                            saved,
                                            ContentValues().apply {
                                                put(MediaStore.Images.Media.IS_PENDING, 0)
                                            },
                                            null,
                                            null
                                        )
                                    }.onFailure {
                                        android.util.Log.e("FUJI_LUNADREAM", "Could not publish fallback JPEG", it)
                                    }
                                    android.util.Log.w(
                                        "FUJI_LUNADREAM",
                                        "Fallback JPEG saved at original capture URI; reason=$reason"
                                    )
                                    withContext(Dispatchers.Main) {
                                        processingLabel = "LunaDream fallback · $reason"
                                        processingProgress = maxOf(processingProgress, 90)
                                    }
                                }

                                val metadataPublished = runCatching {
                                    if (captureIsUltraHdr) publishUltraHdrCapture(context, saved)
                                    else finalizeCapturedImageMetadata(context, saved)
                                }.getOrDefault(false)
                                val digitalXmpPublished = if (digitalZoomApplied) {
                                    DigitalZoomXmpWriter.writeDigitalZoom(
                                        context = context,
                                        uri = saved,
                                        requestedZoomRatio = captureRequestedZoomRatio,
                                        cameraControlZoomRatio = captureActualCameraZoomRatio,
                                        digitalCropRatio = captureDigitalCropRatio
                                    )
                                } else false
                                // Normalization runs before image looks and watermark drawing.
                                // No later rotation is allowed to move the frame away from the
                                // final upright pixel coordinate system.
                                val orientationLabelFixed = orientationNormalized && metadataPublished
                                val inspectorLunaDreamStatus = when {
                                    captureUseRawDream20 && lunaDream20FallbackApplied ->
                                        "RawDream 2.0 · JPEG fallback LunaDream 1.5 · reason: " + (lunaDreamFailureReason ?: "unknown")
                                    captureUseLunaDream20 && lunaDream20FallbackApplied ->
                                        "LunaDream 2.0 · fallback LunaDream 1.5 · reason: " + (lunaDreamFailureReason ?: "unknown")
                                    captureUseLunaDream15 && lunaDreamApplied -> "LunaDream 1.5 · ghép " + lunaDreamCapturedFrameCount + "/5 khung"
                                    captureUseRawDream20 && lunaDreamApplied -> "RawDream 2.0 · DNG gốc + JPEG ghép " + lunaDreamCapturedFrameCount + "/8 khung bằng LunaDream 2.0"
                                    captureUseLunaDream20 && lunaDreamApplied -> "LunaDream 2.0 · ghép đa khung và phục hồi chi tiết"
                                    captureUseLunaDream -> "LunaDream fallback · " + (lunaDreamFailureReason ?: "engine failure")
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
                                    .putString(
                                        "photo_digital_zoom:$saved",
                                        when {
                                            digitalZoomApplied && digitalXmpPublished ->
                                                "DIGITAL total=$captureRequestedZoomRatio camera=${captureZoomPlan.cameraControlZoomRatio} crop=$captureDigitalCropRatio"
                                            digitalZoomApplied ->
                                                "DIGITAL pixels applied; XMP write failed"
                                            captureDigitalCropRatio > 1.001f ->
                                                "Digital crop failed; original camera JPEG retained"
                                            else -> "Camera-controlled zoom=$captureRequestedZoomRatio"
                                        }
                                    )
                                    .apply()
                                val finalThumbnail = withContext(Dispatchers.IO) {
                                    runCatching {
                                        decodeUprightBitmap(context, saved, maxDimension = 320)?.asImageBitmap()
                                    }.getOrNull()
                                }
                                withContext(Dispatchers.Main) {
                                    processingProgress = 98
                                    processingEtaSeconds = 1
                                }
                                withContext(Dispatchers.Main) {
                                    lastUri = saved
                                    thumb = finalThumbnail
                                    thumbUri = saved.takeIf { finalThumbnail != null }
                                    processingProgress = 100
                                    val watermarkRequested = appPrefs.getBoolean("watermark_enabled", true)
                                    if (captureIsUltraHdr && watermarkRequested) {
                                        android.util.Log.i(
                                            "FUJI_WATERMARK",
                                            "Skipped watermark on Ultra HDR to preserve the original gain map."
                                        )
                                    }
                                    val actualOutputMp = outputResolutionResult?.let {
                                        ((it.actualPixels + 500_000L) / 1_000_000L).toInt()
                                    } ?: 0
                                    val message = when {
                                        captureUseRawDream20 && lunaDream20FallbackApplied ->
                                            localizedUiText(context, "LunaDream 2.0 không hoàn tất; đã dùng thuật toán dự phòng 1.5. Lý do:") +
                                                " " + (lunaDreamFailureReason ?: "unknown") + " · JPEG ${actualOutputMp}MP; DNG gốc được giữ."
                                        captureUseLunaDream20 && lunaDream20FallbackApplied ->
                                            localizedUiText(context, "LunaDream 2.0 không hoàn tất; đã dùng thuật toán dự phòng 1.5. Lý do:") +
                                                " " + (lunaDreamFailureReason ?: "unknown") + " · ${actualOutputMp}MP"
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
                                            "LunaDream không hoàn tất: " + (lunaDreamFailureReason ?: "engine không cung cấp nguyên nhân") + ". Đã lưu JPEG gốc làm ảnh dự phòng."
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
                                        captureIsUltraHdr && watermarkRequested && orientationLabelFixed ->
                                            localizedUiText(context, "Đã lưu Ultra HDR nguyên bản và giữ gain map. Watermark được bỏ qua để bảo toàn gain map.")
                                        captureIsUltraHdr && orientationLabelFixed -> "Đã lưu Ultra HDR nguyên bản và giữ gain map."
                                        captureIsUltraHdr && watermarkRequested ->
                                            localizedUiText(context, "Đã lưu Ultra HDR nguyên bản. Watermark được bỏ qua để bảo toàn gain map.")
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
                                    val digitalZoomSuffix = when {
                                        digitalZoomApplied && digitalXmpPublished ->
                                            " " + context.getString(R.string.digital_zoom_success)
                                        digitalZoomApplied ->
                                            " " + context.getString(R.string.digital_zoom_xmp_failed)
                                        captureDigitalCropRatio > 1.001f ->
                                            " " + context.getString(R.string.digital_zoom_crop_failed)
                                        else -> ""
                                    }
                                    val finalMessage = (if (captureIsUltraHdr) message
                                        else message) +
                                        digitalZoomSuffix
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
                            } catch (failure: Throwable) {
                                if (failure is kotlinx.coroutines.CancellationException) throw failure
                                val reason = failure.message?.takeIf { it.isNotBlank() }
                                    ?: failure.javaClass.simpleName
                                android.util.Log.e(
                                    "FUJI_LUNADREAM",
                                    "Capture processing pipeline failed; publishing the last valid capture. reason=$reason",
                                    failure
                                )
                                val fallbackThumbnail = runCatching {
                                    decodeUprightBitmap(context, saved, maxDimension = 320)?.asImageBitmap()
                                }.getOrNull()
                                runCatching {
                                    context.contentResolver.update(
                                        saved,
                                        ContentValues().apply {
                                            put(MediaStore.Images.Media.IS_PENDING, 0)
                                        },
                                        null,
                                        null
                                    )
                                }.onFailure {
                                    android.util.Log.e("FUJI_LUNADREAM", "Could not publish the preserved capture", it)
                                }
                                withContext(Dispatchers.Main) {
                                    lastUri = saved
                                    thumb = fallbackThumbnail
                                    thumbUri = saved.takeIf { fallbackThumbnail != null }
                                    isProcessing = false
                                    processingProgress = 0
                                    processingEtaSeconds = -1
                                    nightShotCaptureInProgress = false
                                    multiFrameHoldActive = false
                                    multiFrameRenderStarted = false
                                    localizedToast(
                                        context,
                                        localizedUiText(
                                            context,
                                            "Ảnh đã chụp nhưng hậu kỳ gặp lỗi; tệp ảnh hợp lệ gần nhất đã được giữ. Lý do:"
                                        ) + " " + reason,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
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
        val zoomAssessment = Camera2Info.opticalZoomAssessment(activeCameraDetails, activePhysicalCameraDetails, camera2Inventory)
        val cameraReportedMaximum = Camera2Info.cameraReportedZoomMax(activeCameraDetails, state.maxZoomRatio)
        val selectableMax = maxTotalSelectableZoom(cameraReportedMaximum, zoomAssessment)
        val safeTarget = targetRatio.coerceIn(state.minZoomRatio, selectableMax)
        val start = zoomSel
        val crossesLensBoundary = (start < 0.98f && safeTarget >= 0.98f) ||
            (start >= 0.98f && safeTarget < 0.98f)
        zoomAnimationJob?.cancel()
        if (appPrefs.getBoolean("haptic_feedback_enabled", true)) {
            zoomHapticMilestonesCrossed(start, safeTarget).forEach { milestone ->
                localView.performHapticFeedback(
                    if (milestone == 1f) HapticFeedbackConstants.CONTEXT_CLICK
                    else HapticFeedbackConstants.CLOCK_TICK
                )
            }
        }
        if (crossesLensBoundary) {
            lensTransitionLabel = if (safeTarget < 0.98f) "SIÊU RỘNG" else "CAM THƯỜNG"
            lensTransitionVisible = true
            scope.launch {
                delay(if (dispReducedMotion) 40L else 180L)
                lensTransitionVisible = false
            }
        }
        zoomAnimationJob = scope.launch {
            val animatedZoom = Animatable(start)
            animatedZoom.animateTo(
                targetValue = safeTarget,
                animationSpec = tween(durationMillis = 620, easing = FastOutSlowInEasing)
            ) {
                val plan = resolveZoomExecutionPlan(
                    requestedZoomRatio = value,
                    minimumCameraRatio = state.minZoomRatio,
                    cameraReportedMaximum = cameraReportedMaximum,
                    assessment = zoomAssessment
                )
                val currentCameraRatio = activeCamera.cameraInfo.zoomState.value?.zoomRatio ?: state.zoomRatio
                if (abs(currentCameraRatio - plan.cameraControlZoomRatio) > 0.002f) {
                    activeCamera.cameraControl.setZoomRatio(plan.cameraControlZoomRatio)
                }
                zoomSel = plan.effectiveZoomRatio
                actualZoomRatio = plan.effectiveZoomRatio
                digitalZoomCropScale = plan.digitalCropRatio
                zoomInteractionGeneration++
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
    fun selectProAfMode(nextMode: String) {
        val safeMode = nextMode.takeIf { it in setOf("AF-S", "AF-C", "MF") } ?: "AF-S"
        val mapped = camera2AfModeForProSelection(safeMode, availableAfModes)
        if (mapped == null) {
            android.util.Log.w(
                "FUJI_PRO_AF",
                "Unsupported AF mode=$safeMode cameraId=${cameraInfo2?.cameraId} available=${availableAfModes?.contentToString()}"
            )
            localizedToast(
                context,
                localizedUiText(context, "Chế độ lấy nét này không được camera hỗ trợ."),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        proAfMode = safeMode
        appPrefs.edit().putString("pro_af_mode", safeMode).apply()
        if (safeMode != "MF") {
            manualFocus = 0f
            milkyWayInfinityFocusEnabled = false
            invalidatePendingFocusLock(camera)
        }
        camera?.let { active ->
            runCatching {
                val options = CaptureRequestOptions.Builder()
                    .setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                    .setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, mapped)
                    .build()
                Camera2CameraControl.from(active.cameraControl).addCaptureRequestOptions(options)
            }.onFailure {
                android.util.Log.w("FUJI_PRO_AF", "Could not apply AF mode=$safeMode", it)
            }
        }
        android.util.Log.i("FUJI_PRO_AF", "AF mode=$safeMode camera2Mode=$mapped")
    }
    val manualInfinityFocusSupported = maxFocus >= 8f &&
        availableAfModes?.contains(CameraCharacteristics.CONTROL_AF_MODE_OFF) == true
    LaunchedEffect(camera, proAfMode, availableAfModes?.contentToString()) {
        val active = camera
        val mapped = camera2AfModeForProSelection(proAfMode, availableAfModes)
        if (active != null && mapped != null) {
            runCatching {
                val options = CaptureRequestOptions.Builder()
                    .setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                    .setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, mapped)
                    .build()
                Camera2CameraControl.from(active.cameraControl).addCaptureRequestOptions(options)
            }.onFailure {
                android.util.Log.w("FUJI_PRO_AF", "Could not restore persisted AF mode=$proAfMode", it)
            }
        }
    }
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
    val afText = when {
        focusLocked -> "AF-L"
        proAfMode == "MF" || manualFocus > 0f || milkyWayInfinityFocusEnabled -> "MF"
        proAfMode == "AF-C" -> "AF-C"
        mode == "Pro" -> "AF-S"
        else -> if (faceTrackingMode == "face" || faceTrackingMode == "face_eye") "AF-C" else "AF-S"
    }
    val mainEquivalentFocal = activeCameraDetails?.eq35Mm?.takeIf { it.isFinite() && it > 0f }
        ?: activePhysicalCameraDetails.mapNotNull { it.eq35Mm }.filter { it.isFinite() && it > 0f }.minOrNull()
    val focalLengthText = mainEquivalentFocal?.let { "${(it * zoomSel).roundToInt()}mm" } ?: "--mm"
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


    val orientationProgress by animateFloatAsState(
        targetValue = if (isLandscape) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 360f),
        label = "camera orientation transition"
    )

    val toolbarContent: @Composable (Modifier, Dp) -> Unit = { toolbarModifier, plateHeight ->
        FujiMetalTopPlate(toolbarModifier, height = plateHeight) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 1.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.fujifilm_logo),
                    contentDescription = null,
                    modifier = Modifier.width(76.dp).height(13.dp),
                    contentScale = ContentScale.Fit
                )
                FujiTopKey(label = "FLASH", icon = if (flash == ImageCapture.FLASH_MODE_ON) "flash_on" else "flash_off", selected = flash == ImageCapture.FLASH_MODE_ON, onClick = {
                    flash = if (flash == ImageCapture.FLASH_MODE_OFF) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
                })
                FujiTopKey(label = "DRIVE", icon = "timer", selected = timerSec > 0 || burstMode, onClick = {
                    timerSec = when (timerSec) { 0 -> 2; 2 -> 5; 5 -> 10; else -> 0 }
                }, onLongClick = {
                    if (mode == "Photo" && nightShotMode == "off" && lunaDreamMode == "off" && rawCaptureMode == "jpeg" && !uhdCaptureEnabled && !fhdCaptureEnabled && !uhdPlusCaptureEnabled) burstMode = !burstMode
                })
                FujiTopKey(label = "FILM", icon = "film", selected = filterDialogVisible || activeFilter != "None", onClick = { filterDialogVisible = true })
                FujiTopKey(label = "ADJ", icon = "sliders", selected = false, onClick = onOpenToolbox)
                FujiTopKey(label = "HISTO", icon = "histogram", selected = showHist, onClick = { onShowHist(!showHist) })
                FujiTopKey(label = "AF", icon = "af", selected = focusPeaking, onClick = { onFocusPeaking(!focusPeaking) })
                FujiTopKey(label = "Q", icon = "q", selected = showQuick, onClick = { showQuick = !showQuick })
            }
        }
    }

    val previewContent: @Composable (Modifier) -> Unit = { previewModifier ->
                    BoxWithConstraints(
                        previewModifier.background(FujiSkin.body).padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // The LCD is always landscape, but the live image frame follows the
                        // physical handset orientation so portrait capture is letterboxed centrally.
                        val selectedPortraitHeightOverWidth = aspectRatios[
                            ratioIdx.coerceIn(0, aspectRatios.lastIndex)
                        ].second
                        val frameWidthOverHeight = if (isLandscape) {
                            selectedPortraitHeightOverWidth
                        } else {
                            1f / selectedPortraitHeightOverWidth
                        }
                        val frameWidth = minOf(maxWidth, maxHeight * frameWidthOverHeight)
                        val frameHeight = frameWidth / frameWidthOverHeight
                        Box(
                            Modifier
                                .width(frameWidth)
                                .height(frameHeight)
                                .clip(RoundedCornerShape(6.dp))
                                .background(FujiSkin.lcd)
                                .border(3.dp, Color(0xFF050607), RoundedCornerShape(6.dp))
                                .border(1.dp, FujiSkin.silver.copy(alpha = 0.60f), RoundedCornerShape(6.dp))
                                .graphicsLayer { alpha = previewOrientationAlpha.value }
                                .pointerInput(camera, mode) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent(PointerEventPass.Initial)
                                            val down = event.changes.firstOrNull {
                                                it.pressed && !it.previousPressed
                                            }
                                            if (down != null) {
                                                android.util.Log.d(
                                                    "FUJI_AF_TOUCH",
                                                    "TOUCH_DISPATCH pass=Initial received=true " +
                                                        "screenPx=${down.position.x},${down.position.y}"
                                                )
                                            }
                                        }
                                    }
                                }
                                .pointerInput(camera, metering, mode) {
                                    detectTapGestures(
                                        onTap = { offset ->
                                            if (lcdDisplayMode == CameraLcdDisplayMode.CLEAN) {
                                                lcdDisplayMode = CameraLcdDisplayMode.STANDARD
                                                return@detectTapGestures
                                            }
                                            val active = camera ?: return@detectTapGestures
                                            val tapPoint = normalizedAfTapPoint(
                                                Offset(offset.x, offset.y),
                                                size.width,
                                                size.height
                                            )
                                            android.util.Log.i(
                                                "FUJI_AF_TOUCH",
                                                "TOUCH_DISPATCH received=true pass=Main screenPx=${offset.x},${offset.y} " +
                                                    "container=${size.width}x${size.height} " +
                                                    "preview=${previewView.width}x${previewView.height} " +
                                                    "tap=${tapPoint.x},${tapPoint.y} focusLocked=$focusLocked"
                                            )
                                            if (focusLocked) {
                                                invalidatePendingFocusLock(active)
                                                runCatching { active.cameraControl.cancelFocusAndMetering() }
                                                val aeUnlocked = applyCameraAeLock(active, locked = false)
                                                manualAfOverrideUntilMs = 0L
                                                centerAfStatus = 0
                                                android.util.Log.i(
                                                    "FUJI_AF",
                                                    "AF_LOCK_TOGGLE unlocked=true aeUnlockApplied=$aeUnlocked " +
                                                        "point=${tapPoint.x},${tapPoint.y}"
                                                )
                                                return@detectTapGestures
                                            }
                                            if (mode != "Pro" || !touchAfEnabled) {
                                                android.util.Log.d("FUJI_AF_TOUCH", "TOUCH_AF_IGNORED mode=$mode enabled=$touchAfEnabled tap=${tapPoint.x},${tapPoint.y}")
                                                return@detectTapGestures
                                            }
                                            // TOUCH AF is explicit in Pro; the default Photo tap is inert.
                                            invalidatePendingFocusLock(active)
                                            manualAfOverrideUntilMs = manualAfOverrideDeadline(SystemClock.elapsedRealtime())
                                            android.util.Log.d(
                                                "FUJI_AF_OSD",
                                                "manualTapOverride=true faceBoxPresent=${faceTrackCenter != null} " +
                                                    "point=${tapPoint.x},${tapPoint.y} " +
                                                    "preview=${previewView.width}x${previewView.height} " +
                                                    "cameraId=${activeCameraDetails?.id ?: "unknown"}"
                                            )
                                            val leftEye = faceLeftEyeCenter
                                            val rightEye = faceRightEyeCenter
                                            val leftEyeDistance = leftEye?.let {
                                                kotlin.math.abs(it.x - tapPoint.x) + kotlin.math.abs(it.y - tapPoint.y)
                                            } ?: Float.POSITIVE_INFINITY
                                            val rightEyeDistance = rightEye?.let {
                                                kotlin.math.abs(it.x - tapPoint.x) + kotlin.math.abs(it.y - tapPoint.y)
                                            } ?: Float.POSITIVE_INFINITY
                                            if (faceTrackingMode == "face_eye") {
                                                when {
                                                    leftEyeDistance <= 0.12f && leftEyeDistance <= rightEyeDistance -> {
                                                        primaryEyeSelector.forceSelect(PrimaryEyeSide.LEFT, SystemClock.elapsedRealtime())
                                                        faceEyeCenter = leftEye
                                                        android.util.Log.d("FUJI_FACE_TRACK", "User selected left eye by tap")
                                                    }
                                                    rightEyeDistance <= 0.12f -> {
                                                        primaryEyeSelector.forceSelect(PrimaryEyeSide.RIGHT, SystemClock.elapsedRealtime())
                                                        faceEyeCenter = rightEye
                                                        android.util.Log.d("FUJI_FACE_TRACK", "User selected right eye by tap")
                                                    }
                                                }
                                            }
                                            centerAfPoint = tapPoint
                                            preferredFaceCenter = digitalPreviewPointToCamera(tapPoint, digitalZoomCropScale)
                                            centerAfStatus = 0
                                            val focusRequestId = focusLockRequestGeneration
                                            if (manualFocus > 0f || milkyWayInfinityFocusEnabled) {
                                                manualFocus = 0f
                                                milkyWayInfinityFocusEnabled = false
                                                runCatching { applyAutoFocus(active) }
                                            }
                                            val afPointSize = CameraAfAreaSize.fromPreference(
                                                appPrefs.getString("af_area_size", "m")
                                            ).focusPointSize
                                            val cameraTapPoint = digitalPreviewPointToCamera(tapPoint, digitalZoomCropScale)
                                            val point = previewView.meteringPointFactory.createPoint(
                                                cameraTapPoint.x * previewView.width,
                                                cameraTapPoint.y * previewView.height,
                                                afPointSize
                                            )
                                            android.util.Log.d(
                                                "FUJI_AF_TRACE",
                                                "tapScreenPx=${offset.x},${offset.y} " +
                                                    "normalized=${tapPoint.x},${tapPoint.y} " +
                                                    "cameraNormalized=${cameraTapPoint.x},${cameraTapPoint.y} " +
                                                    "digitalCrop=$digitalZoomCropScale " +
                                                    "meteringPoint=${point.x},${point.y} size=$afPointSize"
                                            )
                                            val camera2Info = runCatching {
                                                Camera2CameraInfo.from(active.cameraInfo)
                                            }.getOrNull()
                                            val maxAfRegions = camera2Info?.getCameraCharacteristic(
                                                CameraCharacteristics.CONTROL_MAX_REGIONS_AF
                                            ) ?: 0
                                            val maxAeRegions = camera2Info?.getCameraCharacteristic(
                                                CameraCharacteristics.CONTROL_MAX_REGIONS_AE
                                            ) ?: 0
                                            android.util.Log.i(
                                                "FUJI_AF_TRACE",
                                                "REGIONS maxRegionsAF=$maxAfRegions maxRegionsAE=$maxAeRegions " +
                                                    "cameraId=${camera2Info?.cameraId} " +
                                                    "sensorOrientation=${camera2Info?.getCameraCharacteristic(CameraCharacteristics.SENSOR_ORIENTATION)} " +
                                                    "lensFacing=${camera2Info?.getCameraCharacteristic(CameraCharacteristics.LENS_FACING)} " +
                                                    "display=${tapPoint.x},${tapPoint.y} " +
                                                    "cameraNormalized=${cameraTapPoint.x},${cameraTapPoint.y} " +
                                                    "meteringPoint=${point.x},${point.y}"
                                            )
                                            if (maxAfRegions <= 0) {
                                                applyAutoFocus(active)
                                                val triggerSent = triggerCamera2AfOnce(active)
                                                if (maxAeRegions > 0) {
                                                    val aeAction = FocusMeteringAction.Builder(
                                                        point,
                                                        FocusMeteringAction.FLAG_AE
                                                    ).disableAutoCancel().build()
                                                    val aeFuture = runCatching {
                                                        active.cameraControl.startFocusAndMetering(aeAction)
                                                    }.getOrNull()
                                                    if (aeFuture != null) {
                                                        aeFuture.addListener({
                                                            if (focusLockRequestGeneration != focusRequestId ||
                                                                camera !== active || manualAfOverrideUntilMs != Long.MAX_VALUE
                                                            ) return@addListener
                                                            val aeLockApplied = applyCameraAeLock(active, locked = true)
                                                            focusLocked = aeLockApplied
                                                            centerAfStatus = if (aeLockApplied) 1 else -1
                                                            if (!aeLockApplied) {
                                                                manualAfOverrideUntilMs = SystemClock.elapsedRealtime() + 700L
                                                            }
                                                            android.util.Log.w(
                                                                "FUJI_AF",
                                                                "AF_NO_REGIONS maxRegionsAF=0 triggerSent=$triggerSent " +
                                                                    "aeMeteringCompleted=true aeLockApplied=$aeLockApplied " +
                                                                    "AF_STATE=NOT_EXPOSED_BY_CAMERAX"
                                                            )
                                                        }, mainExecutor)
                                                        return@detectTapGestures
                                                    }
                                                }
                                                val aeLockApplied = applyCameraAeLock(active, locked = true)
                                                focusLocked = aeLockApplied
                                                centerAfStatus = if (aeLockApplied) 1 else -1
                                                if (!aeLockApplied) {
                                                    manualAfOverrideUntilMs = SystemClock.elapsedRealtime() + 700L
                                                }
                                                android.util.Log.w(
                                                    "FUJI_AF",
                                                    "AF_NO_REGIONS maxRegionsAF=0 triggerSent=$triggerSent " +
                                                        "aeLockApplied=$aeLockApplied AF_STATE=NOT_EXPOSED_BY_CAMERAX"
                                                )
                                                return@detectTapGestures
                                            }
                                            var meteringFlags = FocusMeteringAction.FLAG_AF
                                            if (maxAeRegions > 0) {
                                                meteringFlags = meteringFlags or FocusMeteringAction.FLAG_AE
                                            }
                                            val action = FocusMeteringAction.Builder(
                                                point, meteringFlags
                                            ).disableAutoCancel().build()
                                            val focusFuture = runCatching {
                                                active.cameraControl.startFocusAndMetering(action)
                                            }.getOrElse { failure ->
                                                val triggerSent = triggerCamera2AfOnce(active)
                                                val aeLockApplied = applyCameraAeLock(active, locked = true)
                                                focusLocked = aeLockApplied
                                                centerAfStatus = if (aeLockApplied) 1 else -1
                                                if (!aeLockApplied) {
                                                    manualAfOverrideUntilMs = SystemClock.elapsedRealtime() + 700L
                                                }
                                                android.util.Log.w(
                                                    "FUJI_AF",
                                                    "AF_CAMERA2_FALLBACK triggerSent=$triggerSent aeLockApplied=$aeLockApplied",
                                                    failure
                                                )
                                                scope.launch(Dispatchers.Main) {
                                                    withFrameNanos { }
                                                    withFrameNanos { }
                                                    if (focusLockRequestGeneration == focusRequestId &&
                                                        centerAfStatus == -1
                                                    ) {
                                                        centerAfStatus = 0
                                                    }
                                                }
                                                return@detectTapGestures
                                            }
                                            focusFuture.addListener({
                                                if (!shouldAcceptFocusRequestResult(
                                                        requestGeneration = focusRequestId,
                                                        currentGeneration = focusLockRequestGeneration,
                                                        requestedCenter = tapPoint,
                                                        currentCenter = centerAfPoint,
                                                        sameCamera = camera === active,
                                                        requestStillActive = manualAfOverrideUntilMs > SystemClock.elapsedRealtime()
                                                    )
                                                ) {
                                                    return@addListener
                                                }
                                                val focusSuccess = runCatching {
                                                    focusFuture.get().isFocusSuccessful
                                                }.getOrDefault(false)
                                                // A normal tap selects and focuses the AF point; it does not lock AF/AE.
                                                // AF-L/AE-L is reserved for the 500 ms long-press gesture below.
                                                focusLocked = false
                                                centerAfStatus = if (focusSuccess) 1 else -1
                                                android.util.Log.i(
                                                    "FUJI_AF",
                                                    "AF_STATE=" + (if (focusSuccess) "FOCUSED" else "NOT_FOCUSED") +
                                                        " AF_LOCK=false AE_LOCK=false request=$focusRequestId"
                                                )
                                                if (!focusSuccess) {
                                                    scope.launch(Dispatchers.Main) {
                                                        withFrameNanos { }
                                                        withFrameNanos { }
                                                        if (focusLockRequestGeneration == focusRequestId &&
                                                            centerAfStatus == -1 && !focusLocked
                                                        ) {
                                                            centerAfStatus = 0
                                                        }
                                                    }
                                                }
                                            }, mainExecutor)
                                        },
                                        onLongPress = { offset ->
                                            if (mode != "Pro" || !touchAfEnabled) return@detectTapGestures
                                            val active = camera ?: return@detectTapGestures
                                            val tapPoint = normalizedAfTapPoint(
                                                Offset(offset.x, offset.y),
                                                size.width,
                                                size.height
                                            )
                                            val previewWidth = previewView.width
                                            val previewHeight = previewView.height
                                            if (previewWidth <= 0 || previewHeight <= 0) {
                                                android.util.Log.w("FUJI_AF", "AF_LONG_PRESS rejected: preview has no size")
                                                return@detectTapGestures
                                            }

                                            invalidatePendingFocusLock(active)
                                            centerAfPoint = tapPoint
                                            preferredFaceCenter = digitalPreviewPointToCamera(tapPoint, digitalZoomCropScale)
                                            centerAfStatus = 0
                                            manualAfOverrideUntilMs = manualAfOverrideDeadline(SystemClock.elapsedRealtime())

                                            val cameraPoint = digitalPreviewPointToCamera(tapPoint, digitalZoomCropScale)
                                            val pointSize = CameraAfAreaSize.fromPreference(
                                                appPrefs.getString("af_area_size", "m")
                                            ).focusPointSize
                                            val meteringPoint = previewView.meteringPointFactory.createPoint(
                                                cameraPoint.x * previewWidth,
                                                cameraPoint.y * previewHeight,
                                                pointSize
                                            )
                                            val camera2Info = runCatching {
                                                Camera2CameraInfo.from(active.cameraInfo)
                                            }.getOrNull()
                                            val maxAfRegions = camera2Info?.getCameraCharacteristic(
                                                CameraCharacteristics.CONTROL_MAX_REGIONS_AF
                                            ) ?: 0
                                            val maxAeRegions = camera2Info?.getCameraCharacteristic(
                                                CameraCharacteristics.CONTROL_MAX_REGIONS_AE
                                            ) ?: 0
                                            val flags = (if (maxAfRegions > 0) FocusMeteringAction.FLAG_AF else 0) or
                                                (if (maxAeRegions > 0) FocusMeteringAction.FLAG_AE else 0)
                                            val requestId = focusLockRequestGeneration
                                            android.util.Log.i(
                                                "FUJI_AF",
                                                "AF_LONG_PRESS screenPx=" + offset.x + "," + offset.y +
                                                    " normalized=" + tapPoint.x + "," + tapPoint.y +
                                                    " cameraNormalized=" + cameraPoint.x + "," + cameraPoint.y +
                                                    " maxAF=" + maxAfRegions + " maxAE=" + maxAeRegions + " flags=" + flags
                                            )

                                            if (maxAfRegions <= 0) {
                                                val triggerSent = triggerCamera2AfOnce(active)
                                                if (maxAeRegions <= 0) {
                                                    focusLocked = triggerSent
                                                    centerAfStatus = if (triggerSent) 1 else -1
                                                    android.util.Log.i(
                                                        "FUJI_AF",
                                                        "AF_LONG_PRESS sensorTrigger=" + triggerSent + " AE_LOCK_UNAVAILABLE=true"
                                                    )
                                                    return@detectTapGestures
                                                }
                                            }
                                            val action = FocusMeteringAction.Builder(meteringPoint, flags)
                                                .disableAutoCancel()
                                                .build()
                                            val future = runCatching {
                                                active.cameraControl.startFocusAndMetering(action)
                                            }.getOrNull()
                                            if (future == null) {
                                                centerAfStatus = -1
                                                android.util.Log.w("FUJI_AF", "AF_LONG_PRESS metering request rejected")
                                                return@detectTapGestures
                                            }
                                            future.addListener({
                                                if (focusLockRequestGeneration != requestId || camera !== active) {
                                                    return@addListener
                                                }
                                                val focusSuccess = runCatching {
                                                    future.get().isFocusSuccessful
                                                }.getOrDefault(false)
                                                val aeLockApplied = focusSuccess && applyCameraAeLock(active, locked = true)
                                                focusLocked = focusSuccess
                                                centerAfStatus = if (focusSuccess) 1 else -1
                                                android.util.Log.i(
                                                    "FUJI_AF",
                                                    "AF_LONG_PRESS_RESULT focusSuccess=" + focusSuccess +
                                                        " aeLockApplied=" + aeLockApplied +
                                                        " AF_LOCK=" + focusSuccess + " AE_LOCK=" + aeLockApplied +
                                                        " point=" + tapPoint.x + "," + tapPoint.y
                                                )
                                                if (!focusSuccess) {
                                                    scope.launch(Dispatchers.Main) {
                                                        withFrameNanos { }
                                                        withFrameNanos { }
                                                        if (focusLockRequestGeneration == requestId &&
                                                            centerAfStatus == -1 && !focusLocked
                                                        ) centerAfStatus = 0
                                                    }
                                                }
                                            }, mainExecutor)
                                        },
                                        onDoubleTap = {
                                            val active = camera ?: return@detectTapGestures
                                            invalidatePendingFocusLock(active)
                                            runCatching { active.cameraControl.cancelFocusAndMetering() }
                                            val aeUnlocked = applyCameraAeLock(active, locked = false)
                                            val center = Offset(0.5f, 0.5f)
                                            centerAfPoint = center
                                            preferredFaceCenter = digitalPreviewPointToCamera(center, digitalZoomCropScale)
                                            centerAfStatus = 0
                                            focusLocked = false
                                            manualAfOverrideUntilMs = SystemClock.elapsedRealtime() + 700L
                                            applyAutoFocus(active)
                                            val previewWidth = previewView.width
                                            val previewHeight = previewView.height
                                            val cameraInfo = runCatching {
                                                Camera2CameraInfo.from(active.cameraInfo)
                                            }.getOrNull()
                                            val maxAfRegions = cameraInfo?.getCameraCharacteristic(
                                                CameraCharacteristics.CONTROL_MAX_REGIONS_AF
                                            ) ?: 0
                                            if (previewWidth > 0 && previewHeight > 0 && maxAfRegions > 0) {
                                                val point = previewView.meteringPointFactory.createPoint(
                                                    previewWidth * 0.5f,
                                                    previewHeight * 0.5f,
                                                    CameraAfAreaSize.fromPreference(
                                                        appPrefs.getString("af_area_size", "m")
                                                    ).focusPointSize
                                                )
                                                val action = FocusMeteringAction.Builder(
                                                    point,
                                                    FocusMeteringAction.FLAG_AF
                                                ).disableAutoCancel().build()
                                                val future = runCatching {
                                                    active.cameraControl.startFocusAndMetering(action)
                                                }.getOrNull()
                                                future?.addListener({
                                                    val success = runCatching { future.get().isFocusSuccessful }.getOrDefault(false)
                                                    if (camera === active && !focusLocked) {
                                                        centerAfStatus = if (success) 1 else -1
                                                        android.util.Log.i(
                                                            "FUJI_AF",
                                                            "DOUBLE_TAP_CENTRE_AF success=$success unlocked=true"
                                                        )
                                                    }
                                                }, mainExecutor)
                                            } else {
                                                triggerCamera2AfOnce(active)
                                            }
                                            android.util.Log.i(
                                                "FUJI_AF",
                                                "DOUBLE_TAP_RESET center=0.5,0.5 unlocked=true " +
                                                    "aeUnlockApplied=$aeUnlocked preview=${previewWidth}x${previewHeight} " +
                                                    "maxRegionsAF=$maxAfRegions"
                                            )
                                        }
                                    )
                                }
                        ) {
                            key(renderEngine) {
                                AndroidView(
                                    factory = { previewView },
                                    modifier = Modifier.fillMaxSize().onGloballyPositioned { coordinates ->
                                        val origin = coordinates.positionInRoot()
                                        previewBoundsText = "${origin.x.roundToInt()},${origin.y.roundToInt()},${(origin.x + coordinates.size.width).roundToInt()},${(origin.y + coordinates.size.height).roundToInt()}"
                                        val renderMethod = digitalZoomPreviewEffect.apply(
                                            previewView,
                                            digitalZoomCropScale.takeIf { it.isFinite() && it >= 1f } ?: 1f
                                        )
                                        val matrixValues = FloatArray(9)
                                        previewView.matrix.getValues(matrixValues)
                                        previewTransformText = String.format(
                                            Locale.US,
                                            "render=%s view=%dx%d scale=(%.4f,%.4f) translation=(%.2f,%.2f) matrix=[%.4f,%.4f,%.4f;%.4f,%.4f,%.4f;%.4f,%.4f,%.4f]",
                                            renderMethod,
                                            previewView.width,
                                            previewView.height,
                                            previewView.scaleX,
                                            previewView.scaleY,
                                            previewView.translationX,
                                            previewView.translationY,
                                            matrixValues[0], matrixValues[1], matrixValues[2],
                                            matrixValues[3], matrixValues[4], matrixValues[5],
                                            matrixValues[6], matrixValues[7], matrixValues[8]
                                        )
                                    }
                                )
                            }
                            frozenPreviewBitmap?.let { frozen ->
                                if (frozenPreviewAlpha > 0.001f) {
                                    Image(
                                        bitmap = frozen,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize().graphicsLayer { alpha = frozenPreviewAlpha },
                                        contentScale = ContentScale.FillBounds
                                    )
                                }
                            }
                            // LCD scanline texture is an optional display-only overlay. It is
                            // drawn above PreviewView and never enters ImageCapture or saved pixels.
                            if (lcdDisplayMode != CameraLcdDisplayMode.CLEAN &&
                                appPrefs.getBoolean("lcd_texture_overlay_enabled", false) &&
                                !appPrefs.getBoolean("simple_ui_mode_enabled", false)
                            ) {
                                Canvas(Modifier.fillMaxSize()) {
                                    val spacing = 4.dp.toPx()
                                    var scanY = 0f
                                    while (scanY < size.height) {
                                        drawLine(
                                            Color.White.copy(alpha = 0.018f),
                                            Offset(0f, scanY),
                                            Offset(size.width, scanY),
                                            strokeWidth = 0.5.dp.toPx()
                                        )
                                        scanY += spacing
                                    }
                                }
                            }
                            if (shutterEffectProgress.value > 0f) {
                                Canvas(Modifier.fillMaxSize()) {
                                    val curtainHeight = size.height * shutterEffectProgress.value.coerceIn(0f, 1f)
                                    drawRect(Color.Black, Offset(0f, 0f), DrawSize(size.width, curtainHeight))
                                }
                            }
                            val manualAfReticleOverrideActive =
                                manualAfOverrideUntilMs > SystemClock.elapsedRealtime()
                            PixelOsdLayer(
                                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = dispOsdAlpha.value },
                                frame = PixelOsdFrame(
                                    showPixelTexture = lcdDisplayMode != CameraLcdDisplayMode.CLEAN,
                                    gridMode = gridOverlayMode,
                                    gridColor = gridOverlayColor,
                                    gridOpacity = gridOverlayOpacity,
                                    showGrid = when (lcdDisplayMode) {
                                        CameraLcdDisplayMode.FULL_INFO,
                                        CameraLcdDisplayMode.LEVEL_GRID -> true
                                        CameraLcdDisplayMode.CLEAN -> false
                                        CameraLcdDisplayMode.STANDARD -> showGrid
                                    },
                                    faceCenter = faceTrackCenter.takeIf {
                                        faceTrackingEnabled &&
                                            !manualAfReticleOverrideActive &&
                                            manualFocus <= 0f &&
                                            !(mode == "Pro" && proAfMode == "MF") &&
                                            !milkyWayInfinityFocusEnabled &&
                                            lcdDisplayMode != CameraLcdDisplayMode.CLEAN &&
                                            lcdDisplayMode != CameraLcdDisplayMode.LEVEL_GRID
                                    },
                                    faceSize = faceTrackSize.takeIf {
                                        !manualAfReticleOverrideActive &&
                                            manualFocus <= 0f && !milkyWayInfinityFocusEnabled
                                    },
                                    eyeCenter = faceEyeCenter.takeIf {
                                        !manualAfReticleOverrideActive &&
                                            manualFocus <= 0f && !milkyWayInfinityFocusEnabled
                                    },
                                    faceFocused = centerAfStatus == 1 &&
                                        !manualAfReticleOverrideActive &&
                                        manualFocus <= 0f && !milkyWayInfinityFocusEnabled,
                                    focusLocked = focusLocked && lcdDisplayMode != CameraLcdDisplayMode.CLEAN &&
                                        lcdDisplayMode != CameraLcdDisplayMode.LEVEL_GRID,
                                    centerAfPoint = centerAfPoint.takeIf { lcdDisplayMode != CameraLcdDisplayMode.CLEAN },
                                    centerAfStatus = if (focusLocked) 1 else centerAfStatus,
                                    showManualAfReticle = manualAfReticleOverrideActive,
                                    centerAfScale = CameraAfAreaSize.fromPreference(
                                        appPrefs.getString("af_area_size", "m")
                                    ).pixelScale,
                                    manualFocusMode = (mode == "Pro" && proAfMode == "MF") ||
                                        manualFocus > 0f || milkyWayInfinityFocusEnabled,
                                    focusPoints = when (lcdDisplayMode) {
                                        CameraLcdDisplayMode.FULL_INFO -> peakPoints
                                        CameraLcdDisplayMode.STANDARD -> if (focusPeaking) peakPoints else emptyList()
                                        else -> emptyList()
                                    },
                                    showLevel = when (lcdDisplayMode) {
                                        CameraLcdDisplayMode.FULL_INFO,
                                        CameraLcdDisplayMode.LEVEL_GRID -> true
                                        CameraLcdDisplayMode.STANDARD -> levelEnabled
                                        CameraLcdDisplayMode.CLEAN -> false
                                    },
                                    horizonRollDegrees = horizonRollDegrees,
                                    showZebra = when (lcdDisplayMode) {
                                        CameraLcdDisplayMode.FULL_INFO -> true
                                        CameraLcdDisplayMode.STANDARD -> zebraEnabled
                                        else -> false
                                    },
                                    zebraPoints = zebraPoints,
                                    showStatus = lcdDisplayMode != CameraLcdDisplayMode.CLEAN,
                                    resolutionText = when (lunaDreamMode) {
                                        "luna20" -> "UHD"
                                        "luna15" -> "FHD+"
                                        else -> "FHD"
                                    },
                                    previewFps = measuredPreviewFps.takeIf { it.isFinite() && it > 0f },
                                    previewFpsTarget = previewFpsTarget.takeIf { it.isFinite() && it > 0f },
                                    batteryPercent = batteryPercent,
                                    batteryCharging = batteryCharging,
                                    batteryLowBlinkOn = batteryLowBlinkOn,
                                    batteryLowRed = batteryWarningState.lowRed,
                                    batteryBlinking = batteryWarningState.blinking,
                                    batteryCrossed = batteryWarningState.crossed,
                                    showBatteryLowNotice = batteryWarningState.showLowNotice,
                                    showEvScale = lcdDisplayMode != CameraLcdDisplayMode.CLEAN,
                                    evIndex = evIndex,
                                    evMinimumIndex = evRange?.lower ?: -9,
                                    evMaximumIndex = evRange?.upper ?: 9,
                                    evStepStops = evStep.takeIf { it > 0f } ?: (1f / 3f),
                                    evDisplayIndex = evDisplayIndex,
                                    showHistogram = showHist && lcdDisplayMode != CameraLcdDisplayMode.CLEAN,
                                    histogram = hist.toList(),
                                    isPro = lcdDisplayMode.showsProHud(mode == "Pro"),
                                    afModeText = if (mode == "Pro") {
                                        proAfMode
                                    } else if (faceTrackingMode == "face_eye" || faceTrackingMode == "face") {
                                        "AF-C"
                                    } else {
                                        "AF-S"
                                    },
                                    meteringText = when (metering) { 1 -> "M"; 2 -> "S"; else -> "C" },
                                    shutterText = shutterText,
                                    apertureText = (apertureValue.takeIf { it > 0f }
                                        ?: activeCameraDetails?.apertures?.minOrNull())
                                        ?.let { "F%.1f".format(Locale.US, it) } ?: "F--",
                                    isoText = isoText,
                                    dynamicRangeText = proDynamicRangeLabel(
                                        hdrEnabled = hdrEnabled,
                                        isPhotoMode = mode == "Photo",
                                        nightShotEnabled = nightShotMode != "off",
                                        rawCaptureActive = imageCapture?.outputFormat == ImageCapture.OUTPUT_FORMAT_RAW ||
                                            imageCapture?.outputFormat == ImageCapture.OUTPUT_FORMAT_RAW_JPEG,
                                        manualExposure = manualExposure,
                                        manualIsoEnabled = manualIsoEnabled,
                                        manualShutterEnabled = manualShutterEnabled,
                                        nativeImageProcessingEnabled = nativeImageProcessingEnabled,
                                        livePhotoEnabled = livePhotoEnabled
                                    ),
                                    whiteBalanceText = toneText,
                                    filmSimulationText = activeFilter.takeIf { it != "None" }?.take(2)?.uppercase(Locale.US) ?: "",
                                    outputSizeLabel = if (lunaDreamMode == "luna20" || lunaDreamMode == "luna15") "L" else "M",
                                    whiteBalanceKelvinText = if (manualColorTempEnabled) "${manualColorTemp}K" else "----K",
                                    touchAfEnabled = touchAfEnabled,
                                    manualExposure = manualExposure,
                                    manualWhiteBalance = manualColorTempEnabled,
                                    manualIso = manualIsoEnabled,
                                    manualShutter = manualShutterEnabled,
                                    adjustmentPanelVisible = proSel != null && mode == "Pro",
                                    selectedControl = when (proSel?.uppercase(Locale.US)) {
                                         "TV" -> "TV"
                                         "TONE" -> "WB"
                                         "FL" -> "FL"
                                         else -> proSel?.uppercase(Locale.US).orEmpty()
                                     },
                                    selectedControlProgress = if (proSel?.uppercase(Locale.US) == "FL") {
                                         val zoomState = camera?.cameraInfo?.zoomState?.value
                                         val minimumZoom = zoomState?.minZoomRatio ?: 1f
                                         val reportedMaximum = zoomState?.let {
                                             Camera2Info.cameraReportedZoomMax(activeCameraDetails, it.maxZoomRatio)
                                         } ?: 2f
                                         val assessment = Camera2Info.opticalZoomAssessment(
                                             activeCameraDetails,
                                             activePhysicalCameraDetails,
                                             camera2Inventory
                                         )
                                         val maximumZoom = maxTotalSelectableZoom(reportedMaximum, assessment)
                                             .coerceAtLeast(minimumZoom)
                                         logarithmicZoomPosition(zoomSel, minimumZoom, maximumZoom)
                                     } else cameraControlProgress(
                                         selectedControl = when (proSel?.uppercase(Locale.US)) {
                                             "TV" -> "TV"
                                             "TONE" -> "WB"
                                             else -> proSel?.uppercase(Locale.US).orEmpty()
                                         },
                                        evIndex = evDisplayIndex,
                                        evMinimumIndex = evRange?.lower ?: -9,
                                        evMaximumIndex = evRange?.upper ?: 9,
                                        shutterTimeNanos = shutterNs,
                                        isoValue = iso,
                                        manualFocusPosition = if (maxFocus > 0f) (manualFocus / maxFocus).coerceIn(0f, 1f) else 0.5f,
                                        whiteBalanceKelvin = manualColorTemp,
                                        manualFocusEnabled = manualFocus > 0f,
                                        manualWhiteBalanceEnabled = manualColorTempEnabled
                                    ),
                                    showProEv = proHudShowEv,
                                    showProShutter = proHudShowShutter,
                                    showProIso = proHudShowIso,
                                    showProFocus = proHudShowFocus,
                                    showProWhiteBalance = proHudShowWhiteBalance,
                                    showZoomBar = zoomOverlayVisible || zoomOverlayAlpha > 0.001f,
                                    zoomRatio = zoomDisplayValue,
                                    zoomMinimumRatio = camera?.cameraInfo?.zoomState?.value?.minZoomRatio ?: 1f,
                                    zoomMaximumRatio = camera?.cameraInfo?.zoomState?.value?.let { state ->
                                        val reportedMax = Camera2Info.cameraReportedZoomMax(activeCameraDetails, state.maxZoomRatio)
                                        maxTotalSelectableZoom(
                                            reportedMax,
                                            Camera2Info.opticalZoomAssessment(activeCameraDetails, activePhysicalCameraDetails, camera2Inventory)
                                        )
                                    } ?: 2f,
                                    zoomOpticalBoundaryRatio = camera?.cameraInfo?.zoomState?.value?.let { state ->
                                        opticalZoomBoundaryRatio(
                                            Camera2Info.opticalZoomAssessment(
                                                activeCameraDetails,
                                                activePhysicalCameraDetails,
                                                camera2Inventory
                                            ),
                                            state.minZoomRatio
                                        )
                                    } ?: 1f,
                                    zoomDigitalBoundaryRatio = camera?.cameraInfo?.zoomState?.value?.let { state ->
                                        Camera2Info.maxSelectableZoom(activeCameraDetails, activePhysicalCameraDetails, state.maxZoomRatio, camera2Inventory)
                                    } ?: 2f,
                                    zoomBarPosition = zoomBarPosition,
                                    zoomBarAlpha = zoomOverlayAlpha,
                                    pixelScaleMode = appPrefs.getString("osd_pixel_mode", "fine") ?: "fine"
                                ),
                                maxRefreshHz = when {
                                    appPrefs.getBoolean("simple_ui_mode_enabled", false) -> 30
                                    performanceMode -> 60
                                    else -> 120
                                },
                                performanceMode = performanceMode,
                                reducedMotion = dispReducedMotion
                            )
                            if (mode == "Pro" && proSel != null) {
                                val selectedPanel = proAdjustmentContent
                                Box(
                                    Modifier.align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .height((frameHeight / 48f * 6f).coerceAtLeast(52.dp))
                                        .background(Color(0xE00B0B0B))
                                        .border(1.dp, Color(0xFF737B7D), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    selectedPanel?.invoke(Modifier.fillMaxSize())
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

                            if (showMilkyWayAdvisor) {
                                Column(
                                    Modifier.align(Alignment.TopCenter).padding(top = 35.dp, start = 8.dp, end = 8.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xD91A1D24))
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        "MILKY WAY INTELLIGENCE · CẢNH TỐI",
                                        color = FujiSkin.orange,
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
                                            color = FujiSkin.orange,
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
                                            color = FujiSkin.orange,
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
                                                                invalidatePendingFocusLock(camera)
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

                            LensTransitionOverlay(
                                visible = lensTransitionVisible,
                                lensLabel = lensTransitionLabel,
                                performanceMode = performanceMode,
                                reducedMotion = dispReducedMotion,
                                modifier = Modifier.align(Alignment.Center)
                            )
                            // Histogram, battery, and AF state are rendered in the low-resolution pixel LCD layer.
                            if (!hasCam) {
                                Text(
                                    "NO SIGNAL\nCần cấp quyền Camera (chạm để cấp)",
                                    color = Color.White, textAlign = TextAlign.Center,
                                    modifier = Modifier.align(Alignment.Center)
                                        .animaTClickable { permissionLauncher.launch(Manifest.permission.CAMERA) }
                                )
                            }
                            val idleLunaEffect = lunaDreamMode != "off" && !isProcessing
                            if ((previewFreezeVisible && previewFreezeEffect != "off") || idleLunaEffect) {
                                val effectMode = if (previewFreezeVisible) previewFreezeEffect else "luna"
                                val overlayAlpha = if (previewFreezeVisible) previewFreezeAlpha.value else 0.35f
                                val position = previewEffectPosition.value
                                val passSeconds = if (effectMode == "night") 6.5f else 8.5f
                                val timeSeconds = if (idleLunaEffect) {
                                    lunaActivationProgress.value * 6.6667f
                                } else {
                                    ((position + 0.35f) / 1.70f).coerceIn(0f, 1f) * passSeconds
                                }
                                Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = overlayAlpha }) {
                                    CameraPreviewProcessingEffect.draw(
                                        canvas = drawContext.canvas.nativeCanvas,
                                        width = size.width,
                                        height = size.height,
                                        mode = effectMode,
                                        timeSeconds = timeSeconds,
                                        progressPercent = if (idleLunaEffect) 0 else processingProgress,
                                        reducedMotion = dispReducedMotion,
                                        simpleMode = appPrefs.getBoolean("simple_ui_mode_enabled", false),
                                        holdSteady = multiFrameHoldActive || nightShotCaptureInProgress,
                                        idleLunaBorder = idleLunaEffect,
                                        activationProgress = lunaActivationProgress.value,
                                        completionBloom = previewEffectBloom,
                                        density = density
                                    )
                                }
                                if (previewFreezeVisible) {
                                    Box(
                                        Modifier.fillMaxSize().clickable(
                                            interactionSource = previewFreezeBlockInteraction,
                                            indication = null
                                        ) {}
                                    )
                                }
                            }
                            if (isProcessing && previewFreezeEffect != "off") {
                                val nightMode = previewFreezeEffect == "night"
                                val modeLabel = when {
                                    nightMode -> stringResource(R.string.mf_mode_night)
                                    lunaDreamMode == "luna15" -> stringResource(R.string.mf_mode_ld15)
                                    else -> stringResource(R.string.mf_mode_ld20)
                                }
                                val isCapturingMultiFrame = multiFrameHoldActive || nightShotCaptureInProgress
                                val phase = resolveMultiFrameStage(
                                    processingLabel = processingLabel,
                                    progressPercent = processingProgress,
                                    capturing = isCapturingMultiFrame
                                )
                                val labelLower = processingLabel.lowercase(Locale.ROOT)
                                val hasError = listOf("fail", "lỗi", "không thể", "incomplete")
                                    .any(labelLower::contains)
                                val stability = when {
                                    kotlin.math.abs(horizonRollDegrees) <= 0.5f -> 3
                                    kotlin.math.abs(horizonRollDegrees) <= 1.5f -> 2
                                    else -> 1
                                }
                                FujiMultiFrameProgressPill(
                                    state = MultiFrameProgressUiState(
                                        visible = true,
                                        modeLabel = modeLabel,
                                        stage = phase,
                                        progressPercent = processingProgress,
                                        capturedFrames = if (nightMode) {
                                            nightShotCapturedFrameCount
                                        } else {
                                            multiFrameCapturedFrames
                                        },
                                        targetFrames = if (nightMode) {
                                            nightShotTargetFrameCount
                                        } else {
                                            multiFrameTargetFrames
                                        },
                                        nightShot = nightMode,
                                        queueCount = if (nightMode && labelLower.contains("hàng đợi 2")) 2 else 0,
                                        holdSteady = isCapturingMultiFrame,
                                        stabilityBars = stability,
                                        elapsedSeconds = processingElapsedSeconds,
                                        batteryTemperatureC = measuredBatteryTempC,
                                        thermalStatus = thermalStatusCode,
                                        errorCode = if (hasError) stringResource(R.string.mf_error_short) else null,
                                        reducedMotion = dispReducedMotion,
                                        simpleMode = appPrefs.getBoolean("simple_ui_mode_enabled", false)
                                    ),
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .offset(y = frameHeight * 0.30f),
                                    maxWidth = frameWidth * 0.36f,
                                    maxHeight = frameHeight * 0.10f
                                )
                            }
                        }
                    }
    }

    proAdjustmentContent = { adjustmentModifier ->
        Column(adjustmentModifier) {
                        val selected = proSel
                        if (selected != null && mode == "Pro") {
                            when (selected) {
                                "EV" -> {
                                    val range = evRange
                                    if (range != null && range.upper > range.lower) {
                                        val span = (range.upper - range.lower).coerceAtLeast(1)
                                        FujiScale(
                                            label = "EV",
                                            valueText = evText,
                                            position = ((evIndex - range.lower).toFloat() / span).coerceIn(0f, 1f),
                                            auto = false,
                                            lowLabel = "%.1f".format(Locale.US, range.lower * evStep),
                                            highLabel = "+%.1f".format(Locale.US, range.upper * evStep),
                                            stepFraction = 1f / span,
                                            onPositionChange = { fraction ->
                                                val value = (range.lower + fraction * span).roundToInt()
                                                evIndex = value
                                                camera?.let { active ->
                                                    runCatching {
                                                        applyAutoExposure(active)
                                                        active.cameraControl.setExposureCompensationIndex(value)
                                                    }
                                                }
                                            },
                                            onBack = { proSel = null },
                                        onAuto = {
                                                evIndex = 0
                                                camera?.let { active ->
                                                    runCatching {
                                                        applyAutoExposure(active)
                                                        active.cameraControl.setExposureCompensationIndex(0)
                                                    }
                                                }
                                            }
                                        )
                                    } else {
                                        Text(
                                            "EV không được hỗ trợ",
                                            color = CamColors.Sub,
                                            fontSize = 11.sp,
                                            fontFamily = FujiTypography.uiFont
                                        )
                                    }
                                }

                                "ISO" -> {
                                    val range = isoRange
                                    if (manualSensorSupported && range != null && range.upper >= range.lower) {
                                        val lo = range.lower.coerceAtLeast(1)
                                        val hi = range.upper.coerceAtMost(2400).coerceAtLeast(lo)
                                        val logSpan = if (hi > lo) ln(hi.toDouble() / lo) else 0.0
                                        val steps = if (logSpan > 0.0) {
                                            (logSpan / ln(2.0) * 3.0).roundToInt().coerceAtLeast(1)
                                        } else 1
                                        val isoPosition = if (logSpan > 0.0) {
                                            (ln(manualIso.coerceIn(lo, hi).toDouble() / lo) / logSpan).toFloat()
                                        } else 0.5f
                                        FujiScale(
                                            label = "ISO",
                                            valueText = if (manualIsoEnabled) manualIso.toString() else iso.toString(),
                                            position = isoPosition,
                                            auto = !manualIsoEnabled,
                                            lowLabel = lo.toString(),
                                            highLabel = hi.toString(),
                                            stepFraction = 1f / steps,
                                            onPositionChange = { fraction ->
                                                val thirdStops = (ln(hi.toDouble() / lo) *
                                                    fraction.coerceIn(0f, 1f) / ln(2.0) * 3.0).roundToInt()
                                                manualIso = (lo * exp(ln(2.0) * thirdStops / 3.0))
                                                    .roundToInt().coerceIn(lo, hi)
                                                manualIsoEnabled = true
                                                manualExposure = true
                                                camera?.let { active ->
                                                    runCatching {
                                                        applyCameraExposureState(
                                                            active,
                                                            true,
                                                            manualShutterEnabled,
                                                            manualIso,
                                                            manualShutterNs,
                                                            iso,
                                                            shutterNs
                                                        )
                                                    }
                                                }
                                            },
                                            onBack = { proSel = null },
                                        onAuto = {
                                                val keepManualShutter = manualShutterEnabled
                                                manualIsoEnabled = false
                                                manualExposure = keepManualShutter
                                                camera?.let { active ->
                                                    runCatching {
                                                        applyCameraExposureState(
                                                            active,
                                                            false,
                                                            keepManualShutter,
                                                            manualIso,
                                                            manualShutterNs,
                                                            iso,
                                                            shutterNs
                                                        )
                                                    }
                                                }
                                            }
                                        )
                                    } else {
                                        Text(
                                            "ISO thủ công không được hỗ trợ",
                                            color = CamColors.Sub,
                                            fontSize = 11.sp,
                                            fontFamily = FujiTypography.uiFont
                                        )
                                    }
                                }

                                "Tv" -> {
                                    val range = shutterRange
                                    if (manualSensorSupported && range != null && range.upper > range.lower) {
                                        val lo = range.lower
                                        val hi = range.upper.coerceAtLeast(lo)
                                        val fraction = shutterFraction(
                                            manualShutterNs.coerceIn(lo, hi),
                                            lo,
                                            hi
                                        )
                                        val steps = (ln(hi.toDouble() / lo.toDouble()) / ln(2.0) * 3.0)
                                            .roundToInt().coerceAtLeast(1)
                                        FujiScale(
                                            label = "Tv",
                                            valueText = if (manualShutterEnabled) shutterText else shutterText,
                                            position = fraction,
                                            auto = !manualShutterEnabled,
                                            lowLabel = shutterTimeLabel(lo),
                                            highLabel = shutterTimeLabel(hi),
                                            stepFraction = 1f / steps,
                                            onPositionChange = { next ->
                                                manualShutterNs = shutterFromFraction(next, lo, hi)
                                                    .coerceIn(lo, hi)
                                                manualShutterEnabled = true
                                                manualExposure = true
                                                camera?.let { active ->
                                                    runCatching {
                                                        applyCameraExposureState(
                                                            active,
                                                            manualIsoEnabled,
                                                            true,
                                                            manualIso,
                                                            manualShutterNs,
                                                            iso,
                                                            shutterNs
                                                        )
                                                    }
                                                }
                                            },
                                            onBack = { proSel = null },
                                        onAuto = {
                                                val keepManualIso = manualIsoEnabled
                                                manualShutterEnabled = false
                                                manualExposure = keepManualIso
                                                camera?.let { active ->
                                                    runCatching {
                                                        applyCameraExposureState(
                                                            active,
                                                            keepManualIso,
                                                            false,
                                                            manualIso,
                                                            manualShutterNs,
                                                            iso,
                                                            shutterNs
                                                        )
                                                    }
                                                }
                                            }
                                        )
                                    } else {
                                        Text(
                                            "Tv thủ công không được hỗ trợ",
                                            color = CamColors.Sub,
                                            fontSize = 11.sp,
                                            fontFamily = FujiTypography.uiFont
                                        )
                                    }
                                }

                                "AF" -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf("AF-S", "AF-C", "MF").forEach { modeOption ->
                                                FujiChip(
                                                    modeOption,
                                                    proAfMode == modeOption,
                                                    { selectProAfMode(modeOption) },
                                                    Modifier.weight(1f)
                                                )
                                            }
                                        }
                                        FujiChip(
                                            "TOUCH AF",
                                            touchAfEnabled,
                                            {
                                                touchAfEnabled = !touchAfEnabled
                                                if (touchAfEnabled.not()) {
                                                    centerAfPoint = Offset(0.5f, 0.5f)
                                                    preferredFaceCenter = null
                                                    invalidatePendingFocusLock(camera)
                                                    centerAfStatus = 0
                                                }
                                                android.util.Log.i("FUJI_AF_TOUCH", "TOUCH_AF enabled=$touchAfEnabled")
                                            },
                                            Modifier.fillMaxWidth()
                                        )
                                        if (proAfMode == "MF" && maxFocus > 0f &&
                                            availableAfModes?.contains(CameraCharacteristics.CONTROL_AF_MODE_OFF) == true
                                        ) {
                                            FujiScale(
                                                label = "AF",
                                                valueText = if (manualFocus > 0f) {
                                                    "%.1fD".format(Locale.US, manualFocus)
                                                } else "∞",
                                                position = (manualFocus / maxFocus).coerceIn(0f, 1f),
                                                auto = false,
                                                lowLabel = "MACRO",
                                                highLabel = "∞",
                                                stepFraction = 0.01f,
                                                onPositionChange = { fraction ->
                                                    manualFocus = fraction * maxFocus
                                                    milkyWayInfinityFocusEnabled = false
                                                    invalidatePendingFocusLock(camera)
                                                    camera?.let { active ->
                                                        runCatching { applyManualFocus(active, manualFocus) }
                                                    }
                                                },
                                                onBack = { proSel = null },
                                        onAuto = {
                                                    manualFocus = 0f
                                                    milkyWayInfinityFocusEnabled = false
                                                    selectProAfMode("AF-S")
                                                }
                                            )
                                        } else if (proAfMode == "MF") {
                                            Text(
                                                localizedUiText(context, "Lấy nét thủ công không được hỗ trợ trên ống kính này."),
                                                color = CamColors.Sub,
                                                fontSize = 11.sp,
                                                fontFamily = FujiTypography.uiFont
                                            )
                                        } else {
                                            Text(
                                                if (proAfMode == "AF-C") localizedUiText(context, "Lấy nét liên tục") else localizedUiText(context, "Lấy nét đơn"),
                                                color = CamColors.Sub,
                                                fontSize = 10.sp,
                                                fontFamily = FujiTypography.uiFont
                                            )
                                        }
                                    }
                                }

                                "FL" -> {
                                    val zoomState = camera?.cameraInfo?.zoomState?.value
                                    val minimumZoom = zoomState?.minZoomRatio ?: 1f
                                    val cameraReportedMaximum = zoomState?.let { state ->
                                        Camera2Info.cameraReportedZoomMax(activeCameraDetails, state.maxZoomRatio)
                                    } ?: 2f
                                    val assessment = Camera2Info.opticalZoomAssessment(
                                        activeCameraDetails,
                                        activePhysicalCameraDetails,
                                        camera2Inventory
                                    )
                                    val maximumZoom = maxTotalSelectableZoom(cameraReportedMaximum, assessment)
                                        .coerceAtLeast(minimumZoom)
                                    FujiScale(
                                        label = "FL",
                                        valueText = focalLengthText,
                                        position = logarithmicZoomPosition(zoomSel, minimumZoom, maximumZoom),
                                        auto = false,
                                        lowLabel = mainEquivalentFocal?.let { "${(it * minimumZoom).roundToInt()}mm" } ?: "--mm",
                                        highLabel = mainEquivalentFocal?.let { "${(it * maximumZoom).roundToInt()}mm" } ?: "--mm",
                                        stepFraction = 0.01f,
                                        onPositionChange = { fraction ->
                                            val requested = minimumZoom * exp(
                                                ln(maximumZoom / minimumZoom) * fraction.coerceIn(0f, 1f)
                                            )
                                            val plan = resolveZoomExecutionPlan(
                                                requestedZoomRatio = requested,
                                                minimumCameraRatio = minimumZoom,
                                                cameraReportedMaximum = cameraReportedMaximum,
                                                assessment = assessment
                                            )
                                            zoomSel = plan.effectiveZoomRatio
                                            actualZoomRatio = plan.effectiveZoomRatio
                                            digitalZoomCropScale = plan.digitalCropRatio
                                            zoomInteractionGeneration++
                                            camera?.let { active ->
                                                runCatching {
                                                    active.cameraControl.setZoomRatio(plan.cameraControlZoomRatio)
                                                }.onFailure {
                                                    android.util.Log.w("FUJI_PRO_FL", "Zoom request clamped to camera range", it)
                                                }
                                            }
                                            appPrefs.edit().putFloat("camera_zoom_ratio", plan.effectiveZoomRatio).apply()
                                        },
                                        onBack = { proSel = null },
                                        onAuto = {
                                            val requested = 1f.coerceIn(minimumZoom, maximumZoom)
                                            val plan = resolveZoomExecutionPlan(
                                                requestedZoomRatio = requested,
                                                minimumCameraRatio = minimumZoom,
                                                cameraReportedMaximum = cameraReportedMaximum,
                                                assessment = assessment
                                            )
                                            zoomSel = plan.effectiveZoomRatio
                                            actualZoomRatio = plan.effectiveZoomRatio
                                            digitalZoomCropScale = plan.digitalCropRatio
                                            camera?.let { active ->
                                                runCatching { active.cameraControl.setZoomRatio(plan.cameraControlZoomRatio) }
                                            }
                                            appPrefs.edit().putFloat("camera_zoom_ratio", plan.effectiveZoomRatio).apply()
                                        }
                                    )
                                }

                                "Tone" -> {
                                    val minKelvin = 2500
                                    val maxKelvin = 10000
                                    FujiScale(
                                        label = "WB",
                                        valueText = if (manualColorTempEnabled) "${manualColorTemp}K" else toneText,
                                        position = ((manualColorTemp - minKelvin).toFloat() /
                                            (maxKelvin - minKelvin)).coerceIn(0f, 1f),
                                        auto = !manualColorTempEnabled,
                                        lowLabel = "2500K",
                                        highLabel = "10000K",
                                        stepFraction = 100f / (maxKelvin - minKelvin),
                                        onPositionChange = { fraction ->
                                            manualColorTemp = (
                                                minKelvin + (fraction * (maxKelvin - minKelvin) / 100f).roundToInt() * 100
                                            ).coerceIn(minKelvin, maxKelvin)
                                            manualColorTempEnabled = true
                                            camera?.let { active ->
                                                runCatching { applyManualWhiteBalance(active, manualColorTemp) }
                                            }
                                        },
                                        onBack = { proSel = null },
                                        onAuto = {
                                            manualColorTempEnabled = false
                                            camera?.let { active -> runCatching { applyAutoWhiteBalance(active) } }
                                        }
                                    )
                                }
                            }
                        }
        }
    }

    val controlsContent: @Composable (Modifier) -> Unit = { controlsModifier ->
        Column(controlsModifier) {
                        if (mode == "Pro") {
                            val proItems = listOf(
                                Triple("EV", evText, "EV"),
                                Triple("Tv", shutterText, "Tv"),
                                Triple("ISO", isoText, "ISO"),
                                Triple("AF", if (proAfMode == "MF" && manualFocus > 0f) "%.1fD".format(Locale.US, manualFocus) else afText, "AF"),
                                Triple("Tone", toneText, "WB"),
                                Triple("FL", focalLengthText, "FL")
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                proItems.forEach { (key, value, label) ->
                                    val active = proSel == key
                                    Column(
                                        Modifier.weight(1f).clip(RoundedCornerShape(2.dp)).background(Color(0xFF080C0D))
                                            .border(0.8.dp, if (active) FujiSkin.orange else Color(0xFF354247), RoundedCornerShape(2.dp))
                                            .animaTClickable {
                                                if (key == "AUTO") {
                                                    manualExposure = false
                                                    manualIsoEnabled = false
                                                    manualShutterEnabled = false
                                                    manualColorTempEnabled = false
                                                    manualFocus = 0f
                                                    milkyWayInfinityFocusEnabled = false
                                                    invalidatePendingFocusLock(camera)
                                                    evIndex = 0

                                                    camera?.let { c ->
                                                        runCatching {
                                                            Camera2CameraControl.from(c.cameraControl).clearCaptureRequestOptions()
                                                            applyAutoExposure(c)
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

                                                        camera?.let { active ->
                                                            runCatching {
                                                                applyAutoExposure(active)
                                                                active.cameraControl.setExposureCompensationIndex(evIndex)
                                                            }
                                                        }
                                                    }
                                                    if (key == "AF" && manualFocus > 0f) {
                                                        manualFocus = 0f
                                                        milkyWayInfinityFocusEnabled = false
                                                        invalidatePendingFocusLock(camera)
                                                        camera?.let { runCatching { applyAutoFocus(it) } }
                                                    }
                                                    proSel = if (active) null else key
                                                }
                                            }.padding(vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(value, color = if (active) FujiSkin.orange else FujiSkin.lcdGreen,
                                            fontFamily = FujiTypography.composeFamily(androidx.compose.ui.platform.LocalContext.current),
                                            fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                                        Text(label, color = FujiSkin.muted, fontSize = 8.sp, letterSpacing = 0.7.sp)
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
                                    color = FujiSkin.orange,
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
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text("EV", color = FujiSkin.orange, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                val range = evRange
                                FujiKey(
                                    "−",
                                    onClick = {
                                        if (range != null) {
                                            val next = (evIndex - 1).coerceIn(range.lower, range.upper)
                                            evIndex = next
                                            manualExposure = false
                                            camera?.let { active -> runCatching { active.cameraControl.setExposureCompensationIndex(next) } }
                                        }
                                    },
                                    modifier = Modifier.width(34.dp),
                                    enabled = range != null && evIndex > range.lower
                                )
                                Text(
                                    if (range != null) evText else localizedUiText(context, "EV N/A"),
                                    color = CamColors.TextColor,
                                    fontFamily = FujiTypography.composeFamily(androidx.compose.ui.platform.LocalContext.current),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.weight(1f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                FujiKey(
                                    "+",
                                    onClick = {
                                        if (range != null) {
                                            val next = (evIndex + 1).coerceIn(range.lower, range.upper)
                                            evIndex = next
                                            manualExposure = false
                                            camera?.let { active -> runCatching { active.cameraControl.setExposureCompensationIndex(next) } }
                                        }
                                    },
                                    modifier = Modifier.width(34.dp),
                                    enabled = range != null && evIndex < range.upper
                                )
                                FujiKey(
                                    "0",
                                    onClick = {
                                        evIndex = 0
                                        manualExposure = false
                                        camera?.let { active -> runCatching { active.cameraControl.setExposureCompensationIndex(0) } }
                                    },
                                    modifier = Modifier.width(34.dp)
                                )
                            }
                        }
            

        }
    }


    val leftHardwareContent: @Composable (Modifier) -> Unit = { sideModifier ->
        Column(
            sideModifier.fillMaxWidth().fillMaxHeight().padding(horizontal = 3.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                Modifier.size(58.dp).clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF070909))
                    .border(2.dp, FujiSkin.silver, RoundedCornerShape(4.dp))
                    .animaTClickable { onOpenAlbum(lastUri) },
                contentAlignment = Alignment.Center
            ) {
                val image = thumb
                if (image != null) {
                    Image(
                        image,
                        "Ảnh gần nhất",
                        Modifier.fillMaxSize().padding(2.dp)
                            .graphicsLayer { rotationZ = animatedIconRotationDegrees },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    FujiIconGlyph("film", Modifier.size(24.dp), FujiSkin.lcdGreen)
                }
            }
            FujiChip("PLAY", false, { onOpenAlbum(lastUri) }, Modifier.fillMaxWidth())
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF454B4D)))
            FujiChip(
                "PRO",
                mode == "Pro",
                { mode = "Pro" },
                Modifier.fillMaxWidth()
            )
            FujiChip(
                "PHOTO",
                mode == "Photo",
                {
                    mode = "Photo"
                    proSel = null
                    manualExposure = false
                    manualIsoEnabled = false
                    manualShutterEnabled = false
                    manualColorTempEnabled = false
                    manualFocus = 0f
                    milkyWayInfinityFocusEnabled = false
                    invalidatePendingFocusLock(camera)

                    camera?.let { runCatching { applyAutoWhiteBalance(it); applyAutoFocus(it) } }
                },
                Modifier.fillMaxWidth()
            )
            FujiChip(
                "FILTER",
                filterDialogVisible || activeFilter != "None",
                {
                    activeFilter = appPrefs.getString("selected_filter", activeFilter) ?: activeFilter
                    filterDialogVisible = true
                },
                Modifier.fillMaxWidth()
            )
            FujiChip(
                "LUNA",
                lunaDreamMode != "off" || nightShotMode == "night",
                {
                    when {
                        livePhotoEnabled -> localizedToast(context, "Vui lòng tắt Live Photo", Toast.LENGTH_LONG).show()
                        nativeImageProcessingEnabled -> localizedToast(
                            context,
                            "Vui lòng tắt Xử lý ảnh gốc thiết bị để sử dụng LunaDream.",
                            Toast.LENGTH_LONG
                        ).show()
                        else -> showLunaDream = true
                    }
                },
                Modifier.fillMaxWidth()
            )
        }
    }

    val rightHardwareContent: @Composable (Modifier) -> Unit = { sideModifier ->
        val zoomStateForDial = camera?.cameraInfo?.zoomState?.value
        val dialZoomMinimum = zoomStateForDial?.minZoomRatio ?: 1f
        val dialCameraReportedMaximum = Camera2Info.cameraReportedZoomMax(
            activeCameraDetails,
            zoomStateForDial?.maxZoomRatio ?: 1f
        )
        val dialZoomMaximum = maxTotalSelectableZoom(
            dialCameraReportedMaximum,
            Camera2Info.opticalZoomAssessment(activeCameraDetails, activePhysicalCameraDetails, camera2Inventory)
        )

        val wZoomDirection = if (zoomButtonsSwapped) 1 else -1
        val tZoomDirection = -wZoomDirection

        val activeDialSelection = proSel
        val commandValue = evText
        Column(
            sideModifier.fillMaxWidth().fillMaxHeight().padding(horizontal = 3.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FujiKey(
                        "W",
                        onClick = { animateZoomTo((zoomSel + wZoomDirection * 0.1f).coerceIn(dialZoomMinimum, dialZoomMaximum)) },
                        modifier = Modifier.weight(1f),
                        enabled = zoomStateForDial != null && if (wZoomDirection > 0) zoomSel < dialZoomMaximum - 0.001f else zoomSel > dialZoomMinimum + 0.001f,
                        onPressStart = { CrashContextDiagnostics.recordUiEvent("ZOOM_W_PRESS_START"); zoomHoldDirection = wZoomDirection },
                        onPressEnd = {
                            zoomHoldDirection = 0
                            CrashContextDiagnostics.recordUiEvent("ZOOM_W_PRESS_END ratio=$zoomSel")
                            appPrefs.edit().putFloat("camera_zoom_ratio", zoomSel).apply()
                            android.util.Log.d("FUJI_ZOOM", "PRESS_END key=W direction=${if (wZoomDirection > 0) "IN" else "OUT"} zoom=$zoomSel")
                        }
                    )
                    FujiKey(
                        "T",
                        onClick = { animateZoomTo((zoomSel + tZoomDirection * 0.1f).coerceIn(dialZoomMinimum, dialZoomMaximum)) },
                        modifier = Modifier.weight(1f),
                        enabled = zoomStateForDial != null && if (tZoomDirection > 0) zoomSel < dialZoomMaximum - 0.001f else zoomSel > dialZoomMinimum + 0.001f,
                        onPressStart = { CrashContextDiagnostics.recordUiEvent("ZOOM_T_PRESS_START"); zoomHoldDirection = tZoomDirection },
                        onPressEnd = {
                            zoomHoldDirection = 0
                            CrashContextDiagnostics.recordUiEvent("ZOOM_T_PRESS_END ratio=$zoomSel")
                            appPrefs.edit().putFloat("camera_zoom_ratio", zoomSel).apply()
                            android.util.Log.d("FUJI_ZOOM", "PRESS_END key=T direction=${if (tZoomDirection > 0) "IN" else "OUT"} zoom=$zoomSel")
                        }
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(stringResource(if (wZoomDirection > 0) R.string.zoom_direction_in else R.string.zoom_direction_out), color = FujiSkin.muted, fontSize = 7.sp)
                    Text(stringResource(if (tZoomDirection > 0) R.string.zoom_direction_in else R.string.zoom_direction_out), color = FujiSkin.muted, fontSize = 7.sp)
                }
                Text("ZOOM", color = FujiSkin.ivory, fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            FujiShutterButton(
                enabled = !isProcessing,
                onHalfPress = {
                    invalidatePendingFocusLock(camera)
                    centerAfStatus = 0
                    manualAfOverrideUntilMs = SystemClock.elapsedRealtime() + 700L
                    if (manualFocus <= 0f && !milkyWayInfinityFocusEnabled) {
                        camera?.let { active -> runCatching { applyAutoFocus(active) } }
                    }
                },
                onCapture = { takePhoto() },
                modifier = Modifier.size(74.dp)
            ) {
                if (starTrailCaptureInProgress || burstCaptureActive || bracketCaptureActive) {
                    FujiIconGlyph("close", Modifier.size(25.dp), Color.White)
                } else if (shutterButtonBitmap != null) {
                    Image(
                        shutterButtonBitmap!!,
                        "Custom shutter button",
                        Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FujiKey("−", onClick = {
                    val active = camera
                    val range = evRange
                    if (active != null && range != null) {
                        val next = (evIndex - 1).coerceIn(range.lower, range.upper)
                        evIndex = next
                        manualExposure = false
                        runCatching { active.cameraControl.setExposureCompensationIndex(next) }
                        android.util.Log.d("FUJI_EV", "TAP direction=DOWN index=$next")
                    }
                }, modifier = Modifier.weight(1f),
                    onPressStart = { CrashContextDiagnostics.recordUiEvent("EV_MINUS_PRESS_START"); evHoldDirection = -1 },
                    onPressEnd = {
                        evHoldDirection = 0
                        CrashContextDiagnostics.recordUiEvent("EV_MINUS_PRESS_END index=$evIndex")
                        appPrefs.edit().putInt("pro_ev_index", evIndex).apply()
                        android.util.Log.d("FUJI_EV", "PRESS_END direction=DOWN index=$evIndex")
                    })
                Column(Modifier.weight(1.3f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        commandValue,
                        color = FujiSkin.lcdGreen,
                        fontFamily = FujiTypography.composeFamily(androidx.compose.ui.platform.LocalContext.current),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        "EV",
                        color = FujiSkin.ivory,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.7.sp
                    )
                }
                FujiKey("+", onClick = {
                    val active = camera
                    val range = evRange
                    if (active != null && range != null) {
                        val next = (evIndex + 1).coerceIn(range.lower, range.upper)
                        evIndex = next
                        manualExposure = false
                        runCatching { active.cameraControl.setExposureCompensationIndex(next) }
                        android.util.Log.d("FUJI_EV", "TAP direction=UP index=$next")
                    }
                }, modifier = Modifier.weight(1f),
                    onPressStart = { CrashContextDiagnostics.recordUiEvent("EV_PLUS_PRESS_START"); evHoldDirection = 1 },
                    onPressEnd = {
                        evHoldDirection = 0
                        CrashContextDiagnostics.recordUiEvent("EV_PLUS_PRESS_END index=$evIndex")
                        appPrefs.edit().putInt("pro_ev_index", evIndex).apply()
                        android.util.Log.d("FUJI_EV", "PRESS_END direction=UP index=$evIndex")
                    })
            }
            FujiChip("SWAP", false, { front = !front; manualFocus = 0f }, Modifier.fillMaxWidth())
            FujiKey(
                "DISP/BACK · ${stringResource(lcdDisplayMode.labelResource)}",
                onClick = {
                    if (proSel != null && mode == "Pro") {
                        android.util.Log.i("FUJI_PRO_SCALE", "BACK source=DISP selected=$proSel")
                        proSel = null
                    } else cycleDispDisplayMode()
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
            )
        }
    }

    val leftHandedLayout = appPrefs.getBoolean("left_handed_layout_enabled", false)
    val simpleUiMode = appPrefs.getBoolean("simple_ui_mode_enabled", false)
    val leftPanelContent: @Composable (Modifier) -> Unit =
        if (leftHandedLayout) rightHardwareContent else leftHardwareContent
    val rightPanelContent: @Composable (Modifier) -> Unit =
        if (leftHandedLayout) leftHardwareContent else rightHardwareContent

    FujiLeatherSurface(
        if (performanceMode) Modifier.fillMaxSize() else Modifier.fillMaxSize().animaTContent(),
        simpleMode = simpleUiMode
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val layoutMetrics = screenProfile.layoutFor(maxWidth.value, maxHeight.value)
            val topPlateHeight = layoutMetrics.topPlateHeight
            val leftColumnWidth = layoutMetrics.leftColumnWidth
            val rightColumnWidth = layoutMetrics.rightColumnWidth
            val chromeAlpha = if (hudVisible) ratioUiAlpha.value else 0f
            val pxPerDp = androidx.compose.ui.platform.LocalDensity.current.density

            Column(Modifier.fillMaxSize().padding(vertical = 4.dp)) {
                Box(
                    Modifier.fillMaxWidth().height(topPlateHeight)
                        .graphicsLayer { alpha = chromeAlpha }
                        .onSizeChanged {
                            Log.d("LUNA_LAYOUT", "TOP_PLATE ${it.width}px x ${it.height}px (${it.width / pxPerDp}dp x ${it.height / pxPerDp}dp)")
                        }
                ) {
                    toolbarContent(Modifier.fillMaxSize().windowInsetsPadding(
                                WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)
                                    .union(WindowInsets.systemGestures.only(WindowInsetsSides.Horizontal))
                            ), topPlateHeight)
                    if (DEBUG_FUJI_LAYOUT) Box(Modifier.fillMaxSize().background(Color.Red.copy(alpha = 0.13f)))
                }
                Row(
                    Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.width(leftColumnWidth).fillMaxHeight()
                            .windowInsetsPadding(
                                WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)
                                    .union(WindowInsets.systemGestures.only(WindowInsetsSides.Horizontal))
                            )
                            .graphicsLayer { alpha = chromeAlpha }
                            .onSizeChanged {
                                Log.d("LUNA_LAYOUT", "LEFT_COLUMN ${it.width}px x ${it.height}px (${it.width / pxPerDp}dp x ${it.height / pxPerDp}dp)")
                            }
                    ) {
                        leftPanelContent(Modifier.fillMaxSize())
                        if (DEBUG_FUJI_LAYOUT) Box(Modifier.fillMaxSize().background(Color.Blue.copy(alpha = 0.17f)))
                    }
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        Box(
                            Modifier.weight(1f).fillMaxWidth()
                                .onSizeChanged {
                                    Log.d("LUNA_LAYOUT", "LCD_SLOT ${it.width}px x ${it.height}px (${it.width / pxPerDp}dp x ${it.height / pxPerDp}dp)")
                                }
                        ) {
                            previewContent(Modifier.fillMaxSize())
                            if (!hasCam) {
                                Text(
                                    localizedUiText(context, "NO SIGNAL"),
                                    color = Color(0xFFE8442B),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            } else if (previewStreamState != PreviewView.StreamState.STREAMING) {
                                Text(
                                    localizedUiText(context, "STARTING..."),
                                    color = FujiSkin.lcdGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                            if (DEBUG_FUJI_LAYOUT) Box(Modifier.fillMaxSize().background(Color.Yellow.copy(alpha = 0.12f)))
                        }
                        if (mode == "Pro") {
                            Box(Modifier.fillMaxWidth().height(layoutMetrics.proControlHeight).graphicsLayer { alpha = chromeAlpha }) {
                                controlsContent(Modifier.fillMaxSize())
                            }
                        }
                    }
                    Box(
                        Modifier.width(rightColumnWidth).fillMaxHeight()
                            .windowInsetsPadding(
                                WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)
                                    .union(WindowInsets.systemGestures.only(WindowInsetsSides.Horizontal))
                            )
                            .graphicsLayer { alpha = chromeAlpha }
                            .onSizeChanged {
                                Log.d("LUNA_LAYOUT", "RIGHT_COLUMN ${it.width}px x ${it.height}px (${it.width / pxPerDp}dp x ${it.height / pxPerDp}dp)")
                            }
                    ) {
                        rightPanelContent(Modifier.fillMaxSize())
                        if (DEBUG_FUJI_LAYOUT) Box(Modifier.fillMaxSize().background(Color(0xFF9B59B6).copy(alpha = 0.18f)))
                    }
                }
            }
        AnimatedVisibility(
            visible = showQuick,
            enter = AnimaT.enter(
                performanceMode = performanceMode,
                reducedMotion = dispReducedMotion
            ),
            exit = AnimaT.exit(
                performanceMode = performanceMode,
                reducedMotion = dispReducedMotion
            ),
            modifier = Modifier.align(if (uiIsLandscape) Alignment.TopEnd else Alignment.TopCenter)
                .padding(start = 10.dp, end = 10.dp, top = if (uiIsLandscape) 8.dp else 64.dp)
        ) {
            QuickPanel(
                ratioIdx = ratioIdx,
                onRatio = { changeAspectRatio(it) },
                deviceLandscape = isLandscape,
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
                faceTrackingMode = faceTrackingMode,
                onFaceTrackingModeChanged = { nextMode ->
                    if (nextMode in setOf("off", "face", "face_eye")) {
                        faceTrackingMode = nextMode
                    }
                },
                onToolbox = { showQuick = false; onOpenToolbox() },
                onWatermarkPanel = { showQuick = false; onOpenWatermarkPanel() },
                watermarkEnabled = appPrefs.getBoolean("watermark_enabled", true),
                watermarkLayoutLabel = when (appPrefs.getString("watermark_template", "classic_bar")) {
                    "caption" -> "CAPTION"
                    "card" -> "CARD"
                    "gallery" -> "GALLERY"
                    "black" -> "BLACK"
                    else -> "BAR"
                },
                onContentManagement = { showQuick = false; onOpenContentManagement() },
                onSettings = { showQuick = false; onOpenSettings() },
                onDismiss = { showQuick = false },
                modifier = Modifier.fillMaxWidth()
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
                    FujiIconGlyph("moon", Modifier.size(38.dp), FujiSkin.orange)
                    Text("ĐANG GHI STAR TRAILS", color = Color.White, fontSize = 16.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
                    Text(
                        starTrailFrameCount.toString() + " khung hình đã chụp",
                        color = FujiSkin.orange,
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
                onNightShotDurationChange = { seconds ->
                    nightShotDurationSeconds = seconds.coerceIn(1, 30)
                    appPrefs.edit().putInt("nightshot_duration_seconds", nightShotDurationSeconds).apply()
                },
                onNightShotToggle = { enabled ->
                    nightShotMode = if (enabled) "night" else "off"
                    if (enabled && nightShotDurationSeconds !in listOf(5, 10, 3)) nightShotDurationSeconds = 5
                    appPrefs.edit()
                        .putString("nightshot_mode", nightShotMode)
                        .putInt("nightshot_duration_seconds", nightShotDurationSeconds)
                        .apply()
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
                                color = FujiSkin.orange,
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
                                            .background(if (viewerShowsOriginal == showOriginal) FujiSkin.orange else Color.Transparent)
                                            .clickable { viewerShowsOriginal = showOriginal }
                                            .padding(horizontal = 14.dp, vertical = 9.dp)
                                    )
                                }
                            }
                        }
                    }
                    Box(Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                        FujiKey(
                            "ĐÓNG",
                            onClick = { photoViewerVisible = false; viewerBitmap = null; viewerOriginalBitmap = null; viewerShowsOriginal = false },
                            modifier = Modifier.width(60.dp)
                        )
                    }
                }
            }
        }
        if (ratioTransitionInProgress) {
            Box(
                Modifier.fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.02f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { }
            )
        }
        }
    }
}


@Composable
private fun LensTransitionOverlay(
    visible: Boolean,
    lensLabel: String,
    performanceMode: Boolean,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val transitionMillis = when {
        reducedMotion -> 0
        performanceMode -> 80
        else -> 120
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(transitionMillis)) + scaleIn(
            initialScale = if (reducedMotion) 1f else 0.94f,
            animationSpec = tween(transitionMillis, easing = FastOutSlowInEasing)
        ),
        exit = fadeOut(tween(transitionMillis)) + scaleOut(
            targetScale = if (reducedMotion) 1f else 0.98f,
            animationSpec = tween(transitionMillis, easing = FastOutSlowInEasing)
        )
    ) {
        Column(
            Modifier.background(Color(0xFF080B0C))
                .border(1.dp, Color(0xFF536C6E))
                .padding(horizontal = 22.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("LENS TRANSITION", color = Color(0xFFBDBDBD), fontSize = 9.sp,
                letterSpacing = 1.5.sp)
            Text(lensLabel, color = FujiSkin.orange,
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
                modifier.background(FujiSkin.lcd).border(1.dp, Color(0xFF344247)),
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
    var selectedLookCategory by remember { mutableStateOf("all") }
    var activeLutIds by remember { mutableStateOf(activeCustomCameraLutIds(context)) }
    var isImportingLut by remember { mutableStateOf(false) }
    var showingOriginal by remember { mutableStateOf(false) }

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
    val allLookEntries = cameraLookGroups.flatMap { group ->
        group.presets.map { group.id to it }
    } + activeCustomLuts.map { customCameraLookCategoryId(it.category) to customCameraLutPreset(it.id) }
    val lookCategories = buildList {
        add(CameraLookCategory("all", R.string.filter_category_all))
        cameraLookGroups.forEach { group ->
            add(CameraLookCategory(group.id, group.labelResource))
        }
        activeCustomLuts.forEach { lut ->
            add(CameraLookCategory(customCameraLookCategoryId(lut.category), customLabel = lut.category))
        }
    }.distinctBy { it.id }
    val visiblePresets = allLookEntries
        .filter { selectedLookCategory == "all" || it.first == selectedLookCategory }
        .map { it.second }
        .filter {
            filterSearch.isBlank() ||
                cameraLookDisplayName(context, it).contains(filterSearch.trim(), ignoreCase = true)
        }
    val activeFilterName = cameraLookDisplayName(context, activeFilter)

    val previewPane: @Composable (Modifier) -> Unit = { paneModifier ->
        Box(
            paneModifier.fillMaxWidth()
                .then(if (isLandscape) Modifier.fillMaxHeight() else Modifier.height(previewHeight))
                .clip(RoundedCornerShape(3.dp)).background(FujiSkin.lcd)
                .border(1.dp, Color(0xFF758084), RoundedCornerShape(3.dp))
                .pointerInput(activeFilter) {
                    detectTapGestures(
                        onPress = {
                            showingOriginal = true
                            tryAwaitRelease()
                            showingOriginal = false
                        }
                    )
                }
        ) {
            FilterLookPreviewImage(
                source = previewSource,
                preset = if (showingOriginal) "None" else activeFilter,
                modifier = Modifier.fillMaxSize(),
                maxDimension = 480,
            )
            Text(
                if (showingOriginal) "ẢNH GỐC · THẢ ĐỂ XEM LỌC" else "GIỮ ĐỂ XEM ẢNH GỐC",
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopStart)
                    .background(Color(0xBB050708)).padding(horizontal = 7.dp, vertical = 5.dp)
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
        FujiSearchField(
            value = filterSearch,
            onValueChange = { filterSearch = it },
            modifier = paneModifier.fillMaxWidth(),
            placeholder = stringResource(R.string.filter_search_placeholder)
        )
    }
    val categoryPane: @Composable (Modifier) -> Unit = { paneModifier ->
        Row(
            paneModifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 7.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            lookCategories.forEach { category ->
                val label = category.labelResource?.let { stringResource(it) }
                    ?: category.customLabel.orEmpty()
                FujiChip(
                    text = label,
                    selected = selectedLookCategory == category.id,
                    onClick = { selectedLookCategory = category.id }
                )
            }
        }
    }
    val presetGrid: @Composable (Modifier) -> Unit = { listModifier ->
        LazyColumn(listModifier, verticalArrangement = Arrangement.spacedBy(0.dp)) {
            items(visiblePresets.chunked(if (isLandscape) 3 else 2), key = { pair -> pair.firstOrNull() ?: "empty" }) { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { preset ->
                        val selected = activeFilter == preset
                        val itemName = cameraLookDisplayName(context, preset)
                        Column(
                            Modifier.weight(1f).padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF15191A))
                                .border(1.dp, if (selected) FujiSkin.orange else Color(0xFF3C4649), RoundedCornerShape(4.dp))
                                .animaTClickable { onSelectFilter(preset) }.padding(6.dp)
                        ) {
                            Box(Modifier.fillMaxWidth().height(76.dp).clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFF070909))
                                .border(1.dp, if (selected) FujiSkin.orange else Color(0xFF687277), RoundedCornerShape(2.dp))) {
                                Row(
                                    Modifier.fillMaxSize().padding(horizontal = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        Modifier.width(5.dp).fillMaxHeight(),
                                        verticalArrangement = Arrangement.SpaceEvenly
                                    ) { repeat(6) { Box(Modifier.size(4.dp).background(Color(0xFF9BA2A3), RoundedCornerShape(1.dp))) } }
                                    Box(Modifier.weight(1f).fillMaxHeight().padding(horizontal = 3.dp)) {
                                        FilterLookPreviewImage(
                                            source = previewSource,
                                            preset = preset,
                                            modifier = Modifier.fillMaxSize(),
                                            maxDimension = 192,
                                        )
                                        if (selected) {
                                            Text(
                                                "●",
                                                color = FujiSkin.orange,
                                                fontSize = 10.sp,
                                                modifier = Modifier.align(Alignment.TopEnd)
                                                    .background(Color(0xAA000000)).padding(horizontal = 3.dp)
                                            )
                                        }
                                    }
                                    Column(
                                        Modifier.width(5.dp).fillMaxHeight(),
                                        verticalArrangement = Arrangement.SpaceEvenly
                                    ) { repeat(6) { Box(Modifier.size(4.dp).background(Color(0xFF9BA2A3), RoundedCornerShape(1.dp))) } }
                                }
                            }
                            Text(
                                itemName,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                modifier = Modifier.padding(top = 5.dp)
                            )
                            Text(
                                "FUJIFILM LOOK",
                                color = FujiSkin.lcdGreen,
                                fontSize = 6.sp,
                                letterSpacing = 0.75.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        FujiPanel(
            title = stringResource(R.string.film_filters_title),
            modifier = Modifier.fillMaxWidth(0.97f)
                .then(if (isLandscape) Modifier.fillMaxHeight(0.94f) else Modifier.heightIn(max = dialogMaxHeight)),
            onBack = onDismiss
        ) {
            if (isLandscape) {
                Row(
                    Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        Modifier.weight(0.88f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Column(
                            Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                if (showingOriginal || activeFilter == "None") {
                                    stringResource(R.string.filter_preview_original)
                                } else {
                                    stringResource(R.string.filter_preview_name, activeFilterName)
                                },
                                color = FujiSkin.lcdGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            previewPane(Modifier.weight(1f).fillMaxWidth())
                        }
                    }
                    Column(
                        Modifier.weight(1.12f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        searchPane(Modifier)
                        categoryPane(Modifier)
                        Text(
                            stringResource(R.string.filter_count, visiblePresets.size, allLookEntries.size),
                            color = FujiSkin.lcdGreen,
                            fontFamily = FujiTypography.composeFamily(androidx.compose.ui.platform.LocalContext.current),
                            fontSize = 9.sp
                        )
                        presetGrid(Modifier.weight(1f).fillMaxWidth())
                    }
                }
            } else {
                Text(
                    if (activeFilter == "None") stringResource(R.string.filter_preview_original) else stringResource(R.string.filter_preview_name, activeFilterName),
                    color = FujiSkin.lcdGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 5.dp)
                )
                previewPane(Modifier.fillMaxWidth().height(140.dp))
                searchPane(Modifier)
                categoryPane(Modifier)
                Text("${visiblePresets.size} bộ lọc", color = FujiSkin.lcdGreen, fontSize = 9.sp)
                presetGrid(Modifier.weight(1f))
            }
        }
    }
}

// Filter groups include the original brand/film looks which were previously hidden
// when the picker was flattened into a single "Bộ lọc" category.
private data class CameraLookGroup(
    val id: String,
    val labelResource: Int,
    val presets: List<String>
)

private data class CameraLookCategory(
    val id: String,
    val labelResource: Int? = null,
    val customLabel: String? = null
)

private fun customCameraLookCategoryId(label: String): String {
    val slug = label.lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
        .ifBlank { "uncategorized" }
    return "custom_$slug"
}

private val cameraLookGroups = builtInCameraLookGroupDefinitions.map { group ->
    val labelResource = when (group.id) {
        "fujifilm" -> R.string.filter_category_fujifilm
        "color_film" -> R.string.filter_category_color_film
        "classic" -> R.string.filter_category_classic
        "bw" -> R.string.filter_category_black_white
        "leica" -> R.string.filter_category_leica
        "hasselblad" -> R.string.filter_category_hasselblad
        "zeiss" -> R.string.filter_category_zeiss
        "sony" -> R.string.filter_category_sony
        "cinema_log" -> R.string.filter_category_cinema_log
        else -> R.string.filter_category_basic
    }
    CameraLookGroup(group.id, labelResource, group.presets)
}

private fun applyCameraExposureState(
    camera: Camera,
    manualIsoEnabled: Boolean,
    manualShutterEnabled: Boolean,
    manualIso: Int,
    manualShutterNs: Long,
    liveIso: Int,
    liveShutterNs: Long
) {
    val state = resolveCameraExposureRequestState(
        manualIsoEnabled, manualShutterEnabled, manualIso, manualShutterNs, liveIso, liveShutterNs
    )
    val options = CaptureRequestOptions.Builder()
        .setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
        .setCaptureRequestOption(
            CaptureRequest.CONTROL_AE_MODE,
            if (state.automatic) CaptureRequest.CONTROL_AE_MODE_ON
            else CaptureRequest.CONTROL_AE_MODE_OFF
        )
    state.sensorSensitivityIso?.let {
        options.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, it)
    }
    state.sensorExposureTimeNs?.let {
        options.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, it)
    }
    state.sensorFrameDurationNs?.let {
        options.setCaptureRequestOption(CaptureRequest.SENSOR_FRAME_DURATION, it)
    }
    Camera2CameraControl.from(camera.cameraControl).addCaptureRequestOptions(options.build())
    android.util.Log.d(
        "FUJI_EXPOSURE",
        "mode=${if (state.automatic) "AUTO" else "MANUAL"} " +
            "iso=${state.sensorSensitivityIso ?: "AE"} shutterNs=${state.sensorExposureTimeNs ?: "AE"}"
    )
}

/**
 * CameraX FocusMeteringAction retains the AF region; this applies the matching Camera2
 * AE lock explicitly after focus succeeds. Unsupported AE-lock devices are not reported
 * as having completed a full AF/AE lock.
 */
private fun applyCameraAeLock(camera: Camera, locked: Boolean): Boolean {
    val cameraId = runCatching { Camera2CameraInfo.from(camera.cameraInfo).cameraId }
        .getOrNull() ?: "unknown"
    val available = runCatching {
        Camera2CameraInfo.from(camera.cameraInfo)
            .getCameraCharacteristic(CameraCharacteristics.CONTROL_AE_LOCK_AVAILABLE) == true
    }.getOrDefault(false)

    if (!available) {
        android.util.Log.w("FUJI_AF", "CONTROL_AE_LOCK unavailable cameraId=$cameraId")
        return !locked
    }

    return runCatching {
        val options = CaptureRequestOptions.Builder()
            .setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, locked)
            .build()
        Camera2CameraControl.from(camera.cameraControl).addCaptureRequestOptions(options)
        android.util.Log.i("FUJI_AF", "CONTROL_AE_LOCK=$locked applied cameraId=$cameraId")
        true
    }.onFailure {
        android.util.Log.w("FUJI_AF", "Could not apply CONTROL_AE_LOCK=$locked cameraId=$cameraId", it)
    }.getOrDefault(false)
}

private fun applyAutoExposure(camera: Camera) {
    val options = CaptureRequestOptions.Builder()
        .setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
        .setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
        .build()
    Camera2CameraControl.from(camera.cameraControl).addCaptureRequestOptions(options)
}

private fun applyManualExposure(camera: Camera, iso: Int, exposureNs: Long) {
    val options = CaptureRequestOptions.Builder()
        .setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
        .setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
        .setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, iso)
        .setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureNs)
        .setCaptureRequestOption(CaptureRequest.SENSOR_FRAME_DURATION, maxOf(exposureNs, 10_000_000L))
        .build()
    Camera2CameraControl.from(camera.cameraControl).addCaptureRequestOptions(options)
}

private fun applyManualFocus(camera: Camera, diopters: Float) {
    val options = CaptureRequestOptions.Builder()
        .setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
        .setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
        .setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, diopters)
        .build()
    Camera2CameraControl.from(camera.cameraControl).addCaptureRequestOptions(options)
}

private fun applyAutoFocus(camera: Camera) {
    val options = CaptureRequestOptions.Builder()
        .setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
        .setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
        .build()
    Camera2CameraControl.from(camera.cameraControl).addCaptureRequestOptions(options)
}

/**
 * Issues a one-shot Camera2 AF trigger for devices that expose no AF metering regions.
 * CONTROL_AF_TRIGGER is reset to IDLE after the sensor has received the START edge.
 */
private fun triggerCamera2AfOnce(camera: Camera): Boolean = runCatching {
    val cameraId = runCatching {
        Camera2CameraInfo.from(camera.cameraInfo).cameraId
    }.getOrDefault("unknown")
    val control = Camera2CameraControl.from(camera.cameraControl)
    val start = CaptureRequestOptions.Builder()
        .setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
        .setCaptureRequestOption(
            CaptureRequest.CONTROL_AF_MODE,
            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
        )
        .setCaptureRequestOption(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START)
        .build()
    control.addCaptureRequestOptions(start)
    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
        runCatching {
            val idle = CaptureRequestOptions.Builder()
                .setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_TRIGGER,
                    CaptureRequest.CONTROL_AF_TRIGGER_IDLE
                )
                .build()
            control.addCaptureRequestOptions(idle)
            android.util.Log.d("FUJI_AF", "CONTROL_AF_TRIGGER=IDLE cameraId=$cameraId")
        }.onFailure {
            android.util.Log.w("FUJI_AF", "Could not reset CONTROL_AF_TRIGGER cameraId=$cameraId", it)
        }
    }, 120L)
    android.util.Log.i("FUJI_AF", "CONTROL_AF_TRIGGER=START cameraId=$cameraId source=Camera2Fallback")
    true
}.onFailure {
    android.util.Log.w("FUJI_AF", "CONTROL_AF_TRIGGER_START failed", it)
}.getOrDefault(false)

private fun applyAutoWhiteBalance(camera: Camera) {
    val options = CaptureRequestOptions.Builder()
        .setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
        .setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
        .build()
    Camera2CameraControl.from(camera.cameraControl).addCaptureRequestOptions(options)
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
    Camera2CameraControl.from(camera.cameraControl).addCaptureRequestOptions(options)
}

private fun shutterTimeLabel(value: Long): String {
    if (value <= 0L) return "--"
    val seconds = value / 1_000_000_000.0
    return if (seconds >= 0.5) {
        String.format(Locale.US, "%.1f″", seconds)
    } else {
        "1/" + (1.0 / seconds).roundToInt()
    }
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
    thumbColor = FujiSkin.orange,
    activeTrackColor = FujiSkin.orange,
    inactiveTrackColor = Color(0xFF3A3A3A)
)

@Composable
private fun MiniChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(7.dp))
        .background(if (selected) FujiSkin.orange else CamColors.Chip)
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
