# Offline LUT import

```text
assets/luts/
  fujifilm/
  leica/
  panasonic-lumix/
  sony/
  cinema-log/
  hald-clut/
  other/
```

Place the files you obtained yourself under the matching group. This tool does not
download LUTs, contact remote services, or add imported files to the APK.

## Metadata sidecar

For each source file, add either filename.ext.meta.json or filename.meta.json.
The sidecar describes the individual LUT asset; do not assume that a repository's
code licence automatically covers every LUT, profile, sample image or vendor datum.

```json
{
  "name_en": "Example film look",
  "name_vi": "Mô phỏng phim ví dụ",
  "category": "Fujifilm",
  "source": "https://example.org/original-lut",
  "license": "CC-BY-4.0",
  "attribution": "Creator / project name",
  "input_space": "sRGB",
  "input_gamut": "sRGB"
}
```

Use an SPDX-style licence identifier where possible. Supported redistributable
identifiers are MIT, Apache-2.0, BSD-2-Clause, BSD-3-Clause, 0BSD, OFL-1.1,
CC-BY-4.0 and CC-BY-SA-4.0. Creative Commons entries require a non-empty
attribution. GPL-3.0 and AGPL-3.0 entries are skipped. Unknown or unapproved
licences can be converted for local review, but the manifest marks them
local_only_review and bundle_candidate=false.

For CC BY-SA assets, preserve the original licence and attribution for the
converted LUT data file. This does not change the licence of the app source code.
Do not bundle an asset into a release unless its individual redistribution
permission has been checked.

## Run

```bash
python tools/lut_ingest.py --input assets/luts --output build/lut-output --size 33
python tools/lut_ingest.py --input assets/luts --output build/lut-output-65 --size 65
python tools/lut_ingest.py --self-test
```

The manifest is written to luts_manifest.json. Converted LUTs and 256px
thumbnails are written under cubes/<group>/ and thumbnails/. Source files
are never overwritten.

## Formats

- .cube: 3D tables in R-fastest, then G, then B order. Combined 1D+3D shaper
  files are rejected rather than silently interpreted incorrectly.
- Panasonic .vlt: ASCII VARICAM 3DLUT. Integer outputs in 0..4095 are
  normalized to 0..1. Input space/gamut must be declared in the sidecar when known.
- HaldCLUT .png: square RGB/RGBA, 8-bit or 16-bit, non-interlaced PNG. The edge
  must be a perfect cube L^3; the Hald grid has N=L^2 samples per channel.

The converter uses trilinear interpolation only for offline grid-size conversion.
The app's real-time LUT renderer still needs its own GPU interpolation implementation
and colour-space adapter. A Log LUT must have an explicit input_space and
input_gamut; this import tool does not infer V-Log, S-Log3 or other log encodings.
