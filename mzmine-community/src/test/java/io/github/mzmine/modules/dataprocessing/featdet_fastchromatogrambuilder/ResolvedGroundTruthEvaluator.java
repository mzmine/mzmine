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
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Compares resolved features, the final result of chromatogram builder and resolver, with the ground
 * truth of {@link SyntheticLcmsData}. Each feature belongs to the ion with the most intensity among
 * its data points, a coalesced centroid counts for both of its ions. Features with more noise
 * intensity than intensity of any ion are false features. The main feature of an ion is its feature
 * with the most intensity of the ion.
 */
final class ResolvedGroundTruthEvaluator {

  private ResolvedGroundTruthEvaluator() {
  }

  /**
   * @param detectableIons      ions whose own data points pass the builder filters
   * @param found               detectable ions with at least one feature
   * @param split               detectable ions with more than one feature, one peak reported
   *                            several times
   * @param features            number of resolved features
   * @param falseFeatures       features of mostly noise
   * @param medianRecovery      median fraction of the intensity of a found ion in its main feature
   * @param p10Recovery         10% quantile of this fraction, the worst found ions
   * @param apexElsewhere       found ions whose main feature misses the apex data point of the ion
   * @param intenseHoles        data points of found ions of at least {@link #INTENSE_FRACTION} of
   *                            the ion height that are within the scan range of the main feature but
   *                            not in it
   * @param ionsWithHoles       found ions with at least one intense hole
   * @param medianMzErrorPpm    median absolute error of the feature m/z of the main features to the
   *                            m/z of the ion
   * @param p95MzErrorPpm       95% quantile of this error
   * @param contaminated        main features with more than {@link #MAX_FOREIGN_FRACTION} of their
   *                            intensity from other ions or noise
   */
  record Summary(int detectableIons, int found, int split, int features, int falseFeatures,
                 double medianRecovery, double p10Recovery, int apexElsewhere, long intenseHoles,
                 int ionsWithHoles, double medianMzErrorPpm, double p95MzErrorPpm,
                 int contaminated) {

    int missed() {
      return detectableIons - found;
    }
  }

  static final double INTENSE_FRACTION = 0.1;
  static final double MAX_FOREIGN_FRACTION = 0.05;

