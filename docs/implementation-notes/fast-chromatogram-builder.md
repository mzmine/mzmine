# Fast chromatogram builder

## Intention

The ADAP chromatogram builder sorts all data points of all selected scans by intensity, which is
slow, holds every data point as an object in one int-indexed array and assigns data points to fixed,
non-overlapping Guava m/z ranges around the first seed data point. The ranges cause holes (a data
point of an ion falls into the range of a neighbor seeded at another retention time) and duplicate
chromatograms (the scatter of one ion spills into an adjacent, clipped range). The fast
chromatogram builder produces the same kind of result, one extracted ion chromatogram per m/z
channel across all selected scans, without a global sort, with memory proportional to the output
plus two bytes per data point, and without these holes and duplicates. Users usually tune the m/z
tolerance to their resolution, so the builder estimates it from the data by default. Ions that
saturate the detector shift their m/z at the apex, the builder keeps them in one chromatogram.

## Decisions

- The fast builder is an algorithm of the `ModularADAPChromatogramBuilderModule` (decision of the
  user), there is no separate module. `ChromatogramBuilderAlgorithms` (a `ModuleOptionsEnum`):
  "Fast (auto)" (default) determines all builder parameters for a sensitivity, "Fast" has user
  parameters with an auto or custom m/z tolerance, "mzmine <4.11" is the ADAP builder. Top level
  parameters (files, scans, suffix, RT correction) are shared, the builder parameters belong to
  each algorithm and keep the ADAP names. Stable ids `fast_auto`, `fast`, `legacy_adap`, the
  display names may change.
- Old batch steps and applied methods (parameter version 1, builder parameters at the top level)
  load as "mzmine <4.11" with their values and results (decision of the user): the top level name
  map points the old names into the legacy parameters, a missing algorithm parameter selects the
  legacy algorithm, the version message tells the user. The integration test batches therefore
  keep using ADAP. The saved user configuration loads the same way: after an update the module
  dialog opens with "mzmine <4.11" and without a message (only batches and presets show version
  messages) until the user selects another algorithm.
- The batch wizard uses "Fast" with its values and its tolerance as custom tolerance, not the auto
  parameters (decision of the user). The image builder and the DIA MS2 RT correction still use the
  ADAP task (`createLegacy`), imaging is not supported by the fast builder.
- Later modules read the used values with `ADAPChromatogramBuilderParameters.getAppliedSettings`.
  The applied method stores the values that were used: "Fast" its estimated tolerance, "Fast
  (auto)" the determined builder values, noise level, signal level and peak width in hidden
  parameters of its own parameter set (decision of the user), which the feature list summary
  shows. They are empty in the module parameters and a rerun determines them again. Replaced:
  copying the values into the parameters of "Fast", the user wanted to see them in "Fast (auto)".
- The batch step measurements (log line after each step and the CSV after the batch) have an
  `algorithm` column (decision of the user): the selected option of every
  `ModuleOptionsEnumComboParameter` of the step and of the parameters of the selected option
  (`StepAlgorithms`), the bare option for one choice, "name: option; ..." for several, empty
  without. Options inside other embedded parameters, e.g., optional advanced import
  parameters, are left out. The speed test CSV (`SpeedMeasurement`) has no such column.
- Contract: the fast task classes are not in the free list of the task controller library, the
  ADAP task is. With "Fast (auto)" as default and the wizard on "Fast", the chromatogram builder
  needs a logged in user until `FastChromatogramBuilderTask` and `FastChromatogramFileTask` are
  added to that list.
- One main task processes all files: it checks all files first, resolves the tolerance once and
  then runs one file task per file on the task controller (`addTasks` and
  `TaskUtils.waitForTasksToFinish`, the pattern of the multithreaded gap filling). A single file
  runs directly on the main task thread. Feature lists are added in the order of the files. Not
  `ThreadPoolTask`: it checks the licenses of its sub tasks again with a new auth service on the
  worker thread, which fails for tasks that are not in the free list of the task controller.
- The m/z tolerance is auto or custom, auto by default. The applied method stores the used
  tolerance for both options, so later modules that read the chromatogram builder tolerance get the
  actual value, and a rerun with auto estimates again with the same result (deterministic).
- Auto estimates from the 3 files with the most data points in the selected scans (ties by name),
  all files are assumed to share instrument properties. Two steps, each fitted with
  `MzScatterModel`, a gaussian plus uniform background per cell of m/z bins and intensity bins:
    1. Mutual nearest neighbors of consecutive scans give the scan to scan scatter (the pair scatter
       over sqrt(2) is the scatter of a data point around its trace center).
    2. A test build with twice this tolerance gives the deviations of data points from their
       chromatogram center, which also contain drift and ions sharing a chromatogram. The test
       tolerance is the fit window, loose noise data points are uniform within it.
       The requirement of each m/z bin covers 99.5% of the signal of all its cells, which keeps the
       heavy tail of weak signals. The tolerance is 1.5 times the envelope of both requirements
       (absolute and relative part fitted to the bins), rounded up to 0.1 mDa and 0.1 ppm.
- The factor 1.5 is calibrated with `ToleranceEstimationBenchmark` (features after resolving over a
  tolerance sweep, Orbitrap QE with two settings, GC-EI-TOF, LC-QTOF MSe, GC-Orbitrap, DOM
  Orbitrap). Tolerances below the plateau lose features quickly, above it slowly. The gaussian
  model misses rare larger shifts of intense signals, e.g., in the injection front of Orbitrap
  data. Without the factor, one data set lost 3% of the features.
- A cell counts only if its peak density is at least 3 times the background density and the
  gaussian is narrower than a quarter of the window, e.g., sparse noise at high m/z does not count.
  A clear signal needs a pair scatter at least 10 times below the median spacing of neighboring
  data points in a scan. Without one, both steps are repeated with the data points of at least half
  the min group intensity, then of at least the min group intensity. If none gives a clear signal,
  there is no estimate: auto falls back to the custom value with a warning in the log.
