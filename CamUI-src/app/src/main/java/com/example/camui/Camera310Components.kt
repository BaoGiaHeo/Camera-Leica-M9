package com.example.camui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text

private val Fuji310Font = FontFamily(
    Font(R.font.saira_semicondensed_regular, FontWeight.Normal),
    Font(R.font.saira_semicondensed_medium, FontWeight.Medium),
    Font(R.font.saira_semicondensed_bold, FontWeight.Bold)
)

@Composable
internal fun Camera310ValueSlider(
    label: String,
    valueText: String,
    value: Float,
    enabled: Boolean = true,
    lightMode: Boolean = false,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth().height(34.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            label,
            color = if (lightMode) Color(0xFF262626) else Color(0xFFF2F2F2),
            fontFamily = Fuji310Font,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier.width(22.dp)
        )
        Slider(
            value = value.coerceIn(0f, 1f),
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            enabled = enabled,
            modifier = Modifier.weight(1f).height(26.dp)
        )
        Text(
            valueText,
            color = if (lightMode) Color(0xFF161616) else Color.White,
            fontFamily = Fuji310Font,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.width(48.dp)
        )
    }
}

@Composable
internal fun Camera310FilterDropdown(
    visible: Boolean,
    presets: List<Pair<String, String>>,
    activeFilter: String,
    lightMode: Boolean,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(120))
    ) {
        val surface = if (lightMode) Color(0xFFE8E8E8) else Color(0xFF222426)
        val rowSurface = if (lightMode) Color(0xFFF8F8F8) else Color(0xFF17191B)
        val foreground = if (lightMode) Color(0xFF191919) else Color(0xFFF2F2F2)
        Column(
            Modifier.fillMaxWidth()
                .background(surface, RoundedCornerShape(14.dp))
                .border(1.dp, if (lightMode) Color(0xFF777777) else Color(0xFF66696C), RoundedCornerShape(14.dp))
                .padding(6.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().height(27.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "CUỘN PHIM",
                    color = foreground,
                    fontFamily = Fuji310Font,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${presets.size + 1}/${presets.size + 1}",
                    color = if (lightMode) Color(0xFF3B6F4D) else Color(0xFF5ED08A),
                    fontFamily = Fuji310Font,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Box(
                    Modifier.size(28.dp).clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text("×", color = foreground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            LazyColumn(
                Modifier.fillMaxWidth().heightIn(max = 164.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                item(key = "None") {
                    Camera310FilterRow(
                        name = "Không (ảnh gốc)",
                        selected = activeFilter == "None",
                        lightMode = lightMode
                    ) { onSelect("None") }
                }
                items(presets, key = { it.first }) { (id, name) ->
                    Camera310FilterRow(
                        name = name,
                        selected = activeFilter == id,
                        lightMode = lightMode
                    ) { onSelect(id) }
                }
            }
        }
    }
}

@Composable
private fun Camera310FilterRow(
    name: String,
    selected: Boolean,
    lightMode: Boolean,
    onClick: () -> Unit
) {
    val foreground = if (lightMode) Color(0xFF161616) else Color(0xFFF2F2F2)
    Row(
        Modifier.fillMaxWidth().height(32.dp)
            .background(
                if (selected) {
                    if (lightMode) Color(0xFFFFF0CE) else Color(0xFF34302A)
                } else {
                    if (lightMode) Color(0xFFF7F7F7) else Color(0xFF17191B)
                },
                RoundedCornerShape(8.dp)
            )
            .border(
                if (selected) 1.5.dp else 0.6.dp,
                if (selected) Color(0xFFF0A030) else if (lightMode) Color(0xFFCBCBCB) else Color(0xFF3B3E40),
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(16.dp)
                .background(if (selected) Color(0xFFF0A030) else Color(0xFF85888A), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("F", color = Color(0xFF101010), fontFamily = Fuji310Font, fontSize = 9.sp, fontWeight = FontWeight.Black)
        }
        Text(
            name,
            color = foreground,
            fontFamily = Fuji310Font,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 7.dp)
        )
        if (selected) Text("✓", color = Color(0xFFF0A030), fontSize = 13.sp, fontWeight = FontWeight.Black)
    }
}
