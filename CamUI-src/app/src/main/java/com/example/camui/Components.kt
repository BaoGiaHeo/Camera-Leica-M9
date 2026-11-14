package com.example.camui

import android.content.Context
import android.widget.Toast

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
    val context = LocalContext.current
    Box(
        Modifier.size(size).clip(CircleShape).background(CamColors.Glass)
            .border(0.5.dp, CamColors.Stroke, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = localizedUiText(context, desc), tint = tint)
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
    "Expo Left" to "Expo Left", "Expo Right" to "Expo Right", "Double Line" to "Double Line",
    "Các công cụ này điều khiển trực tiếp lớp phủ trên kính ngắm." to "These tools control the live viewfinder overlays.",
    "Thông tin camera do Camera2 đọc trực tiếp từ thiết bị." to "Camera information is read directly from the device through Camera2.",
    "Không đọc được Camera ID. Hãy cấp quyền camera cho ứng dụng." to "Could not read Camera IDs. Grant camera permission to the app.",
    "Aspect Ratio" to "Aspect Ratio", "Image Quality" to "Image Quality",
    "Ảnh được lưu vào Pictures/FujifilmMode." to "Photos are saved to Pictures/FujifilmMode.",
    "Capture Quality" to "Capture Quality", "Noise Reduction" to "Noise Reduction",
    "Shutter" to "Shutter", "Hiệu ứng khi bấm chụp" to "Shutter effect",
    "Mẫu watermark · chọn riêng từng kiểu" to "Watermark templates · select a style",
    "Màu khung" to "Frame color", "Model tùy chỉnh" to "Custom model",
    "Auto sẽ lấy tên model của điện thoại: " to "Auto uses the phone model: ",
    "Tên model tùy chỉnh được lưu trong dữ liệu ứng dụng và giữ nguyên cho đến khi xóa dữ liệu hoặc gỡ ứng dụng."
        to "The custom model name is stored in app data until app data is cleared or the app is uninstalled.",
    "Watermark được đóng trực tiếp lên ảnh sau khi chụp. Thông số mm, khẩu độ, shutter và ISO lấy từ camera đang hoạt động khi HAL có cung cấp."
        to "The watermark is embedded after capture. Focal length, aperture, shutter and ISO are read from the active camera when exposed by the HAL.",
    "Đổi màu nút chụp hoặc dùng ảnh PNG/JPG làm mặt nút." to "Change the shutter button color or use a PNG/JPG image.",
    "Đã chọn ảnh cho nút chụp." to "Shutter image selected.",
    "Chọn ngôn ngữ hiển thị. Ứng dụng sẽ tải lại giao diện sau khi đổi." to "Choose the interface language. The app will reload after the change.",
    "Tắt sẽ yêu cầu camera không khử nhiễu; mức hỗ trợ thực tế tùy Camera2 HAL." to "Off requests no camera noise reduction; actual support depends on the Camera2 HAL.",
    "Best sử dụng mức nén JPEG cao nhất; độ phân giải thực tế phụ thuộc độ phân giải mà Camera2/CameraX cung cấp cho camera đang chọn."
        to "Best uses the highest JPEG quality. Actual resolution depends on the output sizes exposed by the active Camera2/CameraX camera.",
    "Người làm App: Baoaka06" to "Developer: Baoaka06",
    "Frame Manage" to "Frame Manager", "Nhập khung phủ PNG/JPG để quản lý." to "Import PNG/JPG overlay frames.",
    "Chưa có frame nào được nhập." to "No frames imported yet.",
    "Đang tải ảnh…" to "Loading photo…", "Đang tải ảnh..." to "Loading photo...",
    "Đóng" to "Close", "Chọn frame để xem trước và đóng lên ảnh sau khi chụp." to "Select a frame preview; it will be embedded after capture.",
    "Frame dựng sẵn" to "Built-in frame", "Imported frame" to "Imported frame",
    "PNG trong suốt khuyên dùng · JPG có thể che ảnh" to "Transparent PNG recommended · JPG may cover the photo",
    "Transparent: bỏ viền và panel nền; các chi tiết chữ/ảnh PNG vẫn được giữ." to "Transparent removes the frame border and backing panel; text and PNG artwork remain.",
    "Áp dụng frame" to "Apply frame",
    "Lens calibration · EIS+ · sensor diagnostics" to "Lens calibration · EIS+ · sensor diagnostics",
    "Fujifilm Mode (app-debug)" to "Fujifilm Mode (app-debug)",
    "Version 1.1 experimental · Debug build" to "Version 1.1 experimental · Debug build",
    "Chưa có ảnh trong Pictures/FujifilmMode." to "No photos in Pictures/FujifilmMode yet.",
    "Không rung máy cho đến khi hoàn tất" to "Do not move the phone until capture finishes."
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
    "Photo Frame" to "フォトフレーム", "Tắt frame" to "フレームなし",
    "Các công cụ này điều khiển trực tiếp lớp phủ trên kính ngắm." to "これらのツールでファインダーの表示を操作します。",
    "Thông tin camera do Camera2 đọc trực tiếp từ thiết bị." to "カメラ情報はCamera2を通じて端末から直接取得します。",
    "Không đọc được Camera ID. Hãy cấp quyền camera cho ứng dụng." to "カメラIDを取得できません。カメラ権限を許可してください。",
    "Aspect Ratio" to "アスペクト比", "Image Quality" to "画質",
    "Ảnh được lưu vào Pictures/FujifilmMode." to "写真はPictures/FujifilmModeに保存されます。",
    "Capture Quality" to "撮影画質", "Noise Reduction" to "ノイズ低減",
    "Shutter" to "シャッター", "Hiệu ứng khi bấm chụp" to "シャッター効果",
    "Mẫu watermark · chọn riêng từng kiểu" to "ウォーターマークのテンプレート",
    "Model tùy chỉnh" to "カスタムモデル",
    "Tên model tùy chỉnh được lưu trong dữ liệu ứng dụng và giữ nguyên cho đến khi xóa dữ liệu hoặc gỡ ứng dụng。"
        to "カスタムモデル名はアプリデータを消去するかアプリをアンインストールするまで保存されます。",
    "Đổi màu nút chụp hoặc dùng ảnh PNG/JPG làm mặt nút." to "シャッターボタンの色を変更するか、PNG/JPG画像を使用します。",
    "Chọn ngôn ngữ hiển thị. Ứng dụng sẽ tải lại giao diện sau khi đổi." to "表示言語を選択します。変更後、アプリの画面を再読み込みします。",
    "Tắt sẽ yêu cầu camera không khử nhiễu; mức hỗ trợ thực tế tùy Camera2 HAL." to "オフにするとカメラのノイズ低減を要求しません。対応状況はCamera2 HALによって異なります。",
    "Đổi màu nút chụp hoặc dùng ảnh PNG/JPG làm mặt nút。" to "シャッターボタンの色を変更するか、PNG/JPG画像を使用します。",
    "Frame Manage" to "フレーム管理", "Nhập khung phủ PNG/JPG để quản lý." to "PNG/JPGのオーバーレイフレームを読み込みます。",
    "Đóng" to "閉じる", "Áp dụng frame" to "フレームを適用",
    "Không rung máy cho đến khi hoàn tất" to "撮影が完了するまでスマートフォンを動かさないでください。",
    "Không đọc được Camera ID. Hãy cấp quyền camera cho ứng dụng。" to "カメラIDを取得できません。カメラ権限を許可してください。"
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
    "Photo Frame" to "사진 프레임", "Tắt frame" to "프레임 끄기",
    "Các công cụ này điều khiển trực tiếp lớp phủ trên kính ngắm." to "이 도구에서 뷰파인더 오버레이를 제어합니다.",
    "Thông tin camera do Camera2 đọc trực tiếp từ thiết bị." to "카메라 정보는 Camera2를 통해 기기에서 직접 가져옵니다.",
    "Không đọc được Camera ID. Hãy cấp quyền camera cho ứng dụng." to "카메라 ID를 읽을 수 없습니다. 카메라 권한을 허용하세요.",
    "Aspect Ratio" to "화면 비율", "Image Quality" to "이미지 품질",
    "Ảnh được lưu vào Pictures/FujifilmMode." to "사진은 Pictures/FujifilmMode에 저장됩니다.",
    "Capture Quality" to "촬영 품질", "Noise Reduction" to "노이즈 감소",
    "Shutter" to "셔터", "Hiệu ứng khi bấm chụp" to "셔터 효과",
    "Mẫu watermark · chọn riêng từng kiểu" to "워터마크 템플릿",
    "Model tùy chỉnh" to "사용자 지정 모델",
    "Tên model tùy chỉnh được lưu trong dữ liệu ứng dụng và giữ nguyên cho đến khi xóa dữ liệu hoặc gỡ ứng dụng."
        to "사용자 지정 모델 이름은 앱 데이터를 삭제하거나 앱을 제거할 때까지 저장됩니다.",
    "Đổi màu nút chụp hoặc dùng ảnh PNG/JPG làm mặt nút." to "셔터 버튼 색상을 변경하거나 PNG/JPG 이미지를 사용하세요.",
    "Chọn ngôn ngữ hiển thị. Ứng dụng sẽ tải lại giao diện sau khi đổi." to "표시 언어를 선택하세요. 변경 후 앱 화면이 다시 로드됩니다.",
    "Tắt sẽ yêu cầu camera không khử nhiễu; mức hỗ trợ thực tế tùy Camera2 HAL." to "끄면 카메라에 노이즈 감소를 끄도록 요청합니다. 지원 여부는 Camera2 HAL에 따라 다릅니다.",
    "Frame Manage" to "프레임 관리", "Nhập khung phủ PNG/JPG để quản lý." to "PNG/JPG 오버레이 프레임을 가져옵니다.",
    "Đóng" to "닫기", "Áp dụng frame" to "프레임 적용",
    "Không rung máy cho đến khi hoàn tất" to "촬영이 끝날 때까지 휴대전화를 움직이지 마세요."
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
    "Photo Frame" to "照片边框", "Tắt frame" to "关闭边框",
    "Các công cụ này điều khiển trực tiếp lớp phủ trên kính ngắm." to "这些工具用于控制取景器辅助显示。",
    "Thông tin camera do Camera2 đọc trực tiếp từ thiết bị." to "相机信息通过 Camera2 从设备直接读取。",
    "Không đọc được Camera ID. Hãy cấp quyền camera cho ứng dụng." to "无法读取相机 ID，请授予应用相机权限。",
    "Aspect Ratio" to "画面比例", "Image Quality" to "图像质量",
    "Ảnh được lưu vào Pictures/FujifilmMode." to "照片保存到 Pictures/FujifilmMode。",
    "Capture Quality" to "拍摄质量", "Noise Reduction" to "降噪",
    "Shutter" to "快门", "Hiệu ứng khi bấm chụp" to "快门效果",
    "Mẫu watermark · chọn riêng từng kiểu" to "水印模板",
    "Model tùy chỉnh" to "自定义型号",
    "Tên model tùy chỉnh được lưu trong dữ liệu ứng dụng và giữ nguyên cho đến khi xóa dữ liệu hoặc gỡ ứng dụng."
        to "自定义型号会保存在应用数据中，直到清除数据或卸载应用。",
    "Đổi màu nút chụp hoặc dùng ảnh PNG/JPG làm mặt nút." to "更改快门按钮颜色，或使用 PNG/JPG 图片。",
    "Chọn ngôn ngữ hiển thị. Ứng dụng sẽ tải lại giao diện sau khi đổi." to "选择界面语言。更改后应用将重新加载界面。",
    "Tắt sẽ yêu cầu camera không khử nhiễu; mức hỗ trợ thực tế tùy Camera2 HAL." to "关闭会请求相机停用降噪，实际支持情况取决于 Camera2 HAL。",
    "Frame Manage" to "边框管理", "Nhập khung phủ PNG/JPG để quản lý." to "导入 PNG/JPG 覆盖边框。",
    "Đóng" to "关闭", "Áp dụng frame" to "应用边框",
    "Không rung máy cho đến khi hoàn tất" to "拍摄完成前请勿移动手机。"
)


