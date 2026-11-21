package com.example.camui

/**
 * Supplemental translations for camera-state, processing and settings copy that is
 * intentionally composed in Kotlin rather than stored as Android string resources.
 *
 * Vietnamese is the canonical source for these messages. English-to-Vietnamese
 * entries are included for text that is authored directly in English.
 */
internal object UiTranslationCatalog {
    private val vietnameseToEnglish = linkedMapOf(
        "Camera này không hỗ trợ RAW/RawDream 2.0 đồng thời. Không thể dùng chế độ RAW đã chọn." to
            "This camera cannot capture RAW and RawDream 2.0 simultaneously. The selected RAW mode is unavailable.",
        "Hình xem trước có dấu hiệu mờ. Có thể ống kính bị bẩn hoặc che khuất; hãy kiểm tra và lau kính camera." to
            "The preview appears blurred. The lens may be dirty or obstructed; inspect and clean the camera glass.",
        "Đang dùng Software HDR bằng CPU vì thiết bị không cung cấp HDR Extension." to
            "Using CPU Software HDR because the device does not provide an HDR Extension.",
        "Thiết bị không có HDR phần cứng; sẽ dùng Software HDR bằng CPU sau khi chụp." to
            "Hardware HDR is unavailable; CPU Software HDR will be applied after capture.",
        "Camera OEM Extension không khởi chạy được; đang dùng pipeline ISP mặc định của thiết bị." to
            "The OEM camera extension could not start; using the device's default ISP pipeline.",
        "Không thể khởi tạo camera theo pipeline gốc của thiết bị." to
            "The camera could not be initialized through the device's native pipeline.",
        "HDR Extension không ghép được với camera này; đang chuyển sang Software HDR xử lý bằng CPU." to
            "The HDR Extension is incompatible with this camera; switching to CPU Software HDR.",
        "HDR Extension không thể ghép với luồng live; đang dùng Ultra HDR JPEG_R thay thế." to
            "The HDR Extension cannot run with the live stream; using Ultra HDR JPEG_R instead.",
        "Live Photo dùng 12.5MP trên tổ hợp camera hiện tại để giữ được clip chuyển động." to
            "Live Photo is using 12.5MP on this camera combination to preserve the motion clip.",
        "Camera này không hỗ trợ Live Photo đồng thời với ảnh tĩnh; đã tắt Live Photo và khôi phục chế độ chụp thường." to
            "This camera cannot capture Live Photo and still photos together. Live Photo was disabled and standard capture restored.",
        "Camera không hỗ trợ HDR phần cứng; đang dùng Software HDR bằng CPU." to
            "Hardware HDR is not supported; using CPU Software HDR.",
        "Camera đang chọn không có đèn LED hỗ trợ Torch." to
            "The selected camera has no LED that supports Torch mode.",
        "Không thể bật đèn LED liên tục trên camera này." to
            "Continuous LED illumination could not be enabled on this camera.",
        "Star Trails tự dừng để bảo vệ bộ nhớ tạm. Đang ghép các khung đã ghi." to
            "Star Trails stopped automatically to protect temporary storage. Merging the captured frames.",
        "Không thể ghép vệt sao từ phiên này. Hãy thử lại với máy ổn định hơn." to
            "Star Trails could not merge this session. Try again with a steadier device.",
        "Cần ít nhất 2 khung hình để tạo vệt sao. Hãy thử lại." to
            "At least two frames are required to create star trails. Try again.",
        "RAW không kết hợp với LunaDream, NightShotMode, Burst hoặc Bracketing. Hãy tắt chế độ tính toán hoặc chuyển định dạng về JPEG." to
            "RAW cannot be combined with LunaDream, NightShotMode, Burst or Bracketing. Turn off computational modes or switch the format to JPEG.",
        "Đang hoàn tất chuỗi Bracketing hiện tại…" to
            "Finishing the current Bracketing sequence…",
        "Chọn HDR Bracketing hoặc Focus Bracketing, không bật đồng thời." to
            "Choose HDR Bracketing or Focus Bracketing; they cannot run at the same time.",
        "Camera không công bố hỗ trợ lấy nét thủ công để Focus Bracketing." to
            "The camera does not report manual-focus support for Focus Bracketing.",
        "Camera không công bố dải bù trừ phơi sáng cho HDR Bracketing." to
            "The camera does not report an exposure-compensation range for HDR Bracketing.",
        "Đang dừng Burst sau khung hình hiện tại…" to
            "Stopping Burst after the current frame…",
        "Burst hoàn tất · đã lưu \$burstCompletedFrames/25 ảnh JPEG gốc. Tốc độ phụ thuộc camera và bộ nhớ." to
            "Burst complete · saved \$burstCompletedFrames/25 original JPEG photos. Speed depends on the camera and storage.",
        "Đang dừng Star Trails sau khung hình hiện tại…" to
            "Stopping Star Trails after the current frame…",
        "LunaDream cần Photo/JPEG; RawDream 2.0 cần RAW + JPEG được camera hỗ trợ. Hãy tắt Ultra HDR, Burst và Bracketing." to
            "LunaDream requires Photo/JPEG; RawDream 2.0 requires camera-supported RAW + JPEG. Turn off Ultra HDR, Burst and Bracketing.",
        "Milky Way Stack cần Photo/JPEG 1x; hãy tắt RAW, UHD, FHD, Burst và Bracketing." to
            "Milky Way Stack requires Photo/JPEG at 1x. Turn off RAW, UHD, FHD, Burst and Bracketing.",
        "Đang đồng bộ hướng cảm biến, hãy chụp lại sau một chút." to
            "Synchronizing sensor orientation. Please capture again in a moment.",
        "Đã lưu ảnh RAW DNG gốc vào Pictures/FujifilmMode/RAW." to
            "Saved the original RAW DNG to Pictures/FujifilmMode/RAW.",
        "Camera không trả về dữ liệu RAW_SENSOR hợp lệ." to
            "The camera did not return valid RAW_SENSOR data.",
        "Đã lưu Ultra HDR nguyên bản và giữ gain map." to
            "Saved the original Ultra HDR and preserved its gain map.",
        "Đã lưu tệp Ultra HDR nguyên bản; giữ nguyên gain map." to
            "Saved the original Ultra HDR file and preserved its gain map.",
        "Live Photo chưa sẵn sàng: camera chưa tạo được luồng video. Ảnh chưa được chụp." to
            "Live Photo is not ready: the camera has not created a video stream. No photo was captured.",
        "Camera không bắt đầu được clip Live Photo. Ảnh chưa được chụp." to
            "The camera could not start the Live Photo clip. No photo was captured.",
        "Đã tắt Milky Way Stack: camera đang chọn không có Manual Sensor phù hợp." to
            "Milky Way Stack was disabled because the selected camera has no compatible Manual Sensor support.",
        "Tripod · nét vô cực · Astro Stack ×8 nếu Manual Sensor hỗ trợ" to
            "Tripod · infinity focus · Astro Stack ×8 when Manual Sensor is supported",
        "TẮT MILKY WAY STACK ×8" to
            "TURN OFF MILKY WAY STACK ×8",
        "ÁP DỤNG GỢI Ý + BẬT ASTRO STACK ×8" to
            "APPLY RECOMMENDATION + ENABLE ASTRO STACK ×8",
        "Đã tắt Milky Way Stack và trả phơi sáng về Auto." to
            "Milky Way Stack was disabled and exposure returned to Auto.",
        "Camera chưa sẵn sàng." to
            "Camera is not ready.",
        "Pin còn rất thấp. Hãy sạc pin trước khi dùng Star Trails, NightShot hoặc phơi sáng dài." to
            "Battery is critically low. Charge the phone before using Star Trails, NightShot or long exposure.",
        "Không thể áp dụng Manual Sensor/WB trên camera này." to
            "Manual Sensor/WB could not be applied on this camera.",
        "ĐANG KHÓA NÉT VÔ CỰC · CHẠM ĐỂ TRỞ VỀ AF" to
            "LOCKING INFINITY FOCUS · TAP TO RETURN TO AF",
        "ĐẶT LẤY NÉT VÔ CỰC" to
            "SET INFINITY FOCUS",
        "Đã trở về AF tự động." to
            "Returned to automatic AF.",
        "Đã đặt nét vô cực theo Camera2." to
            "Infinity focus was set through Camera2.",
        "Camera không chấp nhận lấy nét vô cực." to
            "The camera rejected infinity focus.",
        "Vui lòng tắt Live Photo" to
            "Please turn off Live Photo.",
        "Vui lòng tắt Xử lý ảnh gốc thiết bị để sử dụng LunaDream." to
            "Please turn off Device-native Photo Processing to use LunaDream.",
        "ẢNH GỐC · TRƯỚC LUNADREAM" to
            "ORIGINAL · BEFORE LUNADREAM",
        "ẢNH ĐÃ XỬ LÝ · LUNADREAM" to
            "PROCESSED · LUNADREAM",
        "ẢNH GỐC · THẢ ĐỂ XEM LỌC" to
            "ORIGINAL PHOTO · RELEASE TO VIEW FILTER",
        "GIỮ ĐỂ XEM ẢNH GỐC" to
            "HOLD TO VIEW ORIGINAL PHOTO",
        "Lưu hoặc nạp lại các thiết lập camera" to
            "Save or restore camera settings.",
        "Scanline overlay chỉ hiển thị trên preview, không lưu vào ảnh." to
            "The scanline overlay appears only in the preview and is never saved into photos.",
        "Giảm texture và hiệu ứng để tiết kiệm GPU trên máy yếu." to
            "Reduce textures and effects to save GPU resources on lower-powered devices.",
        "Đảo vị trí cụm chế độ và cụm nút/dial." to
            "Swap the positions of the mode group and the button/dial group.",
        "Màn trập cơ dùng WAV riêng 210 ms; tiếng phím và detent được tạo tổng hợp, có thể tắt riêng." to
            "The mechanical shutter uses a dedicated 210 ms WAV. Key and dial-detent sounds are synthesized and can be disabled separately.",
        "AE bình thường · EV 0 · căn khung và gộp nhiều ảnh" to
            "Standard AE · EV 0 · align and merge multiple photos",
        "ĐANG QUÉT CAMERA…" to
            "SCANNING CAMERAS…",
        "Không thể hoàn tất quét" to
            "The scan could not be completed.",
        "ĐANG QUÉT CAMERA…" to "SCANNING CAMERAS…",
        "Bật/tắt âm thanh chụp mặc định." to
            "Enable or disable the default shutter sound.",
        "Hiện hoặc ẩn thông số bù phơi sáng trong hàng Pro HUD." to
            "Show or hide exposure compensation in the Pro HUD row.",
        "Nhập LUT .cube, LUT Canon RGB 33x33x33 (.bin), hoặc ZIP chứa tối đa 700 LUT .cube/.bin. Hỗ trợ ZIP lồng. LUT Log được giữ riêng, không áp nhầm lên JPEG sRGB." to
            "Import a .cube LUT, Canon RGB 33x33x33 (.bin) LUT, or a ZIP containing up to 700 .cube/.bin LUTs. Nested ZIPs are supported. Log LUTs remain separate and are not incorrectly applied to sRGB JPEGs.",
        "Đã thêm · " to
            "Added · ",
        "Chưa thêm · " to
            "Not added · ",
        "Lưu" to "Save",
        "Nạp" to "Load",
        "Camera không hỗ trợ RAW + JPEG đồng thời. RawDream 2.0 chưa chụp; ảnh không bị chuyển âm thầm sang JPEG thường." to
            "The camera cannot capture RAW + JPEG simultaneously. RawDream 2.0 did not capture, and the app did not silently switch to standard JPEG.",
        "Đang hoàn tất chuỗi Bracketing hiện tại…" to "Finishing the current Bracketing sequence…",
        "Chọn HDR Bracketing hoặc Focus Bracketing, không bật đồng thời." to
            "Choose HDR Bracketing or Focus Bracketing; they cannot be enabled together.",
        "Camera không công bố hỗ trợ lấy nét thủ công để Focus Bracketing." to
            "The camera does not report manual focus support for Focus Bracketing.",
        "Camera không công bố dải bù trừ phơi sáng cho HDR Bracketing." to
            "The camera does not report an exposure compensation range for HDR Bracketing.",
        "Không thể đặt khoảng nét \${index + 1}; dừng Focus Bracketing." to
            "Could not set focus position \${index + 1}; stopping Focus Bracketing.",
        "Đang dừng Burst sau khung hình hiện tại…" to "Stopping Burst after the current frame…",
        "Đang dừng Star Trails sau khung hình hiện tại…" to "Stopping Star Trails after the current frame…",
        "LunaDream cần Photo/JPEG; RawDream 2.0 cần RAW + JPEG được camera hỗ trợ. Hãy tắt Ultra HDR, Burst và Bracketing." to
            "LunaDream requires Photo/JPEG; RawDream 2.0 requires camera-supported RAW + JPEG. Turn off Ultra HDR, Burst and Bracketing.",
        "Milky Way Stack cần Photo/JPEG 1x; hãy tắt RAW, UHD, FHD, Burst và Bracketing." to
            "Milky Way Stack requires Photo/JPEG at 1x. Turn off RAW, UHD, FHD, Burst and Bracketing.",
        "Đang đồng bộ hướng cảm biến, hãy chụp lại sau một chút." to
            "Synchronizing sensor orientation. Please capture again in a moment.",
        "Đã lưu ảnh RAW DNG gốc vào Pictures/FujifilmMode/RAW." to
            "Saved the original RAW DNG to Pictures/FujifilmMode/RAW.",
        "Camera không trả về dữ liệu RAW_SENSOR hợp lệ." to "The camera did not return valid RAW_SENSOR data.",
        "Đã lưu tệp Ultra HDR nguyên bản; giữ nguyên gain map." to
            "Saved the original Ultra HDR file and preserved its gain map.",
        "Live Photo chưa sẵn sàng: camera chưa tạo được luồng video. Ảnh chưa được chụp." to
            "Live Photo is not ready: the camera has not created a video stream. No photo was captured.",
        "Camera không bắt đầu được clip Live Photo. Ảnh chưa được chụp." to
            "The camera could not start the Live Photo clip. No photo was captured.",
        "Đã tắt Milky Way Stack: camera đang chọn không có Manual Sensor phù hợp." to
            "Milky Way Stack was disabled because the selected camera has no compatible Manual Sensor support.",
        "Tripod · nét vô cực · Astro Stack ×8 nếu Manual Sensor hỗ trợ" to
            "Tripod · infinity focus · Astro Stack ×8 when Manual Sensor is supported",
        "Đã tắt Milky Way Stack và trả phơi sáng về Auto." to
            "Milky Way Stack was disabled and exposure returned to Auto.",
        "Không thể áp dụng Manual Sensor/WB trên camera này." to
            "Manual Sensor/WB could not be applied on this camera.",
        "ĐANG KHÓA NÉT VÔ CỰC · CHẠM ĐỂ TRỞ VỀ AF" to
            "LOCKING INFINITY FOCUS · TAP TO RETURN TO AF",
        "ĐẶT LẤY NÉT VÔ CỰC" to "SET INFINITY FOCUS",
        "Đã trở về AF tự động." to "Returned to automatic AF.",
        "Đã đặt nét vô cực theo Camera2." to "Infinity focus was set through Camera2.",
        "Camera không chấp nhận lấy nét vô cực." to "The camera rejected infinity focus.",
        "Vui lòng tắt Live Photo" to "Please turn off Live Photo.",
        "Vui lòng tắt Xử lý ảnh gốc thiết bị để sử dụng LunaDream." to
            "Please turn off Device-native Photo Processing to use LunaDream.",
        "ẢNH GỐC · TRƯỚC LUNADREAM" to "ORIGINAL · BEFORE LUNADREAM",
        "ẢNH ĐÃ XỬ LÝ · LUNADREAM" to "PROCESSED · LUNADREAM",
        "ẢNH GỐC · THẢ ĐỂ XEM LỌC" to "ORIGINAL PHOTO · RELEASE TO VIEW FILTER",
        "GIỮ ĐỂ XEM ẢNH GỐC" to "HOLD TO VIEW ORIGINAL PHOTO",
        "Màn trập cơ dùng WAV riêng 210 ms; tiếng phím và detent được tạo tổng hợp, có thể tắt riêng." to
            "The mechanical shutter uses a dedicated 210 ms WAV. Key and dial-detent sounds are synthesized and can be disabled separately.",
        "AE bình thường · EV 0 · căn khung và gộp nhiều ảnh" to
            "Standard AE · EV 0 · align and merge multiple photos",
        "ĐANG QUÉT CAMERA…" to "SCANNING CAMERAS…",
        "Không thể hoàn tất quét" to "The scan could not be completed.",
        "Còn \${scan.vendorMetadataKeys.size - 30} key khác." to
            "\${scan.vendorMetadataKeys.size - 30} more keys.",
        "Lưu hoặc nạp lại các thiết lập camera" to "Save or restore camera settings.",
        "Scanline overlay chỉ hiển thị trên preview, không lưu vào ảnh." to
            "The scanline overlay appears only in the preview and is never saved into photos.",
        "Giảm texture và hiệu ứng để tiết kiệm GPU trên máy yếu." to
            "Reduce textures and effects to save GPU resources on lower-powered devices.",
        "Đảo vị trí cụm chế độ và cụm nút/dial." to
            "Swap the positions of the mode group and the button/dial group.",
        "Bật/tắt âm thanh chụp mặc định." to "Enable or disable the default shutter sound.",
        "Hiện hoặc ẩn thông số bù phơi sáng trong hàng Pro HUD." to
            "Show or hide exposure compensation in the Pro HUD row.",
        "Sao lưu camera trước/sau, tỷ lệ khung hình, zoom, chế độ Pro/Photo, flash, hẹn giờ, HDR, UHD, chống rung, AI và các tùy chọn đã bật." to
            "Back up front/rear camera selection, aspect ratio, zoom, Pro/Photo mode, flash, timer, HDR, UHD, stabilization, AI and enabled options.",
        "Night capture and Star Trails use separate processing pipelines" to
            "Chụp đêm và Star Trails sử dụng các pipeline xử lý riêng."
        "Thông tin" to "Information",
        "Đóng" to "Close",
        "Ảnh gốc" to "Original photo",
        "Bộ lọc" to "Filter",
        "Đang tải ảnh..." to "Loading photo...",
        "Ấm" to "Warm",
        "Lạnh" to "Cool",
        "Không rõ nguyên nhân" to "Unknown cause",
        "không rõ nguyên nhân" to "unknown cause",
        "không rõ lỗi" to "unknown error",
        "khung hình đã chụp" to "captured frames",
        " khung hình" to " frames",
        " (cần kiểm tra lại metadata)." to " (metadata still needs verification)."
    )

