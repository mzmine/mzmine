/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.chang;

import io.github.mzmine.datamodel.featuredata.IntensityTimeSeries;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.AnyXYProvider;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.AbstractBaselineCorrector;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.BaselineCorrectionParameters;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.BaselineCorrector;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.collections.IndexRange;
import io.github.mzmine.util.collections.SimpleIndexRange;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * https://rdrr.io/bioc/TargetSearch/man/baselineCorrection.html
 */
public class ChangBaselineCorrector extends AbstractBaselineCorrector {

  protected final MemoryMapStorage storage = null;
  private final double alpha;
  private final int maxSegments;
  private final double baselineFraction;
  private final int windowSize;
  private final double threshold;
  protected double[] xBuffer = new double[0];
  protected double[] yBuffer = new double[0];

  public ChangBaselineCorrector() {
    this(null, "", 0.95, 100, 0.2, 10, 0.5);
  }

  public ChangBaselineCorrector(MemoryMapStorage storage, String suffix, double alpha,
      int maxSegments, double baselineFraction, int windowSize, double threshold) {
    super(storage, 0, suffix, null);
    this.alpha = alpha;
    this.maxSegments = maxSegments;
    this.baselineFraction = baselineFraction;
    this.windowSize = windowSize;
    this.threshold = threshold;

  }

  // Method for linear interpolation
  private static double[] linearInterpolation(double[] x, double[] y, double[] newX) {
    double[] newY = new double[newX.length];
    int index = 0;
    for (int i = 0; i < newX.length; i++) {
      while (index < x.length - 1 && newX[i] > x[index + 1]) {
        index++;
      }
      double ratio = (newX[i] - x[index]) / (x[index + 1] - x[index]);
      newY[i] = y[index] + ratio * (y[index + 1] - y[index]);
    }
    return newY;
  }

  public static double[] highPassFilter(double[] x, double alpha) {
    double[] y = new double[x.length];
    y[0] = alpha * (0 + x[0]);

    for (int i = 1; i < x.length; i++) {
      y[i] = alpha * (y[i - 1] + x[i] - x[i - 1]);
    }

    return y;
  }

  @Override
  public <T extends IntensityTimeSeries> T correctBaseline(T timeSeries) {
    additionalData.clear();
    final int numValues = timeSeries.getNumberOfValues();
    if (yBuffer.length < numValues) {
      xBuffer = new double[numValues];
      yBuffer = new double[numValues];
    }

    extractDataIntoBuffer(timeSeries, xBuffer, yBuffer);

    // minimum 5 points per segment
    final int numSegments = Math.min(maxSegments, (int) Math.ceil((double) numValues / 5));

    final double[] filtered = highPassFilter(yBuffer, alpha);
    final int numPerSegment = numValues / numSegments;

    final List<IndexRange> indices = new ArrayList<>();
    for (int i = 0; i < numSegments; i++) {
      IndexRange indexRange = new SimpleIndexRange(i * numPerSegment,
          Math.min((i + 1) * numPerSegment - 1, numValues - 1));
      indices.add(indexRange);
      if (indexRange.maxExclusive() >= numValues) {
        break;
      }
    }

    // standard deviations of the segments
    final double[] segmentSDev = indices.stream()
        .mapToDouble(r -> MathUtils.calcStd(filtered, r.min(), r.maxExclusive())).toArray();
    final List<IndexRange> backgroundSegments = IntStream.range(0, indices.size()).boxed()
        .sorted(Comparator.comparing(i -> segmentSDev[i])).map(indices::get)
        .limit((long) (baselineFraction * numSegments)).toList();

    // map only background segments and calc the standard deviation
    final double backgroundSDev = MathUtils.calcStd(backgroundSegments.stream().flatMapToDouble(
        r -> DoubleStream.of(filtered).skip(r.min()).limit(r.maxExclusive() - r.min())).toArray());

    final boolean[] isSignal = new boolean[numValues];
    for (int i = 0; i < numValues; i++) {
      if (Math.abs(filtered[i]) > 2 * backgroundSDev) {
//        considered to be signal, mark the points around
        for (int j = Math.max(0, i - windowSize); j < Math.min(numValues - 1, j + windowSize);
            j++) {
          isSignal[j] = true;
        }
      }
    }
    isSignal[0] = false;
    isSignal[isSignal.length - 1] = false;

    final double[] backgroundSignal = linearInterpolation(
        IntStream.range(0, numValues).filter(j -> !isSignal[j]).mapToDouble(j -> xBuffer[j])
            .toArray(),
        IntStream.range(0, numValues).filter(j -> !isSignal[j]).mapToDouble(j -> yBuffer[j])
            .toArray(), xBuffer);

    for (int i = 0; i < numValues; i++) {
      yBuffer[i] = Math.max(0,
          yBuffer[i] - backgroundSignal[i] + 2 * (threshold * 0.5) * 2 * backgroundSDev);
    }

    if (isPreview()) {
      additionalData.add(new AnyXYProvider(Color.RED, "baseline", numValues, i -> xBuffer[i],
          i -> backgroundSignal[i] + 2 * (threshold * 0.5) * 2 * backgroundSDev));
      additionalData.add(new AnyXYProvider(Color.BLUE, "background", numValues, i -> xBuffer[i],
          i -> backgroundSignal[i]));
      additionalData.add(
          new AnyXYProvider(Color.GREEN, "isSignal " + backgroundSDev, numValues, i -> xBuffer[i],
              i -> isSignal[i] ? 1000d : -1000d));
    }

    return createNewTimeSeries(timeSeries, numValues, yBuffer);
  }

  @Override
  public BaselineCorrector newInstance(ParameterSet parameters, MemoryMapStorage storage,
      FeatureList flist) {

    final ParameterSet embedded = parameters.getParameter(
        BaselineCorrectionParameters.correctionAlgorithm).getEmbeddedParameters();

    final Double baselineFraction = embedded.getValue(
        ChangBaselineCorrectorParameters.baselineFraction);
    final Double alpha = embedded.getValue(ChangBaselineCorrectorParameters.alpha);
    final Integer numSegments = embedded.getValue(ChangBaselineCorrectorParameters.numSegments);
    final Double threshold = embedded.getValue(ChangBaselineCorrectorParameters.threshold);
    final Integer windowWidth = embedded.getValue(ChangBaselineCorrectorParameters.signalWindow);

    return new ChangBaselineCorrector(storage,
        parameters.getValue(BaselineCorrectionParameters.suffix), alpha, numSegments,
        baselineFraction, windowWidth, threshold);
  }

  @Override
  public @NotNull String getName() {
    return "Chang baseline corrector";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return ChangBaselineCorrectorParameters.class;
  }
}
