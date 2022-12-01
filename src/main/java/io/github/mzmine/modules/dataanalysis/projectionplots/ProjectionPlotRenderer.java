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

package io.github.mzmine.modules.dataanalysis.projectionplots;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;

import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

public class ProjectionPlotRenderer extends XYLineAndShapeRenderer {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  private Paint[] paintsForGroups;

  private final Color[] avoidColors = {new Color(255, 255, 85)};

  private static final Shape dataPointsShape = new Ellipse2D.Double(-6, -6, 12, 12);

  private ProjectionPlotDataset dataset;

  private boolean isAvoidColor(Color color) {
    for (Color c : avoidColors) {
      if ((color.getRed() >= c.getRed()) && (color.getGreen() >= c.getGreen())
          && (color.getBlue() >= c.getBlue()))
        return true;
    }

    setDrawSeriesLineAsPath(true);

    return false;
  }

  public ProjectionPlotRenderer(XYPlot plot, ProjectionPlotDataset dataset) {
    super(false, true);
    this.dataset = dataset;
    this.setSeriesShape(0, dataPointsShape);

    paintsForGroups = new Paint[dataset.getNumberOfGroups()];
    DrawingSupplier drawSupp = plot.getDrawingSupplier();
    for (int groupNumber = 0; groupNumber < dataset.getNumberOfGroups(); groupNumber++) {

      Paint nextPaint = drawSupp.getNextPaint();
      while (isAvoidColor((Color) nextPaint))
        nextPaint = drawSupp.getNextPaint();

      paintsForGroups[groupNumber] = nextPaint;

    }

  }

  public Paint getItemPaint(int series, int item) {

    int groupNumber = dataset.getGroupNumber(item);
    return paintsForGroups[groupNumber];
  }

  public Paint getGroupPaint(int groupNumber) {
    return paintsForGroups[groupNumber];
  }

  protected Shape getDataPointsShape() {
    return dataPointsShape;
  }

}
