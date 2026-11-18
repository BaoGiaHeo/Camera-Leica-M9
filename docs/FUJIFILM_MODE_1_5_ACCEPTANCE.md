# FujiFilm Mode 1.5 — 89-item implementation audit

Audit target: `CamUI-src`, versionName `1.5`, versionCode `6`.

This audit covers all **89/89** listed requirements: **50 CODE**, **11 LIMIT**, **27 DEVICE**, and **1 TEST**. The CamUI unit-test sources contain **22 JUnit test cases** across geometry, EXIF orientation, HDMP decision handling, photo-quality analysis, and Macro lens selection.

## Status legend

- **CODE** — implementation exists in source. This is not proof of physical-device acceptance.
- **LIMIT** — implementation is heuristic, partial, or cannot guarantee the requested physical/optical result.
- **DEVICE** — code is capability-gated; actual support depends on Camera2/HAL and needs a real-device test.
- **TEST** — unit/build/asset check exists in CI; still does not replace a camera-session test.

## A. UI and AnimaT 2.0

| # | Requirement | Audit |
|---:|---|---|
| 1 | Overhaul camera UI | CODE |
| 2 | Redesign viewfinder, shutter, zoom and camera switching | CODE |
| 3 | Compact configurable camera HUD | CODE |
| 4 | Redesign Settings UI | CODE |
| 5 | Film Simulation UI | CODE |
| 6 | Photo Inspector UI | CODE |
| 7 | Shared smooth page/content transitions | CODE |
| 8 | Right-to-left entrance with fade | CODE |
| 9 | Button press feedback | CODE |
| 10 | Menu/panel open-close animation | CODE |
| 11 | Loading skeleton | CODE |
| 12 | Shutter and interaction haptics | CODE |
| 13 | Auto-hide HUD | CODE |
| 14 | Tap, long-press and pinch gestures | CODE |
| 15 | Orientation and screen-size adaptation | DEVICE |
| 16 | Animation Performance Mode and thermal reduction | CODE |
| 17 | Camera Presets | CODE |
| 18 | Shutter capture is not intentionally serialized behind UI animation | DEVICE |

## B. Pro HUD

| # | Requirement | Audit |
|---:|---|---|
| 19 | Live RGB histogram | CODE |
| 20 | Live luminance histogram | CODE |
| 21 | ISO, shutter and EV display | CODE |
| 22 | Focus Peaking | CODE |
| 23 | Zebra Warning | CODE |
| 24 | Focus/exposure status | CODE |
| 25 | Per-element HUD toggles | CODE |

## C. HDMP AI

| # | Requirement | Audit |
|---:|---|---|
| 26 | Smart Detail through the HDMP-owned restoration pipeline | CODE |
| 27 | Model-based denoising/restoration | CODE |
| 28 | Conservative adaptive denoise blending | CODE |
| 29 | Same-resolution detail restoration | CODE |
| 30 | Preserve source content; no generative upscaler is invoked | LIMIT — bounded blending is used, but no neural model can guarantee zero hallucinated or altered texture |
| 31 | HDMP AI 2 primary / HDMP AI 2 Fast fallback and user choice | TEST |
| 32 | Before/after comparison | CODE |
| 33 | Model/tile/progress reporting | CODE |
| 34 | Worker, RAM and thermal-aware execution | DEVICE |

## D. Multi-camera and Smart Macro

| # | Requirement | Audit |
|---:|---|---|
| 35 | Enumerate public Camera2 camera IDs | CODE |
| 36 | Detect logical multi-camera support | DEVICE |
| 37 | Enumerate exposed physical camera IDs | DEVICE |
| 38 | Read focal length, aperture, sensor size and pixel array when published | DEVICE |
| 39 | Deduplicate logical/physical camera entries | CODE |
| 40 | Classify front, ultra-wide, main, tele and close-focus candidates | DEVICE |
| 41 | Smart lens switching using exposed focal/zoom ratios | DEVICE |
| 42 | Reapply eligible manual focus/WB state after camera rebind | DEVICE |
| 43 | Camera hardware diagnostics | CODE |
| 44 | No guessed/hidden camera IDs or hidden-API bypass | CODE |
| 45 | Near-object prompt from focus-distance telemetry | LIMIT — focus diopters are a proxy, not a dedicated distance sensor |
| 46 | Flower icon only when a supported macro/close-focus path is available | DEVICE |
| 47 | First tap requests close-focus/macro route and confirms physical switch when applicable | DEVICE |
| 48 | Second tap exits Macro and returns to 1× AF | DEVICE |
| 49 | Hide Macro control when no supported path is available | DEVICE |
| 50 | Failed lens switch reports an error and returns safely | DEVICE |

## E. Focus and film simulation

