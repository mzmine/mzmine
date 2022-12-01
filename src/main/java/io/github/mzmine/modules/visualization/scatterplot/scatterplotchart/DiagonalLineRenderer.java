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

package io.github.mzmine.modules.visualization.scatterplot.scatterplotchart;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;

public class DiagonalLineRenderer extends XYLineAndShapeRenderer {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  // plot colors for diagonal lines
  private static final Color[] plotDiagonalColors =
      {new Color(165, 42, 42), Color.BLACK, new Color(165, 42, 42)};

  private static final Shape diagonalPointsShape = new Rectangle2D.Float(-3, -3, 6, 6);

  public static final AlphaComposite alpha =
      AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f);

  public DiagonalLineRenderer() {

    super(true, true);

    setDefaultShapesFilled(true);
    setDrawOutlines(true);
    setUseFillPaint(true);

    for (int i = 0; i < plotDiagonalColors.length; i++) {
      setSeriesShape(i, diagonalPointsShape);
      setSeriesPaint(i, plotDiagonalColors[i]);
      setSeriesFillPaint(i, plotDiagonalColors[i]);
    }

    setDefaultShapesVisible(true);

    XYItemLabelGenerator diagonallabelGenerator = new DiagonalLineLabelGenerator();
    setDefaultItemLabelGenerator(diagonallabelGenerator);
    setDefaultItemLabelsVisible(true);

    setDrawSeriesLineAsPath(true);
  }

  public XYItemRendererState initialise(Graphics2D g2, Rectangle2D dataArea, XYPlot plot,
      XYDataset dataset, PlotRenderingInfo info) {

    // Set transparency
    g2.setComposite(alpha);

    return super.initialise(g2, dataArea, plot, dataset, info);
  }

}
