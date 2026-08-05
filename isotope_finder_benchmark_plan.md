# Isotope Finder — Benchmark-Driven Improvement Plan & Status

Branch: `isotope-finder-new-algo`. This document captures the design, implementation status, and
next
steps for improving the isotope finder (charge-state detection, isotope-pattern detection, element
detection) using a deterministic benchmark harness.

Status legend: ✅ done · 🟡 partial · ⛔ deferred/not started.

---

## 0. Context & goals

The new isotope finder is a **13C-first, position-agnostic** scorer (per feature: collect candidate
signals bidirectionally, score each charge hypothesis by sliding a predicted carbon envelope over
the
isolated 13C ladder, store an inclusive pattern per plausible charge). Priorities:

1. **Charge-state detection must be correct** (top priority).
2. **Isotope-pattern detection** — keep more borderline signals for later re-scoring, without noise.
3. **Element detection** — user-defined set + auto-detect of popular heavy elements.

Data is real high-resolution MS: small molecules and large ones (proteins,
poly-brominated/chlorinated,
multi-charge). The guiding principle throughout: **measure before/after with a benchmark; don't ship
a
scoring change that isn't validated.**

---

## 1. Benchmark & test harness ✅

Package `io.github.mzmine.modules.dataprocessing.filter_isotopefinder.benchmark` (test source set).

- **`BenchmarkPatternGenerator`** (`main` + gradle `generateBenchmarkCorpus`) — generates synthetic
  patterns via CDK (`IsotopePatternCalculator.calculateIsotopePattern`) across `catalog × charge ×
  7-variant sweep`, plus a **special single-charge unit-resolution block** (see below), and writes a
  committed **JSON-lines corpus** `src/test/resources/isotopefinder/corpus/patterns.jsonl`
  (**5919 patterns, ~16 MB**). Each line = one pattern: generation details + m/z/intensity arrays +
  ground truth (`trueOffsetsMz`/`borderlineOffsetsMz`/`falseOffsetsMz`/`trueHeavyElements`).
  Deterministic (FNV-1a seed per id; no `Math.random`/wall-clock).
- **Unit-resolution axis (`unit_resolution`, 921 cases) ✅** — special low-mass-resolution cases,
  **z=1 only** (multiply-charged sub-Da spacing is unresolvable at unit resolution) and **SMALL /
  PEPTIDE only** (intact proteins are not resolved on quadrupole / ion-trap instruments). Built from
  the collapsed charge-1 envelope (CDK merge width `UNIT_MERGE_WIDTH = 0.5` → one centroid per
  nominal
  offset), then `SyntheticSpectra.quantizeUnitResolution` puts it on a coarse, low-accuracy m/z axis
  (seeded ±jitter + **1-decimal rounding**, the max reported precision of a unit-mass instrument).
  Three variants under the one axis: clean coarse readout (jitter 0), poor mass accuracy (±0.1 Da),
  jitter (±0.05 Da) + 3 noise peaks. **Scored with a wide axis-specific tolerance**
  (`GroundTruthCase.unitResolutionTolerance` = 0.2 Da abs, 0 ppm) since low-res data is run with a
  Da-scale tolerance, not the high-res 5 mDa / 10 ppm default.
- **Noise separation scales with resolution ✅** — `SyntheticSpectra.addRandomNoise` now takes the
  case `MZTolerance` and keeps injected noise `NOISE_MIN_SEPARATION_TOL_FACTOR = 2 ×
  tol.getMzToleranceForMass(mz)` away from real / other noise peaks (was a hard-coded 10 mDa). So
  the
  min separation grows with the tolerance, i.e. with decreasing mass resolution: ~10 mDa at high res
  (unchanged behaviour), ~0.4 Da at unit resolution — noise no longer lands inside a true peak's
  tolerance window. Callers pass `GroundTruthCase.toleranceForAxis(axis)`.
- **`GenerationConfig`** — programmatic catalog: 1–20 Cl and 1–20 Br series (carbons rising >20 as
  halogens rise), mixed halogen, PCB/PBDE/dioxins, CHNO grid, S/P/Si/metal families, averagine
  peptides
  and proteins (~5–20 kDa), plus named real compounds. Charge rule: SMALL z1–2, PEPTIDE z1–3,
  PROTEIN up to `min(20, round(mass/700))`. **High charges (>3) only for proteins** (generator
  asserts
  this invariant).
