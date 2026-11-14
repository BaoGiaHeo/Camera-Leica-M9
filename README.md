# Fujifilm Mode (app-debug)

Ứng dụng camera Android đang trong giai đoạn thử nghiệm. Tên hiển thị của ứng dụng là **Fujifilm Mode**; các bản thử nghiệm được gắn nhãn **app-debug**.

## Giao diện

Bản thử nghiệm hiện tại giữ lại giao diện gốc của dự án. Bản phủ màu/bo góc toàn bộ View trước đây đã bị gỡ vì không tạo được giao diện PhotonCamera đúng nghĩa và làm giảm chất lượng bố cục.

PhotonCamera (https://github.com/bjzhou/PhotonCamera) là nguồn tham khảo cho đợt thiết kế lại tiếp theo. Chưa có tuyên bố rằng giao diện hiện tại đã được chuyển sang PhotonCamera hoặc đã port các màn hình Compose của dự án đó.

## Build

GitHub Actions tạo APK từ `app.zip` và các bản vá Smali của dự án.

## Fujifilm film simulations

The camera filter picker includes the 11 Fujifilm film-simulation looks: Provia, Velvia, Astia, Classic Chrome, Classic Neg, Eterna, Pro Neg Std, Pro Neg Hi, Nostalgic Neg, Reala Ace, and Bleach Bypass. Their sRGB 3D LUT assets are stored in `CamUI-src/app/src/main/assets/film_sims/`.

The filter picker renders each preview from one clean source image. A captured image is retained before the selected look is applied, so changing filters does not stack multiple looks. The picker preview and the saved JPEG use the same LUT and interpolation function. The camera viewfinder itself is not color-graded in real time by this implementation.

The upstream LUT documentation requires a linear-contrast input for a fully matched RAW workflow. Fujifilm Mode currently applies the sRGB LUT to the camera-rendered JPEG; the result is consistent between in-app preview and saved JPEG, but is not claimed to be pixel-identical to Adobe Camera Raw's rendering or a Fujifilm camera's internal film simulation. DNG/RAW data remains sensor data and is not modified by this JPEG filter path.

Profile asset attribution and license terms are documented in `CamUI-src/app/src/main/assets/film_sims/README.md`. These profile assets are under CC BY-NC-SA 4.0 (attribution, share-alike, non-commercial use).
