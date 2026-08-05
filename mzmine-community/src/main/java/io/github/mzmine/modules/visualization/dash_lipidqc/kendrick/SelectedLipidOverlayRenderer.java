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

package io.github.mzmine.modules.visualization.dash_lipidqc.kendrick;

import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.visualization.spectra.matchedlipid.LipidLabelPainter;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;

/**
 * Renderer that draws a highlighted circle and text label at the position of the currently
 * selected lipid on a Kendrick or ECN scatter plot.
 */
public class SelectedLipidOverlayRenderer extends XYLineAndShapeRenderer {

  private final @NotNull String label;

  public SelectedLipidOverlayRenderer(final @NotNull String label) {
    super(false, false);
    this.label = label;
  }

  @Override
  public void drawItem(final @NotNull Graphics2D g2, final @NotNull XYItemRendererState state,
      final @NotNull Rectangle2D dataArea, final PlotRenderingInfo info,
      final @NotNull XYPlot plot, final @NotNull ValueAxis domainAxis,
      final @NotNull ValueAxis rangeAxis, final @NotNull XYDataset dataset, final int series,
      final int item, final CrosshairState crosshairState, final int pass) {
    final double x = dataset.getXValue(series, item);
    final double y = dataset.getYValue(series, item);
    if (!Double.isFinite(x) || !Double.isFinite(y)) {
      return;
    }

    final double tx = domainAxis.valueToJava2D(x, dataArea, plot.getDomainAxisEdge());
    final double ty = rangeAxis.valueToJava2D(y, dataArea, plot.getRangeAxisEdge());
    final double radius = 9d;
    final Paint borderPaint = ConfigService.getDefaultColorPalette().getPositiveColorAWT();
    g2.setPaint(borderPaint);
    g2.setStroke(new BasicStroke(2.2f));
    g2.draw(new Ellipse2D.Double(tx - radius, ty - radius, radius * 2d, radius * 2d));
    LipidLabelPainter.drawLabel(g2, dataArea, tx, ty, label, borderPaint);
  }
}
