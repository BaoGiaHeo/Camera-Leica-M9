import tempfile
import unittest
from pathlib import Path

from lut_ingest import (
    Lut,
    _png_chunk,
    decode_png_rgb,
    parse_cube,
    parse_hald_png,
    parse_vlt,
    write_cube,
    write_rgb_png,
)


def identity_values(size: int, red_fastest: bool) -> list[tuple[float, float, float]]:
    rows = []
    if red_fastest:
        for blue in range(size):
            for green in range(size):
                for red in range(size):
                    rows.append((red / (size - 1), green / (size - 1), blue / (size - 1)))
    else:
        for red in range(size):
            for green in range(size):
                for blue in range(size):
                    rows.append((red / (size - 1), green / (size - 1), blue / (size - 1)))
    return rows


def write_hald16(path: Path, level: int = 2) -> None:
    side = level ** 3
    lattice = level ** 2
    rows = bytearray()
    for y in range(side):
        rows.append(0)  # PNG filter None
        for x in range(side):
            index = y * side + x
            red = index % lattice
            green = (index // lattice) % lattice
            blue = index // (lattice * lattice)
            for value in (red, green, blue):
                sample = round(value / (lattice - 1) * 65535)
                rows.extend(sample.to_bytes(2, "big"))
    ihdr = __import__("struct").pack(">IIBBBBB", side, side, 16, 2, 0, 0, 0)
    payload = b"\x89PNG\r\n\x1a\n"
    payload += _png_chunk(b"IHDR", ihdr)
    payload += _png_chunk(b"IDAT", __import__("zlib").compress(bytes(rows)))
    payload += _png_chunk(b"IEND", b"")
    path.write_bytes(payload)


class LutIngestTests(unittest.TestCase):
    def test_cube_blue_fastest_axis_is_parsed(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "identity.cube"
            rows = identity_values(2, red_fastest=False)
            lines = ["TITLE \"identity\"", "LUT_3D_SIZE 2", "DOMAIN_MIN 0 0 0", "DOMAIN_MAX 1 1 1"]
            lines += ["%.6f %.6f %.6f" % row for row in rows]
            path.write_text("\n".join(lines) + "\n", encoding="utf-8")
            result = parse_cube(path).sample(0.25, 0.5, 0.75)
            self.assertAlmostEqual(0.25, result[0], places=5)
            self.assertAlmostEqual(0.5, result[1], places=5)
            self.assertAlmostEqual(0.75, result[2], places=5)

    def test_combined_one_and_three_dimensional_cube_is_applied_in_order(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "combined.cube"
            rows = [
                'TITLE "combined identity"',
                "LUT_1D_SIZE 2",
                "LUT_1D_INPUT_RANGE 0 1",
                "0.1 0.2 0.3",
                "0.9 0.8 0.7",
                "LUT_3D_SIZE 2",
            ]
            rows += [
                "%.6f %.6f %.6f" % row
                for row in identity_values(2, red_fastest=False)
            ]
            path.write_text("\n".join(rows) + "\n", encoding="utf-8")
            result = parse_cube(path).sample(0.5, 0.25, 0.75)
            self.assertAlmostEqual(0.5, result[0], places=5)
            self.assertAlmostEqual(0.35, result[1], places=5)
            self.assertAlmostEqual(0.6, result[2], places=5)

    def test_panasonic_vlt_r_fastest_grid_is_parsed(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "identity.vlt"
            rows = identity_values(2, red_fastest=True)
            header = [
                "# panasonic vlt file version 1.0",
                '# source vlt file ""',
                "LUT_3D_SIZE 2",
            ]
            data = [
                f"{round(r * 4095)} {round(g * 4095)} {round(b * 4095)}"
                for r, g, b in rows
            ]
            path.write_text("\n".join(header + data) + "\n", encoding="ascii")
            result = parse_vlt(path).sample(0.25, 0.5, 0.75)
            self.assertAlmostEqual(0.25, result[0], places=3)
            self.assertAlmostEqual(0.5, result[1], places=3)
            self.assertAlmostEqual(0.75, result[2], places=3)

    def test_hald_png_8_bit_maps_r_fastest(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "hald8.png"
            side = 8
            lattice = 4
            pixels = bytearray()
            for y in range(side):
                for x in range(side):
                    index = y * side + x
                    red = index % lattice
                    green = (index // lattice) % lattice
                    blue = index // (lattice * lattice)
                    pixels.extend((
                        round(red / (lattice - 1) * 255),
                        round(green / (lattice - 1) * 255),
                        round(blue / (lattice - 1) * 255),
                    ))
            write_rgb_png(path, side, side, bytes(pixels))
            result = parse_hald_png(path).sample(0.25, 0.5, 0.75)
            self.assertAlmostEqual(0.25, result[0], places=2)
            self.assertAlmostEqual(0.5, result[1], places=2)
            self.assertAlmostEqual(0.75, result[2], places=2)

    def test_hald_png_16_bit_keeps_high_bit_depth(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "hald16.png"
            write_hald16(path)
            width, height, bit_depth, _ = decode_png_rgb(path)
            self.assertEqual((8, 8, 16), (width, height, bit_depth))
            result = parse_hald_png(path).sample(0.25, 0.5, 0.75)
            self.assertAlmostEqual(0.25, result[0], places=3)
            self.assertAlmostEqual(0.5, result[1], places=3)
            self.assertAlmostEqual(0.75, result[2], places=3)

    def test_hald_requires_perfect_cube_edge(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "invalid.png"
            write_rgb_png(path, 7, 7, bytes([0] * (7 * 7 * 3)))
            with self.assertRaisesRegex(ValueError, "not an exact cube"):
                parse_hald_png(path)

    def test_resampled_cube_is_valid_and_preserves_identity(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            source = Lut(
                size=2,
                values=__import__("array").array(
                    "f",
                    [value for row in identity_values(2, red_fastest=True) for value in row],
                ),
                red_fastest=True,
                title="identity",
            )
            output = Path(directory) / "identity65.cube"
            write_cube(source, output, 33)
            parsed = parse_cube(output)
            self.assertEqual(33, parsed.size)
            result = parsed.sample(0.1, 0.4, 0.9)
            self.assertAlmostEqual(0.1, result[0], places=5)
            self.assertAlmostEqual(0.4, result[1], places=5)
            self.assertAlmostEqual(0.9, result[2], places=5)


if __name__ == "__main__":
    unittest.main()
