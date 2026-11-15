# FujiFilm Mode 1.6 — implementation notes

## HDMP selection
- Select one mode before capture: HDMP AI 2 (width64), HDMP AI 1.5 (width32), or OFF HDMP AI (normal capture without HDMP AI processing).
- Select an output target of 24, 48, 60, 108 or 250MP.
- No post-capture model-choice dialog and no automatic switch to classical HDMP. A selected AI model stays locked for the entire render.
- The selected NAFNet model is the complete HDMP stage; the legacy classical HDMP post-pass is skipped after AI rendering.
- NAFNet tensor input, validation and output use RGB/NCHW channel order and are covered by unit tests.
- An image is committed only after all tiles from the selected model finish; partial AI output is never marked complete.
- AI 1.5 targets about 10s; AI 2 targets about 20s. These are optimization targets, not hard cutoffs: processing continues with the selected model after either target is exceeded.
- A failed AI run must not be presented as a successful AI result or silently replaced with classical processing. The original capture is retained for recovery.

## Capture pipeline
- High-resolution still capture uses a single captured frame, then HDMP processing and output resampling; it does not use the former five-frame merge.
- NightShot retains a separate multi-frame capture, alignment, merge and controlled night-tone lift.
- Milky Way is an opt-in NightShot setting (off by default), not a normal Photo-mode stack.
- Keep the device steady while frames are acquired/aligned/merged. Once capture/merge is complete and HDMP processing begins, the phone may move freely.

## Acceptance still required
- Run Gradle unit tests and full-HDMP APK assembly.
- Benchmark both models on the REDMI Turbo 4 Pro with identical source images, output targets, thermal state and camera conditions.
- Test each supported camera/HAL for capture sizes, RAM safety, output dimensions, orientation/EXIF and NightShot alignment.
- Software resampling increases output pixels; it does not create true optical detail at 250MP.
- The About page remains unchanged. Video Pro is intentionally excluded.