    private val englishToVietnamese = mapOf(
        "RAW Capture" to "Chụp RAW",
        "Battery is critically low. Charge the phone before using Star Trails, NightShot or long exposure." to
            "Pin còn rất thấp. Hãy sạc pin trước khi dùng Star Trails, NightShot hoặc phơi sáng dài.",
        "Camera Presets" to "Thiết lập camera",
        "Film Simulation Customizer" to "Tùy chỉnh mô phỏng phim",
        "Saved Film Recipes" to "Công thức mô phỏng phim đã lưu",
        "Night capture and Star Trails use separate processing pipelines" to
            "Chụp đêm và Star Trails dùng các pipeline xử lý riêng.",
        "ISO Auto" to "ISO tự động",
        "Shutter Auto" to "Màn trập tự động",
        "LUT MANAGER" to "QUẢN LÝ LUT",
        "Color Temperature" to "Nhiệt độ màu",
        "Color Tone" to "Tông màu",
        "Green ↔ Magenta" to "Xanh lá ↔ Đỏ tươi",
        "Saturation" to "Độ bão hòa",
        "Highlight" to "Vùng sáng",
        "Shadow" to "Vùng tối",
        "Battery temperature:" to "Nhiệt độ pin:",
        "Android thermal status:" to "Trạng thái nhiệt Android:"
    )

