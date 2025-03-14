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

package io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.neville;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.AbstractBaselineCorrectorParameters;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.BaselineCorrectionParameters;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.BaselineCorrector;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.UnivariateBaselineCorrector;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolver;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.MemoryMapStorage;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.NevilleInterpolator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NevilleBaselineCorrector extends UnivariateBaselineCorrector {

  public NevilleBaselineCorrector() {
    super();
  }

  public NevilleBaselineCorrector(MemoryMapStorage storage, double samplePercentage,
      @NotNull String suffix, MinimumSearchFeatureResolver resolver) {
    super(storage, samplePercentage, suffix, resolver);
  }

  @Override
  protected UnivariateFunction initializeFunction(double[] x, final double[] y) {
    return new NevilleInterpolator().interpolate(x, y);
  }

  @Override
  public BaselineCorrector newInstance(ParameterSet parameters, MemoryMapStorage storage,
      FeatureList flist) {
    final String suffix = parameters.getValue(BaselineCorrectionParameters.suffix);
    final ParameterSet embedded = parameters.getParameter(
        BaselineCorrectionParameters.correctionAlgorithm).getEmbeddedParameters();
    final Double samplePercentage = embedded.getValue(
        AbstractBaselineCorrectorParameters.samplePercentage);
    final MinimumSearchFeatureResolver resolver =
        embedded.getValue(AbstractBaselineCorrectorParameters.applyPeakRemoval)
            ? initializeLocalMinResolver((ModularFeatureList) flist) : null;

    return new NevilleBaselineCorrector(storage, samplePercentage, suffix, resolver);
  }

  @Override
  public @NotNull String getName() {
    return "Neville corrector";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return NevilleBaselineCorrectorParameters.class;
  }
}
