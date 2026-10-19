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

package io.github.mzmine.modules.tools.tools_autoparam.optimizer.metrics;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.types.annotations.shapeclassification.RtQualitySummaryType;
import io.github.mzmine.modules.dataprocessing.filter_featurefilter.peak_fitter.PeakShapeClassification;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.execution.WizardOptimizationProblem;
import org.jetbrains.annotations.NotNull;

/**
 * Minimise: ratio of features classified as double-Gaussian peaks to total features. Mirrors
 * {@link WizardOptimizationProblem}'s {@code minimizeDoublePeaks} objective.
 */
public record DoublePeakRatio() implements SweepMetric {

  @Override
  public @NotNull String name() {
    return "Double peak ratio";
  }

  @Override
  public @NotNull String getUniqueID() {
    return "double_peak_ratio";
  }

  @Override
  @NotNull
  public String toString() {
    return name();
  }

  @Override
  public boolean higherIsBetter() {
    return false;
  }

  @Override
  public double evaluate(@NotNull FeatureList featureList, @NotNull MetricContext context) {
    final long numFeatures = featureList.streamFeatures().count();
    if (numFeatures == 0) {
      return 0;
    }
    final long numDoublePeaks = featureList.streamFeatures(true)
        .map(f -> f.get(RtQualitySummaryType.class))
        .filter(s -> s != null && s.classification() == PeakShapeClassification.DOUBLE_GAUSSIAN)
        .count();
    return (double) numDoublePeaks / numFeatures;
  }
}
