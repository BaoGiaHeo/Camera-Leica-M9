# Fujifilm Mode (app-debug)

Ứng dụng camera Android đang trong giai đoạn thử nghiệm. Tên hiển thị của ứng dụng là **Fujifilm Mode**; các bản thử nghiệm được gắn nhãn **app-debug**.

## Giao diện

Bản thử nghiệm hiện tại giữ lại giao diện gốc của dự án. Bản phủ màu/bo góc toàn bộ View trước đây đã bị gỡ vì không tạo được giao diện PhotonCamera đúng nghĩa và làm giảm chất lượng bố cục.

PhotonCamera (https://github.com/bjzhou/PhotonCamera) là nguồn tham khảo cho đợt thiết kế lại tiếp theo. Chưa có tuyên bố rằng giao diện hiện tại đã được chuyển sang PhotonCamera hoặc đã port các màn hình Compose của dự án đó.

## Build

GitHub Actions tạo APK từ `app.zip` và các bản vá Smali của dự án.
