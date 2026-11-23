# Mobility scan mass detection per frame

Status: the generic part is implemented on branch `frame-mass-detection-refactor` (based on master).
The saturation consolidation and the Agilent importer follow once the `agilent` branch is merged.
Background: Agilent IMS centroiding benchmark
(`mzmine-community/src/test/java/import_data/agilent_centroiding` on `agilent`, see the benchmark
documentation page).

## Intention

Saturated ions are distorted only in the apex mobility scans of their drift peak. The benchmark
showed that the m/z combined from the unsaturated flank scans of the same ion (intensity weighted,
scans below 0.5 x file maximum) is the most accurate estimate for saturated ions (median 1.0 ppm vs.
2.4 ppm adaptive local max on frames, 3.7 ppm on mobility scans). To apply this, the mass detector
must see all mobility scans of a frame at once. The mass detection task and the advanced import must
produce identical results, so the logic lives in the mass detector.

## Current behaviour

- **Frames take the normal scan route** (`getMassValues` / `processScan`). Only the mobility scans
  of a frame are detected together. In the IMS workflow the feature m/z comes from the mobility scan
  mass lists (the IMS expander collects mobility scan mass list points within its m/z tolerance
  around the row m/z and recalculates the feature m/z), so frame mass lists are not corrected.
- **`MassDetector.getMobilityScanMassValues(List<? extends MassSpectrum>)`** returns one
  `[mzs, intensities]` per mobility scan in input order. The default detects every scan
  independently with `getMassValues(MassSpectrum)`, so all existing detectors behave as before.
- **Mass detection task** is unchanged and gets no `MsProcessorList`.
  `MobilityScanStorage.generateAndAddMobilityScanMassLists` calls the new method once per frame (raw
  data shortcut for detectors without active filters kept, MSn denormalization afterwards).
- **Import:** `MsProcessor.processMobilityScans(metadataOnlyScan, List<SimpleSpectralArrays>)`
  runs `processScan` per scan by default and is chained by `MsProcessorList`.
  `MassDetectorMsProcessor` overrides it: MS1/MS2 detector by the metadata MS level, spectrum type
  read once before the shared metadata is set to centroided, arrays wrapped as `SimpleMassSpectrum`.
  The result count and order are a documented contract, not checked at runtime.
- **Importers** call `processMobilityScans` once per frame:
    - `TDFUtils.loadSpectraForTIMSFrame`
    - `MassLynxDataAccess.readMobilityScansForFrame` (optional mzmine centroider still per scan,
      before the processors)
    - mzML merged mobility scans (`loadProccessMemMapMzDataForMergedMobilityScan`) and individual
      mobility scans. Individual scans are now processed when the frame is complete
      (`MzMLParser.Vars.memoryMapAndClearFrameMobilityScanData` ->
      `BuildingMzMLMsScan.processMobilityScansOfFrame`) instead of while parsing.
      Assumption: the first mobility scan's metadata (MS level, spectrum type, injection time)
      represents the whole frame.
- `MassDetectionParameters.create(...)` was added next to `AdvancedSpectraImportParameters.create`.

## Bug fixes found on the way

- `FactorOfLowestMassDetector`: the array version (used by the advanced import) counted zero
  intensities as the lowest signal, so the noise level was 0 and nothing was filtered (e.g., profile
  imzML). It now ignores zeros like the spectrum version used by the task.
- `DenormalizeInjectTimeMsProcessor`: also multiplied MS1 scans by the injection time; now MSn only,
  like the task and the parameter description.
- Merged mzML mobility scans: after the first scan, mass detection had set the shared metadata to
  centroided, so the remaining profile scans were detected as centroid data.
- TDF: mobility scans were dropped if the processor list was empty (not reachable in practice).

## Tests

- `AdvancedImportMassDetectionEqualityTest`: advanced import vs. plain import + mass detection task,
  exact mass lists of scans, frames and mobility scans for `DOM_a.mzML`, `orbi_idx_msn.mzML` (with
  MSn denormalization) and `lc-tims-ms-pasef-a.d`. Fails if mobility scan detection in the import is
  skipped.
- `MobilityScanProcessingTest`: processor chain with shared metadata equals per scan processing;
  default detector method; factor of lowest overloads.
