/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.featdet_massdetection.local_max;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.ScanDataAccess;
import io.github.mzmine.datamodel.features.rawfiletypes.MobilityScanAbundanceLimitType;
import io.github.mzmine.datamodel.features.rawfiletypes.MobilityScanAbundanceLimits;
import io.github.mzmine.datamodel.impl.SimpleMassSpectrum;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectorPreprocessor;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.PreprocessedIntensitiesProvider;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Variant of {@link LocalMaxMassDetector} with an adaptive peak m/z, see {@link AdaptivePeakMz}.
 * Detection (maxima, edges, intensities) is the one of the {@link LocalMaxMassDetector}. This class
 * resolves the saturation threshold per spectrum and corrects saturated mobility scan centroids
 * with the neighbouring mobility scans, see {@link MobilityFlankConsolidator}.
 */
public class AdaptiveLocalMaxMassDetector implements MassDetector, PreprocessedIntensitiesProvider {

  public static final String NAME = "Adaptive local maximum mass detector";
  private static final Logger logger = Logger.getLogger(
      AdaptiveLocalMaxMassDetector.class.getName());

  /**
   * Saturation threshold as fraction of the abundance limit (raw file metadata) or of the
   * saturation plateau (auto). Benchmarked on Agilent 6560 data, isotope ratios distort from
   * ~0.4-0.5 x limit.
   */
  public static final double SATURATION_LIMIT_FRACTION = 0.5;
  /**
   * m/z tolerance to follow a saturated ion into the neighbouring mobility scans, see
   * {@link MobilityFlankConsolidator}
   */
  // decision: absolute tolerance, compared with ppm based tolerances in the Agilent benchmark
  public static final MZTolerance FLANK_TOLERANCE = new MZTolerance(0.01, 0);

  /**
   * Peak detection and the weighted centroid, also holds the preview series
   */
  private final @NotNull LocalMaxMassDetector localMax;

  /**
   * Raw intensity from which on profile points are distorted by detector saturation. Peaks reaching
   * it get the EMG fit. {@link Double#POSITIVE_INFINITY} disables the saturation branch. Used if
   * {@link #saturation} is off.
   */
  private final double saturationThreshold;

  /**
   * Source of the saturation threshold from the parameters, resolved per spectrum type (mobility
   * scans or frames and other scans) and ms level, see {@link #resolveThreshold(MassSpectrum)}.
   * Overrides {@link #saturationThreshold} unless off.
   */
  private final @NotNull SaturationThresholdValue saturation;

  /**
   * Raw file and its thresholds from the metadata or auto-derived, see
   * {@link SaturationPlateauEstimator}. Guarded by {@link #resolveThreshold(MassSpectrum)}.
   */
  private @Nullable RawDataFile thresholdFile;
  private final @NotNull Map<ThresholdKey, Double> fileThresholds = new HashMap<>();
  // mobility scan saturation plateau per ms level, also decides if frames are saturated
  private final @NotNull Map<Integer, Double> mobilityScanPlateaus = new HashMap<>();
  private boolean warnedMissingRawFile;

  public AdaptiveLocalMaxMassDetector() {
    this(0, AbundanceMeasure.Height, 3, new LocalMaxNoSmoothingModule());
  }

  public AdaptiveLocalMaxMassDetector(final double noiseLevel,
      final @NotNull AbundanceMeasure intensityCalculation, final int minNonZeroDp) {
    this(noiseLevel, intensityCalculation, minNonZeroDp, new LocalMaxNoSmoothingModule());
  }

  public AdaptiveLocalMaxMassDetector(final double noiseLevel,
      final @NotNull AbundanceMeasure intensityCalculation, final int minNonZeroDp,
      final @NotNull MassDetectorPreprocessor preprocessor) {
    this(noiseLevel, intensityCalculation, minNonZeroDp, preprocessor, Double.POSITIVE_INFINITY);
  }

  /**
   * @param saturationThreshold raw intensity from which on peaks are treated as saturated and get
   *                            the EMG fit, {@link Double#POSITIVE_INFINITY} to disable
   */
  public AdaptiveLocalMaxMassDetector(final double noiseLevel,
      final @NotNull AbundanceMeasure intensityCalculation, final int minNonZeroDp,
      final @NotNull MassDetectorPreprocessor preprocessor, final double saturationThreshold) {
    this(noiseLevel, intensityCalculation, minNonZeroDp, preprocessor, saturationThreshold,
        SaturationThresholdValue.OFF);
  }