- **`SyntheticSpectra`** — CDK builders + seeded degradation ops (intensity cutoff, noise,
  interference,
  `atCharge`). **Perf:** `atCharge(charge1, z, sign)` derives any charge from a cached charge-1
  pattern
  (`mz_z = mz_1/z − sign·e·(z−1)/z`, exact — CDK enumeration is charge-invariant); the generator
  computes one charge-1 pattern per unique formula in a parallel stream. Without this, per-charge
  CDK +
  large proteins **hangs** generation.
- **Metrics:** `IsotopeMetrics` + `CaseMetrics` + `MetricRow` + `ChargeConfusionMatrix` +
  `BenchmarkReport`. Per-axis metrics: chargeTop1, chargeRecallAlt, chargeStartInvariance, pattern
  P/R/F1, borderlineRecall, noiseLeak, element P/R, scoreMargin, aucCharge, medianDetectMs.
- **`IsotopeBenchmarkMain`** (gradle `isotopeBenchmark`) — runs the current engine over the corpus,
  writes the baseline `src/test/resources/isotopefinder/baseline/metrics_baseline.csv`, prints the
  table + confusion matrix. Regenerate deliberately; CI never regenerates.
- **`IsotopeAccuracyTest`** — fast CI test (`@ParameterizedTest` over `IsotopeCorpus.ciCases()`),
  per-axis thresholds; asserts start-signal invariance on strict axes. The repo's first `@Tag`
  (`benchmark`) splits opt-in benchmark tests from the default `test` task (
  `mzmine-community/build.gradle`).

**Start-signal invariance ✅** — every case is detected from the mono / base / top true peak;
`chargeStartInvariance` metric = fraction with the same winning charge. Overall **0.99**; **1.0 on
every
clean/real axis** (incl. polyhalogen and proteins to z19); only adversarial interference/combined
dip.

**How to run:** `./gradlew :mzmine-community:generateBenchmarkCorpus` · `:isotopeBenchmark` ·
`:test --tests "*IsotopeAccuracyTest"`.

---

## 2. Part B — Charge-state robustness ✅ (top priority)

All in `engine/IsotopeFinderEngine.java` (`detect`/`scoreCharge`) + `engine/ChargeScore.java`.

**Delivered:** overall charge top-1 **0.805 → 0.842** (266-case baseline), **0.869 on the 5919
corpus** (was 0.85 pre-unit-resolution; the 921 z=1 unit cases lift the mean); ~0.99 on all
realistic
axes incl. the 1–20 Cl/Br series and the new `unit_resolution` axis; no regressions; all 34
`IsotopeFinderEngineTest` locked cases green.

**Unit-resolution axis result (921 cases, wide 0.2 Da tolerance, 1-decimal m/z axis):** chargeTop1
**0.966**, chargeRecallAlt / startInvariance **0.999 / 0.998**, patternP/R/F1 **0.984 / 0.995 /
0.987**, borderlineRecall **0.989** — charge and pattern detection hold up well on the coarse
single-decimal axis once the tolerance matches. `elementRecall = 0` (and `elementPrecision` NaN — no
heavy elements claimed) is expected: the M+2/M+4 exact-mass defects the auto-detector relies on are
unresolvable on a coarse axis. `noiseLeak` **0.50 → 0.097** after the tolerance-scaled noise
separation (below); the ~0.10 residual is genuine — dense polyhalogen envelopes fill most nominal
slots, so a few noise peaks unavoidably fall near a true peak. (Before the coarser 1-decimal axis
and
the noise fix the axis read chargeTop1 0.9946 / noiseLeak 0.50; the drop to 0.966 is the honest cost
of the coarser reported precision.)

- **Carbon M+1/M upper-bound penalty** (the working harmonic-doubling discriminator): the
  isolated-13C
  M+1/M ratio must not exceed the max carbon prediction (`expectedM1RatioBounds`); a doubling whose
  "M+1" is a co-eluting compound's monoisotopic is penalised. Uses the isolated 13C ladder (no heavy
  contamination) and the reliable carbon bound — respects the 13C-first / no-heavy-penalty locks.
- **Weak-13C-ladder fallback down-weight** (`NEUTRAL_FALLBACK_WEIGHT`) and **count-invariant
  alternate
  flagging** (absolute `ALT_MARGIN` on bounded quality, replacing the `maxCharge`-dependent
  `prob≥0.2`).

