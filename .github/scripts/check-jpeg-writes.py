#!/usr/bin/env python3
"""Guard final JPEG writes: ImageSink owns final output; all other writes need a reasoned marker."""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[2]
SOURCE_ROOT = ROOT / "CamUI-src" / "app" / "src" / "main"
PATTERN = re.compile(
    r"\.compress\s*\(|"
    r"openOutputStream\s*\(|"
    r"FileOutputStream\s*\(|"
    r"OutputFileOptions\.Builder\s*\(|"
    r"jpeg_stdio_dest\s*\(|"
    r'\bfopen\s*\([^;\n]*,\s*"wb"|'
    r"\b(?:writeJpeg|encodeJpeg)\s*\(",
)
ALLOW_MARKER = "JPEG_INTERMEDIATE_ALLOWLIST:"

violations = []
checked = 0

for source in SOURCE_ROOT.rglob("*"):
    if source.suffix not in {".kt", ".java", ".cpp", ".c", ".h"}:
        continue
    if source.name == "ImageSink.kt":
        continue
    # Settings backup is application/json, not a JPEG/image output path.
    if source.name == "BackupSettings.kt":
        continue
    try:
        lines = source.read_text(encoding="utf-8").splitlines()
    except (OSError, UnicodeError) as error:
        print(f"Không đọc được mã nguồn để kiểm tra JPEG: {source}: {error}", file=sys.stderr)
        sys.exit(2)
    for index, line in enumerate(lines):
        if not PATTERN.search(line):
            continue
        checked += 1
        previous = lines[index - 1] if index > 0 else ""
        if ALLOW_MARKER not in previous:
            relative = source.relative_to(ROOT)
            violations.append(f"{relative}:{index + 1}: {line.strip()}")

if violations:
    print("LỖI: Phát hiện lệnh ghi/encode JPEG ngoài ImageSink.kt mà không có lý do whitelist ngay phía trên.")
    for violation in violations:
        print("  " + violation)
    print("Hãy chuyển lần ghi ảnh cuối sang ImageSink hoặc thêm JPEG_INTERMEDIATE_ALLOWLIST kèm lý do cho dữ liệu trung gian.")
    sys.exit(1)

print(f"Kiểm tra JPEG đạt: {checked} điểm ghi trung gian có whitelist; ImageSink.kt là điểm ghi cuối duy nhất.")
