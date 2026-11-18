package com.example.camui

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Persistent diagnostics for Developer builds only.
 * Debug/Release builds compile the calls but the generated BuildConfig flag keeps
 * all file access and collection disabled.
 */
internal object LunaDreamDiagnostics {
    private const val LOG_FILE_NAME = "lunadream_working.log"
    private const val BACKUP_FILE_NAME = "lunadream_working.log.1"
    private const val MAX_LOG_BYTES = 512L * 1024L
    private const val MAX_ENTRY_CHARS = 48_000
    private val lock = Any()

    @Volatile
    private var appContext: Context? = null

    fun initialize(context: Context) {
        if (!BuildConfig.DEVELOPER_LOG_ENABLED) return
        appContext = context.applicationContext
        record(
            "APP_START",
            "appId=${context.packageName}; version=${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}); " +
                "device=${Build.MANUFACTURER} ${Build.MODEL}; android=${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
        )
    }

    fun record(
        context: Context,
        event: String,
        details: String,
        failure: Throwable? = null
    ) {
        if (!BuildConfig.DEVELOPER_LOG_ENABLED) return
        appContext = context.applicationContext
        record(event, details, failure)
    }

    fun record(
        event: String,
        details: String,
        failure: Throwable? = null
    ) {
        if (!BuildConfig.DEVELOPER_LOG_ENABLED) return
        val context = appContext ?: return
        runCatching {
            val directory = File(context.filesDir, "developer_logs")
            if (!directory.exists() && !directory.mkdirs()) return
            val file = File(directory, LOG_FILE_NAME)
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US).format(Date())
            val thread = Thread.currentThread()
            val entry = buildString {
                appendLine("[$timestamp] [$event] thread=${thread.name}#${thread.id}")
                appendLine(details.take(10_000))
                if (failure != null) {
                    val stack = StringWriter().also { writer ->
                        failure.printStackTrace(PrintWriter(writer))
                    }.toString()
                    appendLine("EXCEPTION:")
                    appendLine(stack.take(MAX_ENTRY_CHARS))
                }
                appendLine()
            }
            synchronized(lock) {
                file.appendText(entry, Charsets.UTF_8)
                if (file.length() > MAX_LOG_BYTES) {
                    val backup = File(directory, BACKUP_FILE_NAME)
                    if (backup.exists()) backup.delete()
                    file.renameTo(backup)
                }
            }
        }
    }

    fun read(context: Context): String {
        if (!BuildConfig.DEVELOPER_LOG_ENABLED) return "LOG LunaDream Working chỉ khả dụng trong bản Developer."
        return runCatching {
            val directory = File(context.applicationContext.filesDir, "developer_logs")
            val files = listOf(
                File(directory, BACKUP_FILE_NAME),
                File(directory, LOG_FILE_NAME)
            ).filter { it.isFile && it.length() > 0L }
            if (files.isEmpty()) {
                "Chưa có lỗi hoặc sự kiện LunaDream nào được ghi nhận.\n\n" +
                    "Hãy thử chụp bằng LunaDream 1.5 hoặc 2.0. Nếu xử lý thất bại, log nguyên nhân sẽ xuất hiện tại đây."
            } else {
                files.joinToString(separator = "\n") { it.readText(Charsets.UTF_8) }
            }
        }.getOrElse { "Không đọc được LOG LunaDream Working: ${it.javaClass.simpleName}: ${it.message}" }
    }

    fun clear(context: Context): Boolean {
        if (!BuildConfig.DEVELOPER_LOG_ENABLED) return false
        return runCatching {
            val directory = File(context.applicationContext.filesDir, "developer_logs")
            val current = File(directory, LOG_FILE_NAME)
            val backup = File(directory, BACKUP_FILE_NAME)
            (!current.exists() || current.delete()) && (!backup.exists() || backup.delete())
        }.getOrDefault(false)
    }
}