**Charge-scaled minimum 13C-distance-signal requirement ✅ (misdetection guard, requirement).** A
high
charge is only accepted when enough signals a genuine **13C distance apart** are present, so a
heavy-isotope comb or a couple of noise peaks that happen to fall on the fine `1.00336/z` Da grid
are
never read as a high charge state. Implemented as a **hard cutoff** (`minSignalsForCharge(z)` in
`IsotopeFinderEngine`) — a cheap pre-filter on the raw candidate count plus the authoritative
`raw = 0`
veto. The veto counts `c13Signals` = **isolated 13C-ladder peaks** (`carbonLadder.size()`): signals
on
the *exact* charge-adjusted 13C grid, collected in **both directions from the base and including it
**.
Heavy isotopes (Cl/Br/S, ~4–9 mDa off the 13C grid) and off-grid noise do **not** count, so a high
charge must be backed by a real 13C ladder, not a halogen comb or grid-adjacent noise. Because these
are distinct positions on the 13C grid, requiring N of them also requires the pattern to span N−1
charge-adjusted 13C distances (the two coincide, not two independent checks).

- **Low-charge heavy-isotope fallback:** at the floor of 2 (z ≤ 3) any two isotope signals still
  qualify (`observedCount >= 2`), because heavy-isotope spacing alone legitimately carries a
  small-molecule detection (e.g. a C,Br molecule with a weak/absent 13C M+1 but a strong 81Br M+2 —
  locked test `terminationCrossesSmallM1GapToCaptureBrM2`). The escalated floor for **z ≥ 4** must
  be
  met by genuine 13C-ladder signals.
- Fixed levels (deliberately not scaled further): z≤3 → 2 (mono + M+1), z 4–5 → 3, z 6–9 → 4, z≥10 →
  5.
  The higher floor only starts above charge 3 (z=3 requires just 2, looser than the prior `z>=3?3:2`
  pre-filter).
- **Benchmark impact: none on the current corpus** (baseline diff empty except `medMs` timing
  jitter;
  `cutoff` 0.9146 and `protein_highz` 1.0 both unchanged). The generator only ever assigns high
  charge
  to real proteins with wide, many-peak envelopes, and the noise/interference axes sit on real
  molecules at their true (low) charge, so the corpus contains no "noise-only fake high-charge" case
  for
  the gate to suppress. The guard is therefore free here; its value is on real data (and on the
  deferred realistic-interferent axis, §6.2). Proven to fire by `IsotopeFinderEngineTest`
  `rejectsHighChargeStateWithoutEnoughSignals` (a 3-peak z=10 fine ladder is rejected; the full
  8-peak
  envelope still detects z=10). All engine/accuracy tests green (38 `IsotopeFinderEngineTest`).

**FT-ringing lower-bound carbon-ratio penalty ✅ (requirement, real-data fix).** Symmetric to the
harmonic upper-bound penalty: when the base peak is the monoisotopic, its isolated 13C M+1 must not
be
far *below* the carbon MINIMUM prediction for the mass the charge implies. Low-intensity FT ringing
around a strong singly charged signal forms a fake fine-spaced ladder at a high charge whose "M+1"
is a
tiny fraction of the base (physically impossible for a real 13C peak at the large mass a high charge
implies), so that charge's quality is down-weighted proportionally. Anchored on the observed base
(offset 0) so it is **placement-independent**; gated on the base being the mono (no 13C-ladder peak
above `MONO_DOMINANCE_FRACTION`=0.1 of the base below it, exempting protein/halogen mid-envelope
apices)
and on `carbonFit.assessed()` (exempting heavy-isotope-only patterns with no 13C ladder). The lower
threshold is a quarter of the already-conservative 1/20-C-per-Da minimum (`C13_RATIO_LOWER_FACTOR`
=0.25)
so genuine low-carbon / heteroatom-rich molecules (within ~2× of prediction) are never touched,
while a
~600× FT-ringing deficit is.

- **Motivating real case:** measured spectrum, m/z 734.4683 (1.17e9) monoisotopic of a singly
  charged
  compound with a clean 13C ladder (735.47/736.47/737.48/738.48); FT ringing (734.27…735.63, all <1%
  of
  the base) previously fit a z=5 grid and won. Now charge 1 with the 5-peak ladder. Locked as
  `IsotopeFinderEngineTest.ftRingingAroundSinglyChargedSignalIsNotReadAsHighCharge`.
