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

package io.github.mzmine.modules.visualization.chromatogram_comparison;

import com.google.common.collect.Range;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Chromatograms of both lists at about the same m/z, see {@link ChromatogramComparison}.
 *
 * @param id                  ascending with the m/z
 * @param mz                  mean m/z of the most intense chromatogram of each side
 * @param apexRt              retention time of the highest data point
 * @param differentDataPoints scans in which both sides have a different data point, one of them
 *                            above the threshold
 * @param issues              potential issues, iterates by severity
 * @param signalRtRange       retention time range of the signals above the threshold and of the
 *                            missing data points, null without data points
 */
public record ChromatogramGroup(int id, double mz, float apexRt, @NotNull GroupSide a,
                                @NotNull GroupSide b, int differentDataPoints,
                                @NotNull Set<ChromatogramIssue> issues,
                                @Nullable Range<Float> signalRtRange) {

  /**
   * The most severe issues first, then the most intense groups, which are the most reliable signals
   * of an issue.
   */
  public static final Comparator<ChromatogramGroup> SEVERITY_ORDER = Comparator.comparing(
          ChromatogramGroup::issues, ChromatogramIssue.SEVERITY_ORDER)
      .thenComparing(ChromatogramGroup::height, Comparator.reverseOrder())
      .thenComparingInt(ChromatogramGroup::id);

  public @NotNull GroupSide side(@NotNull ComparisonSide side) {
    return side == ComparisonSide.A ? a : b;
  }

  /**
   * @return the highest intensity of both sides
   */
  public double height() {
    return Math.max(Objects.requireNonNullElse(a.height(), 0d),
        Objects.requireNonNullElse(b.height(), 0d));
  }

  /**
   * @return m/z difference of B to A in ppm, null if a side is empty
   */
  public @Nullable Double deltaMzPpm() {
    final Double mzA = a.mz();
    final Double mzB = b.mz();
    if (mzA == null || mzB == null) {
      return null;
    }
    return (mzB - mzA) / mzA * 1E6;
  }

  public boolean hasIssues() {
    return !issues.isEmpty();
  }

  @NotNull ChromatogramGroup withId(int newId) {
    return new ChromatogramGroup(newId, mz, apexRt, a, b, differentDataPoints, issues,
        signalRtRange);
  }

  @NotNull ChromatogramGroup withSides(@NotNull GroupSide newA, @NotNull GroupSide newB) {
    return new ChromatogramGroup(id, mz, apexRt, newA, newB, differentDataPoints, issues,
        signalRtRange);
  }
}
