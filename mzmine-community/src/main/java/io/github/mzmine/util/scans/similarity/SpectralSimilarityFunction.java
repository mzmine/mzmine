/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.util.scans.similarity;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.scans.ScanAlignment;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract class to implement differnt spactal similarity functions to match 2 spectra
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public abstract class SpectralSimilarityFunction implements MZmineModule {

  /**
   * @param mzTol
   * @param minMatch minimum overlap in signals
   * @return A spectra similarity if all requirements were met - otherwise null
   */
  @Nullable
  public abstract SpectralSimilarity getSimilarity(MZTolerance mzTol, int minMatch,
      DataPoint[] library, DataPoint[] query);

  /**
   * Align two mass lists. Override if alignement is changed in a specific spectral similarity
   * function.
   *
   * @param mzTol
   * @param a
   * @param b
   * @return
   */
  public static List<DataPoint[]> alignDataPoints(MZTolerance mzTol, DataPoint[] a, DataPoint[] b) {
    return ScanAlignment.align(mzTol, a, b);
  }

  /**
   * Calculate overlap
   *
   * @param aligned
   * @return
   */
  protected static int calcOverlap(List<DataPoint[]> aligned) {
    return (int) aligned.stream().filter(dp -> dp[0] != null && dp[1] != null).count();
  }

  /**
   * Remove unaligned signals (not present in all masslists)
   *
   * @param aligned
   * @return
   */
  protected static List<DataPoint[]> removeUnaligned(List<DataPoint[]> aligned) {
    return ScanAlignment.removeUnaligned(aligned);
  }
}
