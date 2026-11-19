# FujiFilm Mode terminology glossary

This glossary is the canonical terminology for user-facing English and Vietnamese UI strings.
Keep product names, camera control abbreviations, filter names and file formats unchanged.

| English | Tiếng Việt |
|---|---|
| Aspect ratio | Tỉ lệ khung hình |
| Assistive tools | Công cụ hỗ trợ |
| Focus & Lens | Lấy nét & Ống kính |
| Capture & Quality | Chụp & Chất lượng |
| Photo mode setting | Cài đặt chế độ ảnh |
| Appearance | Giao diện |
| Language | Ngôn ngữ |
| About | Giới thiệu |
| Toolbox | Hộp công cụ |
| Contents | Nội dung |
| Metering | Đo sáng |
| Render engine | Bộ xử lý kết xuất |
| Backup & Restore | Sao lưu & Khôi phục |
| Noise reduction | Giảm nhiễu |
| Save location | Vị trí lưu |
| Image quality | Chất lượng ảnh |
| Settings | Cài đặt |
| Frames | Khung hình |
| Other | Khác |
| Device options | Tùy chọn thiết bị |
| Live Photo | Ảnh động |
| Performance | Hiệu năng |
| Center | Trung tâm |
| Frames manager | Quản lý khung |
| LUT Manager | Quản lý LUT |
| Import LUT | Nhập LUT |
| Close | Đóng |
| Grid Lines | Đường lưới |
| Focus Peaking | Viền hỗ trợ lấy nét |
| Histogram | Biểu đồ histogram |
| Sensor information | Thông tin cảm biến |
| Camera ID | ID camera |
| Lens | Ống kính |
| Photo mode | Chế độ ảnh |
| Night capture | Chụp đêm |
| Long Exposure | Phơi sáng lâu |
| Star Trails | Vệt sao |
| Custom shutter button | Nút chụp tùy chỉnh |
| Viewfinder overlays | Lớp phủ kính ngắm |
| Pro HUD elements | Thành phần HUD Pro |
| EV/Shutter/ISO display | Hiển thị EV/Shutter/ISO |
| Choose a mode before capture | Chọn chế độ trước khi chụp |
| LunaDream is off | LunaDream đang tắt |
| LunaDream is on | LunaDream đang bật |
| Hold steady | Giữ máy ổn định |
| Move freely | Có thể di chuyển máy tự do |

## Canonical localization whitelist

The canonical preservation list is maintained in `CamUI-src/i18n_whitelist.txt`.
Only proper names, product and film-look names, camera abbreviations, technical identifiers and file formats belong there. Ordinary interface labels and descriptive sentences must still be translated.

Resource parity is enforced by `LocalizationResourceParityTest`: English and Vietnamese resource keys must match, and numbered format placeholders must remain identical.

## Preserve unchanged in both languages

Camera codes and engraved controls: `EV`, `SS`, `Tv`, `F`, `ISO`, `AF`, `AF-S`, `AF-C`, `MF`, `AF-L`, `AE-L`, `WB`, `DR100/200/400`, `FLASH`, `DRIVE`, `FILM`, `ADJ`, `HISTO`, `Q`, `DISP/BACK`, `MENU/OK`, `PLAY`, `LIVE`, `W`, `T`, `K`, `MP`, `fps`.

Product and mode names: `FujiFilm Mode`, `Fujifilm`, `LunaDream 1.5`, `LunaDream 2.0`, `NightShot`, `Provia`, `Velvia`, `Astia`, `Classic Chrome`.

File formats and technical identifiers: `.cube`, `.bin`, `ZIP`, `JPEG`, `HDR`, `LUT`, `DNG`, `JPEG_R`, `Camera2`, `CameraX`.

Translation rules:
- One visible sentence must be in one language; do not mix English and Vietnamese in the same sentence except for the preserved items above.
- Use numbered format placeholders such as `%1$s` and `%1$d`.
- Use Android plurals for count-dependent labels.
- The selected app language, not the device default locale, governs app-formatted dates, numbers and units.
