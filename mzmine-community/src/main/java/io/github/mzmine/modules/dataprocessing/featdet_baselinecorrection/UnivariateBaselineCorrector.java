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

import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.AnyXYProvider;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolver;
import io.github.mzmine.util.MemoryMapStorage;
import it.unimi.dsi.fastutil.ints.IntList;
import java.awt.Color;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class UnivariateBaselineCorrector extends AbstractResolverBaselineCorrector {

  public UnivariateBaselineCorrector() {
    super(null, 5, "", null);
  }

  public UnivariateBaselineCorrector(@Nullable MemoryMapStorage storage, double samplePercentage,
      @NotNull String suffix, @Nullable MinimumSearchFeatureResolver resolver) {
    super(storage, samplePercentage, suffix, resolver);
  }

  /**
   * @param xDataToCorrect    in place operation, the data to correct is changed
   * @param yDataToCorrect    in place operation, the data to correct is changed
   * @param numValues         corresponding number of values - input arrays may be longer
   * @param xDataFiltered     might be the whole x data or peaks removed
   * @param yDataFiltered     might be the whole y data or peaks removed
   * @param numValuesFiltered number of filtered data points
   * @param addPreview        add preview datasets
   */
  @Override
  protected void subSampleAndCorrect(final double[] xDataToCorrect, final double[] yDataToCorrect,
      int numValues, double[] xDataFiltered, double[] yDataFiltered, int numValuesFiltered,
      final boolean addPreview) {
    // translate into setp size in number of data points
    final int numSamples = (int) (numValuesFiltered * samplePercentage);
    int stepSize = numValuesFiltered / numSamples;
    if (stepSize < 4) {
      stepSize = 4; // minimum required distance between samples
    }
    IntList subsampleIndices = buffer.createSubSampleIndicesFromLandmarks(stepSize);

    XYDataArrays subData = subSampleData(subsampleIndices, xDataFiltered, yDataFiltered,
        numValuesFiltered);

    // initialize a function to map x -> y
    UnivariateFunction function = initializeFunction(subData.x(), subData.y());

    for (int i = 0; i < numValues; i++) {
      double baseline = function.value(xDataToCorrect[i]);
      if (Double.isNaN(baseline)) {
        // TODO change and create function with different settings
        // LOESS - wider bandwidth, maybe more iterations
        baseline = 0;
      }
      // corrected value must be above zero. the baseline itself may be below zero, e.g. for signal
      // drifts due to solvent composition change
      yDataToCorrect[i] = Math.max(yDataToCorrect[i] - baseline, 0);
    }

    if (addPreview) {
      additionalData.add(new AnyXYProvider(Color.RED, "baseline", numValues, j -> xDataToCorrect[j],
          j -> function.value(xDataToCorrect[j])));

      additionalData.add(
          new AnyXYProvider(Color.BLUE, "samples", subData.numValues(), subData::getX,
              subData::getY));
    }
  }

  protected abstract UnivariateFunction initializeFunction(double[] x, final double[] y);

}