    private val additionalVietnameseToEnglish = mapOf(
        "Đang kiểm tra hỗ trợ phơi sáng" to "Checking exposure support",
        "Camera này không hỗ trợ RAW/RawDream 2.0 đồng thời. Không thể dùng chế độ RAW đã chọn." to
            "This camera cannot capture RAW and RawDream 2.0 simultaneously. The selected RAW mode is unavailable.",
        "Camera chưa sẵn sàng." to "Camera is not ready.",
        "Không thể ghép vệt sao từ phiên này. Hãy thử lại với máy ổn định hơn." to
            "Could not merge star trails from this session. Try again with a steadier device.",
        "Cần ít nhất 2 khung hình để tạo vệt sao. Hãy thử lại." to
            "At least two frames are required to create star trails. Try again.",
        "Không rõ nguyên nhân" to "Unknown reason",
        "không rõ nguyên nhân" to "unknown reason",
        "không rõ lỗi" to "unknown error",
        "Camera không hỗ trợ RAW + JPEG đồng thời. RawDream 2.0 chưa chụp; ảnh không bị chuyển âm thầm sang JPEG thường." to
            "The camera cannot capture RAW + JPEG simultaneously. RawDream 2.0 did not capture, and the app did not silently switch to standard JPEG.",
        "LunaDream cần Photo/JPEG; RawDream 2.0 cần RAW + JPEG được camera hỗ trợ. Hãy tắt Ultra HDR, Burst và Bracketing." to
            "LunaDream requires Photo/JPEG; RawDream 2.0 requires camera-supported RAW + JPEG. Turn off Ultra HDR, Burst and Bracketing.",
        "Milky Way Stack cần Photo/JPEG 1x; hãy tắt RAW, UHD, FHD, Burst và Bracketing." to
            "Milky Way Stack requires Photo/JPEG at 1x. Turn off RAW, UHD, FHD, Burst and Bracketing.",
        "Đã lưu ảnh RAW DNG gốc vào Pictures/FujifilmMode/RAW." to
            "Saved the original RAW DNG to Pictures/FujifilmMode/RAW.",
        "Camera không trả về dữ liệu RAW_SENSOR hợp lệ." to "The camera did not return valid RAW_SENSOR data.",
        "Live Photo chưa sẵn sàng: camera chưa tạo được luồng video. Ảnh chưa được chụp." to
            "Live Photo is not ready: the camera has not created a video stream. No photo was captured.",
        "Camera không bắt đầu được clip Live Photo. Ảnh chưa được chụp." to
            "The camera could not start the Live Photo clip. No photo was captured.",
        "Đã tắt Milky Way Stack: camera đang chọn không có Manual Sensor phù hợp." to
            "Milky Way Stack was disabled because the selected camera has no compatible Manual Sensor support.",
        "Đã tắt Milky Way Stack và trả phơi sáng về Auto." to
            "Milky Way Stack was disabled and exposure returned to Auto.",
        "Đã trở về AF tự động." to "Returned to automatic AF.",
        "Đã đặt nét vô cực theo Camera2." to "Infinity focus was set through Camera2.",
        "Camera không chấp nhận lấy nét vô cực." to "The camera rejected infinity focus.",
        "TẮT MILKY WAY STACK ×8" to "TURN OFF MILKY WAY STACK ×8",
        "ÁP DỤNG GỢI Ý + BẬT ASTRO STACK ×8" to "APPLY RECOMMENDATION + ENABLE ASTRO STACK ×8",
        "Vui lòng tắt Live Photo" to "Please turn off Live Photo.",
        "Vui lòng tắt Xử lý ảnh gốc thiết bị để sử dụng LunaDream." to
            "Please turn off Device-native Photo Processing to use LunaDream.",
        "ẢNH GỐC · TRƯỚC LUNADREAM" to "ORIGINAL · BEFORE LUNADREAM",
        "ẢNH ĐÃ XỬ LÝ · LUNADREAM" to "PROCESSED · LUNADREAM",
        "ẢNH GỐC · THẢ ĐỂ XEM LỌC" to "ORIGINAL PHOTO · RELEASE TO VIEW FILTER",
        "GIỮ ĐỂ XEM ẢNH GỐC" to "HOLD TO VIEW ORIGINAL PHOTO",
        "Lưu hoặc nạp lại các thiết lập camera" to "Save or restore camera settings.",
        "Scanline overlay chỉ hiển thị trên preview, không lưu vào ảnh." to
            "The scanline overlay appears only in the preview and is never saved into photos.",
        "Giảm texture và hiệu ứng để tiết kiệm GPU trên máy yếu." to
            "Reduce textures and effects to save GPU resources on lower-powered devices.",
        "Đảo vị trí cụm chế độ và cụm nút/dial." to
            "Swap the positions of the mode group and the button/dial group.",
        "AE bình thường · EV 0 · căn khung và gộp nhiều ảnh" to
            "Standard AE · EV 0 · align and merge multiple photos",
        "ĐANG QUÉT CAMERA…" to "SCANNING CAMERAS…",
        "Không thể hoàn tất quét" to "The scan could not be completed.",
        "Hiện hoặc ẩn thông số bù phơi sáng trong hàng Pro HUD." to
            "Show or hide exposure compensation in the Pro HUD row.",
        "LƯU" to "SAVE",
        "Bật/tắt âm thanh chụp mặc định." to "Enable or disable the default shutter sound.",
        "khung hình đã chụp" to "captured frames",
        "Ảnh gốc" to "Original photo",
        "Ảnh gần nhất" to "Most recent photo",
        "SIÊU RỘNG" to "ULTRAWIDE",
        "CAM THƯỜNG" to "MAIN CAMERA",
        "Đã nhập " to "Imported ",
        "Đã thêm · " to "Added · ",
        "Chưa thêm · " to "Not added · ",
        "Thông tin" to "Information",
        "Đóng" to "Close",
        "Bộ lọc" to "Filter"
    )

