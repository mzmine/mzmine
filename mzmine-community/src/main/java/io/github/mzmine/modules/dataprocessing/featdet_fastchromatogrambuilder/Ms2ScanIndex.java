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

package io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.util.scans.FragmentScanSorter;
import io.github.mzmine.util.scans.ScanUtils;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * MS2 scans of a raw data file sorted by precursor m/z. Finds the fragment scans of a chromatogram
 * with a binary search instead of testing all MS2 scans for every chromatogram. The result equals
 * {@link ScanUtils#streamAllMS2FragmentScans(RawDataFile, com.google.common.collect.Range,
 * com.google.common.collect.Range)} with closed ranges.
 */
final class Ms2ScanIndex {

  private final Scan[] scans;
  private final double[] precursorMzs;
  // position in the MS2 scan list of the file, restores the original order before sorting
  private final int[] fileOrder;

  Ms2ScanIndex(@NotNull RawDataFile dataFile) {
    final List<Scan> ms2Scans = dataFile.getScanNumbers(2);
    final List<Scan> withPrecursor = new ArrayList<>(ms2Scans.size());
    final DoubleArrayList precursors = new DoubleArrayList(ms2Scans.size());
    for (final Scan scan : ms2Scans) {
      final Double precursorMz = scan.getPrecursorMz();
      if (precursorMz != null) {
        withPrecursor.add(scan);
        precursors.add(precursorMz.doubleValue());
      }
    }
    final int n = withPrecursor.size();
    final double[] unsortedPrecursors = precursors.toDoubleArray();
    final int[] order = ChannelConsolidation.identity(n);
    IntArrays.quickSort(order, 0, n, (a, b) -> {
      final int result = Double.compare(unsortedPrecursors[a], unsortedPrecursors[b]);
      return result != 0 ? result : Integer.compare(a, b);
    });
    scans = new Scan[n];
    precursorMzs = new double[n];
    fileOrder = new int[n];
    for (int i = 0; i < n; i++) {
      scans[i] = withPrecursor.get(order[i]);
      precursorMzs[i] = unsortedPrecursors[order[i]];
      fileOrder[i] = order[i];
    }
  }

  /**
   * All bounds are inclusive like the closed ranges of the feature.
   *
   * @return all MS2 scans within both ranges, sorted by {@link FragmentScanSorter#DEFAULT_TIC}
   */
  @NotNull List<Scan> findFragmentScans(float minRt, float maxRt, double minPrecursorMz,
      double maxPrecursorMz) {
    final List<Integer> matches = new ArrayList<>();
    for (int i = ChannelConsolidation.lowerBound(precursorMzs, minPrecursorMz);
        i < precursorMzs.length && precursorMzs[i] <= maxPrecursorMz; i++) {
      final float rt = scans[i].getRetentionTime();
      if (minRt <= rt && rt <= maxRt) {
        matches.add(i);
      }
    }
    if (matches.isEmpty()) {
      return List.of();
    }
    // same order as the stream over all MS2 scans: file order, then the stable sort
    matches.sort((a, b) -> Integer.compare(fileOrder[a], fileOrder[b]));
    final List<Scan> result = new ArrayList<>(matches.size());
    for (final int match : matches) {
      result.add(scans[match]);
    }
    result.sort(FragmentScanSorter.DEFAULT_TIC);
    return result;
  }
}
