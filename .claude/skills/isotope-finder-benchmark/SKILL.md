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
  **The corpus is committed on purpose, despite being fully reproducible from the generator.** The
  CDK isotopologue enumeration for the catalog's proteins is expensive (~15 min), so generating it
  per build or in CI is not viable; and committing the snapshot pins the exact inputs the baselines
  were measured on, so a baseline diff always reflects an engine change and never a corpus change.
  Do not propose generating it at build time or trimming it to save repo size.
- Baseline: `mzmine-community/src/test/resources/isotopefinder/baseline/metrics_baseline.csv` — the
  last-known-good per-axis metrics. This is the reference you diff against.
- Companion baseline: `.../baseline/metrics_requireC13.csv` — the same corpus with the opt-in
  require-13C gate on, regenerated with
  `./gradlew :mzmine-community:isotopeBenchmark --args="<path> requireC13"`. Nothing asserts against
  it; it is documentation. Regenerate it alongside `metrics_baseline.csv` whenever the engine or the
  corpus changes, so the two stay comparable.

### What the require-13C gate costs and buys

Comparing the two committed baselines (same corpus, gate off vs on) shows the trade clearly, and it
is not a strict win — do not enable it by default:

| axis | chargeTop1 off → on | patternRecall off → on |
|---|---|---|
| `cutoff` | **0.9930** → 0.9832 | **0.9856** → 0.9689 |
| `polyhalogen` | 0.9897 → 0.9536 | **0.9934** → 0.8940 |
| `resolution_merged` | 1.0000 → 0.9916 | **0.9986** → 0.9522 |
| `interference_real` | **0.9902** → 0.9832 | **0.9758** → 0.9489 |

The gate costs pattern completeness on every axis — worst on polyhalogens (−0.099 recall), where the
gap-truncation cuts the heavy comb short — and on this corpus it no longer buys charge accuracy
anywhere. Since incomplete patterns degrade downstream formula scoring, leave it off unless harmonic
confusion is demonstrably the dominant problem in the data at hand.

### Measured and rejected — do not re-propose without new data

Ideas that look right on paper, were implemented and benchmarked, and made things worse. The
reasoning behind each is in a `decision:` comment at the named constant.

| change | result | constant |
|---|---|---|
| Count only offsets the envelope predicts in the peak-count reward (as a harmonic guard) | ALL `chargeTop1` 0.9958 → 0.9931 (`upperBound` gate) / 0.9885 (`expected` gate); harmonic error 0.0019 → 0.0048 / 0.0096; `polyhalogen` → 0.9845 / 0.9742 | `TIE_WEIGHT` |
| Relax the gap-bridged significance floor proportionally to the predicted intensity | `noiseLeak` 0.0116 → 0.0121 (`noise`), 0.0174 → 0.0177 (ALL); no recall gain — the wide envelopes already reach recall 1.0000 | `MIN_BRIDGED_REL_INTENSITY` |
| Fold spacing consistency into the quality multiplicatively | regressed polyhalogen combs (a Cl₂/Br₂ comb at z=2 nearly aligns to the z=1 grid) | `SPACING_GRID_FACTOR` / `spacingConsistency` |

### The opt-in "only keep explainable signals" filter

`IsotopeFinderParameters.explainableSignalsOnly` (off by default) drops an emitted signal when it is
off the 13C grid and its mass defect matches no combination of the selected elements' isotopes. Off →
the committed baseline; on → measured over the same corpus:

| metric | off → on |
|---|---|
| `noiseLeak` | **0.0174** → 0.0162 |
| `patternRecall` | **0.9931** → 0.9909 |
| `patternF1` | **0.9938** → 0.9927 |
| `elementPrecision` | **0.8446** → 0.8365 |
| `chargeTop1` | 0.9958 → 0.9958 (unchanged) |

Two things make this filter subtle, and both are load-bearing:

- **The match window must be tighter than the m/z tolerance** (`ATTR_WINDOW_TOL_FACTOR = 0.5`).
  Candidates are only collected within the full tolerance of an isotope distance, so a window equal
  to the tolerance calls everything explainable and the filter becomes a no-op.