  /**
   * @param saturationThreshold raw intensity from which on peaks of all spectra are treated as
   *                            saturated if saturation is off, {@link Double#POSITIVE_INFINITY} to
   *                            disable
   * @param saturation          saturation threshold source, see
   *                            {@link #resolveThreshold(MassSpectrum)}
   */
  public AdaptiveLocalMaxMassDetector(final double noiseLevel,
      final @NotNull AbundanceMeasure intensityCalculation, final int minNonZeroDp,
      final @NotNull MassDetectorPreprocessor preprocessor, final double saturationThreshold,
      final @NotNull SaturationThresholdValue saturation) {
    this.localMax = new LocalMaxMassDetector(noiseLevel, intensityCalculation, minNonZeroDp,
        preprocessor);
    this.saturationThreshold = saturationThreshold;
    this.saturation = saturation;
  }

  @Override
  public MassDetector create(final ParameterSet params) {
    final LocalMaxSmoothingOptions smoothingOption = params.getValue(
        AdaptiveLocalMaxMassDetectorParameters.smoothing);
    final ParameterSet smoothingParams = params.getEmbeddedParameterValue(
        AdaptiveLocalMaxMassDetectorParameters.smoothing);
    final MassDetectorPreprocessor preprocessor = smoothingOption.createPreprocessor(
        smoothingParams);

    return new AdaptiveLocalMaxMassDetector(
        params.getValue(AdaptiveLocalMaxMassDetectorParameters.noiseLevel),
        params.getValue(AdaptiveLocalMaxMassDetectorParameters.intensityCalculation),
        params.getValue(AdaptiveLocalMaxMassDetectorParameters.minNumberOfDp), preprocessor,
        Double.POSITIVE_INFINITY,
        params.getValue(AdaptiveLocalMaxMassDetectorParameters.saturationThreshold));
  }

  /**
   * Centroids the mobility scans of one frame. If a saturation threshold applies, the m/z of
   * saturated centroids is corrected with the unsaturated flanks of the same ion in the
   * neighbouring mobility scans, see {@link MobilityFlankConsolidator}.
   */
  @Override
  public @NotNull List<double[][]> getMobilityScanMassValues(
      @NotNull final List<? extends MassSpectrum> mobilityScans) {
    final List<double[][]> values = new ArrayList<>(mobilityScans.size());
    if (mobilityScans.isEmpty()) {
      return values;
    }
    // all mobility scans of a frame share the file and ms level
    final double threshold = resolveThreshold(mobilityScans.getFirst());
    final AdaptivePeakMz peakMz = new AdaptivePeakMz(threshold);
    if (Double.isInfinite(threshold)) {
      for (final MassSpectrum mobilityScan : mobilityScans) {
        values.add(localMax.centroid(mobilityScan, peakMz, null));
      }
      return values;
    }

    // saturated centroids (EMG mode) are corrected with the unsaturated flanks of the same ion
    final List<double[]> mzs = new ArrayList<>(mobilityScans.size());
    final List<double[]> heights = new ArrayList<>(mobilityScans.size());
    for (final MassSpectrum mobilityScan : mobilityScans) {
      final List<LocalMaxPeak> peaks = new ArrayList<>();
      final double[][] scanValues = localMax.centroid(mobilityScan, peakMz, peaks);
      values.add(scanValues);
      mzs.add(scanValues[0]);
      heights.add(peaks.stream().mapToDouble(LocalMaxPeak::height).toArray());
    }
    final List<double[]> corrected = MobilityFlankConsolidator.consolidate(mzs, heights, threshold,
        FLANK_TOLERANCE);
    for (int i = 0; i < corrected.size(); i++) {
      if (corrected.get(i) != mzs.get(i)) {
        values.set(i, sortByMz(corrected.get(i), values.get(i)[1]));
      }
    }
    return values;
  }

  /**
   * @return [mzs, intensities] sorted by m/z, the input arrays if already sorted
   */
  private static double @NotNull [][] sortByMz(final double @NotNull [] mzs,
      final double @NotNull [] intensities) {
    if (MobilityFlankConsolidator.isSorted(mzs)) {
      return new double[][]{mzs, intensities};
    }
    final Integer[] order = new Integer[mzs.length];
    for (int i = 0; i < order.length; i++) {
      order[i] = i;
    }
    Arrays.sort(order, (a, b) -> Double.compare(mzs[a], mzs[b]));
    final double[] sortedMzs = new double[mzs.length];
    final double[] sortedIntensities = new double[mzs.length];
    for (int i = 0; i < order.length; i++) {
      sortedMzs[i] = mzs[order[i]];
      sortedIntensities[i] = intensities[order[i]];
    }
    return new double[][]{sortedMzs, sortedIntensities};
  }

