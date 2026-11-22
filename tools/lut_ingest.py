#!/usr/bin/env python3
"""Offline LUT ingest: .cube, Panasonic .vlt, and 8/16-bit HaldCLUT PNG.

Input layout:
    assets/luts/<group>/**/*.(cube|vlt|png)
Optional per-LUT metadata:
    <filename>.json  {"source": "...", "license": "...", "attribution": "...",
                      "input_space": "...", "output_space": "..."}
Optional LICENSE/COPYING files are discovered in the input folder hierarchy.

The tool never downloads LUTs and never assumes an unknown license is redistributable.
"""
from __future__ import annotations

import argparse
import array
import hashlib
import json
import math
import re
import struct
import sys
import zipfile
import zlib
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"
LICENSE_NAMES = ("LICENSE", "LICENSE.txt", "COPYING", "COPYING.txt", "NOTICE")
GROUP_NAMES = {
    "fujifilm": "Fujifilm",
    "fuji": "Fujifilm",
    "leica": "Leica",
    "panasonic": "Panasonic Lumix",
    "lumix": "Panasonic Lumix",
    "panasonic_lumix": "Panasonic Lumix",
    "sony": "Sony",
    "cinema": "Cinema-Log",
    "cinema_log": "Cinema-Log",
    "cinema-log": "Cinema-Log",
    "log": "Cinema-Log",
    "hald": "Hald CLUT",
    "hald_clut": "Hald CLUT",
    "hald-clut": "Hald CLUT",
    "other": "Other",
}


@dataclass
class Lut:
    size: int
    values: array.array
    red_fastest: bool
    title: str = ""
    domain_min: tuple[float, float, float] = (0.0, 0.0, 0.0)
    domain_max: tuple[float, float, float] = (1.0, 1.0, 1.0)
    one_d_size: int = 0
    one_d_values: array.array | None = None
    one_d_domain_min: tuple[float, float, float] = (0.0, 0.0, 0.0)
    one_d_domain_max: tuple[float, float, float] = (1.0, 1.0, 1.0)

    def _index(self, r: int, g: int, b: int, channel: int) -> int:
        if self.red_fastest:
            voxel = (b * self.size + g) * self.size + r
        else:
            voxel = (r * self.size + g) * self.size + b
        return voxel * 3 + channel

    @staticmethod
    def _mix(a: float, b: float, t: float) -> float:
        return a + (b - a) * t

    def sample(self, red: float, green: float, blue: float) -> tuple[float, float, float]:
        coords = [red, green, blue]
        if self.one_d_size >= 2 and self.one_d_values is not None:
            mapped: list[float] = []
            for channel, value in enumerate(coords):
                lo_domain = self.one_d_domain_min[channel]
                hi_domain = self.one_d_domain_max[channel]
                span = max(1e-8, hi_domain - lo_domain)
                position = min(1.0, max(0.0, (value - lo_domain) / span)) * (self.one_d_size - 1)
                low = min(self.one_d_size - 1, max(0, int(position)))
                high = min(self.one_d_size - 1, low + 1)
                fraction = position - low
                a = self.one_d_values[low * 3 + channel]
                b = self.one_d_values[high * 3 + channel]
                mapped.append(min(1.0, max(0.0, self._mix(a, b, fraction))))
            coords = mapped

        low: list[int] = []
        high: list[int] = []
        fraction: list[float] = []
        for channel, value in enumerate(coords):
            span = max(1e-8, self.domain_max[channel] - self.domain_min[channel])
            normalized = min(1.0, max(0.0, (value - self.domain_min[channel]) / span))
            position = normalized * (self.size - 1)
            lo = min(self.size - 1, max(0, int(position)))
            low.append(lo)
            high.append(min(self.size - 1, lo + 1))
            fraction.append(position - lo)

        r0, g0, b0 = low
        r1, g1, b1 = high
        rt, gt, bt = fraction
        output: list[float] = []
        for channel in range(3):
            c00 = self._mix(self.values[self._index(r0, g0, b0, channel)],
                            self.values[self._index(r0, g0, b1, channel)], bt)
            c01 = self._mix(self.values[self._index(r0, g1, b0, channel)],
                            self.values[self._index(r0, g1, b1, channel)], bt)
            c10 = self._mix(self.values[self._index(r1, g0, b0, channel)],
                            self.values[self._index(r1, g0, b1, channel)], bt)
            c11 = self._mix(self.values[self._index(r1, g1, b0, channel)],
                            self.values[self._index(r1, g1, b1, channel)], bt)
            value = self._mix(self._mix(c00, c01, gt), self._mix(c10, c11, gt), rt)
            output.append(min(1.0, max(0.0, value)))
        return output[0], output[1], output[2]


