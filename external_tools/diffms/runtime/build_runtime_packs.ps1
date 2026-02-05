#!/usr/bin/env pwsh

# Builds DiffMS runtime packs using micromamba + conda-pack (Windows).
#
# Output:
#   <OutputDirectory>\diffms-runtime-<variant>-windows-<arch>.zip
#
# Requirements:
# - micromamba on PATH or set via MICROMAMBA_EXE env var
#
# Usage:
#   .\build_runtime_packs.ps1 [[-OutputDirectory] <path>]

param(
    [string]$OutputDirectory
)

$ErrorActionPreference = "Stop"

$RootDir = Resolve-Path (Join-Path $PSScriptRoot "..\..\..") | Select-Object -ExpandProperty Path
$RuntimeDir = Join-Path $RootDir "external_tools\diffms\runtime"

if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $PacksDir = Join-Path $RootDir "external_tools\diffms\runtime-packs"
} else {
    $PacksDir = $OutputDirectory
}

$MambaExe = $env:MICROMAMBA_EXE
if ([string]::IsNullOrWhiteSpace($MambaExe)) {
    $MambaExe = "micromamba"
}

if (!(Get-Command $MambaExe -ErrorAction SilentlyContinue)) {
    Write-Error "$MambaExe not found. Please install micromamba or set MICROMAMBA_EXE."
}

$CpuYml = Join-Path $RuntimeDir "diffms-runtime-cpu.yml"
$CudaYml = Join-Path $RuntimeDir "diffms-runtime-cuda.yml"

New-Item -ItemType Directory -Force -Path $PacksDir | Out-Null

$archRaw = "$env:PROCESSOR_ARCHITECTURE".ToLowerInvariant()
if ($archRaw -eq "amd64" -or $archRaw -eq "x86_64") {
  $arch = "x86_64"
} elseif ($archRaw -like "*arm*") {
  $arch = "arm64"
} else {
  # GitHub Windows runners are AMD64; default to x86_64 if unknown.
  $arch = "x86_64"
}

$tempBase = $env:RUNNER_TEMP
if ([string]::IsNullOrWhiteSpace($tempBase)) {
    $tempBase = $env:TEMP
}
$tmpRoot = Join-Path $tempBase ("diffms_runtime_pack_" + [Guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Force -Path $tmpRoot | Out-Null

try {
  $packerPrefix = Join-Path $tmpRoot "packer"
  Write-Host "PROGRESS: 5"
  Write-Host "Building conda-pack tool env..."
  & $MambaExe create -y -p $packerPrefix -c conda-forge conda-pack
  if ($LASTEXITCODE -ne 0) { throw "Failed to create conda-pack environment" }
  Write-Host "PROGRESS: 15"

  function Build-Pack {
    param(
      [string]$variant,
      [string]$yml,
      [string]$out,
      [int]$baseProgress,
      [int]$stepProgress
    )
    $envPrefix = Join-Path $tmpRoot ("env_" + $variant)
    Write-Host "Creating env for $variant from $(Split-Path $yml -Leaf)..."
    if ($env:MAMBA_EXTRA_ARGS) {
      & $MambaExe create -y -p $envPrefix -f $yml $env:MAMBA_EXTRA_ARGS
    } else {
      & $MambaExe create -y -p $envPrefix -f $yml
    }
    if ($LASTEXITCODE -ne 0) { throw "Failed to create environment for $variant" }
    Write-Host ("PROGRESS: " + ($baseProgress + [int]($stepProgress * 0.6)))

    Write-Host "Installing torch-geometric stack via pip (variant=$variant)..."
    $torchVer = (& $MambaExe run -p $envPrefix python -c "import torch; print(torch.__version__)") -replace "`r",""
    if ($LASTEXITCODE -ne 0) { throw "Failed to get torch version" }
    if ($torchVer -match '^(\d+\.\d+\.\d+)') { $torchVer = $Matches[1] }
    if ($torchVer -match '^(\d+\.\d+)\.') { $torchTag = ($Matches[1] + ".0") } else { $torchTag = $torchVer }

    $cudaRaw = (& $MambaExe run -p $envPrefix python -c "import torch; print(torch.version.cuda or '')") -replace "`r",""
    if ($LASTEXITCODE -ne 0) { throw "Failed to get CUDA version" }
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
    Write-Host "Trying to install compiled PyG dependencies from $pygIndex..."
    & $MambaExe run -p $envPrefix python -m pip install --no-cache-dir --quiet `
      torch_scatter torch_sparse torch_cluster torch_spline_conv `
      -f $pygIndex
    if ($LASTEXITCODE -ne 0) {
      Write-Host "WARN: Could not install full PyG wheel set from $pygIndex. Installing torch_geometric only."
    }
    & $MambaExe run -p $envPrefix python -m pip install --no-cache-dir --quiet torch_geometric
    if ($LASTEXITCODE -ne 0) { throw "Failed to install torch_geometric" }
    & $MambaExe run -p $envPrefix python -m pip install --no-cache-dir --quiet tqdm-joblib
    if ($LASTEXITCODE -ne 0) { throw "Failed to install tqdm-joblib" }
    Write-Host ("PROGRESS: " + ($baseProgress + [int]($stepProgress * 0.9)))

    Write-Host "Packing env to $(Split-Path $out -Leaf)..."
    if (Test-Path $out) { Remove-Item -Force $out }
    & $MambaExe run -p $packerPrefix conda-pack -p $envPrefix -o $out --format zip
    if ($LASTEXITCODE -ne 0) { throw "Failed to pack environment for $variant" }
    Write-Host "Done: $out"
    Write-Host ("PROGRESS: " + ($baseProgress + $stepProgress))
  }

  $cpuOut = Join-Path $PacksDir ("diffms-runtime-cpu-windows-$arch.zip")
  Build-Pack "cpu" $CpuYml $cpuOut 15 40

  $cudaOut = Join-Path $PacksDir ("diffms-runtime-cuda-windows-$arch.zip")
  Build-Pack "cuda" $CudaYml $cudaOut 55 45

  Write-Host "Runtime packs ready in: $PacksDir"
  Write-Host "PROGRESS: 100"
} finally {
  if (Test-Path $tmpRoot) { Remove-Item -Recurse -Force $tmpRoot }
}