- The intensity floor is for data without noise filter in the mass detection. Noise dominates the
  pairs and the spacing, and the nearest neighbors of noise look like a broad gaussian that the
  cells accept: GC-QTOF data with absolute noise level 0 (36 M instead of 2 M data points) had a
  pair scatter of 23.6 ppm at a spacing of 92 ppm, and without the clear signal check the estimate
  was 252 ppm. The fallback of 10 ppm lost 7.7% of the features. The builder counts data points
  below the min group intensity as noise, and half of it is the noise level of the batch wizard for
  absolute noise levels (min group intensity = 2 x noise), so the floor gives the estimate of the
  filtered data (23.3 ppm on both). All data points come first, data with a noise filter keep their
  estimate. Rejected: always estimating from the data points above the min group intensity, it
  lowered the filtered GC-QTOF estimate from 23.3 to 16 ppm, at the lower end of the plateau, and
  would change the estimates the factor 1.5 was calibrated on.
- Pass 1 loops once over the scans. Each m/z sorted mass list is merged against the active traces,
  which are sorted by their intensity weighted center. A data point joins the trace with the lowest
  cost within the tolerance, a trace takes at most one data point per scan, conflicts are resolved
  by a greedy matching so a losing data point can join its next best trace. The cost is the squared
  relative m/z distance plus a penalty for intensity jumps beyond 5x between neighboring scans; the
  penalty keeps noise close to the center from replacing the signal of a trace.
- Only statistics of closed traces are kept. Pass 1 records the trace of every data point in
  `TraceAssignments`, the slot of the trace in the sweeper and whether the data point started it (2
  bytes per data point, 4 if a scan needs more than 32767 slots). Pass 2 reads the scans again and
  routes the data points without detecting the traces again. Replaced: replaying pass 1 in pass 2,
  which took as long as pass 1 (pass 2 of GC-QTOF data without noise filter 3.5 s → 2.0 s, 72 MB
  for its 36 M data points). Contract: both passes load a scan with `ScanDataPoints`, the data
  point indices must match.
- Channel consolidation visits recorded traces by decreasing maximum intensity. A trace joins the
  closest channel seed within the tolerance, otherwise it starts a channel if it reaches the
  minimum height (only intense signals start chromatograms, like ADAP). Traces that repeatedly
  received data points in the same scans (collisions) stay in separate channels: they are signals
  resolved by the instrument within the tolerance, which ADAP merges or loses.
- Complementary traces up to 2x the tolerance join a channel if they overlap in time with its seed
  or continue it within the gap allowance and never share a scan. One ion yields one data point per
  scan, so this is one ion with more scatter than the tolerance or a centroid jump, not a second
  ion. Parts further away from the seed are merged after pass 2, see below.
- The complementary join also puts a separate peak that ends right before an intense seed 1 to 2
  tolerances away into the channel of the seed, e.g., 668.414 at -12 ppm before 668.423 on QE
  data. Kept: the resolver separates both peaks again and each feature takes its m/z from its own
  data points. `ChromatogramBuilderBenchmark` counts joins whose trace apex is more than the gap
  allowance outside the seed trace and looks for their peaks in both resolved lists. Over all 24
  file runs of the data sets (with and without noise filter): 8 peaks were resolved only from the
  ADAP chromatograms (4 peaks in both noise variants, at most 2 per file, all QE with sensitive
  settings, 7E4-2E5 next to seeds of 1.5E6-7E8) and 109 only from the fast ones. All 8 are below
  the chromatographic threshold of the merged chromatogram, the 90% intensity quantile that the
  intense seed sets. Rejected: accepting a complementary trace only with its apex next to the
  seed. It would split ions under a global m/z shift the way ADAP does, e.g., ~10 ions of a QE
  file shifted by +13 ppm for 15 scans. The comparison tool shows such peaks as "taken".
- Traces with a single data point, and short traces that cannot start a channel, are not recorded.
  Their data points are loose: in pass 2 a loose data point fills the closest channel within the
  tolerance that has no data point in this scan. This fills holes and keeps the low level signals of
  the full chromatogram, which the resolvers use for their chromatographic threshold. A closer loose
  data point replaces a farther one, which then tries the next channel.
- One data point per channel and scan: two member traces keep the more intense data point, the
  other one becomes loose; member data points win over loose ones, among loose data points the
  closest to the center wins.
- Holes of up to 3 scans between two data points of at least the min group intensity are filled
  with data points that no channel used, within 2x the tolerance around the interpolated m/z, if
  the intensity is within 5x of the log-interpolated intensity. A signal does not vanish for a
  scan, its centroid is shifted, e.g., by a coalescing neighbor or a split peak. The min group
  intensity is what the user counts as signal, the consecutive segments consist of it
  (`FastChromatogramBuilderOptions.holeFillFlank`). Replaced: flanks of at least the min height.
  Most intense holes of the final result whose data point was unused were next to a flank between
  the min group intensity and the min height (35 of 136 on a QE sensitive file), and the resolver
  ended the feature there. With the min group intensity (2026-09-28): truncated features on QE
  sensitive 271 → 188, QE workshop 59 → 40, QE media workshop 36 → 20, split peaks 17 → 16, ADAP
  features found in fast on LC-QTOF MSe 98.62% → 99.31%, synthetic chromatogram holes −30 to
  −60% (high m/z error 1729 → 960) with replaced data points +1%, Auto estimates unchanged or at
  most 0.2 ppm lower. Cost: features with foreign m/z data points on QE sensitive 689 → 769, p95
  of the m/z spread +0.06 ppm. Data sets with min group intensity = min height (GC-EI-QTOF,
  ZenoTOF) are unchanged. Rejected: no min flank intensity. It also fills the edges of weak
  features at the noise level, on GC-EI-QTOF without noise filter truncated features 726 → 139
  but features with foreign m/z data points 296 → 688 and fast features found in ADAP −0.8
  percentage points; at these intensities the fill rule cannot tell the ion from noise. The
  coalesced fill keeps the min height, it is for intense apex centroids. The unused data
  points of the last scans are kept in a ring buffer, so this needs no extra pass. Only unused data
  points within 5x the tolerance of a channel center are kept (flanks at most the complementary
  tolerance plus the tolerance from the center, plus the hole fill tolerance): without noise filter
  most data points are far from all channels, and sorting them per scan took a third of pass 2.
  They arrive in a few sorted runs, a natural merge sort sorts them.
