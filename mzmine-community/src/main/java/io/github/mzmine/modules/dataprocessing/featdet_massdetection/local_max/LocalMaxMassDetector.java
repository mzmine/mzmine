/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.SimpleRange.SimpleIntegerRange;
import io.github.mzmine.datamodel.impl.SimpleMassSpectrum;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.collections.IndexRange;
import io.github.mzmine.util.collections.SimpleIndexRange;
import io.github.mzmine.util.maths.Weighting;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LocalMaxMassDetector implements MassDetector {

  private static final Logger logger = Logger.getLogger(LocalMaxMassDetector.class.getName());

  // Thresholds for peak resolving and centroiding
  private static final double MZ_WEIGHTING_THRESHOLD = 0.4;
  private static final double VALLEY_FACTOR = 0.7;
  private static final double RISE_FACTOR = 1 / 0.7;
  private static final Weighting mzWeighting = Weighting.LINEAR;

  /**
   * Minimum peak length in points excluding zeros
   */
  private final int minNonZeroDp;

  private final double noiseLevel;
  private final AbundanceMeasure intensityCalculation;

  public LocalMaxMassDetector() {
    this(0, AbundanceMeasure.Height, 3);
  }

  public LocalMaxMassDetector(final double noiseLevel, final AbundanceMeasure intensityCalculation,
      final int minNonZeroDp) {
    this.noiseLevel = noiseLevel;
    this.intensityCalculation = intensityCalculation;
    this.minNonZeroDp = minNonZeroDp;
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
    return new LocalMaxMassDetector(params.getValue(LocalMaxMassDetectorParameters.noiseLevel),
        params.getValue(LocalMaxMassDetectorParameters.intensityCalculation),
        params.getValue(LocalMaxMassDetectorParameters.minNumberOfDp));
  }

  @Override
  public boolean filtersActive() {
    return true;
  }

  @Override
  public double[][] getMassValues(final MassSpectrum spectrum) {
    final int numPoints = spectrum.getNumberOfDataPoints();
    if (numPoints < minNonZeroDp) {
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
          consecutiveRanges.add(new SimpleIndexRange(currentRegionStart, i - 1));
        }
        currentRegionStart = i;
        onePointAboveNoise = false;
      }

      lastMz = thisMz;
    }

    // Add the final region if valid
    if (numPoints - currentRegionStart >= minNonZeroDp && onePointAboveNoise) {
      consecutiveRanges.add(new SimpleIndexRange(currentRegionStart, numPoints - 1));
    }

    // Handle case where spectrum was all zeros or empty
    if (absMinIntensity == Double.MAX_VALUE) {
      absMinIntensity = 0.0;
    }

    final DoubleArrayList resultMzs = new DoubleArrayList();
    final DoubleArrayList resultIntensities = new DoubleArrayList();

    for (final IndexRange range : consecutiveRanges) {
      findAndCentroidPeaks(mzs, intensities, range, absMinIntensity, resultMzs, resultIntensities);
    }

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

    // Find all raw local maxima (candidates) in the region above noise
    final IntArrayList candidateIndices = findLocalMaximaIndices(intensities, range);

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
        // include valleyIdx in this peak and also as potential new peak - similar to belows use of range.maxExclusive
        processSinglePeak(mzs, intensities, activePeakIdx, leftBoundary, valleyIdx + 1,
            minIntensity, resultMzs, resultIntensities);

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
   * Calculates centroid and intensity for a defined peak and adds to results.
   *
   * @param peakMaxIdx      The index of the maximum intensity within these bounds.
   * @param startIdx        Inclusive start of peak region (valley or range start).
   * @param endIdx          Exclusive end of peak region (valley or range end).
   * @param absMinIntensity Absolute minimum intensity of the whole spectrum.
   */
  private void processSinglePeak(final double[] mzs, final double[] intensities,
      final int peakMaxIdx, final int startIdx, final int endIdx, final double absMinIntensity,
      final DoubleArrayList resultMzs, final DoubleArrayList resultIntensities) {

    if (endIdx - startIdx < minNonZeroDp) {
      return;
    }

    final double maxIntensity = intensities[peakMaxIdx];
    final double detectionThreshold = Math.max(noiseLevel, 2 * absMinIntensity);

    // check the actual intensity as noise level not the area which is harder to optimize
    // the peak detection should pick the same peaks for the same noise level no matter
    // if AREA or HEIGHT is selected for intensity representation
    if (maxIntensity > detectionThreshold) {
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
    // We iterate strictly from startIdx to endIdx (exclusive), which are the boundaries
    // determined by the peak resolving logic (valleys or region edges).
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

    final double centroidMz = sumMzInt / sumIntForMz;
    final double finalIntensity =
        (intensityCalculation == AbundanceMeasure.Area) ? totalArea : maxIntensity;

    resultMzs.add(centroidMz);
    resultIntensities.add(finalIntensity);
  }

  @Override
  public @NotNull String getName() {
    return "Local maximum mass detector";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return LocalMaxMassDetectorParameters.class;
  }

  @Override
  public double[][] getMassValues(double[] mzs, double[] intensities,
      @NotNull MassSpectrumType type) {
    return getMassValues(new SimpleMassSpectrum(mzs, intensities, type));
  }
}