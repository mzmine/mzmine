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

package io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.polynomial;

import io.github.mzmine.datamodel.featuredata.IntensityTimeSeries;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.AbstractBaselineCorrector;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.BaselineCorrectionParameters;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.BaselineCorrector;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolver;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.MemoryMapStorage;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PolynomialBaselineCorrection extends AbstractBaselineCorrector {

  private final PolynomialCurveFitter fitter;

  public PolynomialBaselineCorrection(MemoryMapStorage storage, int numSamples, String suffix,
      MinimumSearchFeatureResolver resolver, int numPoints) {
    super(storage, numSamples, suffix, resolver);
    fitter = PolynomialCurveFitter.create(numPoints).withMaxIterations(3);
  }

  @Override
  public <T extends IntensityTimeSeries> T correctBaseline(T timeSeries) {
    final int numValues = timeSeries.getNumberOfValues();
    if (yBuffer.length < numValues) {
      xBuffer = new double[numValues];
      yBuffer = new double[numValues];
      xBufferRemovedPeaks = new double[numValues];
      yBufferRemovedPeaks = new double[numValues];
    }
    extractDataIntoBuffer(timeSeries, xBuffer, yBuffer);

    return null;
  }

  @Override
  public BaselineCorrector newInstance(BaselineCorrectionParameters parameters,
      MemoryMapStorage storage, FeatureList flist) {
    return null;
  }

  @Override
  public @NotNull String getName() {
    return "";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return null;
  }
}
