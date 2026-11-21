package com.example.camui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
internal fun ActivationScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) }
    var selectedLanguage by remember { mutableStateOf(AppLanguageManager.currentLanguage) }
    val orange = FujiSkin.orange
    val border = Color(0xFF465154)
    val panel = Color(0xFF111719)
    val muted = Color(0xFF9CA7AA)

    LaunchedEffect(step) {
        if (step == 1) {
            delay(2_500L)
            if (step == 1) step = 2
        }
    }

    Box(
        Modifier.fillMaxSize().background(Color(0xFF050708)).padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.fillMaxWidth().widthIn(max = 920.dp)
                .border(1.dp, border, RoundedCornerShape(10.dp))
                .background(panel, RoundedCornerShape(10.dp))
                .padding(horizontal = 22.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "FUJIFILM MODE",
                color = Color(0xFFF2F3F2),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(3) { index ->
                    Box(
                        Modifier.size(7.dp)
                            .background(if (index == step) orange else Color(0xFF394347), CircleShape)
                    )
                }
            }
            Box(
                Modifier.fillMaxWidth().weight(1f)
                    .background(Color.Black, RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0xFF20282A), RoundedCornerShape(6.dp))
                    .clickable(enabled = step == 1) { step = 2 }
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                when (step) {
                    0 -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            stringResource(R.string.activation_step_language),
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            stringResource(R.string.activation_language_description),
                            color = muted,
                            fontSize = 12.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ActivationChoice(
                                label = stringResource(R.string.language_english),
                                selected = selectedLanguage == "en",
                                onClick = {
                                    selectedLanguage = "en"
                                    AppLanguageManager.setLanguage(context, "en")
                                }
                            )
                            ActivationChoice(
                                label = stringResource(R.string.language_vietnamese),
                                selected = selectedLanguage == "vi",
                                onClick = {
                                    selectedLanguage = "vi"
                                    AppLanguageManager.setLanguage(context, "vi")
                                }
                            )
                        }
                    }
                    1 -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            stringResource(R.string.activation_step_storage),
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            stringResource(R.string.activation_storage_description),
                            color = muted,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                        Text(
                            stringResource(R.string.activation_storage_continue),
                            color = orange,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    else -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            stringResource(R.string.activation_step_publisher),
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            stringResource(R.string.activation_publisher),
                            color = orange,
                            fontSize = 20.sp,
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
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step > 0) {
                    ActivationAction(
                        label = stringResource(R.string.activation_back),
                        selected = false,
                        onClick = { step = (step - 1).coerceAtLeast(0) }
                    )
                } else {
                    Spacer(Modifier.widthIn(min = 84.dp))
                }
                ActivationAction(
                    label = if (step == 2) stringResource(R.string.activation_ok)
                        else stringResource(R.string.activation_continue),
                    selected = true,
                    onClick = {
                        when (step) {
                            0 -> step = 1
                            1 -> step = 2
                            else -> onComplete()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ActivationChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.widthIn(min = 150.dp).border(
            1.dp,
            if (selected) FujiSkin.orange else Color(0xFF465154),
            RoundedCornerShape(6.dp)
        ).background(if (selected) Color(0xFF29231A) else Color(0xFF111719), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) FujiSkin.orange else Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ActivationAction(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.widthIn(min = 84.dp).border(
            1.dp,
            if (selected) FujiSkin.orange else Color(0xFF465154),
            RoundedCornerShape(5.dp)
        ).background(if (selected) Color(0xFF29231A) else Color(0xFF111719), RoundedCornerShape(5.dp))
            .clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) FujiSkin.orange else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
