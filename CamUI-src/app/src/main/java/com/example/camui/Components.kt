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
import androidx.compose.material3.Text as MaterialText
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.platform.LocalConfiguration
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


private val englishUi = mapOf(
    "Settings" to "Settings", "Toolbox" to "Toolbox", "Viewfinder overlays" to "Viewfinder overlays",
    "Grid Lines" to "Grid Lines", "Thirds composition guide" to "Thirds composition guide",
    "Histogram" to "Histogram", "Live luminance histogram" to "Live luminance histogram",
    "Focus Peaking" to "Focus Peaking", "Highlight high-contrast focus edges" to "Highlight high-contrast focus edges",
    "Watermark" to "Watermark", "Enable Watermark" to "Enable Watermark", "Shutter button" to "Shutter button",
    "Assistive Tools" to "Assistive Tools", "Focus & Lens" to "Focus & Lens",
    "Capture & Quality" to "Capture & Quality", "Photo Mode Setting" to "Photo Mode Setting",
    "Appearance" to "Appearance", "Language" to "Language", "About" to "About",
    "Photo" to "Photo", "Pro" to "Pro", "Bộ lọc" to "Filter", "AI Bố cục" to "AI Composition",
    "AI Bố cục ✓" to "AI Composition ✓", "Tắt frame" to "No frame",
    "Màu frame" to "Frame color", "Màu khung" to "Frame color",
    "Hiện thời gian" to "Show date and time", "Hiện model máy" to "Show device model",
    "Hiện thông số" to "Show camera settings", "Save Model" to "Save Model",
    "Import ảnh cho nút chụp" to "Import shutter image", "Đổi ảnh nút chụp" to "Change shutter image",
    "Đã chọn ảnh cho nút chụp." to "Shutter image selected.", "Dùng nút màu" to "Use solid color",
    "Phát hiện ngôn ngữ hệ thống" to "Detect system language", "English" to "English",
    "Vietnamese" to "Vietnamese", "Japan" to "Japanese", "Korea" to "Korean", "China" to "Chinese",
    "Đang phơi sáng" to "Exposing", "Vui lòng để điện thoại cố định" to "Keep the phone steady",
    "Không rung máy cho đến khi hoàn tất" to "Do not move the phone until capture finishes",
    "Đang phơi sáng vui lòng để điện thoại cố định" to "Exposure in progress. Please keep the phone steady.",
    "AI vẫn đang phát triển trong quá trình dùng chức năng có thể phát sinh lỗi hoặc không đúng như người dùng mong muốn"
        to "AI is still under development. Results may be inaccurate or unexpected, and errors may occur.",
    "Tắt frame" to "Off", "Classic Card" to "Classic Card", "Clean Corners" to "Clean Corners",
    "35mm Film" to "35mm Film", "Instant / Polaroid" to "Instant / Polaroid",
    "Center Logo" to "Center Logo", "Expo Around" to "Expo Around",
    "Expo Left" to "Expo Left", "Expo Right" to "Expo Right", "Double Line" to "Double Line"
)

private val japaneseUi = mapOf(
    "Settings" to "設定", "Toolbox" to "ツールボックス", "Viewfinder overlays" to "ファインダー表示",
    "Grid Lines" to "グリッド線", "Thirds composition guide" to "三分割構図ガイド",
    "Histogram" to "ヒストグラム", "Live luminance histogram" to "リアルタイム輝度ヒストグラム",
    "Focus Peaking" to "フォーカスピーキング", "Watermark" to "ウォーターマーク",
    "Enable Watermark" to "ウォーターマークを有効化", "Shutter button" to "シャッターボタン",
    "Assistive Tools" to "補助ツール", "Focus & Lens" to "フォーカスとレンズ",
    "Capture & Quality" to "撮影と画質", "Photo Mode Setting" to "写真モード設定",
    "Appearance" to "外観", "Language" to "言語", "About" to "アプリ情報",
    "Photo" to "写真", "Pro" to "プロ", "Bộ lọc" to "フィルター", "LUT" to "LUT",
    "AI Bố cục" to "AI構図", "AI Bố cục ✓" to "AI構図 ✓",
    "Màu frame" to "フレームの色", "Màu khung" to "枠線の色",
    "Hiện thời gian" to "日時を表示", "Hiện model máy" to "端末モデルを表示",
    "Hiện thông số" to "撮影情報を表示", "Save Model" to "モデルを保存",
    "Import ảnh cho nút chụp" to "シャッター画像を読み込む",
    "Đổi ảnh nút chụp" to "シャッター画像を変更", "Dùng nút màu" to "単色ボタンを使用",
    "Phát hiện ngôn ngữ hệ thống" to "システム言語を検出",
    "English" to "英語", "Vietnamese" to "ベトナム語", "Japan" to "日本語", "Korea" to "韓国語", "China" to "中国語",
    "Đang phơi sáng" to "露光中", "Vui lòng để điện thoại cố định" to "スマートフォンを動かさないでください",
    "Không rung máy cho đến khi hoàn tất" to "撮影が完了するまで動かさないでください",
    "Đang phơi sáng vui lòng để điện thoại cố định" to "露光中です。スマートフォンを固定してください。",
    "Photo Frame" to "フォトフレーム", "Tắt frame" to "フレームなし"
)

