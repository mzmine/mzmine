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
import io.github.mzmine.datamodel.SimpleRange.SimpleIntegerRange;
import io.github.mzmine.datamodel.data_access.ScanDataAccess;
import io.github.mzmine.datamodel.features.rawfiletypes.MobilityScanAbundanceLimitType;
import io.github.mzmine.datamodel.features.rawfiletypes.MobilityScanAbundanceLimits;
import io.github.mzmine.datamodel.impl.SimpleMassSpectrum;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectorPreprocessor;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.PreprocessedIntensitiesProvider;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.collections.IndexRange;
import io.github.mzmine.util.maths.Weighting;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Experimental variant of {@link LocalMaxMassDetector} with an adaptive centroid m/z calculation,
 * kept separate so it can be profiled against the baseline. Detection (maxima, edges) is identical;
 * only the per-peak m/z estimation changes:
 * <ol>
 *   <li><b>Detector saturation</b> (apex at or above the saturation threshold, only if a threshold
 *   is set): the mode of an exponentially modified Gaussian fitted to the saturated core and a few
 *   flank points, with the fitted height bounded by the raw apex (see {@link EmgFitter}). Saturated
 *   TOF peaks are distorted without a flat top, so the threshold and not the peak shape decides.</li>
 *   <li><b>Sharp and asymmetric</b> intense peaks: an apex-centered least-squares parabola vertex,
 *   which tracks the mode and avoids the tail bias of the weighted centroid.</li>
 *   <li><b>Otherwise</b>: the baseline weighted centroid (intensity-weighted mean over the symmetric
 *   window above {@link #MZ_WEIGHTING_THRESHOLD}).</li>
 * </ol>
 * Weak peaks always use the weighted centroid (the fits are unstable at low S/N and the bias is
 * small there). A failed saturation fit falls back to the other branches.
 */
public class AdaptiveLocalMaxMassDetector implements MassDetector, PreprocessedIntensitiesProvider {

  public static final String NAME = "Adaptive local maximum mass detector";
  private static final Logger logger = Logger.getLogger(
      AdaptiveLocalMaxMassDetector.class.getName());

  // Thresholds for peak resolving and centroiding
  private static final double MZ_WEIGHTING_THRESHOLD = 0.4;
  private static final double VALLEY_FACTOR = 0.7;
  private static final double RISE_FACTOR = 1 / 0.7;
  private static final Weighting mzWeighting = Weighting.LINEAR;

  // Adaptive centroid tuning
  // only intense peaks get the parabola treatment; weak peaks keep the weighted centroid
  private static final double ADAPTIVE_MIN_PERCENT_BASE = 1.0;
  // half-width ratio at ASYMMETRY_HEIGHT_FRACTION above which a peak is asymmetric (-> parabola)
  private static final double ASYMMETRY_THRESHOLD = 1.5;
  // decision: 10 % of the apex like the EP asymmetry factor. TOF tails show low on the peak,
  // half height missed tailing saturated frames (benchmark 2026-10-27).
  private static final double ASYMMETRY_HEIGHT_FRACTION = 0.1;
  // number of points (apex +/- (N-1)/2) used for the apex-centered parabola
  private static final int PARABOLA_POINTS = 5;
  // saturation fit window: the saturated core plus this many non-zero points on each flank
  private static final int SATURATION_FLANK_POINTS = 3;
  private static final int SATURATION_MIN_FIT_POINTS = 3;
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
   * Minimum peak length in points excluding zeros
   */
  private final int minNonZeroDp;

  private final double noiseLevel;
  private final AbundanceMeasure intensityCalculation;

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
   * Threshold of the spectrum currently being processed, resolved at every entry point
   */
  // decision: mutable per-call state like spectrumBaseIntensity, the detector is not thread safe
  private double currentSaturationThreshold;

  /**
   * Raw file and its thresholds from the metadata or auto-derived, see
   * {@link SaturationPlateauEstimator}
   */
  private @Nullable RawDataFile thresholdFile;
  private final @NotNull Map<ThresholdKey, Double> fileThresholds = new HashMap<>();
  // mobility scan saturation plateau per ms level, also decides if frames are saturated
  private final @NotNull Map<Integer, Double> mobilityScanPlateaus = new HashMap<>();
  private boolean warnedMissingRawFile;

  /**
   * Optional preprocessing of the intensities (e.g. smoothing) before maximum and edge detection.
   * Returns the original intensities when no preprocessing is configured.
   */
  private final @NotNull MassDetectorPreprocessor preprocessor;

  /**
   * Preprocessed (e.g. smoothed) intensities of the most recently processed spectrum, or null if no
   * preprocessing was applied. Kept for the preview to display the preprocessed series.
   */
  private double @Nullable [] lastPreprocessedIntensities;

  /**
   * Base peak intensity of the spectrum currently being processed, used to gate the adaptive
   * centroid to intense peaks.
   */
  private double spectrumBaseIntensity;

  public AdaptiveLocalMaxMassDetector() {
    this(0, AbundanceMeasure.Height, 3, new LocalMaxNoSmoothingModule());
  }

  public AdaptiveLocalMaxMassDetector(final double noiseLevel,
      final AbundanceMeasure intensityCalculation, final int minNonZeroDp) {
    this(noiseLevel, intensityCalculation, minNonZeroDp, new LocalMaxNoSmoothingModule());
  }

  public AdaptiveLocalMaxMassDetector(final double noiseLevel,
      final AbundanceMeasure intensityCalculation, final int minNonZeroDp,
      final @NotNull MassDetectorPreprocessor preprocessor) {
    this(noiseLevel, intensityCalculation, minNonZeroDp, preprocessor, Double.POSITIVE_INFINITY);
  }

  /**
   * @param saturationThreshold raw intensity from which on peaks are treated as saturated and get
   *                            the EMG fit, {@link Double#POSITIVE_INFINITY} to disable
   */
  public AdaptiveLocalMaxMassDetector(final double noiseLevel,
      final AbundanceMeasure intensityCalculation, final int minNonZeroDp,
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
      final AbundanceMeasure intensityCalculation, final int minNonZeroDp,
      final @NotNull MassDetectorPreprocessor preprocessor, final double saturationThreshold,
      final @NotNull SaturationThresholdValue saturation) {
    this.saturation = saturation;
    this.currentSaturationThreshold = saturationThreshold;
    this.noiseLevel = noiseLevel;
    this.intensityCalculation = intensityCalculation;
    this.minNonZeroDp = minNonZeroDp;
    this.preprocessor = preprocessor;
    this.saturationThreshold = saturationThreshold;
  }

  /**
   * The maximum difference between two consecutive values. Either first 2 non zero values from left
   * or right, which ever is greater. Also assumes and checks that trailing and leading 0 intensity
   * values are available. Otherwise, returns a default value.
   */
  private static double getMaxMzDiff(final double[] mzs, final double[] intensities,
      final int numPoints) {
    double maxDistance = -1;

    // TOF and it seems like Orbitrap have highest mass difference between values at the end of the spectrum
    int top = numPoints - 1;
    for (; top > 1; top--) {
      // tof mz value distances are proportional to sqrt(m/z)
      // so the biggest mass diff will be at the top of the spectrum
      if (intensities[top] > 0 && intensities[top - 1] > 0) {
        // use first two points that are non zero. Who knows if padding zeros are actually spaced
        // according to the digitizer times.
        maxDistance = Math.abs(mzs[top] - mzs[top - 1]);
        break;
      }
    }

    // some detectors may have highest distance in beginning so better just add check here
    double leftMaxDistance = -1;
    for (int i = 1; i < top; i++) {
      // tof mz value distances are proportional to sqrt(m/z)
      // so the biggest mass diff will be at the top of the spectrum
      if (intensities[i] > 0 && intensities[i - 1] > 0) {
        // use first two points that are non zero. Who knows if padding zeros are actually spaced
        // according to the digitizer times.
        leftMaxDistance = Math.abs(mzs[i] - mzs[i - 1]);
        break;
      }
    }

    maxDistance = Math.max(leftMaxDistance, maxDistance);
    if (Double.compare(-1, maxDistance) == 0) {
      return 0.1; // nothing found so use default value
    }

    return maxDistance;
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
    currentSaturationThreshold = threshold;
    if (Double.isInfinite(threshold)) {
      for (final MassSpectrum mobilityScan : mobilityScans) {
        values.add(centroid(mobilityScan, null));
      }
      return values;
    }

    // saturated centroids (EMG mode) are corrected with the unsaturated flanks of the same ion
    final List<double[]> mzs = new ArrayList<>(mobilityScans.size());
    final List<double[]> heights = new ArrayList<>(mobilityScans.size());
    for (final MassSpectrum mobilityScan : mobilityScans) {
      final List<LocalMaxPeak> peaks = new ArrayList<>();
      final double[][] scanValues = centroid(mobilityScan, peaks);
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
  private static double[][] sortByMz(final double[] mzs, final double[] intensities) {
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
   * import) get no saturation handling.
   */
  private double resolveThreshold(@NotNull final MassSpectrum spectrum) {
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
    currentSaturationThreshold = resolveThreshold(spectrum);
    return centroid(spectrum, null);
  }

  /**
   * @param peakSink receives one peak per centroid in the same order (apex height for the
   *                 saturation handling), or null
   */
  private double[][] centroid(final MassSpectrum spectrum,
      @Nullable final List<LocalMaxPeak> peakSink) {
    final PreparedSpectrum prepared = prepareSpectrum(spectrum);
    if (prepared == null) {
      return new double[2][0];
    }

    final DoubleArrayList resultMzs = new DoubleArrayList();
    final DoubleArrayList resultIntensities = new DoubleArrayList();

    for (final IndexRange range : prepared.ranges()) {
      findAndCentroidPeaks(prepared.mzs(), prepared.detectIntensities(), prepared.origIntensities(),
          range, prepared.absMinIntensity(), resultMzs, resultIntensities, peakSink);
    }

    final double[][] result = new double[2][];
    result[0] = resultMzs.toDoubleArray();
    result[1] = resultIntensities.toDoubleArray();

    return result;
  }

  /**
   * Detects peaks like {@link #getMassValues(MassSpectrum)} but returns rich descriptors (apex,
   * edges, centroid m/z and height) instead of only the centroid arrays. Intended for
   * analysis/diagnostics, not the hot import path.
   */
  public @NotNull List<LocalMaxPeak> detectPeaks(final MassSpectrum spectrum) {
    currentSaturationThreshold = resolveThreshold(spectrum);
    final PreparedSpectrum prepared = prepareSpectrum(spectrum);
    if (prepared == null) {
      return List.of();
    }

    final List<LocalMaxPeak> peaks = new ArrayList<>();
    // the result arrays are required by the shared detection method but are not needed here
    final DoubleArrayList ignoredMzs = new DoubleArrayList();
    final DoubleArrayList ignoredIntensities = new DoubleArrayList();

    for (final IndexRange range : prepared.ranges()) {
      findAndCentroidPeaks(prepared.mzs(), prepared.detectIntensities(), prepared.origIntensities(),
          range, prepared.absMinIntensity(), ignoredMzs, ignoredIntensities, peaks);
    }

    return peaks;
  }

  /**
   * Shared preparation: extracts the spectrum arrays, detects the consecutive ranges, applies the
   * optional preprocessing and records the preprocessed series. Returns null if the spectrum is too
   * small to process.
   */
  private @Nullable PreparedSpectrum prepareSpectrum(final MassSpectrum spectrum) {
    // reset so the preview never shows a stale preprocessed series for an unprocessed spectrum
    lastPreprocessedIntensities = null;

    final int numPoints = spectrum.getNumberOfDataPoints();
    if (numPoints < minNonZeroDp) {
      return null;
    }

    // Extract data to local arrays for faster access (avoiding MemorySegment overhead)
    final double[] mzs = new double[numPoints];
    final double[] intensities = new double[numPoints];

    // Bulk extraction loop, also tracks the base peak intensity for the adaptive gate
    double baseIntensity = 0;
    for (int i = 0; i < numPoints; i++) {
      mzs[i] = spectrum.getMzValue(i);
      final double intensity = spectrum.getIntensityValue(i);
      intensities[i] = intensity;
      if (intensity > baseIntensity) {
        baseIntensity = intensity;
      }
    }
    spectrumBaseIntensity = baseIntensity;

    final double maxDiff = getMaxMzDiff(mzs, intensities, numPoints);

    final List<IndexRange> consecutiveRanges = new ArrayList<>();

    double absMinIntensity = Double.MAX_VALUE;
    if (intensities[0] > 0) {
      absMinIntensity = intensities[0];
    }

    int currentRegionStart = 0;
    double lastMz = mzs[0];
    boolean onePointAboveNoise = false;
    for (int i = 1; i < numPoints; i++) {
      final double thisMz = mzs[i];
      final double thisInt = intensities[i];

      // Track absolute minimum intensity > 0
      if (thisInt > 0 && thisInt < absMinIntensity) {
        absMinIntensity = thisInt;
      }

      if (thisInt > noiseLevel) {
        onePointAboveNoise = true;
      }

      final double mzDelta = thisMz - lastMz;

      // If the gap is too large, we close the current region and start a new one
      if (mzDelta >= maxDiff) {
        // Only add regions that contain enough data points to form a peak (e.g., > 2 points)
        // data point at i was a jump to a new region so exclude this point
        if (i - currentRegionStart >= minNonZeroDp && onePointAboveNoise) {
          consecutiveRanges.add(IndexRange.ofInclusive(currentRegionStart, i - 1));
        }
        currentRegionStart = i;
        onePointAboveNoise = false;
      }

      lastMz = thisMz;
    }

    // Add the final region if valid
    if (numPoints - currentRegionStart >= minNonZeroDp && onePointAboveNoise) {
      consecutiveRanges.add(IndexRange.ofInclusive(currentRegionStart, numPoints - 1));
    }

    // Handle case where spectrum was all zeros or empty
    if (absMinIntensity == Double.MAX_VALUE) {
      absMinIntensity = 0.0;
    }

    // Optional preprocessing: maximum and edge detection run on the (potentially smoothed)
    // intensities, while the intensity calculation keeps using the original intensities.
    final double[] detectIntensities = preprocessor.preprocessIntensities(intensities,
        consecutiveRanges);
    // store only if preprocessing actually changed the data (NONE returns the original array)
    lastPreprocessedIntensities = (detectIntensities == intensities) ? null : detectIntensities;

    return new PreparedSpectrum(mzs, intensities, detectIntensities, consecutiveRanges,
        absMinIntensity);
  }

  /**
   * Identifies peaks within a continuous region using the valley/rise logic, then centroids them.
   *
   * @param detectIntensities the (potentially smoothed) intensities used for maximum and edge
   *                          detection.
   * @param origIntensities   the original intensities used for the intensity calculation.
   */
  private void findAndCentroidPeaks(final double[] mzs, final double[] detectIntensities,
      final double[] origIntensities, final IndexRange range, final double minIntensity,
      final DoubleArrayList resultMzs, final DoubleArrayList resultIntensities,
      @Nullable final List<LocalMaxPeak> peakSink) {

    // Find all raw local maxima (candidates) in the region above noise
    final IntArrayList candidateIndices = findLocalMaximaIndices(detectIntensities, range);

    if (candidateIndices.isEmpty()) {
      return;
    }

    // Filter and merge candidates based on the * 0.7 to / 0.7 rule
    int activePeakIdx = candidateIndices.getInt(0);
    int leftBoundary = range.min(); // Start of the region

    // todo check what happens if there are consecutive but zero intensities in the range
    for (int i = 1; i < candidateIndices.size(); i++) {
      final int nextCandidateIdx = candidateIndices.getInt(i);

      // Find the deepest valley between the current active peak and the next candidate (to the right)
      final int valleyIdx = findLowestValleyIndex(detectIntensities, activePeakIdx,
          nextCandidateIdx);

      final double activePeakInt = detectIntensities[activePeakIdx];
      final double nextCandidateInt = detectIntensities[nextCandidateIdx];
      final double valleyInt = detectIntensities[valleyIdx];

      // Rule: Separate if it drops below 0.7 * active AND rises 1.3 * valley
      final boolean dropsEnough = valleyInt < (VALLEY_FACTOR * activePeakInt);
      final boolean risesEnough = nextCandidateInt > (RISE_FACTOR * valleyInt);

      if (dropsEnough && risesEnough) {
        // They are separate peaks.
        // The right boundary for the current peak is the valley.
        // include valleyIdx in this peak and also as potential new peak - similar to belows use of range.maxExclusive
        processSinglePeak(mzs, origIntensities, leftBoundary, valleyIdx + 1, minIntensity,
            resultMzs, resultIntensities, peakSink);

        // Move to the next peak
        activePeakIdx = nextCandidateIdx;
        leftBoundary = valleyIdx; // Next peak starts from this valley
      } else {
        // They are not resolved enough; merge them.
        // Update the active peak index to be the higher of the two.
        // left boundary is kept to merge the peaks
        if (nextCandidateInt > activePeakInt) {
          activePeakIdx = nextCandidateIdx;
        }
      }
    }

    processSinglePeak(mzs, origIntensities, leftBoundary, range.maxExclusive(), minIntensity,
        resultMzs, resultIntensities, peakSink);
  }

  /**
   * Helper to find all indices in range that are local maxima > noise.
   */
  private IntArrayList findLocalMaximaIndices(final double[] intensities, final IndexRange range) {
    final IntArrayList indices = new IntArrayList();
    final int start = range.min();
    final int end = range.maxExclusive();

    for (int i = start; i < end; i++) {
      final double currentInt = intensities[i];

      final double leftInt = (i == start) ? 0 : intensities[i - 1];
      final double rightInt = (i == end - 1) ? 0 : intensities[i + 1];

      // Check local max
      if (currentInt >= leftInt && currentInt > rightInt) {
        indices.add(i);
      }
    }
    return indices;
  }

  /**
   * Finds the index with minimum intensity between two indices.
   */
  private int findLowestValleyIndex(final double[] intensities, final int startIdx,
      final int endIdx) {
    int minIdx = startIdx;
    double minVal = Double.MAX_VALUE;

    // Search strictly between the peaks
    for (int i = startIdx + 1; i < endIdx; i++) {
      final double val = intensities[i];
      if (val < minVal) {
        minVal = val;
        minIdx = i;
      }
    }
    return minIdx;
  }

  /**
   * Calculates the adaptive centroid and intensity for a defined peak and adds to results. The peak
   * edges ({@code startIdx}, {@code endIdx}) are determined on the (potentially smoothed) detection
   * series, but the intensity calculation (apex, height, area, centroid) runs on the original
   * intensities passed here.
   *
   * @param startIdx        Inclusive start of peak region (valley or range start).
   * @param endIdx          Exclusive end of peak region (valley or range end).
   * @param absMinIntensity Absolute minimum intensity of the whole spectrum.
   */
  private void processSinglePeak(final double[] mzs, final double[] intensities, final int startIdx,
      final int endIdx, final double absMinIntensity, final DoubleArrayList resultMzs,
      final DoubleArrayList resultIntensities, @Nullable final List<LocalMaxPeak> peakSink) {

    if (endIdx - startIdx < minNonZeroDp) {
      return;
    }

    // Locate the apex on the original intensities within the detected edges.
    int peakMaxIdx = startIdx;
    double maxIntensity = intensities[startIdx];
    for (int i = startIdx + 1; i < endIdx; i++) {
      if (intensities[i] > maxIntensity) {
        maxIntensity = intensities[i];
        peakMaxIdx = i;
      }
    }

    final double detectionThreshold = Math.max(noiseLevel, 2 * absMinIntensity);

    // check the actual intensity as noise level not the area which is harder to optimize
    // the peak detection should pick the same peaks for the same noise level no matter
    // if AREA or HEIGHT is selected for intensity representation
    if (maxIntensity < detectionThreshold) {
      return;
    }

    final double mzWeightingCutoff = maxIntensity * MZ_WEIGHTING_THRESHOLD;
    final int minPointsPerEdge = Math.min(peakMaxIdx - startIdx, endIdx - peakMaxIdx);
    final SimpleIntegerRange peakSymmetryRange = new SimpleIntegerRange(
        peakMaxIdx - minPointsPerEdge, peakMaxIdx + minPointsPerEdge);

    double sumMzInt = 0.0;
    double sumIntForMz = 0.0;
    double totalArea = 0.0;
    int nonZeroPoints = 0;

    // Integrate Valley-to-Valley
    for (int i = startIdx; i < endIdx; i++) {
      final double intensity = intensities[i];
      final double mz = mzs[i];

      // Integration: Sum ALL points in the valley-to-valley region
      totalArea += intensity;

      if (intensity > mzWeightingCutoff && peakSymmetryRange.contains(i)) {
        sumMzInt += (mz * mzWeighting.transform(intensity));
        sumIntForMz += mzWeighting.transform(intensity);
      }
      if (intensity > 0) {
        nonZeroPoints++;
      }
    }

    // Safety check if no points met the weighting criteria (unlikely if max > detectionThreshold)
    if (sumIntForMz == 0.0 || nonZeroPoints < minNonZeroDp) {
      return;
    }

    final double centroidMz = adaptiveCentroidMz(mzs, intensities, startIdx, endIdx, peakMaxIdx,
        maxIntensity, sumMzInt / sumIntForMz);
    final double finalIntensity =
        (intensityCalculation == AbundanceMeasure.Area) ? totalArea : maxIntensity;

    resultMzs.add(centroidMz);
    resultIntensities.add(finalIntensity);

    if (peakSink != null) {
      peakSink.add(new LocalMaxPeak(peakMaxIdx, startIdx, endIdx, centroidMz, maxIntensity));
    }
  }

  /**
   * Three-way adaptive m/z. A peak reaching the saturation threshold uses the mode of an EMG fit.
   * For other peaks below {@link #ADAPTIVE_MIN_PERCENT_BASE} of the base peak the weighted centroid
   * is kept (fits are unstable at low S/N). For intense peaks: a sharp asymmetric peak uses an
   * apex-centered parabola; everything else keeps the weighted centroid. Any degenerate fit falls
   * back to the next branch.
   */
  private double adaptiveCentroidMz(final double[] mzs, final double[] intensities,
      final int startIdx, final int endIdx, final int peakMaxIdx, final double maxIntensity,
      final double weightedCentroidMz) {

    // 1. detector saturation -> mode of an EMG fitted to the saturated core and the flanks
    if (maxIntensity >= currentSaturationThreshold) {
      final double saturated = saturatedEmgMode(mzs, intensities, startIdx, endIdx, peakMaxIdx,
          maxIntensity);
      if (!Double.isNaN(saturated)) {
        return saturated;
      }
    }

    final boolean intenseEnough =
        maxIntensity >= spectrumBaseIntensity * (ADAPTIVE_MIN_PERCENT_BASE / 100.0);
    if (!intenseEnough) {
      return weightedCentroidMz;
    }

    // 2. sharp and asymmetric -> apex-centered parabola vertex
    // decision: asymmetry from the peak widths, not from the point counts between the edges. The
    // edges can reach far into the baseline and move with smoothing.
    final double asymmetry = widthAsymmetry(mzs, intensities, startIdx, endIdx, peakMaxIdx);
    final boolean asymmetric = Double.isNaN(asymmetry) || asymmetry >= ASYMMETRY_THRESHOLD;
    if (asymmetric) {
      final double fit = apexParabolaVertexMz(mzs, intensities, peakMaxIdx, startIdx, endIdx,
          PARABOLA_POINTS);
      if (!Double.isNaN(fit)) {
        return fit;
      }
    }

    // 3. otherwise the weighted centroid
    return weightedCentroidMz;
  }

  /**
   * Mode of an EMG fitted to the saturated core of the peak (all points at or above the saturation
   * threshold) and {@link #SATURATION_FLANK_POINTS} non-zero points on each side, limited to the
   * peak edges. All points have the same weight, the fitted height is bounded by the raw apex
   * because saturation only lowers the apex. Returns NaN if both flanks have no unsaturated point,
   * the fit fails or its mode leaves the fit window.
   */
  private double saturatedEmgMode(final double[] mzs, final double[] intensities,
      final int startIdx, final int endIdx, final int peakMaxIdx, final double maxIntensity) {
    int coreStart = -1;
    int coreEnd = -1;
    for (int i = startIdx; i < endIdx; i++) {
      if (intensities[i] >= currentSaturationThreshold) {
        if (coreStart < 0) {
          coreStart = i;
        }
        coreEnd = i;
      }
    }
    if (coreStart < 0) {
      return Double.NaN;
    }
    int from = coreStart;
    for (int added = 0; from > startIdx && added < SATURATION_FLANK_POINTS; ) {
      from--;
      if (intensities[from] > 0) {
        added++;
      }
    }
    int to = coreEnd;
    for (int added = 0; to < endIdx - 1 && added < SATURATION_FLANK_POINTS; ) {
      to++;
      if (intensities[to] > 0) {
        added++;
      }
    }

    final DoubleArrayList fitMzs = new DoubleArrayList();
    final DoubleArrayList fitIntensities = new DoubleArrayList();
    double coreMzSum = 0;
    int core = 0;
    int unsaturatedLeft = 0;
    int unsaturatedRight = 0;
    for (int i = from; i <= to; i++) {
      if (intensities[i] <= 0) {
        continue;
      }
      fitMzs.add(mzs[i]);
      fitIntensities.add(intensities[i]);
      if (intensities[i] >= currentSaturationThreshold) {
        coreMzSum += mzs[i];
        core++;
      } else if (i < peakMaxIdx) {
        unsaturatedLeft++;
      } else {
        unsaturatedRight++;
      }
    }
    // sigma and tau need unsaturated points on both flanks
    if (fitMzs.size() < SATURATION_MIN_FIT_POINTS || unsaturatedLeft == 0
        || unsaturatedRight == 0) {
      return Double.NaN;
    }

    final double low = fitMzs.getDouble(0);
    final double high = fitMzs.getDouble(fitMzs.size() - 1);
    final double[] weights = new double[fitMzs.size()];
    Arrays.fill(weights, 1);
    // start at the center of the saturated core, width estimate from the window
    final EmgFitter.Fit fit = EmgFitter.fit(fitMzs.toDoubleArray(), fitIntensities.toDoubleArray(),
        weights, coreMzSum / core, Math.max(1e-6, (high - low) / 6), maxIntensity * 1.2,
        maxIntensity);
    if (fit == null || Double.isNaN(fit.mode()) || fit.mode() < low || fit.mode() > high) {
      return Double.NaN;
    }
    return fit.mode();
  }

  /**
   * Ratio of the wider to the narrower half-width at {@link #ASYMMETRY_HEIGHT_FRACTION} of the apex
   * (>= 1). The apex position is the vertex of the parabola through the apex and its neighbors, so
   * the sampling position of the apex point does not change the ratio. The crossings of the height
   * level are interpolated linearly.
   *
   * @return the asymmetry or NaN if one side does not fall below the level within the edges
   */
  private static double widthAsymmetry(final double[] mzs, final double[] intensities,
      final int startIdx, final int endIdx, final int peakMaxIdx) {
    final double level = intensities[peakMaxIdx] * ASYMMETRY_HEIGHT_FRACTION;
    double leftMz = Double.NaN;
    for (int i = peakMaxIdx; i > startIdx; i--) {
      if (intensities[i - 1] < level) {
        leftMz = interpolateMz(mzs[i - 1], intensities[i - 1], mzs[i], intensities[i], level);
        break;
      }
    }
    double rightMz = Double.NaN;
    for (int i = peakMaxIdx; i < endIdx - 1; i++) {
      if (intensities[i + 1] < level) {
        rightMz = interpolateMz(mzs[i], intensities[i], mzs[i + 1], intensities[i + 1], level);
        break;
      }
    }
    final double apexMz = threePointVertexMz(mzs, intensities, peakMaxIdx, startIdx, endIdx);
    final double left = apexMz - leftMz;
    final double right = rightMz - apexMz;
    if (!(left > 0 && right > 0)) {
      return Double.NaN;
    }
    return Math.max(left, right) / Math.min(left, right);
  }

  /**
   * m/z where the line between two points reaches the intensity level
   */
  private static double interpolateMz(final double mz1, final double int1, final double mz2,
      final double int2, final double level) {
    return int1 == int2 ? (mz1 + mz2) / 2 : mz1 + (level - int1) / (int2 - int1) * (mz2 - mz1);
  }

  /**
   * Vertex of the parabola through the apex and its two neighbors, the apex m/z if the apex is at
   * an edge or the vertex leaves the three points.
   */
  private static double threePointVertexMz(final double[] mzs, final double[] intensities,
      final int i, final int startIdx, final int endIdx) {
    if (i <= startIdx || i >= endIdx - 1) {
      return mzs[i];
    }
    final double x0 = mzs[i - 1], x1 = mzs[i], x2 = mzs[i + 1];
    final double y0 = intensities[i - 1], y1 = intensities[i], y2 = intensities[i + 1];
    final double denominator = (x0 - x1) * (x0 - x2) * (x1 - x2);
    if (denominator == 0) {
      return x1;
    }
    final double a = (x2 * (y1 - y0) + x1 * (y0 - y2) + x0 * (y2 - y1)) / denominator;
    final double b =
        (x2 * x2 * (y0 - y1) + x1 * x1 * (y2 - y0) + x0 * x0 * (y1 - y2)) / denominator;
    if (a >= 0) {
      return x1;
    }
    final double vertex = -b / (2 * a);
    return vertex >= x0 && vertex <= x2 ? vertex : x1;
  }

  /**
   * Vertex m/z of the least-squares parabola over the {@code n} points centered on the apex
   * ({@code apex +/- (n-1)/2}), clamped to the peak edges. Apex-centered (not the n highest points)
   * so a tailing flank does not skew the window. Returns NaN if there are fewer than 3 points, the
   * top is too flat (degenerate), or the vertex falls outside the peak edges.
   */
  private static double apexParabolaVertexMz(final double[] mzs, final double[] intensities,
      final int peakMaxIdx, final int startIdx, final int endIdx, final int n) {
    final int half = (n - 1) / 2;
    final int from = Math.max(startIdx, peakMaxIdx - half);
    final int to = Math.min(endIdx - 1, peakMaxIdx + half);
    final int count = to - from + 1;
    if (count < 3) {
      return Double.NaN;
    }

    // least squares y = a*u^2 + b*u + c with u = mz - apexMz (centering improves conditioning)
    final double xCenter = mzs[peakMaxIdx];
    final double s0 = count;
    double s1 = 0, s2 = 0, s3 = 0, s4 = 0, t0 = 0, t1 = 0, t2 = 0;
    for (int i = from; i <= to; i++) {
      final double u = mzs[i] - xCenter;
      final double y = intensities[i];
      final double u2 = u * u;
      s1 += u;
      s2 += u2;
      s3 += u2 * u;
      s4 += u2 * u2;
      t0 += y;
      t1 += u * y;
      t2 += u2 * y;
    }
    final double det = det3(s4, s3, s2, s3, s2, s1, s2, s1, s0);
    if (Math.abs(det) < 1e-30) {
      return Double.NaN;
    }
    final double a = det3(t2, s3, s2, t1, s2, s1, t0, s1, s0) / det;
    final double b = det3(s4, t2, s2, s3, t1, s1, s2, t0, s0) / det;
    if (Math.abs(a) < 1e-30) {
      return Double.NaN; // too flat -> degenerate vertex
    }
    final double vertex = xCenter - b / (2 * a);
    // reject runaway vertices outside the peak (e.g. nearly-flat noisy tops)
    if (vertex < mzs[startIdx] || vertex > mzs[endIdx - 1]) {
      return Double.NaN;
    }
    return vertex;
  }

  private static double det3(final double a, final double b, final double c, final double d,
      final double e, final double f, final double g, final double h, final double i) {
    return a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g);
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
  public double[][] getMassValues(double[] mzs, double[] intensities,
      @NotNull MassSpectrumType type) {
    return getMassValues(new SimpleMassSpectrum(mzs, intensities, type));
  }

  @Override
  public double @Nullable [] getLastPreprocessedIntensities() {
    return lastPreprocessedIntensities;
  }

  /**
   * Shared, prepared spectrum data used by both {@link #getMassValues(MassSpectrum)} and
   * {@link #detectPeaks(MassSpectrum)}.
   */
  private record PreparedSpectrum(double[] mzs, double[] origIntensities,
                                  double[] detectIntensities, List<IndexRange> ranges,
                                  double absMinIntensity) {

  }
}
