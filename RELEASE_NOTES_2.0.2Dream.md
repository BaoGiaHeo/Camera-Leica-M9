# FujiFilm Mode 2.0.2Dream

## Fixes
- LunaDream 2.0 no longer reads one pixel row beyond the bitmap at the bottom of its final processing stripe.
- Added regression tests for full and partial final-stripe boundaries.
- Reduced the strength-2 denoise blend and bounded detail recovery more conservatively to reduce halos and harsh edge outlines.
- Kept the LunaDream 1.5 fusion and finishing pipeline unchanged.

## Full-frame aspect ratios
- Replaced the old ratio list with 16:9, 21:9, 3:4 and 1:1.
- Orientation normalization no longer crops the sensor image to the selected display ratio.
- Normal photos and processed photos are fitted uniformly into the selected output canvas; unused space is black letterboxing.
- The preview remains FIT_CENTER with CameraX ViewPort.FIT for portrait and landscape rotation.
- Watermark rendering runs after the output ratio/MP step and keeps the final image dimensions unchanged.

## Watermark logos
- Added transparent vector wordmarks from Logo.zip for DJI, GoPro, Hasselblad, Insta360, Kodak, LUMIX and ZEISS.
- Connected the new logos alongside the existing Sony, Nikon, Canon and Fujifilm assets.
- Watermark footer style remains limited to white and black.
- Watermark brand list does not expose Olympus, Pentax, OM System or Sigma.

## Version and verification
- Android version name: 2.0.2Dream
- Android version code: 11
- CI runs unit tests, assembles debug and release APKs, and runs the emulator cold-start smoke test.
- Device validation on REDMI Turbo 4 Pro is still required for real-camera aspect ratio, watermark appearance, thermals and LunaDream processing time.
