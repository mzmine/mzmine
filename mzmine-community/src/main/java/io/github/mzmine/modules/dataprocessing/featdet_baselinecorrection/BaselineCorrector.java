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

import io.github.mzmine.datamodel.data_access.FeatureDataAccess;
import io.github.mzmine.datamodel.data_access.FeatureFullDataAccess;
import io.github.mzmine.datamodel.featuredata.IntensityTimeSeries;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.List;

public interface BaselineCorrector extends MZmineModule {

  static double[] subsample(double[] array, int numValues, int numSamples, boolean check) {
    if (numSamples >= numValues) {
      var reduced = new double[numValues];
      System.arraycopy(array, 0, reduced, 0, numValues);
      return reduced;
    }

    final int increment = numValues / numSamples;

    final double[] result = new double[numSamples + 1];
    for (int i = 0; i < numSamples; i++) {
      result[i] = array[i * increment];
      if (check && result[Math.max(i - 1, 0)] > result[i]) {
        throw new IllegalStateException();
      }
    }

    result[numSamples] = array[numValues - 1];

    return result;
  }

  <T extends IntensityTimeSeries> T correctBaseline(T timeSeries);

  BaselineCorrector newInstance(ParameterSet parameters, MemoryMapStorage storage,
      FeatureList flist);

  default <T extends IntensityTimeSeries> void extractDataIntoBuffer(T timeSeries, double[] xBuffer,
      double[] yBuffer) {
    for (int i = 0; i < timeSeries.getNumberOfValues(); i++) {
      xBuffer[i] = timeSeries.getRetentionTime(i);
      if (xBuffer[i] < xBuffer[Math.max(i - 1, 0)]) {
        throw new IllegalStateException();
      }
    }

    if (timeSeries instanceof FeatureFullDataAccess access) {
      for (int i = 0; i < access.getNumberOfValues(); i++) {
        System.arraycopy(access.getIntensityValues(), 0, yBuffer, 0, access.getNumberOfValues());
      }
    } else if (timeSeries instanceof FeatureDataAccess access) {
      for (int i = 0; i < access.getNumberOfValues(); i++) {
        yBuffer[i] = access.getIntensity(i);
      }
    } else {
      yBuffer = timeSeries.getIntensityValues(yBuffer);
    }
  }

  default List<PlotXYDataProvider> getAdditionalPreviewData() {
    return List.of();
  }
}