private val koreanUi = mapOf(
    "Settings" to "설정", "Toolbox" to "도구 상자", "Viewfinder overlays" to "뷰파인더 오버레이",
    "Grid Lines" to "격자선", "Thirds composition guide" to "삼분할 구도 가이드",
    "Histogram" to "히스토그램", "Live luminance histogram" to "실시간 밝기 히스토그램",
    "Focus Peaking" to "포커스 피킹", "Watermark" to "워터마크",
    "Enable Watermark" to "워터마크 사용", "Shutter button" to "셔터 버튼",
    "Assistive Tools" to "보조 도구", "Focus & Lens" to "초점 및 렌즈",
    "Capture & Quality" to "촬영 및 화질", "Photo Mode Setting" to "사진 모드 설정",
    "Appearance" to "화면 설정", "Language" to "언어", "About" to "정보",
    "Photo" to "사진", "Pro" to "프로", "Bộ lọc" to "필터", "LUT" to "LUT",
    "AI Bố cục" to "AI 구도", "AI Bố cục ✓" to "AI 구도 ✓",
    "Màu frame" to "프레임 색상", "Màu khung" to "테두리 색상",
    "Hiện thời gian" to "날짜 및 시간 표시", "Hiện model máy" to "기기 모델 표시",
    "Hiện thông số" to "촬영 정보 표시", "Save Model" to "모델 저장",
    "Import ảnh cho nút chụp" to "셔터 이미지 가져오기", "Đổi ảnh nút chụp" to "셔터 이미지 변경",
    "Dùng nút màu" to "단색 버튼 사용", "Phát hiện ngôn ngữ hệ thống" to "시스템 언어 감지",
    "English" to "영어", "Vietnamese" to "베트남어", "Japan" to "일본어", "Korea" to "한국어", "China" to "중국어",
    "Đang phơi sáng" to "노출 중", "Vui lòng để điện thoại cố định" to "휴대전화를 고정해 주세요",
    "Không rung máy cho đến khi hoàn tất" to "촬영이 끝날 때까지 움직이지 마세요",
    "Đang phơi sáng vui lòng để điện thoại cố định" to "노출 중입니다. 휴대전화를 움직이지 마세요.",
    "Photo Frame" to "사진 프레임", "Tắt frame" to "프레임 끄기"
)

private val chineseUi = mapOf(
    "Settings" to "设置", "Toolbox" to "工具箱", "Viewfinder overlays" to "取景器辅助线",
    "Grid Lines" to "网格线", "Thirds composition guide" to "三分构图辅助",
    "Histogram" to "直方图", "Live luminance histogram" to "实时亮度直方图",
    "Focus Peaking" to "峰值对焦", "Watermark" to "水印",
    "Enable Watermark" to "启用水印", "Shutter button" to "快门按钮",
    "Assistive Tools" to "辅助工具", "Focus & Lens" to "对焦与镜头",
    "Capture & Quality" to "拍摄与画质", "Photo Mode Setting" to "照片模式设置",
    "Appearance" to "外观", "Language" to "语言", "About" to "关于",
    "Photo" to "照片", "Pro" to "专业", "Bộ lọc" to "滤镜", "LUT" to "LUT",
    "AI Bố cục" to "AI 构图", "AI Bố cục ✓" to "AI 构图 ✓",
    "Màu frame" to "相框颜色", "Màu khung" to "边框颜色",
    "Hiện thời gian" to "显示日期和时间", "Hiện model máy" to "显示设备型号",
    "Hiện thông số" to "显示拍摄参数", "Save Model" to "保存型号",
    "Import ảnh cho nút chụp" to "导入快门图片", "Đổi ảnh nút chụp" to "更换快门图片",
    "Dùng nút màu" to "使用纯色按钮", "Phát hiện ngôn ngữ hệ thống" to "跟随系统语言",
    "English" to "英语", "Vietnamese" to "越南语", "Japan" to "日语", "Korea" to "韩语", "China" to "中文",
    "Đang phơi sáng" to "正在曝光", "Vui lòng để điện thoại cố định" to "请保持手机稳定",
    "Không rung máy cho đến khi hoàn tất" to "拍摄完成前请勿移动手机",
    "Đang phơi sáng vui lòng để điện thoại cố định" to "正在曝光，请保持手机稳定。",
    "Photo Frame" to "照片边框", "Tắt frame" to "关闭边框"
)

private fun translateUiText(value: String, language: String): String {
    if (language == "vi" || language == "system") return value
    val dictionary = when {
        language.startsWith("ja") -> japaneseUi
        language.startsWith("ko") -> koreanUi
        language.startsWith("zh") -> chineseUi
        else -> englishUi
    }
    dictionary[value]?.let { return it }
    // Preserve dynamic values (camera IDs, EV/ISO numbers and timer counts) while translating common labels.
    var translated = value
    val fragments = when {
        language.startsWith("ja") -> listOf(
            "Camera ID " to "カメラID ", "ISO " to "ISO ", "EV" to "露出補正",
            "Không thể " to "", "Đã lưu" to "保存しました"
        )
        language.startsWith("ko") -> listOf(
            "Camera ID " to "카메라 ID ", "ISO " to "ISO ", "EV" to "노출 보정",
            "Không thể " to "", "Đã lưu" to "저장됨"
        )
        language.startsWith("zh") -> listOf(
            "Camera ID " to "相机 ID ", "ISO " to "ISO ", "EV" to "曝光补偿",
            "Không thể " to "", "Đã lưu" to "已保存"
        )
        else -> listOf(
            "Camera ID " to "Camera ID ", "Bộ lọc" to "Filter",
            "Chưa có ảnh" to "No photos yet", "Đang tải" to "Loading"
        )
    }
    fragments.forEach { (from, to) -> translated = translated.replace(from, to) }
    return translated
}

/**
 * Central localized Text wrapper for the camera UI. A selected app language is read
 * from preferences; AppCompat applies the same locale to the activity/resources.
 */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val preference = context.getSharedPreferences("fujifilm_settings", 0)
        .getString("app_language", "system") ?: "system"
    val language = if (preference == "system") configuration.locales[0].language else preference
    MaterialText(
        text = translateUiText(text, language),
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}
