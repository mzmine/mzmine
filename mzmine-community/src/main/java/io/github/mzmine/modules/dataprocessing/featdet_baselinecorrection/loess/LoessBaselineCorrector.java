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

package io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.loess;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.AbstractBaselineCorrector;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.AbstractBaselineCorrectorParameters;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.BaselineCorrectionParameters;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.BaselineCorrector;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.UnivariateBaselineCorrector;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolver;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.MemoryMapStorage;
import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LoessBaselineCorrector extends UnivariateBaselineCorrector {

  private final MemoryMapStorage storage;
  private final double bandwidth;
  private final int iterations;

  public LoessBaselineCorrector() {
    this(null, 50, LoessInterpolator.DEFAULT_BANDWIDTH, LoessInterpolator.DEFAULT_ROBUSTNESS_ITERS,
        "baseline", null);
  }

  public LoessBaselineCorrector(MemoryMapStorage storage, int baselineSamples, double bandwidth,
      int iterations, String suffix, MinimumSearchFeatureResolver resolver) {
    super(storage, baselineSamples, suffix, resolver);
    this.storage = storage;
    this.bandwidth = bandwidth;
    this.iterations = iterations;
  }

  @Override
  protected UnivariateInterpolator initializeInterpolator() {
    return new LoessInterpolator(bandwidth, iterations);
  }

  @Override
  public BaselineCorrector newInstance(BaselineCorrectionParameters parameters,
      MemoryMapStorage storage, FeatureList flist) {
    final ParameterSet embedded = parameters.getParameter(
        BaselineCorrectionParameters.correctionAlgorithm).getEmbeddedParameters();
    final MinimumSearchFeatureResolver resolver =
        embedded.getValue(AbstractBaselineCorrectorParameters.applyPeakRemoval)
            ? AbstractBaselineCorrector.initializeLocalMinResolver((ModularFeatureList) flist)
            : null;

    return new LoessBaselineCorrector(storage,
        embedded.getValue(LoessBaselineCorrectorParameters.numSamples),
        embedded.getValue(LoessBaselineCorrectorParameters.bandwidth),
        embedded.getValue(LoessBaselineCorrectorParameters.iterations),
        parameters.getValue(BaselineCorrectionParameters.suffix), resolver);
  }

  @Override
  public @NotNull String getName() {
    return "Spline baseline correction";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return LoessBaselineCorrectorParameters.class;
  }
}