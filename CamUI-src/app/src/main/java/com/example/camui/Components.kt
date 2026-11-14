package com.example.camui

import android.net.Uri
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CamColors {
    var Bg by mutableStateOf(Color(0xFF121212))
    var Shutter by mutableStateOf(Color.White)
    var TextColor by mutableStateOf(Color.White)
    var Sub by mutableStateOf(Color(0xFF9E9E9E))
    var BackgroundImageUri by mutableStateOf<String?>(null)
    var ImageBackgroundEnabled by mutableStateOf(false)

    // Keep controls readable without hiding a user-selected photo background.
    val Card get() = if (ImageBackgroundEnabled) Color(0x8F101010) else Color(0xFF1E1E1E)
    val Glass get() = if (ImageBackgroundEnabled) Color(0x882A2A2A) else Color(0xD92A2A2A)
    val Panel get() = if (ImageBackgroundEnabled) Color(0xB51A1A1A) else Color(0xF21A1A1A)
    val Chip get() = if (ImageBackgroundEnabled) Color(0x882A2A2A) else Color(0xFF2A2A2A)
    val Yellow = Color(0xFFF5C842)
    val Stroke = Color(0x33FFFFFF)
}

private fun parseAppColor(value: String?, fallback: Color): Color {
    return runCatching { value?.let { Color(android.graphics.Color.parseColor(it)) } ?: fallback }
        .getOrDefault(fallback)
}

/** Ratio values are width / height in portrait orientation. */
val aspectRatios = listOf(
    "3:2" to (2f / 3f),
    "4:3" to (3f / 4f),
    "16:9" to (9f / 16f),
    "1:1" to 1f,
    "21:9" to (9f / 21f)
)

@Composable
fun CamTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("fujifilm_settings", 0)
    val bgHex = prefs.getString("app_background_color", "#121212")
    var initialized by androidx.compose.runtime.remember { mutableStateOf(false) }
    if (!initialized) {
        SideEffect {
            CamColors.BackgroundImageUri = prefs.getString("app_background_image_uri", null)
            initialized = true
        }
    }
    val bgUriText = CamColors.BackgroundImageUri
    val shutterHex = prefs.getString("shutter_button_color", "#FFFFFFFF")
    val bgBitmap by produceState<ImageBitmap?>(initialValue = null, bgUriText) {
        value = if (bgUriText.isNullOrBlank()) null else withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.loadThumbnail(Uri.parse(bgUriText), Size(2048, 2048), null).asImageBitmap()
            }.onFailure {
                android.util.Log.e("FUJI_BACKGROUND", "Could not load selected app background", it)
            }.getOrNull()
        }
    }
    SideEffect {
        val chosenBg = parseAppColor(bgHex, Color(0xFF121212))
        val isLightBackground = bgBitmap == null && (
            android.graphics.Color.red(chosenBg.toArgb()) * 0.299f +
                android.graphics.Color.green(chosenBg.toArgb()) * 0.587f +
                android.graphics.Color.blue(chosenBg.toArgb()) * 0.114f
            ) > 185f
        CamColors.ImageBackgroundEnabled = bgBitmap != null
        // A selected photo becomes the shared background for camera, settings, tools,
        // contents management and album screens. Translucent card tokens let it remain visible.
        CamColors.Bg = if (bgBitmap != null) Color.Transparent else chosenBg
        CamColors.TextColor = if (isLightBackground) Color(0xFF171717) else Color.White
        CamColors.Sub = if (isLightBackground) Color(0xFF626262) else Color(0xFF9E9E9E)
        CamColors.Shutter = parseAppColor(shutterHex, Color.White)
    }
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = CamColors.Yellow,
            onPrimary = Color.Black,
            background = CamColors.Bg,
            surface = CamColors.Card
        )
    ) {
        Box(Modifier.fillMaxSize().background(CamColors.Bg)) {
            if (bgBitmap != null) {
                Image(
                    bitmap = bgBitmap!!,
                    contentDescription = "App background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            content()
        }
    }
}

@Composable
fun CircleBtn(
    icon: ImageVector,
    desc: String,
    onClick: () -> Unit,
    size: Dp = 46.dp,
    tint: Color = Color.White
) {
    Box(
        Modifier.size(size).clip(CircleShape).background(CamColors.Glass)
            .border(0.5.dp, CamColors.Stroke, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = desc, tint = tint)
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.height(38.dp).clip(RoundedCornerShape(12.dp))
            .background(if (selected) CamColors.Yellow else CamColors.Chip).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (selected) Color.Black else Color.White,
            fontWeight = FontWeight.SemiBold, fontSize = 11.sp, maxLines = 1)
    }
}
