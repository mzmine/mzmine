# Auto-param optimizer: evaluation and direction

## Intention

The batch optimizer (`tools_autoparam.optimizer`) searches wizard parameters with MOEA/D,
warm-started
from a single-pass estimate derived from raw data statistics. This note records what a measurement
campaign over seven datasets established about that design, which conclusions did *not* survive
repetition, and the resulting decision about where the optimizer should go next.

It exists because most of the evidence below cannot be recovered from the code: it is the behaviour
of the search, not its structure.

## How the evidence was produced

`EstimateVsOptimumTest` runs the real `BatchOptimizationMainTask` headlessly over a list of datasets
and seeds, writing two configuration-suffixed CSV files under `build/autoparam-benchmarks` by
default:

- `autoparam-all-evaluations-<campaign>.csv` — every proposal of every run, with elapsed time,
  effective parameter values, metrics, and diagnostics.
- `autoparam-estimate-vs-optimum-<campaign>.csv` — one summary row per run per quantity.

The generated campaign suffix encodes the optimizer, metric, sampling, batch ceiling, seeds and
dataset selection. Set `-Dmzmine.test.autoparam.campaign=<short-name>` to use an explicit suffix.
The CSV files used for the findings in this note are retained in
[the optimizer test resources](../../mzmine-community/src/test/resources/io/github/mzmine/modules/tools/tools_autoparam/optimizer/results/),
separate from newly generated output.

`EstimatorStatisticsDumpTest` runs only the statistics pass (~30 s for all datasets, no batches) and
dumps the raw distributions the estimator takes its quantiles from, so candidate derivations can be
evaluated offline instead of by rerunning optimizations.

```
.\gradlew.bat "-Dmzmine.test.maxHeap=40g" \
  "-Dmzmine.test.autoparam.run=true" \
  "-Dmzmine.test.autoparam.dataRoot=D:/path/to/benchmark-data" \
  "-Dmzmine.test.autoparam.optimizer=PATTERN_SEARCH" \
  "-Dmzmine.test.autoparam.iterations=30" \
  "-Dmzmine.test.autoparam.seeds=42,7,101" \
  "-Dmzmine.test.autoparam.only=thermo-20y-qc,agilent6550-lipidmix" \
  :mzmine-community:test --tests "*EstimateVsOptimumTest"
```

Only `mzmine.test.*` system properties reach the test JVM in this build. The heap guard fails fast
rather than dying inside a batch; a real run needs at least 4 GB.

The campaign: 7 datasets (Thermo Orbitrap, SCIEX ZenoTOF ×3, Agilent 6550, two MSV000094173 sets),
10 seeds, 41 evaluations per run, single objective (Yasin isotope score).

## Established findings

Each of these reproduced across seed counts, configurations, or independent measures.

**The warm start does almost all of the work at a small budget.** With 41 evaluations the 20
perturbations reach 86–99 % of a run's final score, and the 20 evolution evaluations that follow add
a
median of +1.5 % and contribute *nothing at all* in 28 of 70 runs. This is a statement about the
budget, not about evolution — see "Evolution needs generations" below.

**Evolution compensates for a bad draw rather than improving on a good one.** Correlation between
warm-start quality and evolution's subsequent gain is negative on all seven datasets (median −0.37,
sign test p = 0.016). Runs whose warm start landed in the worst third gained +3.2 % median; the best
third gained +0.8 %.

**The seed dominates the result.** Same data, same settings, ten seeds: score spread 1.19× to 1.93×
(median 1.48×). Evolution does not damp it — spread before and after the evolution phase is
unchanged. Consequence: **no configuration change can be evaluated from single runs**, which is why
every bounds/perturbation/estimator comparison in this campaign landed inside the noise envelope.

**MOEA/D is structurally inert on a single objective.** `RandomGenerator.initializeWeightsND`
produces `[1.0]` for every weight vector, so all 20 subproblems are identical and the decomposition
does nothing. What runs is a plain (mu+lambda) search; it works, but not as MOEA/D.