def parse_cube(path: Path) -> Lut:
    text = path.read_text(encoding="utf-8-sig")
    size3d = 0
    size1d = 0
    title = path.stem
    domain_min = [0.0, 0.0, 0.0]
    domain_max = [1.0, 1.0, 1.0]
    one_d_domain_min = [0.0, 0.0, 0.0]
    one_d_domain_max = [1.0, 1.0, 1.0]
    data = array.array("f")

    for raw in text.splitlines():
        line = raw.split("#", 1)[0].strip()
        if not line:
            continue
        parts = line.split()
        directive = parts[0]
        if directive == "TITLE":
            match = re.search(r'"([^"]*)"', line)
            if match:
                title = match.group(1)
            continue
        if directive in ("LUT_1D_SIZE", "LUT_3D_SIZE"):
            if len(parts) != 2:
                raise ValueError(f"{directive} must contain one integer")
            value = int(parts[1])
            if directive == "LUT_1D_SIZE":
                size1d = value
                if not 2 <= size1d <= 65536:
                    raise ValueError("LUT_1D_SIZE must be between 2 and 65536")
            else:
                size3d = value
                if not 2 <= size3d <= 129:
                    raise ValueError("LUT_3D_SIZE must be between 2 and 129")
            continue
        if directive in ("DOMAIN_MIN", "DOMAIN_MAX", "LUT_1D_INPUT_RANGE"):
            if len(parts) not in (3, 4):
                raise ValueError(f"{directive} requires two or three components")
            values = [float(value) for value in parts[1:]]
            if len(values) == 2:
                values = values * 3
            if not all(math.isfinite(value) for value in values):
                raise ValueError(f"{directive} contains a non-finite component")
            if directive == "DOMAIN_MIN":
                domain_min = values
            elif directive == "DOMAIN_MAX":
                domain_max = values
            else:
                one_d_domain_min = values
                one_d_domain_max = [float(parts[2])] * 3
            continue
        if directive.isalpha() or directive.startswith("_"):
            # Ignore standard informational directives such as 3D_INPUT_RANGE.
            continue
        if len(parts) != 3:
            raise ValueError(f"invalid .cube row: {raw[:80]}")
        values = tuple(float(part) for part in parts)
        if not all(math.isfinite(value) for value in values):
            raise ValueError("non-finite value in .cube")
        data.extend(values)

    if size3d < 2:
        raise ValueError("only 3D .cube files are accepted by this ingest tool")
    expected1d = size1d * 3
    expected3d = size3d * size3d * size3d * 3
    expected_total = expected1d + expected3d
    if len(data) != expected_total:
        raise ValueError(
            f".cube row count mismatch: expected {expected_total // 3}, got {len(data) // 3}"
        )
    if any(not math.isfinite(lo) or not math.isfinite(hi) or hi <= lo
           for lo, hi in zip(domain_min, domain_max)):
        raise ValueError("DOMAIN_MIN/MAX must be finite increasing ranges")
    one_d_values = array.array("f", data[:expected1d]) if size1d else None
    three_d_values = array.array("f", data[expected1d:])
    return Lut(
        size=size3d,
        values=three_d_values,
        red_fastest=False,
        title=title,
        domain_min=tuple(domain_min),
        domain_max=tuple(domain_max),
        one_d_size=size1d,
        one_d_values=one_d_values,
        one_d_domain_min=tuple(one_d_domain_min),
        one_d_domain_max=tuple(one_d_domain_max),
    )


