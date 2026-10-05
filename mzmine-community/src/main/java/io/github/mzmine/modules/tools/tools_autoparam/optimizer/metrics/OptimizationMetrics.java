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

import java.util.List;

/**
 * Catalog of the selectable optimization metrics. A new metric is a {@link SweepMetric}
 * implementation that is added to {@link #ALL}.
 */
public final class OptimizationMetrics {

  public static final IpoIsotopeScore IPO_ISOTOPE_SCORE = new IpoIsotopeScore();
  public static final SlawIntegrationScore SLAW_INTEGRATION_SCORE = new SlawIntegrationScore();
  public static final IsotopeRatioConsistencyScore ISOTOPE_RATIO_CONSISTENCY_SCORE = new IsotopeRatioConsistencyScore();
  public static final DoublePeakRatio DOUBLE_PEAK_RATIO = new DoublePeakRatio();
  public static final FillRatio FILL_RATIO = new FillRatio();
  public static final GcEiFragmentQuality GC_EI_FRAGMENT_QUALITY = new GcEiFragmentQuality();
  public static final BenchmarkTargetCount BENCHMARK_TARGET_COUNT = new BenchmarkTargetCount();

  public static final List<SweepMetric> ALL = List.of(IPO_ISOTOPE_SCORE, SLAW_INTEGRATION_SCORE,
      ISOTOPE_RATIO_CONSISTENCY_SCORE, DOUBLE_PEAK_RATIO, FILL_RATIO, GC_EI_FRAGMENT_QUALITY,
      BENCHMARK_TARGET_COUNT);

  public static final List<SweepMetric> DEFAULT = List.of(ISOTOPE_RATIO_CONSISTENCY_SCORE);

  private OptimizationMetrics() {
  }
}
