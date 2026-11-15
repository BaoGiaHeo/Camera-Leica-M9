# HDMP Camera 1.4.1

## Version metadata
- versionName: 1.4.1
- versionCode: 5

## Orientation and HDMP processing continuity
- MainActivity handles orientation, screen-size, smallest-screen-size and screen-layout changes through the Android configuration callback instead of being destroyed and recreated for ordinary rotations.
- This preserves the current Compose composition and its CameraScreen coroutine scope while the device changes between portrait and landscape. An in-flight HDMP job is therefore not cancelled solely because the user rotates the device.
- The camera window requests Android's ROTATION_ANIMATION_SEAMLESS mode. Android may fall back to crossfade where the surface cannot rotate seamlessly.
- CameraX target rotation and portrait/landscape geometry continue to be updated by the existing OrientationEventListener and camera binding logic.

## AnimaT and page transitions
- Shared AnimaT entrance now combines a short right-to-left slide, fade-in and subtle scale.
- Shared exit reverses the movement with a light fade-out.
- Main destinations and Settings detail pages now use the same transition system.
- Press feedback and content-size animation are softened to avoid harsh scaling and abrupt reflow.

## Retained functionality
- HDMP AI 2 and HDMP AI 2 Fast selection/fallback.
- Fuji watermark logo and Fuji Classic / Fuji Signature / Fuji Auto templates.
- Existing camera modes, filters, Night Shot, Star Trails and camera information tools.

## Validation and limitations
- GitHub Actions build and model-integrity checks must pass for this revision before an APK is described as build-verified.
- Device tests are still required to verify repeated portrait/landscape changes during an active AI render, camera preview continuity, thermals and frame pacing on the REDMI Turbo 4 Pro and other supported devices.
- This change protects against Activity recreation caused by the handled orientation/size changes. It does not implement durable checkpoint recovery after force-stop or process death.
- The current workflow produces debug APK artifacts. A signed production release requires release signing credentials and install/upgrade testing.
