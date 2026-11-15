# HDMP Camera 1.4

## Implemented in this source revision
- App version code 4 and version name 1.4; experimental suffix removed.
- Quality / Balanced / Speed profile controls in the HDMP settings sheet.
- Speed profile starts HDMP AI 2 Fast directly; the other profiles prefer HDMP AI 2.
- Adaptive ONNX thread limit based on CPU availability, free system RAM, Android thermal status and battery temperature.
- Continuous battery-temperature and Android thermal-status telemetry in the draggable render panel.
- Short inter-tile thermal duty-cycle adjustments; an active ONNX tile is allowed to finish.
- Existing thermal stop keeps completed tiles and uses original pixels for untouched regions.
- Night Shot output ceiling raised to 60MP with a conservative heap-based safe cap.

## Fujifilm watermark update
- Shared logo drawable traced from the user-supplied transparent Fujifilm PNG; black wordmark and red accent preserved, including the original wide aspect ratio.
- Logo-backed watermark styles and previews now use the shared drawable rather than manually typeset FUJIFILM text.
- Added Fuji Auto to choose Fuji Classic for landscape photos and Fuji Signature for portrait photos.
- Added Fuji Classic and Fuji Signature watermark layouts inspired by the supplied landscape and portrait samples.
- Fuji Signature supports an optional user-entered location; it never invents a city name.
- High-resolution white footers append a canvas below the captured image only when a heap budget check allows it; otherwise they use a compact in-image footer.

## Existing features retained in the source
- HDMP AI 2 and HDMP AI 2 Fast fallback, including the manual switch prompt.
- Night Shot 3s / 5s / 10s, Star Trails, FHD / UHD / UHD+ processing.
- Fujifilm film-look LUTs, frame overlay and watermark.
- Camera2 public / disclosed physical ID inventory and orientation-aware capture normalization.

## Still pending before a fully verified Stable release
- Physical-device thermal/performance and camera regression tests.
- Durable render checkpoint recovery after process death.
- Dedicated 25-photo burst capture at the requested 0.2-second target.
- Signed release build and actual device install/upgrade validation.
