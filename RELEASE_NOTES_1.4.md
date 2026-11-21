# Camera processing notes 1.4 (historical)

## Implemented in this source revision
- App version code 4 and version name 1.4; experimental suffix removed.
- The HDMP image pipeline uses bounded, row-tiled CPU processing and validates the saved output before reporting success.
- Processing limits respond to available memory, CPU capacity and Android thermal state.
- An interrupted render preserves the original image; partially rendered data is never published as a completed photo.
- Night Shot output ceiling raised to 60MP with a conservative heap-based safe cap.

## Fujifilm watermark update
- Shared logo drawable traced from the user-supplied transparent Fujifilm PNG; black wordmark and red accent preserved, including the original wide aspect ratio.
- Logo-backed watermark styles and previews now use the shared drawable rather than manually typeset FUJIFILM text.
- Added Fuji Auto to choose Fuji Classic for landscape photos and Fuji Signature for portrait photos.
- Added Fuji Classic and Fuji Signature watermark layouts inspired by the supplied landscape and portrait samples.
- Fuji Signature supports an optional user-entered location; it never invents a city name.
- High-resolution white footers append a canvas below the captured image only when a heap budget check allows it; otherwise they use a compact in-image footer.

## Existing features retained in the source
- Deterministic HDMP FHD / UHD / UHD+ processing and final-output validation.
- Night Shot 3s / 5s / 10s, Star Trails, FHD / UHD / UHD+ processing.
- Fujifilm film-look LUTs, frame overlay and watermark.
- Camera2 public / disclosed physical ID inventory and orientation-aware capture normalization.

## Still pending before a fully verified Stable release
- Physical-device thermal/performance and camera regression tests.
- Durable render checkpoint recovery after process death.
- Dedicated 25-photo burst capture at the requested 0.2-second target.
- Signed release build and actual device install/upgrade validation.
