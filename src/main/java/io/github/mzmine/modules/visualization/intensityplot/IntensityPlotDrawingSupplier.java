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

package io.github.mzmine.modules.visualization.intensityplot;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.AffineTransform;

import org.jfree.chart.plot.DefaultDrawingSupplier;

/**
 * Supplier for shapes and color for the intensity plot data series
 *
 */
class IntensityPlotDrawingSupplier extends DefaultDrawingSupplier {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  // use shapes 1.75 times bigger than default
  private final AffineTransform resizeTransform = AffineTransform.getScaleInstance(1.75, 1.75);

  public Shape getNextShape() {
    Shape baseShape = super.getNextShape();
    return resizeTransform.createTransformedShape(baseShape);
  }

  public Paint getNextPaint() {

    // get new color from the default supplier
    Color baseColor = (Color) super.getNextPaint();

    // ban colors that are too bright
    int colorSum = baseColor.getRed() + baseColor.getGreen() + baseColor.getBlue();
    if (colorSum > 520)
      baseColor = baseColor.darker();

    return baseColor;
  }

}
