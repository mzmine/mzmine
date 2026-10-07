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

package io.github.mzmine.modules.visualization.dash_lipidqc.summary;

import org.jetbrains.annotations.NotNull;

/**
 * Defines how annotations are counted in the lipid summary bar chart: either by total feature list
 * rows or by the number of unique annotation identities.
 */
enum SummaryCountMode {
  ROW_COUNT("Rows", "Number of lipid annotations", "Lipid annotations",
      "Total lipids: "), UNIQUE_ANNOTATIONS("Unique annotations",
      "Number of unique lipid annotations", "Unique lipid annotations",
      "Total unique annotations: ");

  private final @NotNull String label;
  private final @NotNull String rangeAxisLabel;
  private final @NotNull String seriesLabel;
  private final @NotNull String totalLabelPrefix;

  SummaryCountMode(final @NotNull String label, final @NotNull String rangeAxisLabel,
      final @NotNull String seriesLabel, final @NotNull String totalLabelPrefix) {
    this.label = label;
    this.rangeAxisLabel = rangeAxisLabel;
    this.seriesLabel = seriesLabel;
    this.totalLabelPrefix = totalLabelPrefix;
  }

  @NotNull String getRangeAxisLabel() {
    return rangeAxisLabel;
  }

  @NotNull String getSeriesLabel() {
    return seriesLabel;
  }

  @NotNull String getTotalLabelPrefix() {
    return totalLabelPrefix;
  }

  @Override
  public @NotNull String toString() {
    return label;
  }
}

