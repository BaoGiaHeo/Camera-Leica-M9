package com.example.camui

import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Process
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Captures uncaught Kotlin/Java exceptions and gives the user time to copy the report
 * before the original process is allowed to terminate.
 *
 * Native crashes (SIGSEGV), force-stop, low-memory kills and OS process termination
 * cannot be intercepted by an UncaughtExceptionHandler.
 */
class CrashCaptureApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                val reportId = UUID.randomUUID().toString()
                val reportDir = File(filesDir, "crash_reports").apply { mkdirs() }
                val reportFile = File(reportDir, "crash_$reportId.txt")
                val ackFile = File(reportDir, "ack_$reportId")

                val stack = StringWriter().also { writer ->
                    error.printStackTrace(PrintWriter(writer))
                }.toString()
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US).format(Date())
                val report = buildString {
                    appendLine("FUJIFILM MODE - CRASH REPORT")
                    appendLine("Time: $timestamp")
                    appendLine("App: $packageName")
                    appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                    appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                    appendLine("Thread: ${thread.name} (id=${thread.id})")
                    appendLine()
                    appendLine("EXCEPTION")
                    appendLine(stack)
                }
                reportFile.writeText(report, Charsets.UTF_8)
                Log.e("FujiFilmCrash", "Uncaught exception captured. Report: ${reportFile.absolutePath}", error)

                val intent = Intent(this, CrashReportActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra(CrashReportActivity.EXTRA_REPORT_PATH, reportFile.absolutePath)
                    putExtra(CrashReportActivity.EXTRA_ACK_PATH, ackFile.absolutePath)
                }
                startActivity(intent)

                // Keep this crashed process suspended while the independent report Activity
                // remains usable. OK writes an acknowledgement file, then normal crash handling
                // continues. A timeout prevents a permanently stuck process if UI cannot open.
                val deadline = System.currentTimeMillis() + 10 * 60 * 1000L
                while (!ackFile.exists() && System.currentTimeMillis() < deadline) {
                    try {
                        Thread.sleep(200)
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    }
                }
                ackFile.delete()
            } catch (captureError: Throwable) {
                Log.e("FujiFilmCrash", "Could not display crash report", captureError)
            }

            if (previousHandler != null) {
                previousHandler.uncaughtException(thread, error)
            } else {
                Process.killProcess(Process.myPid())
                kotlin.system.exitProcess(10)
            }
        }
    }
}
