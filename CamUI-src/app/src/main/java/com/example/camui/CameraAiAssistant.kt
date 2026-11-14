package com.example.camui
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.compose.ui.geometry.Offset
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import java.io.Closeable
import java.util.concurrent.TimeUnit
import kotlin.math.abs

internal data class CameraAiAnalysis(val posePoints: List<Offset>, val poseHint: String, val sceneHint: String)

/** On-device ML Kit scene labels plus MediaPipe's 33 body landmarks. */
internal class CameraAiAssistant(context: Context) : Closeable {
 private val appContext = context.applicationContext
 private val labeler: ImageLabeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
 private val landmarker: PoseLandmarker? = runCatching {
  val base = BaseOptions.builder().setModelAssetPath("pose_landmarker_lite.task").build()
  val options = PoseLandmarker.PoseLandmarkerOptions.builder()
   .setBaseOptions(base).setRunningMode(RunningMode.IMAGE).setNumPoses(1).build()
  PoseLandmarker.createFromOptions(appContext, options)
 }.onFailure { Log.w("FUJI_AI_POSE", "Pose model unavailable; scene labeling remains active", it) }
  .getOrNull()

 fun analyze(frame: Bitmap, rotationDegrees: Int, frontFacing: Boolean): CameraAiAnalysis {
  var oriented: Bitmap? = null
  var working: Bitmap? = null
  try {
   val matrix = Matrix().apply {
    postRotate(rotationDegrees.toFloat())
    if (frontFacing) postScale(-1f, 1f)
   }
   oriented = Bitmap.createBitmap(frame, 0, 0, frame.width, frame.height, matrix, true)
   val longest = maxOf(oriented!!.width, oriented!!.height)
   working = if (longest > 480) {
    val factor = 480f / longest
    Bitmap.createScaledBitmap(oriented!!,
     (oriented!!.width * factor).toInt().coerceAtLeast(1),
     (oriented!!.height * factor).toInt().coerceAtLeast(1), true)
     .also { if (it !== oriented) oriented!!.recycle() }
   } else oriented
   oriented = null
   val labels = runCatching {
    Tasks.await(labeler.process(InputImage.fromBitmap(working!!, 0)), 900, TimeUnit.MILLISECONDS)
     .sortedByDescending { it.confidence }.take(4).map { it.text.lowercase() }
   }.onFailure { Log.w("FUJI_AI_DIRECTOR", "Scene labels skipped", it) }.getOrDefault(emptyList())
   val pose = runCatching {
    landmarker?.detect(BitmapImageBuilder(working!!).build())?.landmarks()?.firstOrNull()
   }.onFailure { Log.w("FUJI_AI_POSE", "Pose inference skipped", it) }.getOrNull()
   val points = pose?.take(33)?.map { Offset(it.x().coerceIn(0f, 1f), it.y().coerceIn(0f, 1f)) }.orEmpty()
   val poseHint = if (points.size >= 33) poseHint(points)
    else "Đưa toàn thân vào khung để AI nhận diện 33 điểm"
   return CameraAiAnalysis(points, poseHint, sceneHint(labels))
  } finally {
   working?.let { if (!it.isRecycled) it.recycle() }
   oriented?.let { if (!it.isRecycled) it.recycle() }
  }
 }

 private fun poseHint(p: List<Offset>): String {
  val tilt = abs(p[11].y - p[12].y)
  val hipY = (p[23].y + p[24].y) / 2f
  return when {
   tilt > 0.075f -> "Tạo dáng: xoay nhẹ thân và giữ đường vai cân bằng"
   p[15].y > hipY + 0.03f && p[16].y > hipY + 0.03f ->
    "Tạo dáng: đặt nhẹ một tay lên eo hoặc tóc, thả lỏng vai"
   abs(p[23].x - p[24].x) > 0.18f ->
    "Tạo dáng: chuyển trọng tâm nhẹ, tránh khóa cứng đầu gối"
   else -> "Tư thế ổn · thử xoay thân 15° và nhìn lệch ống kính"
  }
 }
 private fun sceneHint(labels: List<String>): String {
  val all = labels.joinToString(" ")
  return when {
   all.contains("person") || all.contains("human") || all.contains("face") ->
    "AI Director: chân dung · chừa khoảng trống theo hướng nhìn, ưu tiên mắt ở 1/3 trên"
   all.contains("food") || all.contains("dish") || all.contains("plate") ->
    "AI Director: ẩm thực · thử góc 45° hoặc từ trên xuống, giữ nền gọn"
   all.contains("mountain") || all.contains("sky") || all.contains("landscape") ||
    all.contains("tree") || all.contains("nature") ->
    "AI Director: phong cảnh · đặt đường chân trời theo 1/3, giữ chi tiết tiền cảnh"
   all.contains("building") || all.contains("architecture") || all.contains("house") ->
    "AI Director: kiến trúc · giữ đường dọc thẳng, tránh nghiêng máy"
   all.contains("flower") || all.contains("plant") ->
    "AI Director: cận cảnh · tách chủ thể khỏi nền và chừa khoảng thở"
   labels.isNotEmpty() -> "AI Director: chủ thể ${labels.first()} · thử bố cục lệch 1/3"
   else -> "AI Director: giữ chủ thể rõ, kiểm tra nền và khoảng trống trước khi chụp"
  }
 }
 override fun close() {
  runCatching { landmarker?.close() }
  runCatching { labeler.close() }
 }
}