**Evolution needs generations, and then it beats random sampling clearly.** At 41 evaluations —
population 20, so a single turnover — the evolution phase is indistinguishable from more random
draws.
At 401 evaluations, 19 turnovers, all three measures separate decisively:

| measure                                         | 41 evaluations  | 401 evaluations     |
|-------------------------------------------------|-----------------|---------------------|
| new bests against the 1/k null                  | 0.76× and 1.22× | **2.39× and 2.73×** |
| median evaluation score, evolution ÷ warm start | 1.06× and 1.02× | **1.73× and 1.24×** |
| trend within the evolution phase, r             | 0.048 and 0.120 | **0.608 and 0.406** |

Pooled record test: 15 observed against 5.86 expected, z = +3.8. The best solution improved 19.6 %
and
24.0 % over the 41-evaluation result, and on both datasets it was found at evaluation 382 of 401, so
the search had not converged even there. Measured on `thermo-20y-qc` and `zenotof-feces-pos`, one
seed.

The 1/k test is the useful one to repeat: under random sampling the chance that evaluation k beats
everything before it is exactly 1/k whatever the score distribution, so the expected number of new
bests over a range of evaluations is known without assuming anything.

**The response surface is smooth and low-order.** A linear model on the eight parameters explains
53–80 % of the score within a dataset. The dominant parameter's response is monotone across all ten
deciles.

**Three of eight parameters carry the score.** Standardised coefficients (σ of score per σ of
parameter, pooled over 2745 feasible evaluations, z-scored within dataset):

| parameter                 | coefficient | direction across the 7 datasets      |
|---------------------------|-------------|--------------------------------------|
| MS1 noise level           | −0.50       | 7 negative                           |
| Min consecutive           | −0.28       | 5 negative, 2 negligible             |
| Inter sample RT tolerance | +0.23       | 7 positive                           |
| Top-to-edge ratio         | −0.12       | 6 negative, 1 negligible             |
| MZ tolerance (ppm)        | +0.16       | 4 positive, 2 negative, 1 negligible |
| Min height                | −0.09       | 5 negative, 1 positive, 1 negligible |
| FWHM                      | +0.07       | 4 positive, 2 negative, 1 negligible |
| Chrom. threshold          | +0.04       | 6 negligible                         |

A coefficient near zero means no effect; 4/7 in one direction is a coin flip.

**PCA is the wrong tool for this data.** The eight components carry 17/15/14/13/11/11/10/9 % of the
variance — an uncorrelated cloud, because each parameter is perturbed independently. There is no
low-dimensional structure to recover.

**The objective is not being gamed.** This was the central open question and it resolved negatively.
Lowering the mass-detection threshold produces *fewer* non-reproducing rows and *far* fewer rows
without isotope partners, not more:

| measure                 | correlation with the noise threshold        | reading                                     |
|-------------------------|---------------------------------------------|---------------------------------------------|
| single-file rows %      | +0.18 (+0.32 with feature count held fixed) | a *higher* threshold gives more junk        |
| rows without isotopes % | +0.70, positive on all 7 (0.45–0.82)        | a lower threshold recovers isotope partners |

The score also already penalises non-reproducing rows (r = −0.03 to −0.61 on all seven). The
mechanism is that a lower threshold keeps more centroids per scan, so features detected in some
files
are detected in all of them, and their isotopes come with them. `MS1 noise level` running to its
floor
is the search finding a better result list, not exploiting the metric.

**The estimator is well centred on most parameters.** Optimum ÷ estimate, geometric mean over 70
runs:
min height 1.04×, FWHM ~1.0×, chrom. threshold 1.02×, top-to-edge 0.97×. Only the two parameters
with
structural problems deviate.

## Unresolved — and to be left unresolved

Recorded so these are not re-litigated. In each case two measurements disagreed, which means the
quantity is below the resolution of this setup, not that the later measurement is correct.

- **Min consecutive rule.** A constant 5 won against 5-seed targets; `0.5 × median(dataPoints)` won
  against 10-seed targets. Indistinguishable. The code keeps `0.5 × median`.