- After pass 2, `ChannelFinalization` merges complementary channels: up to 2x the tolerance apart,
  never sharing a scan, and the target has a data point within the gap allowance of the apex of the
  merging channel that is at least its apex intensity / 5. Passing channels merge into a more
  intense one, failed channels into any passing one. The consolidation checks the time overlap only
  against the seed trace, which split intense ions with a shifted m/z at the apex from their tails.
  Rejected alternative: merging on "never share a scan and touch in time" alone. With sensitive
  settings (noise factor 2, min height 5E4) it merged ~940 channels, e.g., a peak eluting while the
  baseline of an intense channel 18 ppm away is missing, and 1.3-1.6% fewer ADAP features had a
  matching resolved fast feature. A peak with its own apex is no part of the other channel.
- Ions that saturate the detector shift their m/z at the apex. On the Agilent GC-EI-QTOF series
  (6 local files), intensities stop at ~7.4E6 and the apex of m/z 204.10 drifts by up to +67 ppm
  over a plateau of ~20 scans, 1.8-2.5x the tolerance (preset 5 mDa/20 ppm and Auto 23.3 ppm) and
  beyond the complementary tolerance. The plateau formed a channel of its own (e.g., 204.111 with
  42 data points from all saturated peaks) and left a dip to zero in the channel of the ion, two
  to three per file in 5 of 6 files. `ChannelFinalization` therefore bridges dips: a segment of
  another channel (data points with gaps of at most the max gap scans) moves into a passing channel
  if all its data points are within 4x the tolerance of the channel center, the channel has a data
  point of at least the min height within the gap allowance before and after the segment, both
  within 5x of the most intense segment data points at that edge, the segment stays within 5x of
  both flanks (no valley, no larger peak), the channel has no data point above segment / 5 in the
  scans of the segment (those are replaced and recovered like failed data points), and the
  channel has at least half as many data points as the segment within the length of the segment
  before and after it. The valley and the last rule keep the long segment of the ion, between two
  saturated apexes, from moving into the channel of the plateaus. Segments are bridged only if
  they reach half the most intense data point of all scans, the detector limit. Rejected: bridging
  all segments above the min height. On QE data (sensitive settings) it moved ~400 segments per
  file between neighboring channels 1-3 tolerances apart, and ADAP features found in fast dropped
  by 0.2 percentage points (98.40% → 98.18%). With the limit, only the saturated plateaus of the
  GC-QTOF data are bridged, all other data sets are unchanged. The 4x window has margin over the
  largest observed shift (2.5x), and the dip rules keep it from joining other ions.
- Channels that fail the filters would take their member data points with them, e.g., the apex of
  an intense ion taken by the trace of a co-eluting side signal in its apex scan. Their remaining
  data points fill free scans of passing channels with the rules of pass 2 (closest channel within
  the tolerance, then holes between data points of at least the min group intensity within 2x the
  tolerance), the most intense
  first. On a QE QC file (workshop settings, Auto 9.5 ppm): 28 merges, 348 recovered data points,
  ADAP signals missing in the fast chromatograms 56 → 42 groups in the comparison tool. Failed
  channels without a passing channel within the hole fill window are skipped (most noise channels
  of data without noise filter), the data points are sorted by a radix sort of the intensity bits,
  ties by scan and channel (before: scan and m/z, the same except for data points of equal
  intensity in one scan).
- Two ions that the instrument does not resolve give one centroid between both m/z when both are
  intense, which only the closer channel takes. E.g., GC-EI-QTOF 022 with Auto (25 ppm): 262.120
  and 262.133 (+48 ppm) give one centroid at +23 to +32 ppm in 8 apex scans of 262.120, 5 of them
  consecutive. The hole fill of pass 2 cannot fill these holes, it uses only unused data points
  and holes of up to 3 scans. Last step of `ChannelFinalization`: a hole between two data points
  of at least the min height is filled with the data point of another passing channel in the same
  scan that lies between the m/z interpolated in the hole and the median m/z of its channel, is
  shifted from this median toward the hole by at least half the tolerance, within 2x the tolerance
  of the interpolated m/z and within 5x of the interpolated intensity. The data point stays in its
  channel, it is the only case of one data point in two chromatograms. Holes up to 3 scans are
  filled scan by scan, longer holes up to 8 scans only if every scan is filled (a partly filled
  long hole is a hole of the ion). The fills are applied after the search, a shared data point is
  not shared again. The median and not the intensity weighted center: the shared apex centroids
  pull the center of their channel toward the hole (262.1327 instead of ~262.1333), which made the
  shift check fail on synthetic data. The median needs a sort per channel, it is computed only for
  donors that passed the m/z and intensity checks (all channels with a hole: +30% builder time on
  GC-EI-QTOF, now 178 → 187 ms). The tolerance estimation builds without this step: a shared
  centroid is no scatter of one ion, it lowered the estimate on 022 from 25.0 to 23.2 ppm.
- Results of the coalesced fill: GC-EI-QTOF 021 (preset) 800 filled runs, 959 data points (671
  runs of 1 scan), fillable dips 36 → 25, ADAP found in fast 99.64% → 99.69%, fast found in ADAP
  97.93% → 97.43% (72 more fast features, 59 more where the ADAP chromatogram has more holes: a
  hole is a zero between the flanking zeros, the filled peak reaches the min scans). QE sensitive
  fillable dips 56 → 49, all other data sets unchanged. 22 runs on 021 and 40 on 022 exceed both
  flanks by more than 1.5x (none 3x), weak data points (1E3-1E4), several in the artifact channels
  around m/z 204.5-204.75. Open, decision of the user: the shared data point keeps its m/z, which
  moves the weighted m/z of the receiving chromatogram toward the neighbor: within 10 scans of a
  fill median 4 ppm, p90 11 ppm, max 34 ppm on 022; the peak of 262.120 moves from +2.7 to +16.0
  ppm (feature m/z is intensity weighted, `FeatureDataUtils.DEFAULT_CENTER_MEASURE`). The neighbor
  262.133 already had this bias (+38 ppm instead of ~+50). Alternative: the copy gets the
  interpolated m/z of the hole, no m/z shift, but an m/z that is not in the mass list.
