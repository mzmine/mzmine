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
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Compares detected chromatograms with the ground truth of {@link SyntheticLcmsData}. An ion is
 * detectable if its own data points pass the chromatogram filters. For each detectable ion the main
 * chromatogram is the one with most of its data points. A coalesced centroid is a data point of
 * both of its ions.
 */
final class GroundTruthEvaluator {

  private GroundTruthEvaluator() {
  }

  /**
   * @param detectableIons   ions that pass the filters on their own data points
   * @param foundIons        detectable ions whose apex data point is in their main chromatogram
   * @param meanCompleteness mean fraction of the data points of an ion in its main chromatogram
   * @param splitIons        ions with at least 10% of their data points in a second chromatogram
   * @param holes            data points of an ion missing in its main chromatogram in a scan
   *                         without any data point in the main chromatogram
   * @param replaced         data points of an ion missing in its main chromatogram because another
   *                         signal took the scan
   * @param foreign          data points of other ions or noise within the scan range of the ion in
   *                         its main chromatogram
   * @param chromatograms    number of chromatograms
   * @param unmatched        chromatograms that are not the main chromatogram of any detectable ion
   */
  record Summary(int detectableIons, int foundIons, double meanCompleteness, int splitIons,
                 long holes, long replaced, long foreign, int chromatograms, int unmatched) {

    @Override
    public @NotNull String toString() {
      return "detectable=%d found=%d completeness=%.4f split=%d holes=%d replaced=%d foreign=%d chromatograms=%d unmatched=%d".formatted(
          detectableIons, foundIons, meanCompleteness, splitIons, holes, replaced, foreign,
          chromatograms, unmatched);
    }
  }

  @NotNull
  static Summary evaluate(@NotNull SyntheticLcmsData data,
      @NotNull List<EvaluatedChromatogram> chromatograms, int minConsecutive,
      double minGroupIntensity, double minHeight) {
    final int numIons = data.ions.size();
    final int numScans = data.numScans();

    // data points of each ion in the data
    final List<IntArrayList> ionScans = new ArrayList<>();
    final List<DoubleArrayList> ionIntensities = new ArrayList<>();
    for (int k = 0; k < numIons; k++) {
      ionScans.add(new IntArrayList());
      ionIntensities.add(new DoubleArrayList());
    }
    for (int s = 0; s < numScans; s++) {
      for (int i = 0; i < data.labels[s].length; i++) {
        // a coalesced centroid is a data point of both ions
        for (final int label : new int[]{data.labels[s][i], data.sharedLabels[s][i]}) {
          if (label >= 0) {
            ionScans.get(label).add(s);
            ionIntensities.get(label).add(data.intensities[s][i]);
          }
        }
      }
    }

    // data point indices in the scans of all chromatogram data points and ion counts per
    // chromatogram
    final List<int[]> chromIndices = new ArrayList<>();
    final List<Int2IntOpenHashMap> ionCounts = new ArrayList<>();
    for (final EvaluatedChromatogram chrom : chromatograms) {
      final int[] indices = new int[chrom.scans().length];
      final Int2IntOpenHashMap counts = new Int2IntOpenHashMap();
      for (int i = 0; i < indices.length; i++) {
        final int s = chrom.scans()[i];
        final int index = data.findIndex(s, chrom.mzs()[i]);
        if (index < 0) {
          throw new IllegalStateException("Chromatogram data point not in the data");
        }
        indices[i] = index;
        for (final int label : new int[]{data.labels[s][index], data.sharedLabels[s][index]}) {
          if (label >= 0) {
            counts.addTo(label, 1);
          }
        }
      }
      chromIndices.add(indices);
      ionCounts.add(counts);
    }

    int detectable = 0;
    int found = 0;
    int split = 0;
    long holes = 0;
    long replaced = 0;
    long foreign = 0;
    double completenessSum = 0;
    final boolean[] isMain = new boolean[chromatograms.size()];
    final int[] mainScanIndex = new int[numScans];

    for (int k = 0; k < numIons; k++) {
      final int[] scans = ionScans.get(k).toIntArray();
      final double[] intensities = ionIntensities.get(k).toDoubleArray();
      if (scans.length == 0 || !FastChromatogramBuilder.passesFilters(scans, intensities,
          scans.length, minConsecutive, minGroupIntensity, minHeight)) {
        continue;
      }
      detectable++;

      int main = -1;
      int mainCount = 0;
      int significant = 0;
      final int significantCount = Math.max(2, (int) Math.ceil(0.1 * scans.length));
      for (int c = 0; c < chromatograms.size(); c++) {
        final int count = ionCounts.get(c).get(k);
        if (count >= significantCount) {
          significant++;
        }
        if (count > mainCount) {
          mainCount = count;
          main = c;
        }
      }
      if (significant > 1) {
        split++;
      }
      if (main < 0) {
        continue;
      }
      isMain[main] = true;
      completenessSum += (double) mainCount / scans.length;

      // apex data point in main chromatogram
      int apex = 0;
      for (int i = 1; i < intensities.length; i++) {
        if (intensities[i] > intensities[apex]) {
          apex = i;
        }
      }
      // data point index of the main chromatogram in each scan
      Arrays.fill(mainScanIndex, -1);
      final EvaluatedChromatogram chrom = chromatograms.get(main);
      final int[] indices = chromIndices.get(main);
      for (int i = 0; i < chrom.scans().length; i++) {
        mainScanIndex[chrom.scans()[i]] = indices[i];
      }
      if (belongsTo(data, mainScanIndex, scans[apex], k)) {
        found++;
      }
      final int first = scans[0];
      final int last = scans[scans.length - 1];
      for (final int s : scans) {
        if (belongsTo(data, mainScanIndex, s, k)) {
          continue;
        }
        if (mainScanIndex[s] < 0) {
          holes++;
        } else {
          replaced++;
        }
      }
      for (int s = first; s <= last; s++) {
        if (mainScanIndex[s] >= 0 && !belongsTo(data, mainScanIndex, s, k)) {
          foreign++;
        }
      }
    }

    int unmatched = 0;
    for (final boolean main : isMain) {
      if (!main) {
        unmatched++;
      }
    }
    return new Summary(detectable, found, detectable == 0 ? 0 : completenessSum / detectable, split,
        holes, replaced, foreign, chromatograms.size(), unmatched);
  }

  /**
   * @param scanIndex data point index of a chromatogram in each scan, -1 for none
   * @return true if the chromatogram has a data point of the ion in the scan
   */
  private static boolean belongsTo(@NotNull SyntheticLcmsData data, @NotNull int[] scanIndex,
      int scan, int ion) {
    return scanIndex[scan] >= 0 && data.belongsTo(scan, scanIndex[scan], ion);
  }
}
