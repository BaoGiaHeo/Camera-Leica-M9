# FujiFilm Mode 1.6 — implementation notes

## HDMP selection
- Select one model before capture: HDMP AI 2 (width64) or HDMP AI 1.5 (width32).
- Select an output target of 24, 48, 60, 108 or 250MP.
- No post-capture model-choice dialog. An incomplete selected-model run is not reported as a successful AI render.
- Performance targets: AI 1.5 aims for 10s with a 20s processing budget; AI 2 aims for 20s with a 30s processing budget. These must be confirmed by repeatable device benchmarks; setting a software timeout alone does not guarantee completion time.

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