private val fullEnglishUi = mapOf(
    "Cần cấp quyền Camera\n(chạm để cấp)" to "Camera permission required\n(tap to grant)",
    "AF LOCK" to "AF LOCK", "EV không được hỗ trợ" to "EV unavailable",
    "AF tự động" to "AF Auto", "Ấm" to "Warm", "Lạnh" to "Cool",
    "ISO Auto" to "ISO Auto", "Shutter Auto" to "Shutter Auto",
    "Focus locked" to "Focus locked", "Tông màu tự động" to "Auto color temperature",
    "LENS TRANSITION" to "LENS TRANSITION", "FILM WASH" to "FILM WASH",
    "Không dùng bộ lọc · Ảnh gốc" to "No filter · Original photo",
    "Mở Contents Management" to "Open Contents Management",
    "Nhấn để chọn áp dụng sau khi chụp" to "Tap to select and apply after capture",
    "✓ ĐANG DÙNG" to "✓ ACTIVE", "Xem trước · áp dụng ảnh" to "Preview · applied to photo",
    "Settings" to "Settings", "Toolbox" to "Toolbox", "Contents" to "Contents",
    "Metering" to "Metering", "Render Engine" to "Render Engine", "Ultra HDR" to "Ultra HDR",
    "Stabilization" to "Stabilization", "Settings" to "Settings",
    "Photo" to "Photo", "Pro" to "Pro", "Histogram" to "Histogram",
    "Viewfinder overlays" to "Viewfinder overlays", "Thirds composition guide" to "Thirds composition guide",
    "Live luminance histogram" to "Live luminance histogram", "Highlight high-contrast focus edges" to "Highlight high-contrast focus edges",
    "Khung ảnh phong cách rangefinder, không có logo thương hiệu" to "Rangefinder-style photo frame without a brand logo",
    "Mẫu watermark · chọn riêng từng kiểu" to "Watermark templates · choose a style",
    "EXIF cổ điển" to "Classic EXIF", "Viền bo + khoảng trống" to "Rounded border + margin",
    "Nền mờ, thông số nổi" to "Soft background with prominent settings",
    "Thông số góc phải" to "Settings in the right corner", "Ngày chụp tối giản" to "Minimal capture date",
    "Logo ở chính giữa" to "Centered logo",
    "Hiện thời gian" to "Show date and time", "Bật/tắt ngày và giờ trên watermark" to "Show or hide date and time on the watermark",
    "Hiện model máy" to "Show device model", "Bật/tắt tên model điện thoại" to "Show or hide the phone model",
    "Hiện thông số" to "Show camera settings", "Bật/tắt tiêu cự, khẩu độ, tốc độ màn trập và ISO" to "Show or hide focal length, aperture, shutter speed and ISO",
    "Model tùy chỉnh" to "Custom model", "Custom Model (để trống = Auto)" to "Custom model (blank = Auto)",
    "Auto sẽ lấy tên model của điện thoại: " to "Auto uses the phone model: ",
    "Save Model" to "Save Model", "Tên model tùy chỉnh được lưu trong dữ liệu ứng dụng và giữ nguyên cho đến khi xóa dữ liệu hoặc gỡ ứng dụng." to "The custom model name remains in app data until app data is cleared or the app is uninstalled.",
    "Watermark được đóng trực tiếp lên ảnh sau khi chụp. Thông số mm, khẩu độ, shutter và ISO lấy từ camera đang hoạt động khi HAL có cung cấp." to "The watermark is embedded after capture. Focal length, aperture, shutter and ISO are read from the active camera when the HAL provides them.",
    "Hiệu ứng khi bấm chụp" to "Shutter effect", "Rửa film" to "Film Wash", "Chớp nhanh" to "Quick Flash",
    "Âm thanh màn trập" to "Shutter sound", "Dùng âm click chụp ảnh hệ thống của Android" to "Use Android's system camera shutter sound",
    "Photo Frame" to "Photo Frame", "Chọn frame tại đây. Frame được đóng lên ảnh sau khi chụp; không còn nút Frame ở màn hình chụp." to "Choose frames here. The selected frame is embedded after capture; there is no Frame button on the camera screen.",
    "Màu frame" to "Frame color", "Transparent bỏ viền và panel của frame dựng sẵn; với frame PNG sẽ giữ nguyên độ trong suốt gốc." to "Transparent removes built-in frame borders and panels; imported PNG transparency is preserved.",
    "Muốn thêm frame PNG/JPG, vào Contents Management → Frame Manage → Import Frame." to "To add a PNG/JPG frame, open Contents Management → Frame Manager → Import Frame.",
    "PNG / JPG" to "PNG / JPG", "ORIGINAL" to "ORIGINAL", "Imported frame" to "Imported frame",
    "Contents Management" to "Contents Management", "Frame Manage" to "Frame Manager",
    "Nhập khung phủ PNG/JPG để quản lý." to "Import PNG/JPG overlay frames.",
    "Import Frame" to "Import Frame", "Xóa" to "Delete", "Hủy" to "Cancel",
    "Album · Fujifilm Mode" to "Album · Fujifilm Mode",
    "Đang tải ảnh…" to "Loading photos…", "Đang tải ảnh..." to "Loading photos...",
    "Đóng" to "Close", "Photo Frames" to "Photo Frames",
    "Chọn frame để xem trước và đóng lên ảnh sau khi chụp." to "Select a frame preview; it will be embedded after capture.",
    "Frame color" to "Frame color", "Frame dựng sẵn" to "Built-in frame",
    "Transparent: bỏ viền và panel nền; các chi tiết chữ/ảnh PNG vẫn được giữ." to "Transparent removes the frame border and backing panel; text and PNG artwork remain.",
    "Áp dụng frame" to "Apply frame",
    "Japan = 日本語 · Korea = 한국어 · China = 简体中文" to "Japanese = 日本語 · Korean = 한국어 · Chinese = 简体中文",
    "Language" to "Language", "Phát hiện ngôn ngữ hệ thống" to "Detect system language",
    "English" to "English", "Vietnamese" to "Vietnamese", "Japan" to "Japanese", "Korea" to "Korean", "China" to "Chinese",
    "Capture Quality" to "Capture Quality", "HDR · Multi-frame" to "HDR · Multi-frame",
    "Ưu tiên HDR nhiều mức phơi sáng từ CameraX/OEM. Nếu máy không có HDR Extension, ứng dụng thử Ultra HDR JPEG_R khi camera hỗ trợ. Quá trình hợp nhất dùng pipeline xử lý của thiết bị; Android không cho ứng dụng ép chọn riêng GPU hay CPU."
        to "Prioritizes multi-exposure HDR from CameraX/OEM. If the device has no HDR Extension, the app tries Ultra HDR JPEG_R when supported. Merging uses the device imaging pipeline; Android does not let apps force a GPU-only or CPU-only path.",
    "UHD · Độ phân giải tối đa" to "UHD · Maximum resolution",
    "Yêu cầu CameraX chọn kích thước JPEG lớn nhất mà camera đang dùng hỗ trợ; không nội suy vượt cảm biến." to "Requests the largest JPEG size supported by the active camera; does not upscale beyond the sensor.",
    "Noise Reduction" to "Noise Reduction",
    "Tắt sẽ yêu cầu camera không khử nhiễu; mức hỗ trợ thực tế tùy Camera2 HAL." to "Off requests no camera noise reduction; actual support depends on the Camera2 HAL.",
    "Best sử dụng mức nén JPEG cao nhất; độ phân giải thực tế phụ thuộc độ phân giải mà Camera2/CameraX cung cấp cho camera đang chọn." to "Best uses the highest JPEG quality. Actual resolution depends on the output sizes exposed by the active Camera2/CameraX camera.",
    "Ảnh được lưu vào Pictures/FujifilmMode." to "Photos are saved to Pictures/FujifilmMode.",
    "Import ảnh cho nút chụp" to "Import shutter image", "Đổi ảnh nút chụp" to "Change shutter image",
    "Đã chọn ảnh cho nút chụp." to "Shutter image selected.", "Dùng nút màu" to "Use solid color",
    "Đổi màu nút chụp hoặc dùng ảnh PNG/JPG làm mặt nút." to "Change the shutter button color or use a PNG/JPG image.",
    "Chọn ngôn ngữ hiển thị. Ứng dụng sẽ tải lại giao diện sau khi đổi." to "Choose the display language. The app reloads after you change it.",
    "Các công cụ này điều khiển trực tiếp lớp phủ trên kính ngắm." to "These tools directly control the viewfinder overlays.",
    "Thông tin camera do Camera2 đọc trực tiếp từ thiết bị." to "Camera information is read directly from the device through Camera2.",
    "Không đọc được Camera ID. Hãy cấp quyền camera cho ứng dụng." to "Could not read Camera IDs. Grant camera permission to the app.",
    "Thông tin tên mã cảm biến không được Android Camera2 công bố thống nhất trên mọi thiết bị; ứng dụng hiển thị kích thước cảm biến và độ phân giải tối đa mà hệ thống cung cấp." to "Android Camera2 does not expose sensor codenames consistently across devices; the app shows sensor size and the maximum resolution reported by the system.",
    "Lens calibration · EIS+ · sensor diagnostics" to "Lens calibration · EIS+ · sensor diagnostics",
    "Fujifilm Mode (app-debug)" to "Fujifilm Mode (app-debug)", "Người làm App: Baoaka06" to "Developer: Baoaka06",
    "Version 1.1 experimental · Debug build" to "Version 1.1 experimental · Debug build",
    "Camera này không hỗ trợ phơi sáng dài." to "This camera does not support long exposure.",
    "Camera không hỗ trợ phơi sáng thủ công để chụp đêm." to "The camera does not support manual exposure for night capture.",
    "Camera không hỗ trợ Ultra HDR; đã chuyển về JPEG." to "This camera does not support Ultra HDR; switched to JPEG.",
    "Camera không hỗ trợ phơi sáng thủ công để chụp đêm." to "The camera does not support manual exposure for night capture.",
    "Chuyển AF sang Auto trước khi khóa nét." to "Switch AF to Auto before locking focus.",
    "Đã khóa nét · chạm lại để lấy nét mới." to "Focus locked · tap again to refocus.",
    "Chưa có ảnh trong Pictures/FujifilmMode." to "No photos in Pictures/FujifilmMode yet.",
    "Đang đồng bộ hướng và tỉ lệ khung ngắm, hãy chụp lại sau một chút." to "Synchronizing viewfinder orientation and aspect ratio. Please capture again in a moment.",
    "Ảnh đã chụp nhưng MediaStore không trả về URI." to "The photo was captured, but MediaStore did not return a URI.",
    "Camera/API này không hỗ trợ chụp JPEG Ultra HDR." to "This camera/API does not support Ultra HDR JPEG capture.",
    "Ảnh đã lưu nhưng watermark chưa được đóng. Kiểm tra lại mục Watermark." to "Photo saved, but the watermark was not applied. Check Watermark settings.",
    "Không thể bật Ultra HDR hoặc khởi tạo camera:" to "Could not enable Ultra HDR or initialize the camera:",
    "Không mở được camera:" to "Could not open camera:",
    "Lỗi chụp ảnh:" to "Capture error:",
    "Chủ thể ở 1/3 trái · chừa khoảng trống bên phải" to "Subject on left third · leave space on the right",
    "Chủ thể ở 1/3 phải · chừa khoảng trống bên trái" to "Subject on right third · leave space on the left",
    "Chủ thể ở giữa · thử lệch nhẹ theo quy tắc 1/3" to "Subject centered · try shifting slightly using the rule of thirds",
    "Chưa thấy khuôn mặt · thử đặt chủ thể vào 1/3" to "No face detected · try placing the subject on a third",
    "Chủ thể" to "Subject", "ISO Auto" to "ISO Auto", "Shutter Auto" to "Shutter Auto",
    "EV không được hỗ trợ" to "EV unavailable", "AF tự động" to "AF Auto",
    "Tông màu tự động" to "Auto white balance", "Ảnh gốc" to "Original photo",
    "Cần cấp quyền Camera\n(chạm để cấp)" to "Camera permission required\n(tap to grant)",
    "Image Quality" to "Image Quality",
    "Shutter button" to "Shutter button",
    "Watermark" to "Watermark",
    "Màu khung" to "Frame color",
    "Chọn bộ lọc để xem tông màu; bộ lọc được xử lý trên ảnh sau khi chụp." to "Choose a filter to preview its look; it is applied to the photo after capture.",
    "TAP AF · HOLD LOCK" to "TAP AF · HOLD TO LOCK",
    "ĐANG DÙNG" to "ACTIVE",
    "CHỌN" to "SELECT",
    "Đang phơi sáng" to "Exposing",
    "Vui lòng để điện thoại cố định" to "Please keep the phone steady",
    "Không rung máy cho đến khi hoàn tất" to "Do not move the phone until capture finishes",
    "AI vẫn đang phát triển trong quá trình dùng chức năng có thể phát sinh lỗi hoặc không đúng như người dùng mong muốn"
        to "AI is still under development. Errors or unexpected results may occur while using this feature.",
    "HDR · Multi-frame" to "HDR · Multi-frame",
    "Timer" to "Timer",
    "Quick settings" to "Quick settings",
    "Đổi camera" to "Switch camera",
    "Đóng ảnh" to "Close photo",
    "Camera không hỗ trợ Ultra HDR hoặc HDR Extension; đã chuyển về JPEG SDR." to "This camera supports neither Ultra HDR nor the HDR extension; switched to standard JPEG.",
    "Đã lưu Ultra HDR nguyên bản và giữ gain map. Watermark, LUT, frame và sharpen UHD hậu kỳ được bỏ qua để bảo toàn HDR." to "Saved the original Ultra HDR and preserved its gain map. Watermark, LUT, frame and post-capture UHD sharpening are skipped to preserve HDR.",
    "HDR đa khung đang hoạt động; máy không hỗ trợ JPEG_R nên ảnh được lưu dạng JPEG." to "Multi-frame HDR is active. This camera does not support JPEG_R, so the photo will be saved as JPEG.",
    "No photos yet" to "No photos yet",
    "Sao lưu & Khôi phục" to "Backup & Restore",
    "Camera · Zoom · Các tính năng đã bật" to "Camera · Zoom · Enabled features",
    "Dữ liệu" to "Data",
    "Sao lưu cấu hình camera, zoom và các tính năng đã bật." to "Back up camera, zoom and enabled app features.",
    "Xuất bản sao lưu" to "Export backup",
    "Khôi phục bản sao lưu" to "Restore backup",
    "Đã xuất bản sao lưu cài đặt." to "Settings backup exported.",
    "Đã khôi phục cài đặt camera, zoom và các tính năng đã bật." to "Camera, zoom and enabled feature settings restored.",
    "Không thể tạo bản sao lưu. Hãy kiểm tra vị trí lưu và thử lại." to "Could not create the backup. Check the save location and try again.",
    "Không thể khôi phục. Hãy chọn tệp sao lưu Fujifilm Mode hợp lệ." to "Could not restore. Select a valid Fujifilm Mode backup file.",
    "Lưu ý: tệp này lưu cấu hình camera, frame và ảnh nút chụp; không nhúng tệp frame/ảnh gốc. Sau khi cài lại ứng dụng hoặc chuyển máy, cần nhập lại frame/ảnh tùy chỉnh và cấp quyền đọc chúng." to "Note: this file stores settings and references to LUTs, frames and shutter-button images; it does not embed the source assets. Re-import custom assets and grant read access after reinstalling or moving to another device.",
    "Ưu tiên HDR Extension nhiều khung hình của camera hoặc Ultra HDR JPEG_R. Nếu máy không hỗ trợ cả hai, ứng dụng dùng Software HDR bằng GPU hoặc CPU để cân bằng sáng một ảnh; không thể khôi phục chi tiết vùng đã cháy sáng." to "Prefer the camera's multi-frame HDR extension or Ultra HDR JPEG_R. If neither is supported, Software HDR uses GPU or CPU single-image tone mapping; clipped highlights cannot be recovered.",
    "Chọn độ phân giải JPEG cao nhất mà camera hỗ trợ, sau đó tăng độ nét 2× bằng GPU khi ảnh vừa bộ nhớ; ảnh lớn hoặc GPU không phù hợp sẽ chuyển sang CPU. Không tạo thêm chi tiết vượt cảm biến." to "Capture at the highest JPEG resolution supported by the camera, then apply 2× sharpening on the GPU when memory allows; large images or unsupported GPUs fall back to CPU. No detail beyond the sensor is created."
)

