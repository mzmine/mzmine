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

package io.github.mzmine.gui.chartbasics.simplechart.datasets;

import io.github.mzmine.gui.chartbasics.simplechart.providers.PieXYZDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYZDataProvider;
import org.jfree.chart.renderer.xy.XYItemRenderer;

public record DatasetAndRenderer(ColoredXYDataset dataset, XYItemRenderer renderer) {

  public DatasetAndRenderer(PlotXYDataProvider provider, XYItemRenderer renderer) {
    this(getDataset(provider), renderer);
  }

  private static ColoredXYDataset getDataset(PlotXYDataProvider provider) {
    if (provider instanceof PieXYZDataProvider<?> pie) {
      return new ColoredXYZPieDataset<>(pie);
    }

    if (provider instanceof PlotXYZDataProvider xyzProvider) {
      return new ColoredXYZDataset(xyzProvider);
    }

    return new ColoredXYDataset(provider);
  }
}
