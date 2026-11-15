#!/usr/bin/env python3
"""Export the NAFNet SIDD denoisers to fixed-size ONNX tiles for Android.

The exported networks are denoisers (same-resolution residual restoration), not GAN upscalers.
HDMP owns the final blend, validation and save. Real-ESRGAN is intentionally not exported.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import sys
from pathlib import Path


def patch_known_nafnetlib_typo(source_root: Path) -> None:
    model_file = source_root / "nafnetlib" / "models" / "nafnet.py"
    text = model_file.read_text(encoding="utf-8")
    corrected = text.replace("torch.torch.nn.Module", "torch.nn.Module")
    if corrected != text:
        model_file.write_text(corrected, encoding="utf-8")


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def export_one(model_id: str, filename: str, model_dir: Path, output_dir: Path) -> dict:
    import numpy as np
    import onnx
    import onnxruntime as ort
    import torch
    from nafnetlib.core import DenoiseProcessor

    torch.set_num_threads(max(1, min(8, os.cpu_count() or 2)))
    torch.set_grad_enabled(False)

    processor = DenoiseProcessor(
        model_id=model_id,
        model_dir=str(model_dir),
        device="cpu",
    )
    network = processor.net.net_g.eval()
    torch.manual_seed(20261115)
    sample = torch.rand(1, 3, 256, 256, dtype=torch.float32)
    output_path = output_dir / filename

    with torch.no_grad():
        reference = network(sample).detach().cpu().numpy()
        torch.onnx.export(
            network,
            sample,
            str(output_path),
            input_names=["image_bgr"],
            output_names=["restored_bgr"],
            opset_version=17,
            do_constant_folding=False,
            dynamo=False,
        )

    graph = onnx.load(str(output_path), load_external_data=True)
    onnx.checker.check_model(graph)

    session = ort.InferenceSession(
        str(output_path),
        providers=["CPUExecutionProvider"],
    )
    predicted = session.run(
        ["restored_bgr"],
        {"image_bgr": sample.cpu().numpy()},
    )[0]
    if predicted.shape != (1, 3, 256, 256):
        raise RuntimeError(f"{model_id}: unexpected ONNX output shape {predicted.shape}")
    if not np.isfinite(predicted).all():
        raise RuntimeError(f"{model_id}: ONNX smoke test returned NaN/Infinity")
    if not np.isfinite(reference).all():
        raise RuntimeError(f"{model_id}: PyTorch reference returned NaN/Infinity")

    max_abs_error = float(np.max(np.abs(predicted - reference)))
    mean_abs_error = float(np.mean(np.abs(predicted - reference)))
    # ONNX Runtime and PyTorch CPU kernels accumulate FP32 convolution sums in
    # slightly different orders. Keep a tight image-domain tolerance while
    # rejecting conversion errors that affect a material fraction of pixels.
    parity_ok = (
        np.allclose(predicted, reference, rtol=1e-2, atol=3e-3)
        and mean_abs_error <= 5e-4
        and max_abs_error <= 3e-3
    )
    if not parity_ok:
        raise RuntimeError(
            f"{model_id}: ONNX/PyTorch numerical parity failed "
            f"(max_abs={max_abs_error:.6g}, mean_abs={mean_abs_error:.6g}; "
            "limits: max_abs=0.003, mean_abs=0.0005)"
        )
    if output_path.stat().st_size < 1_000_000:
        raise RuntimeError(f"{model_id}: ONNX model file is unexpectedly small")

    return {
        "model_id": model_id,
        "file": filename,
        "bytes": output_path.stat().st_size,
        "sha256": sha256(output_path),
        "smoke_test": "passed",
        "numerical_parity": "passed",
        "max_abs_error": max_abs_error,
        "mean_abs_error": mean_abs_error,
        "input": [1, 3, 256, 256],
        "output": list(predicted.shape),
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--nafnetlib-src", required=True, type=Path)
    parser.add_argument("--model-dir", required=True, type=Path)
    parser.add_argument("--output-dir", required=True, type=Path)
    args = parser.parse_args()

    source_root = args.nafnetlib_src.resolve()
    if not (source_root / "nafnetlib" / "core.py").is_file():
        raise FileNotFoundError(f"nafnetlib source not found under {source_root}")
    args.model_dir.mkdir(parents=True, exist_ok=True)
    args.output_dir.mkdir(parents=True, exist_ok=True)

    patch_known_nafnetlib_typo(source_root)
    sys.path.insert(0, str(source_root))

    models = [
        ("sidd_width64", "nafnet_sidd_width64.onnx"),
        ("sidd_width32", "nafnetlib_sidd_width32.onnx"),
    ]
    manifest = []
    report = []
    for model_id, filename in models:
        result = export_one(model_id, filename, args.model_dir, args.output_dir)
        manifest.append(f"{result['sha256']}  {filename}")
        report.append(result)
        print(json.dumps(result, sort_keys=True), flush=True)

    (args.output_dir / "manifest.sha256").write_text(
        "\n".join(manifest) + "\n",
        encoding="utf-8",
    )
    (args.output_dir / "export-report.json").write_text(
        json.dumps(report, indent=2) + "\n",
        encoding="utf-8",
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
