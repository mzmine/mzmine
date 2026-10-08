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

package io.github.mzmine.modules.dataprocessing.gapfill_gc_ei;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc.SpectralDeconvolutionUtils;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.Comparator;
import java.util.stream.IntStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The signals of a feature list row that need to be re-detected in a raw data file with a gap.
 *
 * @param row          the row to gap-fill
 * @param quantifierMz the m/z of the row. The feature of this m/z is added to the row
 * @param rt           the retention time of the row
 * @param topMzs       the most intense signals of the pseudo spectra of the row, sorted by
 *                     decreasing intensity
 */
public record GcEiGapFillTarget(@NotNull FeatureListRow row, double quantifierMz, float rt,
                                double @NotNull [] topMzs) {

  /**
   * @param numTopSignals number of most intense signals to extract from the pseudo spectra
   * @param mzTolerance   tolerance to merge the pseudo spectra of all features of the row
   * @return the target or null if the row has no pseudo spectrum
   */
  public static @Nullable GcEiGapFillTarget fromRow(@NotNull final FeatureListRow row,
      final int numTopSignals, @NotNull final MZTolerance mzTolerance) {
    if (!row.hasMs2Fragmentation()) {
      return null;
    }

    // decision: merge the pseudo spectra of all samples, so that the top signals represent the
    // consensus spectrum of the row and not a single (potentially co-eluting) sample
    final double[][] merged = SpectralDeconvolutionUtils.mergePseudoSpectra(row, mzTolerance);
    final double[] mzs = merged[0];
    final double[] intensities = merged[1];

    final double[] topMzs = IntStream.range(0, mzs.length).boxed()
        .sorted(Comparator.comparingDouble((Integer i) -> intensities[i]).reversed())
        .limit(numTopSignals).mapToDouble(i -> mzs[i]).toArray();

    return new GcEiGapFillTarget(row, row.getAverageMZ(), row.getAverageRT(), topMzs);
  }
}
