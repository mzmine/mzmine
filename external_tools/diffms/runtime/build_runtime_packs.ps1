#!/usr/bin/env pwsh
$ErrorActionPreference = "Stop"

# Builds DiffMS runtime packs using micromamba + conda-pack (Windows).
#
# Output:
#   external_tools\diffms\runtime-packs\diffms-runtime-<variant>-windows-<arch>.zip
#
# Requirements:
# - micromamba on PATH

$RootDir = Resolve-Path (Join-Path $PSScriptRoot "..\..\..") | Select-Object -ExpandProperty Path
$RuntimeDir = Join-Path $RootDir "external_tools\diffms\runtime"
$PacksDir = Join-Path $RootDir "external_tools\diffms\runtime-packs"

$CpuYml = Join-Path $RuntimeDir "diffms-runtime-cpu.yml"
$CudaYml = Join-Path $RuntimeDir "diffms-runtime-cuda.yml"

New-Item -ItemType Directory -Force -Path $PacksDir | Out-Null

$archRaw = ($env:PROCESSOR_ARCHITECTURE ?? "").ToLowerInvariant()
if ($archRaw -eq "amd64" -or $archRaw -eq "x86_64") {
  $arch = "x86_64"
} elseif ($archRaw -like "*arm*") {
  $arch = "arm64"
} else {
  # GitHub Windows runners are AMD64; default to x86_64 if unknown.
  $arch = "x86_64"
}

$tmpRoot = Join-Path $env:RUNNER_TEMP ("diffms_runtime_pack_" + [Guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Force -Path $tmpRoot | Out-Null

try {
  $packerPrefix = Join-Path $tmpRoot "packer"
  Write-Host "Building conda-pack tool env..."
  micromamba create -y -p $packerPrefix -c conda-forge conda-pack | Out-Null

  function Build-Pack($variant, $yml, $out) {
    $envPrefix = Join-Path $tmpRoot ("env_" + $variant)
    Write-Host "Creating env for $variant from $(Split-Path $yml -Leaf)..."
    if ($env:MAMBA_EXTRA_ARGS) {
      micromamba create -y -p $envPrefix -f $yml $env:MAMBA_EXTRA_ARGS
    } else {
      micromamba create -y -p $envPrefix -f $yml
    }

    Write-Host "Installing torch-geometric stack via pip (variant=$variant)..."
    $torchVer = (micromamba run -p $envPrefix python -c "import torch; print(torch.__version__)") -replace "`r",""
    if ($torchVer -match '^(\d+\.\d+\.\d+)') { $torchVer = $Matches[1] }
    if ($torchVer -match '^(\d+\.\d+)\.') { $torchTag = ($Matches[1] + ".0") } else { $torchTag = $torchVer }

    $cudaRaw = (micromamba run -p $envPrefix python -c "import torch; print(torch.version.cuda or '')") -replace "`r",""
    if ([string]::IsNullOrWhiteSpace($cudaRaw)) {
      $cudaTag = "cpu"
    } else {
      # 11.8 -> cu118, 12.1 -> cu121
      $cudaTag = "cu" + ($cudaRaw -replace "\.", "").Substring(0, 3)
    }
    if ($variant -eq "cuda" -and $cudaTag -eq "cpu") {
      Write-Host "WARN: CUDA variant requested but torch reports no CUDA; using cpu wheels."
    }
    $pygIndex = "https://data.pyg.org/whl/torch-$torchTag%2B$cudaTag.html"

    # Try compiled deps; if unavailable, continue with torch_geometric only.
    try {
      micromamba run -p $envPrefix python -m pip install --no-cache-dir `
        pyg_lib torch_scatter torch_sparse torch_cluster torch_spline_conv `
        -f $pygIndex
    } catch {
      Write-Host "WARN: Could not install full PyG wheel set from $pygIndex. Installing torch_geometric only."
    }
    micromamba run -p $envPrefix python -m pip install --no-cache-dir torch_geometric
    micromamba run -p $envPrefix python -m pip install --no-cache-dir tqdm-joblib

    Write-Host "Packing env to $(Split-Path $out -Leaf)..."
    if (Test-Path $out) { Remove-Item -Force $out }
    micromamba run -p $packerPrefix conda-pack -p $envPrefix -o $out --format zip
    Write-Host "Done: $out"
  }

  $cpuOut = Join-Path $PacksDir ("diffms-runtime-cpu-windows-$arch.zip")
  Build-Pack "cpu" $CpuYml $cpuOut

  $cudaOut = Join-Path $PacksDir ("diffms-runtime-cuda-windows-$arch.zip")
  Build-Pack "cuda" $CudaYml $cudaOut

  Write-Host "Runtime packs ready in: $PacksDir"
} finally {
  if (Test-Path $tmpRoot) { Remove-Item -Recurse -Force $tmpRoot }
}

