/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.tools.isotopepatternscore;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
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
  public static float getSimilarityScore(@NotNull IsotopePattern ip1, @NotNull IsotopePattern ip2,
      @NotNull MZTolerance mzTolerance, double noiseIntensity) {

    double pattern1Intensity = 0.0, pattern2Intensity = 0.0;
    if (ip1.getBasePeakIndex() >= 0) {
      pattern1Intensity = ip1.getIntensityValue(ip1.getBasePeakIndex());
    }
    if (ip2.getBasePeakIndex() >= 0) {
      pattern1Intensity = ip2.getIntensityValue(ip2.getBasePeakIndex());
    }
    final double patternIntensity = Math.max(pattern1Intensity, pattern2Intensity);

    // Normalize the isotopes to intensity 0..1
    IsotopePattern nip1 = IsotopePatternCalculator.normalizeIsotopePattern(ip1);
    IsotopePattern nip2 = IsotopePatternCalculator.normalizeIsotopePattern(ip2);

    // Merge the data points from both isotope patterns into a single array.
    // Data points from first pattern will have positive intensities, data
    // points from second pattern will have negative intensities.
    List<DataPoint> mergedDataPoints = new ArrayList<>();
    for (DataPoint dp : ScanUtils.extractDataPoints(nip1)) {
      if (dp.getIntensity() * patternIntensity < noiseIntensity) {
        continue;
      }
      mergedDataPoints.add(dp);
    }
    for (DataPoint dp : ScanUtils.extractDataPoints(nip2)) {
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