- **FWHM bias.** 0.82× at 5 seeds, 1.01× at 10. Unmeasurable in principle here: the quantile knob
  has
  11–24 % leverage while the optima span 0.65–1.40× the median.
- **A "best" seed.** Seed 101 won 5 of 7 datasets out of five candidates; out of ten candidates the
  wins split 3/2/2. No seed effect.
- **Whether wider RT tolerance is genuinely better.** Consistent positive direction (7/7) but
  tripling
  the ceiling changed the score by −3 %, inside the noise envelope. Not boundary-pinned any more
  (0 of 70 optima on the bound after the ceiling was raised), but the magnitude is not established.

## Decisions taken

**Estimator.** `MS1 noise level` for absolute-intensity detectors moved from the 15th to the 7th
percentile of chromatogram edge intensities; measured optimum sits at the 7.3rd, and this cuts the
typical error from 33 % to 7 %. `Inter sample RT tolerance` moved from `0.8 × max(deviation)` to the
98th percentile of the deviations, decoupling the estimate from its own search bound — the previous
rule was anchored to an extreme value and sat above the 99.6th percentile on every dataset. Its
ceiling is now `3 × max(deviation)`, after which no optimum sits on the bound (previously ~50 of 70
did). FWHM, min height, chrom. threshold and top-to-edge were left alone; they measure as unbiased.

**Warm start.** The initial population is entirely perturbations of the estimate. Uniform random
samples were measured 55 % worse than the estimate itself and never competed. Perturbation is
multiplicative for variables whose bounds span more than 10× — with an absolute sigma, min height
was
perturbed by 1.6–3.7× its own estimate and the noise level by up to 114×, which made those warm
starts
uniform draws over the whole box rather than perturbations. `WarmStartPerturbationTest` pins this.

**Diagnostics.** `ShapeScoreDiagnostic` is computed only when the shape rejection constraint is
enabled; it fits peak models and dominated evaluation cost otherwise. `PrecisionDiagnostic` is
always
computed and is cheap; it measures the failure mode shape fitting is blind to, since a marginal
detection is a small, clean, well-fitted bump.

**Reproducibility.** `PRNG` is seeded with `DEFAULT_RANDOM_SEED = 42` so every user gets the same
result from the same data without configuring anything. Only the programmatic headless constructor
accepts another seed, for benchmarks that need to vary the draw.

## Direction

The objective does not need fixing, which was previously the blocker. What remains is a smooth,
deterministic (cache-proven), effectively three-dimensional function that costs minutes per
evaluation and is worth optimising — the search buys 11 % to 193 % over the estimate depending on
dataset, and a further 20 % if the budget is raised tenfold.

**1. Replace the random warm start with a space-filling design.** Implemented as
`WarmStartSampling`; `SOBOL` is the one to use. Measured over 7 datasets × 3 seeds, the best
perturbation becomes bit-identical across seeds on every dataset (seed spread 1.28× → 1.00×) while
the
median score is unchanged at 1.00× of the previous behaviour. `LATIN_HYPERCUBE` does not help — MOEA
draws a random point inside each stratum, so it fixes the coverage but not the design, and its
measured spread (1.31×) is no better than independent draws. This is worth doing on its own: the
seed
lottery was the largest single source of variation in every result, and removing it is what makes
any
further comparison affordable.

**2. The remaining question is efficiency, not capability.** Evolution does work given generations,
so "replace it because it is weak" is not supported. What is still open is whether something else
reaches the same answer for fewer evaluations, which matters because 401 evaluations is about
18 minutes per dataset here and would be hours on larger files.

- *Bayesian optimisation* exists precisely for that trade: a deterministic expensive black-box with
  a
  good prior, aiming at tens of evaluations rather than hundreds. The concrete comparison to run is
  **can it reach the 401-evaluation result in roughly 80 evaluations**. Cost: a new dependency, and
  multi-objective BO is substantially more complex than the single-objective case.
- *Design of experiments plus a response surface* fits the measured smoothness and is cheaper to
  build, but its weakness is exactly the MS1 noise case — a quadratic fitted over a region whose
  optimum is at the edge places its stationary point outside the region.
