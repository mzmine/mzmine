# DiffMS runtime (bundled Python)

This folder contains the **environment specifications** used to build the DiffMS Python runtimes.
The actual packed runtimes are stored under `external_tools/diffms/runtime-packs/`.

## Runtime design

MZmine ships **packed** runtimes (conda-pack archives) and extracts them into the user directory on
first use, then runs `conda-unpack` to relocate the environment.

See `external_tools/diffms/runtime-packs/README.md`.

## How to build the environment (developer notes)

We recommend building the runtime **per target OS/architecture** (macOS Intel/ARM, Windows x64,
Linux x64) and then bundling it into the corresponding MZmine build.

One pragmatic workflow using `conda-pack`:

- Create a conda environment with the required packages (PyTorch, RDKit, torch-geometric, etc.)
- Run the DiffMS runner once to confirm it works
- Pack the environment into an archive for transport (`conda-pack` supports `.zip` / `.tar.gz`)
- Pack the environment into an archive and place it into `external_tools/diffms/runtime-packs/`
  when building/distributing

To automate this, see:

- `external_tools/diffms/runtime/build_runtime_packs.sh` (macOS/Linux)
- `external_tools/diffms/runtime/build_runtime_packs.ps1` (Windows)

References:

- `conda-pack` documentation: `https://conda.github.io/conda-pack/`
- PyG installation docs (pip wheels + optional deps): `https://pytorch-geometric.readthedocs.io/en/latest/install/installation.html`

## Checkpoints

Model checkpoints are downloaded separately into the MZmine user directory under:

`~/.mzmine/external_resources/models/diffms/`

Use the download button in the DiffMS module "Checkpoint" parameter to fetch the Zenodo archive and
auto-extract `diffms_msg.ckpt`.