  /**
   * Saturation threshold of a spectrum:
   * <ul>
   *   <li>Off: the fixed {@link #saturationThreshold} (infinity unless set by the constructor).</li>
   *   <li>Manual: the entered raw intensity for all spectra.</li>
   *   <li>Raw file metadata: {@link #SATURATION_LIMIT_FRACTION} x the mobility scan abundance limit
   *   for mobility scans, auto if the file has no limit and for frames and other scans.</li>
   *   <li>Auto: {@link #SATURATION_LIMIT_FRACTION} x the saturation plateau of the base peaks of the
   *   mobility scans or the non-IMS scans of the ms level. Frames: see
   *   {@link #frameThreshold(IMSRawDataFile, int)}.</li>
   * </ul>
   * Raw file metadata and auto need the raw data file. Spectra without one (e.g., arrays during the
   * import) get no saturation handling. The file thresholds are cached per file, synchronized
   * because the import may share one detector between threads.
   *
   * @return the threshold, {@link Double#POSITIVE_INFINITY} for no saturation handling
   */
  private synchronized double resolveThreshold(@NotNull final MassSpectrum spectrum) {
    final SaturationThresholdOption option = saturation.option();
    if (option == SaturationThresholdOption.OFF) {
      return saturationThreshold;
    }
    if (option == SaturationThresholdOption.MANUAL) {
      return saturation.manual() > 0 ? saturation.manual() : Double.POSITIVE_INFINITY;
    }

    // the mass detection task passes the data access, the current scan carries the file
    final MassSpectrum unwrapped =
        spectrum instanceof ScanDataAccess access ? access.getCurrentScan() : spectrum;
    if (!(unwrapped instanceof Scan scan) || scan.getDataFile() == null) {
      if (!warnedMissingRawFile) {
        warnedMissingRawFile = true;
        logger.warning(
            "Saturation threshold option %s needs the raw data file, saturation handling is off".formatted(
                option));
      }
      return Double.POSITIVE_INFINITY;
    }

    final RawDataFile file = scan.getDataFile();
    if (thresholdFile != file) {
      thresholdFile = file;
      fileThresholds.clear();
      mobilityScanPlateaus.clear();
    }
    return fileThresholds.computeIfAbsent(
        new ThresholdKey(scan instanceof MobilityScan, scan.getMSLevel()),
        key -> fileThreshold(file, key, option));
  }

  /**
   * @return the threshold from the raw file metadata (mobility scans only), the saturation plateau
   * or the frame rule, infinity if none applies
   */
  private double fileThreshold(@NotNull final RawDataFile file, @NotNull final ThresholdKey key,
      @NotNull final SaturationThresholdOption option) {
    if (!key.mobilityScans() && file instanceof IMSRawDataFile imsFile) {
      return frameThreshold(imsFile, key.msLevel());
    }

    final String spectra = key.mobilityScans() ? "mobility scans" : "scans";
    if (option == SaturationThresholdOption.RAW_FILE_METADATA && key.mobilityScans()) {
      final MobilityScanAbundanceLimits limits = file.getFileMetadata()
          .get(MobilityScanAbundanceLimitType.class);
      final double limit = limits == null ? Double.NaN : limits.forMsLevel(key.msLevel());
      if (!Double.isNaN(limit)) {
        return SATURATION_LIMIT_FRACTION * limit;
      }
      logger.info("No abundance limit in the metadata of %s for MS%d %s, using auto".formatted(
          file.getName(), key.msLevel(), spectra));
    }

    final double plateau =
        key.mobilityScans() && file instanceof IMSRawDataFile imsFile ? mobilityScanPlateau(imsFile,
            key.msLevel()) : SaturationPlateauEstimator.estimateScanPlateau(file, key.msLevel());
    final double threshold =
        Double.isNaN(plateau) ? Double.POSITIVE_INFINITY : SATURATION_LIMIT_FRACTION * plateau;
    logger.info(
        "Auto saturation threshold of %s MS%d %s: %.0f (plateau %.0f)".formatted(file.getName(),
            key.msLevel(), spectra, threshold, plateau));
    return threshold;
  }

