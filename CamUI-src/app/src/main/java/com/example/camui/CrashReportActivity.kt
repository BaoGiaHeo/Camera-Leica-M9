package com.example.camui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File

/**
 * Runs in a separate process so it can remain interactive while the crashed app
 * process is suspended inside its uncaught-exception handler.
 */
class CrashReportActivity : Activity() {
    companion object {
        const val EXTRA_REPORT_PATH = "crash_report_path"
        const val EXTRA_ACK_PATH = "crash_ack_path"
    }

    private var ackPath: String? = null
    private var reportText: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ackPath = intent.getStringExtra(EXTRA_ACK_PATH)
        val reportPath = intent.getStringExtra(EXTRA_REPORT_PATH)
        reportText = runCatching {
            reportPath?.let { File(it).readText(Charsets.UTF_8) }
        }.getOrNull().orEmpty().ifBlank { "Không đọc được tệp log. Hãy kiểm tra thư mục crash_reports." }

        window.statusBarColor = Color.rgb(18, 18, 18)
        window.navigationBarColor = Color.rgb(18, 18, 18)
        window.setBackgroundDrawableResource(android.R.color.background_dark)
        val kk7Typeface = FujiTypography.androidTypeface(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(18))
            setBackgroundColor(Color.rgb(18, 18, 18))
        }
        val title = TextView(this).apply {
            text = "FujiFilm Mode đã gặp lỗi"
            textSize = 22f
            typeface = kk7Typeface
            setTextColor(Color.WHITE)
        }
        val subtitle = TextView(this).apply {
            text = "Ứng dụng đang tạm dừng. Sao chép mã log bên dưới trước khi đóng ứng dụng."
            textSize = 14f
            typeface = kk7Typeface
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(8), 0, dp(14))
        }
        val logView = TextView(this).apply {
            text = reportText
            textSize = 12f
            typeface = kk7Typeface
            setTextColor(Color.rgb(225, 225, 225))
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setTextIsSelectable(true)
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(logView, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }
        val copyButton = mechanicalAction("Sao chép log") {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("FujiFilm Mode crash log", reportText))
            Toast.makeText(this@CrashReportActivity, "Đã sao chép log", Toast.LENGTH_SHORT).show()
        }
        val okButton = mechanicalAction("OK – Đóng ứng dụng") {
            runCatching { ackPath?.let { File(it).writeText("ok") } }
            finish()
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        root.addView(copyButton, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(12) })
        root.addView(okButton, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        setContentView(root)
    }

    private fun mechanicalAction(label: String, action: () -> Unit): TextView = TextView(this).apply {
        text = label.uppercase()
        textSize = 10f
        typeface = FujiTypography.androidTypeface(this@CrashReportActivity)
        gravity = Gravity.CENTER
        setTextColor(Color.rgb(238, 241, 239))
        setPadding(dp(12), dp(11), dp(12), dp(11))
        background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.rgb(76, 83, 86), Color.rgb(24, 29, 31))
        ).apply {
            cornerRadius = dp(3).toFloat()
            setStroke(dp(1), Color.rgb(130, 139, 142))
        }
        isClickable = true
        isFocusable = true
        setOnClickListener { action() }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
