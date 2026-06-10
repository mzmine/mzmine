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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection.local_max;

import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectorPreprocessor;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectorPreprocessorModule;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.savitzkygolay.SavitzkyGolayFilter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.collections.IndexRange;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Savitzky-Golay smoothing option for the {@link LocalMaxMassDetector}. Smoothing is applied
 * independently to each consecutive range (the points within a range are equidistant) and writes
 * into a separate buffer, so the original intensities remain available for the intensity
 * calculation.
 */
public class LocalMaxSavitzkyGolayModule implements MassDetectorPreprocessorModule,
    MassDetectorPreprocessor {

  /**
   * Normalized Savitzky-Golay weights. Null for the no-arg module instance that only exposes the
   * parameters.
   */
  private final double @NotNull [] weights;

  /**
   * No-arg constructor used to expose the parameter set through the module registry.
   */
  public LocalMaxSavitzkyGolayModule() {
    this.weights = SavitzkyGolayFilter.getNormalizedWeights(3);
  }

  private LocalMaxSavitzkyGolayModule(final double @NotNull [] weights) {
    this.weights = weights;
  }

  @Override
  public @NotNull MassDetectorPreprocessor createPreprocessor(
      @Nullable final ParameterSet parameters) {
    final int width = parameters.getValue(LocalMaxSavitzkyGolayParameters.width);
    final double[] normWeights = SavitzkyGolayFilter.getNormalizedWeights(
        SavitzkyGolayFilter.getClosestFilterWidth(width));
    return new LocalMaxSavitzkyGolayModule(normWeights);
  }

  @Override
  public double @NotNull [] preprocessIntensities(final double @NotNull [] intensities,
      @NotNull final List<IndexRange> ranges) {
    final double[] smoothed = new double[intensities.length];
    for (final IndexRange range : ranges) {
      // confined to [min, maxExclusive) so consecutive ranges do not bleed into each other
      SavitzkyGolayFilter.convolve(intensities, range.min(), range.maxExclusive(), weights,
          smoothed);
    }
    return smoothed;
  }

  @Override
  public @NotNull String getName() {
    return "Savitzky-Golay";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return LocalMaxSavitzkyGolayParameters.class;
  }
}