- Traces are closed after 3 scans without data point, traces with a single data point after 1 scan.
  The early close keeps the number of active noise traces low. It cannot connect an ion whose m/z
  alternates every scan by more than the tolerance, `singleDataPointMaxGapScans = 1` can, at ~50%
  more runtime on noisy data.
- Without noise filter in the mass detection, both passes sweep mostly noise: GC-EI-TOF with
  absolute noise level 0 has 23x the data points, 1.6 M traces, 333 k collision events and 3.1 M
  loose data points of which 22 k fill a channel. Speed work on this worst case (GC-EI-QTOF without
  noise filter, 36 M data points, 17 M traces, 1 M recorded, 3 M collision events), builder time
  8.8 s → 4.9 s, the results are identical: pass 2 without replay; the consolidation sorts the
  traces with radix sorts of the double bits and finds windows with `BinnedLowerBound` (1.6 s →
  0.7 s); `TraceCollisionGraph` counts the events of each trace pair before it looks up the
  records, 0.5 M pairs instead of 3 M events; the recovery skips unreachable failed channels
  (1.6 s → 0.7 s); channel buffers are sized to their member traces. The fast task is now
  1.4-4x faster than ADAP on TOF data without noise filter (before 0.6-2.1x, the small MSe file
  was slower) and 1.6-14x on the filtered data sets, the small files gain least. The remaining
  task time of filtered data is mostly the feature creation of mzmine, shared with ADAP: the
  quality parameters (FWHM, tailing, asymmetry) of each chromatogram are ~40% of it. They are kept,
  the chromatogram list shows them like the ADAP list.
- The height filter is applied within the consecutive segment, which the ADAP code comment
  describes but its implementation does not do (it never resets the maximum). ADAP therefore keeps
  chromatograms whose only consecutive segment stays below the minimum height, the fast builder
  does not.
- Zero, negative and non-finite values are skipped. Flanking zeros are added next to every detected
  data point like in ADAP, with the unweighted mean m/z.
- MS2 scans are found with a precursor m/z sorted index and return the same scans in the same order
  as `ScanUtils.streamAllMS2FragmentScans`.
- Internal tuning values are grouped in `FastChromatogramBuilderOptions` and are not user
  parameters. `ChromatogramBuilderBenchmark` compares both builders on synthetic data with ground
  truth and on real data, `ToleranceEstimationBenchmark` compares the estimate with the presets
  and a tolerance sweep (tag `benchmark`, run with `gradlew benchmark`). Both use the data sets of
  `ChromatogramBenchmarkDatasets` with the settings of the batch wizard next to the data (noise
  level, builder parameters, crop and polarity of the builder scans, preset tolerance), each once
  more without noise filter in the mass detection (factor 1 of the lowest signal, absolute level
  0) and the same builder settings. A data set with missing files or OneDrive placeholders is
  skipped with the reason, reading a placeholder downloads it. The ZenoTOF data set is the NIST
  SRM 1950 plasma file `1_Srm1950_DDA/Pos/20230407_plasma_6_POS.mzML` (the feces file was a
  placeholder). `ChromatogramBuilderBenchmark` also counts dips of both builders: up to 50 scans
  without data point, or with data points below the weaker flank / 5, between two data points of
  at least 10x the min height; fillable if at least half of the dip scans have a mass list data
  point within 4x the tolerance and 5x of the interpolated intensity.
- The benchmark judges the final result, the features after the local minimum resolver, not only
  the chromatograms. On real data (`FeatureHoleMetrics`), a dip of a chromatogram (the rule above,
  but between any two data points) counts only where a resolved feature of the same builder is:
  inside a feature (a hole), between two features (a split peak) or at the end of one
  (truncated). Its weight is the log interpolated intensity relative to the feature height:
  intense from 10%, apex from 50%. The height relative to the feature and not an absolute flank
  intensity: a hole at half the height of a 5E4 peak changes the peak as much as one of a 5E7
  peak, and the absolute 10x min height of the dips missed weak peaks. Fillable: in at least half
  of the dip scans a mass list data point within 2x the tolerance (the complementary tolerance,
  the widest join of one ion) and 5x of the interpolated intensity that is unused or in a
  chromatogram without data point in both flank scans. Rejected: the 4x window of the dips and
  any owner. Most intense "truncated" features were then co-eluting neighbor ions, e.g., 40-180
  ppm away on GC-EI-QTOF (5 mDa at m/z 100 is 50 ppm), and truncated features on QE sensitive
  dropped from 1625 to 1236 (ADAP) and 731 to 271 (fast) with the final rule. A chromatogram with
  data points at the flanks co-exists with the dipping ion, the rule of complementary channels
  that never share a scan. Unfillable intense holes are dropouts of the raw data and should be
  equal for both builders. Lost signal: intensity of the fillable data points / intensity of all
  features. Intense holes inside a feature are rare for both builders: the local minimum resolver
  splits at the zero of a hole, holes show as split peaks and truncated features.
  `FeatureMzMetrics` measures the m/z spread of the data points above 10% of the height, features
  with such a data point beyond the tolerance (foreign) and the m/z difference of matched features
  of both lists. The summary at the top of the report sums these per data set and builder.
- Synthetic data with ground truth is also resolved (`ResolvedGroundTruthEvaluator`): each feature
  belongs to the ion with the most intensity among its data points, per detectable ion found,
  split, missed, false features, intensity recovery, intense holes within the main feature, m/z
  error to the true m/z and contamination. Two cases model the real failure modes:
  `saturated` (40 ions of 3-20x a detector limit of 2E6, intensity capped, m/z +25 ppm per factor
  10 above it, up to 3.3x the tolerance) and `coalescing pairs` (300 partners 15-25 ppm away that
  give one centroid at the weighted m/z while the weaker has at least 20% of the stronger; the
  centroid belongs to both ions, `SyntheticLcmsData.sharedLabels`).