- `DenormalizeInjectTimeMsProcessorTest`: MS1 scans stay unchanged.
- Regenerated expected statistics (advanced entries only): `ImzMLImportTest` (factor of lowest),
  `MzMLImportTest` and `TimsTofImportTest` (MS1 intensities, MSn unchanged).

## Known limitations

- Profile IMS data (MassLynx, individual mzML mobility scans): the raw data of one frame stays in
  memory until the frame is processed (~160 MB for 200 drift bins x 50k points, per file imported in
  parallel).
- The advanced import uses `getMassValues(double[], double[], type)` for frames and scans and
  `getMassValues(MassSpectrum)` for mobility scans. `RECURSIVE` and `WAVELET` do not implement the
  array version, so they fail on frames and scans but work on mobility scans.
- Workflows that end before the IMS expander keep the frame m/z of saturated ions from the regular
  detection.
- Not verified locally: MassLynx IMS (tests return early without the local data), merged mzML
  mobility scans (no test file), `ThermoRawImportTest` statistics (skipped).

## Planned: saturation consolidation (after `agilent` is merged)

`AdaptiveLocalMaxMassDetector.getMobilityScanMassValues`, only active if a saturation threshold is
set:

1. Centroid every mobility scan as today (incl. the EMG saturation branch).
2. For each centroid whose raw apex height reaches the threshold, follow the same ion through the
   neighbouring mobility scans (m/z within ~10 ppm, stop at a gap) to get its drift peak.
3. m/z = intensity-weighted mean of the members below the threshold and above 2 % of the drift peak
   maximum (same as `MobilogramCentroider` in the benchmark).
4. Replace the m/z of the saturated members only; without usable unsaturated members the EMG result
   stays.

- Compare the raw apex height inside the detector, never the reported intensity (an area with
  `AbundanceMeasure.Area`).
- Conformers of the same ion are merged (same exact m/z). Different ions within the tolerance would
  be mixed; keep the tolerance tight and split groups at intensity valleys.

## Decided: saturation threshold (2026-10-16)

- **Threshold = 0.5 x abundance limit of the mobility scans**, read from the raw file. Agilent
  stores
  `NumTransients` and `FrameSpecAbundLimit` per frame method (`AcqData/IMSFrameMeth.xml`) and per
  frame record (`IMSFrame.bin`, SDK `IImsFrameRecord.NumTransients`,
  `IImsFrameInfo.FrameSpecAbundanceLimit`); the limit = transients x per-transient limit (3072
  counts
  native, 24576 for demultiplexed files). The bridge now reports `numTransients` and
  `frameSpecAbundLimit` per IMS frame and `abundLimit` per non-IMS scan.
- **Mobility scans only.** The limit is not defined for the total frame spectrum, frames get no EMG.
- Same rule for multiplexed/demultiplexed files: their data never got near the reported limit (max
  0.42 x limit) and showed no isotope ratio distortion, so the EMG never fires there. Whether the
  reported demultiplexed limit is the true onset could not be verified (the demultiplexed tune mix
  file of MSV000096126 has no signal).
- Benchmark (mobility scans, `20261016_121959`): median |ppm| saturated targets 2.28 at 0.5 x limit
  (0.4 x limit 2.19, old 0.5 x observed file maximum 2.53, no EMG 3.70); all targets 2.09 (0.4:
  2.06,
  old 2.26). 0.5 was chosen as the more conservative value: fewer EMG fits on unsaturated peaks (15
  vs 22 asymmetric), paired comparison 82 vs 70 targets closer than 0.4, and no extra low bias. The
  limit-based threshold no longer fits unsaturated peaks of demultiplexed and weak files (the old
  relative threshold made C1.d.DeMP.d worse, 1.84 -> 2.78 ppm).
- Known metadata problem: the PNNL PreProcessor output `_MA-d3-c3-Min20_SR.d` keeps 17 transients
  and 52,224 in `IMSFrameMeth.xml` while its intensities reach 342k (the `_MA-d3-csum` output
  updates
  the transients). The EMG then also fits unsaturated peaks of such files (24 of 159 targets, median
  unchanged in the benchmark). The per-frame records may hold the corrected value; check through the
  bridge.

## Implemented: saturation threshold in production (2026-10-16)

- **Raw file metadata** `MobilityScanAbundanceLimitType` ("Mobility scan abundance limit", value
  record `MobilityScanAbundanceLimits(ms1, ms2)`, NaN = unknown). The Agilent reader sets it from
  the first frame of each ms level (bridge field `mobilityScanAbundLimit`). No frame abundance limit
  (not reported by the raw data) and no non-IMS scan limit yet.
