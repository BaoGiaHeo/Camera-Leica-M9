# Fujifilm Mode — HDMP Camera 1.4.1

Android camera application with HDMP-controlled computational photography.

## Version 1.4.1 update

- App metadata is **1.4.1** (versionCode 5).
- The camera Activity handles orientation, screen-size, smallest-screen-size and screen-layout changes without being recreated. This keeps the active CameraScreen Compose scope alive when the user rotates the device, so rotation alone no longer cancels an in-flight HDMP render.
- Android's camera-friendly seamless rotation mode is requested for the app window; Android can fall back to crossfade when a seamless surface rotation is unavailable.
- AnimaT now uses a consistent right-to-left slide, soft fade and subtle scale for opening pages. Settings detail pages use the same motion in reverse when closing.
- Press feedback and content-size transitions are softened to reduce abrupt movement.

**Scope:** this specifically protects processing from Activity recreation caused by orientation/size changes. It is not a durable checkpoint for force-stop, process death or an operating-system kill. Actual rotation smoothness and camera preview behavior still require testing on physical devices.


## Version 1.4 baseline features retained in 1.4.1

- The 1.4 baseline removed the experimental suffix; version 1.4.1 increments metadata to versionCode 5.
- HDMP AI provides **Quality**, **Balanced**, and **Speed** profiles. Speed selects HDMP AI 2 Fast directly.
- ONNX intra-op threads are bounded by CPU availability, free system memory, and current Android thermal/battery readings.
- The processing panel continuously displays battery temperature and Android thermal status and can be moved by dragging.
- Brief thermal duty-cycle adjustments happen between completed inference tiles; active native ONNX inference is not interrupted.
- The thermal-stop path keeps completed tiles and fills untouched regions from the original capture.
- Night Shot has a **60MP upper ceiling** with a lower heap-derived safe cap when device memory is limited.

## AI processing design

HDMP remains the pipeline coordinator and final image validator. HDMP AI 2 uses the primary NAFNet model; HDMP AI 2 Fast is used when selected by the user or when the primary model needs fallback. Fast starts from the original capture, not from a partially processed primary output. These models perform conservative restoration/denoising; they are not generative upscalers and do not guarantee a fixed speed multiplier or invented detail.

## Watermark designs

- All logo-bearing watermark styles use one shared Fujifilm mark derived from the transparent logo image supplied for this project. The drawable keeps the black wordmark, red accent and original aspect ratio.
- Existing Standard 1, Standard 2, Blur, Center Logo and Liquid Glass previews use the shared mark; the watermark renderer draws the image asset instead of rebuilding the FUJIFILM lettering.
- Added **Fuji Auto**, which selects Fuji Classic for landscape captures and Fuji Signature for portrait captures using the final upright pixel dimensions.
- Added **Fuji Classic** (landscape-style white footer with logo left and camera settings right) and **Fuji Signature** (centered logo, “Shot on” model/date line and optional location line).
- Fuji Signature location is user-entered in Watermark settings; the app does not fabricate GPS place names.
- White-footer templates append space below the captured photo when the current memory budget safely permits it. Otherwise they use a compact in-image footer without resizing the source.

## Existing camera functionality

The source includes Pro/Photo capture, FHD/UHD/UHD+, Night Shot, Star Trails, Fujifilm LUT looks, image overlays/watermark, Camera2 logical/physical camera inventory, and orientation-aware capture normalization. Camera hardware enumeration uses only IDs and physical IDs exposed through public Android Camera2 APIs; hidden OEM IDs cannot be queried or fabricated by an ordinary application.

## Still requiring validation before declaring a fully verified Stable release

A successful cloud build does not replace physical-device tests. The following still need dedicated validation:

- Thermal, CPU and RAM behavior on real devices, including the target Redmi device.
- Actual maximum Night Shot output size; the memory guard can select a lower size than 60MP.
- Durable render checkpoint/restore after the app process is killed.
- A dedicated 25-frame / 0.2-second burst mode.
- Signed release APK, install/upgrade tests and longer camera-session regression tests.

## Fujifilm film simulations

The film-look picker uses the 11 LUT assets in `CamUI-src/app/src/main/assets/film_sims/`. Attribution and licensing terms are documented alongside those assets. The current path applies sRGB LUTs to captured JPEGs; it does not claim pixel-identical rendering to Adobe Camera Raw or Fujifilm in-camera processing.