- `ChromatogramBuilderProfile` profiles the builder alone, the fast file task and the ADAP task on
  the first file of each data set and on synthetic data of growing size (`scaling`): time,
  allocated bytes of the thread, retained heap of the result, the peak live heap sampled with full
  GCs from a second thread (a lower bound), and for the builder the live heap at the pass
  boundaries (full GCs when the builder resets or finishes the scans, no hook in the builder).
  With `profile.jfr=true` it records the timed runs and `JfrProfileSummary` writes the time and
  allocation by phase (callees of `FastChromatogramBuilder.build`, of the file task and of the
  ADAP task), the hot methods and the allocation by class and by allocating mzmine method.
- Remaining fillable dips of the fast builder are mostly two neighboring chromatograms 1-4
  tolerances apart that take turns in the same scans (two close ions whose centroids coalesce, or
  one ion that switches between two m/z states), e.g., 212.089/212.095 on QE data. ADAP splits them
  the same way, the coalesced fill fills the holes of such pairs if the shared centroid lies
  between them. The fast builder has 8x fewer fillable dips than ADAP on QE data (sensitive: 49 vs
  408). Short holes whose signal is in another chromatogram ("stolen") are more frequent than with
  ADAP only on GC-QTOF data with the preset tolerance, at low m/z: 5 mDa is 71 ppm at m/z 70, and
  two centroid populations 40 ppm apart end up in two channels with the same center that take the
  data points of each other. With Auto (23.3 ppm) they are separate channels and the stolen holes
  halve (2012 → 1084).

### Fast (auto)

- "Fast (auto)" determines the min intensity for consecutive scans, the min height, the min
  consecutive scans and the m/z tolerance from the same 3 sample files as the tolerance estimate,
  for the sensitivities Sensitive, Medium (default) and Abundant (decision of the user).
  `BuilderParameterEstimation`, all values are rounded to 2 significant digits.
- The intensity levels come from `SignalPersistenceProfile`: per intensity bin (10 per decade) the
  fraction of data points with a data point in the next scan within the near window and 5x of
  their intensity, minus the hits in control windows of the same width 2-3 near windows away
  (chance hits, up to 0.33 on unfiltered TOF data), as `(near - control) / (1 - control)`. Noise
  level = from where half of the data points are signals (N50), signal level = from where 95% are
  (N95). Rejected: the lowest intensity of the mass lists or a fixed intensity quantile, both
  depend on the noise filter of the mass detection and do not exist for unfiltered data. The
  profile rises smoothly from 0 to 1 on all data sets, e.g., unfiltered GC-EI-TOF 0.05 at 6, 0.5
  at 130, 0.95 at 500, 0.99 at 1000 counts.
- A noise filter removes the next-scan partners of weak signals, so filtered data have a low
  fraction right at their floor (0.45 at 500 for TOF data filtered at 500) and N95 is about twice
  the filter level. N95 of filtered and unfiltered imports of the same data agree within ~2x,
  N50 within 1.3x on Orbitrap, GC-Orbitrap, LC-QTOF MSe and DOM and within 4.4x on GC-TOF data.
  Unfiltered ZenoTOF data have persistent signals down to ~10 counts (N50 11, N95 130 vs 500 /
  1200 with the wizard filter of 500): the levels follow the data the builder receives.
- The levels are searched from the most intense bin down: the level is where two consecutive bins
  fall below the fraction, single bins scatter. Searched from the bottom, persistent data points
  far below the noise (ion tails in synthetic data without detection threshold) set the level.
- The near window is sqrt(2) times the 99.5% scatter tolerance of consecutive signals
  (`MzToleranceEstimation.findScatter`), tried on all data points and then above the 50, 75, 90,
  97 and 99% intensity quantiles, and once more above N50 of the first profile. Without that
  refinement the unfiltered LC-QTOF MSe data had a near window of 306 ppm and N50 477 instead of
  ~620.
- Sensitivities (calibrated with `BuilderParameterEstimationBenchmark` against the wizard settings
  of the benchmark data sets, see the baseline below): Medium = min group N50 and min height N95,
  the results of the wizard defaults (98-100% of the wizard features on the QE QC, GC-EI-TOF and
  GC-EI-QTOF data with the wizard noise filter, the wizard uses 1E4 / 5E4 for Orbitrap and 1E3 /
  1E3 for TOF). Sensitive = half of both. Abundant = 2x N95 and 10x N95, the QE workshop settings
  (1E5 / 5E5: 99% of their features from the factor 2 import). The first version had Medium 3x
  stricter than the wizard and lost ~40% of its features.
- Min consecutive scans = 0.3 / 0.5 / 0.8 times the median full width at half maximum in scans of
  the peaks with an apex of at least 10x N95 in the test build of the tolerance estimation, within
  3 and 20; without 20 such peaks 3 / 4 / 6. A lower apex limit (10x N50) measured noise spikes on
  unfiltered ZenoTOF data (3 instead of 5 scans). Widths: QE 5, LC-QTOF 5, GC-EI-TOF 8, GC-EI-QTOF
  10, GC-Orbitrap 14-15 scans. The DOM test files (87 scans) give 2 scans, the clamp applies.
- Fallbacks: without N50 the median intensity, without N95 2x N50, without a clear scatter the
  fallback tolerance of the task, without a tolerance estimate that tolerance. The tolerance
  estimate uses the determined intensities and 4 consecutive scans for its test build.

## Benchmark baseline

2026-09-26, both builders with the preset tolerance of the wizard. ADAP / fast: median task time,
summed over the files of a data set, fast before the speed work of 2026-09-26 in brackets (same
machine, same day; timings vary by ~10% between runs). Found: resolved features of one list with
a feature of the other within the tolerance and 0.03 min. Dips: fillable dips of ADAP / fast.
Auto: estimate and its features relative to the best tolerance of the sweep (preset in brackets).
Found and dips include the coalesced fill (same day), found changed only for GC-EI-QTOF, on the
other data sets by less than 0.05 percentage points, times are from before it.

