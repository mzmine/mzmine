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

package io.github.mzmine.datamodel.featuredata;

import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeries;
import io.github.mzmine.datamodel.otherdetectors.SimpleOtherTimeSeries;
import io.github.mzmine.util.MemoryMapStorage;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import javax.annotation.Nonnull;

public class OtherFeatureUtils {

  private OtherFeatureUtils() {

  }

  public static OtherFeature shiftRtAxis(MemoryMapStorage storage, @Nonnull OtherFeature feature,
      float shift) {
    final OtherTimeSeries timeSeries = feature.getFeatureData();

    double[] intensities = new double[timeSeries.getNumberOfValues()];
    timeSeries.getIntensityValues(intensities);

    float[] rts = new float[intensities.length];
    for (int i = 0; i < intensities.length; i++) {
      rts[i] = timeSeries.getRetentionTime(i) + shift;
    }

    final SimpleOtherTimeSeries shifted = new SimpleOtherTimeSeries(storage, rts, intensities,
        timeSeries.getName(), timeSeries.getTimeSeriesData());

    return feature.createSubFeature(shifted);
  }

  public static OtherFeature bin(MemoryMapStorage storage, @Nonnull OtherFeature feature,
      int width) {
    final OtherTimeSeries timeSeries = feature.getFeatureData();

    double[] intensities = new double[timeSeries.getNumberOfValues()];
    timeSeries.getIntensityValues(intensities);

    DoubleArrayList newIntensities = new DoubleArrayList();
    FloatArrayList newRts = new FloatArrayList();
    for (int i = 0; i < intensities.length; i += width) {
      double summedIntensity = 0;
      float summedRt = 0;
      int lastJ = 0;
      for (int j = 0; j < width && i + j < intensities.length; j++) {
        summedIntensity += intensities[i+j];
        summedRt += timeSeries.getRetentionTime(i+j);
        lastJ = j;
      }
      newIntensities.add(summedIntensity / (lastJ + 1));
      newRts.add(summedRt / (lastJ + 1));
    }

    final SimpleOtherTimeSeries shifted = new SimpleOtherTimeSeries(storage, newRts.toFloatArray(),
        newIntensities.toDoubleArray(), timeSeries.getName(), timeSeries.getTimeSeriesData());

    return feature.createSubFeature(shifted);
  }
}
