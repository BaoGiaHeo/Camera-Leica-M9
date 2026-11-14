package com.example.camui

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val BACKUP_FORMAT = "fujifilm-mode-settings"
private const val BACKUP_VERSION = 1

@Composable
fun BackupRestorePanel(onRestoreComplete: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Sao lưu cấu hình camera, zoom và các tính năng đã bật.") }
    var busy by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            busy = true
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val backup = createSettingsBackup(context)
                        context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use {
                            it.write(backup.toString(2))
                        } ?: error("Không mở được tệp đích")
                    }
                }
                busy = false
                status = if (result.isSuccess) "Đã xuất bản sao lưu cài đặt." else
                    "Không thể tạo bản sao lưu. Hãy kiểm tra vị trí lưu và thử lại."
                localizedToast(context, status, Toast.LENGTH_LONG).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            busy = true
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val raw = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                            it.readText()
                        } ?: error("Không đọc được tệp")
                        restoreSettingsBackup(context, JSONObject(raw))
                    }
                }
                busy = false
                if (result.isSuccess) onRestoreComplete()
                status = if (result.isSuccess) {
                    "Đã khôi phục cài đặt camera, zoom và các tính năng đã bật."
                } else {
                    "Không thể khôi phục. Hãy chọn tệp sao lưu Fujifilm Mode hợp lệ."
                }
                localizedToast(context, status, Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Sao lưu & Khôi phục", color = CamColors.TextColor, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text(
            "Sao lưu camera trước/sau, tỷ lệ khung hình, zoom, chế độ Pro/Photo, flash, hẹn giờ, HDR, UHD, chống rung, AI và các tùy chọn đã bật.",
            color = CamColors.Sub, fontSize = 12.sp
        )
        Button(
            onClick = {
                val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                exportLauncher.launch("FujifilmMode-Backup-$stamp.json")
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Xuất bản sao lưu")
        }
        Button(
            onClick = { importLauncher.launch(arrayOf("application/json", "text/json", "application/octet-stream")) },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Khôi phục bản sao lưu")
        }
        Text(
            status,
            color = CamColors.TextColor,
            fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(CamColors.Card).padding(12.dp)
        )
        Text(
            "Lưu ý: tệp này lưu cấu hình camera, frame và ảnh nút chụp; không nhúng tệp frame/ảnh gốc. Sau khi cài lại ứng dụng hoặc chuyển máy, cần nhập lại frame/ảnh tùy chỉnh và cấp quyền đọc chúng.",
            color = CamColors.Sub, fontSize = 10.sp
        )
    }
}

private fun createSettingsBackup(context: Context): JSONObject {
    val settings = context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE)
    val contents = context.getSharedPreferences("fujifilm_contents", Context.MODE_PRIVATE)
    return JSONObject().apply {
        put("format", BACKUP_FORMAT)
        put("version", BACKUP_VERSION)
        put("created_at", System.currentTimeMillis())
        put("device_model", android.os.Build.MODEL ?: "unknown")
        put("preferences", JSONObject().apply {
            put("fujifilm_settings", encodePreferences(settings))
            put("fujifilm_contents", encodePreferences(contents))
        })
    }
}

private fun encodePreferences(preferences: SharedPreferences): JSONObject {
    val json = JSONObject()
    preferences.all.toSortedMap().forEach { (key, value) ->
        val item = JSONObject()
        when (value) {
            is Boolean -> item.put("type", "boolean").put("value", value)
            is Int -> item.put("type", "int").put("value", value)
            is Long -> item.put("type", "long").put("value", value)
            is Float -> item.put("type", "float").put("value", value.toDouble())
            is String -> item.put("type", "string").put("value", value)
            is Set<*> -> item.put("type", "string_set").put(
                "value",
                JSONArray(value.mapNotNull { it as? String }.sorted())
            )
            else -> return@forEach
        }
        json.put(key, item)
    }
    return json
}

private fun restoreSettingsBackup(context: Context, backup: JSONObject) {
    require(backup.optString("format") == BACKUP_FORMAT) { "Không đúng định dạng Fujifilm Mode" }
    require(backup.optInt("version", -1) in 1..BACKUP_VERSION) { "Phiên bản sao lưu không được hỗ trợ" }
    val allPrefs = backup.optJSONObject("preferences") ?: error("Thiếu dữ liệu cài đặt")
    val settingsJson = allPrefs.optJSONObject("fujifilm_settings") ?: error("Thiếu cấu hình camera")
    val contentsJson = allPrefs.optJSONObject("fujifilm_contents") ?: JSONObject()

    val settings = context.getSharedPreferences("fujifilm_settings", Context.MODE_PRIVATE)
    val contents = context.getSharedPreferences("fujifilm_contents", Context.MODE_PRIVATE)
    val settingsEditor = settings.edit().clear()
    val contentsEditor = contents.edit().clear()
    decodePreferences(settingsJson, settingsEditor)
    decodePreferences(contentsJson, contentsEditor)
    check(settingsEditor.commit()) { "Không ghi được cấu hình camera" }
    check(contentsEditor.commit()) { "Không ghi được danh sách nội dung" }
}

private fun decodePreferences(json: JSONObject, editor: SharedPreferences.Editor) {
    val keys = json.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        val item = json.optJSONObject(key) ?: continue
        when (item.optString("type")) {
            "boolean" -> editor.putBoolean(key, item.optBoolean("value"))
            "int" -> editor.putInt(key, item.optInt("value"))
            "long" -> editor.putLong(key, item.optLong("value"))
            "float" -> editor.putFloat(key, item.optDouble("value").toFloat())
            "string" -> if (!item.isNull("value")) editor.putString(key, item.optString("value"))
            "string_set" -> {
                val array = item.optJSONArray("value") ?: JSONArray()
                val values = linkedSetOf<String>()
                for (index in 0 until array.length()) {
                    val value = array.optString(index, "")
                    if (value.isNotEmpty()) values.add(value)
                }
                editor.putStringSet(key, values)
            }
        }
    }
}