- **Detector parameters**: `AdaptiveLocalMaxMassDetector` has its own parameter set
  `AdaptiveLocalMaxMassDetectorParameters` (local max parameters + `SaturationThresholdParameter`,
  a combo with input): Off (default, old batches load with it), Manual (raw intensity), Raw file
  metadata (0.5 x limit of the frame's ms level), Auto (0.5 x saturation plateau, see below). The
  threshold is resolved per frame in `getMobilityScanMassValues` and only applied to mobility
  scans, never to frames or other scans. Metadata and Auto need the raw file (mobility scans of a
  file); without it they log a warning and keep saturation handling off.
- **Advanced import** uses `AdaptiveLocalMaxImportParameters` (only Off and Manual), injected via
  `ModuleOptionsEnumComboParameter.replaceOptionParameters`. Values with hidden options are reset to
  Off.
- **Agilent auto-centroiding** (`AGILENT_READER_AUTO_CENTROID`): `frameCentroider` without
  threshold for frames, one mobility scan centroider per ms level with 0.5 x the limit of the first
  frame of this ms level. Both are adaptive local max with Gaussian (7) smoothing (decision: the
  previous centroider was local max with the same smoothing). Mobility scans of a frame are
  centroided and processed together (`getMobilityScanMassValues`, `processMobilityScans`).
- **TODOs (separate PR)** in `AgilentDataAccess`: warning when the limit changes within the frames
  of an ms level (first frame's limit is used), and the guard for mobility scan intensities above
  the reported limit (wrong metadata, e.g. PreProcessor `c3` output) is in place but does nothing
  yet.
- **Auto (saturation plateau)**, `SaturationPlateauEstimator`: highest mobility scan base peak of
  the ms level is the plateau if at least 10 base peaks are within 1 % of it. Checked on all frames
  of the benchmark files: native saturated files 28-614 scans on the plateau at 0.95-0.96 x limit
  (detected in all), unsaturated/weak files 1-4 scans (not detected). Borderline: demultiplexed C2M
  and C4M reach 256.5k / 259.6k (0.58 x reported limit) with 9 and 8 scans within 1 %, and the
  PreProcessor `c3` MS2 frames hit exactly 10. The similar maxima of C2M and C4M hint at a real
  ceiling of demultiplexed data near 0.58 x the reported limit (unconfirmed).

## Implemented: mobility flank consolidation (2026-10-16)

- `MobilityFlankConsolidator` (untargeted): every centroid with apex height >= threshold seeds a
  group, the group follows the nearest centroid within `FLANK_TOLERANCE` (0.01 Da) into the
  neighbouring mobility scans (one missing scan bridged), m/z = height weighted mean of the members
  below the threshold and >= 2 % of the group maximum; only saturated members are replaced, groups
  without usable flanks keep the EMG m/z. `AdaptiveLocalMaxMassDetector.getMobilityScanMassValues`
  runs it whenever a saturation threshold applies (manual, metadata, auto, or the fixed threshold
  of the Agilent mobility scan centroiders).
- Benchmark (`20261016_154531`, mobility scans, threshold 0.5 x abundance limit, all through
  `getMobilityScanMassValues`): saturated targets median / p95 / >5 ppm / >10 ppm: production
  (adaptive + EMG + flank) 1.31 / 5.76 / 13 / 4, adaptive + EMG 2.28 / 7.96 / 32 / 4, adaptive
  without saturation handling 3.70 / 8.51 / 53 / 4, local max 7.05 / 18.99 / 111 / 68, targeted
  mobility edge reference 1.00 / 5.06 / 10 / 3. Paired against adaptive + EMG: 92 targets closer, 38
  farther, only 6 farther by > 2 ppm (max 4.2 ppm). Unsaturated classes unchanged; the remaining
  > 10 ppm outliers are the same with and without saturation handling (mostly weak signals).
- Tolerance (0.005 / 0.01 / 0.02 Da, 10 ppm) makes no difference with the adaptive base. With a
  local max base the saturated seeds read up to ~20 ppm off, small tolerances fail to link them (>
  10 ppm outliers: 37 at 0.005 Da, 14 at 0.01 Da, 10 at 0.02 Da) and unsaturated peaks keep the
  worse local max m/z (183 targets > 2 ppm worse than adaptive + EMG). Decision: adaptive base.
- Speed (`MassDetectorSpeedTest`, mobility scans of all indexed frames): production path 1.12-1.14 x
  local max on the LC files (per-scan adaptive + EMG 1.08-1.10 x).

## Implemented: saturation threshold for frames and scans (2026-10-26)

- The saturation threshold parameter ("Saturation threshold", formerly "... (mobility scans)")
  now applies to all spectra. `AdaptiveLocalMaxMassDetector.resolveThreshold` resolves it per
  spectrum type (mobility scans vs. frames and other scans), ms level and file, cached per file.
  The mass detection task passes a `ScanDataAccess`, its current scan carries the file.
- Manual: the same raw value for all spectra. Different values for frames and mobility scans need
  separate mass detection steps with the scan type filter (decision: no per-type manual values).
- Raw file metadata (else auto): the mobility scan abundance limit for mobility scans, auto if the
  file has no limit and always for frames and other scans (decision: no new metadata for non-IMS
  scans, the limit of the Agilent readers stays an import-only input).
- Auto: `SaturationPlateauEstimator.estimateMobilityScanPlateau` (mobility scans) or
  `estimateScanPlateau` (`file.getScans()` of non-IMS files), same 10 scans within 1 % rule.
  First implemented with the frames like non-IMS scans, replaced by the frame rule below because
  frames show no plateau.
- Plateau on the benchmark data (`SaturationPlateauProbeTest`, all frames, profile): no frame
  plateau in any file (1-7 frames within 1 % of the frame maximum, Agilent tune mix raw 9 of 123).
  Mobility scan plateaus as before in the native saturated files (28-614 scans, 0.95-0.96 x
  limit) and none in the weak / demultiplexed files (C2M 9, C4M 8). So auto never enables the EMG
  on frames of these files; frames of the native saturated files reach 0.4-3.5 M (vs. 52k
  mobility scan limit).
- Frame EMG with a relative threshold (`adaptive_local_max_emg_f0.5`, 0.5 x frame maximum of the
  file, benchmark `20261026_095217`): saturated targets median 2.38 -> 2.03 ppm, p95 5.65 -> 5.15
  ppm vs. adaptive without EMG; asymmetric 1.60 -> 1.66 ppm, all 2.00 -> 1.99 ppm.
- Benchmark `20261026_095217` with the changed detector: identical to `20261016_154531` (all
  configs use the fixed constructor threshold).
- **Frame rule (decision, replaces the frame plateau):** with raw file metadata and auto, frames
  count as saturated if the mobility scans of the same file and ms level have a saturation plateau
  (`estimateMobilityScanPlateau`, also if the metadata has a limit). Threshold = 0.5 x highest frame
  base peak of the ms level (`SaturationPlateauEstimator.maxScanBasePeak`), otherwise off. Offline
  estimate from `20261026_095217` (f0.5 frame EMG only in files with an MS1 mobility scan
  plateau, adaptive without EMG elsewhere): saturated median / p95 2.41 / 5.64 -> 2.04 / 5.08
  ppm (ungated 2.10 / 5.08), asymmetric 1.61 -> 1.62, all 2.01 -> 1.95. The benchmark maximum
  is taken over the indexed frames only, not all frames.
- Spectra without raw file (import arrays): only Off and Manual, as before.
- Negative controls (Bruker timsTOF urine, SCIEX ZenoTOF plasma, Waters DDA, profile import): one
  scan base peak within 1 % of the maximum per MS level, no plateau, no saturation handling.

Remaining work:

- Separate PR: the two Agilent reader TODOs (limit change warning, wrong-metadata guard action).
- Auto plateau: tune `MIN_PLATEAU_SCANS` / `PLATEAU_TOLERANCE` on more files (borderline cases
  above); confirm the true saturation level of demultiplexed data.
- The MassLynx `mzmineCentroider` still runs per spectrum.
- `MsDataImportAndMassDetectWrapperTask.applyMassDetection` only processes frames; needs the
  mobility scan method if IMS files pass through it with mass detection.
- Verification: synthetic frame test (clipped apex scans get the flank m/z, unsaturated ions and
  default detectors unchanged); rerun `AgilentCentroidingAccuracyTest`, mobility scan results should
  match `mobilogram_flank_adaptive_local_max_none_f0.5`.