private val fullJapaneseUi = mapOf(
    "Cần cấp quyền Camera\n(chạm để cấp)" to "カメラ権限が必要です\\n（タップして許可）",
    "EV không được hỗ trợ" to "EVは利用できません", "AF tự động" to "AFオート",
    "Ấm" to "暖色", "Lạnh" to "寒色", "Focus locked" to "フォーカスロック",
    "Tông màu tự động" to "ホワイトバランス：オート", "LENS TRANSITION" to "レンズ切替中",
    "FILM WASH" to "フィルムウォッシュ", "Không dùng bộ lọc · Ảnh gốc" to "フィルターなし · オリジナル",
    "Mở Contents Management" to "コンテンツ管理を開く",
    "Nhấn để chọn áp dụng sau khi chụp" to "タップして選択し、撮影後に適用",
    "✓ ĐANG DÙNG" to "✓ 使用中", "Xem trước · áp dụng ảnh" to "プレビュー · 写真に適用",
    "Settings" to "設定", "Toolbox" to "ツールボックス", "Contents" to "コンテンツ",
    "Metering" to "測光", "Render Engine" to "描画エンジン", "Ultra HDR" to "Ultra HDR",
    "Stabilization" to "手ぶれ補正", "Aspect Ratio" to "アスペクト比",
    "Shutter" to "シャッター", "Rửa film" to "フィルム現像", "Chớp nhanh" to "クイックフラッシュ",
    "Âm thanh màn trập" to "シャッター音", "Dùng âm click chụp ảnh hệ thống của Android" to "Androidのシステムシャッター音を使用",
    "Viewfinder overlays" to "ファインダー表示", "Thirds composition guide" to "三分割構図ガイド",
    "Live luminance histogram" to "リアルタイム輝度ヒストグラム",
    "Highlight high-contrast focus edges" to "コントラストの高い輪郭を強調",
    "Khung ảnh phong cách rangefinder, không có logo thương hiệu" to "ブランドロゴなしのレンジファインダー風フレーム",
    "Mẫu watermark · chọn riêng từng kiểu" to "ウォーターマークのテンプレート",
    "EXIF cổ điển" to "クラシックEXIF", "Viền bo + khoảng trống" to "角丸フレームと余白",
    "Nền mờ, thông số nổi" to "ぼかし背景と撮影情報", "Thông số góc phải" to "右側に撮影情報",
    "Ngày chụp tối giản" to "シンプルな撮影日時", "Logo ở chính giữa" to "中央ロゴ",
    "Hiện thời gian" to "日時を表示", "Bật/tắt ngày và giờ trên watermark" to "ウォーターマークの日時を表示/非表示",
    "Hiện model máy" to "端末モデルを表示", "Bật/tắt tên model điện thoại" to "端末モデル名を表示/非表示",
    "Hiện thông số" to "撮影情報を表示", "Bật/tắt tiêu cự, khẩu độ, tốc độ màn trập và ISO" to "焦点距離、絞り、シャッター速度、ISOを表示/非表示",
    "Model tùy chỉnh" to "カスタムモデル", "Custom Model (để trống = Auto)" to "カスタムモデル（空欄＝自動）",
    "Auto sẽ lấy tên model của điện thoại: " to "自動では端末モデル名を使用：",
    "Save Model" to "モデルを保存", "Tên model tùy chỉnh được lưu trong dữ liệu ứng dụng và giữ nguyên cho đến khi xóa dữ liệu hoặc gỡ ứng dụng。" to "カスタムモデル名はアプリデータを消去するか、アプリをアンインストールするまで保存されます。",
    "Photo Frame" to "フォトフレーム", "Chọn frame tại đây. Frame được đóng lên ảnh sau khi chụp; không còn nút Frame ở màn hình chụp." to "ここでフレームを選択します。撮影後に写真へ合成され、撮影画面にはフレームボタンは表示されません。",
    "Màu frame" to "フレームの色", "Transparent bỏ viền và panel của frame dựng sẵn; với frame PNG sẽ giữ nguyên độ trong suốt gốc." to "透明では内蔵フレームの枠と背景を非表示にし、PNG本来の透明度を保持します。",
    "Muốn thêm frame PNG/JPG, vào Contents Management → Frame Manage → Import Frame." to "PNG/JPGフレームを追加するには、コンテンツ管理 → フレーム管理 → フレーム読み込みを開きます。",
    "Contents Management" to "コンテンツ管理", "Frame Manage" to "フレーム管理", "Nhập khung phủ PNG/JPG để quản lý." to "PNG/JPGオーバーレイフレームを読み込みます。",
    "Xóa" to "削除", "Hủy" to "キャンセル",
    "Album · Fujifilm Mode" to "アルバム · Fujifilm Mode", "Đang tải ảnh…" to "写真を読み込み中…",
    "Đang tải ảnh..." to "写真を読み込み中…", "Đóng" to "閉じる",
    "Chọn frame để xem trước và đóng lên ảnh sau khi chụp." to "フレームを選択してください。撮影後に写真へ合成されます。",
    "Frame color" to "フレームの色", "Frame dựng sẵn" to "内蔵フレーム",
    "Transparent: bỏ viền và panel nền; các chi tiết chữ/ảnh PNG vẫn được giữ." to "透明では枠と背景パネルを消し、文字とPNG画像は保持します。",
    "Áp dụng frame" to "フレームを適用",
    "Capture Quality" to "撮影画質", "HDR · Multi-frame" to "HDR · マルチフレーム",
    "Ưu tiên HDR nhiều mức phơi sáng từ CameraX/OEM. Nếu máy không có HDR Extension, ứng dụng thử Ultra HDR JPEG_R khi camera hỗ trợ. Quá trình hợp nhất dùng pipeline xử lý của thiết bị; Android không cho ứng dụng ép chọn riêng GPU hay CPU。"
        to "CameraX/OEMの複数露出HDRを優先します。HDR拡張がない場合、対応カメラではUltra HDR JPEG_Rを使用します。合成処理は端末の画像処理パイプラインで実行され、アプリからGPUまたはCPUを強制指定することはできません。",
    "UHD · Độ phân giải tối đa" to "UHD · 最大解像度",
    "Yêu cầu CameraX chọn kích thước JPEG lớn nhất mà camera đang dùng hỗ trợ; không nội suy vượt cảm biến." to "使用中のカメラが対応する最大JPEGサイズをCameraXに要求します。センサー解像度を超える拡大処理は行いません。",
    "Noise Reduction" to "ノイズ低減", "Tắt sẽ yêu cầu camera không khử nhiễu; mức hỗ trợ thực tế tùy Camera2 HAL." to "オフではカメラにノイズ低減の無効化を要求します。対応状況はCamera2 HALによって異なります。",
    "Best sử dụng mức nén JPEG cao nhất; độ phân giải thực tế phụ thuộc độ phân giải mà Camera2/CameraX cung cấp cho camera đang chọn." to "Bestでは最高JPEG品質を使用します。実際の解像度は選択中のカメラがCamera2/CameraXへ公開する出力サイズによります。",
    "Ảnh được lưu vào Pictures/FujifilmMode." to "写真はPictures/FujifilmModeに保存されます。",
    "Import ảnh cho nút chụp" to "シャッター画像を読み込む", "Đổi ảnh nút chụp" to "シャッター画像を変更",
    "Đã chọn ảnh cho nút chụp." to "シャッター画像を選択しました。", "Dùng nút màu" to "単色ボタンを使用",
    "Đổi màu nút chụp hoặc dùng ảnh PNG/JPG làm mặt nút." to "シャッターボタンの色を変更するか、PNG/JPG画像を使用します。",
    "Chọn ngôn ngữ hiển thị. Ứng dụng sẽ tải lại giao diện sau khi đổi." to "表示言語を選択します。変更後、アプリの画面を再読み込みします。",
    "Các công cụ này điều khiển trực tiếp lớp phủ trên kính ngắm." to "これらのツールでファインダー表示を直接操作します。",
    "Thông tin camera do Camera2 đọc trực tiếp từ thiết bị." to "カメラ情報はCamera2を通じて端末から直接取得します。",
    "Không đọc được Camera ID. Hãy cấp quyền camera cho ứng dụng." to "カメラIDを取得できません。アプリにカメラ権限を許可してください。",
    "Camera này không hỗ trợ phơi sáng dài." to "このカメラは長時間露光に対応していません。",
    "Đang phơi sáng vui lòng để điện thoại cố định" to "露光中です。スマートフォンを固定してください。",
    "Chuyển AF sang Auto trước khi khóa nét." to "フォーカスロックの前にAFをオートへ切り替えてください。",
    "Đã khóa nét · chạm lại để lấy nét mới." to "フォーカスをロックしました · もう一度タップすると再フォーカスします。",
    "Chưa có ảnh trong Pictures/FujifilmMode." to "Pictures/FujifilmModeに写真がありません。",
    "Đang đồng bộ hướng và tỉ lệ khung ngắm, hãy chụp lại sau một chút." to "ファインダーの向きと比率を同期中です。少し待ってから撮影してください。",
    "Camera không hỗ trợ phơi sáng thủ công để chụp đêm." to "このカメラは夜景撮影用のマニュアル露光に対応していません。",
    "Camera không hỗ trợ Ultra HDR; đã chuyển về JPEG." to "このカメラはUltra HDRに対応していません。JPEGへ切り替えました。",
    "Chưa thấy khuôn mặt · thử đặt chủ thể vào 1/3" to "顔を検出できません · 被写体を三分割位置に配置してください",
    "Chủ thể ở 1/3 trái · chừa khoảng trống bên phải" to "被写体は左の三分割位置 · 右側に余白を残してください",
    "Chủ thể ở 1/3 phải · chừa khoảng trống bên trái" to "被写体は右の三分割位置 · 左側に余白を残してください",
    "Chủ thể ở giữa · thử lệch nhẹ theo quy tắc 1/3" to "被写体は中央です · 三分割構図に合わせて少しずらしてみてください",
    "Image Quality" to "画質",
    "Shutter button" to "シャッターボタン",
    "Watermark" to "ウォーターマーク",
    "Màu khung" to "枠線の色",
    "Chọn bộ lọc để xem tông màu; bộ lọc được xử lý trên ảnh sau khi chụp。" to "フィルターの色調をプレビューします。撮影後に写真へ適用されます。",
    "Chọn bộ lọc để xem tông màu; bộ lọc được xử lý trên ảnh sau khi chụp." to "フィルターの色調をプレビューします。撮影後に写真へ適用されます。",
    "TAP AF · HOLD LOCK" to "タップでAF · 長押しでロック",
    "ĐANG DÙNG" to "使用中",
    "CHỌN" to "選択",
    "Đang phơi sáng" to "露光中",
    "Vui lòng để điện thoại cố định" to "スマートフォンを固定してください",
    "Không rung máy cho đến khi hoàn tất" to "撮影が完了するまで動かさないでください",
    "AI vẫn đang phát triển trong quá trình dùng chức năng có thể phát sinh lỗi hoặc không đúng như người dùng mong muốn"
        to "AI機能は開発中です。使用中にエラーが発生したり、期待と異なる結果になる場合があります。",
    "HDR · Multi-frame" to "HDR · マルチフレーム",
    "Timer" to "タイマー",
    "Quick settings" to "クイック設定",
    "Đổi camera" to "カメラ切替",
    "Đóng ảnh" to "写真を閉じる",
    "Camera không hỗ trợ Ultra HDR hoặc HDR Extension; đã chuyển về JPEG SDR." to "このカメラはUltra HDRにもHDR拡張にも対応していません。標準JPEGに切り替えました。",
    "Đã lưu Ultra HDR nguyên bản và giữ gain map. Watermark, LUT, frame và sharpen UHD hậu kỳ được bỏ qua để bảo toàn HDR." to "Ultra HDRの元画像を保存し、ゲインマップを保持しました。HDRを保つため、ウォーターマーク、LUT、フレーム、撮影後のUHDシャープ処理はスキップされます。",
    "HDR đa khung đang hoạt động; máy không hỗ trợ JPEG_R nên ảnh được lưu dạng JPEG." to "マルチフレームHDRは有効です。このカメラはJPEG_Rに対応していないため、写真はJPEGで保存されます。",
    "Sao lưu & Khôi phục" to "バックアップと復元",
    "Camera · Zoom · Các tính năng đã bật" to "カメラ · ズーム · 有効な機能",
    "Dữ liệu" to "データ",
    "Sao lưu cấu hình camera, zoom và các tính năng đã bật." to "カメラ、ズーム、有効なアプリ機能をバックアップします。",
    "Xuất bản sao lưu" to "バックアップを書き出す",
    "Khôi phục bản sao lưu" to "バックアップを復元",
    "Đã xuất bản sao lưu cài đặt." to "設定のバックアップを書き出しました。",
    "Đã khôi phục cài đặt camera, zoom và các tính năng đã bật." to "カメラ、ズーム、有効な機能の設定を復元しました。",
    "Không thể tạo bản sao lưu. Hãy kiểm tra vị trí lưu và thử lại." to "バックアップを作成できません。保存先を確認して再試行してください。",
    "Không thể khôi phục. Hãy chọn tệp sao lưu Fujifilm Mode hợp lệ." to "復元できません。有効なFujifilm Modeバックアップファイルを選択してください。",
    "Lưu ý: tệp này lưu cấu hình camera, frame và ảnh nút chụp; không nhúng tệp frame/ảnh gốc. Sau khi cài lại ứng dụng hoặc chuyển máy, cần nhập lại frame/ảnh tùy chỉnh và cấp quyền đọc chúng." to "このファイルには設定とLUT・フレーム・シャッターボタン画像への参照が保存されます。元ファイル自体は含まれません。再インストールや機種変更後はカスタム素材を再インポートし、読み取り権限を付与してください。",
    "Ưu tiên HDR Extension nhiều khung hình của camera hoặc Ultra HDR JPEG_R. Nếu máy không hỗ trợ cả hai, ứng dụng dùng Software HDR bằng GPU hoặc CPU để cân bằng sáng một ảnh; không thể khôi phục chi tiết vùng đã cháy sáng." to "カメラのマルチフレームHDR拡張またはUltra HDR JPEG_Rを優先します。どちらも非対応の場合、GPUまたはCPUで単一画像のトーンマッピングを行います。白飛びしたディテールは復元できません。",
    "Chọn độ phân giải JPEG cao nhất mà camera hỗ trợ, sau đó tăng độ nét 2× bằng GPU khi ảnh vừa bộ nhớ; ảnh lớn hoặc GPU không phù hợp sẽ chuyển sang CPU. Không tạo thêm chi tiết vượt cảm biến." to "カメラが対応する最大JPEG解像度で撮影し、メモリが許す場合はGPUで2倍シャープ化します。大きな画像や非対応GPUではCPUに切り替えます。センサー以上のディテールは生成されません。"
)