def parse_vlt(path: Path) -> Lut:
    text = path.read_text(encoding="ascii")
    size = 0
    rows: list[tuple[float, float, float]] = []
    for raw in text.splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("LUT_3D_SIZE"):
            parts = line.split()
            if len(parts) != 2:
                raise ValueError("VLT LUT_3D_SIZE header is malformed")
            size = int(parts[1])
            continue
        parts = line.split()
        if len(parts) != 3:
            raise ValueError(f"invalid VLT data row: {raw[:80]}")
        channels = tuple(int(part) for part in parts)
        if any(value < 0 or value > 4095 for value in channels):
            raise ValueError("Panasonic VLT channel values must be in 0..4095")
        rows.append(tuple(value / 4095.0 for value in channels))
    if size < 2 or size > 33:
        raise ValueError("Panasonic VLT grid size must be in 2..33")
    expected = size ** 3
    if len(rows) != expected:
        raise ValueError(f"VLT row count mismatch: expected {expected}, got {len(rows)}")
    values = array.array("f")
    for row in rows:
        values.extend(row)
    # Panasonic VariCam VLT enumerates R first, then G, then B (R-fastest).
    return Lut(size=size, values=values, red_fastest=True, title=path.stem)


def _paeth(a: int, b: int, c: int) -> int:
    p = a + b - c
    pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
    if pa <= pb and pa <= pc:
        return a
    if pb <= pc:
        return b
    return c


def decode_png_rgb(path: Path) -> tuple[int, int, int, array.array]:
    data = path.read_bytes()
    if not data.startswith(PNG_SIGNATURE):
        raise ValueError("not a PNG file")
    offset = len(PNG_SIGNATURE)
    width = height = bit_depth = color_type = interlace = 0
    compressed = bytearray()
    while offset < len(data):
        if offset + 8 > len(data):
            raise ValueError("truncated PNG chunk")
        length = struct.unpack_from(">I", data, offset)[0]
        chunk_type = data[offset + 4:offset + 8]
        start = offset + 8
        end = start + length
        if end + 4 > len(data):
            raise ValueError("truncated PNG data")
        payload = data[start:end]
        if chunk_type == b"IHDR":
            width, height, bit_depth, color_type, compression, filtering, interlace = struct.unpack(">IIBBBBB", payload)
            if compression != 0 or filtering != 0:
                raise ValueError("unsupported PNG compression or filter method")
        elif chunk_type == b"IDAT":
            compressed.extend(payload)
        elif chunk_type == b"IEND":
            break
        offset = end + 4

    if width <= 0 or height <= 0:
        raise ValueError("PNG has no valid IHDR")
    if width > 4096 or height > 4096 or width != height:
        raise ValueError("HaldCLUT PNG dimensions must be square and no larger than 4096x4096")
    if bit_depth not in (8, 16):
        raise ValueError("HaldCLUT PNG must be 8-bit or 16-bit")
    if color_type not in (2, 6):
        raise ValueError("HaldCLUT PNG must use RGB or RGBA color")
    if interlace != 0:
        raise ValueError("interlaced HaldCLUT PNG is not supported")

    channels = 3 if color_type == 2 else 4
    bytes_per_sample = bit_depth // 8
    bytes_per_pixel = channels * bytes_per_sample
    row_bytes = width * bytes_per_pixel
    expected_raw_bytes = height * (row_bytes + 1)
    if expected_raw_bytes > 256 * 1024 * 1024:
        raise ValueError("HaldCLUT PNG expands beyond the 256 MiB safety limit")
    inflater = zlib.decompressobj()
    raw = inflater.decompress(bytes(compressed), expected_raw_bytes + 1)
    if len(raw) != expected_raw_bytes or not inflater.eof or inflater.unconsumed_tail:
        raise ValueError("PNG decompressed byte count does not match dimensions or exceeds the safety limit")

    maximum = 255 if bit_depth == 8 else 65535
    samples = array.array("f")
    previous = bytearray(row_bytes)
    cursor = 0
    for _y in range(height):
        filter_type = raw[cursor]
        cursor += 1
        scanline = bytearray(raw[cursor:cursor + row_bytes])
        cursor += row_bytes
        for index in range(row_bytes):
            left = scanline[index - bytes_per_pixel] if index >= bytes_per_pixel else 0
            above = previous[index]
            upper_left = previous[index - bytes_per_pixel] if index >= bytes_per_pixel else 0
            if filter_type == 1:
                predictor = left
            elif filter_type == 2:
                predictor = above
            elif filter_type == 3:
                predictor = (left + above) // 2
            elif filter_type == 4:
                predictor = _paeth(left, above, upper_left)
            elif filter_type == 0:
                predictor = 0
            else:
                raise ValueError(f"unsupported PNG row filter {filter_type}")
            scanline[index] = (scanline[index] + predictor) & 0xFF

        for pixel in range(width):
            base = pixel * bytes_per_pixel
            for channel in range(3):
                sample_offset = base + channel * bytes_per_sample
                if bit_depth == 8:
                    value = scanline[sample_offset]
                else:
                    value = int.from_bytes(scanline[sample_offset:sample_offset + 2], "big")
                samples.append(value / maximum)
        previous = scanline
    return width, height, bit_depth, samples


