# Automatic parameter optimizer

## Intention

Find a strong wizard parameter set with a practical number of full batch executions. The search
must be reproducible, expose time to result, work in GUI and headless runs, and preserve the
meaning of the selected quality metric.

Detailed measurements and rejected algorithm pilots are recorded in
[autoparam-optimizer-evaluation.md](autoparam-optimizer-evaluation.md).

## Decisions

- Retain deterministic pattern search for single-objective optimization and MOEA/D for genuine
  multi-objective searches. MOEA/D remains the default until the smaller pattern-search campaign
  is accepted as sufficient product validation.
- GP-ARD, multivariate TPE, and standalone Sobol search were removed after matched pilot runs did
  not outperform pattern search. Sobol remains shared infrastructure for MOEA/D warm starts and
  pattern-search restarts. The pilot CSVs remain under
  [the optimizer test resources](../../mzmine-community/src/test/resources/io/github/mzmine/modules/tools/tools_autoparam/optimizer/results/).
- The requested iteration limit counts uncached full batch executions, including the raw-data
  estimate. Cache hits remain visible but consume no batch budget. A separate proposal cap prevents
  duplicate loops.
- Elapsed optimization time is the primary efficiency measure. It starts after import/statistics
  and includes the estimate, proposal generation, diagnostics, and batch execution. Per-batch
  runtime and batch/proposal indices remain available as diagnostics.
- Pattern search starts directly at the raw-data estimate. MOEA/D uses a fixed population of 20;
  its warm-start sampling setting controls how that population is distributed around the estimate.
  All random behavior uses the seeded MOEA Framework PRNG. Pattern search and Sobol designs are
  deterministic.
- Pattern-search step sizes adapt independently per parameter. If neither direction improves the
  incumbent, only that parameter's step is halved; an improving direction retains its current step.
  There is no full-sweep contraction. A restart is attempted only after a complete sweep without
  improvement once every parameter has reached its minimum step.
- The optimizer selector is a `ModuleOptionsEnum`: pattern search embeds one optimization target,
  while MOEA/D embeds its multi-target checklist and optional raw-data initialization. This keeps
  multiple objectives and warm-start sampling out of algorithms that cannot use them.
- Candidates are materialized through `problem.newSolution()`, represented canonically on
  `[0, 1]`, tagged with their origin before evaluation, and deduplicated by their effective values.
  Every registered parameter explicitly declares a linear or logarithmic `SearchScale`; scale is
  based on the parameter's meaning rather than inferred from its current bounds. Continuous
  tolerances, widths, intensities, heights, and SNR are multiplicative; thresholds, ratios, counts,
  and enum choices are linear. Ordinal values are rounded before comparison.
- The optional peak-shape rejection remains a constraint rather than part of the score. Its limit
  is derived from the estimate with a floor for nearly perfect baselines. The expensive shape
  diagnostic runs only when the constraint is enabled; the cheap precision diagnostic always runs.
- GUI runs publish completed solutions to one observable table and a score/best-so-far chart. Stop
  search finishes the current batch, prevents the next proposal, and returns all completed results.
  On completion the table sorts by isotope-ratio consistency when available, otherwise by the first
  objective. The selected, bold front row is the best objective for a single-objective run or the
  lowest mean rank across all objectives for a multi-objective front; objective direction is taken
  from the MOEA objective definition.
  Headless runs create no JavaFX state and expose the same result through `OptimizationOutcome`.
- The statistics dashboard shows the cross-file absolute RT-deviation distribution together with
  the inter-sample RT estimate and search bounds. Because it is inherently a cross-file statistic,
  this plot is unchanged by the dashboard's single-file/overlay display mode. Optimizer GUI runs
  reuse the deviations already calculated for the search range.
- Dashboard estimate markers name the wizard parameter they feed. FWHM and minimum-height estimates
  coincide with their distribution medians; minimum consecutive scans is half the median isotope
  trace width rounded to the integer applied by the wizard. The edge-intensity histogram marks the
  seventh-percentile MS1 noise estimate only for the absolute-intensity detector; no marker is shown
  for factor-of-lowest-signal data because edge intensities and detector factors have different
  units. The categorical m/z plot marks the estimated preset, one predefined tolerance step above
  the most frequently sufficient tolerance. The dashboard and single-pass optimizer call the same
  estimator functions.
- The wizard's **Estimate** action (next to the parameter tabs) uses the optimizer's
  representative-file rule: up to
  ten filename-identified QCs when at least three are available, otherwise up to ten non-blanks
  when at least four are available, and otherwise up to ten of all selected files. It analyses that
  subset on a background task, applies only estimates backed by those statistics to the active
  wizard presets, and adds batch-only estimates as customization overrides. The statistics
  dashboard only opens via the context menu entry "Estimate parameters and show statistics". It does
  not change mobility FWHM; with only one file it also leaves the sample-to-sample
  RT tolerance unchanged.
- General wizard preset factories do not depend on optimizer classes. Optimizable parameters are
  registered centrally in `OptimizationParameterRegistry`; optional Wavelet parameters are excluded
  from new configurations explicitly, not by list position.
- Scan RT correction is a Boolean search choice for LC workflows (including Wavelet and GC-CI).
  Its initial estimate uses absolute per-feature RT differences from each aligned benchmark row's
  median, then a median per file. Enable correction when any file median exceeds both three times
  the median across files and half the median peak FWHM. These are initial heuristic thresholds,
  not calibrated performance guarantees. Require rows detected in at least 80% of files (rounded
  up), at least three detections per row, and at least five eligible observations per file.
  Fewer than three eligible files or missing peak widths retains the preset fallback without
  overwriting the user's switch in estimate-only mode; optimization still explores both values.
  The existing inter-sample RT tolerance statistic remains unchanged. Estimation reuses its
  benchmark alignment, so shifts outside that alignment's RT window may remain undetected.
- `WizardOptimizationProblem` owns the search space, cache, constraints, and evaluation history.
  `OptimizationBatchEvaluator` owns reduced batch construction/execution, scoring, and diagnostics.
  `BenchmarkFeatureLoader` owns optional CSV and statistics-derived target loading.
- Real-data benchmark tests are opt-in with `-Dmzmine.test.autoparam.run=true`. Data paths are
  relative to `mzmine.test.autoparam.dataRoot`, and generated output defaults to
  `build/autoparam-benchmarks` so normal tests and source roots remain clean.

## Validation contract

Compare best feasible score as a function of elapsed time, with the raw estimate and equal full
batch ceilings. Retain complete trajectories, cache information, constraints, and precision/shape
diagnostics. Prefer the simpler method when final scores differ by less than 5%; do not promote a
new default from a single dataset or a single stochastic MOEA/D seed.