private val fullKoreanUi = mapOf(
    "Cần cấp quyền Camera\n(chạm để cấp)" to "카메라 권한이 필요합니다\\n(탭하여 허용)",
    "EV không được hỗ trợ" to "EV를 사용할 수 없음", "AF tự động" to "AF 자동",
    "Ấm" to "따뜻하게", "Lạnh" to "차갑게", "Focus locked" to "초점 잠금",
    "Tông màu tự động" to "자동 화이트 밸런스", "LENS TRANSITION" to "렌즈 전환 중",
    "FILM WASH" to "필름 워시", "Không dùng bộ lọc · Ảnh gốc" to "필터 사용 안 함 · 원본 사진",
    "Mở Contents Management" to "콘텐츠 관리 열기",
    "Nhấn để chọn áp dụng sau khi chụp" to "탭하여 선택하고 촬영 후 적용",
    "✓ ĐANG DÙNG" to "✓ 사용 중", "Xem trước · áp dụng ảnh" to "미리보기 · 사진에 적용",
    "Settings" to "설정", "Toolbox" to "도구 상자", "Contents" to "콘텐츠",
    "Metering" to "측광", "Render Engine" to "렌더링 엔진", "Ultra HDR" to "Ultra HDR",
    "Stabilization" to "손떨림 보정", "Aspect Ratio" to "화면 비율",
    "Shutter" to "셔터", "Rửa film" to "필름 워시", "Chớp nhanh" to "빠른 플래시",
    "Âm thanh màn trập" to "셔터 소리", "Dùng âm click chụp ảnh hệ thống của Android" to "Android 시스템 셔터 소리 사용",
    "Viewfinder overlays" to "뷰파인더 오버레이", "Thirds composition guide" to "삼분할 구도 가이드",
    "Live luminance histogram" to "실시간 밝기 히스토그램",
    "Highlight high-contrast focus edges" to "대비가 높은 초점 가장자리 강조",
    "Khung ảnh phong cách rangefinder, không có logo thương hiệu" to "브랜드 로고가 없는 레인지파인더 스타일 프레임",
    "Mẫu watermark · chọn riêng từng kiểu" to "워터마크 템플릿",
    "EXIF cổ điển" to "클래식 EXIF", "Viền bo + khoảng trống" to "둥근 테두리와 여백",
    "Nền mờ, thông số nổi" to "흐린 배경과 강조된 촬영 정보", "Thông số góc phải" to "오른쪽 모서리 촬영 정보",
    "Ngày chụp tối giản" to "미니멀 촬영 날짜", "Logo ở chính giữa" to "중앙 로고",
    "Hiện thời gian" to "날짜 및 시간 표시", "Bật/tắt ngày và giờ trên watermark" to "워터마크 날짜와 시간 표시/숨기기",
    "Hiện model máy" to "기기 모델 표시", "Bật/tắt tên model điện thoại" to "휴대전화 모델 이름 표시/숨기기",
    "Hiện thông số" to "촬영 정보 표시", "Bật/tắt tiêu cự, khẩu độ, tốc độ màn trập và ISO" to "초점 거리, 조리개, 셔터 속도 및 ISO 표시/숨기기",
    "Model tùy chỉnh" to "사용자 지정 모델", "Custom Model (để trống = Auto)" to "사용자 지정 모델(비워 두면 자동)",
    "Auto sẽ lấy tên model của điện thoại: " to "자동으로 휴대전화 모델을 사용합니다: ",
    "Save Model" to "모델 저장", "Photo Frame" to "사진 프레임",
    "Chọn frame tại đây. Frame được đóng lên ảnh sau khi chụp; không còn nút Frame ở màn hình chụp." to "여기서 프레임을 선택하세요. 촬영 후 사진에 합성되며 카메라 화면에는 프레임 버튼이 표시되지 않습니다.",
    "Màu frame" to "프레임 색상", "Transparent bỏ viền và panel của frame dựng sẵn; với frame PNG sẽ giữ nguyên độ trong suốt gốc." to "투명을 선택하면 기본 프레임 테두리와 배경이 제거되고 가져온 PNG의 투명도는 유지됩니다.",
    "Muốn thêm frame PNG/JPG, vào Contents Management → Frame Manage → Import Frame." to "PNG/JPG 프레임을 추가하려면 콘텐츠 관리 → 프레임 관리 → 프레임 가져오기를 여세요.",
    "Contents Management" to "콘텐츠 관리", "Frame Manage" to "프레임 관리자", "Nhập khung phủ PNG/JPG để quản lý." to "PNG/JPG 오버레이 프레임을 가져옵니다.",
    "Xóa" to "삭제", "Hủy" to "취소",
    "Album · Fujifilm Mode" to "앨범 · Fujifilm Mode", "Đang tải ảnh…" to "사진 불러오는 중…",
    "Đang tải ảnh..." to "사진 불러오는 중…", "Đóng" to "닫기",
    "Chọn frame để xem trước và đóng lên ảnh sau khi chụp." to "프레임을 선택하세요. 촬영 후 사진에 합성됩니다.",
    "Frame color" to "프레임 색상", "Frame dựng sẵn" to "내장 프레임",
    "Transparent: bỏ viền và panel nền; các chi tiết chữ/ảnh PNG vẫn được giữ." to "투명 모드는 테두리와 배경 패널을 제거하고 텍스트 및 PNG 아트워크는 유지합니다.",
    "Áp dụng frame" to "프레임 적용",
    "Capture Quality" to "촬영 품질", "HDR · Multi-frame" to "HDR · 멀티 프레임",
    "Ưu tiên HDR nhiều mức phơi sáng từ CameraX/OEM. Nếu máy không có HDR Extension, ứng dụng thử Ultra HDR JPEG_R khi camera hỗ trợ. Quá trình hợp nhất dùng pipeline xử lý của thiết bị; Android không cho ứng dụng ép chọn riêng GPU hay CPU."
        to "CameraX/OEM의 다중 노출 HDR을 우선 사용합니다. 기기에 HDR 확장이 없으면 카메라가 지원할 때 Ultra HDR JPEG_R을 시도합니다. 병합은 기기의 이미지 처리 파이프라인을 사용하며 Android는 앱이 GPU 또는 CPU를 강제로 지정하도록 허용하지 않습니다.",
    "UHD · Độ phân giải tối đa" to "UHD · 최대 해상도",
    "Yêu cầu CameraX chọn kích thước JPEG lớn nhất mà camera đang dùng hỗ trợ; không nội suy vượt cảm biến." to "현재 카메라가 지원하는 가장 큰 JPEG 크기를 CameraX에 요청합니다. 센서 해상도 이상으로 확대하지 않습니다.",
    "Noise Reduction" to "노이즈 감소",
    "Tắt sẽ yêu cầu camera không khử nhiễu; mức hỗ trợ thực tế tùy Camera2 HAL." to "끄면 카메라에 노이즈 감소를 끄도록 요청합니다. 실제 지원은 Camera2 HAL에 따라 다릅니다.",
    "Best sử dụng mức nén JPEG cao nhất; độ phân giải thực tế phụ thuộc độ phân giải mà Camera2/CameraX cung cấp cho camera đang chọn." to "Best는 가장 높은 JPEG 품질을 사용합니다. 실제 해상도는 선택한 카메라가 Camera2/CameraX에 제공하는 출력 크기에 따라 달라집니다.",
    "Ảnh được lưu vào Pictures/FujifilmMode." to "사진은 Pictures/FujifilmMode에 저장됩니다.",
    "Import ảnh cho nút chụp" to "셔터 이미지 가져오기", "Đổi ảnh nút chụp" to "셔터 이미지 변경",
    "Đã chọn ảnh cho nút chụp." to "셔터 이미지를 선택했습니다.", "Dùng nút màu" to "단색 버튼 사용",
    "Đổi màu nút chụp hoặc dùng ảnh PNG/JPG làm mặt nút." to "셔터 버튼 색상을 변경하거나 PNG/JPG 이미지를 사용하세요.",
    "Chọn ngôn ngữ hiển thị. Ứng dụng sẽ tải lại giao diện sau khi đổi." to "표시 언어를 선택하세요. 변경 후 앱 화면이 다시 로드됩니다.",
    "Các công cụ này điều khiển trực tiếp lớp phủ trên kính ngắm." to "이 도구에서 뷰파인더 오버레이를 직접 제어합니다.",
    "Thông tin camera do Camera2 đọc trực tiếp từ thiết bị." to "카메라 정보는 Camera2를 통해 기기에서 직접 가져옵니다.",
    "Không đọc được Camera ID. Hãy cấp quyền camera cho ứng dụng." to "카메라 ID를 읽을 수 없습니다. 앱에 카메라 권한을 허용하세요.",
    "Camera này không hỗ trợ phơi sáng dài." to "이 카메라는 장시간 노출을 지원하지 않습니다.",
    "Đang phơi sáng vui lòng để điện thoại cố định" to "노출 중입니다. 휴대전화를 고정해 주세요.",
    "Chuyển AF sang Auto trước khi khóa nét." to "초점을 잠그기 전에 AF를 자동으로 전환하세요.",
    "Đã khóa nét · chạm lại để lấy nét mới." to "초점이 잠겼습니다 · 다시 탭하면 초점을 다시 맞춥니다.",
    "Chưa có ảnh trong Pictures/FujifilmMode." to "Pictures/FujifilmMode에 사진이 없습니다.",
    "Đang đồng bộ hướng và tỉ lệ khung ngắm, hãy chụp lại sau một chút." to "뷰파인더 방향과 비율을 동기화 중입니다. 잠시 후 다시 촬영하세요.",
    "Camera không hỗ trợ phơi sáng thủ công để chụp đêm." to "이 카메라는 야간 촬영용 수동 노출을 지원하지 않습니다.",
    "Camera không hỗ trợ Ultra HDR; đã chuyển về JPEG." to "이 카메라는 Ultra HDR을 지원하지 않아 JPEG로 전환했습니다.",
    "Chưa thấy khuôn mặt · thử đặt chủ thể vào 1/3" to "얼굴을 찾지 못했습니다 · 피사체를 삼분할 위치에 배치해 보세요",
    "Chủ thể ở 1/3 trái · chừa khoảng trống bên phải" to "피사체가 왼쪽 삼분할 위치에 있습니다 · 오른쪽에 여백을 남기세요",
    "Chủ thể ở 1/3 phải · chừa khoảng trống bên trái" to "피사체가 오른쪽 삼분할 위치에 있습니다 · 왼쪽에 여백을 남기세요",
    "Chủ thể ở giữa · thử lệch nhẹ theo quy tắc 1/3" to "피사체가 중앙에 있습니다 · 삼분할 구도에 맞춰 약간 옮겨 보세요",
    "Image Quality" to "이미지 품질",
    "Shutter button" to "셔터 버튼",
    "Watermark" to "워터마크",
    "Màu khung" to "테두리 색상",
    "Chọn bộ lọc để xem tông màu; bộ lọc được xử lý trên ảnh sau khi chụp." to "필터 색감을 미리 봅니다. 촬영 후 사진에 적용됩니다.",
    "TAP AF · HOLD LOCK" to "탭하여 AF · 길게 눌러 초점 잠금",
    "ĐANG DÙNG" to "사용 중",
    "CHỌN" to "선택",
    "Đang phơi sáng" to "노출 중",
    "Vui lòng để điện thoại cố định" to "휴대전화를 고정해 주세요",
    "Không rung máy cho đến khi hoàn tất" to "촬영이 끝날 때까지 휴대전화를 움직이지 마세요",
    "AI vẫn đang phát triển trong quá trình dùng chức năng có thể phát sinh lỗi hoặc không đúng như người dùng mong muốn"
        to "AI 기능은 개발 중입니다. 사용 중 오류가 발생하거나 예상과 다른 결과가 나올 수 있습니다.",
    "HDR · Multi-frame" to "HDR · 멀티 프레임",
    "Timer" to "타이머",
    "Quick settings" to "빠른 설정",
    "Đổi camera" to "카메라 전환",
    "Đóng ảnh" to "사진 닫기",
    "Camera không hỗ trợ Ultra HDR hoặc HDR Extension; đã chuyển về JPEG SDR." to "이 카메라는 Ultra HDR 및 HDR 확장을 지원하지 않아 일반 JPEG로 전환했습니다.",
    "Đã lưu Ultra HDR nguyên bản và giữ gain map. Watermark, LUT, frame và sharpen UHD hậu kỳ được bỏ qua để bảo toàn HDR." to "원본 Ultra HDR과 게인 맵을 보존했습니다. HDR 보존을 위해 워터마크, LUT, 프레임 및 촬영 후 UHD 선명화는 건너뜁니다.",
    "HDR đa khung đang hoạt động; máy không hỗ trợ JPEG_R nên ảnh được lưu dạng JPEG." to "멀티 프레임 HDR이 활성화되었습니다. 이 카메라는 JPEG_R을 지원하지 않아 JPEG로 저장됩니다.",
    "Sao lưu & Khôi phục" to "백업 및 복원",
    "Camera · Zoom · Các tính năng đã bật" to "카메라 · 줌 · 활성화된 기능",
    "Dữ liệu" to "데이터",
    "Sao lưu cấu hình camera, zoom và các tính năng đã bật." to "카메라, 줌 및 활성화된 앱 기능을 백업합니다.",
    "Xuất bản sao lưu" to "백업 내보내기",
    "Khôi phục bản sao lưu" to "백업 복원",
    "Đã xuất bản sao lưu cài đặt." to "설정 백업을 내보냈습니다.",
    "Đã khôi phục cài đặt camera, zoom và các tính năng đã bật." to "카메라, 줌 및 활성화된 기능 설정을 복원했습니다.",
    "Không thể tạo bản sao lưu. Hãy kiểm tra vị trí lưu và thử lại." to "백업을 만들 수 없습니다. 저장 위치를 확인한 후 다시 시도하세요.",
    "Không thể khôi phục. Hãy chọn tệp sao lưu Fujifilm Mode hợp lệ." to "복원할 수 없습니다. 유효한 Fujifilm Mode 백업 파일을 선택하세요.",
    "Lưu ý: tệp này lưu cấu hình camera, frame và ảnh nút chụp; không nhúng tệp frame/ảnh gốc. Sau khi cài lại ứng dụng hoặc chuyển máy, cần nhập lại frame/ảnh tùy chỉnh và cấp quyền đọc chúng." to "이 파일에는 설정과 LUT·프레임·셔터 버튼 이미지 참조만 저장되며 원본 에셋은 포함되지 않습니다. 재설치하거나 기기를 변경한 후에는 사용자 지정 에셋을 다시 가져오고 읽기 권한을 부여해야 합니다.",
    "Ưu tiên HDR Extension nhiều khung hình của camera hoặc Ultra HDR JPEG_R. Nếu máy không hỗ trợ cả hai, ứng dụng dùng Software HDR bằng GPU hoặc CPU để cân bằng sáng một ảnh; không thể khôi phục chi tiết vùng đã cháy sáng." to "카메라의 멀티 프레임 HDR 확장 또는 Ultra HDR JPEG_R을 우선 사용합니다. 둘 다 지원하지 않으면 GPU 또는 CPU로 단일 이미지 톤 매핑을 수행합니다. 날아간 하이라이트 디테일은 복원할 수 없습니다.",
    "Chọn độ phân giải JPEG cao nhất mà camera hỗ trợ, sau đó tăng độ nét 2× bằng GPU khi ảnh vừa bộ nhớ; ảnh lớn hoặc GPU không phù hợp sẽ chuyển sang CPU. Không tạo thêm chi tiết vượt cảm biến." to "카메라가 지원하는 최대 JPEG 해상도로 촬영한 뒤 메모리가 허용하면 GPU로 2배 선명화합니다. 큰 이미지나 미지원 GPU에서는 CPU로 전환합니다. 센서 이상의 세부 정보를 생성하지는 않습니다."
)

