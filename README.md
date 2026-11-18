# LunaDream 2.5.0dream

LunaDream is an Android camera application built with Kotlin, Jetpack Compose, CameraX and Camera2 metadata discovery.

## 2.5.0dream — hardware-inspired camera interface

- Launcher name: **LunaDream**
- Version: **2.5.0dream** (versionCode 15)
- The camera UI is locked to landscape-left / landscape-right. Android system bars are hidden in immersive mode and can be revealed temporarily with an edge swipe.
- The camera body uses a black leatherette-style surface, brushed silver top plate, knurled rotary controls, engraved labels and a separate LCD preview layer.
- Photo has seven top-plate keys. Pro hides FILM and has six top-plate keys plus a six-cell LCD information strip for EV, Tv, ISO, AF, WB and RESET.
- The left-hand mode rail and right-hand shutter / zoom / command controls can be reversed in Appearance settings.
- Film Filters uses a landscape split view: live-look preview on the left and searchable, categorized 35 mm film tiles on the right. Press and hold the preview to compare the original image. The built-in film-look catalog remains in the camera source.
- LunaDream 1.5 and LunaDream 2.0 retain their existing capture and processing state. NightShotMode has a separate physical switch and duration selection.
- Q Menu and Settings use the shared FujiSkin component family (FujiPanel, FujiTopKey, FujiSwitch, FujiChip, FujiTile, FujiListRow, FujiSearchField, FujiDial and FujiScrollbar).
- Aspect-ratio changes fade the camera surface out for 200 ms, apply the selected ratio, then fade it in over 250 ms. Input is blocked during the transition.
- Physical capture orientation is read independently of the fixed landscape UI. Sensor readings use circular smoothing and a 12-degree hysteresis band; an unknown / flat reading retains the last valid orientation.
- LCD scanline texture is drawn only on the viewfinder display layer and is not passed to the still-image capture output.
- Sound and haptic preferences control the shutter, dial detents, key/switch feedback and vibration. The bundled LunaDream shutter sound is an original 210 ms mono WAV at 44.1 kHz.

## Camera processing is retained

The 2.5.0dream interface update keeps the existing CameraX use cases, Camera2 controls, focus/zoom behavior, filter recipes, LunaDream 1.5 / 2.0 processing paths, NightShotMode and image-orientation normalization. UI work must not silently replace a selected capture mode or change the corresponding processor.

## Important validation notes

The orientation manager and layout have not yet been physically validated across all combinations. Before calling the release complete, test:

- Four physical orientations: landscape-left, landscape-right, portrait and portrait-upside-down.
- Four aspect ratios: 4:3, 16:9, 1:1 and 21:9 (display labels change with device hold direction).
- Front and rear cameras.
- Preview frame geometry, tap-to-focus mapping, output pixel width/height, EXIF orientation and gallery rendering.
- Camera switching, zoom lens transitions, timer capture, burst, LunaDream 1.5 / 2.0, NightShotMode, Star Trails and device thermal behavior.

Upscaling a captured image to a requested megapixel count changes output dimensions; it does not create optical detail that the sensor did not capture. A claim of improved detail or denoising quality requires side-by-side real-device captures and measurements.

## Build locally

From the `CamUI-src` directory with JDK 17 and Android SDK configured:

```bash
gradle :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease
```

The workflow's release APK is debug-signed for installable device testing. It is not a production Play Store signing configuration.
