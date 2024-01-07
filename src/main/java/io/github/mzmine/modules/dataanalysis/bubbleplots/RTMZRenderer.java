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

package io.github.mzmine.modules.dataanalysis.bubbleplots;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;

import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.AbstractXYZDataset;

public class RTMZRenderer extends XYLineAndShapeRenderer {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  private static final Shape dataPointsShape = new Ellipse2D.Double(-3, -3, 7, 7);

  private AbstractXYZDataset dataset;
  private PaintScale paintScale;

  public RTMZRenderer(AbstractXYZDataset dataset, PaintScale paintScale) {
    super(false, true);
    this.dataset = dataset;
    this.paintScale = paintScale;
    this.setSeriesShape(0, dataPointsShape);

    setDrawSeriesLineAsPath(true);
  }

  @Override
  public Paint getItemPaint(int series, int item) {

    double cv = dataset.getZValue(series, item);
    if (Double.isNaN(cv))
      return new Color(255, 0, 0);

    return paintScale.getPaint(cv);

  }

  void setPaintScale(PaintScale paintScale) {
    this.paintScale = paintScale;
  }

}