private val fullChineseUi = mapOf(
    "Cần cấp quyền Camera\n(chạm để cấp)" to "需要相机权限\\n（点击授权）",
    "EV không được hỗ trợ" to "不支持 EV", "AF tự động" to "自动 AF",
    "Ấm" to "暖色", "Lạnh" to "冷色", "Focus locked" to "已锁定对焦",
    "Tông màu tự động" to "自动白平衡", "LENS TRANSITION" to "镜头切换中",
    "FILM WASH" to "胶片显影", "Không dùng bộ lọc · Ảnh gốc" to "不使用滤镜 · 原图",
    "Mở Contents Management" to "打开内容管理",
    "Nhấn để chọn áp dụng sau khi chụp" to "点击选择并在拍摄后应用",
    "✓ ĐANG DÙNG" to "✓ 正在使用", "Xem trước · áp dụng ảnh" to "预览 · 应用到照片",
    "Settings" to "设置", "Toolbox" to "工具箱", "Contents" to "内容",
    "Metering" to "测光", "Render Engine" to "渲染引擎", "Ultra HDR" to "Ultra HDR",
    "Stabilization" to "防抖", "Aspect Ratio" to "画面比例",
    "Shutter" to "快门", "Rửa film" to "胶片显影", "Chớp nhanh" to "快速闪光",
    "Âm thanh màn trập" to "快门声音", "Dùng âm click chụp ảnh hệ thống của Android" to "使用 Android 系统快门音",
    "Viewfinder overlays" to "取景器辅助显示", "Thirds composition guide" to "三分构图辅助",
    "Live luminance histogram" to "实时亮度直方图",
    "Highlight high-contrast focus edges" to "突出高对比度对焦边缘",
    "Khung ảnh phong cách rangefinder, không có logo thương hiệu" to "无品牌标志的旁轴相机风格边框",
    "Mẫu watermark · chọn riêng từng kiểu" to "水印模板",
    "EXIF cổ điển" to "经典 EXIF", "Viền bo + khoảng trống" to "圆角边框与留白",
    "Nền mờ, thông số nổi" to "柔化背景与突出参数", "Thông số góc phải" to "右侧角落参数",
    "Ngày chụp tối giản" to "极简拍摄日期", "Logo ở chính giữa" to "居中标志",
    "Hiện thời gian" to "显示日期和时间", "Bật/tắt ngày và giờ trên watermark" to "显示/隐藏水印日期和时间",
    "Hiện model máy" to "显示设备型号", "Bật/tắt tên model điện thoại" to "显示/隐藏手机型号",
    "Hiện thông số" to "显示拍摄参数", "Bật/tắt tiêu cự, khẩu độ, tốc độ màn trập và ISO" to "显示/隐藏焦距、光圈、快门速度和 ISO",
    "Model tùy chỉnh" to "自定义型号", "Custom Model (để trống = Auto)" to "自定义型号（留空为自动）",
    "Auto sẽ lấy tên model của điện thoại: " to "自动使用手机型号：",
    "Save Model" to "保存型号", "Photo Frame" to "照片边框",
    "Chọn frame tại đây. Frame được đóng lên ảnh sau khi chụp; không còn nút Frame ở màn hình chụp." to "在此选择边框。拍摄后会合成到照片中，拍摄界面不再显示边框按钮。",
    "Màu frame" to "边框颜色", "Transparent bỏ viền và panel của frame dựng sẵn; với frame PNG sẽ giữ nguyên độ trong suốt gốc." to "透明模式会移除内置边框和底板，并保留导入 PNG 的原始透明度。",
    "Muốn thêm frame PNG/JPG, vào Contents Management → Frame Manage → Import Frame." to "添加 PNG/JPG 边框，请打开内容管理 → 边框管理 → 导入边框。",
    "Contents Management" to "内容管理", "Frame Manage" to "边框管理", "Nhập khung phủ PNG/JPG để quản lý." to "导入 PNG/JPG 覆盖边框。",
    "Xóa" to "删除", "Hủy" to "取消",
    "Album · Fujifilm Mode" to "相册 · Fujifilm Mode", "Đang tải ảnh…" to "正在加载照片…",
    "Đang tải ảnh..." to "正在加载照片…", "Đóng" to "关闭",
    "Chọn frame để xem trước và đóng lên ảnh sau khi chụp." to "选择边框预览；拍摄后会合成到照片中。",
    "Frame color" to "边框颜色", "Frame dựng sẵn" to "内置边框",
    "Transparent: bỏ viền và panel nền; các chi tiết chữ/ảnh PNG vẫn được giữ." to "透明模式会移除边框和底板，但保留文字与 PNG 图案。",
    "Áp dụng frame" to "应用边框",
    "Capture Quality" to "拍摄质量", "HDR · Multi-frame" to "HDR · 多帧",
    "Ưu tiên HDR nhiều mức phơi sáng từ CameraX/OEM. Nếu máy không có HDR Extension, ứng dụng thử Ultra HDR JPEG_R khi camera hỗ trợ. Quá trình hợp nhất dùng pipeline xử lý của thiết bị; Android không cho ứng dụng ép chọn riêng GPU hay CPU."
        to "优先使用 CameraX/OEM 多曝光 HDR。若设备没有 HDR 扩展，则在相机支持时尝试 Ultra HDR JPEG_R。合成由设备图像处理管线完成；Android 不允许应用强制指定仅使用 GPU 或 CPU。",
    "UHD · Độ phân giải tối đa" to "UHD · 最高分辨率",
    "Yêu cầu CameraX chọn kích thước JPEG lớn nhất mà camera đang dùng hỗ trợ; không nội suy vượt cảm biến." to "请求 CameraX 使用当前相机支持的最大 JPEG 尺寸，不会插值放大到超过传感器分辨率。",
    "Noise Reduction" to "降噪",
    "Tắt sẽ yêu cầu camera không khử nhiễu; mức hỗ trợ thực tế tùy Camera2 HAL." to "关闭会请求相机停用降噪，实际支持情况取决于 Camera2 HAL。",
    "Best sử dụng mức nén JPEG cao nhất; độ phân giải thực tế phụ thuộc độ phân giải mà Camera2/CameraX cung cấp cho camera đang chọn." to "Best 使用最高 JPEG 质量。实际分辨率取决于当前相机通过 Camera2/CameraX 提供的输出尺寸。",
    "Ảnh được lưu vào Pictures/FujifilmMode." to "照片保存到 Pictures/FujifilmMode。",
    "Import ảnh cho nút chụp" to "导入快门图片", "Đổi ảnh nút chụp" to "更换快门图片",
    "Đã chọn ảnh cho nút chụp." to "已选择快门图片", "Dùng nút màu" to "使用纯色按钮",
    "Đổi màu nút chụp hoặc dùng ảnh PNG/JPG làm mặt nút." to "更改快门按钮颜色，或使用 PNG/JPG 图片。",
    "Chọn ngôn ngữ hiển thị. Ứng dụng sẽ tải lại giao diện sau khi đổi." to "选择界面语言。更改后应用将重新加载界面。",
    "Các công cụ này điều khiển trực tiếp lớp phủ trên kính ngắm." to "这些工具可直接控制取景器辅助显示。",
    "Thông tin camera do Camera2 đọc trực tiếp từ thiết bị." to "相机信息通过 Camera2 从设备直接读取。",
    "Không đọc được Camera ID. Hãy cấp quyền camera cho ứng dụng." to "无法读取相机 ID，请授予应用相机权限。",
    "Camera này không hỗ trợ phơi sáng dài." to "此相机不支持长时间曝光。",
    "Đang phơi sáng vui lòng để điện thoại cố định" to "正在曝光，请保持手机稳定。",
    "Chuyển AF sang Auto trước khi khóa nét." to "锁定焦点前请先将 AF 切换为自动。",
    "Đã khóa nét · chạm lại để lấy nét mới." to "已锁定焦点 · 再次点击可重新对焦。",
    "Chưa có ảnh trong Pictures/FujifilmMode." to "Pictures/FujifilmMode 中暂无照片。",
    "Đang đồng bộ hướng và tỉ lệ khung ngắm, hãy chụp lại sau một chút." to "正在同步取景器方向和画面比例，请稍后再拍摄。",
    "Camera không hỗ trợ phơi sáng thủ công để chụp đêm." to "此相机不支持夜景拍摄所需的手动曝光。",
    "Camera không hỗ trợ Ultra HDR; đã chuyển về JPEG." to "此相机不支持 Ultra HDR，已切换为 JPEG。",
    "Chưa thấy khuôn mặt · thử đặt chủ thể vào 1/3" to "未检测到人脸 · 尝试将主体放在三分线位置",
    "Chủ thể ở 1/3 trái · chừa khoảng trống bên phải" to "主体位于左侧三分线 · 右侧留出空间",
    "Chủ thể ở 1/3 phải · chừa khoảng trống bên trái" to "主体位于右侧三分线 · 左侧留出空间",
    "Chủ thể ở giữa · thử lệch nhẹ theo quy tắc 1/3" to "主体位于中央 · 可按三分法稍微偏移",
    "Image Quality" to "图像质量",
    "Shutter button" to "快门按钮",
    "Watermark" to "水印",
    "Màu khung" to "边框颜色",
    "Chọn bộ lọc để xem tông màu; bộ lọc được xử lý trên ảnh sau khi chụp." to "选择滤镜预览色调；滤镜会在拍摄后应用到照片。",
    "TAP AF · HOLD LOCK" to "点击 AF · 长按锁定",
    "ĐANG DÙNG" to "正在使用",
    "CHỌN" to "选择",
    "Đang phơi sáng" to "正在曝光",
    "Vui lòng để điện thoại cố định" to "请保持手机稳定",
    "Không rung máy cho đến khi hoàn tất" to "拍摄完成前请勿移动手机",
    "AI vẫn đang phát triển trong quá trình dùng chức năng có thể phát sinh lỗi hoặc không đúng như người dùng mong muốn"
        to "AI 功能仍在开发中，使用过程中可能出现错误或结果不符合预期。",
    "HDR · Multi-frame" to "HDR · 多帧",
    "Timer" to "定时器",
    "Quick settings" to "快捷设置",
    "Đổi camera" to "切换相机",
    "Đóng ảnh" to "关闭照片",
    "Camera không hỗ trợ Ultra HDR hoặc HDR Extension; đã chuyển về JPEG SDR." to "此相机不支持 Ultra HDR 或 HDR 扩展，已切换为标准 JPEG。",
    "Đã lưu Ultra HDR nguyên bản và giữ gain map. Watermark, LUT, frame và sharpen UHD hậu kỳ được bỏ qua để bảo toàn HDR." to "已保存原始 Ultra HDR 并保留增益图。为保留 HDR，水印、LUT、边框和拍摄后的 UHD 锐化将被跳过。",
    "HDR đa khung đang hoạt động; máy không hỗ trợ JPEG_R nên ảnh được lưu dạng JPEG." to "多帧 HDR 已启用。此相机不支持 JPEG_R，因此照片将以 JPEG 格式保存。",
    "Sao lưu & Khôi phục" to "备份与恢复",
    "Camera · Zoom · Các tính năng đã bật" to "相机 · 变焦 · 已启用功能",
    "Dữ liệu" to "数据",
    "Sao lưu cấu hình camera, zoom và các tính năng đã bật." to "备份相机、变焦和已启用的应用功能。",
    "Xuất bản sao lưu" to "导出备份",
    "Khôi phục bản sao lưu" to "恢复备份",
    "Đã xuất bản sao lưu cài đặt." to "设置备份已导出。",
    "Đã khôi phục cài đặt camera, zoom và các tính năng đã bật." to "相机、变焦和已启用功能的设置已恢复。",
    "Không thể tạo bản sao lưu. Hãy kiểm tra vị trí lưu và thử lại." to "无法创建备份。请检查保存位置后重试。",
    "Không thể khôi phục. Hãy chọn tệp sao lưu Fujifilm Mode hợp lệ." to "无法恢复。请选择有效的 Fujifilm Mode 备份文件。",
    "Lưu ý: tệp này lưu cấu hình camera, frame và ảnh nút chụp; không nhúng tệp frame/ảnh gốc. Sau khi cài lại ứng dụng hoặc chuyển máy, cần nhập lại frame/ảnh tùy chỉnh và cấp quyền đọc chúng." to "此文件保存设置以及 LUT、边框和快门按钮图片的引用，不会包含原始素材。重新安装应用或更换设备后，需要重新导入自定义素材并授予读取权限。",
    "Ưu tiên HDR Extension nhiều khung hình của camera hoặc Ultra HDR JPEG_R. Nếu máy không hỗ trợ cả hai, ứng dụng dùng Software HDR bằng GPU hoặc CPU để cân bằng sáng một ảnh; không thể khôi phục chi tiết vùng đã cháy sáng." to "优先使用相机多帧 HDR 扩展或 Ultra HDR JPEG_R。如果两者均不支持，则使用 GPU 或 CPU 对单张图像进行软件色调映射；无法恢复已经过曝丢失的细节。",
    "Chọn độ phân giải JPEG cao nhất mà camera hỗ trợ, sau đó tăng độ nét 2× bằng GPU khi ảnh vừa bộ nhớ; ảnh lớn hoặc GPU không phù hợp sẽ chuyển sang CPU. Không tạo thêm chi tiết vượt cảm biến." to "先以相机支持的最高 JPEG 分辨率拍摄，再在内存允许时使用 GPU 进行 2 倍锐化；大图或 GPU 不兼容时改用 CPU。不会生成超出传感器能力的细节。"
)