- *Raising the budget* is the option that needs no new code at all, and on this evidence buys 20 %.
  Whether that is worth the wall clock is a product decision rather than a technical one.

**3. Keep MOEA/D for genuine multi-objective runs.** For a single objective it is a plain
(mu+lambda) search wearing a decomposition that does nothing; that is not a reason to remove it, but
it is a reason not to expect the decomposition's benefits.

### Pattern-search pilot

A deterministic adaptive pattern search was implemented after the main evaluation. The pilot was
intentionally stopped after the fourth completed dataset. Each run made 81 proposals: 80 real batch
executions and one cache hit for the already evaluated estimate.

| dataset            | estimate | best score | best / estimate | best batch | best time | finish time |
|--------------------|---------:|-----------:|----------------:|-----------:|----------:|------------:|
| thermo-20y-qc      |     3014 |       5766 |           1.91× |         78 |     250 s |       258 s |
| zenotof-plasma-pos |     7030 |      10974 |           1.56× |         75 |     117 s |       125 s |
| zenotof-plasma-neg |     3195 |       5007 |           1.57× |         68 |     164 s |       207 s |
| zenotof-feces-pos  |    15420 |      41330 |           2.68× |         66 |     393 s |       479 s |

Pattern search therefore improved the estimator on all four datasets, by 56–168 %. The best result
did not appear until batch 66–78, so the runs do not demonstrate convergence within 80 batches.

For the two datasets with matched controls, pattern search was also compared at each MOEA/D run's
elapsed finish time against seeds 42, 7 and 101:

| dataset           | median common time | pattern score | median MOEA/D score | difference |
|-------------------|-------------------:|--------------:|--------------------:|-----------:|
| thermo-20y-qc     |              125 s |          5412 |                4612 |    +17.3 % |
| zenotof-feces-pos |              319 s |         38226 |               30593 |    +25.0 % |

Pattern search won all six matched comparisons. Its final scores on those datasets also exceeded
the historical 401-evaluation MOEA/D scores: by 1.3 % on Thermo and 20.3 % on ZenoTOF feces.

This establishes pattern search as a strong, deterministic baseline. It does **not** establish it as
the preferred optimizer: the pilot covers only four datasets, direct time-matched controls exist for
two, and the late best batches leave open whether a more global method can find equal or better
solutions sooner. GP Bayesian optimization should therefore be evaluated under the same real-batch
ceiling and elapsed-time reporting before choosing a default.

### GP Bayesian-optimization pilot

The deterministic GP-ARD implementation was screened at the same 80-batch ceiling on the two
datasets with existing matched controls. Both runs made 81 proposals: 80 real batches and one cache
hit for the repeated estimate. Neither run used the numerical Sobol fallback.

| dataset           | estimate | GP best | best / estimate | best batch | best time | finish time |
|-------------------|---------:|--------:|----------------:|-----------:|----------:|------------:|
| thermo-20y-qc     |     3014 |    5447 |           1.81× |         44 |     127 s |       186 s |
| zenotof-feces-pos |    15420 |   33334 |           2.16× |         64 |     267 s |       332 s |

The primary comparison is best score at the GP run's elapsed finish time:

| dataset                  | GP score | pattern-search score | GP difference |
|--------------------------|---------:|---------------------:|--------------:|
| thermo-20y-qc, 186 s     |     5447 |                 5460 |       −0.25 % |
| zenotof-feces-pos, 332 s |    33334 |                38226 |       −12.8 % |

On Thermo the methods were effectively tied when GP stopped, although pattern search was stronger
in the first minute and later reached 5766 at 258 s. On ZenoTOF feces, GP improved the estimate
substantially but plateaued near 33334 while pattern search continued to 41330. The portion after
the 17-point initial design added 51.3 % on Thermo and 49.2 % on feces, so the sequential GP phase
was doing useful work; it was simply less effective than the coordinate search on this screen.

