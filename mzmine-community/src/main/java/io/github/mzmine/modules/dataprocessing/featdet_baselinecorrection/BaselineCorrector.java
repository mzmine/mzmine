/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection;

import io.github.mzmine.datamodel.featuredata.IntensityTimeSeries;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.util.MemoryMapStorage;
import io.mzio.mzmine.datamodel.MZmineModule;
import io.mzio.mzmine.datamodel.parameters.ParameterSet;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;

public interface BaselineCorrector extends MZmineModule {

  static double[] subsample(double[] array, int numValues, int numSamples,
      boolean checkDataAscending) {
    if (numSamples >= numValues) {
      var reduced = new double[numValues];
      System.arraycopy(array, 0, reduced, 0, numValues);
      return reduced;
    }

    // use float to get more precise increments
    final float increment = (float) numValues / numSamples;

    final double[] result = new double[numSamples];
    for (int i = 0; i < numSamples; i++) {
      // floor to lower number
      result[i] = array[(int) (i * increment)];
      if (checkDataAscending && result[Math.max(i - 1, 0)] > result[i]) {
        throw new IllegalStateException("Data is not sorted ascending");
      }
    }

    return result;
  }

  /**
   * Subsample by list of indices
   */
  static double[] subsample(double[] array, int numValues, IntList indices,
      boolean checkDataAscending) {
    double[] result = new double[indices.size()];
    for (int i = 0; i < indices.size(); i++) {
      int dataIndex = indices.getInt(i);
      if (dataIndex >= numValues) {
        throw new IllegalStateException(
            "Index is out of data range for numValues %d and index %d".formatted(numValues,
                dataIndex));
      }
      result[i] = array[dataIndex];
      if (checkDataAscending && result[Math.max(i - 1, 0)] > result[i]) {
        throw new IllegalStateException("Data is not sorted ascending");
      }
    }
    return result;
  }

  <T extends IntensityTimeSeries> T correctBaseline(T timeSeries);

  BaselineCorrector newInstance(ParameterSet parameters, MemoryMapStorage storage,
      FeatureList flist);

  default List<PlotXYDataProvider> getAdditionalPreviewData() {
    return List.of();
  }
}
