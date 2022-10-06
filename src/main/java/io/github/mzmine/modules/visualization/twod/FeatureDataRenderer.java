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

package io.github.mzmine.modules.visualization.twod;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;

import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

/**
 * 
 */
class FeatureDataRenderer extends XYLineAndShapeRenderer {

  private static final long serialVersionUID = 1L;

  private static final Color peakColor = Color.green;

  // data points shape
  private static final Shape dataPointsShape = new Ellipse2D.Double(-2, -2, 5, 5);

  FeatureDataRenderer() {
    setDefaultShapesFilled(true);
    setDrawOutlines(false);
    setUseFillPaint(false);
    setDefaultShapesVisible(false);
    setDefaultShape(dataPointsShape);

    FeatureToolTipGenerator toolTipGenerator = new FeatureToolTipGenerator();
    setDefaultToolTipGenerator(toolTipGenerator);
    setDrawSeriesLineAsPath(true);

  }

  public Paint getItemPaint(int row, int column) {
    return peakColor;
  }

  public Shape getItemShape(int row, int column) {
    return dataPointsShape;
  }

}
