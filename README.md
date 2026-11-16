# FujiFilm Mode 2.0.0Dream

FujiFilm Mode is an Android camera application built with CameraX, Camera2 metadata discovery and Jetpack Compose.

## 2.0.0Dream

- App label: **FujiFilm Mode**
- Version: **2.0.0Dream** (versionCode 8)
- LunaDream 1.5: one camera frame, a restrained detail-restoration pass and a 50MP output target.
- LunaDream 2.0: multi-frame alignment/fusion, motion-aware denoising, restrained detail recovery and a 108MP output target.
- LunaDream 1.5 and 2.0 use different capture pipelines. The selected mode stays selected after capture; failures retain the original image and report that processing did not complete.
- The old megapixel chooser and the ONNX/NAFNet AI inference module are removed from the application and the build.
- LunaDream selection uses a rainbow fade and a frosted-glass star accent only while a mode is enabled.
- Physical rotation displays an in-app rotate button. The app starts in portrait and rotates only after the user taps the offered landscape/portrait action; the control fades away after 10 seconds.
- NightShotMode remains a separate capture pipeline.
- Landscape camera controls, settings, filter selection and the LunaDream panel are height-aware and scroll within available space.

## Processing limits and validation

LunaDream 1.5 requests a 50MP output and LunaDream 2.0 requests 108MP. The output sizer uses a device-memory guard, so a lower actual pixel count can be reported rather than risking an out-of-memory crash. Interpolation sets the pixel dimensions; it does not create optical information that was not captured. A claim that LunaDream outperforms every other mode requires side-by-side testing on real captures, with fine detail, noise, motion ghosts, black/bright halos, output resolution and processing time measured separately.

The release workflow runs the app unit tests and assembles both debug and release APKs. The workflow uses the debug signing key for the release variant so it can be installed for device testing. That is **not** a production signing key and the APK is not ready for Play Store publication without the owner's production signing setup.

## Camera implementation notes

The camera inventory reads only public Camera2 logical and physical camera IDs exposed by each device. Lens switching and output sizes remain dependent on the device HAL. Runtime verification is still required for camera switching, autofocus, stabilization, RAW, NightShotMode, landscape orientation, sustained heat and 108MP image output.

## Build locally

From the CamUI-src directory with JDK 17 and Android SDK configured, run:

    gradle :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease
