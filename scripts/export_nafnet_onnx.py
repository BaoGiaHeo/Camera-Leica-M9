#!/usr/bin/env python3
"""Export Megvii's official NAFNet width64 and nafnetlib width32 to ONNX.

Both networks perform same-resolution denoising. HDMP owns validation, conservative
blending, image commit and final save; this script never exports an image upscaler.
"""
from __future__ import annotations

import argparse
import hashlib
import importlib.util
import json
import logging
import os
import sys
import types
from pathlib import Path


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _package(name: str, directory: Path) -> None:
    module = types.ModuleType(name)
    module.__path__ = [str(directory)]
    module.__package__ = name
    sys.modules[name] = module


def _load_module(name: str, path: Path):
    spec = importlib.util.spec_from_file_location(name, path)
    if spec is None or spec.loader is None:
        raise ImportError(f"Cannot load official NAFNet module: {path}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[name] = module
    spec.loader.exec_module(module)
    return module


def load_official_megvii_width64(source_root: Path, checkpoint_path: Path):
    """Instantiate the exact official Megvii SIDD width64 architecture and weights."""
    import torch

    basicsr = source_root / "basicsr"
    arch_dir = basicsr / "models" / "archs"
    for name, directory in (
        ("basicsr", basicsr),
        ("basicsr.models", basicsr / "models"),
        ("basicsr.models.archs", arch_dir),
    ):
        _package(name, directory)

    # arch_util only needs get_root_logger for optional logging paths. Stub that
    # utility import so the official architecture can be loaded without installing
    # the entire BasicSR training stack and its unrelated data dependencies.
    utility_stub = types.ModuleType("basicsr.utils")
    utility_stub.get_root_logger = lambda *args, **kwargs: logging.getLogger("nafnet")
    utility_stub.__path__ = []
    sys.modules["basicsr.utils"] = utility_stub

    _load_module(
        "basicsr.models.archs.arch_util",
        arch_dir / "arch_util.py",
    )
    _load_module(
        "basicsr.models.archs.local_arch",
        arch_dir / "local_arch.py",
    )
    official_arch = _load_module(
        "basicsr.models.archs.NAFNet_arch",
        arch_dir / "NAFNet_arch.py",
    )

    network = official_arch.NAFNet(
        img_channel=3,
        width=64,
        enc_blk_nums=[2, 2, 4, 8],
        middle_blk_num=12,
        dec_blk_nums=[2, 2, 2, 2],
    )
    checkpoint = torch.load(
        checkpoint_path,
        map_location="cpu",
        weights_only=False,
    )
    state = checkpoint.get("params") if isinstance(checkpoint, dict) else None
    if state is None and isinstance(checkpoint, dict):
        state = checkpoint.get("params_ema")
    if state is None:
        raise RuntimeError("Official Megvii checkpoint has no 'params' or 'params_ema' state dict")
    network.load_state_dict(state, strict=True)
    network.eval().cpu()
    return network


def load_nafnetlib_width32(source_root: Path, model_dir: Path):
    """Load the requested width32 fallback from mikecokina/nafnetlib."""
    source_root = source_root.resolve()
    model_file = source_root / "nafnetlib" / "models" / "nafnet.py"
    if not model_file.is_file():
        raise FileNotFoundError(f"nafnetlib source not found under {source_root}")
    text = model_file.read_text(encoding="utf-8")
    corrected = text.replace("torch.torch.nn.Module", "torch.nn.Module")
    if corrected != text:
        model_file.write_text(corrected, encoding="utf-8")
    sys.path.insert(0, str(source_root))
    from nafnetlib.core import DenoiseProcessor

    processor = DenoiseProcessor(
        model_id="sidd_width32",
        model_dir=str(model_dir),
        device="cpu",
    )
    return processor.net.net_g.eval().cpu()


def export_one(model_id: str, filename: str, network, output_dir: Path, channel_order: str) -> dict:
    import numpy as np
    import onnx
    import onnxruntime as ort
    import torch

    if channel_order not in {"RGB", "BGR"}:
        raise ValueError(f"Unsupported NAFNet input channel order: {channel_order}")
    torch.set_num_threads(max(1, min(8, os.cpu_count() or 2)))
    torch.set_grad_enabled(False)
    network = network.eval().cpu()
    torch.manual_seed(20261115)
    sample = torch.rand(1, 3, 256, 256, dtype=torch.float32)
    output_path = output_dir / filename

    with torch.no_grad():
        reference = network(sample).detach().cpu().numpy()
        torch.onnx.export(
            network,
            sample,
            str(output_path),
            input_names=["input_image"],
            output_names=["restored_image"],
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
        ["restored_image"],
        {"input_image": sample.cpu().numpy()},
    )[0]
    if predicted.shape != (1, 3, 256, 256):
        raise RuntimeError(f"{model_id}: unexpected ONNX output shape {predicted.shape}")
    if not np.isfinite(predicted).all() or not np.isfinite(reference).all():
        raise RuntimeError(f"{model_id}: PyTorch/ONNX produced NaN or Infinity")

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
        "channel_order": channel_order,
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
    parser.add_argument("--official-nafnet-src", required=True, type=Path)
    parser.add_argument("--official-width64-checkpoint", required=True, type=Path)
    parser.add_argument("--nafnetlib-src", required=True, type=Path)
    parser.add_argument("--model-dir", required=True, type=Path)
    parser.add_argument("--output-dir", required=True, type=Path)
    args = parser.parse_args()

    if not (args.official_nafnet_src / "basicsr/models/archs/NAFNet_arch.py").is_file():
        raise FileNotFoundError("Official Megvii NAFNet source was not found")
    if not args.official_width64_checkpoint.is_file() or args.official_width64_checkpoint.stat().st_size < 1_000_000:
        raise FileNotFoundError("Official Megvii SIDD width64 checkpoint is missing or too small")
    if not (args.nafnetlib_src / "src/nafnetlib/core.py").is_file() and not (args.nafnetlib_src / "nafnetlib/core.py").is_file():
        raise FileNotFoundError(f"nafnetlib source not found under {args.nafnetlib_src}")

    args.model_dir.mkdir(parents=True, exist_ok=True)
    args.output_dir.mkdir(parents=True, exist_ok=True)

    official_network = load_official_megvii_width64(
        args.official_nafnet_src.resolve(),
        args.official_width64_checkpoint.resolve(),
    )
    fallback_source = args.nafnetlib_src.resolve()
    if (fallback_source / "src/nafnetlib/core.py").is_file():
        fallback_source = fallback_source / "src"
    fallback_network = load_nafnetlib_width32(fallback_source, args.model_dir)

    models = [
        (
            "megvii_nafnet_sidd_width64",
            "nafnet_sidd_width64.onnx",
            official_network,
            "RGB",
        ),
        (
            "nafnetlib_sidd_width32",
            "nafnetlib_sidd_width32.onnx",
            fallback_network,
            "RGB",
        ),
    ]
    manifest = []
    report = []
    for model_id, filename, network, channel_order in models:
        result = export_one(model_id, filename, network, args.output_dir, channel_order)
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