- **Benchmark impact (improvement, no regressions):** ALL chargeTop1 0.8686 → **0.8741**, noiseLeak
  0.2065 → **0.1974**, patternF1 0.9508 → 0.9527, harmonic/neighbour error 0.118/0.103 →
  0.113/0.098.
  Driven mainly by `unit_resolution` (chargeTop1 0.966 → **0.999**, noiseLeak 0.097 → **0.003**,
  aucCharge 0.66 → 1.00) and `charge` (scoreMargin 0.36 → 0.59). `protein_highz` 1.0, `polyhalogen`
  0.990, `clean` 1.0 all unchanged — no realistic axis regressed. 39 `IsotopeFinderEngineTest`
  green.

**Require-13C lower-bound loosened for carbon-poor molecules ✅ (requirement, real-data fix).** The
optional "require 13C" gate rejected valid singly charged patterns of heteroatom-rich (
Cl/Br/S/metal)
molecules: their real 13C M+1/M is legitimately below the averagine carbon-minimum (1/20-C-per-Da)
prediction, but the old lower bound `m1Bounds[0]*(1-C13_RATIO_SLACK)` assumed at least that many
carbons. Now the gate's lower bound uses `REQUIRE_C13_LOWER_FACTOR`=0.5 (effectively allowing down
to
~1/40 C per Da) while the upper bound keeps the slack (still catching an "M+1" too large to be 13C).

- **Motivating real cases:** measured spectra m/z 271.0317 and 330.1267 (singly charged, Cl/S-rich).
  Locked as `requireC13DetectsChargeOneForHeteroatomRichMolecule271`/`...330` plus a synthetic
  carbon-poor guard `requireC13AcceptsCarbonPoorHeteroatomRichPattern` (which failed before the
  fix).
  NB: with current defaults the two real spectra already detect charge 1 (271 by a thin ~2% margin);
  the loosening removes that fragility.
- **Reproduced the user's exact config** (elements H,C,N,O,S; tol 0.009 Da / 25 ppm; maxCharge 10;
  require-13C on; element auto-detect on; signal mode; default carbon params) as
  `userConfigDetectsChargeOneForMolecule271`/`...330`: **both detect charge 1** (with require-13C on
  and
  off), and still do even with the require-13C lower bound reverted to the old value AND the
  FT-ringing
  penalty disabled. So the engine is not the cause of the reported no-detection for these spectra.
  `IsotopeFinderTask` was reviewed and is correct (feeds the representative scan's
  `ScanDataType.MASS_LIST`
  to `detect`, seeds from feature m/z/height, skips cleanly on null). Remaining suspects are
  data-level:
  the representative scan's mass list in the real run may differ from the pasted spectrum (a
  mass-detection intensity threshold dropping the 13C M+1, or a different scan selected). Needs the
  raw
  file / mass list to pin down.
- **Benchmark impact: none** (the gate is off by default; `isotopeBenchmark` runs
  `requireC13=false`,
  baseline diff empty). 42 `IsotopeFinderEngineTest` green.

