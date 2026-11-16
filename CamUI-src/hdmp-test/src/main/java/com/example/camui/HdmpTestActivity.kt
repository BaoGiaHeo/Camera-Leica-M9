package com.example.camui

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.concurrent.Executors

class HdmpTestActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private var sourceUri: Uri? = null
    private lateinit var status: TextView
    private lateinit var chooseButton: Button
    private lateinit var ai15Button: Button
    private lateinit var ai2Button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "HDMP AI Test"
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 24)
        }
        root.addView(TextView(this).apply {
            text = "HDMP AI TEST"
            textSize = 24f
            setPadding(0, 0, 0, 8)
        })
        root.addView(TextView(this).apply {
            text = "Kiểm tra hai model NAFNet đang dùng trong FujiFilm Mode 1.6. Ảnh gốc được giữ nguyên; mỗi lượt chạy tạo một bản kết quả riêng trong thư viện ảnh."
            textSize = 14f
            setPadding(0, 0, 0, 18)
        })
        chooseButton = Button(this).apply {
            text = "CHỌN ẢNH KIỂM TRA"
            setOnClickListener {
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/*"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                }, PICK_IMAGE)
            }
        }
        ai15Button = Button(this).apply {
            text = "CHẠY HDMP AI 1.5 (NAFNet Width32)"
            isEnabled = false
            setOnClickListener { runModel(HdmpAiEngine.Model.NAFNETLIB_WIDTH32) }
        }
        ai2Button = Button(this).apply {
            text = "CHẠY HDMP AI 2 (NAFNet Width64)"
            isEnabled = false
            setOnClickListener { runModel(HdmpAiEngine.Model.NAFNET_WIDTH64) }
        }
        status = TextView(this).apply {
            text = "Chưa chọn ảnh. Dùng cùng một ảnh gốc để so sánh hai model."
            textSize = 14f
            setTextIsSelectable(true)
            setPadding(0, 20, 0, 0)
        }
        root.addView(chooseButton, matchParent())
        root.addView(ai15Button, matchParent())
        root.addView(ai2Button, matchParent())
        root.addView(status, matchParent())
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun matchParent() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { bottomMargin = 8 }

    @Deprecated("Deprecated in Android API; retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            runCatching {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            sourceUri = uri
            status.text = "Đã chọn ảnh: $uri\nChọn một model để bắt đầu."
            ai15Button.isEnabled = true
            ai2Button.isEnabled = true
        }
    }

    private fun runModel(model: HdmpAiEngine.Model) {
        val input = sourceUri ?: return
        setButtons(false)
        status.text = "Đang chuẩn bị ảnh và khởi tạo " + modelLabel(model) + "…"
        executor.execute {
            val started = android.os.SystemClock.elapsedRealtime()
            var outputUri: Uri? = null
            val report = runCatching {
                outputUri = makeIndependentOutputCopy(input, model)
                val result = HdmpAiEngine.process(
                    context = this,
                    uri = outputUri!!,
                    workerCount = 8,
                    selectedModel = model,
                    onProgress = { _, percent ->
                        runOnUiThread { status.text = modelLabel(model) + " đang xử lý: " + percent + "%" }
                    }
                )
                val wallMs = android.os.SystemClock.elapsedRealtime() - started
                result.detail + "\n\nThời gian toàn lượt đo (gồm tạo bản sao ảnh): " + wallMs + " ms" +
                    (outputUri?.let { "\nẢnh kết quả: " + it } ?: "")
            }.getOrElse { failure ->
                "LỖI KIỂM TRA: " + failure.javaClass.simpleName + ": " + failure.message
            }
            runOnUiThread {
                status.text = report
                setButtons(true)
            }
        }
    }

    private fun makeIndependentOutputCopy(input: Uri, model: HdmpAiEngine.Model): Uri {
        val stamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
            .format(java.util.Date())
        val suffix = if (model == HdmpAiEngine.Model.NAFNET_WIDTH64) "AI2" else "AI15"
        val name = "HDMP_Test_" + suffix + "_" + stamp + ".jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/HDMP AI Test")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val destination = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Không tạo được ảnh kiểm thử trong thư viện")
        try {
            val copied = contentResolver.openInputStream(input)?.use { source ->
                contentResolver.openOutputStream(destination, "w")?.use { target ->
                    source.copyTo(target)
                    true
                } ?: false
            } ?: false
            check(copied) { "Không sao chép được ảnh đầu vào" }
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                contentResolver.update(destination, ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }, null, null)
            }
            return destination
        } catch (failure: Throwable) {
            contentResolver.delete(destination, null, null)
            throw failure
        }
    }

    private fun modelLabel(model: HdmpAiEngine.Model) =
        if (model == HdmpAiEngine.Model.NAFNET_WIDTH64) "HDMP AI 2" else "HDMP AI 1.5"

    private fun setButtons(enabled: Boolean) {
        chooseButton.isEnabled = enabled
        ai15Button.isEnabled = enabled && sourceUri != null
        ai2Button.isEnabled = enabled && sourceUri != null
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }

    companion object { private const val PICK_IMAGE = 4102 }
}
