# LUT source licence audit

Checked on 2026-11-23. This is a source-level audit, not permission to redistribute
every asset stored in the repositories below. The licence of each individual LUT,
profile, sample image and vendor-derived data file must still be recorded in that
file's metadata sidecar before it can be marked as a release bundle candidate.

| Source | Repository licence / notice observed | Ingest policy |
|---|---|---|
| [YahiaAngelo/Film-Luts](https://github.com/YahiaAngelo/Film-Luts) | Repository LICENSE is MIT. Its README explicitly says the maintainer does not claim ownership of the LUTs and that individual LUTs may be subject to their creators' rights. | Do not infer MIT for every LUT. Require an asset-specific licence/attribution sidecar; otherwise local-only review. |
| [slaycamera/luts](https://github.com/slaycamera/luts) | The README identifies five original .cube LUTs as CC BY 4.0 and requires credit to slay. | May be a release candidate only with CC BY 4.0 attribution and source recorded. |
| [cedeber/hald-clut](https://github.com/cedeber/hald-clut) | The repository LICENSE is GPL-3.0. That differs from the CC BY-SA assumption in the project request. The individual PNGs have not been separately cleared. | Do not copy repository code. Do not bundle PNGs until each asset has an independently verified licence; mark GPL-3.0 assets restricted. |
| [shenmintao/V-Log-Alchemy](https://github.com/shenmintao/V-Log-Alchemy) | Repository LICENSE is Apache-2.0. README describes creative LUTs authored for Panasonic V-Log/V-Gamut input, with conversion adapters and recovered vendor transforms. | Preserve Apache notices for copied code; verify the individual LUT/profile and any vendor-derived data rights before bundling. Record V-Log/V-Gamut as input only when the exact file is documented that way. |
| [hstockeb/film-simulations-and-presets](https://github.com/hstockeb/film-simulations-and-presets) | README identifies the preset collection as CC BY 4.0. | Release candidate with creator attribution, original source and CC BY 4.0 metadata. |
| [751K/sony-look-bridge](https://github.com/751K/sony-look-bridge) | README states code/documentation/profile-generation work is MIT, but third-party data in profiles (including Sony look parameters and Adobe colour matrices) is not covered by that licence. | MIT may cover source code only. Do not bundle derived Sony/Adobe profile data without separate permission/terms. |
| [MatthewGreenberg/fujilab](https://github.com/MatthewGreenberg/fujilab) | No LICENSE file was found at the repository root during this check. README describes .3dl LUTs derived from another Fuji X-Trans LUT pack. | No code or LUT copying. Use only high-level algorithmic observations until a clear licence and asset provenance are established. |

## Attribution and release rule

- Each imported asset must have a sidecar with source URL, SPDX-style licence,
  attribution, input colour space and input gamut.
- Unknown licence is not an automatic release permission. The importer labels it
  local_only_review and sets bundle_candidate to false.
- GPL-3.0/AGPL-3.0 sources are excluded from the release bundle by the importer.
- CC BY 4.0 requires attribution. CC BY-SA 4.0 additionally requires the
  converted LUT data to retain the applicable ShareAlike terms; this does not
  relicense the application source.
- About > Credits should list only assets actually included in the build, using
  their exact sidecar attribution. This file is an audit record, not the final
  runtime Credits list.
- FujiFilm, Leica, Panasonic, Sony, Kodak and other manufacturer names are
  identifiers for compatibility/look categories only. This app is not an
  official film simulation or endorsed camera product from those companies.