  /**
   * @param features   detected data points of the resolved features
   * @param featureMzs m/z of each feature in the feature list, same order
   */
  @NotNull
  static Summary evaluate(@NotNull SyntheticLcmsData data,
      @NotNull List<EvaluatedChromatogram> features, @NotNull double[] featureMzs,
      int minConsecutive, double minGroupIntensity, double minHeight) {
    final int numIons = data.ions.size();
    final List<IntArrayList> ionScans = new ArrayList<>(numIons);
    final List<DoubleArrayList> ionIntensities = new ArrayList<>(numIons);
    for (int k = 0; k < numIons; k++) {
      ionScans.add(new IntArrayList());
      ionIntensities.add(new DoubleArrayList());
    }
    for (int s = 0; s < data.numScans(); s++) {
      for (int i = 0; i < data.labels[s].length; i++) {
        for (final int label : new int[]{data.labels[s][i], data.sharedLabels[s][i]}) {
          if (label >= 0) {
            ionScans.get(label).add(s);
            ionIntensities.get(label).add(data.intensities[s][i]);
          }
        }
      }
    }

    // the ion of each feature
    final int numFeatures = features.size();
    final int[] featureIon = new int[numFeatures];
    final double[] featureIonIntensity = new double[numFeatures];
    final double[] featureTotal = new double[numFeatures];
    final int[][] featureIndices = new int[numFeatures][];
    int falseFeatures = 0;
    for (int f = 0; f < numFeatures; f++) {
      final EvaluatedChromatogram feature = features.get(f);
      final Int2DoubleOpenHashMap ionSums = new Int2DoubleOpenHashMap();
      final int[] indices = new int[feature.size()];
      double noise = 0;
      double total = 0;
      for (int i = 0; i < feature.size(); i++) {
        final int s = feature.scans()[i];
        final int index = data.findIndex(s, feature.mzs()[i]);
        if (index < 0) {
          throw new IllegalStateException("Feature data point not in the data");
        }
        indices[i] = index;
        final double intensity = feature.intensities()[i];
        total += intensity;
        final int label = data.labels[s][index];
        if (label < 0) {
          noise += intensity;
          continue;
        }
        ionSums.addTo(label, intensity);
        if (data.sharedLabels[s][index] >= 0) {
          ionSums.addTo(data.sharedLabels[s][index], intensity);
        }
      }
      featureIndices[f] = indices;
      featureTotal[f] = total;
      int best = -1;
      double bestSum = 0;
      for (final var entry : ionSums.int2DoubleEntrySet()) {
        if (entry.getDoubleValue() > bestSum || (entry.getDoubleValue() == bestSum
            && entry.getIntKey() < best)) {
          best = entry.getIntKey();
          bestSum = entry.getDoubleValue();
        }
      }
      if (best < 0 || noise > bestSum) {
        falseFeatures++;
        featureIon[f] = SyntheticLcmsData.NOISE;
        continue;
      }
      featureIon[f] = best;
      featureIonIntensity[f] = bestSum;
    }

    final List<IntArrayList> ionFeatures = new ArrayList<>(numIons);
    for (int k = 0; k < numIons; k++) {
      ionFeatures.add(new IntArrayList());
    }
    for (int f = 0; f < numFeatures; f++) {
      if (featureIon[f] >= 0) {
        ionFeatures.get(featureIon[f]).add(f);
      }
    }

    int detectable = 0;
    int found = 0;
    int split = 0;
    int apexElsewhere = 0;
    long intenseHoles = 0;
    int ionsWithHoles = 0;
    int contaminated = 0;
    final DoubleArrayList recoveries = new DoubleArrayList();
    final DoubleArrayList mzErrors = new DoubleArrayList();
    for (int k = 0; k < numIons; k++) {
      final int[] scans = ionScans.get(k).toIntArray();
      final double[] intensities = ionIntensities.get(k).toDoubleArray();
      if (scans.length == 0 || !FastChromatogramBuilder.passesFilters(scans, intensities,
          scans.length, minConsecutive, minGroupIntensity, minHeight)) {
        continue;
      }
      detectable++;
      final IntArrayList own = ionFeatures.get(k);
      if (own.isEmpty()) {
        continue;
      }
      found++;
      if (own.size() > 1) {
        split++;
      }
      int main = own.getInt(0);
      for (int j = 1; j < own.size(); j++) {
        if (featureIonIntensity[own.getInt(j)] > featureIonIntensity[main]) {
          main = own.getInt(j);
        }
      }

      double ionTotal = 0;
      int apex = 0;
      for (int i = 0; i < intensities.length; i++) {
        ionTotal += intensities[i];
        if (intensities[i] > intensities[apex]) {
          apex = i;
        }
      }
      recoveries.add(featureIonIntensity[main] / ionTotal);
      final double ionMz = data.ions.get(k).mz();
      mzErrors.add(Math.abs(featureMzs[main] - ionMz) / ionMz * 1E6);
      if (featureTotal[main] - featureIonIntensity[main]
          > MAX_FOREIGN_FRACTION * featureTotal[main]) {
        contaminated++;
      }

      // scans of the main feature that hold a data point of the ion
      final EvaluatedChromatogram feature = features.get(main);
      final int first = feature.scans()[0];
      final int last = feature.scans()[feature.size() - 1];
      final boolean[] hasIon = new boolean[last - first + 1];
      for (int i = 0; i < feature.size(); i++) {
        final int s = feature.scans()[i];
        hasIon[s - first] = data.belongsTo(s, featureIndices[main][i], k);
      }
      final int apexScan = scans[apex];
      if (apexScan < first || apexScan > last || !hasIon[apexScan - first]) {
        apexElsewhere++;
      }
      final double minIntense = INTENSE_FRACTION * intensities[apex];
      int holes = 0;
      for (int i = 0; i < scans.length; i++) {
        final int s = scans[i];
        if (s >= first && s <= last && intensities[i] >= minIntense && !hasIon[s - first]) {
          holes++;
        }
      }
      intenseHoles += holes;
      if (holes > 0) {
        ionsWithHoles++;
      }
    }
    return new Summary(detectable, found, split, numFeatures, falseFeatures,
        ChromatogramBenchmarkUtils.quantile(recoveries, 0.5),
        ChromatogramBenchmarkUtils.quantile(recoveries, 0.1), apexElsewhere, intenseHoles,
        ionsWithHoles, ChromatogramBenchmarkUtils.quantile(mzErrors, 0.5),
        ChromatogramBenchmarkUtils.quantile(mzErrors, 0.95), contaminated);
  }
}
