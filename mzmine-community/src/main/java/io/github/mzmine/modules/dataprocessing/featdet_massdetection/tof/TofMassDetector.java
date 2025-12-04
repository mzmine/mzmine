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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

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

  public TofMassDetector(double noiseLevel, AbundanceMeasure intensityCalculation) {
    this.noiseLevel = noiseLevel;
    this.intensityCalculation = intensityCalculation;
  }

  @Override
  public MassDetector create(ParameterSet params) {
    return new TofMassDetector(params.getValue(TofMassDetectorParameters.noiseLevel),
        params.getValue(TofMassDetectorParameters.intensityCalculation));
  }

  @Override
  public boolean filtersActive() {
    return false;
  }

  @Override
  public double[][] getMassValues(MassSpectrum spectrum) {
    if (spectrum.getNumberOfDataPoints() < 3) {
      return new double[2][0];
    }

    final double maxDiff = getMaxMzDiff(spectrum);

    final List<IndexRange> consecutiveRanges = new ArrayList<>();

    // 1. Detect continuous regions based on m/z density
    int currentRegionStart = 0;
    double lastMz = spectrum.getMzValue(0);

    for (int i = 1; i < spectrum.getNumberOfDataPoints(); i++) {
      final double thisMz = spectrum.getMzValue(i);
      final double mzDelta = thisMz - lastMz;

      // If the gap is too large, we close the current region and start a new one
      if (mzDelta >= maxDiff) {
        // Only add regions that contain enough data points to form a peak (e.g., > 2 points)
        if (i - currentRegionStart > 2) {
          consecutiveRanges.add(new SimpleIndexRange(currentRegionStart, i));
        }
        currentRegionStart = i;
      }

      lastMz = thisMz;
    }

    // Add the final region if valid
    if (spectrum.getNumberOfDataPoints() - currentRegionStart > 2) {
      consecutiveRanges.add(
          new SimpleIndexRange(currentRegionStart, spectrum.getNumberOfDataPoints()));
    }

    // 2. Process regions to find centroids
    DoubleArrayList resultMzs = new DoubleArrayList();
    DoubleArrayList resultIntensities = new DoubleArrayList();

    for (IndexRange range : consecutiveRanges) {
      findAndCentroidPeaks(spectrum, range, resultMzs, resultIntensities);
    }

    // 3. Convert results to double[][] format
    double[][] result = new double[2][];
    result[0] = resultMzs.toDoubleArray();
    result[1] = resultIntensities.toDoubleArray();

    return result;
  }

  /**
   * Identifies peaks within a continuous region using the valley/rise logic, then centroids them.
   */
  private void findAndCentroidPeaks(MassSpectrum spectrum, IndexRange range,
      DoubleArrayList resultMzs, DoubleArrayList resultIntensities) {

    // 1. Find all raw local maxima (candidates) in the region above noise
    IntArrayList candidateIndices = findLocalMaximaIndices(spectrum, range);

    if (candidateIndices.isEmpty()) {
      return;
    }

    // 2. Filter and merge candidates based on the 0.7 / 1.3 rule
    // We walk through candidates. 'activePeakIdx' represents the highest point of the current peak being resolved.
    // 'leftBoundary' tracks where the current peak started (valley or region start).

    int activePeakIdx = candidateIndices.getInt(0);
    int leftBoundary = range.min(); // Start of the region

    for (int i = 1; i < candidateIndices.size(); i++) {
      int nextCandidateIdx = candidateIndices.getInt(i);

      // Find the deepest valley between the current active peak and the next candidate
      int valleyIdx = findLowestValleyIndex(spectrum, activePeakIdx, nextCandidateIdx);

      double activePeakInt = spectrum.getIntensityValue(activePeakIdx);
      double nextCandidateInt = spectrum.getIntensityValue(nextCandidateIdx);
      double valleyInt = spectrum.getIntensityValue(valleyIdx);

      // Rule: Separate if it drops below 0.7 * active AND rises 1.3 * valley
      boolean dropsEnough = valleyInt < (VALLEY_FACTOR * activePeakInt);
      boolean risesEnough = nextCandidateInt > (RISE_FACTOR * valleyInt);

      if (dropsEnough && risesEnough) {
        // They are separate peaks.
        // Process the current active peak.
        // The right boundary for the current peak is the valley.
        processSinglePeak(spectrum, activePeakIdx, leftBoundary, valleyIdx, resultMzs,
            resultIntensities);

        // Move to the next peak
        activePeakIdx = nextCandidateIdx;
        leftBoundary = valleyIdx; // Next peak starts from this valley
      } else {
        // They are not resolved enough; merge them.
        // We consider them one peak structure.
        // Update the active peak index to be the higher of the two (centroiding favors the true max).
        if (nextCandidateInt > activePeakInt) {
          activePeakIdx = nextCandidateIdx;
        }
        // leftBoundary remains unchanged.
      }
    }

    // Process the final confirmed peak
    processSinglePeak(spectrum, activePeakIdx, leftBoundary, range.maxExclusive(), resultMzs,
        resultIntensities);
  }

  /**
   * Helper to find all indices in range that are local maxima > noise.
   */
  private IntArrayList findLocalMaximaIndices(MassSpectrum spectrum, IndexRange range) {
    IntArrayList indices = new IntArrayList();
    int start = range.min();
    int end = range.maxExclusive();

    for (int i = start; i < end; i++) {
      double currentInt = spectrum.getIntensityValue(i);
      if (currentInt < noiseLevel) {
        continue;
      }

      double leftInt = (i == start) ? 0 : spectrum.getIntensityValue(i - 1);
      double rightInt = (i == end - 1) ? 0 : spectrum.getIntensityValue(i + 1);

      // Check local max (handle flat tops by taking the first point)
      if (currentInt > leftInt && currentInt >= rightInt) {
        indices.add(i);
      }
    }
    return indices;
  }

  /**
   * Finds the index with minimum intensity between two indices.
   */
  private int findLowestValleyIndex(MassSpectrum spectrum, int startIdx, int endIdx) {
    int minIdx = startIdx;
    double minVal = Double.MAX_VALUE;

    // Search strictly between the peaks
    for (int i = startIdx + 1; i < endIdx; i++) {
      double val = spectrum.getIntensityValue(i);
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
   * @param peakMaxIdx The index of the maximum intensity within these bounds.
   * @param startIdx   Inclusive start of peak integration/centroiding.
   * @param endIdx     Exclusive end of peak integration/centroiding.
   */
  private void processSinglePeak(MassSpectrum spectrum, int peakMaxIdx, int startIdx, int endIdx,
      DoubleArrayList mzs, DoubleArrayList intensities) {

    double maxIntensity = spectrum.getIntensityValue(peakMaxIdx);

    // M/z Calculation: Weighted average using ONLY points > 0.4 * maxIntensity
    double mzThreshold = maxIntensity * MZ_WEIGHTING_THRESHOLD;
    double sumMzInt = 0.0;
    double sumIntForMz = 0.0;

    // Intensity Calculation: Area (sum of all points in bounds) or Height (maxIntensity)
    double totalArea = 0.0;

    for (int i = startIdx; i < endIdx; i++) {
      double intensity = spectrum.getIntensityValue(i);
      double mz = spectrum.getMzValue(i);

      // Accumulate area for the whole defined region
      totalArea += intensity;

      // Accumulate weighted average stats only if above threshold
      if (intensity > mzThreshold) {
        sumMzInt += (mz * intensity);
        sumIntForMz += intensity;
      }
    }

    if (sumIntForMz == 0) {
      // Should not happen given logic, but safety check
      return;
    }

    double centroidMz = sumMzInt / sumIntForMz;
    double finalIntensity =
        (intensityCalculation == AbundanceMeasure.Area) ? totalArea : maxIntensity;

    mzs.add(centroidMz);
    intensities.add(finalIntensity);
  }

  private static double getMaxMzDiff(MassSpectrum spectrum) {
    for (int i = spectrum.getNumberOfDataPoints() - 1; i > 1; i--) {
      // tof mz value distances are proportional to sqrt(m/z)
      // so the biggest mass diff will be at the top of the spectrum
      if (Double.compare(spectrum.getIntensityValue(i), 0) != 0 || !(
          spectrum.getIntensityValue(i - 1) > 0)) {
        continue;
      }
      return Math.abs(spectrum.getMzValue(i) - spectrum.getMzValue(i - 1));
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