def parse_hald_png(path: Path) -> Lut:
    width, height, _depth, pixels = decode_png_rgb(path)
    if width != height:
        raise ValueError("HaldCLUT image must be square")
    level = round(width ** (1.0 / 3.0))
    if level < 2 or level ** 3 != width:
        raise ValueError(f"HaldCLUT side {width} is not an exact cube (L^3)")
    size = level * level
    if len(pixels) != size ** 3 * 3:
        raise ValueError("HaldCLUT pixel count does not match L^2 grid")
    # Hald map requested by the project: i=y*side+x; R=i mod N; G=(i/N) mod N; B=i/N^2.
    # This yields an R-fastest 3D lattice.
    return Lut(size=size, values=pixels, red_fastest=True, title=path.stem)


def write_cube(lut: Lut, target: Path, output_size: int) -> None:
    target.parent.mkdir(parents=True, exist_ok=True)
    with target.open("w", encoding="utf-8", newline="\n") as handle:
        handle.write(f'TITLE "{lut.title.replace(chr(34), "")}"\n')
        handle.write(f"LUT_3D_SIZE {output_size}\n")
        handle.write("DOMAIN_MIN 0 0 0\nDOMAIN_MAX 1 1 1\n")
        # Standard .cube order has Blue as the fastest-varying axis.
        for red in range(output_size):
            r = red / (output_size - 1)
            for green in range(output_size):
                g = green / (output_size - 1)
                for blue in range(output_size):
                    b = blue / (output_size - 1)
                    values = lut.sample(r, g, b)
                    handle.write(f"{values[0]:.7f} {values[1]:.7f} {values[2]:.7f}\n")


def _png_chunk(kind: bytes, payload: bytes) -> bytes:
    return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", zlib.crc32(kind + payload) & 0xFFFFFFFF)


def write_rgb_png(path: Path, width: int, height: int, rgb: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    scanlines = b"".join(b"\x00" + rgb[y * width * 3:(y + 1) * width * 3] for y in range(height))
    ihdr = struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0)
    payload = PNG_SIGNATURE + _png_chunk(b"IHDR", ihdr) + _png_chunk(b"IDAT", zlib.compress(scanlines, 6)) + _png_chunk(b"IEND", b"")
    path.write_bytes(payload)


def create_thumbnail(lut: Lut, path: Path) -> None:
    width = height = 256
    output = bytearray(width * height * 3)
    for y in range(height):
        g = y / (height - 1)
        for x in range(width):
            r = x / (width - 1)
            b = 0.5
            values = lut.sample(r, g, b)
            offset = (y * width + x) * 3
            output[offset] = round(values[0] * 255)
            output[offset + 1] = round(values[1] * 255)
            output[offset + 2] = round(values[2] * 255)
    write_rgb_png(path, width, height, bytes(output))


def classify_license(text: str) -> str:
    value = text.lower()
    if "cc by-nc-sa" in value or "creative commons attribution-noncommercial-sharealike" in value:
        return "CC-BY-NC-SA"
    if "cc by-nc" in value or "attribution-noncommercial" in value:
        return "CC-BY-NC"
    if "cc by-sa" in value or "attribution-sharealike" in value:
        return "CC-BY-SA"
    if "cc by 4.0" in value or "creativecommons.org/licenses/by/4.0" in value or "attribution 4.0 international" in value:
        return "CC-BY-4.0"
    if "apache license" in value and "version 2.0" in value:
        return "Apache-2.0"
    if "mit license" in value or value.strip().startswith("mit\n"):
        return "MIT"
    if "gnu general public license" in value or "gpl-" in value:
        return "GPL"
    return "UNKNOWN"