private fun translateFullUiText(value: String, language: String): String? {
    val map = when {
        language.startsWith("ja") -> fullJapaneseUi
        language.startsWith("ko") -> fullKoreanUi
        language.startsWith("zh") -> fullChineseUi
        language.startsWith("en") -> fullEnglishUi
        else -> return null
    }
    map[value]?.let { return it }
    val dynamic = when {
        language.startsWith("ja") -> listOf(
            "Camera ID " to "カメラID ", "ISO " to "ISO ", "Shutter " to "シャッター ",
            "Tông màu " to "色温度 ", "Auto sẽ lấy tên model của điện thoại: " to "自動では端末モデル名を使用：",
            "Không thể khởi tạo HDR hoặc camera: " to "HDRまたはカメラを初期化できません：",
            "Bộ lọc: " to "フィルター：", "AI BỐ CỤC · " to "AI構図 · ",
            "Lỗi chụp ảnh:" to "撮影エラー：", "Không mở được camera:" to "カメラを開けません：",
            "Không thể bật Ultra HDR hoặc khởi tạo camera:" to "Ultra HDRを有効化またはカメラを初期化できません："
        )
        language.startsWith("ko") -> listOf(
            "Camera ID " to "카메라 ID ", "ISO " to "ISO ", "Shutter " to "셔터 ",
            "Tông màu " to "색온도 ", "Auto sẽ lấy tên model của điện thoại: " to "자동으로 휴대전화 모델을 사용합니다: ",
            "Không thể khởi tạo HDR hoặc camera: " to "HDR 또는 카메라를 초기화할 수 없습니다: ",
            "Bộ lọc: " to "필터: ", "AI BỐ CỤC · " to "AI 구도 · ",
            "Lỗi chụp ảnh:" to "촬영 오류:", "Không mở được camera:" to "카메라를 열 수 없습니다:",
            "Không thể bật Ultra HDR hoặc khởi tạo camera:" to "Ultra HDR을 켜거나 카메라를 초기화할 수 없습니다:"
        )
        language.startsWith("zh") -> listOf(
            "Camera ID " to "相机 ID ", "ISO " to "ISO ", "Shutter " to "快门 ",
            "Tông màu " to "色温 ", "Auto sẽ lấy tên model của điện thoại: " to "自动使用手机型号：",
            "Không thể khởi tạo HDR hoặc camera: " to "无法初始化 HDR 或相机：",
            "Bộ lọc: " to "滤镜：", "AI BỐ CỤC · " to "AI 构图 · ",
            "Lỗi chụp ảnh:" to "拍摄错误：", "Không mở được camera:" to "无法打开相机：",
            "Không thể bật Ultra HDR hoặc khởi tạo camera:" to "无法启用 Ultra HDR 或初始化相机："
        )
        else -> listOf(
            "Camera ID " to "Camera ID ", "ISO " to "ISO ", "Shutter " to "Shutter ",
            "Tông màu " to "Color temperature ", "Auto sẽ lấy tên model của điện thoại: " to "Auto uses phone model: ",
            "Không thể khởi tạo HDR hoặc camera: " to "Could not initialize HDR or camera: ",
            "Bộ lọc: " to "Filter: ", "AI BỐ CỤC · " to "AI COMPOSITION · ",
            "Lỗi chụp ảnh:" to "Capture error:", "Không mở được camera:" to "Could not open camera:",
            "Không thể bật Ultra HDR hoặc khởi tạo camera:" to "Could not enable Ultra HDR or initialize camera:"
        )
    }
    dynamic.firstOrNull { value.startsWith(it.first) }?.let {
        return it.second + value.removePrefix(it.first)
    }
    Regex("""^Đã nhập LUT: (.+)$""").matchEntire(value)?.let { match ->
        val name = match.groupValues[1]
        val prefix = dynamic.firstOrNull { it.first == "Đã nhập LUT: " }?.second ?: "Imported LUT: "
        return prefix + name
    }
    Regex("""^(\d+) ảnh$""").matchEntire(value)?.let { match ->
        val count = match.groupValues[1]
        return when {
            language.startsWith("ja") -> "${count}枚の写真"
            language.startsWith("ko") -> "사진 ${count}장"
            language.startsWith("zh") -> "$count 张照片"
            else -> "$count photos"
        }
    }
    return null
}

private fun translateUiText(value: String, language: String): String {
    if (language == "vi" || language == "system") return value
    translateFullUiText(value, language)?.let { return it }
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

fun localizedUiText(context: Context, value: String): String {
    val preference = context.getSharedPreferences("fujifilm_settings", 0)
        .getString("app_language", "system") ?: "system"
    val language = if (preference == "system") {
        context.resources.configuration.locales[0]?.language ?: "en"
    } else preference
    return translateUiText(value, language)
}

fun localizedToast(context: Context, value: String, duration: Int): Toast {
    return Toast.makeText(context, localizedUiText(context, value), duration)
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
    val localized = localizedUiText(context, text)
    MaterialText(
        text = localized,
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
