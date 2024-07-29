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

package io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.spline;

import io.github.mzmine.datamodel.data_access.FeatureDataAccess;
import io.github.mzmine.datamodel.featuredata.IntensityTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonSpectrumSeries;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeries;
import io.github.mzmine.datamodel.otherdetectors.SimpleOtherTimeSeries;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.BaselineCorrector;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.MemoryMapStorage;
import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LoessBaselineCorrector implements BaselineCorrector, MZmineModule {

  private final MemoryMapStorage storage;
  private double[] xBuffer = new double[0];
  private double[] yBuffer = new double[0];

  private final LoessInterpolator interpolator = new LoessInterpolator();

  public LoessBaselineCorrector(MemoryMapStorage storage) {

    this.storage = storage;
  }

  @Override
  public <T extends IntensityTimeSeries> T correctBaseline(T timeSeries) {
    if (yBuffer.length < timeSeries.getNumberOfValues()) {
      xBuffer = new double[timeSeries.getNumberOfValues()];
      yBuffer = new double[timeSeries.getNumberOfValues()];
    }

    for (int i = 0; i < timeSeries.getNumberOfValues(); i++) {
      xBuffer[i] = timeSeries.getRetentionTime(i);
    }

    if (timeSeries instanceof FeatureDataAccess access) {
      for (int i = 0; i < access.getNumberOfValues(); i++) {
        yBuffer[i] = access.getIntensity(i);
      }
    } else {
      yBuffer = timeSeries.getIntensityValues(yBuffer);
    }

    final PolynomialSplineFunction function = interpolator.interpolate(subsample(xBuffer, 50),
        subsample(yBuffer, 50));

    for (int i = 0; i < timeSeries.getNumberOfValues(); i++) {
      yBuffer[i] = yBuffer[i] - function.value(xBuffer[i]);
    }

    return switch (timeSeries) {
      case IonSpectrumSeries<?> s ->
          s.copyAndReplace(storage, s.getMzValues(new double[0]), yBuffer);
      case OtherTimeSeries o -> new SimpleOtherTimeSeries(o.getOtherDataFile().getCorrespondingRawDataFile()
          .getMemoryMapStorage(), , yBuffer, o.getName(), o.getTimeSeriesData());
    };

  }

  @Override
  public @NotNull String getName() {
    return "Spline baseline correction";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return null;
  }

  private double[] subsample(double[] array, int numSamples) {
    if (numSamples >= array.length) {
      return array;
    }

    final int increment = array.length / numSamples;

    final double[] result = new double[numSamples + 1];
    for (int i = 0; i < numSamples; i++) {
      result[i] = array[i * increment];
    }

    result[numSamples] = array[array.length - 1];

    return result;
  }
}
