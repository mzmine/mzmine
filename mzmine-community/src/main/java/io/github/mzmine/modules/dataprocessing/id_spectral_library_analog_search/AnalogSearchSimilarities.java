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

package io.github.mzmine.modules.dataprocessing.id_spectral_library_analog_search;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.maths.similarity.Similarity;
import io.github.mzmine.util.scans.ScanAlignment;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.similarity.SpectralSimilarity;
import io.github.mzmine.util.scans.similarity.Weights;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Shared similarity helpers used by both the cosine and the ML analog-search tasks. The ML task
 * also needs modified-cosine for the fallback "visualization" similarity attached to each ML
 * match.
 */
final class AnalogSearchSimilarities {

  static final String FN_FALLBACK_COSINE = "Modified cosine (fallback for ML match)";

  private AnalogSearchSimilarities() {
  }

  /**
   * Aligns query vs library data points (modification-aware when both precursors are provided),
   * computes the weighted cosine, and packages the result with aligned data so the UI mirror plot
   * can render. Returns {@code null} if {@code overlap < minOverlap}.
   */
  static @Nullable SpectralSimilarity computeModifiedCosine(final DataPoint[] querySorted,
      final DataPoint[] librarySorted, final MZTolerance mzTol, final int minOverlap,
      final double rowPrecursorMz, final Double libraryPrecursorMz,
      @NotNull final String functionName) {
    final List<DataPoint[]> aligned;
    if (rowPrecursorMz > 0 && libraryPrecursorMz != null && libraryPrecursorMz > 0) {
      aligned = ScanAlignment.alignOfSortedModAware(mzTol, librarySorted, querySorted,
          libraryPrecursorMz, rowPrecursorMz);
    } else {
      aligned = ScanAlignment.alignOfSorted(mzTol, librarySorted, querySorted);
    }

    int overlap = 0;
    for (final DataPoint[] pair : aligned) {
      if (pair[0] != null && pair[1] != null) {
        overlap++;
      }
    }
    if (overlap < minOverlap) {
      return null;
    }

    final double[][] matrix = ScanAlignment.toIntensityMatrixWeighted(aligned,
        Weights.SQRT.getIntensity(), Weights.SQRT.getMz());
    final double cosine = Similarity.COSINE.calc(matrix);

    // need to clone the data points as the similarity calculation sorts by mz
    return new SpectralSimilarity(functionName, cosine, overlap, librarySorted.clone(),
        querySorted.clone(), aligned);
  }

  /**
   * Cosine fallback for ML matches: same as {@link #computeModifiedCosine} but with
   * {@code minOverlap = 1} so a {@link SpectralSimilarity} is produced whenever at least one peak
   * aligns within {@code mzTol}. Returns {@code null} when the library entry has no data points or
   * when zero peaks align.
   */
  static @Nullable SpectralSimilarity computeFallbackCosine(final DataPoint[] queryDpsSorted,
      final SpectralLibraryEntry entry, final MZTolerance mzTol, final double rowPrecursor) {
    final DataPoint[] entryDps = entry.getDataPoints();
    if (entryDps == null || entryDps.length == 0) {
      return null;
    }
    final DataPoint[] entryDpsSorted = entryDps.clone();
    Arrays.sort(entryDpsSorted, DataPointSorter.DEFAULT_INTENSITY);
    final Double libPrecursor = entry.getPrecursorMZ();
    return computeModifiedCosine(queryDpsSorted.clone(), entryDpsSorted, mzTol, 1, rowPrecursor,
        libPrecursor, FN_FALLBACK_COSINE);
  }

  static DataPoint[] sortAndCopyScan(final Scan scan) {
    if (scan.getMassList() == null) {
      throw new MissingMassListException(scan);
    }
    @NotNull DataPoint[] dps = ScanUtils.extractDataPoints(scan.getMassList());
    Arrays.sort(dps, DataPointSorter.DEFAULT_INTENSITY);
    return dps;
  }
}
