# DiffMS Python Runtime Build Scripts

This folder contains the **environment specifications and build scripts** used to build DiffMS Python runtimes on-demand.

## Runtime Design

MZmine builds DiffMS Python runtimes **on-demand** using these scripts. The built runtime packs are stored in `~/.mzmine/diffms/runtime-packs/` and extracted into `~/.mzmine/external_resources/models/diffms/runtimes/` on first use, then `conda-unpack` is run to relocate the environment.

## How to Build (Developer Notes)

The build scripts automate the creation of conda-pack archives for DiffMS runtimes:

1. **Create conda environment** from the YAML specifications:
   - `diffms-runtime-cpu.yml` - CPU-only runtime
   - `diffms-runtime-cuda.yml` - CUDA-enabled runtime (skipped on macOS)

2. **Run the build script**:
   - `build_runtime_packs.sh` (macOS/Linux)
   - `build_runtime_packs.ps1` (Windows)

3. **Output**: Runtime packs are created in the specified output directory (default: `~/.mzmine/diffms/runtime-packs/`)

The build scripts:
- Use micromamba to create conda environments
- Install PyTorch and torch-geometric with appropriate CUDA support
- Pack the environment using conda-pack
- Name files according to: `diffms-runtime-<variant>-<os>-<arch>.tar.gz`

## Usage in MZmine

Users can build runtimes using the **DiffMS Build Runtime** module in MZmine, which:
- Downloads micromamba if needed
- Runs the appropriate build script for the current platform
- Stores packs in `~/.mzmine/diffms/runtime-packs/`

## Directory Structure

```
external_tools/python-runtimes/diffms/
├── diffms-runtime-cpu.yml       # Conda environment spec for CPU
├── diffms-runtime-cuda.yml      # Conda environment spec for CUDA
├── build_runtime_packs.sh       # Build script (Unix)
├── build_runtime_packs.ps1      # Build script (Windows)
└── README.md                    # This file
```

## References

- `conda-pack` documentation: `https://conda.github.io/conda-pack/`
- PyG installation docs: `https://pytorch-geometric.readthedocs.io/en/latest/install/installation.html`
- Micromamba: `https://mamba.readthedocs.io/en/latest/user_guide/micromamba.html`

## Checkpoints

Model checkpoints are downloaded separately into the MZmine user directory under:

`~/.mzmine/external_resources/models/diffms/checkpoints/`

Use the download button in the DiffMS module "Checkpoint" parameter to fetch the Zenodo archive and auto-extract `diffms_msg.ckpt`.