| data set                     | MS1 data points  | ADAP / fast ms       | ADAP found in fast | fast found in ADAP | dips     | Auto                           | best of sweep |
|------------------------------|------------------|----------------------|--------------------|--------------------|----------|--------------------------------|---------------|
| Orbitrap QE, sensitive       | 1.7 M            | 3443 / 353 (447)     | 98.40%             | 88.50%             | 408 / 49 | 14.5 ppm, 99.3% (99.9%)        | 10 ppm        |
| ... no noise filter          | 1.9 M            | 4205 / 377 (489)     | 98.32%             | 88.45%             | 408 / 48 | 13.8 ppm, 99.4% (99.9%)        | 10 ppm        |
| Orbitrap QE, workshop        | 0.82 M           | 1057 / 124 (183)     | 99.62%             | 95.25%             | 1 / 0    | 10.7 ppm, 99.8% (99.5%)        | 12 ppm        |
| ... no noise filter          | 1.9 M            | 1932 / 240 (393)     | 99.01%             | 93.75%             | 1 / 0    | 13.8 ppm, 100% (98.7%)         | 15 ppm        |
| Orbitrap QE media, sensitive | 0.95 M           | 1786 / 158 (220)     | 98.21%             | 84.79%             | 501 / 88 | 13.4 ppm, 99.6% (100%)         | 10 ppm        |
| ... no noise filter          | 1.0 M            | 1966 / 173 (246)     | 98.12%             | 84.70%             | 507 / 92 | 13.2 ppm, 99.6% (100%)         | 10 ppm        |
| Orbitrap QE media, workshop  | 0.46 M           | 479 / 63 (90)        | 99.43%             | 92.16%             | 25 / 5   | 9.9 ppm, 98.2% (98.2%)         | 30 ppm        |
| ... no noise filter          | 1.0 M            | 1002 / 125 (204)     | 99.23%             | 90.73%             | 25 / 5   | 12.8 ppm, 99.2% (98.3%)        | 20 ppm        |
| GC-EI-TOF                    | 0.15 M           | 81 / 20 (26)         | 100%               | 100%               | 0 / 0    | 21.1 ppm, 99.6% (99.4%)        | 15 ppm        |
| ... no noise filter          | 3.5 M            | 917 / 381 (931)      | 100%               | 100%               | 0 / 0    | 21.1 ppm, 99.8% (99.1%)        | 15 ppm        |
| GC-EI-QTOF                   | 2.0 M            | 3586 / 259 (327)     | 99.69%             | 97.43%             | 70 / 25  | 23.3 ppm, 99.7% (99.4%)        | 60 ppm        |
| ... no noise filter          | 36 M             | 19937 / 5419 (10701) | 99.28%             | 96.72%             | 70 / 24  | 23.3 ppm, 98.7% (98.6%)        | 60 ppm        |
| LC-QTOF ZenoTOF DDA          | 0.03 M           | 35 / 10 (10)         | 100%               | 97.04%             | 0 / 0    | 0.9 mDa/9.4 ppm, 96.7% (99.2%) | 60 ppm        |
| ... no noise filter          | 4.5 M, 1.5 M > 0 | 717 / 175 (382)      | 99.61%             | 96.25%             | 0 / 0    | 0.9 mDa/9.4 ppm, 93.7% (96.5%) | 60 ppm        |
| LC-QTOF MSe                  | 0.02 M           | 8 / 5 (5)            | 98.62%             | 94.70%             | 0 / 0    | 50.9 ppm, 100% (95.0%)         | 50.9 ppm      |
| ... no noise filter          | 0.30 M           | 32 / 23 (50)         | 98.05%             | 94.38%             | 0 / 0    | 50.9 ppm, 100% (92.5%)         | 50.9 ppm      |
| GC-Orbitrap                  | 2.7 M            | 2409 / 243 (389)     | 100%               | 100%               | 0 / 0    | 4.0 ppm, 100% (100%)           | 10 ppm        |
| ... no noise filter          | 4.5 M            | 3393 / 406 (718)     | 100%               | 100%               | 0 / 0    | 5.2 ppm, 100% (100%)           | 10 ppm        |
| DOM Orbitrap                 | 0.33 M           | 177 / 48 (65)        | 99.38%             | 99.69%             | 0 / 0    | 9.0 ppm, 99.4% (99.4%)         | 6 ppm         |
| ... no noise filter          | 0.36 M           | 180 / 51 (73)        | 99.39%             | 100%               | 0 / 0    | 9.0 ppm, 99.1% (99.1%)         | 6 ppm         |

- The fast lists have more features than the ADAP lists on the QE data, most unmatched fast
  features are peaks that ADAP has with more holes.
- Auto misses the plateau on the ZenoTOF DDA file: 0.9 mDa or 9.4 ppm reach 96.7% of the features
  of 60 ppm, the preset 20 ppm 99.2%, and the features grow up to the end of the sweep. At the
  estimate, 99% of the data points are within 8.2 ppm of their neighbors in the chromatogram, also
  below 1000 counts. Larger tolerances add mostly weak data points (below 2000 counts) that deviate
  by more than 9.4 ppm, 3% of those below 1000 counts by more than 20 ppm, likely noise neighbors
  that close the gaps of weak peaks in these sparse data (36 data points per scan above the noise
  level 500). The same happens on QE data at 60 ppm. The feature plateau may overrate large
  tolerances on sparse data, the calibration is unchanged until a ground truth decides it.
- Auto misses the plateau (99% of the best) on the QE media file with workshop settings: 9.9 ppm
  and the preset 10 ppm reach 98.2%, the features grow slowly up to 30 ppm while fewer of them
  match the preset features. The calibration is unchanged. On unfiltered GC-EI-QTOF data 20 to 40
  ppm give 98.6-99.1%, only 60 ppm is higher.
- Without noise filter, the QE workshop lists lose 0.6 percentage points of the ADAP features at
  the preset 10 ppm: short peaks at the min consecutive scans whose m/z scatters by about 10 ppm
  between scans are in no fast chromatogram. Auto raises the tolerance on these data (13.9 instead
  of 10.8 ppm), which reaches 100% of the best tolerance of the sweep.

## Final result baseline

