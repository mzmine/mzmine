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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;

import javax.swing.SwingUtilities;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;

public class ScatterPlotRenderer extends XYLineAndShapeRenderer {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  private static final Shape dataPointsShape = new Ellipse2D.Float(-2.5f, -2.5f, 5, 5);

  public static final Color pointColor = Color.blue;
  public static final Color searchColor = Color.orange;

  public static final AlphaComposite pointAlpha =
      AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f);

  public static final AlphaComposite selectionAlpha =
      AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f);

  public ScatterPlotRenderer() {

    super(false, true);

    ScatterPlotToolTipGenerator toolTipGenerator = new ScatterPlotToolTipGenerator();
    setDefaultToolTipGenerator(toolTipGenerator);

    XYItemLabelGenerator ItemlabelGenerator = new ScatterPlotItemLabelGenerator();
    setDefaultItemLabelGenerator(ItemlabelGenerator);
    setDefaultItemLabelFont(new Font("SansSerif", Font.BOLD, 11));
    setDefaultItemLabelPaint(Color.black);
    setDefaultItemLabelsVisible(false);

    setSeriesItemLabelsVisible(0, false);
    setSeriesPaint(0, pointColor);
    setSeriesShape(0, dataPointsShape);

    setSeriesItemLabelsVisible(1, false);
    setSeriesPaint(1, searchColor);
    setSeriesShape(1, dataPointsShape);

    setDrawSeriesLineAsPath(true);

  }

  public void drawItem(java.awt.Graphics2D g2, XYItemRendererState state,
      java.awt.geom.Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot, ValueAxis domainAxis,
      ValueAxis rangeAxis, XYDataset dataset, int series, int item, CrosshairState crosshairState,
      int pass) {

    if (series == 0)
      g2.setComposite(pointAlpha);
    else
      g2.setComposite(selectionAlpha);

    super.drawItem(g2, state, dataArea, info, plot, domainAxis, rangeAxis, dataset, series, item,
        crosshairState, pass);
  }

  /**
   * Draws an item label.
   * 
   * @param g2 the graphics device.
   * @param orientation the orientation.
   * @param dataset the dataset.
   * @param series the series index (zero-based).
   * @param item the item index (zero-based).
   * @param x the x coordinate (in Java2D space).
   * @param y the y coordinate (in Java2D space).
   * @param negative indicates a negative value (which affects the item label position).
   */
  protected void drawItemLabel(Graphics2D g2, PlotOrientation orientation, XYDataset dataset,
      int series, int item, double x, double y, boolean negative) {

    XYItemLabelGenerator generator = getItemLabelGenerator(series, item);
    Font labelFont = getItemLabelFont(series, item);
    g2.setFont(labelFont);
    String label = generator.generateLabel(dataset, series, item);

    if ((label == null) || (label.length() == 0))
      return;

    // get the label position..
    ItemLabelPosition position = null;
    if (!negative) {
      position = getPositiveItemLabelPosition(series, item);
    } else {
      position = getNegativeItemLabelPosition(series, item);
    }

    // work out the label anchor point...
    Point2D anchorPoint =
        calculateLabelAnchorPoint(position.getItemLabelAnchor(), x, y, orientation);

    FontMetrics metrics = g2.getFontMetrics(labelFont);
    int width = SwingUtilities.computeStringWidth(metrics, label) + 2;
    int height = metrics.getHeight();

    int X = (int) (anchorPoint.getX() - (width / 2));
    int Y = (int) (anchorPoint.getY() - (height));

    g2.setPaint(searchColor);
    g2.fillRect(X, Y, width, height);

    super.drawItemLabel(g2, orientation, dataset, series, item, x, y, negative);

  }

}
