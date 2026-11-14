# Fujifilm film-simulation LUT assets

This directory contains the 11 sRGB 3D LUTs used by Fujifilm Mode:

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

## Important color-pipeline note

The upstream documentation says the main film-look LUTs are designed for images with linear contrast and a linear camera profile. The original XMP Creative Profiles are intended for a RAW/DNG workflow using an Adobe Standard Linear base profile. The current Fujifilm Mode JPEG pipeline applies the sRGB LUT directly to the camera-rendered JPEG; therefore its visual output is internally consistent between the filter-picker preview and saved JPEG, but it is not guaranteed to be pixel-identical to Adobe Camera Raw's RAW rendering or to an in-camera Fujifilm simulation. DNG/RAW data is not rewritten by this JPEG processing path.