2026-09-26, fast with the hole fill flanks of the min group intensity from 2026-09-28, preset
tolerance, resolved with the local minimum resolver, sums over the files of a data set, ADAP /
fast (`ChromatogramBuilderBenchmark`, summary at the top of the report). Split
peaks, truncated and affected features count intense fillable holes (see the decision above).
Intense holes inside a feature are 0-2 for both builders on all data sets. Foreign: features with
a data point of at least 10% of the height beyond the tolerance from the feature m/z.

| data set                    | split peaks | truncated | affected features | lost signal     | foreign m/z |
|-----------------------------|-------------|-----------|-------------------|-----------------|-------------|
| Orbitrap QE, sensitive      | 77 / 16     | 1236 / 188 | 1274 / 213       | 0.262% / 0.046% | 93 / 769    |
| Orbitrap QE, workshop       | 17 / 3      | 212 / 40  | 238 / 45          | 0.185% / 0.030% | 12 / 155    |
| Orbitrap QE media, sensitive | 36 / 21    | 641 / 140 | 651 / 176         | 0.248% / 0.037% | 57 / 340    |
| Orbitrap QE media, workshop | 6 / 3       | 126 / 20  | 130 / 26          | 0.192% / 0.017% | 15 / 87     |
| GC-EI-QTOF                  | 6 / 4       | 244 / 130 | 234 / 129         | 0.005% / 0.002% | 36 / 157    |
| ... no noise filter         | 24 / 17     | 970 / 726 | 894 / 689         | 0.007% / 0.002% | 181 / 296   |
| LC-QTOF ZenoTOF DDA         | 2 / 2       | 4 / 0     | 8 / 4             | 0.025% / 0.004% | 0 / 7       |
| LC-QTOF MSe                 | 0 / 0       | 7 / 5     | 7 / 5             | 0.007% / 0.004% | 2 / 5       |
| DOM Orbitrap                | 0 / 0       | 3 / 0     | 3 / 0             | 0.010% / 0.000% | 0 / 0       |

- GC-EI-TOF and GC-Orbitrap have no intense holes in either list. The unfiltered variants of the
  QE data sets behave like the filtered ones.
- The remaining holes of both builders are mostly the alternating pairs (e.g., 660.807/660.795 on
  QE, 201.094/201.102 on GC-EI-QTOF), counted for both. The fast builder leaves a few unused data
  points 1-2 tolerances away in holes longer than 3 scans (e.g., 447.2447 at -13 to -18 ppm on
  QE sensitive), the log of the benchmark lists them. On GC-EI-QTOF most unused fills are at the
  edges of weak features with flanks below the min group intensity (86 of 135), see the hole
  fill decision.
- More foreign data points and a wider m/z spread are the cost of the complementary joins, the
  hole fills and the shared centroids: p95 of the spread 3.7 instead of 3.0 ppm on QE sensitive,
  5.9 instead of 5.4 ppm on GC-EI-QTOF, medians within 0.1 ppm. Matched features have the same
  m/z in both lists (median difference 0.00 ppm, p95 at most 0.4 ppm).
- Synthetic ground truth: missed detectable ions ADAP / fast: saturated 80 / 6, isobaric pairs
  475 / 11, high m/z error 144 / 9, standard 12 / 0. The fast builder has more intense holes and
  contaminated features on isobaric pairs (449 / 135 vs 231 / 54): it finds the second ion of a
  co-eluting pair within the tolerance, which ADAP merges and misses. The m/z error of the found
  ions is the same (median 0.2-0.5 ppm), the p95 on saturated data 3.5 instead of 2.3 ppm (the
  bridged plateau keeps its shifted m/z). Coalescing pairs: 351 / 316 missed, both builders put
  the coalesced centroids mostly into one feature of the stronger ion, p95 m/z error 7.5 / 7.7
  ppm.

## Profile baseline

2026-09-26, `ChromatogramBuilderProfile`, first file of each data set, 5 timed runs: median time,
allocated MB and bytes per data point of one run, peak live heap (sampled). Builder alone / fast
file task / ADAP task.

| data set               | data points | time ms            | allocated MB      | bytes per data point | peak MB          |
|------------------------|-------------|--------------------|-------------------|----------------------|------------------|
| Orbitrap QE, sensitive | 0.84 M      | 103 / 207 / 1830   | 67 / 251 / 831    | 79 / 297 / 985       | 32 / 41 / 100    |
| GC-EI-QTOF             | 2.0 M       | 163 / 246 / 3325   | 149 / 315 / 481   | 74 / 157 / 240       | 60 / 82 / 211    |
| ... no noise filter    | 36 M        | 4943 / 5275 / 19249 | 1335 / 1996 / 3066 | 37 / 55 / 85       | 575 / 599 / 1700 |
| ZenoTOF, no noise filter | 4.5 M     | 171 / 178 / 695    | 46 / 62 / 224     | 10 / 14 / 50         | 13 / 21 / 186    |
| GC-Orbitrap            | 2.7 M       | 175 / 248 / 1845   | 89 / 174 / 305    | 33 / 63 / 111        | 47 / 53 / 150    |

- Scaling on synthetic data (1x to 8x scans and ions at a constant density, 0.4 to 3.3 M data
  points): builder 68 → 87 ns per data point, fast task 105 → 126, ADAP 304 → 1272 (3x the time
  per doubling). Allocation per data point of the builder stays at 20-27 bytes.
- On filtered data the feature creation of mzmine is half of the fast task: on QE sensitive
  `FastChromatogramBuilder.build` 49% of the samples, `createFeature` 28% (the quality
  parameters in `FeatureDataUtils.recalculateIonSeriesDependingTypes` 16%), row creation and
  `addRow` 16%, the sort 4%. The rows allocate more than the builder: `ModularFeatureListRow
  .getFeatures()` builds a stream and a list on each call and the row bindings of `addRow` call it
  repeatedly, ~48 MB per run for 9.7 k rows. ADAP has the same cost and spends 39% of its time and
  72% of its allocation in `FeatureConvertors.ADAPChromatogramToModularFeature`.
