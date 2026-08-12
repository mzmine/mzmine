/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.arpls;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.AnyXYProvider;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.AbstractBaselineCorrectorParameters;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.AbstractResolverBaselineCorrector;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.BaselineCorrectionParameters;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.BaselineCorrector;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.BaselineCorrectors;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolver;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.MemoryMapStorage;
import java.awt.Color;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Baseline correction using asymmetrically reweighted penalized least squares (arPLS). In contrast
 * to the interpolation based correctors, arPLS fits the baseline on the full resolution trace and
 * rejects peaks through its own asymmetric reweighting (see {@link ArplsBaseline}). The optional
 * peak exclusion of {@link AbstractResolverBaselineCorrector} may still be enabled to bridge
 * detected peaks beforehand, which can help on very wide or overlapping signals.
 */
public class ArplsBaselineCorrector extends AbstractResolverBaselineCorrector {

  private final double lambda;
  private final double ratio;
  private final int maxIterations;
  // reusable workspace, shared across all traces this corrector processes
  private final ArplsBaseline arpls = new ArplsBaseline();

  public ArplsBaselineCorrector() {
    this(null, ArplsBaselineCorrectorParameters.DEFAULT_LAMBDA,
        ArplsBaselineCorrectorParameters.DEFAULT_RATIO,
        ArplsBaselineCorrectorParameters.DEFAULT_MAX_ITERATIONS, "baseline", null);
  }

  public ArplsBaselineCorrector(@Nullable final MemoryMapStorage storage, final double lambda,
      final double ratio, final int maxIterations, @NotNull final String suffix,
      @Nullable final MinimumSearchFeatureResolver resolver) {
    // samplePercentage is unused: arPLS fits the full trace instead of sub sampling
    super(storage, 0.05, suffix, resolver);
    this.lambda = lambda;
    this.ratio = ratio;
    this.maxIterations = maxIterations;
  }

  @Override
  protected void subSampleAndCorrect(final double[] xDataToCorrect, final double[] yDataToCorrect,
      final int numValues, final double[] xDataFiltered, final double[] yDataFiltered,
      final int numValuesFiltered, final boolean addPreview) {
    // arPLS fits the baseline on the full (optionally peak-bridged) trace - no sub sampling.
    // peaks are bridged, not removed, so the filtered grid keeps the full resolution and filtered
    // index i aligns with the data point i that is corrected. The returned array is a reused buffer
    // that may be longer than numValues - bound every access by numValues, never its length.
    final double[] baseline = arpls.fitBaseline(yDataFiltered, numValuesFiltered, lambda, ratio,
        maxIterations);

    for (int i = 0; i < numValues; i++) {
      // corrected value must stay >= 0. The baseline itself may dip below zero, e.g. for signal
      // drifts due to solvent composition change
      yDataToCorrect[i] = Math.max(yDataToCorrect[i] - baseline[i], 0d);
    }

    if (addPreview) {
      // snapshot: the baseline buffer is reused for the next trace, but the preview provider reads
      // it lazily, so it needs its own copy of exactly numValues points
      final double[] previewBaseline = Arrays.copyOf(baseline, numValues);
      additionalData.add(new AnyXYProvider(Color.RED, "baseline", numValues, j -> xDataToCorrect[j],
          j -> previewBaseline[j]));
    }
  }

  @Override
  public BaselineCorrector newInstance(final ParameterSet parameters,
      final MemoryMapStorage storage, final FeatureList flist) {
    final ParameterSet embedded = parameters.getParameter(
        BaselineCorrectionParameters.correctionAlgorithm).getEmbeddedParameters();
    final MinimumSearchFeatureResolver resolver =
        embedded.getValue(AbstractBaselineCorrectorParameters.applyPeakRemoval)
            ? initializeLocalMinResolver((ModularFeatureList) flist) : null;

    return new ArplsBaselineCorrector(storage,
        embedded.getValue(ArplsBaselineCorrectorParameters.lambda),
        ArplsBaselineCorrectorParameters.DEFAULT_RATIO,
        embedded.getValue(ArplsBaselineCorrectorParameters.maxIterations),
        parameters.getValue(BaselineCorrectionParameters.suffix), resolver);
  }

  @Override
  public @NotNull String getName() {
    return BaselineCorrectors.ARPLS.toString();
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return ArplsBaselineCorrectorParameters.class;
  }
}
