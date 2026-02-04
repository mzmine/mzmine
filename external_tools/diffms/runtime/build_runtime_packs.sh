#!/usr/bin/env bash
set -euo pipefail

# Builds DiffMS runtime packs using micromamba + conda-pack.
#
# Output:
#   external_tools/diffms/runtime-packs/diffms-runtime-<variant>-<os>-<arch>.<ext>
#
# Requirements:
# - micromamba on PATH
#
# Notes:
# - CUDA pack is skipped on macOS (no CUDA).
# - The produced archives are intended to be shipped inside the MZmine distribution.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
RUNTIME_DIR="$ROOT_DIR/external_tools/diffms/runtime"
PACKS_DIR="$ROOT_DIR/external_tools/diffms/runtime-packs"

CPU_YML="$RUNTIME_DIR/diffms-runtime-cpu.yml"
CUDA_YML="$RUNTIME_DIR/diffms-runtime-cuda.yml"

mkdir -p "$PACKS_DIR"

uname_s="$(uname -s | tr '[:upper:]' '[:lower:]')"
case "$uname_s" in
  darwin) os="macos" ;;
  linux) os="linux" ;;
  *) echo "Unsupported OS for this script: $uname_s" >&2; exit 2 ;;
esac

uname_m="$(uname -m | tr '[:upper:]' '[:lower:]')"
case "$uname_m" in
  x86_64|amd64) arch="x86_64" ;;
  arm64|aarch64) arch="arm64" ;;
  *) echo "Unsupported arch for this script: $uname_m" >&2; exit 2 ;;
esac

ext="tar.gz"

tmp_root="$(mktemp -d)"
cleanup() { rm -rf "$tmp_root"; }
trap cleanup EXIT

packer_prefix="$tmp_root/packer"
echo "Building conda-pack tool env..."
micromamba create -y -p "$packer_prefix" -c conda-forge conda-pack >/dev/null

build_pack() {
  local variant="$1" yml="$2" out="$3"
  local env_prefix="$tmp_root/env_$variant"

  echo "Creating env for $variant from $(basename "$yml")..."
  micromamba create -y -p "$env_prefix" -f "$yml" ${MAMBA_EXTRA_ARGS:-}

  echo "Installing torch-geometric stack via pip (variant=$variant)..."
  local torch_ver
  torch_ver="$(micromamba run -p "$env_prefix" python -c "import torch; print(torch.__version__)" | tr -d '\r' | head -n 1)"
  # Keep only major.minor.patch
  torch_ver="$(echo "$torch_ver" | sed -E 's/^([0-9]+\\.[0-9]+\\.[0-9]+).*/\\1/')"

  # PyG wheel index uses a canonical patch version per minor series (e.g., 2.3.* -> 2.3.0).
  # See PyG installation docs.
  local torch_mm
  torch_mm="$(echo "$torch_ver" | sed -E 's/^([0-9]+\\.[0-9]+)\\..*/\\1/')"
  local torch_tag="${torch_mm}.0"

  # Detect CUDA tag from the installed torch build.
  local cuda_raw
  cuda_raw="$(micromamba run -p "$env_prefix" python -c "import torch; print(torch.version.cuda or '')" | tr -d '\r' | head -n 1)"
  local cuda_tag="cpu"
  if [[ -n "$cuda_raw" ]]; then
    # 11.8 -> cu118, 12.1 -> cu121
    cuda_tag="cu$(echo "$cuda_raw" | tr -d '.' | cut -c1-3)"
  fi
  # In case the env is CPU-only but the user asked for CUDA variant, fall back to cpu wheels.
  if [[ "$variant" == "cuda" && "$cuda_tag" == "cpu" ]]; then
    echo "WARN: CUDA variant requested but torch reports no CUDA; using cpu wheels."
  fi

  local pyg_index="https://data.pyg.org/whl/torch-${torch_tag}%2B${cuda_tag}.html"

  # Install optional compiled deps first; if not available for this platform, fall back to
  # installing torch_geometric only (PyG docs: optional dependencies are not required for basic usage).
  # https://pytorch-geometric.readthedocs.io/en/latest/install/installation.html
  if ! micromamba run -p "$env_prefix" python -m pip install --no-cache-dir \
      pyg_lib torch_scatter torch_sparse torch_cluster torch_spline_conv \
      -f "$pyg_index"; then
    echo "WARN: Could not install full PyG wheel set from $pyg_index. Installing torch_geometric only."
  fi
  micromamba run -p "$env_prefix" python -m pip install --no-cache-dir torch_geometric

  # pip-only dependency used by vendored DiffMS in fp2mol dataset preprocessing (import name: tqdm_joblib)
  micromamba run -p "$env_prefix" python -m pip install --no-cache-dir tqdm-joblib

  echo "Packing env to $(basename "$out")..."
  rm -f "$out"
  micromamba run -p "$packer_prefix" conda-pack -p "$env_prefix" -o "$out" --format tar.gz

  echo "Done: $out"
}

cpu_out="$PACKS_DIR/diffms-runtime-cpu-${os}-${arch}.${ext}"
build_pack "cpu" "$CPU_YML" "$cpu_out"

if [[ "$os" != "macos" ]]; then
  cuda_out="$PACKS_DIR/diffms-runtime-cuda-${os}-${arch}.${ext}"
  build_pack "cuda" "$CUDA_YML" "$cuda_out"
else
  echo "Skipping CUDA runtime pack on macOS."
fi

echo "Runtime packs ready in: $PACKS_DIR"