- Builder on the worst case (GC-EI-QTOF without noise filter): pass 1 43%, pass 2 26%,
  finalization 16%, consolidation 14%. Hot spots: `BinnedLowerBound.lowerBound` 18% of the
  samples, mostly the loose data points of pass 2 (`ChannelDataCollector.findFreeChannel`, 27.6 M
  loose data points) and the recovery; the channel buffers grow by copying (~280 MB per run) and
  are copied once more into the chromatograms (~170 MB); the collision events (~190 MB) and the
  recorded trace summaries (~200 MB) grow by doubling. Live heap at the end of pass 1 229 MB, at
  the end of pass 2 425 MB (channel buffers of 10 M data points), result 169 MB.

## Parameter estimation baseline

2026-10-01, `BuilderParameterEstimationBenchmark`, the QE "20 years" files were OneDrive
placeholders and skipped, the QE QC crops of the integration test stand in. Per sensitivity: min
consecutive / min group / min height, features after the local minimum resolver (its min height
and min scans from the run) and the share of the wizard features found within the preset tolerance
and 0.03 min. Time: estimation of Medium, the other sensitivities take the same.

| data set                     | N50 / N95   | wizard                   | Sensitive               | Medium                  | Abundant               | ms   |
|------------------------------|-------------|--------------------------|-------------------------|-------------------------|------------------------|------|
| QE QC, factor 2              | 1.2E4/5.1E4 | 4/1E4/5E4, 1122          | 3/5.8E3/2.5E4, 1431 100% | 3/1.2E4/5.1E4, 1115 99% | 4/1E5/5.1E5, 390 35%   | 332  |
| ... factor 5 (workshop)      | 2.5E4/8.5E4 | 6/1E5/5E5, 371           | 3/1.2E4/4.3E4, 768 100% | 3/2.5E4/8.5E4, 737 100% | 4/1.7E5/8.5E5, 280 75% | 138  |
| ... no filter, workshop wizard | 9.7E3/5.2E4 | 6/1E5/5E5, 382         | 3/4.8E3/2.6E4, 1443 100% | 3/9.7E3/5.2E4, 1118 100% | 4/1E5/5.2E5, 388 99%  | 312  |
| GC-EI-TOF                    | 600/1.2E3   | 4/1E3/1E3, 496           | 3/300/600, 512 100%     | 4/600/1.2E3, 511 100%   | 6/2.4E3/1.2E4, 177 36% | 239  |
| ... no noise filter          | 137/535     | 4/1E3/1E3, 544           | 3/68/270, 1094 100%     | 4/140/540, 772 100%     | 6/1.1E3/5.4E3, 287 53% | 2239 |
| GC-EI-QTOF                   | 604/1.3E3   | 4/1E3/1E3, 12705         | 3/300/660, 12906 99%    | 5/600/1.3E3, 12681 98%  | 8/2.6E3/1.3E4, 3967 31% | 816 |
| ... no noise filter          | 156/692     | 4/1E3/1E3, 15277         | 3/78/350, 24914 92%     | 5/160/690, 19608 98%    | 8/1.4E3/6.9E3, 5476 36% | 5289 |
| LC-QTOF ZenoTOF DDA          | 501/1.2E3   | 5/1E3/1E3, 711           | 3/250/590, 784 99%      | 3/500/1.2E3, 780 99%    | 4/2.4E3/1.2E4, 217 31% | 46   |
| ... no noise filter          | 11/128      | 5/1E3/1E3, 802           | 3/5.7/64, 6006 99%      | 3/11/130, 4245 100%     | 4/260/1.3E3, 928 95%   | 1630 |
| LC-QTOF MSe                  | 650/1.3E3   | 4/600/1E3, 154           | 3/320/660, 174 100%     | 3/650/1.3E3, 146 92%    | 4/2.6E3/1.3E4, 48 31%  | 26   |
| ... no noise filter          | 608/1.3E3   | 4/600/1E3, 163           | 3/300/660, 233 99%      | 3/610/1.3E3, 151 89%    | 4/2.6E3/1.3E4, 46 28%  | 203  |
| GC-Orbitrap                  | 1.2E3/2.5E3 | 4/5E3/5E4, 828           | 5/590/1.2E3, 6100 100%  | 8/1.2E3/2.5E3, 6008 100% | 12/4.9E3/2.5E4, 1459 100% | 851 |
| ... no noise filter          | 903/2.0E3   | 4/5E3/5E4, 828           | 4/450/990, 7406 100%    | 7/900/2.0E3, 7175 100%  | 11/3.9E3/2.0E4, 1738 100% | 1125 |
| DOM Orbitrap                 | 7.3E4/2.0E5 | 4/5E4/2E5, 320           | 3/3.6E4/9.8E4, 466 100% | 3/7.3E4/2.0E5, 404 98%  | 3/3.9E5/2.0E6, 101 31% | 384  |
| ... no noise filter          | 6.6E4/1.9E5 | 4/5E4/2E5, 322           | 3/3.3E4/9.5E4, 502 100% | 3/6.6E4/1.9E5, 426 98%  | 3/3.8E5/1.9E6, 104 32% | 430  |

- The GC-Orbitrap wizard settings are 10x above N95 (5E4 height), all sensitivities find all its
  features. On unfiltered ZenoTOF data the auto levels follow the persistent low level signals,
  Medium has 5x the features of the wizard with its noise filter of 500.
- The estimation is several passes, not one loop: over the sampled scan pairs (at most 1000 per
  sample file) the histogram, one scatter pass and fit per tried floor (1 on filtered data, up to
  6 without filter), the profile, the refinement scatter and profile; then the tolerance
  estimation with its own scatter floors and the test build on all selected scans of the 3 sample
  files. `BuilderParameterEstimate.Timings` logs the steps. Medium, 2026-10-01: QE QC 293 ms
  (scatter 78, refinement 70, tolerance 137 of which test build 45), GC-EI-QTOF 738 ms (test build
  194), unfiltered GC-EI-QTOF 4435 ms (scatter 1573, profile 233, refinement 474, tolerance 2059 of
  which test build 863). The scatter fits are 60-80% of the time, the test build 15-45%. The
  tolerance estimation repeats the floor 0 scatter of the near window step.
