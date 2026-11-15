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

package io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.fragmentation;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Finds the most intense observed centroid within tolerance of an exact fragment mass.
 * <p>
 * The mass list is expected to be sorted by m/z. A binary search locates the lower tolerance bound,
 * after which only points inside the bounded window are inspected. Conventional lipid rules and
 * OAD prediction share this class so their peak-selection semantics remain identical.
 */
public class MassListPeakFinder {

  private final @Nullable MassList massList;
  private final @NotNull MZTolerance mzTolerance;

  public MassListPeakFinder(final @Nullable MassList massList,
      final @NotNull MZTolerance mzTolerance) {
    this.massList = massList;
    this.mzTolerance = mzTolerance;
  }

  /**
   * Finds the most intense peak that mutually satisfies the configured absolute/ppm tolerance.
   *
   * @return a value with {@link BestDataPoint#fragmentMatched()} false when no peak matched or the
   *         mass list is absent
   */
  public @NotNull BestDataPoint findBest(final double mzExact) {
    if (massList == null) {
      return BestDataPoint.NO_MATCH;
    }

    final Range<Double> toleranceRange = mzTolerance.getToleranceRange(mzExact);
    final int index = massList.binarySearch(toleranceRange.lowerEndpoint(),
        DefaultTo.GREATER_EQUALS);
    if (index < 0) {
      return BestDataPoint.NO_MATCH;
    }

    double maxIntensity = 0d;
    double bestMzValue = 0d;
    boolean fragmentMatched = false;
    final int numberOfDataPoints = massList.getNumberOfDataPoints();
    for (int i = index; i < numberOfDataPoints; i++) {
      final double mzValue = massList.getMzValue(i);
      if (mzValue > toleranceRange.upperEndpoint()) {
        break;
      }
      final double intensity = massList.getIntensityValue(i);
      if (mzTolerance.getToleranceRange(mzValue).contains(mzExact) && intensity > maxIntensity) {
        maxIntensity = intensity;
        bestMzValue = mzValue;
        fragmentMatched = true;
      }
    }
    return new BestDataPoint(fragmentMatched, bestMzValue, maxIntensity);
  }

  /** Result of one exact-mass probe without allocating a {@link io.github.mzmine.datamodel.DataPoint}. */
  public record BestDataPoint(boolean fragmentMatched, double mzValue, double intensity) {

    private static final @NotNull BestDataPoint NO_MATCH = new BestDataPoint(false, 0d, 0d);
  }
}