- **Mass defect cannot always separate chemistry**: an 81Br spacing sits 0.03 mDa from 15N+18O, so in
  a sufficiently rich element list almost any deviation is reachable. The filter's power comes from
  the element list being *narrow*, which is why the defect table is built from the user's elements
  (plus auto-detection candidates) and **not** from the default heavy-candidate set.

Since it is off by default, a baseline diff must stay empty when you change it — measure it by
temporarily enabling it in `BenchmarkRunner.buildEngine` and reading the console table.

Also note: `medianDetectMs` drifts by **2× between runs on the same machine and code** depending on
machine state. Never read a timing change from two runs made at different times — A/B it back to
back, and confirm with the ratio between a cheap axis and `protein_highz` rather than absolute ms.

### Retired axes: no adversarial cases

The corpus once carried two axes (`interference`, `combined`) built on an **adversarial** decoy: the
target's own envelope shifted by exactly half the isotope spacing, which synthesises a near-perfect
doubled-charge comb. They were removed — at ~0.55 `chargeTop1` they were the worst numbers on the
board and were repeatedly misread as a real-world failure rate, when in fact they measured the
theoretical maximum of harmonic confusion against an input no real spectrum produces. Optimising
against them risked making the harmonic guard trigger-happy on genuine data.

Co-elution is now measured only by `interference_real` (a different compound, non-harmonic offset,
scaled intensity). **Do not reintroduce a self-shifted decoy.** Their sweep slots are retired in
place (`SweepVariant.retired()`) rather than deleted, because the variant index is baked into every
case id and seed — deleting them would renumber later variants and silently change cases that
already exist. Append new variants; never reuse a retired slot.

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

# 4. The heavy @Tag("benchmark") test tier (excluded from the default `test` task). Contains
#    IsotopeBenchmarkRegressionTest, which runs the WHOLE corpus and fails if any per-axis metric
#    dropped more than 0.01 below the committed baseline — run this before proposing an engine
#    change as an improvement. Also the non-asserting IsotopeFinderScoreDiagnosisTest.
./gradlew :mzmine-community:benchmark

# 5. ONLY when you changed the generator/catalog (GenerationConfig, BenchmarkPatternGenerator,
#    SyntheticSpectra) — NOT for engine changes. Rewrites the 15 MB corpus. VERY SLOW: measured at
#    ~4 h, dominated by the largest protein enumerations — budget for it. Regenerate the baseline
#    (task 1) afterwards and commit corpus + baseline together.
#    NOTE: the result is NOT bit-reproducible (CDK's enumeration drifts ~1e-13 per call), so the
#    regenerated file always differs from the committed one. The drift is ~1.3 ppb in m/z and moves
#    the metrics by <0.003 — expect a large, noisy file diff that is nonetheless a no-op.
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
| `elementPrecision/Recall`    | heavy-element (Cl/Br/S/Si) detection via `ElementAutoDetector`; precision falls as the reported ambiguity set grows — see below |
| `elementContainment`         | **the asserted element metric** — fraction of cases where every true heavy element was reported                             |
| `elementSetSize`             | mean number of heavy elements reported (the cost of containment)                                                            |
| `scoreMargin`, `aucCharge`   | winner-vs-runner-up separation, ranking quality                                                                             |
| `medianDetectMs`             | per-case detection time — watch for perf regressions (proteins are the slow axis)                                           |

`NaN` is expected where an axis has no cases of that kind (e.g. `noiseLeak` on non-noise axes,
`elementRecall` on unit-resolution where M+2 defects are unresolvable). Priorities, in order:
**chargeTop1 → chargeStartInvariance → pattern F1 → borderlineRecall / noiseLeak →
elementContainment**.

Every axis is now realistic, so **every axis should stay ~0.99–1.0 on `chargeTop1`** — a drop
anywhere is a genuine regression, not an artificial stress case. The weakest metric on the board is
`elementContainment` (0.622 overall), which is the open problem worth working on; charge and pattern
completeness are essentially saturated.

### Heavy-element detection: `elementContainment` is the target, not `elementPrecision`

The detector reports every heavy element the evidence **cannot rule out**, ranked by confidence. Two
columns exist for that shape and the asserted one is `elementContainment` (was every true element in
the reported set?); `elementSetSize` is its cost (mean elements per case). `elementPrecision` and
`elementSetSize` are **reported but not asserted** by `IsotopeBenchmarkRegressionTest`, because both
move with the size of the ambiguity set rather than with detection quality — asserting precision would
fail every deliberate widening.

