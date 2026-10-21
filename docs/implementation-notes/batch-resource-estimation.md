# Batch resource estimation

## Intention

Let users refine resource estimates by adding measurements, even when the current estimate fits.

## Decisions

- Keep implementation types and helpers package-private. The module and parameter-set classes
  retain public no-argument constructors because MZmine instantiates and clones them through
  reflection from other packages.
- The public module CLI entry point runs synchronously and returns a `Task` for the normal startup
  exit-status handling. `MZmineCoreArgumentParser` in the companion `mzio_mzmine/mzmine-core`
  project
  supplies `--estimate-resources [smallest-subset [subsets [statistics.tsv]]]` with `--batch`.
  Omitted trailing values default to 4 files, 3 subsets, and no TSV export. The separate
  `--estimate-smallest-subset`, `--estimate-subsets`, and `--estimate-output` options remain
  supported;
  specifying the same setting in both forms is an error. Existing CLI input and database
  overrides apply before subset selection. Thread, memory and temporary-directory options retain
  their normal behavior; batch export steps are skipped.
- The parser stores one nullable `EstimateResourcesSettings` record: null means estimation was not
  requested. `BatchResourceEstimationParameters.fromSettings` maps the record and batch file to the
  module's parameters, including the optional statistics export.
- Community resolves the parser library from `local-repo`; refresh the companion project's local
  `mzmine-core` publication when changing its CLI API. A successful estimation returns success
  even if the predicted batch would exceed available resources; task failures return an error.
- Additional runs are started beside the target file count and thread count, independently of the
  verdict. The banner reports the result without duplicating the run controls.
- Default to twice the largest completed run, capped at the actual batch file count. Recalculate
  this default after a successful additional run, not when the extrapolation target changes.
- Additional runs must exceed the largest measured subset and cannot exceed the batch size. This
  preserves the ascending run order required by the model's final-segment extrapolation. Once all
  files have been measured, the run controls are disabled.
- Disable the run controls while processing. Additional measurements update the existing result
  tab and preserve its selected extrapolation target. Errors and cancellation re-enable the
  controls.
- Optional TSV export runs in both GUI and headless execution after the initial estimation and
  every successful additional run. Replace the selected file with the complete report, including
  prior runs, validation measurements and predictions, and the latest full-batch prediction.
  The editable GUI extrapolation target does not change the export target. Export errors are
  reported as task errors while the completed estimation remains available in the GUI.
