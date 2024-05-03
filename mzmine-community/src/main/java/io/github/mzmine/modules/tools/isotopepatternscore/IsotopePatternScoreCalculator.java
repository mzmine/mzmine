/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.tools.isotopepatternscore;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.scans.ScanUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class IsotopePatternScoreCalculator {

  public static boolean checkMatch(IsotopePattern ip1, IsotopePattern ip2,
      @NotNull MZTolerance mzTolerance, double noiseIntensity, double minimumScore) {

    double score = getSimilarityScore(ip1, ip2, mzTolerance, noiseIntensity);

    return score >= minimumScore;
  }

  /**
   * Returns a calculated similarity score of two isotope patterns in the range of 0 (not similar at
   * all) to 1 (100% same).
   */
  public static float getSimilarityScore(@NotNull MassSpectrum ip1, @NotNull MassSpectrum ip2,
      @NotNull MZTolerance mzTolerance, double noiseIntensity) {

    if(ip1.getNumberOfDataPoints() == 0 || ip2.getNumberOfDataPoints() == 0) {
      return 0f;
    }

    double pattern1Intensity = 0.0, pattern2Intensity = 0.0;
    if (ip1.getBasePeakIndex() >= 0) {
      pattern1Intensity = ip1.getIntensityValue(ip1.getBasePeakIndex());
    }
    if (ip2.getBasePeakIndex() >= 0) {
      pattern1Intensity = ip2.getIntensityValue(ip2.getBasePeakIndex());
    }
    final double patternIntensity = Math.max(pattern1Intensity, pattern2Intensity);


    // Merge the data points from both isotope patterns into a single array.
    // Data points from first pattern will have positive intensities, data
    // points from second pattern will have negative intensities.
    List<DataPoint> mergedDataPoints = new ArrayList<>();

    // Normalize the isotopes to intensity 0..1
    for (DataPoint dp : ScanUtils.normalizeSpectrum(ip1)) {
      if (dp.getIntensity() * patternIntensity < noiseIntensity) {
        continue;
      }
      mergedDataPoints.add(dp);
    }
    for (DataPoint dp : ScanUtils.normalizeSpectrum(ip2)) {
      if (dp.getIntensity() * patternIntensity < noiseIntensity) {
        continue;
      }
      DataPoint negativeDP = new SimpleDataPoint(dp.getMZ(), dp.getIntensity() * -1);
      mergedDataPoints.add(negativeDP);
    }
    DataPoint mergedDPArray[] = mergedDataPoints.toArray(new DataPoint[0]);

    // Sort the merged data points by m/z
    Arrays.sort(mergedDPArray, new DataPointSorter(SortingProperty.MZ, SortingDirection.Ascending));

    // Iterate the merged data points and sum all isotopes within m/z
    // tolerance
    for (int i = 0; i < mergedDPArray.length - 1; i++) {

      Range<Double> toleranceRange = mzTolerance.getToleranceRange(mergedDPArray[i].getMZ());

      if (!toleranceRange.contains(mergedDPArray[i + 1].getMZ())) {
        continue;
      }

      double summedIntensity =
          mergedDPArray[i].getIntensity() + mergedDPArray[i + 1].getIntensity();

      double newMZ = mergedDPArray[i + 1].getMZ();

      // Update the next data point and remove the current one
      mergedDPArray[i + 1] = new SimpleDataPoint(newMZ, summedIntensity);
      mergedDPArray[i] = null;

    }

    // Calculate the resulting score. Ideal score is 1, in case the final
    // data point array is empty.
    float result = 1f;

    for (DataPoint dp : mergedDPArray) {
      if (dp == null) {
        continue;
      }
      double remainingIntensity = Math.abs(dp.getIntensity());

      // In case some large isotopes were grouped together, the summed
      // intensity may be over 1
      if (remainingIntensity > 1) {
        remainingIntensity = 1;
      }

      // Decrease the score with each remaining peak
      result *= 1.f - remainingIntensity;
    }

    return result;
  }

}