def license_policy(license_id: str) -> tuple[bool, str]:
    if license_id in ("MIT", "Apache-2.0", "CC-BY-4.0"):
        return True, "Redistribute with the original notice and required attribution."
    if license_id == "CC-BY-SA":
        return True, "Redistribute only with attribution and the same ShareAlike license for the LUT file."
    if license_id in ("CC-BY-NC", "CC-BY-NC-SA"):
        return False, "Non-commercial license: do not include in a commercial app build."
    if license_id == "GPL":
        return False, "GPL source/data is excluded from the app bundle by project policy."
    return False, "Unknown license: manual review is required before redistribution."


def find_license(path: Path, input_root: Path) -> tuple[str, str | None, str]:
    for folder in (path.parent, *path.parent.parents):
        if folder != input_root and input_root not in folder.parents and folder != input_root.parent:
            break
        for name in LICENSE_NAMES:
            candidate = folder / name
            if candidate.is_file():
                text = candidate.read_text(encoding="utf-8", errors="replace")
                return classify_license(text), str(candidate.relative_to(input_root)), text
        if folder == input_root:
            break
    return "UNKNOWN", None, ""


def group_for_path(path: Path, root: Path) -> str:
    try:
        parts = path.relative_to(root).parts[:-1]
    except ValueError:
        parts = path.parts[:-1]
    for part in parts:
        key = re.sub(r"[\s]+", "_", part.strip().lower())
        if key in GROUP_NAMES:
            return GROUP_NAMES[key]
    return "Other"


def friendly_name(stem: str) -> str:
    return re.sub(r"\s+", " ", re.sub(r"[_-]+", " ", stem)).strip() or "Imported LUT"


def load_sidecar(path: Path) -> dict:
    sidecar = path.with_suffix(".json")
    if not sidecar.is_file():
        return {}
    try:
        value = json.loads(sidecar.read_text(encoding="utf-8"))
        return value if isinstance(value, dict) else {}
    except (OSError, json.JSONDecodeError):
        return {}


def process_one(
    source: Path,
    input_root: Path,
    output_root: Path,
    output_size: int,
) -> dict:
    suffix = source.suffix.lower()
    if suffix == ".cube":
        lut = parse_cube(source)
    elif suffix == ".vlt":
        lut = parse_vlt(source)
    elif suffix == ".png":
        lut = parse_hald_png(source)
    else:
        raise ValueError(f"unsupported extension: {suffix}")

    sidecar = load_sidecar(source)
    group = group_for_path(source, input_root)
    source_bytes = source.read_bytes()
    stable_id = hashlib.sha256(source_bytes).hexdigest()[:20]
    name = str(sidecar.get("name_en") or friendly_name(source.stem))
    name_vi = str(sidecar.get("name_vi") or name)
    converted_group = re.sub(r"[^A-Za-z0-9_-]+", "_", group).strip("_")
    relative_cube = Path(converted_group) / f"{stable_id}.cube"
    relative_thumb = Path(converted_group) / f"{stable_id}.png"
    cube_path = output_root / "cube" / relative_cube
    thumb_path = output_root / "thumbnails" / relative_thumb
    lut.title = name
    write_cube(lut, cube_path, output_size)
    create_thumbnail(lut, thumb_path)

    license_id, license_path, license_text = find_license(source, input_root)
    allowed, license_note = license_policy(str(sidecar.get("license") or license_id))
    license_id = str(sidecar.get("license") or license_id)
    if license_id == "UNKNOWN":
        allowed = False
        license_note = "Unknown license: manual review is required before redistribution."

    input_space = str(sidecar.get("input_space") or "sRGB (verify source)")
    output_space = str(sidecar.get("output_space") or "sRGB")
    log_wrapper = bool(sidecar.get("log_wrapper", False)) or group == "Cinema-Log"
    source_url = str(sidecar.get("source") or "")
    attribution = str(sidecar.get("attribution") or "")
    if not attribution and license_id == "CC-BY-4.0":
        attribution = str(sidecar.get("author") or "Attribution required; add author/site in sidecar.")
    if license_id == "CC-BY-SA":
        attribution = attribution or str(sidecar.get("author") or "Attribution required; add author/site in sidecar.")

    return {
        "id": stable_id,
        "name": {"en": name, "vi": name_vi},
        "group": group,
        "file": str(Path("cube") / relative_cube).replace("\\", "/"),
        "thumbnail": str(Path("thumbnails") / relative_thumb).replace("\\", "/"),
        "source_file": str(source.relative_to(input_root)).replace("\\", "/"),
        "source": source_url,
        "input_space": input_space,
        "output_space": output_space,
        "log_wrapper_required": log_wrapper,
        "grid_size": output_size,
        "license": license_id,
        "license_file": license_path,
        "attribution": attribution,
        "redistributable": allowed,
        "license_note": license_note,
    }


