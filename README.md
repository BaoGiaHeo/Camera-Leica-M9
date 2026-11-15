# Fujifilm Mode — HDMP Camera 1.4

Android camera application with HDMP-controlled computational photography.

## Version 1.4 source update

- App metadata uses version **1.4** and no longer carries an experimental suffix.
- HDMP AI provides **Quality**, **Balanced**, and **Speed** profiles. Speed selects HDMP AI 2 Fast directly.
- ONNX intra-op threads are bounded by CPU availability, free system memory, and current Android thermal/battery readings.
- The processing panel continuously displays battery temperature and Android thermal status and can be moved by dragging.
- Brief thermal duty-cycle adjustments happen between completed inference tiles; active native ONNX inference is not interrupted.
- The thermal-stop path keeps completed tiles and fills untouched regions from the original capture.
- Night Shot has a **60MP upper ceiling** with a lower heap-derived safe cap when device memory is limited.

## AI processing design

HDMP remains the pipeline coordinator and final image validator. HDMP AI 2 uses the primary NAFNet model; HDMP AI 2 Fast is used when selected by the user or when the primary model needs fallback. Fast starts from the original capture, not from a partially processed primary output. These models perform conservative restoration/denoising; they are not generative upscalers and do not guarantee a fixed speed multiplier or invented detail.

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
