package com.example.camui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxSize

object CamColors {
    var Bg by mutableStateOf(Color(0xFF121212))
    var Shutter by mutableStateOf(Color.White)
    var TextColor by mutableStateOf(Color.White)
    var Sub by mutableStateOf(Color(0xFF9E9E9E))
    val Card = Color(0xFF1E1E1E)
    val Glass = Color(0xD92A2A2A)
    val Panel = Color(0xF21A1A1A)
    val Chip = Color(0xFF2A2A2A)
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
    val shutterHex = prefs.getString("shutter_button_color", "#FFFFFFFF")
    SideEffect {
        val fixedBackground = Color(0xFF121212)
        CamColors.Bg = fixedBackground
        CamColors.TextColor = Color.White
        CamColors.Sub = Color(0xFF9E9E9E)
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