The current GP is therefore numerically sound but not more efficient than pattern search on this
screen. The result does not rule out Bayesian optimization in general: it specifically tests a
17-point estimate-centred Sobol design, regularised Matérn-5/2 ARD, constrained Expected
Improvement,
and a mixed global/local candidate pool. Any GP follow-up should change a concrete part of that
design rather than repeat these deterministic runs with another random seed.

The GP optimizer option, runtime implementation and dedicated tests were subsequently removed. It
did not improve the practical ranking and carried the highest numerical complexity. The pilot csv
files and this analysis are retained as evidence for that decision.

### Sobol-only control

To separate GP guidance from merely making more structured evaluations, `SobolSearchAlgorithm` was
run on the same two datasets. It consumes the identical 17-point estimate-centred initial design as
GP, then evaluates unused global canonical Sobol points until the same 80-real-batch ceiling. It is
implemented as a sequential algorithm rather than an enlarged MOEA/D initialization: MOEA/D would
either add evolutionary offspring or generate more local perturbations, so it would not be the same
control.

| dataset           | Sobol best | best batch | finish time | GP at finish | pattern at finish |
|-------------------|-----------:|-----------:|------------:|-------------:|------------------:|
| thermo-20y-qc     |       3600 |         15 |       112 s |         5368 |              5412 |
| zenotof-feces-pos |      22347 |         15 |       146 s |        33226 |             34452 |

On both datasets, none of the 63 post-design global Sobol points improved the common initial design.
This is strong evidence that GP's roughly 50 % post-design gain came from useful surrogate guidance,
not merely from spending the remaining batches. Pattern search nevertheless remained best at equal
elapsed time, so the current ranking on these controls is pattern search, GP, then unguided Sobol.

The standalone Sobol-search option, implementation, origin and dedicated tests were subsequently
removed. Sobol sampling remains in use for warm-start designs and pattern-search restarts.

### TPE pilot

The deterministic multivariate TPE implementation was run with the same 17-point initial design and
80-real-batch ceiling. Each run made 81 proposals, including one cache hit for the repeated
estimate,
and neither used its numerical Sobol fallback.

| dataset           | estimate | TPE best | best / estimate | best batch | best time | finish time |
|-------------------|---------:|---------:|----------------:|-----------:|----------:|------------:|
| thermo-20y-qc     |     3014 |     4949 |           1.64× |         68 |     186 s |       224 s |
| zenotof-feces-pos |    15420 |    35418 |           2.30× |         61 |     279 s |       379 s |

The primary elapsed-time comparison is:

| dataset, TPE finish      |   TPE | pattern search |    GP | TPE vs pattern | TPE vs GP |
|--------------------------|------:|---------------:|------:|---------------:|----------:|
| thermo-20y-qc, 224 s     |  4949 |           5632 |  5447 |        −12.1 % |    −9.1 % |
| zenotof-feces-pos, 379 s | 35418 |          40298 | 33334 |        −12.1 % |    +6.3 % |

TPE guidance was useful: relative to the shared initial-design best, it added 37.5% on Thermo and
58.5% on feces. It was not merely reproducing the unguided control. However, it lost to pattern
search by the same roughly 12% margin at both of its finish times. It also gives no consistent
advantage over GP: worse on Thermo, better on feces, and slower to finish on both. The current
two-dataset ranking therefore remains pattern search first; neither surrogate is a reason to change
the default yet.

The TPE option, runtime implementation, origin and dedicated tests were subsequently removed. Its
pilot csv files and this analysis are retained as evidence for that decision.

## A note on reading this evidence

Several conclusions here were reversed once measured again, and the pattern is worth recording. Two
measurements that disagree usually mean the quantity sits below the resolution of the setup — not
that the later one is right. Three specific errors were made and corrected:

- Extrapolating "evolution adds nothing at 41 evaluations" into "evolution is the wrong tool". The
  measurement was sound; the generalisation across budgets was not.
- Reporting single-run differences between configurations as real, when the seed alone moves the
  score 1.19–1.93× and swamps them.
- Treating a self-derived benchmark set as usable for a precision term. It gives valid recall and
  invalid precision, because an unmatched detection is not a false positive when the reference list
  is incomplete by construction.
