package com.example.camui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ToolboxScreen(
    onBack: () -> Unit,
    showGrid: Boolean,
    onShowGrid: (Boolean) -> Unit,
    showHist: Boolean,
    onShowHist: (Boolean) -> Unit,
    focusPeaking: Boolean,
    onFocusPeaking: (Boolean) -> Unit
) {
    Column(Modifier.fillMaxSize().background(CamColors.Bg).padding(16.dp)) {
        PageHeader("Toolbox", onBack)
        Text("Viewfinder overlays", color = CamColors.Sub, fontSize = 13.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
        ToolToggle("Grid Lines", "Thirds composition guide", Icons.Default.GridOn, showGrid, onShowGrid)
        ToolToggle("Histogram", "Live luminance histogram", Icons.Default.ShowChart, showHist, onShowHist)
        ToolToggle("Focus Peaking", "Highlight high-contrast focus edges", Icons.Default.Tune,
            focusPeaking, onFocusPeaking)
    }
}

@Composable
fun ContentManagementScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val luts = remember { mutableStateListOf<String>() }
    val frames = remember { mutableStateListOf<String>() }

    val lutPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: uri.toString()
            luts.add(name)
        }
    }
    val framePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: uri.toString()
            frames.add(name)
        }
    }

    Column(Modifier.fillMaxSize().background(CamColors.Bg).padding(16.dp)) {
        PageHeader("Contents Management", onBack)
        Text("LUT Manage", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text("Nhập tệp .cube để quản lý hồ sơ màu.", color = CamColors.Sub, fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
        Button(onClick = { lutPicker.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text("Import LUT")
        }
        if (luts.isEmpty()) EmptyNote("Chưa có LUT nào được nhập.")
        else {
            LazyColumn(Modifier.height(118.dp)) {
                items(luts) { name -> ManagedItem(name, onDelete = { luts.remove(name) }) }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("Frame Manage", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text("Nhập khung phủ PNG/JPG để quản lý.", color = CamColors.Sub, fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
        Button(onClick = { framePicker.launch(arrayOf("image/png", "image/jpeg")) },
            modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text("Import Frame")
        }
        if (frames.isEmpty()) EmptyNote("Chưa có frame nào được nhập.")
        else {
            LazyColumn {
                items(frames) { name -> ManagedItem(name, onDelete = { frames.remove(name) }) }
            }
        }
    }
}

@Composable
private fun PageHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ToolToggle(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(16.dp))
            .background(CamColors.Card).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = CamColors.Yellow)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(description, color = CamColors.Sub, fontSize = 11.sp)
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun ManagedItem(name: String, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(10.dp))
            .background(CamColors.Card).padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Image, contentDescription = null, tint = CamColors.Sub)
        Text(name, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = CamColors.Sub)
        }
    }
}

@Composable
private fun EmptyNote(text: String) {
    Text(text, color = CamColors.Sub, fontSize = 12.sp, modifier = Modifier.padding(vertical = 10.dp))
}
