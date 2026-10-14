# Native parameter-task integration

Source: `steffen/auto-param-2-pattern-search`,
`786aaddd97ea707851a1c5a4b880f6bf90571078`.

The automatic-parameter package is now ported from the source branch, including
`WizardParameterEstimationTask`, `BatchOptimizationMainTask`, optimizer configuration
and outcome/GUI classes. The two custom classes `LocalParameterAnalysisTask` and
`CandidateEvaluator` were removed. The extra `BatchTask.forIsolatedProject` API and
its test were removed; the earlier milestone's fixed-project processing API remains.

Integration changes to upstream are limited to result/progress accessors, exposing
the existing front-ranking helper, and using this base branch's existing
`snapshotSequence()` hook in place of `getSequence()`. Scientific preparation,
candidate generation, optimization metrics and algorithms use upstream behavior.

The source wizard UI chooses up to ten inputs, but its task constructors do not
impose that limit. The bridge calls those tasks with the explicit local selection,
including the whole dataset as requested. The Python adapter receives no raw data.

The user approved merging Steffen's branch, including the LC-wavelet preset.
A three-way merge of HEAD and the source branch was prepared with `git merge-tree`;
204 changed paths were applied without an index update or commit. Conflicts in the
wizard tab, HPLC parameters and ion-interface factory retain the existing MCP hooks
and scan RT correction while adding the native upstream feature. Upstream docs,
tests and benchmark result fixtures are included; benchmark campaigns have not been run.
See company `docs/local-parameter-analysis-native-validation.md` for current evidence;
previous custom-adapter test results do not validate this correction.

## RT correction update, 2026-09-17

Fetched `steffen/auto-param-2-pattern-search` at
`1762ae2e9e8c33a91a27e090462815b944e089c8` (previous fetched tip
`786aaddd97ea707851a1c5a4b880f6bf90571078`). Ported the 11 paths in
`1762ae2e9e` through a content merge, preserving the local bridge hooks and existing
uncommitted changes. The intermediate master merge contains unrelated changes and was
not imported wholesale; the native automatic-parameter package had no changes in that
merge. Existing scan RT correction parameters and builders provide the dependency.

The upstream implementation now estimates and optimizes the Boolean scan RT correction
switch. Its statistics, evidence requirements, search domain, fallback and tests are
unchanged. The optimizer also receives its actual input files before building each
evaluation queue. Both wizard actions and the company bridge now call
`RawDataPreparation.selectOptimizerInputFiles`; the old wizard helper was removed.
The local whole-dataset selection remains available. The helper's local explanatory
comment and final argument modifier do not change its native selection algorithm.

Independent community assemble and 76 focused tests passed, including the four upstream
RT correction regressions, native selector, parameter registry, preparation, optimizer
execution/search/GUI, dashboard and override tests. See company
`docs/local-parameter-analysis-rt-update.md` for complete commands and bridge validation.
The Git index and commits were not changed.

## Bounded estimation evidence, 2026-09-17

`ParameterEstimationEvidence` provides a typed, bounded aggregate explanation for a
`PreparedParameter` and `ParameterEstimationContext`. It contains the estimate basis,
an explanation, observation count/unit, finite aggregate measurements, and limitations;
it deliberately excludes raw-file identities, spectra, traces, metadata, and arrays.

`ParameterEstimators.rtCorrectionThreshold` is shared by the unchanged RT estimator and
the evidence record. The RT decision remains a strict comparison of the largest eligible
file deviation against `max(3 × median file deviation, 0.5 × median peak width)`. Only
aligned rows detected in `max(3, ceil(80% of analyzed files))` contribute, and a file
needs at least five shared observations. The evidence reports those criteria, aggregate
RT values, and that shared-row count, correction feasibility, and correction benefit are
not assessed there. It does not turn insufficient evidence into a negative finding.

Evidence for the other native rules identifies measured-derived estimates, fixed
heuristics, preset-derived values, and the native m/z fallback. m/z evidence counts the
within-file isotope-tolerance observations used by `DataFileStatistics.extractToleranceCounts`,
not the separate inter-sample alignment statistic. MS2 noise is explicitly reported as
derived from MS1 by the existing 2.5 divisor rather than independently measured.

`WizardOptimizationProblem` captures immutable evidence once for its fixed prepared
baseline, keyed by parameter-definition ID, along with the baseline file count. Candidate
evaluations do not re-measure data and therefore do not replace that baseline evidence.

No file selection, estimation threshold, RT correction, or optimization behavior changed.

Focused verification (11 tests):

```zsh
./gradlew :mzmine-community:test \
  --tests io.github.mzmine.modules.tools.tools_autoparam.estimation.RtCorrectionEstimationTest \
  --tests io.github.mzmine.modules.tools.tools_autoparam.optimizer.execution.WizardOptimizationProblemParameterTest
```

Independent community-module packaging verification:

```zsh
./gradlew :mzmine-community:assemble
```
