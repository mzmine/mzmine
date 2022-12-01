/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.chromatogram;

import io.github.mzmine.gui.chartbasics.simplechart.SimpleChartUtility;
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.data.xy.XYDataset;

public class FeatureTICRenderer extends XYAreaRenderer {

  public FeatureTICRenderer() {
    super();
    SimpleChartUtility.tryApplyDefaultChartThemeToRenderer(this);
  }

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private static final float OPACITY = 0.45f;

  private static Composite makeComposite(final float alpha) {

    return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
  }

  @Override
  public void drawItem(final Graphics2D g2, final XYItemRendererState state,
      final Rectangle2D dataArea, final PlotRenderingInfo info, final XYPlot plot,
      final ValueAxis domainAxis, final ValueAxis rangeAxis, final XYDataset dataSet,
      final int series, final int item, final CrosshairState crosshairState, final int pass) {

    g2.setComposite(makeComposite(OPACITY));
    super.drawItem(g2, state, dataArea, info, plot, domainAxis, rangeAxis, dataSet, series, item,
        crosshairState, pass);
  }
}
