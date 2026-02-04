# DiffMS runtime packs (conda-pack archives)

This folder is reserved for **packed** DiffMS Python runtimes, built per OS/architecture using
`conda-pack`, and shipped *inside* the MZmine distribution.

At runtime, MZmine will:

- select the best matching pack for the current platform (and requested CPU/CUDA)
- extract it into the user directory (`~/.mzmine/...`) which is writable
- run `conda-unpack` once to rewrite prefixes (relocation)
- run DiffMS using the extracted `python`

## Naming convention (recommended)

Use a predictable filename so MZmine can auto-detect the correct pack:

- `diffms-runtime-<variant>-<os>-<arch>.<ext>`

Where:

- `<variant>`: `cpu` or `cuda`
- `<os>`: `windows`, `linux`, `macos`
- `<arch>`: `x86_64` or `arm64`
- `<ext>`: `tar.gz` (macOS/Linux) or `zip` (Windows)

Examples:

- `diffms-runtime-cpu-macos-arm64.tar.gz`
- `diffms-runtime-cpu-windows-x86_64.zip`
- `diffms-runtime-cuda-linux-x86_64.tar.gz`

## How to build (developer notes)

1. Create a conda environment from one of the specs:

- `external_tools/diffms/runtime/diffms-runtime-cpu.yml`
- `external_tools/diffms/runtime/diffms-runtime-cuda.yml`

2. Pack it:

```bash
conda-pack -n <env> -o diffms-runtime-...tar.gz
```

3. Put the archive into this directory before running `./gradlew -p mzmine-community ...` so it is
copied into the final installer/app bundle.

Reference:

- `conda-pack` docs: `https://conda.github.io/conda-pack/`

