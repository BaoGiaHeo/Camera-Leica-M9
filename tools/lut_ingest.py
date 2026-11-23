#!/usr/bin/env python3
"""Offline LUT ingester for FujiFilm Mode.

Reads assets/luts/<group> and converts .cube, Panasonic .vlt and HaldCLUT PNG
files into normalized 3D .cube files (33^3 or 65^3), thumbnails and a manifest.
It never downloads source assets and never assumes that a repository licence
also covers every LUT/image contained in that repository.

Optional sidecar metadata:
  <file>.meta.json  (preferred) or <file-without-extension>.meta.json

Example:
  python tools/lut_ingest.py --input assets/luts --output build/lut-output --size 33
"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
import re
import struct
import sys
import tempfile
import zlib
from pathlib import Path
from typing import Iterable

GROUPS = {
    "fujifilm": "fujifilm",
    "fuji": "fujifilm",
    "leica": "leica",
    "panasonic": "panasonic-lumix",
    "lumix": "panasonic-lumix",
    "panasonic-lumix": "panasonic-lumix",
    "sony": "sony",
    "cinema": "cinema-log",
    "log": "cinema-log",
    "cinema-log": "cinema-log",
    "hald": "hald-clut",
    "hald-clut": "hald-clut",
    "other": "other",
}
ACCEPTED_LICENSES = {
    "MIT", "Apache-2.0", "CC-BY-4.0", "CC-BY-SA-4.0",
    "BSD-2-Clause", "BSD-3-Clause", "0BSD", "OFL-1.1",
}
REVIEW_LICENSES = {"", "unknown", "proprietary", "all-rights-reserved", "unlicensed"}
RESTRICTED_LICENSES = {"GPL-3.0", "GPL-3.0-only", "GPL-3.0-or-later", "AGPL-3.0"}


def fail(message: str) -> None:
    raise ValueError(message)


def stable_id(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()[:24]


def group_for(path: Path, root: Path) -> str:
    try:
        relative = path.relative_to(root)
        parts = relative.parts[:-1]
    except ValueError:
        parts = ()
    for part in parts:
        key = part.lower().replace("_", "-").replace(" ", "-")
        if key in GROUPS:
            return GROUPS[key]
    return "other"


def read_sidecar(source: Path) -> dict:
    candidates = [
        source.with_name(source.name + ".meta.json"),
        source.with_suffix(".meta.json"),
    ]
    for candidate in candidates:
        if candidate.is_file():
            data = json.loads(candidate.read_text(encoding="utf-8"))
            if not isinstance(data, dict):
                fail(f"Sidecar must contain a JSON object: {candidate}")
            return data
    return {}


def license_decision(meta: dict) -> tuple[str, bool, str]:
    licence = str(meta.get("license", "unknown")).strip()
    normalized = licence.lower().replace("_", "-")
    canonical = next(
        (known for known in ACCEPTED_LICENSES | RESTRICTED_LICENSES
         if known.lower() == normalized),
        licence or "unknown",
    )
    if canonical in RESTRICTED_LICENSES:
        return canonical, False, "restricted-license"
    if canonical in ACCEPTED_LICENSES:
        needs_credit = canonical.startswith("CC-BY")
        if needs_credit and not str(meta.get("attribution", "")).strip():
            return canonical, False, "attribution-required"
        return canonical, True, "approved-with-attribution" if needs_credit else "approved"
    if normalized in REVIEW_LICENSES or canonical == "unknown":
        return "unknown", False, "license-review-required"
    return canonical, False, "license-not-allowlisted"


def parse_cube(path: Path) -> tuple[int, list[tuple[float, float, float]]]:
    size = None
    lut_1d = None
    rows: list[tuple[float, float, float]] = []
    for raw in path.read_text(encoding="utf-8-sig", errors="strict").splitlines():
        line = raw.split("#", 1)[0].strip()
        if not line:
            continue
        fields = line.split()
        key = fields[0].upper()
        if key == "TITLE":
            continue
        if key == "DOMAIN_MIN" or key == "DOMAIN_MAX":
            if len(fields) != 4:
                fail(f"{key} must contain three values")
            continue
        if key == "LUT_1D_SIZE":
            lut_1d = int(fields[1])
            continue
        if key == "LUT_3D_SIZE":
            size = int(fields[1])
            continue
        if key in {"BEGIN", "END"}:
            continue
        if len(fields) != 3:
            fail(f"Unsupported .cube row: {raw[:100]}")
        try:
            rows.append(tuple(float(v) for v in fields))
        except ValueError:
            fail(f"Invalid .cube numeric row: {raw[:100]}")
    if lut_1d:
        fail("Combined LUT_1D_SIZE + LUT_3D_SIZE cubes are not yet supported; export a baked 3D cube")
    if size is None or not 2 <= size <= 129:
        fail("Missing or unsupported LUT_3D_SIZE (expected 2..129)")
    if len(rows) != size ** 3:
        fail(f".cube declares {size}^3={size**3} entries but contains {len(rows)}")
    if not all(math.isfinite(v) for row in rows for v in row):
        fail(".cube contains NaN or infinite values")
    return size, rows


def parse_vlt(path: Path) -> tuple[int, list[tuple[float, float, float]]]:
    """Read Panasonic VARICAM VLT 1.0 ASCII tables (integer values 0..4095)."""
    text = path.read_text(encoding="ascii", errors="strict")
    size = None
    rows: list[tuple[float, float, float]] = []
    for raw in text.splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        fields = line.split()
        if fields[0].upper() == "LUT_3D_SIZE":
            if len(fields) != 2:
                fail("Invalid VLT LUT_3D_SIZE header")
            size = int(fields[1])
            continue
        if len(fields) != 3:
            fail(f"Invalid VLT data row: {raw[:100]}")
        try:
            values = tuple(float(v) for v in fields)
        except ValueError:
            fail(f"Invalid VLT numeric row: {raw[:100]}")
        if not all(math.isfinite(v) for v in values):
            fail("VLT contains NaN or infinite values")
        rows.append(values)
    if size is None or not 2 <= size <= 65:
        fail("VLT missing LUT_3D_SIZE or has unsupported grid (2..65)")
    if len(rows) != size ** 3:
        fail(f"VLT declares {size}^3={size**3} entries but contains {len(rows)}")
    maximum = max(max(row) for row in rows)
    minimum = min(min(row) for row in rows)
    if minimum < 0:
        fail("VLT output contains negative values")
    scale = 4095.0 if maximum > 1.0 else 1.0
    if maximum > scale:
        fail(f"VLT output value {maximum} exceeds expected 0..4095 range")
    return size, [tuple(v / scale for v in row) for row in rows]


def paeth(a: int, b: int, c: int) -> int:
    p = a + b - c
    pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
    return a if pa <= pb and pa <= pc else b if pb <= pc else c


def decode_hald_png(path: Path) -> tuple[int, int, bytes, int, int]:
    """Decode non-interlaced RGB/RGBA 8- or 16-bit HaldCLUT PNG using stdlib."""
    raw = path.read_bytes()
    if raw[:8] != b"\x89PNG\r\n\x1a\n":
        fail("Not a PNG file")
    offset = 8
    width = height = bit_depth = colour_type = None
    compressed = bytearray()
    while offset + 12 <= len(raw):
        length = struct.unpack_from(">I", raw, offset)[0]
        kind = raw[offset + 4:offset + 8]
        data_start = offset + 8
        data_end = data_start + length
        if data_end + 4 > len(raw):
            fail("Truncated PNG chunk")
        chunk = raw[data_start:data_end]
        if kind == b"IHDR":
            width, height, bit_depth, colour_type, compression, filt, interlace = struct.unpack(">IIBBBBB", chunk)
            if compression != 0 or filt != 0 or interlace != 0:
                fail("Hald PNG must use standard compression/filter and no interlacing")
        elif kind == b"IDAT":
            compressed.extend(chunk)
        elif kind == b"IEND":
            break
        offset = data_end + 4
    if width is None or height is None:
        fail("PNG is missing IHDR")
    if width != height:
        fail("HaldCLUT PNG must be square")
    if colour_type not in (2, 6) or bit_depth not in (8, 16):
        fail("HaldCLUT PNG must be RGB/RGBA with 8-bit or 16-bit channels")
    channels = 3 if colour_type == 2 else 4
    bytes_per_sample = bit_depth // 8
    bpp = channels * bytes_per_sample
    stride = width * bpp
    decoded = zlib.decompress(bytes(compressed))
    if len(decoded) != height * (stride + 1):
        fail("Unexpected PNG decompressed size")
    pixels = bytearray(height * stride)
    previous = bytearray(stride)
    source_offset = 0
    for y in range(height):
        filter_type = decoded[source_offset]
        source_offset += 1
        scan = bytearray(decoded[source_offset:source_offset + stride])
        source_offset += stride
        if filter_type == 1:
            for i in range(stride):
                scan[i] = (scan[i] + (scan[i - bpp] if i >= bpp else 0)) & 255
        elif filter_type == 2:
            for i in range(stride):
                scan[i] = (scan[i] + previous[i]) & 255
        elif filter_type == 3:
            for i in range(stride):
                left = scan[i - bpp] if i >= bpp else 0
                scan[i] = (scan[i] + ((left + previous[i]) // 2)) & 255
        elif filter_type == 4:
            for i in range(stride):
                left = scan[i - bpp] if i >= bpp else 0
                up = previous[i]
                upper_left = previous[i - bpp] if i >= bpp else 0
                scan[i] = (scan[i] + paeth(left, up, upper_left)) & 255
        elif filter_type != 0:
            fail(f"Unsupported PNG filter {filter_type}")
        start = y * stride
        pixels[start:start + stride] = scan
        previous = scan
    return width, height, bytes(pixels), bit_depth, channels


def sample_hald(
    pixels: bytes, edge: int, bit_depth: int, channels: int, n: int,
    r: float, g: float, b: float
) -> tuple[float, float, float]:
    maximum = 255.0 if bit_depth == 8 else 65535.0
    bytes_per_sample = bit_depth // 8

    def point(ri: int, gi: int, bi: int) -> tuple[float, float, float]:
        index = ri + gi * n + bi * n * n
        offset = index * channels * bytes_per_sample
        if bit_depth == 8:
            return tuple(pixels[offset + c] / maximum for c in range(3))
        return tuple(struct.unpack_from(">H", pixels, offset + c * 2)[0] / maximum for c in range(3))

    return trilinear(n, point, r, g, b)


def trilinear(
    size: int, getter, r: float, g: float, b: float
) -> tuple[float, float, float]:
    r = min(max(r, 0.0), size - 1.0)
    g = min(max(g, 0.0), size - 1.0)
    b = min(max(b, 0.0), size - 1.0)
    r0, g0, b0 = int(r), int(g), int(b)
    r1, g1, b1 = min(r0 + 1, size - 1), min(g0 + 1, size - 1), min(b0 + 1, size - 1)
    fr, fg, fb = r - r0, g - g0, b - b0
    corners = [
        getter(r0, g0, b0), getter(r1, g0, b0),
        getter(r0, g1, b0), getter(r1, g1, b0),
        getter(r0, g0, b1), getter(r1, g0, b1),
        getter(r0, g1, b1), getter(r1, g1, b1),
    ]
    result = []
    for channel in range(3):
        c00 = corners[0][channel] * (1-fr) + corners[1][channel] * fr
        c10 = corners[2][channel] * (1-fr) + corners[3][channel] * fr
        c01 = corners[4][channel] * (1-fr) + corners[5][channel] * fr
        c11 = corners[6][channel] * (1-fr) + corners[7][channel] * fr
        c0 = c00 * (1-fg) + c10 * fg
        c1 = c01 * (1-fg) + c11 * fg
        result.append(c0 * (1-fb) + c1 * fb)
    return tuple(result)


def resample_table(
    source_size: int, table: list[tuple[float, float, float]], target_size: int
) -> list[tuple[float, float, float]]:
    def get(r: int, g: int, b: int):
        return table[r + g * source_size + b * source_size * source_size]
    output = []
    for b in range(target_size):
        for g in range(target_size):
            for r in range(target_size):
                source = [
                    r * (source_size - 1) / (target_size - 1),
                    g * (source_size - 1) / (target_size - 1),
                    b * (source_size - 1) / (target_size - 1),
                ]
                output.append(trilinear(source_size, get, *source))
    return output


def parse_source(path: Path, target_size: int) -> tuple[list[tuple[float, float, float]], int, str]:
    extension = path.suffix.lower()
    if extension == ".cube":
        size, table = parse_cube(path)
        return resample_table(size, table, target_size), size, "cube"
    if extension == ".vlt":
        size, table = parse_vlt(path)
        return resample_table(size, table, target_size), size, "vlt"
    if extension == ".png":
        edge, height, pixels, depth, channels = decode_hald_png(path)
        root = round(edge ** (1.0 / 3.0))
        if root ** 3 != edge:
            fail(f"Hald edge {edge} is not a perfect cube")
        source_size = root * root
        if edge * height != source_size ** 3:
            fail("Hald dimensions do not match L^3 by L^3")
        output = []
        for b in range(target_size):
            for g in range(target_size):
                for r in range(target_size):
                    source_r = r * (source_size - 1) / (target_size - 1)
                    source_g = g * (source_size - 1) / (target_size - 1)
                    source_b = b * (source_size - 1) / (target_size - 1)
                    output.append(sample_hald(
                        pixels, edge, depth, channels, source_size,
                        source_r, source_g, source_b
                    ))
        return output, source_size, "hald-png"
    fail(f"Unsupported extension: {extension}")


def png_chunk(kind: bytes, data: bytes) -> bytes:
    body = kind + data
    return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)


def write_png_rgb(path: Path, width: int, height: int, pixels: bytes) -> None:
    header = struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0)
    scanlines = b"".join(b"\x00" + pixels[y * width * 3:(y + 1) * width * 3] for y in range(height))
    png = (
        b"\x89PNG\r\n\x1a\n"
        + png_chunk(b"IHDR", header)
        + png_chunk(b"IDAT", zlib.compress(scanlines, 6))
        + png_chunk(b"IEND", b"")
    )
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


def make_thumbnail(table: list[tuple[float, float, float]], size: int, path: Path) -> None:
    def get(r: int, g: int, b: int):
        return table[r + g * size + b * size * size]
    pixels = bytearray(256 * 256 * 3)
    for y in range(256):
        for x in range(256):
            r = x / 255.0
            g = 1.0 - y / 255.0
            b = (x + (255 - y)) / 510.0
            colour = trilinear(size, get, r * (size - 1), g * (size - 1), b * (size - 1))
            offset = (y * 256 + x) * 3
            pixels[offset:offset + 3] = bytes(
                int(min(max(v, 0.0), 1.0) * 255.0 + 0.5) for v in colour
            )
    write_png_rgb(path, 256, 256, bytes(pixels))


def write_cube(path: Path, title: str, size: int, table: list[tuple[float, float, float]]) -> None:
    safe_title = re.sub(r"[^A-Za-z0-9 _.-]", "", title).strip() or "Imported LUT"
    lines = [f'TITLE "{safe_title}"', f"LUT_3D_SIZE {size}",
             "DOMAIN_MIN 0.0 0.0 0.0", "DOMAIN_MAX 1.0 1.0 1.0"]
    lines.extend(f"{r:.8f} {g:.8f} {b:.8f}" for r, g, b in table)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def self_test() -> None:
    # Identity table verifies R-fastest ordering and interpolation endpoints.
    size = 3
    table = []
    for b in range(size):
        for g in range(size):
            for r in range(size):
                table.append((r/(size-1), g/(size-1), b/(size-1)))
    output = resample_table(size, table, 5)
    assert len(output) == 125
    assert output[0] == (0.0, 0.0, 0.0)
    assert output[-1] == (1.0, 1.0, 1.0)
    middle = output[2 + 2*5 + 2*25]
    assert all(abs(v - 0.5) < 1e-9 for v in middle)

    # Exercise the three parsers with generated identity fixtures.
    with tempfile.TemporaryDirectory(prefix="fuji-lut-test-") as directory:
        root = Path(directory)
        cube = root / "identity.cube"
        cube_rows = [
            f"{r/(2-1):.6f} {g/(2-1):.6f} {b/(2-1):.6f}"
            for b in range(2) for g in range(2) for r in range(2)
        ]
        cube.write_text("LUT_3D_SIZE 2\n" + "\n".join(cube_rows) + "\n", encoding="utf-8")
        cube_size, cube_table = parse_cube(cube)
        assert cube_size == 2 and len(cube_table) == 8
        assert cube_table[1] == (1.0, 0.0, 0.0)

        vlt = root / "identity.vlt"
        vlt_rows = [
            f"{int(r*4095)} {int(g*4095)} {int(b*4095)}"
            for b in range(2) for g in range(2) for r in range(2)
        ]
        vlt.write_text(
            "\n".join([
                "# panasonic vlt file version 1.0",
                '# source vlt file ""',
                "LUT_3D_SIZE 2",
                *vlt_rows,
            ]) + "\n",
            encoding="ascii",
        )
        vlt_size, vlt_table = parse_vlt(vlt)
        assert vlt_size == 2 and len(vlt_table) == 8
        assert all(abs(v - 1.0) < 1e-9 for v in vlt_table[-1])

        # L=2 gives an 8x8 Hald image and N=L^2=4 samples per channel.
        hald_pixels = bytearray(8 * 8 * 3)
        for blue in range(4):
            for green in range(4):
                for red in range(4):
                    index = red + green * 4 + blue * 16
                    offset = index * 3
                    hald_pixels[offset:offset+3] = bytes((
                        round(red * 255 / 3),
                        round(green * 255 / 3),
                        round(blue * 255 / 3),
                    ))
        hald = root / "identity-hald.png"
        write_png_rgb(hald, 8, 8, bytes(hald_pixels))
        converted, hald_size, kind = parse_source(hald, 3)
        assert kind == "hald-png" and hald_size == 4 and len(converted) == 27
        assert converted[0] == (0.0, 0.0, 0.0)
        assert all(abs(v - 1.0) < 1e-6 for v in converted[-1])

    print("SELF_TEST_OK: CUBE, VLT, Hald PNG, R-fastest ordering and trilinear resampling")


def ingest(input_root: Path, output_root: Path, target_size: int) -> dict:
    manifest = {
        "schema_version": 1,
        "target_cube_size": target_size,
        "interpolation": "trilinear-offline-resampling",
        "items": [],
        "skipped": [],
    }
    supported = {".cube", ".vlt", ".png"}
    excluded_dirs = {"processed", "converted", "thumbnails", "build", ".git"}
    if not input_root.exists():
        fail(f"Input directory does not exist: {input_root}")
    sources = sorted(
        p for p in input_root.rglob("*")
        if p.is_file() and p.suffix.lower() in supported
        and not any(part.lower() in excluded_dirs for part in p.relative_to(input_root).parts[:-1])
    )
    for source in sources:
        item = {"source_file": str(source), "group": group_for(source, input_root)}
        try:
            original = source.read_bytes()
            meta = read_sidecar(source)
            licence, redistributable, licence_status = license_decision(meta)
            item.update({
                "id": stable_id(original),
                "name_en": str(meta.get("name_en") or meta.get("name") or source.stem),
                "name_vi": str(meta.get("name_vi") or meta.get("name_en") or meta.get("name") or source.stem),
                "category": str(meta.get("category") or item["group"]),
                "source": str(meta.get("source") or ""),
                "license": licence,
                "attribution": str(meta.get("attribution") or ""),
                "input_space": str(meta.get("input_space") or ("sRGB" if source.suffix.lower() == ".png" else "unspecified")),
                "input_gamut": str(meta.get("input_gamut") or ("sRGB" if source.suffix.lower() == ".png" else "unspecified")),
                "redistribution_allowed": redistributable,
                "license_status": licence_status,
            })
            if licence in RESTRICTED_LICENSES:
                item["status"] = "skipped"
                item["reason"] = "Restricted repository/file licence; not converted for app packaging"
                manifest["skipped"].append(item)
                continue
            table, source_size, source_format = parse_source(source, target_size)
            item["source_size"] = source_size
            item["source_format"] = source_format
            item["output_size"] = target_size
            item["status"] = "ready" if redistributable else "local_only_review"
            item["bundle_candidate"] = bool(redistributable)
            relative_group = item["group"]
            cube_rel = Path("cubes") / relative_group / (item["id"] + ".cube")
            thumb_rel = Path("thumbnails") / (item["id"] + ".png")
            write_cube(output_root / cube_rel, item["name_en"], target_size, table)
            make_thumbnail(table, target_size, output_root / thumb_rel)
            item["cube_file"] = cube_rel.as_posix()
            item["thumbnail_file"] = thumb_rel.as_posix()
            item["source_sha256"] = hashlib.sha256(original).hexdigest()
            item["sidecar_present"] = bool(meta)
            manifest["items"].append(item)
        except Exception as exc:
            item["status"] = "skipped"
            item["reason"] = f"{type(exc).__name__}: {exc}"
            manifest["skipped"].append(item)
    manifest_path = output_root / "luts_manifest.json"
    manifest_path.parent.mkdir(parents=True, exist_ok=True)
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return manifest


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, default=Path("assets/luts"))
    parser.add_argument("--output", type=Path, default=Path("build/lut-output"))
    parser.add_argument("--size", type=int, choices=(33, 65), default=33)
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    if args.self_test:
        self_test()
        return 0
    result = ingest(args.input, args.output, args.size)
    print(
        f"LUT_INGEST: ready={len(result['items'])} "
        f"skipped={len(result['skipped'])} target={args.size}^3 "
        f"manifest={args.output / 'luts_manifest.json'}"
    )
    for item in result["skipped"]:
        print(f"SKIP: {item['source_file']}: {item.get('reason', 'unknown')}", file=sys.stderr)
    return 0 if result["items"] else 2


if __name__ == "__main__":
    raise SystemExit(main())