    private val additionalEnglishToVietnamese = mapOf(
        "Checking exposure support" to "Đang kiểm tra hỗ trợ phơi sáng",
        "Unknown reason" to "Không rõ nguyên nhân",
        "unknown reason" to "không rõ nguyên nhân",
        "unknown error" to "không rõ lỗi",
        "TURN OFF MILKY WAY STACK ×8" to "TẮT MILKY WAY STACK ×8",
        "APPLY RECOMMENDATION + ENABLE ASTRO STACK ×8" to "ÁP DỤNG GỢI Ý + BẬT ASTRO STACK ×8",
        "ULTRAWIDE" to "SIÊU RỘNG",
        "MAIN CAMERA" to "CAM THƯỜNG",
        "Most recent photo" to "Ảnh gần nhất"
    )

    private data class Prefix(val source: String, val translated: String)

    private val vietnamesePrefixes = listOf(
        Prefix("Không thể khởi tạo HDR hoặc camera: ", "Could not initialize HDR or camera: "),
        Prefix("Không mở được camera: ", "Could not open camera: "),
        Prefix("Không thể khởi tạo camera. Giao diện vẫn được giữ mở: ", "Could not initialize the camera. The UI will remain open: "),
        Prefix("Không thể khởi tạo dịch vụ camera: ", "Could not initialize the camera service: "),
        Prefix("Star Trails đang ghi: ", "Star Trails recording: "),
        Prefix("Đã lưu Star Trails từ ", "Saved Star Trails from "),
        Prefix("Star Trails gặp lỗi: ", "Star Trails error: "),
        Prefix("Không thể đặt khoảng nét ", "Could not set focus position "),
        Prefix("Import LUT thất bại: ", "LUT import failed: "),
        Prefix("Đã nhập ", "Imported "),
        Prefix("NIGHTSHOT · BẬT · ", "NIGHTSHOT · ON · "),
        Prefix("NIGHTSHOT · TẮT", "NIGHTSHOT · OFF")
    )

