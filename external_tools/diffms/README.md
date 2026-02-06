# DiffMS Integration

This directory contains the DiffMS integration components for MZmine.

## Directory Structure

```
external_tools/diffms/
├── vendor/
│   └── DiffMS/                      # Vendored DiffMS source code
├── src/
│   └── mzmine_diffms_predict.py     # MZmine Python runner for DiffMS
└── README.md                        # This file
```

## Components

### Vendor Directory

The `vendor/DiffMS/` directory contains a pinned version of the DiffMS source code, vendored into the MZmine distribution to keep user setup minimal. The vendored commit hash is recorded in `vendor/DIFFMS_PINNED_COMMIT.txt`.

### Source Directory

The `src/` directory contains the MZmine-specific Python runner (`mzmine_diffms_predict.py`) that:
- Loads the vendored DiffMS code
- Processes MZmine feature list data
- Runs DiffMS inference
- Returns SMILES structures for annotation

## Runtime Building

Python runtime environments are built on-demand using scripts located in:
`external_tools/python-runtimes/diffms/`

See that directory's README for details on building runtime packs.

## User Directory Structure

At runtime, DiffMS uses the following user directories:

- **Runtime packs**: `~/.mzmine/diffms/runtime-packs/` - Built runtime packs (conda-pack archives)
- **Extracted runtimes**: `~/.mzmine/external_resources/models/diffms/runtimes/` - Extracted and unpacked runtime environments
- **Checkpoints**: `~/.mzmine/external_resources/models/diffms/checkpoints/` - Model checkpoint files

## Usage

DiffMS is accessed through the MZmine module system. Users need to:
1. Build a runtime pack (using the DiffMS Build Runtime module)
2. Download a checkpoint (using the checkpoint parameter download button)
3. Run DiffMS structure generation on their feature lists
