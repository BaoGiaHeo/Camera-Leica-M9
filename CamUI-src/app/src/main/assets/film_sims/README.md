# Fujifilm film-simulation LUT assets

This directory contains the 11 sRGB 3D LUTs used by Fujifilm Mode:

The linked TingfengLuo repository contains 11 unique Fujifilm Creative Profiles (`.xmp) and 11 `wrapper.xmp` preset files. Each wrapper only selects its matching Creative Profile; it is not an additional filter. The 11 bundled looks below match those 11 unique profile names. The repository's 1,464 camera-specific `.dcp files are linear base profiles, not additional film looks, so they are not shown as filters in the app.


- Provia
- Velvia
- Astia
- Classic Chrome
- Classic Neg
- Eterna
- Pro Neg Std
- Pro Neg Hi
- Nostalgic Neg
- Reala Ace
- Bleach Bypass

## Source and attribution

The corresponding Adobe Creative Profiles / XMP profile collection was published by TingfengLuo:
https://github.com/TingfengLuo/Camera-Profile-for-Fujifilm-Film-Simulation

The profile README identifies the embedded film-look tables as originating from abpy/FujifilmCameraProfiles:
https://github.com/abpy/FujifilmCameraProfiles

The .cube runtime assets here are the sRGB variants of those 3D look tables, selected because this app's current photo pipeline processes Android JPEG bitmaps in an sRGB-oriented pipeline.

## License

The upstream FujifilmCameraProfiles look profiles are licensed under Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International (CC BY-NC-SA 4.0):
https://creativecommons.org/licenses/by-nc-sa/4.0/

Attribution is retained here. Adaptations and redistribution of these profile assets must use the same license and must not be sold. This restriction applies to the LUT/profile assets, not to photographs produced by applying them.

## Preview and capture consistency

The filter picker and the saved-JPEG path call the same pixel-transform function. For these 11 Fujifilm profiles, both paths load the same bundled 32³ sRGB cube and use the same trilinear interpolation. Each preview is rendered from the clean, unfiltered source image; selecting another preset never compounds it on top of the previous preview. Other retained filter looks use their same shared per-pixel transform for preview and capture.

## Important color-pipeline note

The upstream documentation says the main film-look LUTs are designed for images with linear contrast and a linear camera profile. The original XMP Creative Profiles are intended for a RAW/DNG workflow using an Adobe Standard Linear base profile. The current Fujifilm Mode JPEG pipeline applies the sRGB LUT directly to the camera-rendered JPEG; therefore its visual output is internally consistent between the filter-picker preview and saved JPEG, but it is not guaranteed to be pixel-identical to Adobe Camera Raw's RAW rendering or to an in-camera Fujifilm simulation. DNG/RAW data is not rewritten by this JPEG processing path.
