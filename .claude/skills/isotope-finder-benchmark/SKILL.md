---
name: Isotope finder benchmark
description: How to run and interpret the isotope-finder accuracy benchmark before/after changing the engine or related scoring/model code. Use when editing IsotopeFinderEngine, ChargeScore, the EnvelopeModel implementations (CarbonAveragineEnvelopeModel / formula mode), ElementAutoDetector, or anything under the filter_isotopefinder engine/signal packages that affects charge-state, pattern, or element detection.
---

# Isotope finder benchmark

A deterministic benchmark measures the isotope finder's charge-state, isotope-pattern, and
heavy-element detection accuracy over a committed synthetic corpus. **Any change to the detection
engine or its scoring/model code must be validated with it — do not ship a scoring change that isn't
measured before/after.** That is the guiding principle of this whole line of work.

Everything lives in the test source set, package
`io.github.mzmine.modules.dataprocessing.filter_isotopefinder.benchmark`, plus the engine tests in
the
parent package. Committed data:

- Corpus: `mzmine-community/src/test/resources/isotopefinder/corpus/patterns.jsonl` (~15 MB, 5919
  patterns). Regenerate deliberately — **never** as part of an engine change.
- Baseline: `mzmine-community/src/test/resources/isotopefinder/baseline/metrics_baseline.csv` — the
  last-known-good per-axis metrics. This is the reference you diff against.

## The before/after workflow (do this for every engine change)

The `isotopeBenchmark` task **overwrites the committed baseline CSV in place**. Use that to your
advantage — git shows the delta:

1. Start from a clean baseline: `git status` should show `metrics_baseline.csv` unmodified (it
   reflects the code at HEAD). If it's dirty from a prior run, `git checkout -- <baseline csv>`
   first.
2. Make your engine/scoring/model change.
3. Run the benchmark (see below). It rewrites `metrics_baseline.csv`.
4. **`git diff -- mzmine-community/src/test/resources/isotopefinder/baseline/metrics_baseline.csv`
   ** —
   every changed number is a per-axis metric delta caused by your change. Read the console table it
   also prints (metrics table + charge confusion matrix + harmonic/neighbour error rates).
5. Decide: if the change is a validated improvement (or a deliberate, explained trade-off), commit
   the
   new baseline together with the code. If it's a regression, fix or revert. If your change is not
   supposed to affect the default engine (e.g. an opt-in feature that's off by default, like element
   auto-detection), the baseline diff must be **empty** — a non-empty diff means you changed default
   behavior unintentionally.

## Commands

Run from the repo root (`D:\git\mzmine3`). Gradle supplies the test classpath and `--enable-preview`
automatically for all three tasks below.

```bash
# 1. Regenerate the baseline over the whole corpus with the CURRENT engine (signal/carbon-averagine
#    mode, requireC13=false). Overwrites metrics_baseline.csv; prints the table + confusion matrix.
#    This is the main tool for before/after measurement. ~1–2 min.
./gradlew :mzmine-community:isotopeBenchmark

# 2. Fast CI accuracy test (subset via IsotopeCorpus.ciCases(); untagged, part of the default `test`).
#    Must stay green. Asserts per-axis thresholds + start-signal invariance on strict axes.
./gradlew :mzmine-community:test --tests "*IsotopeAccuracyTest"

# 3. Locked unit behaviors + the auto-detector's own tests — must stay green after any engine edit.
./gradlew :mzmine-community:test --tests "*IsotopeFinderEngineTest" --tests "*ElementAutoDetectorTest"

# 4. The heavy @Tag("benchmark") test tier (excluded from the default `test` task).
./gradlew :mzmine-community:benchmark

# 5. ONLY when you changed the generator/catalog (GenerationConfig, BenchmarkPatternGenerator,
#    SyntheticSpectra) — NOT for engine changes. Rewrites the 15 MB corpus; deterministic (~15 min,
#    dominated by the largest protein enumerations). Regenerate the baseline (task 1) afterwards.
./gradlew :mzmine-community:generateBenchmarkCorpus
```

