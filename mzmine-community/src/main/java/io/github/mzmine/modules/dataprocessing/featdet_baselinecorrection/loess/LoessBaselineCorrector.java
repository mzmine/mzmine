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

package io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.loess;

import io.github.mzmine.datamodel.data_access.FeatureDataAccess;
import io.github.mzmine.datamodel.featuredata.IntensityTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonSpectrumSeries;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeries;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.BaselineCorrectionParameters;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.BaselineCorrector;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.Arrays;
import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LoessBaselineCorrector implements BaselineCorrector, MZmineModule {

  private final MemoryMapStorage storage;
  private final double bandwidth;
  private final int iterations;
  private final String suffix;
  private double[] xBuffer = new double[0];
  private double[] yBuffer = new double[0];

  private int numSamples;

  public LoessBaselineCorrector() {
    this(null, 50, LoessInterpolator.DEFAULT_BANDWIDTH, LoessInterpolator.DEFAULT_ROBUSTNESS_ITERS,
        "baseline");
  }

  public LoessBaselineCorrector(MemoryMapStorage storage, int baselineSamples, double bandwidth,
      int iterations, String suffix) {
    this.storage = storage;
    this.bandwidth = bandwidth;
    this.iterations = iterations;
    this.suffix = suffix;
    numSamples = baselineSamples;
  }

  @Override
  public <T extends IntensityTimeSeries> T correctBaseline(T timeSeries) {
    final int numValues = timeSeries.getNumberOfValues();
    if (yBuffer.length < numValues) {
      xBuffer = new double[numValues];
      yBuffer = new double[numValues];
    }

    for (int i = 0; i < numValues; i++) {
      xBuffer[i] = timeSeries.getRetentionTime(i);
      if (xBuffer[i] < xBuffer[Math.max(i - 1, 0)]) {
        throw new IllegalStateException();
      }
    }

    if (timeSeries instanceof FeatureDataAccess access) {
      for (int i = 0; i < access.getNumberOfValues(); i++) {
        yBuffer[i] = access.getIntensity(i);
      }
    } else {
      yBuffer = timeSeries.getIntensityValues(yBuffer);
    }

    final double[] subSampleX = subsample(xBuffer, numValues, numSamples, true);
    final double[] subSampleY = subsample(yBuffer, numValues, numSamples, false);
    var interpolator = new LoessInterpolator(bandwidth, iterations - 1);
    final PolynomialSplineFunction function = interpolator.interpolate(subSampleX, subSampleY);

    for (int i = 0; i < numValues; i++) {
      yBuffer[i] = yBuffer[i] - function.value(xBuffer[i]);
    }

    return switch (timeSeries) {
      case IonSpectrumSeries<?> s -> (T) s.copyAndReplace(storage, s.getMzValues(new double[0]),
          Arrays.copyOfRange(yBuffer, 0, numValues));
      case OtherTimeSeries o -> (T) o.copyAndReplace(
          o.getTimeSeriesData().getOtherDataFile().getCorrespondingRawDataFile()
              .getMemoryMapStorage(), Arrays.copyOfRange(yBuffer, 0, numValues),
          o.getName() + " " + suffix);
      default -> throw new IllegalStateException(
          "Unexpected time series: " + timeSeries.getClass().getName());
    };
  }

  @Override
  public BaselineCorrector newInstance(BaselineCorrectionParameters parameters,
      MemoryMapStorage storage) {
    final ParameterSet embedded = parameters.getParameter(
        BaselineCorrectionParameters.correctionAlgorithm).getEmbeddedParameters();
    return new LoessBaselineCorrector(storage,
        embedded.getValue(LoessBaselineCorrectorParameters.numSamples),
        embedded.getValue(LoessBaselineCorrectorParameters.bandwidth),
        embedded.getValue(LoessBaselineCorrectorParameters.iterations),
        parameters.getValue(BaselineCorrectionParameters.suffix));
  }

  @Override
  public @NotNull String getName() {
    return "Spline baseline correction";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return LoessBaselineCorrectorParameters.class;
  }

  private static double[] subsample(double[] array, int numValues, int numSamples, boolean check) {
    if (numSamples >= numValues) {
      var reduced = new double[numValues];
      System.arraycopy(array, 0, reduced, 0, numValues);
      return reduced;
    }

    final int increment = numValues / numSamples;

    final double[] result = new double[numSamples + 1];
    for (int i = 0; i < numSamples; i++) {
      result[i] = array[i * increment];
//      if (check && result[Math.max(i - 1, 0)] > result[i]) {
//        throw new IllegalStateException();
//      }
    }

    result[numSamples] = array[numValues - 1];

    return result;
  }
}