def main(argv: Iterable[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, default=Path("assets/luts"), help="User-provided LUT folder")
    parser.add_argument("--output", type=Path, default=Path("assets/luts/processed"), help="Converted output folder")
    parser.add_argument("--size", type=int, choices=(33, 65), default=65, help="Output .cube grid size")
    parser.add_argument("--zip", dest="zip_file", type=Path, default=None, help="Optional import ZIP path")
    parser.add_argument("--include-non-redistributable", action="store_true",
                        help="Include unknown/restricted files in ZIP (not recommended)")
    args = parser.parse_args(list(argv) if argv is not None else None)

    if not args.input.is_dir():
        print(f"[ERROR] Input folder not found: {args.input}", file=sys.stderr)
        return 2
    args.output.mkdir(parents=True, exist_ok=True)
    files = sorted(
        path for path in args.input.rglob("*")
        if path.is_file() and path.suffix.lower() in (".cube", ".vlt", ".png")
        and args.output not in path.parents
    )
    manifest: list[dict] = []
    errors: list[dict] = []
    seen: set[str] = set()
    for source in files:
        try:
            item = process_one(source, args.input, args.output, args.size)
            if item["id"] in seen:
                print(f"[SKIP] duplicate content: {source}")
                continue
            seen.add(item["id"])
            manifest.append(item)
            print(f"[OK] {source} -> {item['file']} [{item['group']}; {item['license']}]")
        except Exception as error:  # One corrupt LUT must not stop the offline batch.
            errors.append({"file": str(source), "error": str(error)})
            print(f"[SKIP] {source}: {error}", file=sys.stderr)

    report = {
        "schema_version": 1,
        "generated_by": "tools/lut_ingest.py",
        "output_grid_size": args.size,
        "count": len(manifest),
        "items": manifest,
        "errors": errors,
    }
    (args.output / "luts_manifest.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8"
    )

    credits = []
    for item in manifest:
        credits.append(
            f"- {item['name']['en']} ({item['group']}) — {item['license']}; "
            f"source: {item['source'] or item['source_file']}; "
            f"credit: {item['attribution'] or 'not supplied'}"
        )
    (args.output / "THIRD_PARTY_NOTICES.txt").write_text(
        "LUT source and license report. Verify each item before bundling.\n"
        + "\n".join(credits) + "\n",
        encoding="utf-8"
    )

    if args.zip_file:
        args.zip_file.parent.mkdir(parents=True, exist_ok=True)
        with zipfile.ZipFile(args.zip_file, "w", compression=zipfile.ZIP_DEFLATED) as archive:
            for item in manifest:
                if not item["redistributable"] and not args.include_non_redistributable:
                    continue
                cube_file = args.output / item["file"]
                archive.write(cube_file, f"{item['group']}/{item['name']['en']}.cube")
                if item["license_file"]:
                    license_source = args.input / item["license_file"]
                    if license_source.is_file():
                        archive.write(license_source, f"{item['group']}/LICENSE")
        print(f"[ZIP] {args.zip_file}")

    allowed = sum(1 for item in manifest if item["redistributable"])
    print(f"[DONE] imported={len(manifest)} redistributable={allowed} skipped={len(errors)}")
    return 0 if manifest or not files else 1


if __name__ == "__main__":
    raise SystemExit(main())