    private fun translatePrefix(value: String, prefixes: List<Prefix>): String? {
        val prefix = prefixes.firstOrNull { value.startsWith(it.source) } ?: return null
        return prefix.translated + value.removePrefix(prefix.source)
    }

    fun toEnglish(value: String): String? {
        vietnameseToEnglish[value]?.let { return it }
        additionalVietnameseToEnglish[value]?.let { return it }
        Regex("^Đã nhập (.+) LUT\\. Vào Content Manager để Add LUT vào bộ lọc\\.$").matchEntire(value)?.let {
            return "Imported ${it.groupValues[1]} LUT. Open Content Manager to add the LUT to filters."
        }
        Regex("^Đã nhập (.+) LUT\\. Chọn Add để bật từng bộ lọc\\.$").matchEntire(value)?.let {
            return "Imported ${it.groupValues[1]} LUT. Select Add to enable each filter."
        }
        Regex("^(\\d+) bộ lọc$").matchEntire(value)?.let {
            return it.groupValues[1] + " filters"
        }
        Regex("^Còn (\\d+) key khác\\.$").matchEntire(value)?.let {
            return it.groupValues[1] + " more keys."
        }
        Regex("^Đã lưu Camera Preset (\\d+)$").matchEntire(value)?.let {
            return "Saved Camera Preset " + it.groupValues[1]
        }
        Regex("^Preset (\\d+) chưa được lưu$").matchEntire(value)?.let {
            return "Preset " + it.groupValues[1] + " has not been saved."
        }
        Regex("^Đã nạp Camera Preset (\\d+) · cấu hình chụp đã khôi phục$").matchEntire(value)?.let {
            return "Loaded Camera Preset " + it.groupValues[1] + " · capture settings restored."
        }
        Regex("^Đã lưu Film Recipe (\\d+)$").matchEntire(value)?.let {
            return "Saved Film Recipe " + it.groupValues[1]
        }
        Regex("^Film Recipe (\\d+) chưa được lưu$").matchEntire(value)?.let {
            return "Film Recipe " + it.groupValues[1] + " has not been saved."
        }
        Regex("^Đã nạp Film Recipe (\\d+)$").matchEntire(value)?.let {
            return "Loaded Film Recipe " + it.groupValues[1]
        }
        Regex("^Star Trails đang ghi: (.+)\\. Nhấn nút chụp lần nữa để dừng và ghép vệt sao\\.$").matchEntire(value)?.let {
            return "Star Trails recording: ${it.groupValues[1]}. Press the shutter again to stop after the current frame and merge the trails."
        }
        Regex("^Đã lưu Star Trails từ (\\d+) khung hình(?: \\(cần kiểm tra lại metadata\\)\\.)?$").matchEntire(value)?.let {
            return "Saved Star Trails from ${it.groupValues[1]} frames."
        }
        Regex("^Burst hoàn tất · đã lưu (\\d+)/25 ảnh JPEG gốc\\. Tốc độ phụ thuộc camera và bộ nhớ\\.$").matchEntire(value)?.let {
            return "Burst complete · saved ${it.groupValues[1]}/25 original JPEG photos. Speed depends on the camera and storage."
        }
        Regex("^Đã áp dụng ISO (\\d+) · (.+) · WB (\\d+)K\\. Chụp 8 ảnh để căn chỉnh và giảm nhiễu\\.$").matchEntire(value)?.let {
            return "Applied ISO ${it.groupValues[1]} · ${it.groupValues[2]} · WB ${it.groupValues[3]}K. Capture 8 photos for alignment and noise reduction."
        }
        Regex("^Tông màu (\\d+) K$").matchEntire(value)?.let {
            return "Color temperature ${it.groupValues[1]} K"
        }
        Regex("^(\\d+) ảnh$").matchEntire(value)?.let {
            return "${it.groupValues[1]} photos"
        }
        Regex("^Hiển thị (\\d+) / (\\d+) LUT khớp · Tổng (\\d+)$").matchEntire(value)?.let {
            return "Showing ${it.groupValues[1]} / ${it.groupValues[2]} matching LUTs · Total ${it.groupValues[3]}"
        }
        Regex("^Bạn có chắc chắn muốn xóa LUT \\"(.+)\\"\\? LUT này cũng sẽ bị gỡ khỏi mục Bộ lọc\\.$").matchEntire(value)?.let {
            return "Are you sure you want to delete LUT \\"${it.groupValues[1]}\\"? This LUT will also be removed from Filters."
        }
        translatePrefix(value, vietnamesePrefixes)?.let { return it }
        return null
    }