Minimum gate for an engine change: task 1 (read the baseline diff) + tasks 2 and 3 green.

## Reading the metrics CSV

One row per `axis` (the degradation family), plus a final `ALL` row. Columns:

| column                       | meaning                                                                                                                     |
|------------------------------|-----------------------------------------------------------------------------------------------------------------------------|
| `nCases`                     | cases in this axis                                                                                                          |
| `chargeTop1`                 | **top priority** — fraction where the winning charge equals the true charge                                                 |
| `chargeRecallAlt`            | true charge is the winner OR a flagged alternate                                                                            |
| `chargeStartInvariance`      | same winning charge regardless of which true peak seeds the search (position-agnostic property; strict axes must stay ~1.0) |
| `patternPrecision/Recall/F1` | detected isotope signals vs ground-truth true offsets                                                                       |
| `borderlineRecall`           | fraction of borderline signals kept (inclusiveness)                                                                         |
| `noiseLeak`                  | fraction of injected noise peaks wrongly kept (lower is better)                                                             |
| `elementPrecision/Recall`    | heavy-element (Cl/Br/S/Si) detection via `ElementAutoDetector`                                                              |
| `scoreMargin`, `aucCharge`   | winner-vs-runner-up separation, ranking quality                                                                             |
| `medianDetectMs`             | per-case detection time — watch for perf regressions (proteins are the slow axis)                                           |

`NaN` is expected where an axis has no cases of that kind (e.g. `noiseLeak` on non-noise axes,
`elementRecall` on unit-resolution where M+2 defects are unresolvable). Priorities, in order:
**chargeTop1 → chargeStartInvariance → pattern F1 → borderlineRecall / noiseLeak → element P/R**.
Hardest axes are the adversarial `interference` / `combined` (self-shifted decoy, ~0.52 top-1);
don't
chase those at the cost of the realistic axes (`charge`, `clean`, `noise`, `polyhalogen`,
`protein_highz`,
`unit_resolution`), which should stay ~0.97–1.0.

## Gotchas

- The benchmark JVM/generator must stay deterministic: **no `Math.random`, no `Date.now()`
  /wall-clock**
  in generator or scoring paths (seeds are FNV-1a of the case id). A non-deterministic change makes
  the
  baseline diff meaningless.
- `isotopeBenchmark` and `IsotopeAccuracyTest` exercise the **default engine only** (signal mode,
  `requireC13=false`, element detection USER_DEFINED). Opt-in features that are off by default won't
  show up in the baseline — add a focused test in `IsotopeFinderEngineTest` for those instead.
- If generation ever hangs, it's per-charge CDK on large proteins — check `MoleculeClass`
  minAbundance
  floors and for orphaned generation JVMs; the generator derives all charges from a cached charge-1
  pattern for a reason.
- CI never regenerates corpus or baseline; both are committed and loaded read-only.

## Key files

- `IsotopeBenchmarkMain` — the `isotopeBenchmark` entry point (runs current engine, writes
  baseline).
- `BenchmarkPatternGenerator` + `GenerationConfig` + `SyntheticSpectra` — corpus generation.
- `IsotopeMetrics` + `CaseMetrics` + `MetricRow` + `ChargeConfusionMatrix` + `BenchmarkReport` —
  metrics.
- `IsotopeCorpus` / `BenchmarkCorpusLoader` / `GroundTruthCase` — corpus loading + per-case
  truth/tolerance.
- `IsotopeAccuracyTest` (CI) and `IsotopeFinderEngineTest` (locked unit behaviors) — the test gates.
- Engine under test: `engine/IsotopeFinderEngine`, `engine/ChargeScore`,
  `signal/CarbonAveragineEnvelopeModel`,
  `engine/ElementAutoDetector`.
- Design/status log: `isotope_finder_benchmark_plan.md` at the repo root.
