package io.github.mzmine.modules.dataprocessing.featdet_massdetection.tof;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.collections.IndexRange;
import io.github.mzmine.util.collections.SimpleIndexRange;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TofMassDetector implements MassDetector {

  private static final Logger logger = Logger.getLogger(TofMassDetector.class.getName());

  // Thresholds for peak resolving and centroiding
  private static final double MZ_WEIGHTING_THRESHOLD = 0.4;
  private static final double VALLEY_FACTOR = 0.7;
  private static final double RISE_FACTOR = 1.3;

  private final double noiseLevel;
  private final AbundanceMeasure intensityCalculation;

  public TofMassDetector() {
    this(0, AbundanceMeasure.Height);
  }

  public TofMassDetector(final double noiseLevel, final AbundanceMeasure intensityCalculation) {
    this.noiseLevel = noiseLevel;
    this.intensityCalculation = intensityCalculation;
  }

  @Override
  public MassDetector create(final ParameterSet params) {
    return new TofMassDetector(params.getValue(TofMassDetectorParameters.noiseLevel),
        params.getValue(TofMassDetectorParameters.intensityCalculation));
  }

  @Override
  public boolean filtersActive() {
    return false;
  }

  @Override
  public double[][] getMassValues(final MassSpectrum spectrum) {
    final int numPoints = spectrum.getNumberOfDataPoints();
    if (numPoints < 3) {
      return new double[2][0];
    }

    // Extract data to local arrays for faster access (avoiding MemorySegment overhead)
    final double[] mzs = new double[numPoints];
    final double[] intensities = new double[numPoints];

    // Bulk extraction loop
    for (int i = 0; i < numPoints; i++) {
      mzs[i] = spectrum.getMzValue(i);
      intensities[i] = spectrum.getIntensityValue(i);
    }

    final double maxDiff = getMaxMzDiff(mzs, intensities, numPoints);

    final List<IndexRange> consecutiveRanges = new ArrayList<>();

    // 1. Detect continuous regions and track minimum positive intensity
    int currentRegionStart = 0;
    double lastMz = mzs[0];

    // Initialize minIntensity. Check first point.
    double minIntensity = Double.MAX_VALUE;
    if (intensities[0] > 0) {
      minIntensity = intensities[0];
    }

    boolean onePointAboveNoise = false;
    for (int i = 1; i < numPoints; i++) {
      final double thisMz = mzs[i];
      final double thisInt = intensities[i];

      // Track absolute minimum intensity > 0
      if (thisInt > 0 && thisInt < minIntensity) {
        minIntensity = thisInt;
      }

      if (thisInt > noiseLevel) {
        onePointAboveNoise = true;
      }

      final double mzDelta = thisMz - lastMz;

      // If the gap is too large, we close the current region and start a new one
      if (mzDelta >= maxDiff) {
        // Only add regions that contain enough data points to form a peak (e.g., > 2 points)
        if (i - currentRegionStart > 2 && onePointAboveNoise) {
          consecutiveRanges.add(new SimpleIndexRange(currentRegionStart, i));
        }
        currentRegionStart = i;
        onePointAboveNoise = false;
      }

      lastMz = thisMz;
    }

    // Add the final region if valid
    if (numPoints - currentRegionStart > 2 && onePointAboveNoise) {
      consecutiveRanges.add(new SimpleIndexRange(currentRegionStart, numPoints));
    }

    // Handle case where spectrum was all zeros or empty
    if (minIntensity == Double.MAX_VALUE) {
      minIntensity = 0.0;
    }

    // 2. Process regions to find centroids
    final DoubleArrayList resultMzs = new DoubleArrayList();
    final DoubleArrayList resultIntensities = new DoubleArrayList();

    for (final IndexRange range : consecutiveRanges) {
      findAndCentroidPeaks(mzs, intensities, range, minIntensity, resultMzs, resultIntensities);
    }

    // 3. Convert results to double[][] format
    final double[][] result = new double[2][];
    result[0] = resultMzs.toDoubleArray();
    result[1] = resultIntensities.toDoubleArray();

    return result;
  }

  /**
   * Identifies peaks within a continuous region using the valley/rise logic, then centroids them.
   */
  private void findAndCentroidPeaks(final double[] mzs, final double[] intensities,
      final IndexRange range, final double minIntensity, final DoubleArrayList resultMzs,
      final DoubleArrayList resultIntensities) {

    // 1. Find all raw local maxima (candidates) in the region above noise
    final IntArrayList candidateIndices = findLocalMaximaIndices(intensities, range);

    if (candidateIndices.isEmpty()) {
      return;
    }

    // 2. Filter and merge candidates based on the 0.7 / 1.3 rule
    int activePeakIdx = candidateIndices.getInt(0);
    int leftBoundary = range.min(); // Start of the region

    for (int i = 1; i < candidateIndices.size(); i++) {
      final int nextCandidateIdx = candidateIndices.getInt(i);

      // Find the deepest valley between the current active peak and the next candidate
      final int valleyIdx = findLowestValleyIndex(intensities, activePeakIdx, nextCandidateIdx);

      final double activePeakInt = intensities[activePeakIdx];
      final double nextCandidateInt = intensities[nextCandidateIdx];
      final double valleyInt = intensities[valleyIdx];

      // Rule: Separate if it drops below 0.7 * active AND rises 1.3 * valley
      final boolean dropsEnough = valleyInt < (VALLEY_FACTOR * activePeakInt);
      final boolean risesEnough = nextCandidateInt > (RISE_FACTOR * valleyInt);

      if (dropsEnough && risesEnough) {
        // They are separate peaks.
        // The right boundary for the current peak is the valley.
        processSinglePeak(mzs, intensities, activePeakIdx, leftBoundary, valleyIdx, minIntensity,
            resultMzs, resultIntensities);

        // Move to the next peak
        activePeakIdx = nextCandidateIdx;
        leftBoundary = valleyIdx; // Next peak starts from this valley
      } else {
        // They are not resolved enough; merge them.
        // Update the active peak index to be the higher of the two.
        if (nextCandidateInt > activePeakInt) {
          activePeakIdx = nextCandidateIdx;
        }
        // leftBoundary remains unchanged.
      }
    }

    // Process the final confirmed peak
    processSinglePeak(mzs, intensities, activePeakIdx, leftBoundary, range.maxExclusive(),
        minIntensity, resultMzs, resultIntensities);
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
      if (currentInt < noiseLevel) {
        continue;
      }

      final double leftInt = (i == start) ? 0 : intensities[i - 1];
      final double rightInt = (i == end - 1) ? 0 : intensities[i + 1];

      // Check local max
      // Strictly greater than right neighbor as requested
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
   * Calculates centroid and intensity for a defined peak and adds to results.
   *
   * @param peakMaxIdx   The index of the maximum intensity within these bounds.
   * @param startIdx     Inclusive start of peak region (valley or range start).
   * @param endIdx       Exclusive end of peak region (valley or range end).
   * @param minIntensity Absolute minimum intensity of the whole spectrum.
   */
  private void processSinglePeak(final double[] mzs, final double[] intensities,
      final int peakMaxIdx, final int startIdx, final int endIdx, final double minIntensity,
      final DoubleArrayList resultMzs, final DoubleArrayList resultIntensities) {

    final double maxIntensity = intensities[peakMaxIdx];

    // 1. Peak Validity Check
    // "The noise level only defines the minimum intensity a peak must exceed at least once"
    final double detectionThreshold;
    if (maxIntensity < 5 * minIntensity) {
      detectionThreshold = 2 * minIntensity;
    } else {
      detectionThreshold = noiseLevel;
    }

    if (maxIntensity < detectionThreshold) {
      return;
    }

    // 2. Prepare for calculation
    // M/z weighting still uses the top 40% logic
    final double mzWeightingCutoff = maxIntensity * MZ_WEIGHTING_THRESHOLD;

    double sumMzInt = 0.0;
    double sumIntForMz = 0.0;
    double totalArea = 0.0;

    // 3. Integrate Valley-to-Valley
    // We iterate strictly from startIdx to endIdx (exclusive), which are the boundaries
    // determined by the peak resolving logic (valleys or region edges).
    for (int i = startIdx; i < endIdx; i++) {
      final double intensity = intensities[i];
      final double mz = mzs[i];

      // Integration: Sum ALL points in the valley-to-valley region
      totalArea += intensity;

      // Centroid M/Z: Weighted average using ONLY points > 0.4 * maxIntensity
      if (intensity > mzWeightingCutoff) {
        sumMzInt += (mz * intensity);
        sumIntForMz += intensity;
      }
    }

    // Safety check if no points met the weighting criteria (unlikely if max > detectionThreshold)
    if (sumIntForMz == 0) {
      return;
    }

    final double centroidMz = sumMzInt / sumIntForMz;
    final double finalIntensity =
        (intensityCalculation == AbundanceMeasure.Area) ? totalArea : maxIntensity;

    resultMzs.add(centroidMz);
    resultIntensities.add(finalIntensity);
  }

  private static double getMaxMzDiff(final double[] mzs, final double[] intensities,
      final int numPoints) {
    for (int i = numPoints - 1; i > 1; i--) {
      // tof mz value distances are proportional to sqrt(m/z)
      // so the biggest mass diff will be at the top of the spectrum
      if (Double.compare(intensities[i], 0) != 0 || !(intensities[i - 1] > 0)) {
        continue;
      }
      return Math.abs(mzs[i] - mzs[i - 1]);
    }
    return 0.1;
  }

  @Override
  public @NotNull String getName() {
    return "TOF mass detector";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return TofMassDetectorParameters.class;
  }
}