# Camera look presets

The app's built-in Fujifilm-inspired and film-inspired looks are implemented as compact, app-authored sRGB transforms in `UtilityScreens.kt`.

The previous bundled `.cube` LUT files have been removed from the commercial build because their upstream source is licensed CC BY-NC-SA 4.0. That license does not permit commercial redistribution without separate permission.

The current built-in transforms are visual approximations, not pixel-identical conversions of Fujifilm camera profiles, Lightroom/XMP profiles, or the original non-commercial LUTs. They are applied after the capture/merge pipeline so they do not add work to LunaDream acquisition, alignment, or frame fusion.

The separate CC BY 4.0 source attribution for re-authored looks is recorded in `THIRD_PARTY_NOTICES.txt`.
