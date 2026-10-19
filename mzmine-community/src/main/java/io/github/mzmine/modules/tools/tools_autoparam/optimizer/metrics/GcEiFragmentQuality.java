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
import io.github.mzmine.datamodel.features.FeatureListRow;
import org.jetbrains.annotations.NotNull;

/**
 * Maximise: fraction of aligned rows whose fragment scans contain more than {@value #MIN_SIGNALS}
 * signals. Designed for GC-EI workflows where fragment scans are deconvoluted EI pseudo spectra — a
 * higher signal count indicates a richer, more identifiable fragmentation pattern.
 */
public record GcEiFragmentQuality() implements SweepMetric {

  /**
   * Minimum number of signals (data points) a fragment scan must contain to be considered good
   * quality.
   */
  private static final int MIN_SIGNALS = 10;

  @Override
  public @NotNull String name() {
    return "GC-EI fragment spectrum quality";
  }

  @Override
  public @NotNull String getUniqueID() {
    return "gc_ei_fragment_quality";
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
    final int totalRows = featureList.getNumberOfRows();
    if (totalRows == 0) {
      return 0.0;
    }

    int goodRows = 0;
    for (FeatureListRow row : featureList.getRows()) {
      if (row.getMostIntenseFragmentScan() != null
          && row.getMostIntenseFragmentScan().getNumberOfDataPoints() > MIN_SIGNALS) {
        goodRows++;
      }
    }

    return (double) goodRows * goodRows / totalRows;
  }
}
