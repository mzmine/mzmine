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

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.execution.WizardOptimizationProblem;
import io.github.mzmine.util.MathUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Maximise: number of features that carry an isotope pattern. Mirrors
 * {@link WizardOptimizationProblem}'s {@code maximizeFeaturesWithIsos} objective.
 */
public record IpoIsotopeScore() implements SweepMetric {

  @Override
  public @NotNull String name() {
    return "IPO isotope score";
  }

  @Override
  public @NotNull String getUniqueID() {
    return "ipo_isotope_score";
  }

  @Override
  public @NotNull String toString() {
    return name();
  }

  @Override
  public boolean higherIsBetter() {
    return true;
  }

  @Override
  public double evaluate(@NotNull FeatureList featureList, @NotNull MetricContext context) {
    final double noise = MathUtils.calcQuantile(
        featureList.streamFeatures(false).mapToDouble(Feature::getHeight).toArray(), 0.03);

    long score = 0;
    for (RawDataFile file : featureList.getRawDataFiles()) {
      int numPeaks = 0;
      int numWithIsos = 0;
      for (FeatureListRow row : featureList.getRows()) {
        final Feature f = row.getFeature(file);
        if (f == null || f.getHeight() < noise) {
          continue;
        }
        numPeaks++;
        if (f.getIsotopePattern() != null) {
          numWithIsos++;
        }
      }
      score += (long) (((double) numWithIsos * numWithIsos) / Math.max(1, numPeaks));
    }

    return score;
  }
}
