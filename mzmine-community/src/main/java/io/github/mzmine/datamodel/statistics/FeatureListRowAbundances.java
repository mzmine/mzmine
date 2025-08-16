/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.datamodel.statistics;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import java.util.Arrays;
import java.util.BitSet;
import org.jetbrains.annotations.NotNull;

public interface FeatureListRowAbundances {

  /**
   * @param abundances         is the feature abundances for all samples in the order of the samples
   *                           {@link RawDataFile} list of the parent {@link DataTable}
   * @param trackMissingValues creates a {@link MissingValuesImputedFeatureListRowAbundances} to
   *                           track the original missing values so that if zero values are imputed
   *                           later the original missing values ratio is kept
   */
  static FeatureListRowAbundances of(FeatureListRow row, double[] abundances,
      boolean trackMissingValues) {
    if (trackMissingValues) {
      return new SimpleFeatureListRowAbundances(row, abundances);
    } else {
      int nMissingValues = 0;
      BitSet missingValues = new BitSet(abundances.length);
      for (int i = 0; i < abundances.length; i++) {
        if (Double.isNaN(abundances[i])) {
          missingValues.set(i);
          nMissingValues++;
        }
      }
      return new MissingValuesImputedFeatureListRowAbundances(row, abundances, missingValues,
          nMissingValues);
    }
  }

  @NotNull FeatureListRow row();

  double @NotNull [] abundances();

  boolean wasMissingValue(int sampleIndex);

  @NotNull FeatureListRowAbundances copy();

  default int numberOfSamples() {
    return abundances().length;
  }

  default double getValue(int sampleIndex) {
    return abundances()[sampleIndex];
  }

  /**
   * @return the original missing values. So even after missing value imputation, this should give
   * the original missing values
   */
  default int countOriginalMissingValues() {
    int count = 0;
    for (int i = 0; i < numberOfSamples(); i++) {
      if (wasMissingValue(i)) {
        count++;
      }
    }
    return count;
  }

  @NotNull FeatureListRowAbundances subsetByIndexes(int[] subsetIndexes);

  /**
   * Simple implementation, also see {@link MissingValuesImputedFeatureListRowAbundances} that keeps
   * track of missing values after imputation
   */
  record SimpleFeatureListRowAbundances(FeatureListRow row, double[] abundances) implements
      FeatureListRowAbundances {

    @Override
    public @NotNull SimpleFeatureListRowAbundances copy() {
      return new SimpleFeatureListRowAbundances(row, Arrays.copyOf(abundances, abundances.length));
    }

    @Override
    public @NotNull FeatureListRowAbundances subsetByIndexes(int[] subsetIndexes) {
      final double[] subset = new double[subsetIndexes.length];
      for (int i = 0; i < subset.length; i++) {
        final int sampleIndex = subsetIndexes[i];
        subset[i] = abundances()[sampleIndex];
      }
      return new SimpleFeatureListRowAbundances(row(), subset);
    }

    @Override
    public boolean wasMissingValue(int sampleIndex) {
      return Double.isNaN(getValue(sampleIndex));
    }
  }

  /**
   * Keeps track of
   *
   * @param originalMissingValues the missing values in the original data, after imputation this is
   *                              not obvious from the data
   */
  record MissingValuesImputedFeatureListRowAbundances(FeatureListRow row, double[] abundances,
                                                      BitSet originalMissingValues,
                                                      int nMissingValues) implements
      FeatureListRowAbundances {

    @Override
    public @NotNull MissingValuesImputedFeatureListRowAbundances copy() {
      return new MissingValuesImputedFeatureListRowAbundances(row,
          Arrays.copyOf(abundances, abundances.length), (BitSet) originalMissingValues.clone(),
          nMissingValues);
    }

    @Override
    public boolean wasMissingValue(int sampleIndex) {
      return originalMissingValues.get(sampleIndex);
    }

    @Override
    public int countOriginalMissingValues() {
      return nMissingValues;
    }

    @Override
    public @NotNull FeatureListRowAbundances subsetByIndexes(int[] subsetIndexes) {
      int nMissingInSub = 0;
      final BitSet subsetMissingValues = new BitSet(subsetIndexes.length);
      final double[] subset = new double[subsetIndexes.length];
      for (int i = 0; i < subset.length; i++) {
        final int sampleIndex = subsetIndexes[i];
        subset[i] = abundances()[sampleIndex];
        // transfer missing value
        if (wasMissingValue(sampleIndex)) {
          subsetMissingValues.set(i, wasMissingValue(sampleIndex));
          nMissingInSub++;
        }
      }

      return new MissingValuesImputedFeatureListRowAbundances(row(), subset, subsetMissingValues,
          nMissingInSub);
    }
  }
}
