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

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Finds the chromatogram that contains a data point by its scan index and exact m/z.
 */
final class ChromatogramDataPointIndex {

  private final double[][] mzs;
  private final int[][] chromatogramIndices;

  ChromatogramDataPointIndex(@NotNull List<EvaluatedChromatogram> chromatograms, int numScans) {
    final List<DoubleArrayList> scanMzs = new ArrayList<>(numScans);
    final List<IntArrayList> scanChroms = new ArrayList<>(numScans);
    for (int s = 0; s < numScans; s++) {
      scanMzs.add(new DoubleArrayList());
      scanChroms.add(new IntArrayList());
    }
    for (int c = 0; c < chromatograms.size(); c++) {
      final EvaluatedChromatogram chrom = chromatograms.get(c);
      for (int i = 0; i < chrom.size(); i++) {
        scanMzs.get(chrom.scans()[i]).add(chrom.mzs()[i]);
        scanChroms.get(chrom.scans()[i]).add(c);
      }
    }
    mzs = new double[numScans][];
    chromatogramIndices = new int[numScans][];
    for (int s = 0; s < numScans; s++) {
      final double[] values = scanMzs.get(s).toDoubleArray();
      final int[] chroms = scanChroms.get(s).toIntArray();
      final int[] order = ChannelConsolidation.identity(values.length);
      IntArrays.quickSort(order, 0, values.length, (a, b) -> Double.compare(values[a], values[b]));
      mzs[s] = new double[values.length];
      chromatogramIndices[s] = new int[values.length];
      for (int i = 0; i < values.length; i++) {
        mzs[s][i] = values[order[i]];
        chromatogramIndices[s][i] = chroms[order[i]];
      }
    }
  }

  /**
   * @return index of the chromatogram with this data point or -1
   */
  int find(int scan, double mz) {
    final double[] values = mzs[scan];
    final int i = ChannelConsolidation.lowerBound(values, mz);
    return i < values.length && values[i] == mz ? chromatogramIndices[scan][i] : -1;
  }
}