    fun toVietnamese(value: String): String? {
        englishToVietnamese[value]?.let { return it }
        additionalEnglishToVietnamese[value]?.let { return it }
        additionalVietnameseToEnglish.entries.firstOrNull { it.value == value }?.let { return it.key }
        vietnameseToEnglish.entries.firstOrNull { it.value == value }?.let { return it.key }
        Regex("^Star Trails recording: (.+)\\. Press the shutter again to stop after the current frame and merge the trails\\.$").matchEntire(value)?.let {
            return "Star Trails đang ghi: ${it.groupValues[1]}. Nhấn nút chụp lần nữa để dừng và ghép vệt sao."
        }
        Regex("^Saved Star Trails from (\\d+) frames\\.$").matchEntire(value)?.let {
            return "Đã lưu Star Trails từ ${it.groupValues[1]} khung hình"
        }
        Regex("^Burst complete · saved (\\d+)/25 original JPEG photos\\. Speed depends on the camera and storage\\.$").matchEntire(value)?.let {
            return "Burst hoàn tất · đã lưu ${it.groupValues[1]}/25 ảnh JPEG gốc. Tốc độ phụ thuộc camera và bộ nhớ."
        }
        Regex("^Applied ISO (\\d+) · (.+) · WB (\\d+)K\\. Capture 8 photos for alignment and noise reduction\\.$").matchEntire(value)?.let {
            return "Đã áp dụng ISO ${it.groupValues[1]} · ${it.groupValues[2]} · WB ${it.groupValues[3]}K. Chụp 8 ảnh để căn chỉnh và giảm nhiễu."
        }
        Regex("^Color temperature (\\d+) K$").matchEntire(value)?.let {
            return "Tông màu ${it.groupValues[1]} K"
        }
        Regex("^(\\d+) photos$").matchEntire(value)?.let {
            return "${it.groupValues[1]} ảnh"
        }
        Regex("^Showing (\\d+) / (\\d+) matching LUTs · Total (\\d+)$").matchEntire(value)?.let {
            return "Hiển thị ${it.groupValues[1]} / ${it.groupValues[2]} LUT khớp · Tổng ${it.groupValues[3]}"
        }
        Regex("^Are you sure you want to delete LUT \\"(.+)\\"\\? This LUT will also be removed from Filters\\.$").matchEntire(value)?.let {
            return "Bạn có chắc chắn muốn xóa LUT \\"${it.groupValues[1]}\\"? LUT này cũng sẽ bị gỡ khỏi mục Bộ lọc."
        }
        Regex("^Imported (.+) LUT\\. Open Content Manager to add the LUT to filters\\.$").matchEntire(value)?.let {
            return "Đã nhập ${it.groupValues[1]} LUT. Vào Content Manager để Add LUT vào bộ lọc."
        }
        Regex("^Imported (.+) LUT\\. Select Add to enable each filter\\.$").matchEntire(value)?.let {
            return "Đã nhập ${it.groupValues[1]} LUT. Chọn Add để bật từng bộ lọc."
        }
        val reversePrefixes = vietnamesePrefixes.map { Prefix(it.translated, it.source) }
        translatePrefix(value, reversePrefixes)?.let { return it }
        Regex("^(\\d+) filters$").matchEntire(value)?.let {
            return it.groupValues[1] + " bộ lọc"
        }
        Regex("^Loaded Camera Preset (\\d+) · capture settings restored\\.$").matchEntire(value)?.let {
            return "Đã nạp Camera Preset ${it.groupValues[1]} · cấu hình chụp đã khôi phục"
        }
        Regex("^Saved Camera Preset (\\d+)$").matchEntire(value)?.let {
            return "Đã lưu Camera Preset ${it.groupValues[1]}"
        }
        Regex("^Preset (\\d+) has not been saved\\.$").matchEntire(value)?.let {
            return "Preset ${it.groupValues[1]} chưa được lưu"
        }
        Regex("^Saved Film Recipe (\\d+)$").matchEntire(value)?.let {
            return "Đã lưu Film Recipe ${it.groupValues[1]}"
        }
        Regex("^Film Recipe (\\d+) has not been saved\\.$").matchEntire(value)?.let {
            return "Film Recipe ${it.groupValues[1]} chưa được lưu"
        }
        Regex("^Loaded Film Recipe (\\d+)$").matchEntire(value)?.let {
            return "Đã nạp Film Recipe ${it.groupValues[1]}"
        }
        Regex("^(\\d+) more keys\\.$").matchEntire(value)?.let {
            return "Còn " + it.groupValues[1] + " key khác."
        }
        return null
    }
}
