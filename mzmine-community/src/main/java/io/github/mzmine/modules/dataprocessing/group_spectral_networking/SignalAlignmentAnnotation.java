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

package io.github.mzmine.modules.dataprocessing.group_spectral_networking;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public enum SignalAlignmentAnnotation {
  MATCH, MODIFIED, FILTERED, NONE;

  /**
   * Classifies each pre-aligned pair as {@link #MATCH}, {@link #MODIFIED}, or {@link #NONE}. Pairs
   * whose library/query m/z fall within {@code mzTol} are {@link #MATCH}; pairs that align but fall
   * outside are {@link #MODIFIED} (assumed to align via the precursor-mass shift); pairs with a
   * {@code null} entry on either side are {@link #NONE}. {@link #FILTERED} is never produced here;
   * it is reserved for callers that mark pairs filtered out before alignment.
   *
   * @param aligned column-major aligned data with shape {@code [2][N]}: {@code aligned[0]} =
   *                library signals, {@code aligned[1]} = query signals (matching the layout
   *                produced by {@code ScanAlignment.convertBackToMassLists}).
   * @param mzTol   tolerance treated as "direct match"
   * @return per-pair classification of length {@code N}
   */
  public static SignalAlignmentAnnotation @NotNull [] classify(@NotNull final DataPoint[][] aligned,
      @NotNull final MZTolerance mzTol) {
    final int n = aligned.length >= 2 ? aligned[0].length : 0;
    final SignalAlignmentAnnotation[] out = new SignalAlignmentAnnotation[n];
    for (int i = 0; i < n; i++) {
      final DataPoint lib = aligned[0][i];
      final DataPoint query = aligned[1][i];
      out[i] = classifyPair(lib, query, mzTol);
    }
    return out;
  }

  /**
   * Row-major variant: classifies each pair in a {@code List<DataPoint[]>} where each entry is a
   * length-2 {@code [library, query]} pair.
   */
  public static SignalAlignmentAnnotation @NotNull [] classify(
      @NotNull final List<DataPoint[]> aligned, @NotNull final MZTolerance mzTol) {
    final SignalAlignmentAnnotation[] out = new SignalAlignmentAnnotation[aligned.size()];
    for (int i = 0; i < out.length; i++) {
      final DataPoint[] pair = aligned.get(i);
      out[i] = classifyPair(pair[0], pair[1], mzTol);
    }
    return out;
  }

  private static SignalAlignmentAnnotation classifyPair(final DataPoint lib, final DataPoint query,
      final MZTolerance mzTol) {
    if (lib == null || query == null) {
      return NONE;
    }
    return mzTol.checkWithinTolerance(lib.getMZ(), query.getMZ()) ? MATCH : MODIFIED;
  }
}