| # | Requirement | Audit |
|---:|---|---|
| 51 | Smart Focus Tracking | LIMIT — Camera2 face detection is supported; generic object tracking is not implemented |
| 52 | Tap-to-focus/metering and face-follow metering | DEVICE |
| 53 | Face/object recognition when hardware supports it | LIMIT — hardware face detection is used; there is no general-purpose object detector |
| 54 | Film Simulation Customizer | CODE |
| 55 | Highlight and Shadow controls | CODE |
| 56 | Color Chrome control | CODE |
| 57 | Grain control | CODE |
| 58 | Color Temperature control | CODE |
| 59 | Saturation and color tone controls | CODE |
| 60 | Preview, save and load Film Recipes | CODE |

## F. Night Shot and Milky Way

| # | Requirement | Audit |
|---:|---|---|
| 61 | Night Shot capture/merge pipeline | DEVICE |
| 62 | Preview luminance analysis | CODE |
| 63 | Exposure/frame-count guidance | LIMIT — 3/5/10-second targets map to nominal frame counts; actual capture cadence is HAL-dependent |
| 64 | Frame alignment and stabilization assistance | LIMIT — global translation alignment and camera stabilization are used; this is not full optical-flow motion correction |
| 65 | Highlight-preserving night tone mapping | LIMIT — post-capture tone mapping cannot restore sensor-clipped detail |
| 66 | 3s, 5s and 10s Night Shot options | CODE |
| 67 | HDMP integration after multi-frame capture | DEVICE |
| 68 | Milky Way conditions guidance | LIMIT — uses preview luminance and camera metadata; does not measure cloud cover, moonlight or light pollution |
| 69 | ISO, shutter and WB recommendations | CODE |
| 70 | Manual infinity-focus control when supported | DEVICE |
| 71 | Eight-frame Astro Stack option | DEVICE |
| 72 | Frame alignment and noise averaging | LIMIT — global alignment and averaging; not a dedicated star-registration solver |
| 73 | Reduce star trails while preserving stars | LIMIT — alignment/averaging can reduce small shifts, but cannot guarantee zero trails for moving sky or long exposures |
| 74 | No synthetic stars/celestial objects are generated by a separate image generator | CODE — no generative star overlay is used |

## G. Burst and Photo Inspector

| # | Requirement | Audit |
|---:|---|---|
| 75 | Burst capture upgrades | CODE |
| 76 | Up to 25 JPEG frames with a best-effort ~200ms interval | DEVICE — the camera HAL determines actual cadence |
| 77 | One failed frame does not cancel the remaining burst | CODE |
| 78 | Successful frames are saved and progress is reported | CODE |
| 79 | Photo Inspector redesign | CODE |
| 80 | Encoded pixel dimensions, file size, ISO and shutter metadata | CODE |
| 81 | Logical/physical camera and HDMP status | CODE |
| 82 | JPEG/DNG metadata inspection where decodable/published | DEVICE |

## H. Additional tools

| # | Requirement | Audit |
|---:|---|---|
| 83 | Level and horizon overlay | DEVICE |
| 84 | Five-position Focus Bracketing when manual focus is supported | DEVICE |
| 85 | Three-position HDR Bracketing when exposure compensation permits | DEVICE |
| 86 | RAW/DNG and RAW+ through CameraX/Camera2 capabilities | DEVICE |
| 87 | Photo Quality Analyzer for edge sharpness, noise and clipping suggestions | LIMIT — heuristic only; cannot reliably distinguish shake, subject motion, missed focus, depth of field or natural texture |
| 88 | Lens correction using public HAL correction modes | DEVICE |
| 89 | RAM/thermal/worker-count safeguards and original-photo rollback | DEVICE |

## Specific regression fixes in this branch

- HDMP AI 2 continuation now waits for the user's decision at tile boundaries. **Bỏ qua, tiếp tục** continues width64 past its soft budget; Fast only starts after an explicit Fast choice or safe timeout/failure fallback. Thermal stop still takes priority.
- Macro candidate selection now matches Camera2's normalized rear-facing value `BACK`; five unit tests cover rear selection, front rejection, zoom-range checks, focus-distance threshold and invalid metadata.
- Photo Inspector uses actual encoded bounds plus EXIF orientation and reads pixels once for the quality analysis. It adds near-black clipping reporting.

## Release/acceptance boundary

The source-level audit does not claim that every physical capability is available on every phone. Public Camera2 metadata can be absent or incomplete; a logical camera may not permit every physical lens switch; RAW, manual sensor, OIS, correction modes, focus bracketing and HDR bracketing depend on the HAL.

CI checks can confirm unit tests, Kotlin compilation, APK assembly, model-export smoke tests, ONNX/PyTorch numerical parity and bundled model hashes. They cannot prove real capture behavior, device thermal stability, image quality, or 25-frame cadence on the REDMI Turbo 4 Pro. Those remain required physical-device acceptance tests. The debug build is not a production-signed release.

Video Pro Mode remains intentionally excluded. The About screen remains unchanged.
