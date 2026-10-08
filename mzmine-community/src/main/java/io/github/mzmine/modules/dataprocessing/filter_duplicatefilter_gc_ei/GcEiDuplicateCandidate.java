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

package io.github.mzmine.modules.dataprocessing.filter_duplicatefilter_gc_ei;

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc.SpectralDeconvolutionUtils;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.Comparator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A GC-EI row with the values needed to find duplicates.
 *
 * @param row          the row
 * @param quantifierMz the m/z of the row
 * @param rt           the retention time of the row
 * @param spectrum     the merged pseudo spectra of all features, sorted by m/z
 * @param numDetected  number of detected or manually integrated features
 * @param maxHeight    the highest feature of the row
 */
record GcEiDuplicateCandidate(@NotNull FeatureListRow row, double quantifierMz, float rt,
                              @NotNull DataPoint[] spectrum, int numDetected, float maxHeight) {

  /**
   * The best row of a group of duplicates is kept: most detected features, then most features in
   * total, then the highest feature. Lower row ID for reproducible results.
   */
  static final Comparator<GcEiDuplicateCandidate> BEST_FIRST = Comparator.comparingInt(
          GcEiDuplicateCandidate::numDetected).reversed()
      .thenComparing(c -> c.row().getNumberOfFeatures(), Comparator.reverseOrder())
      .thenComparing(GcEiDuplicateCandidate::maxHeight, Comparator.reverseOrder())
      .thenComparingInt(c -> c.row().getID());

  /**
   * @param mzTolerance tolerance to merge the pseudo spectra of the row
   * @return the candidate or null if the row has no pseudo spectrum
   */
  static @Nullable GcEiDuplicateCandidate fromRow(@NotNull final FeatureListRow row,
      @NotNull final MZTolerance mzTolerance) {
    if (!row.hasMs2Fragmentation()) {
      return null;
    }
    final double[][] merged = SpectralDeconvolutionUtils.mergePseudoSpectra(row, mzTolerance);
    final DataPoint[] spectrum = new DataPoint[merged[0].length];
    for (int i = 0; i < spectrum.length; i++) {
      spectrum[i] = new SimpleDataPoint(merged[0][i], merged[1][i]);
    }

    int numDetected = 0;
    for (final Feature feature : row.getFeatures()) {
      if (isDetected(feature.getFeatureStatus())) {
        numDetected++;
      }
    }
    return new GcEiDuplicateCandidate(row, row.getAverageMZ(), row.getAverageRT(), spectrum,
        numDetected, requireNonNullElse(row.getMaxHeight(), 0f));
  }

  static boolean isDetected(@NotNull final FeatureStatus status) {
    return switch (status) {
      case DETECTED, MANUAL -> true;
      case UNKNOWN, ESTIMATED, COMPOUND_AGGREGATED -> false;
    };
  }

  /**
   * @return true if the spectrum contains a signal within the tolerance of the m/z
   */
  boolean containsMz(final double mz, @NotNull final MZTolerance mzTolerance) {
    for (final DataPoint dp : spectrum) {
      if (mzTolerance.checkWithinTolerance(mz, dp.getMZ())) {
        return true;
      }
    }
    return false;
  }
}