  /**
   * Frames sum the mobility scans and show no saturation plateau of their own. They count as
   * saturated if the mobility scans of the same ms level have a saturation plateau, the threshold
   * is then {@link #SATURATION_LIMIT_FRACTION} x the highest frame base peak of the ms level. Same
   * rule for raw file metadata, auto and the Agilent import.
   *
   * @param mobilityScanPlateau  saturation plateau of the mobility scan base peaks of the ms level
   *                             (see {@link SaturationPlateauEstimator#estimatePlateau(double[])}),
   *                             NaN if there is none
   * @param highestFrameBasePeak highest frame base peak of the ms level
   * @return the frame saturation threshold, infinity if the frames are not saturated
   */
  // decision: gated relative threshold, benchmark 20261026_095217 saturated frame targets median
  // 2.41 -> 2.04 ppm, the ungated relative threshold hurt unsaturated files
  public static double frameSaturationThreshold(final double mobilityScanPlateau,
      final double highestFrameBasePeak) {
    return Double.isNaN(mobilityScanPlateau) || !(highestFrameBasePeak > 0)
        ? Double.POSITIVE_INFINITY : SATURATION_LIMIT_FRACTION * highestFrameBasePeak;
  }

  private double frameThreshold(@NotNull final IMSRawDataFile file, final int msLevel) {
    if (Double.isNaN(mobilityScanPlateau(file, msLevel))) {
      logger.info(
          "No mobility scan saturation plateau in %s MS%d, frames are not saturated".formatted(
              file.getName(), msLevel));
      return Double.POSITIVE_INFINITY;
    }
    final double maxBasePeak = SaturationPlateauEstimator.maxScanBasePeak(file, msLevel);
    final double threshold = frameSaturationThreshold(mobilityScanPlateau(file, msLevel),
        maxBasePeak);
    logger.info(
        "Saturation threshold of %s MS%d frames: %.0f (highest frame base peak %.0f)".formatted(
            file.getName(), msLevel, threshold, maxBasePeak));
    return threshold;
  }

  /**
   * Takes over the file thresholds of a previous detector, so a new detector with changed
   * parameters (e.g., in the preview) does not scan the whole raw file again. The saturation
   * plateaus only depend on the data, the file thresholds also on the saturation threshold option.
   * Waits until the previous detector finished resolving its thresholds.
   *
   * @param previous detector of the same raw file, a different file is ignored on the next spectrum
   */
  public void takeOverFileCache(@NotNull final AdaptiveLocalMaxMassDetector previous) {
    // decision: lock order is always new -> previous, so a chain of preview detectors waits for
    // the older scan instead of starting a parallel one
    synchronized (this) {
      synchronized (previous) {
        if (previous.thresholdFile == null) {
          return;
        }
        thresholdFile = previous.thresholdFile;
        fileThresholds.clear();
        mobilityScanPlateaus.clear();
        mobilityScanPlateaus.putAll(previous.mobilityScanPlateaus);
        if (previous.saturation.option() == saturation.option()) {
          fileThresholds.putAll(previous.fileThresholds);
        }
      }
    }
  }

  /**
   * @return the saturation plateau of the mobility scans of the ms level or NaN, cached per file
   */
  private double mobilityScanPlateau(@NotNull final IMSRawDataFile file, final int msLevel) {
    return mobilityScanPlateaus.computeIfAbsent(msLevel,
        level -> SaturationPlateauEstimator.estimateMobilityScanPlateau(file, level));
  }

  /**
   * Thresholds are resolved per spectrum type and ms level of a file
   *
   * @param mobilityScans mobility scans or frames and other scans
   */
  private record ThresholdKey(boolean mobilityScans, int msLevel) {

  }

  @Override
  public boolean filtersActive() {
    return true;
  }

  @Override
  public double[][] getMassValues(final MassSpectrum spectrum) {
    return localMax.centroid(spectrum, new AdaptivePeakMz(resolveThreshold(spectrum)), null);
  }

  /**
   * Detects peaks like {@link #getMassValues(MassSpectrum)} but returns rich descriptors (apex,
   * edges, centroid m/z and height) instead of only the centroid arrays. Intended for
   * analysis/diagnostics, not the hot import path.
   *
   * @return one peak per centroid of {@link #getMassValues(MassSpectrum)}
   */
  public @NotNull List<LocalMaxPeak> detectPeaks(@NotNull final MassSpectrum spectrum) {
    final List<LocalMaxPeak> peaks = new ArrayList<>();
    localMax.centroid(spectrum, new AdaptivePeakMz(resolveThreshold(spectrum)), peaks);
    return peaks;
  }

  @Override
  public @NotNull String getName() {
    return NAME;
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return AdaptiveLocalMaxMassDetectorParameters.class;
  }

  @Override
  public double[][] getMassValues(final double @NotNull [] mzs, final double @NotNull [] intensities,
      @NotNull final MassSpectrumType type) {
    return getMassValues(new SimpleMassSpectrum(mzs, intensities, type));
  }

  @Override
  public double @Nullable [] getLastPreprocessedIntensities() {
    return localMax.getLastPreprocessedIntensities();
  }
}
