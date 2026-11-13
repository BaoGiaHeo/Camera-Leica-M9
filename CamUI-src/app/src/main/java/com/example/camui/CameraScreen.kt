package com.example.camui

import android.Manifest
import android.content.ContentValues
import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.net.Uri
import android.provider.MediaStore
import android.util.Rational
import android.util.Size
import android.view.Surface
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
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val meteringModes = listOf("Center", "Matrix", "Spot")
private val wbModes = listOf(
    "Auto" to CaptureRequest.CONTROL_AWB_MODE_AUTO,
    "Daylight" to CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT,
    "Cloudy" to CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT,
    "Tungsten" to CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT,
    "Fluorescent" to CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT
)

@Composable
fun CameraScreen(
    onOpenSettings: () -> Unit,
    onOpenToolbox: () -> Unit,
    onOpenContentManagement: () -> Unit,
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
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var targetRotation by remember { mutableIntStateOf(Surface.ROTATION_0) }
    val focusPeakingEnabled = rememberUpdatedState(focusPeaking)
    LaunchedEffect(configuration.orientation) {
        targetRotation = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
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

    var mode by remember { mutableStateOf("Photo") }
    var front by remember { mutableStateOf(false) }
    var flash by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var timerSec by remember { mutableIntStateOf(0) }
    var countdown by remember { mutableIntStateOf(0) }
    var showQuick by remember { mutableStateOf(false) }
    var zoomSel by remember { mutableFloatStateOf(1f) }
    var metering by remember { mutableIntStateOf(0) }
    var ultraHdr by remember { mutableStateOf(false) }
    var stabilization by remember { mutableStateOf(false) }
    var renderEngine by remember { mutableStateOf("Performance") }
    var proSel by remember { mutableStateOf<String?>(null) }
    var evIndex by remember { mutableIntStateOf(0) }
    var iso by remember { mutableIntStateOf(0) }
    var shutterNs by remember { mutableLongStateOf(0L) }
    var focusDiopter by remember { mutableFloatStateOf(0f) }
    var manualExposure by remember { mutableStateOf(false) }
    var manualIso by remember { mutableIntStateOf(400) }
    var manualShutterNs by remember { mutableLongStateOf(10_000_000L) }
    var manualFocus by remember { mutableFloatStateOf(0f) }
    var whiteBalance by remember { mutableIntStateOf(CaptureRequest.CONTROL_AWB_MODE_AUTO) }
    var hist by remember { mutableStateOf(IntArray(64)) }
    var peakPoints by remember { mutableStateOf(emptyList<Offset>()) }
    var lastUri by remember { mutableStateOf<Uri?>(null) }
    var thumb by remember { mutableStateOf<ImageBitmap?>(null) }
    var viewerBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var photoViewerVisible by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var previewUseCase by remember { mutableStateOf<Preview?>(null) }
    var analysisUseCase by remember { mutableStateOf<ImageAnalysis?>(null) }
    val appPrefs = remember { context.getSharedPreferences("fujifilm_settings", 0) }
    val imageQuality = appPrefs.getString("image_quality", "Best") ?: "Best"
    val noiseReduction = appPrefs.getString("noise_reduction", "Medium") ?: "Medium"

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

    // The custom CameraX ViewPort crops the preview and saved image to the same ratio.
    LaunchedEffect(hasCam, front, ratioIdx, ultraHdr, isLandscape, renderEngine) {
        if (!hasCam) return@LaunchedEffect
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = runCatching { future.get() }.getOrNull() ?: return@addListener
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

            fun bindCapture(outputFormat: Int) {
                val previewBuilder = Preview.Builder().setTargetRotation(rotation)
                Camera2Interop.Extender(previewBuilder).setSessionCaptureCallback(
                    object : CameraCaptureSession.CaptureCallback() {
                        override fun onCaptureCompleted(
                            session: CameraCaptureSession,
                            request: CaptureRequest,
                            result: TotalCaptureResult
                        ) {
                            result.get(CaptureResult.SENSOR_SENSITIVITY)?.let { iso = it }
                            result.get(CaptureResult.SENSOR_EXPOSURE_TIME)?.let { shutterNs = it }
                            result.get(CaptureResult.LENS_FOCUS_DISTANCE)?.let { focusDiopter = it }
                        }
                    }
                )
                val preview = previewBuilder.build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val captureBuilder = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .setTargetRotation(rotation)
                if (outputFormat == ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR) {
                    captureBuilder.setOutputFormat(ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR)
                }
                captureBuilder.setJpegQuality(when (imageQuality) {
                    "Low" -> 60
                    "Medium" -> 78
                    "High" -> 92
                    else -> 100
                })
                if (imageQuality == "Best") {
                    captureBuilder.setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                            .build()
                    )
                }
                val requestedNrMode = when (noiseReduction) {
                    "Off" -> CaptureRequest.NOISE_REDUCTION_MODE_OFF
                    "Low" -> CaptureRequest.NOISE_REDUCTION_MODE_MINIMAL
                    "High" -> CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY
                    else -> CaptureRequest.NOISE_REDUCTION_MODE_FAST
                }
                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                val activeFacingId = runCatching {
                    cameraManager.cameraIdList.firstOrNull { id ->
                        cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) ==
                            if (front) CameraCharacteristics.LENS_FACING_FRONT else CameraCharacteristics.LENS_FACING_BACK
                    }
                }.getOrNull()
                val availableNrModes = activeFacingId?.let { id ->
                    runCatching {
                        cameraManager.getCameraCharacteristics(id)
                            .get(CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES)?.toSet()
                    }.getOrNull()
                }.orEmpty()
                val effectiveNrMode = requestedNrMode.takeIf { it in availableNrModes }
                    ?: availableNrModes.firstOrNull()
                if (effectiveNrMode != null) {
                    Camera2Interop.Extender(captureBuilder).setCaptureRequestOption(
                        CaptureRequest.NOISE_REDUCTION_MODE, effectiveNrMode
                    )
                }
                val capture = captureBuilder.build()
                // Set the actual still-image crop (not just the preview ViewPort).
                capture.setCropAspectRatio(ratio)
                capture.flashMode = flash

                val analysis = ImageAnalysis.Builder()
                    .setTargetRotation(rotation)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                previewUseCase = preview
                analysisUseCase = analysis
                var lastAnalysis = 0L
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
                            var y = 8
                            while (y < image.height - 1) {
                                var x = 8
                                while (x < image.width - 1) {
                                    val index = y * rowStride + x * pixelStride
                                    val value = buffer.get(index).toInt() and 0xFF
                                    bins[value shr 2]++
                                    if (focusPeakingEnabled.value && x > 1 && y > 1 && index < buffer.limit()) {
                                        val leftIndex = index - pixelStride
                                        val upIndex = index - rowStride
                                        if (leftIndex >= 0 && upIndex >= 0 && upIndex < buffer.limit()) {
                                            val left = buffer.get(leftIndex).toInt() and 0xFF
                                            val up = buffer.get(upIndex).toInt() and 0xFF
                                            if (abs(value - left) + abs(value - up) > 48 && edges.size < 350) {
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
                                    x += 12
                                }
                                y += 12
                            }
                            scope.launch(Dispatchers.Main) {
                                hist = bins
                                peakPoints = if (focusPeakingEnabled.value) edges else emptyList()
                            }
                        }
                    } finally {
                        image.close()
                    }
                }

                val selector = if (front) CameraSelector.DEFAULT_FRONT_CAMERA
                    else CameraSelector.DEFAULT_BACK_CAMERA
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
                evIndex = 0
                zoomSel = 1f
                val capabilities = runCatching {
                    Camera2CameraInfo.from(bound.cameraInfo).getCameraCharacteristic(
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES
                    )
                }.getOrNull()
                val supportsManualSensor = capabilities?.contains(
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR
                ) == true
                if (manualExposure && supportsManualSensor) {
                    applyManualExposure(bound, manualIso, manualShutterNs)
                }
                applyWhiteBalance(bound, whiteBalance)
                if (stabilization) applyStabilization(bound, true)
            }

            try {
                val requestedFormat = if (ultraHdr && Build.VERSION.SDK_INT >= 34)
                    ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR
                    else ImageCapture.OUTPUT_FORMAT_JPEG
                bindCapture(requestedFormat)
                if (ultraHdr && Build.VERSION.SDK_INT >= 34) {
                    val supported = runCatching {
                        ImageCapture.getImageCaptureCapabilities(camera!!.cameraInfo)
                            .supportedOutputFormats
                            .contains(ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR)
                    }.getOrDefault(false)
                    if (!supported) {
                        ultraHdr = false
                        Toast.makeText(context, "Camera này không hỗ trợ Ultra HDR; đã chuyển về JPEG.", Toast.LENGTH_LONG).show()
                        provider.unbindAll()
                        bindCapture(ImageCapture.OUTPUT_FORMAT_JPEG)
                    }
                }
            } catch (e: Exception) {
                if (ultraHdr) {
                    ultraHdr = false
                    provider.unbindAll()
                    runCatching { bindCapture(ImageCapture.OUTPUT_FORMAT_JPEG) }
                        .onFailure {
                            camera = null
                            Toast.makeText(context, "Không thể bật Ultra HDR hoặc khởi tạo camera: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    camera = null
                    Toast.makeText(context, "Không mở được camera: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
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
                context.contentResolver.loadThumbnail(uri, Size(320, 320), null).asImageBitmap()
            }.getOrNull()
        }
    }

    LaunchedEffect(photoViewerVisible, lastUri) {
        if (!photoViewerVisible) return@LaunchedEffect
        val uri = lastUri ?: return@LaunchedEffect
        viewerBitmap = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.loadThumbnail(uri, Size(2048, 2048), null).asImageBitmap()
            }.getOrNull()
        }
    }

    fun openLastPhoto() {
        if (lastUri == null) {
            Toast.makeText(context, "Chưa có ảnh trong Pictures/FujifilmMode.", Toast.LENGTH_SHORT).show()
            return
        }
        photoViewerVisible = true
    }

    fun takePhoto() {
        if (camera == null) return
        val capture = imageCapture ?: return
        scope.launch {
            if (timerSec > 0) {
                for (s in timerSec downTo 1) {
                    countdown = s
                    delay(1000)
                }
                countdown = 0
            }
            capture.targetRotation = targetRotation
            val name = "FUJI_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$name.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.ORIENTATION, when (targetRotation) {
                    Surface.ROTATION_90 -> 90
                    Surface.ROTATION_180 -> 180
                    Surface.ROTATION_270 -> 270
                    else -> 0
                })
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FujifilmMode")
            }
            val output = ImageCapture.OutputFileOptions.Builder(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            ).build()
            capture.takePicture(
                output,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                        val saved = result.savedUri
                        if (saved != null) {
                            val info = camera?.cameraInfo?.let { runCatching { Camera2CameraInfo.from(it) }.getOrNull() }
                            val focal = info?.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                                ?.firstOrNull()?.let { "%.1f mm".format(Locale.US, it) } ?: ""
                            val aperture = info?.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)
                                ?.firstOrNull()?.let { "f/%.1f".format(Locale.US, it) } ?: ""
                            val actualIso = if (manualExposure) manualIso else iso
                            val actualShutterNs = if (manualExposure) manualShutterNs else shutterNs
                            val shutterLabel = if (actualShutterNs > 0L) {
                                val seconds = actualShutterNs / 1_000_000_000.0
                                if (seconds >= 0.5) "%.1fs".format(Locale.US, seconds)
                                else "1/" + (1.0 / seconds).roundToInt() + "s"
                            } else ""
                            scope.launch(Dispatchers.IO) {
                                val stamped = runCatching {
                                    applyCameraWatermark(context, saved, focal, aperture, shutterLabel,
                                        if (actualIso > 0) actualIso.toString() else "AUTO")
                                }.getOrDefault(false)
                                withContext(Dispatchers.Main) {
                                    lastUri = saved
                                    val message = if (stamped) "Đã lưu ảnh và đóng watermark."
                                        else "Đã lưu ảnh vào Pictures/FujifilmMode."
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Ảnh đã chụp nhưng MediaStore không trả về URI.", Toast.LENGTH_LONG).show()
                        }
                    }
                    override fun onError(exception: androidx.camera.core.ImageCaptureException) {
                        Toast.makeText(context, "Lỗi chụp ảnh: ${exception.message}", Toast.LENGTH_LONG).show()
                    }
                }
            )
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
    val availableAwbModes = cameraInfo2?.getCameraCharacteristic(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES)?.toSet()
        ?: setOf(CaptureRequest.CONTROL_AWB_MODE_AUTO)
    val supportedWbModes = wbModes.filter { it.second in availableAwbModes }
    val evRange = camera?.cameraInfo?.exposureState?.exposureCompensationRange
    val evStep = camera?.cameraInfo?.exposureState?.exposureCompensationStep?.toFloat() ?: 0f
    val evText = if (evStep > 0f) "%+.1f".format(Locale.US, evIndex * evStep) else "0.0"
    val currentIso = if (manualExposure) manualIso else iso
    val isoText = if (currentIso > 0) currentIso.toString() else "AUTO"
    val currentShutter = if (manualExposure) manualShutterNs else shutterNs
    val shutterText = if (currentShutter > 0) {
        val seconds = currentShutter / 1_000_000_000.0
        if (seconds >= 0.5) "%.1f\"".format(Locale.US, seconds)
        else "1/${(1.0 / seconds).roundToInt()}"
    } else "AUTO"
    val afText = if (manualFocus > 0f) "%.1f D".format(Locale.US, manualFocus) else "AF"

    Box(Modifier.fillMaxSize().background(CamColors.Bg).systemBarsPadding()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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
                    Icons.Default.FilterCenterFocus, "Metering",
                    onClick = { metering = (metering + 1) % meteringModes.size },
                    tint = if (metering > 0) CamColors.Yellow else Color.White
                )
                CircleBtn(
                    Icons.Default.BarChart, "Histogram",
                    onClick = { onShowHist(!showHist) },
                    tint = if (showHist) CamColors.Yellow else Color.White
                )
                CircleBtn(
                    Icons.Default.Settings, "Quick settings",
                    onClick = { showQuick = !showQuick },
                    tint = if (showQuick) CamColors.Yellow else Color.White
                )
            }

            Spacer(Modifier.height(7.dp))
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .aspectRatio(aspectRatios[ratioIdx].second.let { if (isLandscape) 1f / it else it })
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                        .pointerInput(camera) {
                            detectTransformGestures { _, _, zoomChange, _ ->
                                val zoomState = camera?.cameraInfo?.zoomState?.value ?: return@detectTransformGestures
                                val maxZoom = min(2f, zoomState.maxZoomRatio)
                                val nextZoom = (zoomState.zoomRatio * zoomChange)
                                    .coerceIn(zoomState.minZoomRatio, maxZoom)
                                camera?.cameraControl?.setZoomRatio(nextZoom)
                                zoomSel = nextZoom
                            }
                        }
                        .pointerInput(camera, metering) {
                            detectTapGestures { offset ->
                                val active = camera ?: return@detectTapGestures
                                if (mode == "Pro" && manualFocus > 0f) {
                                    manualFocus = 0f
                                    runCatching { applyAutoFocus(active) }
                                }
                                val area = when (metering) { 0 -> 0.38f; 1 -> 0.70f; else -> 0.12f }
                                // Center meters at frame center; Matrix uses a broad region
                                // around the tapped subject; Spot uses a tight region.
                                val pointX = offset.x
                                val pointY = offset.y
                                val point = previewView.meteringPointFactory.createPoint(pointX, pointY, area)
                                val action = FocusMeteringAction.Builder(
                                    point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                                ).setAutoCancelDuration(3, TimeUnit.SECONDS).build()
                                active.cameraControl.startFocusAndMetering(action)
                            }
                        }
                ) {
                    key(renderEngine) {
                        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
                    }
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
                                drawCircle(Color(0xFFFF3B30), 1.5.dp.toPx(),
                                    Offset(p.x * size.width, p.y * size.height))
                            }
                        }
                    }
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

            // Only show values exposed by this camera's zoom range.
            val zoomStateForUi = camera?.cameraInfo?.zoomState?.value
            val zoomMaxForUi = min(2f, zoomStateForUi?.maxZoomRatio ?: 1f)
            val zoomMinForUi = zoomStateForUi?.minZoomRatio ?: 1f
            val zoomSteps = listOf(0.6f, 0.7f, 1f, 2f)
                .filter { it >= zoomMinForUi - 0.01f && it <= zoomMaxForUi + 0.01f }
                .ifEmpty { listOf(zoomMinForUi.coerceAtMost(2f)) }
            val activeCameraIdLabel = cameraInfo2?.cameraId ?: "—"
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                zoomSteps.forEach { z ->
                    val selectedZoom = abs(zoomSel - z) < 0.08f
                    val label = if (z < 1f) String.format(Locale.US, "%.1f×", z) else "${z.toInt()}×"
                    Text(label,
                        color = if (selectedZoom) CamColors.Yellow else Color.White,
                        fontWeight = if (selectedZoom) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val next = z.coerceIn(zoomMinForUi, zoomMaxForUi)
                                camera?.cameraControl?.setZoomRatio(next)
                                zoomSel = next
                            }.padding(horizontal = 5.dp, vertical = 3.dp)
                    )
                }
                Text("Camera ID $activeCameraIdLabel", color = CamColors.Sub, fontSize = 8.sp,
                    modifier = Modifier.padding(start = 5.dp))
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
                            if (manualSensorSupported && range != null && range.upper > range.lower) {
                                val lo = range.lower
                                val hi = range.upper.coerceAtMost(2400).coerceAtLeast(range.lower)
                                Slider(
                                    value = ((manualIso.coerceIn(lo, hi) - lo).toFloat() / (hi - lo).coerceAtLeast(1)).coerceIn(0f, 1f),
                                    onValueChange = { f ->
                                        manualIso = (lo + f * (hi - lo)).roundToInt().coerceIn(lo, hi)
                                        manualExposure = true
                                        camera?.let { runCatching { applyManualExposure(it, manualIso, manualShutterNs) } }
                                    },
                                    colors = cameraSliderColors()
                                )
                            } else Text("Cảm biến không hỗ trợ ISO thủ công", color = CamColors.Sub, fontSize = 10.sp)
                        }
                        "Tv" -> {
                            val range = shutterRange
                            if (manualSensorSupported && range != null && range.upper > range.lower) {
                                val lo = range.lower
                                val hi = range.upper.coerceAtMost(1_000_000_000L).coerceAtLeast(range.lower)
                                Slider(
                                    value = shutterFraction(manualShutterNs.coerceAtMost(1_000_000_000L), lo, hi),
                                    onValueChange = { f ->
                                        manualShutterNs = shutterFromFraction(f, lo, hi).coerceAtMost(1_000_000_000L)
                                        manualExposure = true
                                        camera?.let { runCatching { applyManualExposure(it, manualIso, manualShutterNs) } }
                                    },
                                    colors = cameraSliderColors()
                                )
                            } else Text("Cảm biến không hỗ trợ shutter thủ công", color = CamColors.Sub, fontSize = 10.sp)
                        }
                        "AF" -> {
                            if (maxFocus > 0f) {
                                Slider(
                                    value = (manualFocus / maxFocus).coerceIn(0f, 1f),
                                    onValueChange = { f ->
                                        manualFocus = f * maxFocus
                                        camera?.let { runCatching { applyManualFocus(it, manualFocus) } }
                                    },
                                    colors = cameraSliderColors()
                                )
                            } else Text("Ống kính chỉ hỗ trợ AF tự động", color = CamColors.Sub, fontSize = 10.sp)
                        }
                        "AWB" -> {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                supportedWbModes.forEach { (label, value) ->
                                    MiniChip(label, whiteBalance == value) {
                                        whiteBalance = value
                                        camera?.let { runCatching { applyWhiteBalance(it, value) } }
                                    }
                                }
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
                    Triple("AWB", supportedWbModes.firstOrNull { it.second == whiteBalance }?.first ?: "Auto", "AWB"),
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
                                        manualFocus = 0f
                                        whiteBalance = CaptureRequest.CONTROL_AWB_MODE_AUTO
                                        evIndex = 0
                                        camera?.let { c ->
                                            runCatching {
                                                Camera2CameraControl.from(c.cameraControl).clearCaptureRequestOptions()
                                                c.cameraControl.setExposureCompensationIndex(0)
                                                applyAutoFocus(c)
                                                applyWhiteBalance(c, CaptureRequest.CONTROL_AWB_MODE_AUTO)
                                            }
                                        }
                                        proSel = null
                                    } else {
                                        if (key == "EV" && manualExposure) {
                                            camera?.let {
                                                runCatching {
                                                    Camera2CameraControl.from(it.cameraControl).clearCaptureRequestOptions()
                                                    it.cameraControl.setExposureCompensationIndex(evIndex)
                                                }
                                            }
                                            manualExposure = false
                                        }
                                        if (key == "AF" && manualFocus > 0f) {
                                            manualFocus = 0f
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
            } else {
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("EV", color = CamColors.Yellow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    val range = evRange
                    if (range != null && range.upper > range.lower) {
                        Slider(
                            modifier = Modifier.weight(1f),
                            value = evIndex.toFloat().coerceIn(range.lower.toFloat(), range.upper.toFloat()),
                            onValueChange = {
                                val value = it.roundToInt()
                                evIndex = value
                                camera?.let { c -> runCatching { c.cameraControl.setExposureCompensationIndex(value) } }
                            },
                            valueRange = range.lower.toFloat()..range.upper.toFloat(),
                            steps = (range.upper - range.lower - 1).coerceAtLeast(0),
                            colors = cameraSliderColors()
                        )
                    } else {
                        Text("EV không được hỗ trợ", color = CamColors.Sub, fontSize = 10.sp,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                    }
                    Text(evText, color = Color.White, fontSize = 11.sp)
                    Text("AUTO", color = CamColors.Sub, fontSize = 9.sp,
                        modifier = Modifier.padding(start = 10.dp).clickable {
                            evIndex = 0
                            camera?.let { runCatching { it.cameraControl.setExposureCompensationIndex(0) } }
                        })
                }
            }

            Row(Modifier.fillMaxWidth().padding(vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(13.dp)).background(CamColors.Glass)
                    .clickable { openLastPhoto() }, contentAlignment = Alignment.Center) {
                    val image = thumb
                    if (image != null) Image(image, "Mở ảnh vừa chụp", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Icon(Icons.Default.PhotoLibrary, "Mở album", tint = Color.White)
                }
                Box(Modifier.size(72.dp).border(4.dp, Color.White, CircleShape)
                    .padding(7.dp).clip(CircleShape).background(Color.White).clickable { takePhoto() })
                CircleBtn(Icons.Default.FlipCameraAndroid, "Đổi camera",
                    onClick = { front = !front; manualFocus = 0f }, size = 52.dp)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                listOf("Pro", "Photo").forEach { item ->
                    val active = mode == item
                    Box(Modifier.padding(horizontal = 4.dp).width(92.dp).height(36.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (active) CamColors.Yellow else CamColors.Glass)
                        .clickable { mode = item; if (item == "Photo") proSel = null },
                        contentAlignment = Alignment.Center) {
                        Text(item, color = if (active) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
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
                        Toast.makeText(context, "Camera/API này không hỗ trợ chụp JPEG Ultra HDR.", Toast.LENGTH_LONG).show()
                    } else {
                        ultraHdr = !ultraHdr
                    }
                },
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

private fun applyWhiteBalance(camera: Camera, mode: Int) {
    val options = CaptureRequestOptions.Builder()
        .setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
        .setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, mode)
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
