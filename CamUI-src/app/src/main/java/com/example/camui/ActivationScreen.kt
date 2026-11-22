package com.example.camui

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt

private data class ActivationStorageSnapshot(
    val usedBytes: Long,
    val freeBytes: Long,
    val totalBytes: Long
)

private data class ActivationBatterySnapshot(
    val percent: Int,
    val charging: Boolean
)

@Composable
internal fun ActivationScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) }
    var selectedLanguage by remember { mutableStateOf(AppLanguageManager.currentLanguage) }
    var storage by remember { mutableStateOf(readActivationStorage()) }
    var battery by remember { mutableStateOf(readActivationBattery(context)) }
    var lcdPoweredOn by remember { mutableStateOf(false) }

    val orange = FujiSkin.orange
    val white = Color(0xFFF2F2F2)
    val border = Color(0xFF7A8588)
    val muted = Color(0xFF9CA7AA)
    val red = Color(0xFFFF4A3D)
    val lcdAlpha by animateFloatAsState(
        targetValue = if (lcdPoweredOn) 1f else 0f,
        animationSpec = tween(400),
        label = "activation LCD power on"
    )

    LaunchedEffect(Unit) {
        lcdPoweredOn = true
    }

    LaunchedEffect(step) {
        if (step == 1) {
            storage = readActivationStorage()
            battery = readActivationBattery(context)
            delay(2_500L)
            if (step == 1) step = 2
        }
    }

    BackHandler(enabled = true) {
        if (step > 0) step = 0
    }

    val freeSpaceLow = storage.freeBytes < 1_073_741_824L
    val batteryLow = battery.percent < 20

    Box(
        Modifier.fillMaxSize().background(Color(0xFF050708)).padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.fillMaxSize().widthIn(max = 1100.dp)
                .border(1.dp, border, Color.Black)
                .background(Color(0xFF07090A))
                .padding(horizontal = 24.dp, vertical = 14.dp)
                .graphicsLayer { alpha = lcdAlpha },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.activation_title),
                color = white,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(3) { index ->
                    Box(
                        Modifier.size(7.dp).background(
                            if (index == step) orange else Color(0xFF394347),
                            CircleShape
                        )
                    )
                }
            }
            AnimatedContent(
                targetState = step,
                modifier = Modifier.fillMaxWidth().weight(1f),
                transitionSpec = {
                    (fadeIn(tween(250)) + slideInHorizontally(tween(250)) { it / 8 }) togetherWith
                        (fadeOut(tween(250)) + slideOutHorizontally(tween(250)) { -it / 8 })
                },
                label = "activation step transition"
            ) { visibleStep ->
                Box(
                    Modifier.fillMaxSize()
                        .background(Color.Black)
                        .border(1.dp, Color(0xFF30383A))
                        .clickable(enabled = visibleStep == 1) {
                            if (step == 1) step = 2
                        }
                        .padding(horizontal = 22.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (visibleStep) {
                        0 -> Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                stringResource(R.string.activation_step_language),
                                color = white,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                stringResource(R.string.activation_language_description),
                                color = muted,
                                fontSize = 12.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                ActivationChoice(
                                    label = stringResource(R.string.language_english),
                                    selected = selectedLanguage == "en",
                                    onClick = {
                                        selectedLanguage = "en"
                                        AppLanguageManager.setLanguage(context, "en")
                                        step = 1
                                    }
                                )
                                ActivationChoice(
                                    label = stringResource(R.string.language_vietnamese),
                                    selected = selectedLanguage == "vi",
                                    onClick = {
                                        selectedLanguage = "vi"
                                        AppLanguageManager.setLanguage(context, "vi")
                                        step = 1
                                    }
                                )
                            }
                        }
                        1 -> {
                            val usedText = formatActivationBytes(storage.usedBytes)
                            val freeText = formatActivationBytes(storage.freeBytes)
                            val totalText = formatActivationBytes(storage.totalBytes)
                            Column(
                                Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    stringResource(R.string.activation_step_storage),
                                    color = white,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                        Text(
                                            stringResource(R.string.activation_storage_used, usedText),
                                            color = white,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        ActivationStorageBar(
                                            usedFraction = if (storage.totalBytes > 0L) {
                                                storage.usedBytes.toFloat() / storage.totalBytes
                                            } else 0f,
                                            warning = freeSpaceLow
                                        )
                                        Text(
                                            stringResource(R.string.activation_storage_free, freeText),
                                            color = if (freeSpaceLow) red else muted,
                                            fontSize = 13.sp,
                                            fontWeight = if (freeSpaceLow) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            stringResource(R.string.activation_storage_total, totalText),
                                            color = muted,
                                            fontSize = 11.sp
                                        )
                                        if (freeSpaceLow) {
                                            Text(
                                                stringResource(R.string.activation_warning_storage),
                                                color = red,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(1.dp).fillMaxHeight().background(Color(0xFF384245)))
                                    Column(
                                        Modifier.weight(0.72f),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        ActivationBatteryIcon(
                                            percent = battery.percent,
                                            charging = battery.charging,
                                            warning = batteryLow
                                        )
                                        Text(
                                            stringResource(R.string.activation_battery_level, battery.percent),
                                            color = if (batteryLow && !battery.charging) red else white,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (batteryLow && !battery.charging) {
                                            Text(
                                                stringResource(R.string.activation_warning_battery),
                                                color = red,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Text(
                                    stringResource(R.string.activation_storage_continue),
                                    color = orange,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        else -> Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                stringResource(R.string.activation_step_publisher),
                                color = white,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                stringResource(R.string.activation_publisher),
                                color = orange,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                stringResource(R.string.activation_publisher_note),
                                color = muted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().height(48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (step) {
                    0 -> Spacer(Modifier.width(92.dp))
                    1 -> ActivationAction(
                        label = stringResource(R.string.activation_back),
                        selected = false,
                        onClick = { step = 0 }
                    )
                    else -> ActivationAction(
                        label = stringResource(R.string.activation_back),
                        selected = false,
                        onClick = { step = 0 }
                    )
                }
                when (step) {
                    0 -> Spacer(Modifier.width(92.dp))
                    1 -> Spacer(Modifier.width(92.dp))
                    else -> ActivationAction(
                        label = stringResource(R.string.activation_ok),
                        selected = true,
                        onClick = onComplete
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivationChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.widthIn(min = 164.dp).border(
            1.dp,
            if (selected) FujiSkin.orange else Color(0xFF465154)
        ).background(if (selected) Color(0xFF29231A) else Color(0xFF111719))
            .clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) FujiSkin.orange else Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ActivationAction(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.widthIn(min = 92.dp).border(
            1.dp,
            if (selected) FujiSkin.orange else Color(0xFF465154)
        ).background(if (selected) Color(0xFF29231A) else Color(0xFF111719))
            .clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) FujiSkin.orange else Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ActivationStorageBar(usedFraction: Float, warning: Boolean) {
    val segments = 24
    val used = (usedFraction.coerceIn(0f, 1f) * segments).roundToInt().coerceIn(0, segments)
    Row(
        Modifier.fillMaxWidth().height(16.dp).border(1.dp, Color(0xFF596467)).padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(segments) { index ->
            Box(
                Modifier.weight(1f).fillMaxHeight().background(
                    if (index < used) {
                        if (warning) Color(0xFFFF4A3D) else FujiSkin.orange
                    } else Color(0xFF273033)
                )
            )
        }
    }
}

@Composable
private fun ActivationBatteryIcon(percent: Int, charging: Boolean, warning: Boolean) {
    val color = if (warning && !charging) Color(0xFFFF4A3D) else FujiSkin.orange
    Row(
        Modifier.width(120.dp).height(48.dp).border(2.dp, color).padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val cells = (percent.coerceIn(0, 100) / 25f).roundToInt().coerceIn(0, 4)
        repeat(4) { index ->
            Box(
                Modifier.weight(1f).fillMaxHeight()
                    .background(if (index < cells) color else Color(0xFF273033))
            )
        }
    }
    if (charging) {
        Text("⚡", color = color, fontSize = 12.sp)
    }
}

private fun readActivationStorage(): ActivationStorageSnapshot = runCatching {
    val stat = StatFs(Environment.getDataDirectory().absolutePath)
    val total = stat.totalBytes.coerceAtLeast(0L)
    val free = stat.availableBytes.coerceIn(0L, total)
    ActivationStorageSnapshot(
        usedBytes = (total - free).coerceAtLeast(0L),
        freeBytes = free,
        totalBytes = total
    )
}.getOrDefault(ActivationStorageSnapshot(0L, 0L, 0L))

private fun readActivationBattery(context: Context): ActivationBatterySnapshot {
    val intent = runCatching {
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }.getOrNull()
    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        ?: BatteryManager.BATTERY_STATUS_UNKNOWN
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
    val percent = if (level >= 0 && scale > 0) {
        (level * 100f / scale).roundToInt().coerceIn(0, 100)
    } else {
        0
    }
    return ActivationBatterySnapshot(percent, charging)
}

private fun formatActivationBytes(bytes: Long): String {
    val gib = bytes / 1_073_741_824.0
    return when {
        gib >= 10.0 -> String.format(Locale.US, "%.0f GB", gib)
        gib >= 1.0 -> String.format(Locale.US, "%.1f GB", gib)
        else -> String.format(Locale.US, "%.0f MB", bytes / 1_048_576.0)
    }
}