**Tried and abandoned (documented in-code so they aren't retried):**

- Quality-primary winner selection — breaks genuine higher-charge detection (a lower charge fits a
  sub-grid of a higher-charge pattern). Winner stays `raw`-primary (bounded quality × peak-count
  reward).
- Spacing-consistency term folded into quality — breaks polyhalogen combs (a Cl₂/Br₂ comb at z=2 has
  ~1 Da steps that nearly match the z=1 13C grid). Kept as a diagnostic field only, not in
  selection.
- Stricter *per-charge-hypothesis `z+1`* min-signal gate (raising the floor for the immediately
  higher
  charge) — regressed the `cutoff` axis (high-charge patterns that lose weak peaks below the noise
  floor
  fall short). Note this is distinct from the charge-scaled minimum above, which is a fixed
  monotonic
  floor per charge and left `cutoff` unchanged; do not conflate the two.

**Hardest remaining axis:** adversarial self-shifted-decoy `interference`/`combined` (~0.52 top-1);
the
true charge is flagged as an alternate (recall@alt ~0.93). See §6 next steps.

---

## 3. Part D — Element auto-detection 🟡 (first slice done + hardened)

New: `engine/ElementAutoDetector`, `engine/DetectedComposition`, `engine/ElementIsotopes`.
**Standalone — does NOT touch the charge/pattern engine**; only the benchmark
`IsotopeMetrics.detectHeavyElements` calls it (replaced the old placeholder stub). Infers
{Cl,Br,S,Si}
from M+2/M+4 exact-mass defects + relative intensities; all masses/abundances from
`IsotopesUtils.getIsotopeRecord`.

**Design (robust to real-data conditions — validated by `ElementAutoDetectorTest`, 13 green):**

- **Mono-independent:** detects each element from Δ-spaced peak PAIRS across the whole envelope (not
  a
  lowest-peak-as-mono anchor); survives a missing/below-threshold monoisotopic.
  `detectAcrossCharges`
  tries multiple charges when charge is unknown.
- **Charge-aware:** spacing `Δ/z`; all windows scale by `tol × z`.
- **Tolerance-robust:** heavy M+2 strength measured **relative to the base peak** (partner ratios
  inflate on weak peaks); defect from the **median of significant pairs** (partner ≥30% of
  strongest)
  with a **self-calibrating sigma = 1.5×stddev(spacings)** (sharp when clean, forgiving under m/z
  jitter); a **soft atom-count cap** (≤8 free, else `cap/atoms`) stops a weak element claiming a
  strong
  comb.
- ⚠️ **Do NOT re-add a "fewest-atoms" Occam prior** — it flips Cl₄→Br (tried twice, both regressed).

**Honest physical limits (encoded, not faked):** Cl(1.997)/Br(1.998) are indistinguishable under
> 0.9 mDa jitter (report the halogen class); ³⁴S/³⁰Si (~1 mDa) and ³³S/²⁹Si (~0.2 mDa) are
> degenerate →
> S vs Si not separable at a 5 mDa tolerance.

**Benchmark (4998, exact masses — robustness not exercised):** element P/R ALL **0.84 / 0.49** (vs
the
pre-hardening 0.88 / 0.60 — a deliberate robustness trade); **polyhalogen P 0.96 / R 0.79**;
`resolution_merged` weakest (merged fine structure blurs defects); proteins ~0 (CHNOS, heavy
buried).

---

## 4. Isotope-pattern inclusiveness 🟡

The benchmark shows inclusiveness is **already strong** (borderlineRecall ~0.99), so tuning
(KEEP_CUTOFF / HORIZON) has no headroom and was not pursued. The remaining piece is **provenance
tagging** (below), whose value is downstream and not captured by current metrics.

---

## 5. Current verification status

- `./gradlew :mzmine-community:test --tests "*IsotopeFinderEngineTest"` — **34/34 green** (locked
  behaviors preserved).
- `... "*IsotopeAccuracyTest"` — **8/8 green** (incl. the new `unit_resolution` ci case and
  start-signal invariance on strict axes).
- `... "*ElementAutoDetectorTest"` — **13/13 green** (R1 mono-independent, R2 charge, R3 jitter, R4
  candidates).
- `./gradlew :mzmine-community:isotopeBenchmark` — baseline committed; charge/pattern columns
  unchanged
  by the element work (engine untouched).
- **Nothing is committed** (commit-when-asked). Changed/new files listed in §7.

---

## 6. Next steps ⛔

Ordered by value:

1. **Part D2 — element-detection engine integration. ✅** `ElementAutoDetector` is wired into the
   engine as a two-pass flow (charge chosen on carbon+spacing first, then elements detected at the
   chosen charge from the **raw spectrum** window around the winner, so off-ladder S/Si M+2 peaks
   are
   recoverable). Detected per-element counts feed a new `EnvelopeModel.buildEnvelope(..., detectedHeavyCounts,
   includeUserHeavies)` overload on `CarbonAveragineEnvelopeModel`, **replacing the crude `mass/200`
   ,cap-8
   heavy-atom heuristic** with per-element counts (the crude count survives only as the fallback for
   user heavies). Opt-in parameter `elementDetectionMode` (`ElementDetectionMode` enum:
   USER_DEFINED default / AUTO_DETECT / USER_PLUS_AUTO). **Off by default** — the two-pass block
   never
   runs under USER_DEFINED and the envelope reproduces the old bound byte-for-byte; charge selection
   is
   untouched (heavy bound feeds only the pattern upper bound, not the winner `raw`/`quality`). The
   winner's `DetectionResult` now carries the `detectedComposition`. New: `ElementDetectionMode`;
   engine
   8-arg constructor (6-arg delegates to OFF); `IsotopeFinderEngineTest` +3 tests (37 green).
   **Deferred within D2:** feed detected element ranges into formula-mode `MolecularFormulaRange` (
   see
   item 7); surface the composition on the `Feature` for downstream re-scoring (item 3/5).
2. **Interference-axis realism (a "check").** The `interference`/`combined` axes use a self-shifted
   copy
   of the same molecule (+0.5/charge) — a perfect harmonic ghost, arguably adversarial. Add a
   **realistic interferent** (a different co-eluting molecule at a random offset), re-baseline, and
   see
   whether the ~0.52 top-1 was a test artifact vs a real weakness before investing in more charge
   work.
3. **Part C3 — provenance tagging.** Tag each kept isotope signal as `C13_LADDER` /
   `HEAVY_ISOTOPE` /
   `BRIDGED` / `CONTIGUOUS_OTHER` (enum `IsotopeSignalProvenance` + record `IsotopeSignal`, threaded
   through the module-internal `DetectionResult`, NOT the shared `IsotopePattern`) so a downstream
   re-scorer can distinguish backbone from heavy/bridged peaks after formula assignment.
4. **Real-data overlay.** Build `RealPatternLoader` + a small curated set of real high-res spectra
   (CSV of m/z+intensity + truth labels under `src/test/resources/isotopefinder/patterns/`), gated
   by
   `@Tag("benchmark")`, skipped gracefully when absent. Wire an end-to-end module test via
   `MZmineTestUtil.callModuleWithTimeout` on a real mzML. This is the honest validation synthetic
   can't
   give; needs user-provided files.
5. **Element metric ← real detector.** Once D2 lands, the benchmark element metric already calls
   `ElementAutoDetector`; extend it to read the engine's detected composition directly.
6. **S/Si and mixtures.** Improve weak-element (S/Si) recall and Cl+S/Cl+Br mixture separation —
   inherent
   at a 5 mDa tolerance; a resolution-aware path (use the tight tolerance on genuinely high-res
   spectra) and M+4-comb atom counting are the levers.
7. **Formula-mode (deferred earlier):** envelope caching by (mass,charge,polarity),
   rank-then-average
   formula enumeration, feeding detected element ranges into `MolecularFormulaRange`.
8. **Housekeeping:** decide whether to commit the 14 MB corpus or `.gitignore` + regenerate; tighten
   CI accuracy-test thresholds now that the baseline is stable; consider a soft timing ceiling in
   the
   benchmark tier.

---

## 7. Files touched (uncommitted)

**Engine (Part B):** `filter_isotopefinder/engine/IsotopeFinderEngine.java`,
`engine/ChargeScore.java`.
**Element detection (Part D):** new `engine/ElementAutoDetector.java`,
`engine/DetectedComposition.java`,
`engine/ElementIsotopes.java`.
**Benchmark (test source set, package `...benchmark`):** `BenchmarkPatternGenerator` (
unit-resolution
block), `GenerationConfig` (`UNIT_MERGE_WIDTH`/`UNIT_RESOLUTION_AXIS`/`UnitVariant`/
`unitResolutionSweep`),
`MoleculeClass`, `SyntheticSpectra` (`quantizeUnitResolution`), `InjectionResult`,
`BenchmarkPattern`,
`GroundTruthCase` (`unitResolutionTolerance`/`toleranceForAxis`), `BenchmarkCorpusLoader`,
`IsotopeCorpus` (unit_resolution ci case), `IsotopeMetrics`, `CaseMetrics`, `MetricRow`,
`ChargeConfusionMatrix`, `BenchmarkReport`, `IsotopeBenchmarkMain`; test `IsotopeAccuracyTest`,
`ElementAutoDetectorTest`.
**Build:** `mzmine-community/build.gradle` (`benchmark` tag task, `generateBenchmarkCorpus`,
`isotopeBenchmark`).
**Resources:** `src/test/resources/isotopefinder/corpus/patterns.jsonl`,
`baseline/metrics_baseline.csv`.