Membership is three tests, each of which had to exist for a measured reason:

1. **Spacing match** — some observed pair sits within the m/z tolerance (× charge) of the element's
   isotope distance. This is the coarse "possible" test.
2. **Intensity impossibility floor** (`REACH_FLOOR_FRACTION = 0.15`) — one atom must produce its
   per-atom M+2 abundance, so Br (~97 % per atom) is impossible on a weak comb. Without it, a plain
   CHNO ¹³C+¹⁵N peak (2.00039) is within 5 mDa of the ⁸¹Br distance (1.99795) and *every* organic
   molecule reports Br: precision 0.208 vs 0.294.
3. **Defect consistency** (`MIN_CONFIDENCE`) — the robust median defect must be consistent with the
   element at the precision the data supports, which is what keeps clean CHNO patterns empty.

`defectSigma` takes the **max of the spacing spread and `tol/√nPairs`**. The count term is essential:
with a single pair the spread is 0, which claimed sub-mDa precision on data that has none and dropped
`unit_resolution` containment to 0.021 (0.494 with it).

Measured progression, whole corpus (containment / recall / precision / set size):

| variant | elemIn | elemR | elemP | elemN |
|---|---|---|---|---|
| winner + defect-separated second (old) | — | 0.4370 | 0.8446 | ~1.0 |
| spacing match only | 0.670 | 0.670 | 0.208 | 1.99 |
| + intensity floor | 0.667 | 0.667 | 0.235 | 1.72 |
| + defect consistency, σ floor only | 0.504 | 0.505 | 0.457 | 0.78 |
| **+ count-aware σ (current)** | **0.622** | **0.624** | 0.294 | 1.32 |

`protein_highz` is 0.000 across every variant — no heavy pair is ever found there, so proteins are the
place to look next, not the reporting rule.

### Element alternatives are combined with max, not convolution

`CarbonAveragineEnvelopeModel.buildEnvelope` treats the **user's** elements as co-present (convolved)
but **detected** elements as mutually exclusive alternatives, taking the element-wise maximum of each
alternative's bound. Convolving them would bound the pattern as if the molecule contained Cl and Br
and S and Si at once, which loosens the bound multiplicatively and thereby widens `computeKeptOffsets`
termination and flattens `intensityAgreement`. With element detection off (the benchmark default) this
path is inert, so the change does not show in the committed baseline — cover it with
`IsotopeFinderEngineTest` auto-detect cases instead.

### The detector's input: the engine's raw window

`IsotopeMetrics.detectHeavyElements` runs the detector on
`ElementAutoDetector.collectDetectionWindow` — the raw spectrum window around the pattern — which is
the same call `IsotopeFinderEngine` makes during processing, so metric and production cannot drift. It
previously passed only the emitted pattern, i.e. measured a detector that does not ship.

**That honesty immediately showed the raw window to be a net liability.** Same detector, same corpus,
only the input differs (measured before the membership rework, so the absolute values are superseded —
the comparison is not):

| axis | pattern only (P / R) | engine's raw window (P / R) |
|---|---|---|
| `interference_real` | **0.8552** / 0.5839 | 0.4878 / **0.5959** |
| `unit_resolution` | NaN / 0.0000 (no detections) | 0.2976 / 0.0188 |
| `noise` | **0.7315** / 0.6331 | 0.7148 / 0.6331 |
| `polyhalogen` | 0.7186 / 0.8660 | 0.7186 / 0.8660 |
| `ALL` | **0.7185** / 0.4807 | 0.6579 / **0.4800** |

The window buys essentially **no recall** (0.4807 → 0.4800) and costs 0.06 precision, because it
hands the detector foreign peaks: a co-eluting compound's signals form spurious M+2 pairs, and — worse
— `detect` normalises every intensity against the *most intense peak of its input*, so an interferent
taller than the pattern rescales all the base-relative ratios the scoring depends on. At unit
resolution, where no M+2 defect is resolvable, the window manufactures detections that are ~70 %
wrong where the pattern-only input correctly produced none. Two candidate fixes, unmeasured:
normalise on the pattern's base peak rather than the window's, or feed the pattern's attributed heavy
signals instead of a raw window (the open half of review item 6.5).

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
